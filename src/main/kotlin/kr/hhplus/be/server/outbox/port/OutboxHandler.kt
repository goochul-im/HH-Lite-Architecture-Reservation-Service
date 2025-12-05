package kr.hhplus.be.server.outbox.port

import kr.hhplus.be.server.outbox.domain.AggregateType
import kr.hhplus.be.server.outbox.domain.EventType
import kr.hhplus.be.server.outbox.domain.OutboxMessage

interface OutboxHandler {

    fun canHandle(aggregateType: AggregateType): Boolean

    fun handle(message: OutboxMessage) : OutboxMessage

}
