package kr.hhplus.be.server.outbox.domain

class OutboxMessage(
    val id: Long? = null,
    val aggregateType: AggregateType,
    val eventType: EventType,
    val payload: Map<String, Any>,
    var status: OutboxStatus
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
