package kr.hhplus.be.server.reservation.event.listener

import kr.hhplus.be.server.outbox.domain.AggregateType
import kr.hhplus.be.server.outbox.domain.EventType
import kr.hhplus.be.server.outbox.domain.OutboxStatus
import kr.hhplus.be.server.outbox.port.OutboxRepository
import kr.hhplus.be.server.reservation.event.ReservationCreatedEvent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argThat
import org.mockito.kotlin.verify

@ExtendWith(MockitoExtension::class)
class ReservationOutboxEventListenerTest {

    @Mock
    private lateinit var outboxRepository: OutboxRepository

    @InjectMocks
    private lateinit var listener: ReservationOutboxEventListener

    @Test
    fun `ReservationCreatedEvent 수신 시 OutboxMessage를 저장한다`() {
        // given
        val event = ReservationCreatedEvent(
            reservationId = 1L,
            concertId = 10L,
            seatNumber = 5,
            memberId = "member1"
        )

        // when
        listener.reservationCreatedEvent(event)

        // then
        verify(outboxRepository).save(argThat {
            aggregateType == AggregateType.TEMP_RESERVATION &&
                eventType == EventType.INSERT &&
                status == OutboxStatus.PENDING &&
                payload["seatNumber"] == 5 &&
                payload["concertId"] == 10L &&
                payload["id"] == 1L
        })
    }
}
