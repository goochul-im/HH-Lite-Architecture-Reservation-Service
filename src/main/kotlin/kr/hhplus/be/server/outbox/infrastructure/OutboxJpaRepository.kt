package kr.hhplus.be.server.outbox.infrastructure

import kr.hhplus.be.server.outbox.domain.OutboxMessage
import org.springframework.data.jpa.repository.JpaRepository

interface OutboxJpaRepository : JpaRepository<OutboxMessage, Long> {
}
