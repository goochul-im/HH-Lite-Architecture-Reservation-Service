package kr.hhplus.be.server.reservation.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "좌석 예약 응답")
data class ReservationResponse(
    @Schema(description = "예약 id", example = "1")
    val id: Long,
    @Schema(description = "콘서트 ID", example = "1")
    val concertId: Long,
    @Schema(description = "콘서트 이름", example = "아이유 콘서트")
    val concertName: String,
    @Schema(description = "콘서트 날짜", example = "2025-12-25")
    val concertDate: LocalDate,
    @Schema(description = "좌석 번호", example = "12")
    val seatNumber: Int
)

@Schema(description = "예약 가능 좌석 목록 응답")
data class AvailableSeatsResponse(
    @Schema(description = "콘서트 ID", example = "1")
    val concertId: Long,
    @Schema(description = "예약 가능한 좌석 목록")
    val seats: List<Int>
)

@Schema(description = "임시 예약 결제 응답")
data class PayReservationResponse(
    @Schema(description = "콘서트 ID", example = "1")
    val concertId: Long,
    @Schema(description = "콘서트 이름", example = "아이유 콘서트")
    val concertName: String,
    @Schema(description = "콘서트 날짜", example = "2025-12-25")
    val concertDate: LocalDate,
    @Schema(description = "좌석 번호", example = "12")
    val seatNumber: Int
)
