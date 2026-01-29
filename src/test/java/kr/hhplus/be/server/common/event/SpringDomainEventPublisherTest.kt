package kr.hhplus.be.server.common.event

import kr.hhplus.be.server.outbox.domain.AggregateType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.springframework.context.ApplicationEventPublisher

@ExtendWith(MockitoExtension::class)
class SpringDomainEventPublisherTest {

    @Mock
    private lateinit var applicationEventPublisher: ApplicationEventPublisher

    @InjectMocks
    private lateinit var springDomainEventPublisher: SpringDomainEventPublisher

    @Test
    fun `publish 호출 시 ApplicationEventPublisher에 이벤트를 위임한다`() {
        // given
        val event = object : DomainEvent(
            aggregateType = AggregateType.RESERVATION,
            aggregateId = "1"
        ) {}

        // when
        springDomainEventPublisher.publish(event)

        // then
        verify(applicationEventPublisher).publishEvent(event)
    }

    @Test
    fun `publishAll 호출 시 모든 이벤트를 순차적으로 발행한다`() {
        // given
        val event1 = object : DomainEvent(
            aggregateType = AggregateType.RESERVATION,
            aggregateId = "1"
        ) {}
        val event2 = object : DomainEvent(
            aggregateType = AggregateType.RESERVATION,
            aggregateId = "2"
        ) {}

        // when
        springDomainEventPublisher.publishAll(listOf(event1, event2))

        // then
        verify(applicationEventPublisher).publishEvent(event1)
        verify(applicationEventPublisher).publishEvent(event2)
    }
}
