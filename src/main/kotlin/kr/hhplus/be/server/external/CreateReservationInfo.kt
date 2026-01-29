package kr.hhplus.be.server.external

data class CreateReservationInfo(
    val reservationId: Long,
    val concertId: Long,
    val reserverId: String
)
