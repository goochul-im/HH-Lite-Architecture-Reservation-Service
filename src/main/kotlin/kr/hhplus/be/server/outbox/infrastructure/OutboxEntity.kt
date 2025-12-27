package kr.hhplus.be.server.outbox.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import kr.hhplus.be.server.common.BaseEntity
import kr.hhplus.be.server.common.util.SerializeUtil
import kr.hhplus.be.server.outbox.domain.AggregateType
import kr.hhplus.be.server.outbox.domain.EventType
import kr.hhplus.be.server.outbox.domain.OutboxMessage
import kr.hhplus.be.server.outbox.domain.OutboxStatus
import kr.hhplus.be.server.reservation.infrastructure.ReservationStatus
import java.time.LocalDateTime

@Entity
@Table(
    name = "outbox",
    indexes = [
        Index(name = "idx_status_created_at", columnList = "status, created_at")
    ]
)
class OutboxEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, name = "aggregate_type")
    @Enumerated(EnumType.STRING)
    val aggregateType: AggregateType,

    @Column(nullable = false, name = "envent_type")
    @Enumerated(EnumType.STRING)
    val eventType: EventType,

    @Column(columnDefinition = "json")
    val payload: String,

    @Column(nullable = false, name = "status")
    @Enumerated(EnumType.STRING)
    val status: OutboxStatus,

    @Column(nullable = true, name = "processed_at")
    val processedAt: LocalDateTime? = null

) : BaseEntity() {

    fun toDomain(): OutboxMessage {
        return OutboxMessage(
            this.id,
            this.aggregateType,
            this.eventType,
            SerializeUtil.outboxStringToMap(this.payload),
            this.status,
            this.processedAt
        )
    }

    companion object {
        fun from(domain: OutboxMessage): OutboxEntity {
            return OutboxEntity(
                domain.id,
                domain.aggregateType,
                domain.eventType,
                SerializeUtil.outboxMapToString(domain.payload),
                domain.status,
                domain.processedAt
            )
        }
    }

}
