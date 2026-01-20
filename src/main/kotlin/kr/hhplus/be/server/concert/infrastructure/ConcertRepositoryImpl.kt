package kr.hhplus.be.server.concert.infrastructure

import kr.hhplus.be.server.concert.domain.Concert
import kr.hhplus.be.server.concert.port.ConcertRepository
import kr.hhplus.be.server.exception.ResourceNotFoundException
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class ConcertRepositoryImpl(
    private val concertJpaRepository: ConcertJpaRepository
) : ConcertRepository {

    override fun findById(id: Long): Concert {
        return concertJpaRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Concert id : $id not found") }
            .toDomain()
    }

    override fun findByDate(date: LocalDate): Concert? {
        return concertJpaRepository.findByDate(date)?.toDomain()
    }

    override fun findAllAvailable(fromDate: LocalDate): List<Concert> {
        return concertJpaRepository.findAllByDateGreaterThan(fromDate)
            .map { it.toDomain() }
    }

    override fun save(concert: Concert): Concert {
        val entity = ConcertEntity.from(concert)
        return concertJpaRepository.save(entity).toDomain()
    }

    override fun existsById(id: Long): Boolean {
        return concertJpaRepository.existsById(id)
    }
}
