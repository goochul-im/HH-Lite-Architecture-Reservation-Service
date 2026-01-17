package kr.hhplus.be.server.reservation.service

import kr.hhplus.be.server.common.port.DistributeLockManager
import kr.hhplus.be.server.concert.domain.Concert
import kr.hhplus.be.server.reservation.domain.Reservation
import kr.hhplus.be.server.reservation.dto.ReservationRequest
import kr.hhplus.be.server.reservation.infrastructure.ReservationStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ReservationFacadeTest {

    @Mock
    lateinit var reservationService: ReservationService

    @Mock
    lateinit var lockManager: DistributeLockManager

    @InjectMocks
    lateinit var reservationFacade: ReservationFacade

    @Test
    fun `분산 락을 이용하여 예약을 생성한다`() {
        // given
        val concertId = 1L
        val dto = ReservationRequest(
            concertId = concertId,
            memberId = "1",
            seatNumber = 10
        )
        val concert = Concert(
            id = concertId,
            name = "테스트 콘서트",
            date = LocalDate.of(2025, 1, 1)
        )
        val reservation = Reservation(
            id = 1L,
            concert = concert,
            seatNumber = 10,
            status = ReservationStatus.PENDING,
            reserver = null
        )
        val lockKey = "LOCK:RESERVATION:${dto.concertId}:${dto.seatNumber}"

        // Mock lockManager.runWithLock to execute the task passed to it
        given(lockManager.runWithLock<Reservation>(eq(lockKey), any())).willAnswer { invocation ->
            val task = invocation.getArgument<io.jsonwebtoken.lang.Supplier<Reservation>>(1)
            task.get()
        }
        given(reservationService.make(dto)).willReturn(reservation)

        // when
        val result = reservationFacade.makeWithLock(dto)

        // then
        assertThat(result).isEqualTo(reservation)
        verify(lockManager).runWithLock<Reservation>(eq(lockKey), any())
        verify(reservationService).make(dto)
    }

    @Test
    fun `예약 생성 중 예외가 발생하면 예외가 전파된다`() {
        // given
        val concertId = 1L
        val dto = ReservationRequest(
            concertId = concertId,
            memberId = "1",
            seatNumber = 10
        )
        val lockKey = "LOCK:RESERVATION:${dto.concertId}:${dto.seatNumber}"

        given(lockManager.runWithLock<Reservation>(eq(lockKey), any())).willAnswer { invocation ->
            val task = invocation.getArgument<io.jsonwebtoken.lang.Supplier<Reservation>>(1)
            task.get()
        }
        given(reservationService.make(dto)).willThrow(RuntimeException("예약 실패"))

        // when & then
        org.junit.jupiter.api.assertThrows<RuntimeException> {
            reservationFacade.makeWithLock(dto)
        }
        verify(lockManager).runWithLock<Reservation>(eq(lockKey), any())
        verify(reservationService).make(dto)
    }
}
