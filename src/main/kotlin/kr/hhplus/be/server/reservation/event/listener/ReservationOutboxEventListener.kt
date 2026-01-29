package kr.hhplus.be.server.reservation.event.listener

import kr.hhplus.be.server.outbox.domain.AggregateType
import kr.hhplus.be.server.outbox.domain.EventType
import kr.hhplus.be.server.outbox.domain.OutboxMessage
import kr.hhplus.be.server.outbox.domain.OutboxStatus
import kr.hhplus.be.server.outbox.port.OutboxRepository
import kr.hhplus.be.server.reservation.dto.TempReservationPayload
import kr.hhplus.be.server.reservation.event.ReservationCreatedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ReservationOutboxEventListener(
    private val outboxRepository: OutboxRepository,
) {

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun reservationCreatedEvent(event: ReservationCreatedEvent) {

        outboxRepository.save(OutboxMessage(
            aggregateType = AggregateType.TEMP_RESERVATION,
            eventType = EventType.INSERT,
            payload = TempReservationPayload(event.reservationId, event.concertId, event.seatNumber).toMap(),
            status = OutboxStatus.PENDING
        ))

    }

}
