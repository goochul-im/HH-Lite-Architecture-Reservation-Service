package kr.hhplus.be.server.outbox.infrastructure

import kr.hhplus.be.server.outbox.domain.OutboxMessage
import kr.hhplus.be.server.outbox.port.OutboxRepository
import org.springframework.stereotype.Repository

@Repository
class OutboxRepositoryImpl(
    private val repo: OutboxJpaRepository,
) : OutboxRepository {

    override fun save(outboxMessage: OutboxMessage): OutboxMessage {
        val entity = OutboxEntity.from(outboxMessage)
        return repo.save(entity).toDomain()
    }

}
