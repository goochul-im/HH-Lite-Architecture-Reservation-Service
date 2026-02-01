package kr.hhplus.be.server.reservation.event

import kr.hhplus.be.server.common.event.DomainEvent
import kr.hhplus.be.server.outbox.domain.AggregateType

data class ReservationExpiredEvent(
    val reservationId: Long,
    val concertId: Long
) : DomainEvent(
    aggregateId = reservationId.toString(),
    aggregateType = AggregateType.RESERVATION
)
