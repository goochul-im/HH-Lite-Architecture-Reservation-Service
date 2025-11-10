package kr.hhplus.be.server.member

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository : JpaRepository<Member, String> {
    fun findByUsername(username: String): Member?
}
