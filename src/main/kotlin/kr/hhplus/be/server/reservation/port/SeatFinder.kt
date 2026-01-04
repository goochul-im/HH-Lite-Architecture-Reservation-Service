package kr.hhplus.be.server.reservation.port

import java.time.LocalDate

interface SeatFinder {

    fun getAvailableSeat(date: LocalDate) : List<Int>

}
