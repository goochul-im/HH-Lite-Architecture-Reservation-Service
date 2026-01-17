package kr.hhplus.be.server.outbox.handler

import kr.hhplus.be.server.common.port.TimeUtil
import kr.hhplus.be.server.outbox.domain.AggregateType
import kr.hhplus.be.server.outbox.domain.OutboxMessage
import kr.hhplus.be.server.outbox.domain.OutboxStatus
import kr.hhplus.be.server.outbox.exception.OutboxException
import kr.hhplus.be.server.outbox.port.OutboxHandler
import kr.hhplus.be.server.outbox.port.OutboxRepository
import kr.hhplus.be.server.reservation.port.TempReservationPort
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class TempReservationOutboxHandlerImpl(
    private val tempReservationService: TempReservationPort,
    private val outboxRepository: OutboxRepository,
    private val timeUtil: TimeUtil
) : OutboxHandler {

    private val log = KotlinLogging.logger { }

    override fun canHandle(aggregateType: AggregateType): Boolean {
        return aggregateType == AggregateType.TEMP_RESERVATION
    }

    override fun handle(message: OutboxMessage): OutboxMessage {
        val payload = message.payload
        try {
            val concertId = (payload["concertId"] as Number).toLong()
            val reservationId = (payload["id"] as Number).toLong()
            val seatNumber = payload["seatNumber"] as Int

            tempReservationService.save(concertId, reservationId, seatNumber)
        } catch (e: Exception) {
            log.error { "Redis 처리 중 예외가 발생하였습니다." }
            throw OutboxException("임시 대기열 저장 중 에러 발생 : ${e.message}")
        }
        message.status = OutboxStatus.DONE
        message.processedAt = timeUtil.nowDateTime()
        return outboxRepository.save(message)
    }
}
