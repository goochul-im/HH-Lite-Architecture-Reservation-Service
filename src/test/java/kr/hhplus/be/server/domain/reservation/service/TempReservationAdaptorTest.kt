package kr.hhplus.be.server.domain.reservation.service

import jakarta.persistence.EntityNotFoundException
import kr.hhplus.be.server.concert.infrastructure.ConcertEntity
import kr.hhplus.be.server.reservation.infrastructure.ReservationEntity
import kr.hhplus.be.server.reservation.infrastructure.ReservationJpaRepository
import kr.hhplus.be.server.reservation.domain.ReservationStatus
import kr.hhplus.be.server.reservation.infrastructure.TempReservationAdaptor
import kr.hhplus.be.server.reservation.infrastructure.RedisReservationOperations
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.*
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.cache.CacheManager
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
class TempReservationAdaptorTest {

    @Mock
    private lateinit var redisOperations: RedisReservationOperations

    @Mock
    private lateinit var reservationJpaRepository: ReservationJpaRepository

    @Mock
    private lateinit var cacheManager: CacheManager

    @InjectMocks
    private lateinit var adaptor: TempReservationAdaptor

    private val concertId = 1L
    private val reservationId = 1L
    private val seatNumber = 5

    @Test
    fun `save 호출 시 redisOperations의 saveTempReservation 호출`() {
        // When
        adaptor.save(concertId, reservationId, seatNumber)

        // Then
        verify(redisOperations).saveTempReservation(
            seatListKey = "temp:reservations:$reservationId",
            reserveKey = "check:seat:$concertId",
            seatNumber = seatNumber,
            timeoutSeconds = 300L
        )
    }

    @Test
    fun `getTempReservation 호출 시 올바른 값 반환`() {
        // Given
        val expectedSeats = listOf(1, 2, 3)
        given(redisOperations.getTempReservedSeats("check:seat:$concertId"))
            .willReturn(expectedSeats)

        // When
        val result = adaptor.getTempReservation(concertId)

        // Then
        assertThat(result)
            .isNotNull()
            .isEqualTo(expectedSeats)
            .hasSize(3)
            .containsExactly(1, 2, 3)
    }

    @Test
    fun `getTempReservation 빈 목록 반환`() {
        // Given
        given(redisOperations.getTempReservedSeats("check:seat:$concertId"))
            .willReturn(emptyList())

        // When
        val result = adaptor.getTempReservation(concertId)

        // Then
        assertThat(result)
            .isEmpty()
    }

    @Test
    fun `isValidReservation이 true 반환`() {
        // Given
        given(redisOperations.isReservationExists("temp:reservations:$reservationId"))
            .willReturn(true)

        // When
        val result = adaptor.isValidReservation(reservationId)

        // Then
        assertThat(result)
            .isNotNull()
            .isTrue()
    }

    @Test
    fun `isValidReservation이 false 반환`() {
        // Given
        given(redisOperations.isReservationExists("temp:reservations:$reservationId"))
            .willReturn(false)

        // When
        val result = adaptor.isValidReservation(reservationId)

        // Then
        assertThat(result)
            .isNotNull
            .isFalse
    }

    @Test
    fun `delete 호출 시 redisOperations의 deleteReservation 호출`() {
        // When
        adaptor.delete(reservationId)

        // Then
        verify(redisOperations, times(1))
            .deleteReservation("temp:reservations:$reservationId")
    }

    @Test
    fun `cleanupExpiredReservation 성공`() {
        // Given
        val concertEntity = ConcertEntity(
            id = concertId,
            name = "테스트 콘서트",
            date = LocalDate.of(2025, 11, 19),
            totalSeats = 50
        )
        val reservationEntity = ReservationEntity(
            id = reservationId,
            concert = concertEntity,
            seatNumber = seatNumber,
            status = ReservationStatus.PENDING,
            reserver = null
        )

        given(reservationJpaRepository.findById(reservationId))
            .willReturn(Optional.of(reservationEntity))
        given(reservationJpaRepository.save(any()))
            .willReturn(reservationEntity)

        // When
        adaptor.cleanupExpiredReservation(reservationId)

        // Then
        verify(redisOperations, times(1))
            .removeFromReserveSet("check:seat:$concertId", seatNumber)

        verify(reservationJpaRepository, times(1))
            .save(any())

        assertThat(reservationEntity.status)
            .isNotNull
            .isEqualTo(ReservationStatus.CANCEL)
    }

    @Test
    fun `cleanupExpiredReservation 예약이 없을 때 EntityNotFoundException 발생`() {
        // Given
        given(reservationJpaRepository.findById(reservationId))
            .willReturn(Optional.empty())

        // When & Then
        assertThatThrownBy {
            adaptor.cleanupExpiredReservation(reservationId)
        }
            .isInstanceOf(EntityNotFoundException::class.java)
            .hasMessage("${reservationId}를 가진 예약 정보를 조회할 수 없습니다.")
    }

    @Test
    fun `cleanupExpiredReservation 예약 없을 때 Redis 작업 미실행`() {
        // Given
        given(reservationJpaRepository.findById(reservationId))
            .willReturn(Optional.empty())

        // When & Then
        assertThatThrownBy {
            adaptor.cleanupExpiredReservation(reservationId)
        }
            .isInstanceOf(EntityNotFoundException::class.java)

        // Redis 작업이 호출되지 않음을 확인
        verify(redisOperations, times(0))
            .removeFromReserveSet(anyString(), anyInt())
    }
}
