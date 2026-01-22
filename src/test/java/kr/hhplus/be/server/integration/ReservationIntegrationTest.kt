package kr.hhplus.be.server.integration

import kr.hhplus.be.server.TestcontainersConfiguration
import kr.hhplus.be.server.concert.infrastructure.ConcertEntity
import kr.hhplus.be.server.concert.infrastructure.ConcertJpaRepository
import kr.hhplus.be.server.member.infrastructure.MemberEntity
import kr.hhplus.be.server.member.infrastructure.MemberJpaRepository
import kr.hhplus.be.server.outbox.scheduler.OutboxScheduler
import kr.hhplus.be.server.reservation.dto.ReservationRequest
import kr.hhplus.be.server.reservation.infrastructure.RedisReservationOperations
import kr.hhplus.be.server.reservation.infrastructure.ReservationJpaRepository
import kr.hhplus.be.server.reservation.domain.ReservationStatus
import kr.hhplus.be.server.reservation.infrastructure.TempReservationAdaptor
import kr.hhplus.be.server.reservation.infrastructure.TempReservationConstant
import kr.hhplus.be.server.reservation.service.ReservationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@SpringBootTest
@Import(TestcontainersConfiguration::class)
@ActiveProfiles("test")
class ReservationIntegrationTest {

    @Autowired
    private lateinit var reservationService: ReservationService

    @Autowired
    private lateinit var memberJpaRepository: MemberJpaRepository

    @Autowired
    private lateinit var concertJpaRepository: ConcertJpaRepository

    @Autowired
    private lateinit var reservationJpaRepository: ReservationJpaRepository

    @Autowired
    private lateinit var redissonClient: RedissonClient

    @Autowired
    lateinit var redisReservationOperations: RedisReservationOperations

    @Autowired
    lateinit var outboxScheduler: OutboxScheduler

    @Autowired
    lateinit var tempReservationAdaptor: TempReservationAdaptor

    private lateinit var testMemberEntity: MemberEntity
    private lateinit var testConcert: ConcertEntity

    @BeforeEach
    fun setUp() {
        reservationJpaRepository.deleteAll()
        concertJpaRepository.deleteAll()
        memberJpaRepository.deleteAll()
        redissonClient.keys.flushall()

        testMemberEntity = memberJpaRepository.save(MemberEntity(username = "Test User", password = "password"))
        testConcert = concertJpaRepository.save(
            ConcertEntity(name = "통합 테스트 콘서트", date = LocalDate.now(), totalSeats = 50)
        )
    }

    @AfterEach
    fun redisCleanup() {
        redisReservationOperations.cleanUp()
    }

    @Test
    @DisplayName("예약 생성부터 만료까지의 전체 흐름 통합 테스트")
    fun reservation_full_flow_integration_test() {
        // Given: 예약 정보
        val concertId = testConcert.id!!
        val seatNumber = 25
        val reservationRequest = ReservationRequest(concertId, testMemberEntity.id!!, seatNumber)

        // When: 1. 예약을 생성한다.
        reservationService.make(reservationRequest)

        outboxScheduler.schedule() // 강제 스케줄링 실행 (Outbox 패턴 처리)

        // Then: 2. DB와 Redis에 예약 정보가 올바르게 저장되었는지 확인한다.
        val savedReservation = reservationJpaRepository.findAll().firstOrNull()
        assertThat(savedReservation).isNotNull()
        assertThat(savedReservation?.status).isEqualTo(ReservationStatus.PENDING)

        val reservationId = savedReservation!!.id!!
        val seatListKey = "${TempReservationConstant.TEMP_RESERVATIONS}$reservationId"
        val reserveKey = "${TempReservationConstant.CHECK_SEAT}$concertId"

        // Redisson 객체를 통해 확인
        val bucket = redissonClient.getBucket<String>(seatListKey)
        val set = redissonClient.getSet<String>(reserveKey)

        assertThat(bucket.isExists).isTrue()
        assertThat(set.contains(seatNumber.toString())).isTrue()

        // When: 3. 예약이 만료될 때 (임시 예약 정리 로직 호출)
        tempReservationAdaptor.cleanupExpiredReservation(reservationId)

        // Then: 4. 최종 상태 및 Redis 데이터 삭제 확인
        val finalReservation = reservationJpaRepository.findById(reservationId).get()
        assertThat(finalReservation.status).isEqualTo(ReservationStatus.CANCEL)

        // Redis 데이터가 정상적으로 삭제되었는지 확인
        assertThat(bucket.isExists).isFalse()
        assertThat(set.contains(seatNumber.toString())).isFalse()
    }
}
