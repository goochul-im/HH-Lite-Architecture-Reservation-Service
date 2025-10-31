package kr.hhplus.be.server.auth

import jakarta.servlet.http.HttpSession
import kr.hhplus.be.server.AuthConstant
import kr.hhplus.be.server.common.annotation.LoginCheck
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {

    @LoginCheck
    @GetMapping
    fun test(session: HttpSession): String {
        return session.getAttribute(AuthConstant.userToken).toString()
    }

    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
        session: HttpSession
    ): String {
        session.setAttribute(AuthConstant.userToken, "test")
        return "session saved ok"
    }

}

data class LoginRequest(
    val username: String,
    val password: String
)
