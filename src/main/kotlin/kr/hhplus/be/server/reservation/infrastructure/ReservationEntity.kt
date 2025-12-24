package kr.hhplus.be.server.reservation.infrastructure

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
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.hhplus.be.server.common.BaseEntity
import kr.hhplus.be.server.member.domain.Member
import kr.hhplus.be.server.member.infrastructure.MemberEntity
import kr.hhplus.be.server.reservation.domain.Reservation
import java.time.LocalDate

@Entity
@Table(
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_reservation_date_seat",
            columnNames = ["reservation_date", "seat_num"]
        )
    ]
)
class ReservationEntity(

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
) : BaseEntity() {

    fun toDomain() : Reservation {
        return Reservation(
            this.id,
            this.date,
            this.seatNumber,
            this.status,
            this.reserver?.toDomain()
        )
    }

    companion object{

        fun from(reservation: Reservation) : ReservationEntity{
            return ReservationEntity(
                reservation.id,
                reservation.date,
                reservation.seatNumber,
                reservation.status,
                reservation.reserver?.let { MemberEntity.from(it) }
            )
        }

    }

}


enum class ReservationStatus{

    PENDING, RESERVE, CANCEL

}
