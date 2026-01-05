package kr.hhplus.be.server.application.wating.infrastructure

import kr.hhplus.be.server.application.wating.WaitingQueueConstant
import kr.hhplus.be.server.application.wating.service.port.WaitingQueuePort
import mu.KotlinLogging
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.TimeUnit

@Component
class WaitingQueueAdaptor(
    private val redissonClient: RedissonClient,
    @param:Value("\${waiting.expiration-time-second}")
    private val enteringTime: Long = 10,
) : WaitingQueuePort {

    private val log = KotlinLogging.logger { }

    private fun getWaitZSet() = redissonClient.getScoredSortedSet<String>(WaitingQueueConstant.ZSET_WAIT_KEY)
    private fun getEnterBucket(userId: String) = redissonClient.getBucket<String>("${WaitingQueueConstant.ENTER_LIST_KEY}$userId")

    override fun add(userId: String) {
        val score = System.currentTimeMillis().toDouble()
        // add()는 성공 시 true, 이미 존재하면 false를 반환합니다.
        val added = getWaitZSet().add(score, userId)
        log.info { "Redis wait:order: 에 $userId 추가 여부 : $added" }
    }

    override fun getMyRank(userId: String): Long? {
        // Redisson의 rank는 0부터 시작합니다. (순위가 없으면 null 반환)
        val rank = getWaitZSet().rank(userId)
        return rank?.toLong()
    }

    override fun isEnteringKey(userId: String): Boolean {
        // hasKey 대신 exists()를 사용합니다.
        return getEnterBucket(userId).isExists
    }

    override fun deleteToken(userId: String) {
        // RBucket의 delete()를 호출합니다.
        getEnterBucket(userId).delete()
    }

    override fun renewalTokenTTL(userId: String) {
        val bucket = getEnterBucket(userId)
        // 존재 여부 확인 후 TTL 갱신 (expire 연장)
        if (bucket.isExists) {
            bucket.expire(Duration.ofSeconds(enteringTime))
        }
    }

    override fun enteringQueue() {
        val opsForZSet = getWaitZSet()
        val top5 = opsForZSet.pollFirst(5)

        if (top5 == null || top5.isEmpty()) {
            return
        }

        for (userid in top5) {
            val bucket = getEnterBucket(userid)
            bucket.set("1", Duration.ofSeconds(enteringTime))
            log.info { "대기열에서 접속 리스트로 이동 : $userid" }
        }
    }
}
