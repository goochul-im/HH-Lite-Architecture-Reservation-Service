package kr.hhplus.be.server.application.point.event

import kr.hhplus.be.server.common.event.DomainEvent
import kr.hhplus.be.server.outbox.domain.AggregateType

data class PointChargedEvent(
    val memberId: String,
    val chargedAmount: Int,
    val totalPoint: Int
) : DomainEvent(
    aggregateId = memberId,
    aggregateType = AggregateType.POINT
)
