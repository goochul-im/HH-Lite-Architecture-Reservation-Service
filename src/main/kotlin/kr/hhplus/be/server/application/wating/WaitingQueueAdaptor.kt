package kr.hhplus.be.server.application.wating

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@Component
class WaitingQueueAdaptor(
    private val redisTemplate: RedisTemplate<String, Any>
) : WaitingQueuePort{

    private val QUEUE_PREFIX = "queue:"

    override fun getUsedNumberByDate(date: LocalDate) : List<Int> {
        TODO("Not yet implemented")
    }

    override fun saveNumber(userId :String, num: Int, date: LocalDate) {

        val key = QUEUE_PREFIX + date.toString() // "queue:2025-11-08"

        redisTemplate.opsForHash<String, UserEntry>().put( // {userId : "UUID-12345", num: <좌석 번호>}
            key,
            userId,
            UserEntry(
                userId,
                num
            )
        )

        redisTemplate.expire(key, 5, TimeUnit.MINUTES) // 5분으로 만료시간 간주



    }
}

data class UserEntry(
    val userId: String,
    val num: Int
)
