package kr.hhplus.be.server.outbox.handler

import kr.hhplus.be.server.common.port.TimeUtil
import kr.hhplus.be.server.outbox.domain.AggregateType
import kr.hhplus.be.server.outbox.domain.EventType
import kr.hhplus.be.server.outbox.domain.OutboxMessage
import kr.hhplus.be.server.outbox.domain.OutboxStatus
import kr.hhplus.be.server.outbox.exception.OutboxException
import kr.hhplus.be.server.outbox.port.OutboxRepository
import kr.hhplus.be.server.reservation.port.TempReservationPort
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class TempReservationOutboxHandlerImplTest {

    @Mock
    lateinit var outboxRepository: OutboxRepository

    @Mock
    lateinit var tempReservationService: TempReservationPort

    @Mock
    lateinit var timeUtil: TimeUtil

    @InjectMocks
    lateinit var handler: TempReservationOutboxHandlerImpl

    @Test
    fun `handle을 통해 OutboxMessage를 저장하고 상태를 DONE으로 변경할 수 있다`() {
        //given
        val concertId = 1L
        val reservationId = 1L
        val seatNumber = 10

        val outboxMessage = OutboxMessage(
            1L,
            AggregateType.TEMP_RESERVATION,
            EventType.INSERT,
            mapOf(
                "concertId" to concertId,
                "id" to reservationId,
                "seatNumber" to seatNumber
            ),
            OutboxStatus.PENDING
        )

        doNothing().whenever(tempReservationService).save(eq(concertId), eq(reservationId), eq(seatNumber))
        given(timeUtil.nowDateTime()).willReturn(LocalDateTime.of(2025, 1, 1, 0, 0))
        given(outboxRepository.save(any<OutboxMessage>())).willReturn(
            OutboxMessage(
                1L,
                AggregateType.TEMP_RESERVATION,
                EventType.INSERT,
                mapOf(
                    "concertId" to concertId,
                    "id" to reservationId,
                    "seatNumber" to seatNumber
                ),
                OutboxStatus.DONE
            )
        )

        //when
        val result = handler.handle(outboxMessage)

        //then
        assertThat(result.id).isEqualTo(1L)
        assertThat(result.aggregateType).isEqualTo(AggregateType.TEMP_RESERVATION)
        assertThat(result.eventType).isEqualTo(EventType.INSERT)
        assertThat(result.payload)
            .containsEntry("concertId", concertId)
            .containsEntry("id", reservationId)
            .containsEntry("seatNumber", seatNumber)
        assertThat(result.status).isEqualTo(OutboxStatus.DONE)
    }

    @Test
    fun `handle을 사용할 때 Redis에서 에러가 발생하면 OutboxException을 던진다`() {
        //given
        val concertId = 1L
        val reservationId = 1L
        val seatNumber = 10

        val outboxMessage = OutboxMessage(
            1L,
            AggregateType.TEMP_RESERVATION,
            EventType.INSERT,
            mapOf(
                "concertId" to concertId,
                "id" to reservationId,
                "seatNumber" to seatNumber
            ),
            OutboxStatus.PENDING
        )

        doThrow(RuntimeException("throw exception")).whenever(tempReservationService).save(eq(concertId), eq(reservationId), eq(seatNumber))

        //when & then
        assertThatThrownBy { handler.handle(outboxMessage) }.isInstanceOf(OutboxException::class.java)
    }
}
