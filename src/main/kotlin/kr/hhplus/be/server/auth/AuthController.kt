package kr.hhplus.be.server.auth

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<TokenResponse> {
        val accessToken = authService.login(request)
        return ResponseEntity.ok(TokenResponse(accessToken = accessToken))
    }

}

data class LoginRequest(
    val username: String,
    val password: String
)

data class TokenResponse(
    val accessToken: String? = null,
    val waitingToken: String? = null
)
