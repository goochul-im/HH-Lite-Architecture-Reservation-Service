package kr.hhplus.be.server.concert.infrastructure

import kr.hhplus.be.server.TestcontainersConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@Import(TestcontainersConfiguration::class)
@ActiveProfiles("test")
class ConcertSoldOutAdapterTest {

    @Autowired
    private lateinit var concertSoldOutAdapter: ConcertSoldOutAdapter

    @Autowired
    private lateinit var redissonClient: RedissonClient

    @BeforeEach
    fun setUp() {
        redissonClient.keys.flushall()
    }

    @AfterEach
    fun tearDown() {
        redissonClient.keys.flushall()
    }

    @Nested
    @DisplayName("markSoldOut 테스트")
    inner class MarkSoldOutTest {

        @Test
        @DisplayName("콘서트를 매진으로 표시할 수 있다")
        fun `콘서트를 매진으로 표시할 수 있다`() {
            // Given
            val concertId = 1L
            val timestamp = System.currentTimeMillis()

            // When
            concertSoldOutAdapter.markSoldOut(concertId, timestamp)

            // Then
            assertThat(concertSoldOutAdapter.isSoldOut(concertId)).isTrue()
        }

        @Test
        @DisplayName("이미 매진된 콘서트는 중복으로 추가되지 않는다")
        fun `이미 매진된 콘서트는 중복으로 추가되지 않는다`() {
            // Given
            val concertId = 1L
            val timestamp1 = 1000L
            val timestamp2 = 2000L

            // When
            concertSoldOutAdapter.markSoldOut(concertId, timestamp1)
            concertSoldOutAdapter.markSoldOut(concertId, timestamp2)

            // Then
            val ranking = concertSoldOutAdapter.getRank(concertId)
            assertThat(ranking.timestamp).isEqualTo(0L) // rank는 0 (첫 번째)
        }
    }

    @Nested
    @DisplayName("isSoldOut 테스트")
    inner class IsSoldOutTest {

        @Test
        @DisplayName("매진된 콘서트는 true를 반환한다")
        fun `매진된 콘서트는 true를 반환한다`() {
            // Given
            val concertId = 1L
            concertSoldOutAdapter.markSoldOut(concertId, System.currentTimeMillis())

            // When
            val result = concertSoldOutAdapter.isSoldOut(concertId)

            // Then
            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("매진되지 않은 콘서트는 false를 반환한다")
        fun `매진되지 않은 콘서트는 false를 반환한다`() {
            // Given
            val concertId = 999L

            // When
            val result = concertSoldOutAdapter.isSoldOut(concertId)

            // Then
            assertThat(result).isFalse()
        }
    }

    @Nested
    @DisplayName("getTopN 테스트")
    inner class GetTopNTest {

        @Test
        @DisplayName("매진 시간 순으로 상위 N개의 콘서트를 조회할 수 있다")
        fun `매진 시간 순으로 상위 N개의 콘서트를 조회할 수 있다`() {
            // Given
            concertSoldOutAdapter.markSoldOut(1L, 3000L)
            concertSoldOutAdapter.markSoldOut(2L, 1000L)
            concertSoldOutAdapter.markSoldOut(3L, 2000L)

            // When
            val result = concertSoldOutAdapter.getTopN(3)

            // Then
            assertThat(result).hasSize(3)
            assertThat(result[0].concertId).isEqualTo(2L) // 가장 빠른 매진
            assertThat(result[0].timestamp).isEqualTo(1000L)
            assertThat(result[1].concertId).isEqualTo(3L)
            assertThat(result[1].timestamp).isEqualTo(2000L)
            assertThat(result[2].concertId).isEqualTo(1L)
            assertThat(result[2].timestamp).isEqualTo(3000L)
        }

        @Test
        @DisplayName("요청한 수보다 매진 콘서트가 적으면 있는 만큼만 반환한다")
        fun `요청한 수보다 매진 콘서트가 적으면 있는 만큼만 반환한다`() {
            // Given
            concertSoldOutAdapter.markSoldOut(1L, 1000L)
            concertSoldOutAdapter.markSoldOut(2L, 2000L)

            // When
            val result = concertSoldOutAdapter.getTopN(10)

            // Then
            assertThat(result).hasSize(2)
        }

        @Test
        @DisplayName("매진된 콘서트가 없으면 빈 목록을 반환한다")
        fun `매진된 콘서트가 없으면 빈 목록을 반환한다`() {
            // When
            val result = concertSoldOutAdapter.getTopN(10)

            // Then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("getRank 테스트")
    inner class GetRankTest {

        @Test
        @DisplayName("콘서트의 랭킹을 조회할 수 있다")
        fun `콘서트의 랭킹을 조회할 수 있다`() {
            // Given
            concertSoldOutAdapter.markSoldOut(1L, 3000L)
            concertSoldOutAdapter.markSoldOut(2L, 1000L)
            concertSoldOutAdapter.markSoldOut(3L, 2000L)

            // When
            val rank1 = concertSoldOutAdapter.getRank(1L)
            val rank2 = concertSoldOutAdapter.getRank(2L)
            val rank3 = concertSoldOutAdapter.getRank(3L)

            // Then
            assertThat(rank2.timestamp).isEqualTo(0L) // 1등 (가장 빠른 매진)
            assertThat(rank3.timestamp).isEqualTo(1L) // 2등
            assertThat(rank1.timestamp).isEqualTo(2L) // 3등
        }

        @Test
        @DisplayName("매진되지 않은 콘서트의 랭킹은 0을 반환한다")
        fun `매진되지 않은 콘서트의 랭킹은 0을 반환한다`() {
            // Given
            val concertId = 999L

            // When
            val result = concertSoldOutAdapter.getRank(concertId)

            // Then
            assertThat(result.concertId).isEqualTo(concertId)
            assertThat(result.timestamp).isEqualTo(0L)
        }
    }
}
