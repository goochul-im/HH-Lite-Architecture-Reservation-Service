package kr.hhplus.be.server.outbox.scheduler

import kr.hhplus.be.server.outbox.domain.AggregateType
import kr.hhplus.be.server.outbox.domain.EventType
import kr.hhplus.be.server.outbox.domain.OutboxMessage
import kr.hhplus.be.server.outbox.domain.OutboxStatus
import kr.hhplus.be.server.outbox.port.OutboxHandler
import kr.hhplus.be.server.outbox.port.OutboxRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times

@ExtendWith(MockitoExtension::class)
class OutboxSchedulerTest {

    @Mock
    lateinit var outboxRepository: OutboxRepository

    @Mock
    lateinit var outboxHandler: OutboxHandler

    lateinit var outboxScheduler: OutboxScheduler

    @BeforeEach
    fun setUp() {
        outboxScheduler = OutboxScheduler(outboxRepository, listOf(outboxHandler))
    }

    @Test
    fun `스케줄링 실행시 대기중인 메시지가 없으면 로직이 실행되지 않는다`() {
        // given
        BDDMockito.given(outboxRepository.getPendingList()).willReturn(emptyList())

        // when
        outboxScheduler.schedule()

        // then
        Mockito.verify(outboxHandler, never()).canHandle(any())
        Mockito.verify(outboxHandler, never()).handle(any())
    }

    @Test
    fun `스케줄링 실행시 핸들러가 존재하면 메시지를 처리한다`() {
        // given
        val message = OutboxMessage(
            id = 1L,
            aggregateType = AggregateType.TEMP_RESERVATION,
            eventType = EventType.INSERT,
            payload = mapOf("key" to "value"),
            status = OutboxStatus.PENDING
        )
        BDDMockito.given(outboxRepository.getPendingList()).willReturn(listOf(message))
        BDDMockito.given(outboxHandler.canHandle(AggregateType.TEMP_RESERVATION)).willReturn(true)

        // when
        outboxScheduler.schedule()

        // then
        Mockito.verify(outboxHandler, times(1)).handle(message)
    }

    @Test
    fun `스케줄링 실행시 핸들러를 찾지 못하면 handle이 작동하지 않는다`() {
        // given
        val message = OutboxMessage(
            id = 1L,
            aggregateType = AggregateType.TEMP_RESERVATION,
            eventType = EventType.INSERT,
            payload = mapOf("key" to "value"),
            status = OutboxStatus.PENDING
        )
        BDDMockito.given(outboxRepository.getPendingList()).willReturn(listOf(message))
        BDDMockito.given(outboxHandler.canHandle(AggregateType.TEMP_RESERVATION)).willReturn(false)

        // when & then
        Assertions.assertDoesNotThrow {
            outboxScheduler.schedule()
        }

        Mockito.verify(outboxHandler, never()).handle(message)
    }

    @Test
    fun `스케줄링 실행시 메시지 처리중 예외가 발생하면 handle은 실행되지만 다음 스케줄로 넘어간다`() {
        // given
        val message = OutboxMessage(
            id = 1L,
            aggregateType = AggregateType.TEMP_RESERVATION,
            eventType = EventType.INSERT,
            payload = mapOf("key" to "value"),
            status = OutboxStatus.PENDING
        )
        BDDMockito.given(outboxRepository.getPendingList()).willReturn(listOf(message))
        BDDMockito.given(outboxHandler.canHandle(AggregateType.TEMP_RESERVATION)).willReturn(true)
        BDDMockito.given(outboxHandler.handle(message)).willThrow(RuntimeException("Processing failed"))

        // when & then
        Assertions.assertDoesNotThrow {
            outboxScheduler.schedule()
        }

        Mockito.verify(outboxHandler, times(1)).handle(message)
    }
}
