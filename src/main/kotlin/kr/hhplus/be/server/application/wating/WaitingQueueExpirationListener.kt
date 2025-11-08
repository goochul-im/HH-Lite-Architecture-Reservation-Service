package kr.hhplus.be.server.application.wating

import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.stereotype.Component

@Component
class WaitingQueueExpirationListener(
    listenerContainer: RedisMessageListenerContainer,
    private val redisJsonTemplate: RedisTemplate<String, Any>
) : KeyExpirationEventMessageListener(listenerContainer){

    override fun onMessage(message: Message, pattern: ByteArray?) {
        val expiredKey = message.toString()

        // 예: 키가 'queue:2025-11-08' 형태인지 확인
        if (expiredKey.startsWith("queue:")) {
            println("만료된 대기열 키 감지: $expiredKey")

            // 참고: 만료된 키의 '값'은 이미 Redis에서 삭제되어 조회 불가능
        }
    }

}
