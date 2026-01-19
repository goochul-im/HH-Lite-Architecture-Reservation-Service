package kr.hhplus.be.server.concert.service

import kr.hhplus.be.server.concert.port.ConcertRepository
import kr.hhplus.be.server.concert.port.ConcertSoldOutPort
import org.springframework.stereotype.Service

@Service
class ConcertRankingService(
    private val soldOutPort: ConcertSoldOutPort,
    private val concertRepository: ConcertRepository
) {

    fun checkAndMarkSoldOut(concertId: Long) {
        if (soldOutPort.isSoldOut(concertId)) {
            return
        }

        val concert = concertRepository.findById(concertId)
        soldOutPort.markSoldOut(concertId, System.currentTimeMillis())

    }
    //test
}
