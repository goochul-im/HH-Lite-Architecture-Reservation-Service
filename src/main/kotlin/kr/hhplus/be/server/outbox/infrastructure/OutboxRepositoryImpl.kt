package kr.hhplus.be.server.outbox.infrastructure

import kr.hhplus.be.server.outbox.port.OutboxRepository
import org.springframework.stereotype.Repository

@Repository
class OutboxRepositoryImpl(
    private val repo: OutboxJpaRepository,
) : OutboxRepository {
}
