package kr.hhplus.be.server.application.point.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "포인트 응답")
data class PointResponse(
    @Schema(description = "현재 포인트")
    val point: Int
)
