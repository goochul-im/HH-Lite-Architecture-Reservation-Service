package kr.hhplus.be.server.domain.reservation

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface ReservationRepository : JpaRepository<Reservation, Long> {

    @Query("select r.seatNumber from Reservation r " +
            "where " +
            "r.date = :date and " +
            "r.status = 'RESERVE'")
    fun getReservedSeatnumber(@Param("date") date: LocalDate) : List<Int>

}
