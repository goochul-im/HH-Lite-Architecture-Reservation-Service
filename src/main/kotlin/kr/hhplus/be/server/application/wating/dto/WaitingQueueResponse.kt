package kr.hhplus.be.server.application.wating.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "대기열 순번 응답")
data class WaitingRankResponse(
    @Schema(description = "나의 대기 순번")
    val rank: Long
)

@Schema(description = "대기열 토큰 유효성 응답")
data class WaitingStatusResponse(
    @Schema(description = "토큰 유효 여부")
    val valid: Boolean
)
