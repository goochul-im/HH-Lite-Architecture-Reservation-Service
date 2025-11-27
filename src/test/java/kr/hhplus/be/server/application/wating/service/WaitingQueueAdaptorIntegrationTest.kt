package kr.hhplus.be.server.application.wating.service

import kr.hhplus.be.server.TestcontainersConfiguration
import kr.hhplus.be.server.application.wating.WaitingQueueConstant
import kr.hhplus.be.server.application.wating.infrastructure.WaitingQueueAdaptor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.TimeUnit

@SpringBootTest
@Import(TestcontainersConfiguration::class)
@ActiveProfiles("test")
class WaitingQueueAdaptorIntegrationTest {

    @Autowired
    private lateinit var waitingQueueAdaptor: WaitingQueueAdaptor

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    @AfterEach
    fun tearDown() {
        redisTemplate.delete(WaitingQueueConstant.ZSET_WAIT_KEY)
        val keys = redisTemplate.keys("${WaitingQueueConstant.ENTER_LIST_KEY}*")
        if (keys != null && keys.isNotEmpty()) {
            redisTemplate.delete(keys)
        }
    }


    @Test
    @DisplayName("대기열 추가 및 순위 확인 테스트")
    fun `add and getMyRank should work correctly`() {
        // given
        val userId1 = "testUser1"
        val userId2 = "testUser2"

        // when
        waitingQueueAdaptor.add(userId1)
        Thread.sleep(10) // Ensure different scores
        waitingQueueAdaptor.add(userId2)

        // then
        val rank1 = waitingQueueAdaptor.getMyRank(userId1)
        val rank2 = waitingQueueAdaptor.getMyRank(userId2)

        assertThat(rank1).isEqualTo(0)
        assertThat(rank2).isEqualTo(1)
    }

    @Test
    @DisplayName("입장 허용 키 존재 여부 확인 테스트")
    fun `isEnteringKey should return correct boolean`() {
        // given
        val userId = "testUser"
        val enteringKey = "${WaitingQueueConstant.ENTER_LIST_KEY}$userId"

        // when
        val before = waitingQueueAdaptor.isEnteringKey(userId)
        redisTemplate.opsForValue().set(enteringKey, "1")
        val after = waitingQueueAdaptor.isEnteringKey(userId)

        // then
        assertThat(before).isFalse()
        assertThat(after).isTrue()
    }

    @Test
    @DisplayName("토큰 삭제 테스트")
    fun `deleteToken should remove the key from redis`() {
        // given
        val userId = "testUser"
        val enteringKey = "${WaitingQueueConstant.ENTER_LIST_KEY}$userId"
        redisTemplate.opsForValue().set(enteringKey, "1")

        // when
        waitingQueueAdaptor.deleteToken(userId)

        // then
        val keyExists = redisTemplate.hasKey(enteringKey)
        assertThat(keyExists).isFalse()
    }

    @Test
    @DisplayName("토큰 TTL 갱신 테스트")
    fun `renewalTokenTTL should extend expiration time`() {
        // given
        val userId = "testUser"
        val enteringKey = "${WaitingQueueConstant.ENTER_LIST_KEY}$userId"
        redisTemplate.opsForValue().set(enteringKey, "1", 10, TimeUnit.SECONDS)

        // when
        val ttlBefore = redisTemplate.getExpire(enteringKey, TimeUnit.SECONDS)
        waitingQueueAdaptor.renewalTokenTTL(userId)
        val ttlAfter = redisTemplate.getExpire(enteringKey, TimeUnit.SECONDS)

        // then
        assertThat(ttlBefore).isNotNull()
        assertThat(ttlAfter).isNotNull()
        // The new TTL is set by `enteringTime` value from properties, which is likely larger than the initial 10s
        assertThat(ttlAfter).isGreaterThan(ttlBefore!!)
    }

//    @Test
//    @DisplayName("입장 처리 로직 테스트")
//    fun `enteringQueue should move top users to entering state`() {
//        // given
//        (1..10).forEach {
//            waitingQueueAdaptor.add("user$it")
//            Thread.sleep(10)
//        }
//
//        // when
//        waitingQueueAdaptor.enteringQueue()
//
//        // then
//        val top5 = redisTemplate.opsForZSet().range(WaitingQueueConstant.ZSET_WAIT_KEY, 0, 4)
//        assertThat(top5).hasSize(5)
//
//        top5?.forEach { userId ->
//            val isEntering = waitingQueueAdaptor.isEnteringKey(userId)
//            assertThat(isEntering).isTrue()
//        }
//
//        // Check users not in top 5
//        val notTopUser = "user6"
//        val isNotTopUserEntering = waitingQueueAdaptor.isEnteringKey(notTopUser)
//        assertThat(isNotTopUserEntering).isFalse()
//    }
}
