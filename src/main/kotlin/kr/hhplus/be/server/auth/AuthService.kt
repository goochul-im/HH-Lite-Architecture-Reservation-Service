package kr.hhplus.be.server.auth

import kr.hhplus.be.server.common.jwt.JwtProvider
import kr.hhplus.be.server.domain.member.MemberRepository
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val jwtProvider: JwtProvider,
    private val memberRepository: MemberRepository,
) {
    fun login(request: LoginRequest): String {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.username, request.password)
        )
        SecurityContextHolder.getContext().authentication = authentication

        val member = memberRepository.findByUsername(request.username)
            ?: throw UsernameNotFoundException("User not found")

        return jwtProvider.createAccessToken(member.id!!)
    }
}
