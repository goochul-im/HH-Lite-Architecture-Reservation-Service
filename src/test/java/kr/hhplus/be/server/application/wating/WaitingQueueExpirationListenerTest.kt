package kr.hhplus.be.server.application.wating

import kr.hhplus.be.server.TestcontainersConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.core.RedisTemplate
import java.time.LocalDate

@SpringBootTest
@Import(TestcontainersConfiguration::class) // Testcontainers 설정 가져오기
class WaitingQueueExpirationListenerTest {

    @Autowired
    private lateinit var waitingQueueExpirationListener: WaitingQueueExpirationListener

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, Any>

    // 각 테스트 후 Redis 데이터 클린업
    @AfterEach
    fun tearDown() {
        redisTemplate.delete(WaitingQueueConstant.ZSET_WAIT_KEY)
        val keysToDelete = redisTemplate.keys("${WaitingQueueConstant.QUEUE_PREFIX}*")
        keysToDelete.addAll(redisTemplate.keys("${WaitingQueueConstant.CLEANUP_PREFIX}*"))
        if (keysToDelete.isNotEmpty()) {
            redisTemplate.delete(keysToDelete)
        }
    }

    @Test
    @DisplayName("onMessage 호출 시 만료된 대기열 키에 해당하는 사용자가 ZSet에서 성공적으로 제거되어야 한다")
    fun onMessage_shouldRemoveUserFromZSetWhenQueueKeyExpires() {
        // given (준비)
        val userId = "test-user-onmessage-1"
        val num = 100
        val date = LocalDate.now()

        val queueKey = WaitingQueueConstant.QUEUE_PREFIX + date.toString()
        val cleanupKey = WaitingQueueConstant.CLEANUP_PREFIX + date.toString()
        val zsetKey = WaitingQueueConstant.ZSET_WAIT_KEY

        // Redis에 초기 상태 설정: saveNumber가 호출된 것처럼 데이터 생성
        redisTemplate.opsForHash<String, UserEntry>().put(queueKey, userId, UserEntry(userId, num))
        redisTemplate.opsForSet().add(cleanupKey, userId)
        redisTemplate.opsForZSet().add(zsetKey, userId, System.currentTimeMillis().toDouble())

        // ZSet에 사용자가 존재하는지 확인
        Assertions.assertNotNull(redisTemplate.opsForZSet().score(zsetKey, userId), "테스트 시작 전 ZSet에 사용자가 존재해야 합니다.")
        // cleanupKey가 존재하는지 확인
        Assertions.assertTrue(redisTemplate.hasKey(cleanupKey), "테스트 시작 전 cleanupKey가 존재해야 합니다.")

        // 만료된 키 메시지 생성 (실제 Redis 이벤트와 유사하게)
        val expiredMessage = object : Message {
            override fun getBody(): ByteArray {
                return queueKey.toByteArray()
            }

            override fun getChannel(): ByteArray {
                return byteArrayOf()
            }

            override fun toString(): String {
                return queueKey
            }
        }

        // when (실행)
        // cleanupKey가 아직 살아있는 상태에서 onMessage 호출
        waitingQueueExpirationListener.onMessage(expiredMessage, null)

        // then (검증)
        // ZSet에서 사용자가 제거되었는지 확인
        val scoreAfterCleanup = redisTemplate.opsForZSet().score(zsetKey, userId)
        Assertions.assertNull(scoreAfterCleanup, "onMessage 호출 후 ZSet에서 사용자가 제거되어야 합니다.")
    }
}
