package kr.hhplus.be.server.auth.security

import kr.hhplus.be.server.member.infrastructure.MemberEntity
import kr.hhplus.be.server.member.infrastructure.MemberJpaRepository
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val memberJpaRepository: MemberJpaRepository
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val member = memberJpaRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("User not found with username: $username")
        return buildUserDetails(member)
    }

    fun loadUserById(id: String): UserDetails {
        val member = memberJpaRepository.findById(id).orElseThrow {
            UsernameNotFoundException("User not found with id: $id")
        }
        return buildUserDetails(member)
    }

    private fun buildUserDetails(memberEntity: MemberEntity): UserDetails {
        return User.builder()
            .username(memberEntity.id) // Spring Security's username is the unique ID
            .password(memberEntity.password)
            .authorities(emptyList()) // No specific roles for now
            .build()
    }
}
