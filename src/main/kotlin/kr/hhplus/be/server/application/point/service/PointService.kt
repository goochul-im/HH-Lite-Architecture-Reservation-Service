package kr.hhplus.be.server.application.point.service

import jakarta.transaction.Transactional
import kr.hhplus.be.server.application.point.event.PointChargedEvent
import kr.hhplus.be.server.common.event.DomainEventPublisher
import kr.hhplus.be.server.member.port.MemberRepository
import org.springframework.stereotype.Service

@Service
class PointService(
    private val memberRepository: MemberRepository,
    private val eventPublisher: DomainEventPublisher
) {

    @Transactional
    fun charge(id: String, chargePoint: Int) : Int {
        val member = memberRepository.findById(id)
        member.chargePoint(chargePoint)
        memberRepository.save(member)
        eventPublisher.publish(PointChargedEvent(
            member.id!!,
            chargePoint,
            member.point,
        ))
        return member.point
    }

    fun inquiry(id: String): Int {
        val member = memberRepository.findById(id)
        return member.point
    }

}
