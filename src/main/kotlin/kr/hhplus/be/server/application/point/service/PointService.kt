package kr.hhplus.be.server.application.point.service

import kr.hhplus.be.server.member.infrastructure.MemberJpaRepository
import kr.hhplus.be.server.member.port.MemberRepository
import org.springframework.stereotype.Service

@Service
class PointService(
    private val memberRepository: MemberRepository,
) {

    fun charge(id: String, chargePoint: Int) : Int {
        val member = memberRepository.findById(id)
        member.chargePoint(chargePoint)
        memberRepository.save(member)
        return member.point
    }

    fun inquiry(id: String): Int {
        val member = memberRepository.findById(id)
        return member.point
    }

}
