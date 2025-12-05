package kr.hhplus.be.server.member.service

import kr.hhplus.be.server.member.domain.Member
import kr.hhplus.be.server.member.port.EncodeService
import kr.hhplus.be.server.member.port.MemberRepository
import org.springframework.stereotype.Service

@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: EncodeService
) {

    fun signUp(username: String, password: String) : Member {
        val member = Member(
            username = username,
            password = passwordEncoder.encode(password)
        )
        return memberRepository.signUp(member)
    }

}
