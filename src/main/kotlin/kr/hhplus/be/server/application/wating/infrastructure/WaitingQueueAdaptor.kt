package kr.hhplus.be.server.application.wating.infrastructure

import kr.hhplus.be.server.application.wating.WaitingQueueConstant
import kr.hhplus.be.server.application.wating.service.port.WaitingQueuePort
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class WaitingQueueAdaptor(
    private val redisTemplate: StringRedisTemplate,
    @param:Value("\${waiting.expiration-time-second}")
    private val enteringTime: Long = 10,
) : WaitingQueuePort {

    private val log = KotlinLogging.logger { }

    override fun add(userId: String) {
        val score = System.currentTimeMillis().toDouble()
        val add = redisTemplate.opsForZSet().add(WaitingQueueConstant.ZSET_WAIT_KEY, userId, score)
        log.info(" Redis wait:order: 에 $userId 추가 여부 : $add ")
    }

    override fun getMyRank(userId: String): Long? {
        return redisTemplate.opsForZSet().rank(WaitingQueueConstant.ZSET_WAIT_KEY, userId)
    }

    override fun isEnteringKey(userId: String): Boolean {
        val key = "${WaitingQueueConstant.ENTER_LIST_KEY}$userId"
        return redisTemplate.hasKey(key)
    }

    override fun deleteToken(userId: String) {
        val key = "${WaitingQueueConstant.ENTER_LIST_KEY}$userId"
        redisTemplate.delete(key)
    }

    override fun renewalTokenTTL(userId: String) {
        val key = "${WaitingQueueConstant.ENTER_LIST_KEY}$userId"
        if (redisTemplate.hasKey(key)) {
            redisTemplate.expire(key, enteringTime, TimeUnit.SECONDS)
        }
    }

    override fun enteringQueue() {
        val opsForZSet = redisTemplate.opsForZSet()
        val top5 = opsForZSet.popMin(WaitingQueueConstant.ZSET_WAIT_KEY, 5)

        if (top5 == null || top5.isEmpty()) {
            return
        }

        for (userid in top5) {
            val enterKey = "${WaitingQueueConstant.ENTER_LIST_KEY}${userid.value}"
            redisTemplate.opsForValue().set(enterKey, "1")
            redisTemplate.expire(enterKey, enteringTime, TimeUnit.SECONDS)
            log.info { "대기열에서 접속 리스트로 이동 : ${userid.value}" }
        }
    }
}
