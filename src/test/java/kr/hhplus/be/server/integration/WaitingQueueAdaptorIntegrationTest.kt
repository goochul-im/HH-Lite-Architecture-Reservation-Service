package kr.hhplus.be.server.integration

import kr.hhplus.be.server.TestcontainersConfiguration
import kr.hhplus.be.server.application.wating.WaitingQueueConstant
import kr.hhplus.be.server.application.wating.infrastructure.WaitingQueueAdaptor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.util.concurrent.TimeUnit

@SpringBootTest
@Import(TestcontainersConfiguration::class)
@ActiveProfiles("test")
class WaitingQueueAdaptorIntegrationTest {

    @Autowired
    private lateinit var waitingQueueAdaptor: WaitingQueueAdaptor

    @Autowired
    private lateinit var redissonClient: RedissonClient

    @AfterEach
    fun tearDown() {
        // 1. 특정 대기열 ZSET 삭제
        redissonClient.getScoredSortedSet<String>(WaitingQueueConstant.ZSET_WAIT_KEY).delete()

        // 2. 패턴을 이용한 대기열 진입 키들 일괄 삭제
        // keys().deleteByPattern은 Redis의 SCAN 명령을 사용하여 안전하게 대량의 키를 삭제합니다.
        val pattern = "${WaitingQueueConstant.ENTER_LIST_KEY}*"
        redissonClient.keys.deleteByPattern(pattern)
    }

    @Test
    @DisplayName("대기열 추가 및 순위 확인 테스트")
    fun `add and getMyRank should work correctly`() {
        // given
        val userId1 = "testUser1"
        val userId2 = "testUser2"

        // when
        waitingQueueAdaptor.add(userId1)
        Thread.sleep(10) // 스코어 차이를 위해 잠시 대기
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

        // when - redisTemplate 대신 Redisson의 RBucket 사용
        val before = waitingQueueAdaptor.isEnteringKey(userId)
        redissonClient.getBucket<String>(enteringKey).set("1")
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
        redissonClient.getBucket<String>(enteringKey).set("1")

        // when
        waitingQueueAdaptor.deleteToken(userId)

        // then - hasKey 대신 isExists 사용
        val keyExists = redissonClient.getBucket<String>(enteringKey).isExists
        assertThat(keyExists).isFalse()
    }

    @Test
    @DisplayName("토큰 TTL 갱신 테스트")
    fun `renewalTokenTTL should extend expiration time`() {
        // given
        val userId = "testUser"
        val enteringKey = "${WaitingQueueConstant.ENTER_LIST_KEY}$userId"
        // 처음엔 10초로 설정
        redissonClient.getBucket<String>(enteringKey).set("1", Duration.ofSeconds(10))

        // when
        val ttlBefore = redissonClient.getBucket<String>(enteringKey).remainTimeToLive() // ms 단위 반환
        waitingQueueAdaptor.renewalTokenTTL(userId)
        val ttlAfter = redissonClient.getBucket<String>(enteringKey).remainTimeToLive()

        // then
        assertThat(ttlBefore).isGreaterThan(0)
        assertThat(ttlAfter).isGreaterThan(ttlBefore)
    }

    @Test
    @DisplayName("입장 처리 로직 테스트")
    fun `enteringQueue should move top users to entering state`() {
        // given
        (1..5).forEach {
            waitingQueueAdaptor.add("user$it")
            Thread.sleep(10)
        }

        // when
        waitingQueueAdaptor.enteringQueue()

        // then
        (1..5).forEach {
            val isEntering = waitingQueueAdaptor.isEnteringKey("user$it")
            assertThat(isEntering).isTrue()
        }

        // Check users not in top 5
        val notTopUser = "user6"
        val isNotTopUserEntering = waitingQueueAdaptor.isEnteringKey(notTopUser)
        assertThat(isNotTopUserEntering).isFalse()
    }
}
