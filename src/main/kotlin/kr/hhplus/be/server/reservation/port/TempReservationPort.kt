package kr.hhplus.be.server.reservation.port

interface TempReservationPort {

    fun save(concertId: Long, reservationId: Long, seatNumber: Int)

    fun cleanupExpiredReservation(reservationId: Long)

    fun getTempReservation(concertId: Long): List<Int>

    fun delete(reservationId: Long)

    fun isValidReservation(reservationId: Long): Boolean

}
