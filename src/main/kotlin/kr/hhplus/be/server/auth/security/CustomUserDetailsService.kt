package kr.hhplus.be.server.auth.security

import kr.hhplus.be.server.domain.member.Member
import kr.hhplus.be.server.domain.member.MemberRepository
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val memberRepository: MemberRepository
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val member = memberRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("User not found with username: $username")
        return buildUserDetails(member)
    }

    fun loadUserById(id: String): UserDetails {
        val member = memberRepository.findById(id).orElseThrow {
            UsernameNotFoundException("User not found with id: $id")
        }
        return buildUserDetails(member)
    }

    private fun buildUserDetails(member: Member): UserDetails {
        return User.builder()
            .username(member.id) // Spring Security's username is the unique ID
            .password(member.password)
            .authorities(emptyList()) // No specific roles for now
            .build()
    }
}
