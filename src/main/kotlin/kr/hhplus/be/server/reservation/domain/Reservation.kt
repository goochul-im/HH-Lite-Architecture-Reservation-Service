package kr.hhplus.be.server.reservation.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import kr.hhplus.be.server.common.BaseEntity
import kr.hhplus.be.server.member.infrastructure.MemberEntity
import java.time.LocalDate

@Entity
class Reservation(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "reservation_date")
    var date: LocalDate,
    @Column(name = "seat_num")
    var seatNumber: Int,
    @Column(name = "reservation_status")
    @Enumerated(EnumType.STRING)
    var status: ReservationStatus,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    var reserver: MemberEntity?
) : BaseEntity()


enum class ReservationStatus{

    PENDING, RESERVE, CANCEL

}
