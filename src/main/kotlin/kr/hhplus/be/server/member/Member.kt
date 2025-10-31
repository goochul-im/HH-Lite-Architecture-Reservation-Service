package kr.hhplus.be.server.member

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

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
) {
    fun chargePoint(amount: Int) {
        if (point + amount < 0) {
            TODO("예외처리")
        }

        this.point += amount
    }
}

