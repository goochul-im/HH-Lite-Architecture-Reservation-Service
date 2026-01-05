package kr.hhplus.be.server.reservation.service

import kr.hhplus.be.server.reservation.port.ReservationRepository
import kr.hhplus.be.server.reservation.port.SeatFinder
import kr.hhplus.be.server.reservation.port.TempReservationPort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.given
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class SeatFinderTest {

    @Mock
    lateinit var reservationRepository: ReservationRepository

    @Mock
    lateinit var tempReservationService: TempReservationPort

    @InjectMocks
    lateinit var seatFinder: SeatFinderImpl

    @Test
    fun `선택한 날짜의 예약 가능한 자리를 가져올 수 있다`() {
        //given
        val date = LocalDate.now()
        given(reservationRepository.getReservedSeatNumber(date)).willReturn(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9))
        given(tempReservationService.getTempReservation(date)).willReturn(
            listOf(
                41,
                42,
                43,
                44,
                45,
                46,
                47,
                48,
                49,
                50
            )
        )

        //when
        val result = seatFinder.getAvailableSeat(date)

        //then
        val expect = (10..40).toList()
        assertThat(result).containsAll(expect)
    }

}
