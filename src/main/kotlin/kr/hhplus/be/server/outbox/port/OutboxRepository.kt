package kr.hhplus.be.server.outbox.port

import kr.hhplus.be.server.outbox.domain.OutboxMessage

interface OutboxRepository {

    fun save(outboxMessage: OutboxMessage) : OutboxMessage

    fun getPendingList() : List<OutboxMessage>

}
