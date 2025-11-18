package kr.hhplus.be.server.domain.reservation.service

import jakarta.persistence.EntityNotFoundException
import kr.hhplus.be.server.member.Member
import kr.hhplus.be.server.reservation.domain.Reservation
import kr.hhplus.be.server.reservation.domain.ReservationRepository
import kr.hhplus.be.server.reservation.domain.ReservationStatus
import kr.hhplus.be.server.reservation.service.TempReservationAdaptor
import kr.hhplus.be.server.reservation.TempReservationConstant
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.*
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.redis.connection.RedisConnection
import org.springframework.data.redis.core.*
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.util.*

@ActiveProfiles("test")
@ExtendWith(MockitoExtension::class)
class TempReservationAdaptorTest {

    @Mock
    private lateinit var redisTemplate: StringRedisTemplate

    @Mock
    private lateinit var reservationRepository: ReservationRepository

    // Mocks for Redis operations
    @Mock
    private lateinit var valueOperations: ValueOperations<String, Any>

    @Mock
    private lateinit var setOperations: SetOperations<String, String>

    @Mock
    private lateinit var redisOperations: RedisOperations<String, Any>

    @Mock
    private lateinit var redisConnection: RedisConnection

    @InjectMocks
    private lateinit var tempReservationAdaptor: TempReservationAdaptor


//    @Test
//    @DisplayName("임시 예약 정보 Redis 저장 테스트")
//    fun save_temp_reservation_to_redis_test() {
//        // Given
//        val date = LocalDate.now()
//        val reservationId = 1L
//        val seatNumber = 10
//
//        val seatListKey = "${TempReservationConstant.TEMP_RESERVATIONS}$reservationId"
//        val reserveKey = "${TempReservationConstant.CHECK_SEAT}$date"
//
//        // Mock RedisTemplate behavior
//        given(redisTemplate.opsForValue()).willReturn(valueOperations)
//        given(redisTemplate.opsForSet()).willReturn(setOperations)
//        given(redisTemplate.expire(any<String>(), any<Duration>())).willReturn(true)
//
//        // Mock the execute block
//        given(redisTemplate.execute(any<RedisCallback<*>>())).willAnswer {
//            null
//        }
//
//        // When
//        tempReservationComponent.save(date, reservationId, seatNumber)
//
//        // Then
//        verify(valueOperations).set(seatListKey, seatNumber)
//        verify(setOperations).add(reserveKey, seatNumber)
//        verify(redisTemplate).expire(eq(seatListKey), any<Duration>())
//    }

    @Test
    @DisplayName("만료된 예약 정리 테스트")
    fun cleanup_expired_reservation_test() {
        // Given
        val reservationId = 1L
        val date = LocalDate.now()
        val seatNumber = 15
        val member = Member(id = "testmemberid", username = "Test User", password = "password")
        val reservation = Reservation(
            id = reservationId,
            date = date,
            seatNumber = seatNumber,
            status = ReservationStatus.PENDING,
            reserver = member
        )
        val reserveKey = "${TempReservationConstant.CHECK_SEAT}$date"

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation))
        given(redisTemplate.opsForSet()).willReturn(setOperations)

        // When
        tempReservationAdaptor.cleanupExpiredReservation(reservationId)

        // Then
        verify(reservationRepository).findById(reservationId)
        verify(setOperations).remove(reserveKey, seatNumber.toString())
    }

    @Test
    @DisplayName("만료된 예약 정리 시 예약 정보가 없을 경우 예외 발생 테스트")
    fun cleanup_expired_reservation_not_found_test() {
        // Given
        val reservationId = 99L
        given(reservationRepository.findById(reservationId)).willReturn(Optional.empty())

        // When & Then
        assertThatThrownBy {
            tempReservationAdaptor.cleanupExpiredReservation(reservationId)
        }.isInstanceOf(EntityNotFoundException::class.java)
            .hasMessageContaining("${reservationId}를 가진 예약 정보를 조회할 수 없습니다.")

        verify(reservationRepository).findById(reservationId)
        verify(redisTemplate, never()).opsForSet()
        verify(reservationRepository, never()).save(any())
    }
}
