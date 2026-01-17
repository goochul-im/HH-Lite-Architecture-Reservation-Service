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
import kr.hhplus.be.server.common.BaseEntity
import kr.hhplus.be.server.concert.infrastructure.ConcertEntity
import kr.hhplus.be.server.member.infrastructure.MemberEntity
import kr.hhplus.be.server.reservation.domain.Reservation

@Entity
@Table(name = "reservation")
class ReservationEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    val concert: ConcertEntity,

    @Column(name = "seat_num")
    var seatNumber: Int,

    @Column(name = "reservation_status")
    @Enumerated(EnumType.STRING)
    var status: ReservationStatus,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    var reserver: MemberEntity?
) : BaseEntity() {

    fun toDomain(): Reservation {
        return Reservation(
            this.id,
            this.concert.toDomain(),
            this.seatNumber,
            this.status,
            this.reserver?.toDomain()
        )
    }

    companion object {
        fun from(reservation: Reservation): ReservationEntity {
            return ReservationEntity(
                reservation.id,
                ConcertEntity.from(reservation.concert),
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
