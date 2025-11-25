package kr.hhplus.be.server.auth

import kr.hhplus.be.server.common.jwt.JwtProvider
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val jwtProvider: JwtProvider
) {

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<TokenResponse> {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.username, request.password)
        )
        val token = jwtProvider.createAccessToken(authentication.name)
        return ResponseEntity.ok(TokenResponse(accessToken = token))
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
