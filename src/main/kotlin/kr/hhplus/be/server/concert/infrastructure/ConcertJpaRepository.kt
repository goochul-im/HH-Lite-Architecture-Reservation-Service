package kr.hhplus.be.server.concert.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ConcertJpaRepository : JpaRepository<ConcertEntity, Long> {
    fun findByDate(date: LocalDate): ConcertEntity?
    fun findAllByDateGreaterThan(date: LocalDate): List<ConcertEntity>
}
