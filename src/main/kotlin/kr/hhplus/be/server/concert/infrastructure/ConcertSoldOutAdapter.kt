package kr.hhplus.be.server.concert.infrastructure

import kr.hhplus.be.server.concert.dto.ConcertRanking
import kr.hhplus.be.server.concert.port.ConcertSoldOutPort
import org.redisson.api.RScoredSortedSet
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component

@Component
class ConcertSoldOutAdapter(
    private val redissonClient: RedissonClient
) : ConcertSoldOutPort {

    companion object{
        const val RANKING_KEY = "concert:soldout:ranking"
    }

    private fun getRankingSet() =
        redissonClient.getScoredSortedSet<String>(RANKING_KEY)

    override fun markSoldOut(concertId: Long, timestamp: Long) {
        if (!isSoldOut(concertId)) {
            getRankingSet().add(timestamp.toDouble(), concertId.toString())
        }
    }

    override fun isSoldOut(concertId: Long): Boolean {
        return getRankingSet().contains(concertId.toString())
    }

    override fun getTopN(n: Int): List<ConcertRanking> {
        return getRankingSet().entryRange(0, n - 1).map {
            ConcertRanking(
                it.value.toLong(),
                it.score.toLong()
            )
        }
    }

    override fun getRank(concertId: Long): ConcertRanking {
        val rank = getRankingSet().rank(concertId.toString())?.toLong()
        return ConcertRanking(concertId, rank ?: 0)
    }

}
