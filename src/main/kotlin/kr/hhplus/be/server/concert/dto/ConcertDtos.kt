package kr.hhplus.be.server.concert.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "콘서트 생성 요청")
data class ConcertCreateRequest(
    @Schema(description = "콘서트 이름", example = "아이유 콘서트")
    val name: String,
    @Schema(description = "콘서트 날짜", example = "2025-12-25")
    val date: LocalDate,
    @Schema(description = "총 좌석 수", example = "50")
    val totalSeats: Int = 50
)

@Schema(description = "콘서트 응답")
data class ConcertResponse(
    @Schema(description = "콘서트 ID", example = "1")
    val id: Long,
    @Schema(description = "콘서트 이름", example = "아이유 콘서트")
    val name: String,
    @Schema(description = "콘서트 날짜", example = "2025-12-25")
    val date: LocalDate,
    @Schema(description = "총 좌석 수", example = "50")
    val totalSeats: Int
)

@Schema(description = "콘서트 목록 응답")
data class ConcertListResponse(
    @Schema(description = "콘서트 목록")
    val concerts: List<ConcertResponse>
)
