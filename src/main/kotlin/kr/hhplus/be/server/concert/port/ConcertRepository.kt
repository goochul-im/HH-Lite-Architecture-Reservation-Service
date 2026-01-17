package kr.hhplus.be.server.concert.port

import kr.hhplus.be.server.concert.domain.Concert
import java.time.LocalDate

interface ConcertRepository {
    fun findById(id: Long): Concert
    fun findByDate(date: LocalDate): Concert?
    fun findAllAvailable(fromDate: LocalDate): List<Concert>
    fun save(concert: Concert): Concert
    fun existsById(id: Long): Boolean
}
