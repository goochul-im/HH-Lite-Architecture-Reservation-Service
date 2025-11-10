package kr.hhplus.be.server.application.wating

import kr.hhplus.be.server.auth.TokenResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.User
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
}
