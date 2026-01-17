package kr.hhplus.be.server.reservation.service

import kr.hhplus.be.server.concert.port.ConcertRepository
import kr.hhplus.be.server.reservation.port.ReservationRepository
import kr.hhplus.be.server.reservation.port.SeatFinder
import kr.hhplus.be.server.reservation.port.TempReservationPort
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class SeatFinderImpl(
    private val reservationRepository: ReservationRepository,
    private val tempReservationService: TempReservationPort,
    private val concertRepository: ConcertRepository
) : SeatFinder {

    @Cacheable(value = ["availableSeats"], key = "#concertId")
    override fun getAvailableSeats(concertId: Long): List<Int> {
        val concert = concertRepository.findById(concertId)
        val totalSeats = concert.totalSeats

        val seatInPersistent = reservationRepository.getReservedSeatNumbers(concertId).toSet()
        val seatInTemp = tempReservationService.getTempReservation(concertId).toSet()

        return (1..totalSeats).filter { !seatInPersistent.contains(it) && !seatInTemp.contains(it) }
    }
}
