package kr.hhplus.be.server.reservation.port

import kr.hhplus.be.server.member.domain.Member
import kr.hhplus.be.server.reservation.domain.Reservation
import java.time.LocalDate

interface ReservationRepository {

    fun save(reservation: Reservation) : Reservation
    fun getReservedSeatNumber(date: LocalDate) : List<Int>

    fun findReservationByIdAndReserver(id:Long, reserverId: String) : Reservation

}
