package kr.hhplus.be.server.member.service

import kr.hhplus.be.server.member.domain.Member
import kr.hhplus.be.server.member.port.MemberRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun signUp(username: String, password: String) {
        val member = Member(
            username = username,
            password = passwordEncoder.encode(password)
        )
        memberRepository.save(member)
    }

}
