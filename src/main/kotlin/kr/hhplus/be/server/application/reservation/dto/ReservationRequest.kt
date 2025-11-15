package kr.hhplus.be.server.application.reservation.dto

import java.time.LocalDate

data class ReservationRequest(
    val date: LocalDate,
    val memberId: String,
    val seatNumber: Int
)
