package kr.hhplus.be.server.integration

import kr.hhplus.be.server.TestcontainersConfiguration
import kr.hhplus.be.server.reservation.dto.ReservationRequest
import kr.hhplus.be.server.member.infrastructure.MemberEntity
import kr.hhplus.be.server.member.infrastructure.MemberJpaRepository
import kr.hhplus.be.server.outbox.scheduler.OutboxScheduler
import kr.hhplus.be.server.reservation.infrastructure.RedisReservationOperations
import kr.hhplus.be.server.reservation.infrastructure.ReservationJpaRepository
import kr.hhplus.be.server.reservation.infrastructure.ReservationStatus
import kr.hhplus.be.server.reservation.infrastructure.TempReservationAdaptor
import kr.hhplus.be.server.reservation.service.ReservationService
import kr.hhplus.be.server.reservation.infrastructure.TempReservationConstant
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
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
    private lateinit var reservationJpaRepository: ReservationJpaRepository

    // RedisTemplate 대신 RedissonClient 주입
    @Autowired
    private lateinit var redissonClient: RedissonClient

    @Autowired
    lateinit var redisReservationOperations: RedisReservationOperations

    @Autowired
    lateinit var outboxScheduler: OutboxScheduler

    @Autowired
    lateinit var tempReservationAdaptor: TempReservationAdaptor

    private lateinit var testMemberEntity: MemberEntity

    @BeforeEach
    fun setUp() {
        // 테스트 데이터 정리
        reservationJpaRepository.deleteAll()
        memberJpaRepository.deleteAll()

        // Redisson 방식으로 전체 삭제
        redissonClient.keys.flushall()

        // 테스트용 사용자 생성
        testMemberEntity = memberJpaRepository.save(MemberEntity(username = "Test User", password = "password"))
    }

    @AfterEach
    fun redisCleanup() {
        redisReservationOperations.cleanUp()
    }

    @Test
    @DisplayName("예약 생성부터 만료까지의 전체 흐름 통합 테스트")
    fun reservation_full_flow_integration_test() {
        // Given: 예약 정보
        val date = LocalDate.now()
        val seatNumber = 25
        val reservationRequest = ReservationRequest(date, testMemberEntity.id!!, seatNumber)

        // When: 1. 예약을 생성한다.
        reservationService.make(reservationRequest)

        outboxScheduler.schedule() // 강제 스케줄링 실행 (Outbox 패턴 처리)

        // Then: 2. DB와 Redis에 예약 정보가 올바르게 저장되었는지 확인한다.
        val savedReservation = reservationJpaRepository.findAll().firstOrNull()
        assertThat(savedReservation).isNotNull()
        assertThat(savedReservation?.status).isEqualTo(ReservationStatus.PENDING)

        val reservationId = savedReservation!!.id!!
        val seatListKey = "${TempReservationConstant.TEMP_RESERVATIONS}$reservationId"
        val reserveKey = "${TempReservationConstant.CHECK_SEAT}$date"

        // Redisson 객체를 통해 확인
        val bucket = redissonClient.getBucket<String>(seatListKey)
        val set = redissonClient.getSet<String>(reserveKey)

        assertThat(bucket.isExists).isTrue()
        assertThat(set.contains(seatNumber.toString())).isTrue()

        // When 3 단계 바로 위에서 확인
        println("Check Key: $seatListKey")
        println("Exists in Redis: ${redissonClient.getBucket<String>(seatListKey).isExists}")

        // 실제 Redis에 어떤 키들이 있는지 출력
        redissonClient.keys.getKeys().forEach { println("Actual Key in Redis: $it") }

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
