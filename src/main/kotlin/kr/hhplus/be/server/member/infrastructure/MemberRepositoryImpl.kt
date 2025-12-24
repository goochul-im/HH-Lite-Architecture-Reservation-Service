package kr.hhplus.be.server.member.infrastructure

import kr.hhplus.be.server.exception.DuplicateResourceException
import kr.hhplus.be.server.exception.ResourceNotFoundException
import kr.hhplus.be.server.member.domain.Member
import kr.hhplus.be.server.member.port.MemberRepository
import org.springframework.stereotype.Repository

@Repository
class MemberRepositoryImpl(
    val memberJpaRepository: MemberJpaRepository
) : MemberRepository{

    override fun findByUsername(username: String): Member {
        return (memberJpaRepository.findByUsername(username)?.toDomain()
            ?: throw ResourceNotFoundException("Member username : $username not found")
        )
    }

    override fun findById(id: String): Member {
        val memberJpaEntity = memberJpaRepository.findById(id).orElseThrow {
            ResourceNotFoundException("Member id : $id not found")
        }
        return memberJpaEntity.toDomain()
    }

    override fun signUp(member: Member): Member {
        if (memberJpaRepository.countByUsername(member.username) > 0) {
            throw DuplicateResourceException("Member username : ${member.username} is duplicate")
        }

        val entity = MemberEntity.from(member)
        return memberJpaRepository.save(entity).toDomain()
    }

    override fun save(member: Member): Member {
        val entity = MemberEntity.from(member)
        return memberJpaRepository.save(entity).toDomain()
    }

    override fun saveAndFlush(member: Member): Member {
        val entity = MemberEntity.from(member)
        val domain = memberJpaRepository.save(entity).toDomain()
        memberJpaRepository.flush()
        return domain
    }
}
