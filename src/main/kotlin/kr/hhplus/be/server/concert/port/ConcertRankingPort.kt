package kr.hhplus.be.server.concert.port

import kr.hhplus.be.server.concert.dto.ConcertRanking
import kr.hhplus.be.server.concert.dto.ConcertRankingResponse

interface ConcertRankingPort {

    fun checkAndMarkSoldOut(concertId: Long)

    fun getRanking(topN: Int): List<ConcertRankingResponse>

}
