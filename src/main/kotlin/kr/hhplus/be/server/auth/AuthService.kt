package kr.hhplus.be.server.auth

import jakarta.persistence.EntityNotFoundException
import kr.hhplus.be.server.common.jwt.JwtProvider
import kr.hhplus.be.server.member.infrastructure.MemberEntity
import kr.hhplus.be.server.member.infrastructure.MemberJpaRepository
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val jwtProvider: JwtProvider,
    private val memberJpaRepository: MemberJpaRepository,
) {
    fun login(request: LoginRequest): String {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.username, request.password)
        )
        SecurityContextHolder.getContext().authentication = authentication

        val member = memberJpaRepository.findByUsername(request.username)
            ?: throw UsernameNotFoundException("User not found")

        return jwtProvider.createAccessToken(member.id!!)
    }

    fun getById(id: String): MemberEntity {
        return memberJpaRepository.findById(id).get() ?: throw EntityNotFoundException("ID ${id}에 해당하는 값을 찾을 수 없습니다")
    }
}
