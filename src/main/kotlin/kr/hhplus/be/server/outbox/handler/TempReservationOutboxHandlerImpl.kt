package kr.hhplus.be.server.outbox.handler

import io.lettuce.core.RedisException
import kr.hhplus.be.server.common.port.TimeUtil
import kr.hhplus.be.server.common.util.TimeProvider
import kr.hhplus.be.server.outbox.domain.AggregateType
import kr.hhplus.be.server.outbox.domain.OutboxMessage
import kr.hhplus.be.server.outbox.domain.OutboxStatus
import kr.hhplus.be.server.outbox.exception.OutboxException
import kr.hhplus.be.server.outbox.port.OutboxHandler
import kr.hhplus.be.server.outbox.port.OutboxRepository
import kr.hhplus.be.server.reservation.port.TempReservationPort
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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
            val dateString = payload["date"] as String
            val localDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)

            tempReservationService.save(
                localDate,
                (payload["id"] as Number).toLong(),
                payload["seatNumber"] as Int)
        } catch (e: Exception) {
            log.error { "Redis 처리 중 예외가 발생하였습니다." }
            throw OutboxException("임시 대기열 저장 중 에러 발생 : ${e.message}")
        }
        message.status = OutboxStatus.DONE
        message.processedAt = timeUtil.nowDateTime()
        return outboxRepository.save(message)
    }

}
