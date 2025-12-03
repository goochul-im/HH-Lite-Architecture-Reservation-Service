package kr.hhplus.be.server.outbox.domain

import java.time.LocalDateTime

class OutboxMessage(
    val id: Long? = null,
    val aggregateType: AggregateType,
    val eventType: EventType,
    val payload: Map<String, Any>,
    var status: OutboxStatus,
    var processedAt: LocalDateTime? = null,
)

enum class AggregateType{
    TEMP_RESERVATION,
}

enum class EventType{
    INSERT
}

enum class OutboxStatus{
    PENDING, CANCELLED, DONE
}
