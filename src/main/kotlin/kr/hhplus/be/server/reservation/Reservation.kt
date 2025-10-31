package kr.hhplus.be.server.reservation

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import kr.hhplus.be.server.member.Member
import java.time.LocalDateTime

@Entity
class Reservation(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id : Long? = null,
    @Column(name = "reservation_date")
    var date: LocalDateTime,
    @Column(name = "seat_num")
    var seatNumber: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    var reserver: Member
)
