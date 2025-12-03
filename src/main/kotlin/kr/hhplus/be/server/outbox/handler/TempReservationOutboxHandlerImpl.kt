package kr.hhplus.be.server.outbox.handler

import kr.hhplus.be.server.outbox.domain.AggregateType
import kr.hhplus.be.server.outbox.domain.OutboxMessage
import kr.hhplus.be.server.outbox.domain.OutboxStatus
import kr.hhplus.be.server.outbox.port.OutboxHandler
import kr.hhplus.be.server.outbox.port.OutboxRepository
import kr.hhplus.be.server.reservation.port.TempReservationPort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class TempReservationOutboxHandlerImpl(
    private val tempReservationService: TempReservationPort,
    private val outboxRepository: OutboxRepository,
) : OutboxHandler {

    override fun canHandle(aggregateType: AggregateType): Boolean {
        return aggregateType == AggregateType.TEMP_RESERVATION
    }

    /**
     * 새로운 트랜잭션에서 시작합니다. 같은 트랜잭션 내에서 시작할 경우 아웃박스도
     * 롤백되어 삭제되는 경우가 생길 수 있습니다.
     * outboxRepository에 DONE 상태로 업데이트합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun handle(message: OutboxMessage): OutboxMessage {
        val payload = message.payload
        tempReservationService.save(
            payload["date"] as LocalDate,
            payload["id"] as Long,
            payload["seatNumber"] as Int)
        message.status = OutboxStatus.DONE
        return outboxRepository.save(message)
    }

}
