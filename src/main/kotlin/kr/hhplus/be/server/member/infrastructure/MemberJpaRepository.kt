package kr.hhplus.be.server.member.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberJpaRepository : JpaRepository<MemberEntity, String> {
    fun findByUsername(username: String): MemberEntity?
    fun findById(id: String) : MemberEntity?
}
