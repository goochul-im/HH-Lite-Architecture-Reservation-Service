package kr.hhplus.be.server.domain.reservation

import kr.hhplus.be.server.reservation.infrastructure.TempReservationAdaptor
import kr.hhplus.be.server.reservation.infrastructure.TempReservationConstant
import kr.hhplus.be.server.reservation.listener.TempReservationListener
import kr.hhplus.be.server.reservation.port.TempReservationPort
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.BDDMockito.*
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@ExtendWith(MockitoExtension::class)
class TempReservationListenerTest {

    @Mock
    private lateinit var container: RedisMessageListenerContainer

    @Mock
    private lateinit var tempReservationAdaptor: TempReservationPort

    @InjectMocks
    private lateinit var tempReservationListener: TempReservationListener

    @Test
    @DisplayName("임시 예약 만료 메시지 수신 시 예약 정리 메소드 호출 테스트")
    fun onMessage_cleanup_expired_reservation_test() {
        // Given
        val reservationId = 123L
        val expireKey = "${TempReservationConstant.TEMP_RESERVATIONS}$reservationId"
        val message = object : Message {
            override fun getBody(): ByteArray {
                return expireKey.toByteArray()
            }

            override fun getChannel(): ByteArray {
                return byteArrayOf()
            }

            override fun toString(): String {
                return expireKey
            }
        }
        val pattern = "test-pattern".toByteArray()

        // When
        tempReservationListener.onMessage(message, pattern)

        // Then
        verify(tempReservationAdaptor).cleanupExpiredReservation(reservationId)
    }

    @Test
    @DisplayName("관련 없는 키 만료 메시지 수신 시 무시 테스트")
    fun onMessage_ignore_irrelevant_key_test() {
        // Given
        val expireKey = "some:other:key:123"
        val message = object : Message {
            override fun getBody(): ByteArray {
                return expireKey.toByteArray()
            }

            override fun getChannel(): ByteArray {
                return byteArrayOf()
            }

            override fun toString(): String {
                return expireKey
            }
        }
        val pattern = "test-pattern".toByteArray()

        // When
        tempReservationListener.onMessage(message, pattern)

        // Then
        verify(tempReservationAdaptor, times(0)).cleanupExpiredReservation(any(Long::class.java))
    }

    @Test
    @DisplayName("메시지 처리 중 예외 발생 시 로그 기록 및 예외 전파 테스트")
    fun onMessage_exception_handling_test() {
        // Given
        val reservationId = 456L
        val expireKey = "${TempReservationConstant.TEMP_RESERVATIONS}$reservationId"
        val message = object : Message {
            override fun getBody(): ByteArray {
                return expireKey.toByteArray()
            }

            override fun getChannel(): ByteArray {
                return byteArrayOf()
            }

            override fun toString(): String {
                return expireKey
            }
        }
        val pattern = "test-pattern".toByteArray()
        val exception = RuntimeException("Test Exception")

        given(tempReservationAdaptor.cleanupExpiredReservation(reservationId)).willThrow(exception)

        // When & Then
        assertThatThrownBy {
            tempReservationListener.onMessage(message, pattern)
        }.isInstanceOf(RuntimeException::class.java)
            .isEqualTo(exception)

        verify(tempReservationAdaptor, times(1)).cleanupExpiredReservation(reservationId)
    }
}
