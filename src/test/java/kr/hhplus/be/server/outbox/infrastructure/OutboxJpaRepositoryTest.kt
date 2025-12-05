package kr.hhplus.be.server.outbox.infrastructure

import kr.hhplus.be.server.outbox.domain.AggregateType
import kr.hhplus.be.server.outbox.domain.EventType
import kr.hhplus.be.server.outbox.domain.OutboxStatus
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest

@DataJpaTest
class OutboxJpaRepositoryTest {

    @Autowired
    lateinit var repository: OutboxJpaRepository

    @Test
    fun `getPendingList를 사용하여 pending상태의 아웃박스를 가져올 수 있다`(){
        //given
        repeat(5) {
            repository.save(OutboxEntity(
                aggregateType = AggregateType.TEMP_RESERVATION,
                eventType = EventType.INSERT,
                payload = "test",
                status = OutboxStatus.PENDING
            ))
        }
        repeat(5) {
            repository.save(OutboxEntity(
                aggregateType = AggregateType.TEMP_RESERVATION,
                eventType = EventType.INSERT,
                payload = "test",
                status = OutboxStatus.CANCELLED
            ))
        }

        //when
        val pendingList : List<OutboxEntity> = repository.getPendingList()

        //then
        assertThat(pendingList).hasSize(5)
        assertThat(pendingList)
            .allMatch { it.status == OutboxStatus.PENDING }

    }

}
