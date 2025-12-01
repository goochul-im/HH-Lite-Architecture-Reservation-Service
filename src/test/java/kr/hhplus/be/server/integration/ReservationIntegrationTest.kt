package kr.hhplus.be.server.integration

import kr.hhplus.be.server.TestcontainersConfiguration
import kr.hhplus.be.server.reservation.dto.ReservationRequest
import kr.hhplus.be.server.member.infrastructure.MemberEntity
import kr.hhplus.be.server.member.infrastructure.MemberJpaRepository
import kr.hhplus.be.server.reservation.infrastructure.ReservationJpaRepository
import kr.hhplus.be.server.reservation.infrastructure.ReservationStatus
import kr.hhplus.be.server.reservation.service.ReservationService
import kr.hhplus.be.server.reservation.TempReservationConstant
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
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

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, String>

    private lateinit var testMemberEntity: MemberEntity

    @BeforeEach
    fun setUp() {
        // 테스트 데이터 정리
        reservationJpaRepository.deleteAll()
        memberJpaRepository.deleteAll()
        redisTemplate.connectionFactory?.connection?.flushAll()

        // 테스트용 사용자 생성
        testMemberEntity = memberJpaRepository.save(MemberEntity(username = "Test User", password = "password"))
    }

    @Test
    @DisplayName("예약 생성부터 만료까지의 전체 흐름 통합 테스트")
    fun reservation_full_flow_integration_test() {
        // Given: 예약 정보
        val date = LocalDate.now()
        val seatNumber = 25
        val reservationRequest = ReservationRequest(date, testMemberEntity.id!!, seatNumber)

        // When: 1. 예약을 생성한다. (만료 시간은 1초로 설정)
        reservationService.make(reservationRequest)

        // Then: 2. DB와 Redis에 예약 정보가 올바르게 저장되었는지 확인한다.
        val savedReservation = reservationJpaRepository.findAll().firstOrNull()
        assertThat(savedReservation).isNotNull()
        assertThat(savedReservation?.status).isEqualTo(ReservationStatus.PENDING)

        val reservationId = savedReservation!!.id!!
        val seatListKey = "${TempReservationConstant.TEMP_RESERVATIONS}$reservationId"
        val reserveKey = "${TempReservationConstant.CHECK_SEAT}$date"

        assertThat(redisTemplate.hasKey(seatListKey)).isTrue()
        assertThat(redisTemplate.opsForSet().isMember(reserveKey, seatNumber.toString())).isTrue()

        // When: 3. 예약이 만료될 때까지 기다린다. (awaitility 사용)
        await.atMost(Duration.ofSeconds(5)).untilAsserted {
            val expiredReservation = reservationJpaRepository.findById(reservationId).orElse(null)
            assertThat(expiredReservation).isNotNull()
            assertThat(expiredReservation?.status).isEqualTo(ReservationStatus.CANCEL)
        }

        // Then: 4. 예약 만료 후 DB와 Redis의 데이터가 정상적으로 정리되었는지 확인한다.
        val finalReservation = reservationJpaRepository.findById(reservationId).get()
        assertThat(finalReservation.status).isEqualTo(ReservationStatus.CANCEL)
        assertThat(redisTemplate.hasKey(seatListKey)).isFalse()
        assertThat(redisTemplate.opsForSet().isMember(reserveKey, seatNumber.toString())).isFalse()
    }
}
