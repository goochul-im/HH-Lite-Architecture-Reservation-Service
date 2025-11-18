package kr.hhplus.be.server.application.wating

import kr.hhplus.be.server.application.wating.WaitingQueueConstant.ENTER_LIST_KEY
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
        if (expiredKey.startsWith(ENTER_LIST_KEY)) {

        }

    }
}

