package kr.hhplus.be.server.application.wating.service

import kr.hhplus.be.server.application.wating.WaitingQueueConstant.ZSET_WAIT_KEY
import kr.hhplus.be.server.application.wating.WaitingQueueConstant.ENTER_LIST_KEY
import kr.hhplus.be.server.application.wating.port.WaitingQueuePort
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class WaitingQueueAdaptor(
    private val redisTemplate: StringRedisTemplate,
    @param:Value("\${waiting.expiration-time-second}")
    private val enteringTime: Long
) : WaitingQueuePort {

    override fun add(userId: String) {
        val score = System.currentTimeMillis().toDouble()
        redisTemplate.opsForZSet().add(ZSET_WAIT_KEY, userId, score)
    }

    override fun getMyRank(userId: String): Long? {
        return redisTemplate.opsForZSet().rank(ZSET_WAIT_KEY, userId)
    }

    override fun isEnteringKey(userId: String): Boolean {
        val key = "$ENTER_LIST_KEY$userId"
        return redisTemplate.hasKey(key)
    }

    override fun deleteToken(userId: String) {
        val key = "$ENTER_LIST_KEY$userId"
        redisTemplate.delete(key)
    }

    override fun renewalTokenTTL(userId: String) {
        val key = "$ENTER_LIST_KEY$userId"
        if (redisTemplate.hasKey(key)) {
            redisTemplate.expire(key, enteringTime, TimeUnit.SECONDS)
        }
    }

    override fun enteringQueue() {
        val opsForZSet = redisTemplate.opsForZSet()
        val top5 = opsForZSet.range(ZSET_WAIT_KEY, 0, 4)

        if (top5 == null || top5.isEmpty()) {
            return
        }

        for (userid in top5) {
            val enterKey = "$ENTER_LIST_KEY$userid"
            redisTemplate.opsForValue().set(enterKey, "1")
            redisTemplate.expire(enterKey, enteringTime, TimeUnit.SECONDS)
        }
    }
}
