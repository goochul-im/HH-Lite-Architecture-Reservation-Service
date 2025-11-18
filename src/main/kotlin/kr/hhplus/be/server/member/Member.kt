package kr.hhplus.be.server.member

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import kr.hhplus.be.server.common.BaseEntity

@Entity
class Member(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,
    @Column(nullable = false)
    var point: Int = 0,
    @Column
    val username: String,
    @Column
    var password:String
) : BaseEntity() {

    fun chargePoint(amount: Int) {
        this.point += amount
    }

    fun usePoint(amount: Int) {
        if (point < amount) {
            throw RuntimeException("잔액이 모자랍니다")
        }

        this.point -= amount
    }

}

