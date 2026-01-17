package kr.hhplus.be.server.reservation.port

interface SeatFinder {

    fun getAvailableSeats(concertId: Long): List<Int>

}
