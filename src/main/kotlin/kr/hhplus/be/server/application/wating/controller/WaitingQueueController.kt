package kr.hhplus.be.server.application.wating.controller

import kr.hhplus.be.server.application.wating.service.WaitingQueueService
import kr.hhplus.be.server.auth.TokenResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/wait")
class WaitingQueueController(
    private val waitingQueueService: WaitingQueueService
) {
    @PostMapping("/enter")
    fun enter(@AuthenticationPrincipal user: User): ResponseEntity<TokenResponse> {
        val waitingToken = waitingQueueService.enter(user.username) // user.username holds the member ID (UUID)
        return ResponseEntity.ok(TokenResponse(waitingToken = waitingToken))
    }

    @GetMapping("/myRank")
    fun getMyRank(@AuthenticationPrincipal user: User): ResponseEntity<Long> = ResponseEntity.ok(
        waitingQueueService.getMyRank(user.username)
    )

    @GetMapping("/check")
    fun check(@AuthenticationPrincipal user: User): ResponseEntity<*> = ResponseEntity.ok(
        waitingQueueService.isValidWaitingToken(user.username)
    )
}
