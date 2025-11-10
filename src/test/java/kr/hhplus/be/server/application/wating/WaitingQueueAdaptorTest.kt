package kr.hhplus.be.server.application.wating

import kr.hhplus.be.server.TestcontainersConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.RedisTemplate
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@SpringBootTest
@Import(TestcontainersConfiguration::class) // Testcontainers 설정 가져오기
class WaitingQueueAdaptorTest {

    @Autowired
    private lateinit var waitingQueueAdaptor: WaitingQueueAdaptor

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, Any>

    @Value("\${waiting.expiration_time}")
    private lateinit var waitingTime: String

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
    @DisplayName("saveNumber 호출 시 cleanupKey의 만료시간이 queueKey보다 1분 더 길게 설정되어야 한다")
    fun saveNumber_setsCorrectExpirationTimes() {
        // given (준비)
        val userId = "test-user-123"
        val num = 10
        val date = LocalDate.now()

        val queueKey = WaitingQueueConstant.QUEUE_PREFIX + date.toString()
        val cleanupKey = WaitingQueueConstant.CLEANUP_PREFIX + date.toString()
        val zsetKey = WaitingQueueConstant.ZSET_WAIT_KEY

        // when (실행)
        waitingQueueAdaptor.saveNumber(userId, num, date)

        // then (검증)
        // 1. 데이터 저장 기본 검증
        Assertions.assertNotNull(redisTemplate.opsForHash<String, UserEntry>().get(queueKey, userId))
        Assertions.assertNotNull(redisTemplate.opsForZSet().score(zsetKey, userId))
        Assertions.assertTrue(redisTemplate.opsForSet().isMember(cleanupKey, userId)!!)

        // 2. 만료 시간(TTL) 검증
        val queueTtl = redisTemplate.getExpire(queueKey, TimeUnit.SECONDS)
        val cleanupTtl = redisTemplate.getExpire(cleanupKey, TimeUnit.SECONDS)
        val expectedQueueTtl = TimeUnit.MINUTES.toSeconds(waitingTime.toLong())
        val expectedCleanupTtl = TimeUnit.MINUTES.toSeconds(waitingTime.toLong() + 1)

        Assertions.assertTrue(queueTtl > 0, "queueKey의 TTL이 설정되어야 합니다.")
        Assertions.assertTrue(cleanupTtl > 0, "cleanupKey의 TTL이 설정되어야 합니다.")

        // TTL이 설정 값 근처인지 확인 (실행 시간에 따른 약간의 오차 감안)
        Assertions.assertTrue(
            queueTtl <= expectedQueueTtl && queueTtl > expectedQueueTtl - 10,
            "queueKey의 TTL이 약 ${waitingTime}분이어야 합니다."
        )
        Assertions.assertTrue(
            cleanupTtl <= expectedCleanupTtl && cleanupTtl > expectedCleanupTtl - 10,
            "cleanupKey의 TTL이 약 ${waitingTime.toLong() + 1}분이어야 합니다."
        )

        // cleanupKey의 TTL이 queueKey의 TTL보다 긴지 확인
        Assertions.assertTrue(cleanupTtl > queueTtl, "cleanupKey의 TTL이 queueKey의 TTL보다 길어야 합니다.")
    }
}
