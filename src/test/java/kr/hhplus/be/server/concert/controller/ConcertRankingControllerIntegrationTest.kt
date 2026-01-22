package kr.hhplus.be.server.concert.controller

import kr.hhplus.be.server.TestcontainersConfiguration
import kr.hhplus.be.server.concert.infrastructure.ConcertEntity
import kr.hhplus.be.server.concert.infrastructure.ConcertJpaRepository
import kr.hhplus.be.server.concert.infrastructure.ConcertSoldOutAdapter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@ActiveProfiles("test")
@WithMockUser(username = "testUser")
class ConcertRankingControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var concertJpaRepository: ConcertJpaRepository

    @Autowired
    private lateinit var concertSoldOutAdapter: ConcertSoldOutAdapter

    @Autowired
    private lateinit var redissonClient: RedissonClient

    private lateinit var concert1: ConcertEntity
    private lateinit var concert2: ConcertEntity
    private lateinit var concert3: ConcertEntity

    @BeforeEach
    fun setUp() {
        redissonClient.keys.flushall()
        concertJpaRepository.deleteAll()

        concert1 = concertJpaRepository.save(
            ConcertEntity(name = "아이유 콘서트", date = LocalDate.of(2025, 12, 25), totalSeats = 50)
        )
        concert2 = concertJpaRepository.save(
            ConcertEntity(name = "BTS 콘서트", date = LocalDate.of(2025, 12, 26), totalSeats = 100)
        )
        concert3 = concertJpaRepository.save(
            ConcertEntity(name = "블랙핑크 콘서트", date = LocalDate.of(2025, 12, 27), totalSeats = 80)
        )
    }

    @AfterEach
    fun tearDown() {
        redissonClient.keys.flushall()
        concertJpaRepository.deleteAll()
    }

    @Test
    fun `매진 콘서트 랭킹을 조회할 수 있다`() {
        // Given: 콘서트들을 매진 순서대로 등록
        concertSoldOutAdapter.markSoldOut(concert2.id!!, 1000L) // BTS - 1등
        concertSoldOutAdapter.markSoldOut(concert1.id!!, 2000L) // 아이유 - 2등
        concertSoldOutAdapter.markSoldOut(concert3.id!!, 3000L) // 블랙핑크 - 3등

        // When & Then
        mockMvc.perform(
            get("/api/ranking/concert")
                .param("topN", "10")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].concertId").value(concert2.id))
            .andExpect(jsonPath("$[0].concertName").value("BTS 콘서트"))
            .andExpect(jsonPath("$[0].rank").value(1))
            .andExpect(jsonPath("$[1].concertId").value(concert1.id))
            .andExpect(jsonPath("$[1].concertName").value("아이유 콘서트"))
            .andExpect(jsonPath("$[1].rank").value(2))
            .andExpect(jsonPath("$[2].concertId").value(concert3.id))
            .andExpect(jsonPath("$[2].concertName").value("블랙핑크 콘서트"))
            .andExpect(jsonPath("$[2].rank").value(3))
    }

    @Test
    fun `기본값으로 상위 10개의 매진 콘서트 랭킹을 조회한다`() {
        // Given
        concertSoldOutAdapter.markSoldOut(concert1.id!!, 1000L)
        concertSoldOutAdapter.markSoldOut(concert2.id!!, 2000L)

        // When & Then
        mockMvc.perform(
            get("/api/ranking/concert")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `topN 파라미터로 조회할 콘서트 수를 제한할 수 있다`() {
        // Given
        concertSoldOutAdapter.markSoldOut(concert1.id!!, 1000L)
        concertSoldOutAdapter.markSoldOut(concert2.id!!, 2000L)
        concertSoldOutAdapter.markSoldOut(concert3.id!!, 3000L)

        // When & Then
        mockMvc.perform(
            get("/api/ranking/concert")
                .param("topN", "2")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].rank").value(1))
            .andExpect(jsonPath("$[1].rank").value(2))
    }

    @Test
    fun `매진된 콘서트가 없으면 빈 목록을 반환한다`() {
        // When & Then
        mockMvc.perform(
            get("/api/ranking/concert")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `매진 시간이 빠른 순서대로 랭킹이 매겨진다`() {
        // Given: concert3가 가장 먼저 매진
        concertSoldOutAdapter.markSoldOut(concert3.id!!, 500L)  // 블랙핑크 - 가장 먼저 매진
        concertSoldOutAdapter.markSoldOut(concert1.id!!, 1500L) // 아이유
        concertSoldOutAdapter.markSoldOut(concert2.id!!, 2500L) // BTS - 가장 늦게 매진

        // When & Then
        mockMvc.perform(
            get("/api/ranking/concert")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].concertName").value("블랙핑크 콘서트"))
            .andExpect(jsonPath("$[0].rank").value(1))
            .andExpect(jsonPath("$[1].concertName").value("아이유 콘서트"))
            .andExpect(jsonPath("$[1].rank").value(2))
            .andExpect(jsonPath("$[2].concertName").value("BTS 콘서트"))
            .andExpect(jsonPath("$[2].rank").value(3))
    }
}
