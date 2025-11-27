package kr.hhplus.be.server.auth

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hhplus.be.server.common.jwt.JwtProvider
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "인증", description = "로그인 및 토큰 발급 API")
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val jwtProvider: JwtProvider
) {

    @Operation(summary = "로그인", description = "사용자 이름과 비밀번호로 로그인하고 액세스 토큰을 발급받습니다.")
    @ApiResponse(responseCode = "200", description = "로그인 성공", content = [Content(schema = Schema(implementation = TokenResponse::class))])
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<TokenResponse> {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.username, request.password)
        )
        val token = jwtProvider.createAccessToken(authentication.name)
        return ResponseEntity.ok(TokenResponse(accessToken = token))
    }

}

@Schema(description = "로그인 요청")
data class LoginRequest(
    @Schema(description = "사용자 아이디", example = "1")
    val username: String,
    @Schema(description = "비밀번호", example = "password")
    val password: String
)

@Schema(description = "토큰 응답")
data class TokenResponse(
    @Schema(description = "액세스 토큰")
    val accessToken: String? = null,
    @Schema(description = "대기열 토큰")
    val waitingToken: String? = null
)
