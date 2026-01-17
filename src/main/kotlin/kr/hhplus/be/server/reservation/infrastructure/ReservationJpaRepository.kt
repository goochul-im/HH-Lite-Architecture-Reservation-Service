package kr.hhplus.be.server.reservation.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReservationJpaRepository : JpaRepository<ReservationEntity, Long> {

    @Query(
        "select r.seatNumber from ReservationEntity r " +
            "where r.concert.id = :concertId and r.status = 'RESERVE'"
    )
    fun getReservedSeatNumbers(@Param("concertId") concertId: Long): List<Int>

    fun findReservationEntityByIdAndReserver_Id(
        id: Long,
        reserverId: String
    ): ReservationEntity?

}
