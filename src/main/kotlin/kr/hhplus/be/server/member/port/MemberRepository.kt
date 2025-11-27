package kr.hhplus.be.server.member.port

import kr.hhplus.be.server.member.domain.Member

interface MemberRepository {

    fun findByUsername(username : String) : Member

    fun findById(id: String) : Member

    fun save(member: Member) : Member

}
