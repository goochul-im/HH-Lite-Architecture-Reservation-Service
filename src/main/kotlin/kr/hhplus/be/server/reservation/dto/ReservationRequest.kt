package kr.hhplus.be.server.reservation.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "좌석 예약 정보")
data class ReservationRequest(
    @Schema(description = "예약 날짜", example = "2025-11-26")
    val date: LocalDate,
    @Schema(description = "회원 ID", example = "1")
    val memberId: String,
    @Schema(description = "좌석 번호", example = "12")
    val seatNumber: Int
)
