package kr.hhplus.be.server.common.event

import kr.hhplus.be.server.outbox.domain.AggregateType
import java.time.Instant
import java.util.UUID

abstract class DomainEvent (
    val eventId: String = UUID.randomUUID().toString(),
    val createdAt: Instant = Instant.now(),
    val aggregateType: AggregateType,
    val aggregateId: String
)
