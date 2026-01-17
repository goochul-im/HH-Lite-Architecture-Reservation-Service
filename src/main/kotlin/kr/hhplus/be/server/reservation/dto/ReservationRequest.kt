package kr.hhplus.be.server.reservation.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "좌석 예약 정보")
data class ReservationRequest(
    @Schema(description = "콘서트 ID", example = "1")
    val concertId: Long,
    @Schema(description = "회원 ID", example = "uuid-1234")
    val memberId: String,
    @Schema(description = "좌석 번호", example = "12")
    val seatNumber: Int
)
