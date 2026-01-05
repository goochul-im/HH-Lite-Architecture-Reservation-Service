package kr.hhplus.be.server.reservation.service

import kr.hhplus.be.server.reservation.port.ReservationRepository
import kr.hhplus.be.server.reservation.port.SeatFinder
import kr.hhplus.be.server.reservation.port.TempReservationPort
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class SeatFinderImpl(
    private val reservationRepository: ReservationRepository,
    private val tempReservationService: TempReservationPort
) : SeatFinder {

    @Cacheable(value = ["availableSeats"], key = "#date.toString()")
    override fun getAvailableSeat(date: LocalDate): List<Int> {
        val seatInPersistent = reservationRepository.getReservedSeatNumber(date).toSet()
        val seatInTemp = tempReservationService.getTempReservation(date).toSet()

        val availableSeat = (1..50).filter { !seatInPersistent.contains(it) && !seatInTemp.contains(it) }

        return availableSeat
    }
}
