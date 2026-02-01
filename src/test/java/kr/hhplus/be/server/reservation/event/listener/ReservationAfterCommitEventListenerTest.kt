package kr.hhplus.be.server.reservation.event.listener

import kr.hhplus.be.server.concert.port.ConcertRankingPort
import kr.hhplus.be.server.external.DataFlatformPort
import kr.hhplus.be.server.reservation.event.ReservationCreatedEvent
import kr.hhplus.be.server.reservation.event.ReservationExpiredEvent
import kr.hhplus.be.server.reservation.event.ReservationPaidEvent
import kr.hhplus.be.server.reservation.port.TempReservationPort
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager

@ExtendWith(MockitoExtension::class)
class ReservationAfterCommitEventListenerTest {

    @Mock
    private lateinit var concertRankingPort: ConcertRankingPort

    @Mock
    private lateinit var tempReservationPort: TempReservationPort

    @Mock
    private lateinit var cacheManager: CacheManager

    @Mock
    private lateinit var cache: Cache

    @Mock
    private lateinit var dataFlatformPort: DataFlatformPort

    @InjectMocks
    private lateinit var listener: ReservationAfterCommitEventListener

    @Test
    fun `ReservationCreatedEvent 수신 시 매진 체크를 수행한다`() {
        // given
        val event = ReservationCreatedEvent(
            reservationId = 1L,
            concertId = 10L,
            seatNumber = 5,
            memberId = "member1"
        )

        // when
        listener.handleReservationCreate(event)

        // then
        verify(concertRankingPort).checkAndMarkSoldOut(10L)
    }

    @Test
    fun `ReservationPaidEvent 수신 시 임시예약을 삭제한다`() {
        // given
        val event = ReservationPaidEvent(
            reservationId = 1L,
            concertId = 10L,
            memberId = "member1"
        )

        // when
        listener.handleReservationPaid(event)

        // then
        verify(tempReservationPort).delete(1L)
    }

    @Test
    fun `ReservationExpiredEvent 수신 시 캐시를 evict한다`() {
        // given
        val event = ReservationExpiredEvent(
            reservationId = 1L,
            concertId = 10L
        )
        org.mockito.kotlin.given(cacheManager.getCache("availableSeats")).willReturn(cache)

        // when
        listener.handleReservationExpired(event)

        // then
        verify(cacheManager).getCache("availableSeats")
        verify(cache).evict(10L)
    }
}
