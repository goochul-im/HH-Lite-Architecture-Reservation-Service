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
        val memberJpaEntity = memberJpaRepository.findById(id).orElseThrow {
            RuntimeException("Member with id $id not found")
        }
        return toDomain(memberJpaEntity)
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
