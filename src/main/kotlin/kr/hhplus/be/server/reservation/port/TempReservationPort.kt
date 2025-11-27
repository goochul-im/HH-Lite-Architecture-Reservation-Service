package kr.hhplus.be.server.reservation.port

import java.time.LocalDate

interface TempReservationPort {

    fun save(date: LocalDate, reservationId: Long, seatNumber: Int)

    fun cleanupExpiredReservation(reservationId: Long)

    fun getTempReservation(date: LocalDate): List<Int>

    fun delete(reservationId: Long)

    fun isValidReservation(reservationId: Long) : Boolean

}
