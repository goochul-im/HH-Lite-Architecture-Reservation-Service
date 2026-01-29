package kr.hhplus.be.server.reservation.event

import kr.hhplus.be.server.common.event.DomainEvent
import kr.hhplus.be.server.outbox.domain.AggregateType

data class ReservationPaidEvent(
    val reservationId: Long,
    val concertId: Long,
    val memberId: String
) : DomainEvent(
    aggregateId = reservationId.toString(),
    aggregateType = AggregateType.RESERVATION
)
