package kr.hhplus.be.server.member.infrastructure

import kr.hhplus.be.server.exception.ResourceNotFoundException
import kr.hhplus.be.server.member.domain.Member
import kr.hhplus.be.server.member.port.MemberRepository

class MemberRepositoryImpl(
    val memberJpaRepository: MemberJpaRepository
) : MemberRepository{

    override fun findByUsername(username: String): Member {
        return toDomain(memberJpaRepository.findByUsername(username)
            ?: throw ResourceNotFoundException("Member username : $username not found")
        )
    }

    override fun findById(id: String): Member {
        val memberJpaEntity = memberJpaRepository.findById(id).orElseThrow {
            ResourceNotFoundException("Member id : $id not found")
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
