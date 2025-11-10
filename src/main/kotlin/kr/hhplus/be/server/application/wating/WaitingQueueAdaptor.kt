package kr.hhplus.be.server.application.wating

import kr.hhplus.be.server.application.wating.WaitingQueueConstant.CLEANUP_PREFIX
import kr.hhplus.be.server.application.wating.WaitingQueueConstant.QUEUE_PREFIX
import kr.hhplus.be.server.application.wating.WaitingQueueConstant.ZSET_WAIT_KEY
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@Component
class WaitingQueueAdaptor(
    private val redisTemplate: RedisTemplate<String, Any>,
    @param:Value("\${waiting.expiration_time}")
    private val waitingTime: Long
) : WaitingQueuePort{


    override fun getUsedNumberByDate(date: LocalDate) : List<Int> {
        TODO("Not yet implemented")
    }

    override fun saveNumber(userId :String, num: Int, date: LocalDate) {

        val entry = UserEntry(userId, num)
        val dateKey = date.toString()
        val key = QUEUE_PREFIX + dateKey // "queue:2025-11-08"
        val cleanupKey = CLEANUP_PREFIX + dateKey

        redisTemplate.opsForHash<String, UserEntry>().put( // {userId : "UUID-12345", num: <좌석 번호>}
            key,
            userId,
            entry
        )

        val timestamp = System.currentTimeMillis().toDouble()
        redisTemplate.opsForZSet().add(ZSET_WAIT_KEY, userId, timestamp) // 대기열 수 확인용

        redisTemplate.opsForSet().add(cleanupKey, userId) //

        redisTemplate.expire(key, waitingTime, TimeUnit.MINUTES) // 만료시간 5분
        redisTemplate.expire(cleanupKey, waitingTime + 1, TimeUnit.MINUTES)

    }
}


data class UserEntry(
    val userId: String = "",
    val num: Int = 0
)
