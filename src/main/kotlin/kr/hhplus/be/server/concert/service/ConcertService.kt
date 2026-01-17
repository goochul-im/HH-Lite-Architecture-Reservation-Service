package kr.hhplus.be.server.concert.service

import kr.hhplus.be.server.concert.domain.Concert
import kr.hhplus.be.server.concert.dto.ConcertCreateRequest
import kr.hhplus.be.server.concert.port.ConcertRepository
import kr.hhplus.be.server.exception.DuplicateResourceException
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class ConcertService(
    private val concertRepository: ConcertRepository
) {

    @Transactional
    fun create(request: ConcertCreateRequest): Concert {
        if (concertRepository.findByDate(request.date) != null) {
            throw DuplicateResourceException("해당 날짜에 이미 콘서트가 존재합니다: ${request.date}")
        }

        val concert = Concert(
            name = request.name,
            date = request.date,
            totalSeats = request.totalSeats
        )
        return concertRepository.save(concert)
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): Concert {
        return concertRepository.findById(id)
    }

    @Transactional(readOnly = true)
    @Cacheable(value = ["concerts"], key = "'available'")
    fun findAvailableConcerts(): List<Concert> {
        return concertRepository.findAllAvailable(LocalDate.now())
    }
}
