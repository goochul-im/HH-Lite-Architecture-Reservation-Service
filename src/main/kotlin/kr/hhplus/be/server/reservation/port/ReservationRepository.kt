package kr.hhplus.be.server.reservation.port

import kr.hhplus.be.server.reservation.domain.Reservation

interface ReservationRepository {

    fun save(reservation: Reservation): Reservation

    fun getReservedSeatNumbers(concertId: Long): List<Int>

    fun findReservationByIdAndReserver(id: Long, reserverId: String): Reservation

}
