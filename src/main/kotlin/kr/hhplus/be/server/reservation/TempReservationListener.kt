package kr.hhplus.be.server.reservation

import jakarta.transaction.Transactional
import kr.hhplus.be.server.reservation.port.TempReservationPort
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.stereotype.Component
import mu.KotlinLogging

@Component
class TempReservationListener(
    container: RedisMessageListenerContainer,
    private val tempReservationAdaptor: TempReservationPort
) : KeyExpirationEventMessageListener(container) {

    private val log = KotlinLogging.logger {  }

    @Transactional
    override fun onMessage(message: Message, pattern: ByteArray?) {

        val expireKey = String(message.body)

        if (expireKey.startsWith(TempReservationConstant.TEMP_RESERVATIONS)) {

            try {
                val reservationId = expireKey.split(":")[2].toLong()
                tempReservationAdaptor.cleanupExpiredReservation(reservationId)
            } catch (e: Exception) {
                log.info { "예외 발생 !!" }
                throw e
            }

        }

    }

}
