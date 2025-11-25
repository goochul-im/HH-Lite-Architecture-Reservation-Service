package kr.hhplus.be.server.member.infrastructure

import kr.hhplus.be.server.member.domain.Member
import kr.hhplus.be.server.member.port.MemberRepository

class MemberRepositoryImpl(
    val memberJpaRepository: MemberJpaRepository
) : MemberRepository{

    override fun findByUsername(username: String): Member {
        return toDomain(memberJpaRepository.findByUsername(username)
            ?: throw RuntimeException("TODO"))
    }

    override fun findById(id: String): Member {
        return toDomain(memberJpaRepository.findById(id)
            ?: throw RuntimeException("TODO"))
    }

    private fun toDomain(memberEntity: MemberEntity): Member {
        return Member(
            memberEntity.id,
            memberEntity.point,
            memberEntity.username,
            memberEntity.password,
        )
    }
}
