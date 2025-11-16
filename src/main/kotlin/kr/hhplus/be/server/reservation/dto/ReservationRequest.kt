package kr.hhplus.be.server.reservation.dto

import java.time.LocalDate

data class ReservationRequest(
    val date: LocalDate,
    val memberId: String,
    val seatNumber: Int
)
