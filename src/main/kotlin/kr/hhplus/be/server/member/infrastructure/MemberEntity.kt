package kr.hhplus.be.server.member.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import kr.hhplus.be.server.common.BaseEntity
import kr.hhplus.be.server.member.domain.Member

@Entity
class MemberEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,
    @Column(nullable = false)
    var point: Int = 0,
    @Column
    val username: String,
    @Column
    var password: String
) : BaseEntity() {

    public fun toDomain(): Member {
        return Member(
            this.id,
            this.point,
            this.username,
            this.password,
        )
    }

    companion object{

        fun from(member: Member): MemberEntity {
            return MemberEntity(
                member.id,
                member.point,
                member.username,
                member.password
            )
        }

    }

}

