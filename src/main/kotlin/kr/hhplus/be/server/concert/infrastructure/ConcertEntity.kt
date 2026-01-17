package kr.hhplus.be.server.concert.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.hhplus.be.server.common.BaseEntity
import kr.hhplus.be.server.concert.domain.Concert
import java.time.LocalDate

@Entity
@Table(name = "concert")
class ConcertEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    val name: String,
    @Column(nullable = false, name = "concert_date")
    val date: LocalDate,
    @Column(nullable = false, name = "total_seats")
    val totalSeats: Int = 50
) : BaseEntity() {

    fun toDomain(): Concert {
        return Concert(
            this.id,
            this.name,
            this.date,
            this.totalSeats
        )
    }

    companion object {
        fun from(concert: Concert): ConcertEntity {
            return ConcertEntity(
                concert.id,
                concert.name,
                concert.date,
                concert.totalSeats
            )
        }
    }
}
