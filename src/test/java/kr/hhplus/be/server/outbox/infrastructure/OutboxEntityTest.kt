package kr.hhplus.be.server.outbox.infrastructure

import kr.hhplus.be.server.outbox.domain.AggregateType
import kr.hhplus.be.server.outbox.domain.EventType
import kr.hhplus.be.server.outbox.domain.OutboxMessage
import kr.hhplus.be.server.outbox.domain.OutboxStatus
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OutboxEntityTest {

    @Test
    fun `엔티티에서 도메인 모델로 변환이 가능하다`(){
        //given
        val entity = OutboxEntity(
            1L,
            AggregateType.TEMP_RESERVATION,
            EventType.INSERT,
            """{"key1":"value1","key2":"value2","key3":"value3"}""",
            OutboxStatus.PENDING
        )

        //when
        val result = entity.toDomain()

        //then
        assertThat(result.id).isEqualTo(1L)
        assertThat(result.aggregateType).isEqualTo(AggregateType.TEMP_RESERVATION)
        assertThat(result.eventType).isEqualTo(EventType.INSERT)
        assertThat(result.payload)
            .containsEntry("key1", "value1")
            .containsEntry("key2", "value2")
            .containsEntry("key3", "value3")
        assertThat(result.status).isEqualTo(OutboxStatus.PENDING)
    }

    @Test
    fun `도메인 모델에서 엔티티로 변환이 가능하다`(){
        //given
        val domain = OutboxMessage(
            1L,
            AggregateType.TEMP_RESERVATION,
            EventType.INSERT,
            mapOf("key1" to "value1","key2" to "value2","key3" to "value3"),
            OutboxStatus.PENDING
        )

        //when
        val result = OutboxEntity.from(domain)

        //then
        assertThat(result.id).isEqualTo(1L)
        assertThat(result.aggregateType).isEqualTo(AggregateType.TEMP_RESERVATION)
        assertThat(result.eventType).isEqualTo(EventType.INSERT)
        assertThat(result.payload).isEqualTo("""{"key1":"value1","key2":"value2","key3":"value3"}""")
        assertThat(result.status).isEqualTo(OutboxStatus.PENDING)
    }

}
