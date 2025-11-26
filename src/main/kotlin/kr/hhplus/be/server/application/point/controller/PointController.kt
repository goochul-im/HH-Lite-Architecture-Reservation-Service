package kr.hhplus.be.server.application.point.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.application.point.dto.PointResponse
import kr.hhplus.be.server.application.point.service.PointService
import org.apache.catalina.User
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "포인트", description = "포인트 조회/충전 API")
@RestController
@RequestMapping("/api/point")
class PointController(
    private val pointService: PointService
) {

    @Operation(summary = "포인트 조회", description = "사용자의 현재 포인트를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공", content = [Content(schema = Schema(implementation = PointResponse::class))])
    @GetMapping
    fun inquiry(@AuthenticationPrincipal user: User): ResponseEntity<PointResponse> {
        return ResponseEntity.ok(
            PointResponse(
                point = pointService.inquiry(user.username)
            )
        )
    }

    @Operation(summary = "포인트 충전", description = "사용자의 포인트를 충전합니다.")
    @ApiResponse(responseCode = "200", description = "충전 성공", content = [Content(schema = Schema(implementation = PointResponse::class))])
    @PostMapping("/charge")
    fun charge(
        @AuthenticationPrincipal user: User,
        @RequestBody req: UsePointReq
    ) : ResponseEntity<PointResponse> {
        return ResponseEntity.ok(
            PointResponse(
                point = pointService.charge(user.username, req.point)
            )
        )
    }

}

@Schema(description = "포인트 충전 요청")
data class UsePointReq(
    @Schema(description = "충전할 포인트", example = "10000")
    val point: Int
)
