package kr.hhplus.be.server.concert.service

import kr.hhplus.be.server.concert.dto.ConcertRanking
import kr.hhplus.be.server.concert.dto.ConcertRankingResponse
import kr.hhplus.be.server.concert.port.ConcertRankingPort
import kr.hhplus.be.server.concert.port.ConcertRepository
import kr.hhplus.be.server.concert.port.ConcertSoldOutPort
import kr.hhplus.be.server.reservation.port.ReservationRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class ConcertRankingService(
    private val soldOutPort: ConcertSoldOutPort,
    private val concertRepository: ConcertRepository,
    private val reservationRepository: ReservationRepository
) : ConcertRankingPort {

    override fun checkAndMarkSoldOut(concertId: Long) {
        if (soldOutPort.isSoldOut(concertId)) {
            return
        }

        val concert = concertRepository.findById(concertId)
        val reservationCount = reservationRepository.countByConcert(concert)
        if (reservationCount <= concert.totalSeats) return

        soldOutPort.markSoldOut(concertId, System.currentTimeMillis())
    }

    override fun getRanking(topN: Int): List<ConcertRankingResponse> {
        return soldOutPort.getTopN(topN).mapIndexed { index, rank ->
            val concert = concertRepository.findById(rank.concertId)
            ConcertRankingResponse(
                concertId = concert.id!!,
                concertName = concert.name,
                rank = (index + 1).toLong()
            )
        }
    }
}
