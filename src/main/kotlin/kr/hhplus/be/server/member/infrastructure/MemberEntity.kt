package kr.hhplus.be.server.member.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import kr.hhplus.be.server.common.BaseEntity
import kr.hhplus.be.server.member.domain.Member

@Entity
@Table(name = "member")
class MemberEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,
    @Column(nullable = false)
    var point: Int = 0,
    @Column
    val username: String,
    @Column
    var password: String,
    @Version
    var version: Long = 0,
) : BaseEntity() {

    fun toDomain(): Member {
        return Member(
            this.id,
            this.point,
            this.username,
            this.password,
            this.version
        )
    }

    companion object{

        fun from(member: Member): MemberEntity {
            return MemberEntity(
                member.id,
                member.point,
                member.username,
                member.password,
                member.version
            )
        }

    }

}

