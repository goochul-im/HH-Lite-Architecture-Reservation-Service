package kr.hhplus.be.server.application.wating

import kr.hhplus.be.server.application.wating.WaitingQueueConstant.CLEANUP_PREFIX
import kr.hhplus.be.server.application.wating.WaitingQueueConstant.QUEUE_PREFIX
import kr.hhplus.be.server.application.wating.WaitingQueueConstant.ZSET_WAIT_KEY
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.stereotype.Component

@Component
class WaitingQueueExpirationListener(
    listenerContainer: RedisMessageListenerContainer,
    private val redisJsonTemplate: RedisTemplate<String, Any>
) : KeyExpirationEventMessageListener(listenerContainer) {

    override fun onMessage(message: Message, pattern: ByteArray?) {
        val expiredKey = message.toString()

        // 예: 키가 'queue:2025-11-08' 형태인지 확인
        if (expiredKey.startsWith(QUEUE_PREFIX)) {
            println("만료된 대기열 키 감지: $expiredKey")

            val dateSting = expiredKey.substringAfter(QUEUE_PREFIX)
            val cleanupKey = CLEANUP_PREFIX + dateSting

            val userIdsToRemove = redisJsonTemplate.opsForSet().members(cleanupKey)

            if (!userIdsToRemove.isNullOrEmpty()) {
                println("만료된 대기열($expiredKey) Cleanup 시작. 제거 대상 ${userIdsToRemove.size}명.")

                // 4. ZSet에서 해당 사용자 ID들을 제거 (ZREM)
                redisJsonTemplate.opsForZSet().remove(ZSET_WAIT_KEY, *userIdsToRemove.toTypedArray())
            }

            redisJsonTemplate.delete(cleanupKey)
        }

    }
}

