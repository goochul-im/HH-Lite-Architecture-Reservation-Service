package kr.hhplus.be.server.member.domain

/**
 * 순수 도메인 Member 객체
 */
class Member(
    val id: String? = null,
    var point: Int = 0,
    val username: String,
    var password:String
) {

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
