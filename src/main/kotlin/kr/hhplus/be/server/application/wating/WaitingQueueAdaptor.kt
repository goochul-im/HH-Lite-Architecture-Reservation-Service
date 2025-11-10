package kr.hhplus.be.server.application.wating

import kr.hhplus.be.server.application.wating.WaitingQueueConstant.CLEANUP_PREFIX
import kr.hhplus.be.server.application.wating.WaitingQueueConstant.WAITING_QUEUE
import kr.hhplus.be.server.application.wating.WaitingQueueConstant.ZSET_WAIT_KEY
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@Component
class WaitingQueueAdaptor(
    private val redisTemplate: RedisTemplate<String, Any>,
    @param:Value("\${waiting.expiration_time}")
    private val waitingTime: Long
) : WaitingQueuePort {

    override fun add(userId: String): Long? {
        val score = System.currentTimeMillis().toDouble()
        redisTemplate.opsForZSet().add(ZSET_WAIT_KEY, userId, score)
        return redisTemplate.opsForZSet().rank(ZSET_WAIT_KEY, userId)
    }

    override fun getMyRank(userId: String): Int {
        TODO("Not yet implemented")
    }

    override fun issueWaitingToken(userId: String): String {
        TODO("Not yet implemented")
    }

    override fun validateToken(userId: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun deleteToken(userId: String) {
        TODO("Not yet implemented")
    }

    override fun renewalTokenTTL(userId: String) {
        TODO("Not yet implemented")
    }
}
