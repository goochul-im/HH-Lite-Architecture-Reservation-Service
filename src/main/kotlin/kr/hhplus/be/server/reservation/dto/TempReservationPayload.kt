package kr.hhplus.be.server.reservation.dto

data class TempReservationPayload(
    val id: Long,
    val concertId: Long,
    val seatNumber: Int
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "concertId" to concertId,
            "seatNumber" to seatNumber
        )
    }
}
