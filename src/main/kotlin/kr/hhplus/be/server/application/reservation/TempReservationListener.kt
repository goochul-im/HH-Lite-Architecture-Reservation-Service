package kr.hhplus.be.server.application.reservation

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import kr.hhplus.be.server.domain.reservation.ReservationRepository
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.stereotype.Component
import kr.hhplus.be.server.application.reservation.TempReservationConstant.CHECK_SEAT
import kr.hhplus.be.server.application.reservation.TempReservationConstant.TEMP_RESERVATIONS
import kr.hhplus.be.server.domain.reservation.ReservationStatus
import mu.KotlinLogging

@Component
class TempReservationListener(
    container: RedisMessageListenerContainer,
    private val tempReservationComponent: TempReservationComponent
) : KeyExpirationEventMessageListener(container) {

    private val log = KotlinLogging.logger {  }

    @Transactional
    override fun onMessage(message: Message, pattern: ByteArray?) {

        val expireKey = String(message.body)

        if (expireKey.startsWith(TEMP_RESERVATIONS)) {

            try {
                val reservationId = expireKey.split(":")[2].toLong()
                tempReservationComponent.cleanupExpiredReservation(reservationId)
            } catch (e: Exception) {
                log.info { "예외 발생 !!" }
                throw e
            }

        }

    }

}
