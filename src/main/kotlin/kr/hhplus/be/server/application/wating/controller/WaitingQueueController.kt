package kr.hhplus.be.server.application.wating.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.application.wating.dto.WaitingRankResponse
import kr.hhplus.be.server.application.wating.dto.WaitingStatusResponse
import kr.hhplus.be.server.application.wating.service.WaitingQueueService
import kr.hhplus.be.server.auth.TokenResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "대기열", description = "콘서트 참여 대기열 관련 API")
@RestController
@RequestMapping("/api/wait")
class WaitingQueueController(
    private val waitingQueueService: WaitingQueueService
) {
    @Operation(summary = "대기열 진입", description = "콘서트 참여를 위한 대기열에 진입하고 대기열 토큰을 발급받습니다.")
    @ApiResponse(responseCode = "200", description = "진입 성공 및 토큰 발급", content = [Content(schema = Schema(implementation = TokenResponse::class))])
    @PostMapping("/enter")
    fun enter(@AuthenticationPrincipal user: User): ResponseEntity<TokenResponse> {
        val waitingToken = waitingQueueService.enter(user.username) // user.username holds the member ID (UUID)
        return ResponseEntity.ok(TokenResponse(waitingToken = waitingToken))
    }

    @Operation(summary = "대기열 순번 조회", description = "대기열에서의 자신의 현재 순번을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공", content = [Content(schema = Schema(implementation = WaitingRankResponse::class))])
    @GetMapping("/myRank")
    fun getMyRank(@AuthenticationPrincipal user: User): ResponseEntity<WaitingRankResponse> = ResponseEntity.ok(
        WaitingRankResponse(rank = waitingQueueService.getMyRank(user.username))
    )

    @Operation(summary = "대기열 토큰 유효성 검사", description = "현재 발급된 대기열 토큰이 유효한지(활성 상태인지) 확인합니다.")
    @ApiResponse(responseCode = "200", description = "확인 성공", content = [Content(schema = Schema(implementation = WaitingStatusResponse::class))])
    @GetMapping("/check")
    fun check(@AuthenticationPrincipal user: User): ResponseEntity<WaitingStatusResponse> = ResponseEntity.ok(
        WaitingStatusResponse(valid = waitingQueueService.isValidWaitingToken(user.username))
    )
}
