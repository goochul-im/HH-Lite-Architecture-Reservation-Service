package kr.hhplus.be.server.reservation.infrastructure

import jakarta.persistence.Id
import kr.hhplus.be.server.member.infrastructure.MemberEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface ReservationJpaRepository : JpaRepository<ReservationEntity, Long> {

    @Query(
        "select r.seatNumber from ReservationEntity r " +
            "where " +
            "r.date = :date and " +
            "r.status = 'RESERVE'")
    fun getReservedSeatNumber(@Param("date") date: LocalDate) : List<Int>

    fun findReservationEntityByIdAndReserver_Id(
        id: Long,
        reserverId: String
    ): ReservationEntity?

}
