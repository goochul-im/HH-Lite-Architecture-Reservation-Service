package kr.hhplus.be.server.concert.controller

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhplus.be.server.TestcontainersConfiguration
import kr.hhplus.be.server.concert.dto.ConcertCreateRequest
import kr.hhplus.be.server.concert.infrastructure.ConcertEntity
import kr.hhplus.be.server.concert.infrastructure.ConcertJpaRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@SpringBootTest(properties = ["spring.cache.type=none"])
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@ActiveProfiles("test")
@WithMockUser(username = "testUser")
class ConcertControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var concertJpaRepository: ConcertJpaRepository

    @BeforeEach
    fun setUp() {
        concertJpaRepository.deleteAll()
    }

    @AfterEach
    fun tearDown() {
        concertJpaRepository.deleteAll()
    }

    @Test
    fun `콘서트를 등록할 수 있다`() {
        // Given
        val request = ConcertCreateRequest(
            name = "아이유 콘서트",
            date = LocalDate.of(2025, 12, 25),
            totalSeats = 50
        )

        // When & Then
        mockMvc.perform(
            post("/api/concerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("아이유 콘서트"))
            .andExpect(jsonPath("$.date").value("2025-12-25"))
            .andExpect(jsonPath("$.totalSeats").value(50))
            .andExpect(jsonPath("$.id").exists())
    }

    @Test
    fun `같은 날짜에 콘서트가 이미 존재하면 등록에 실패한다`() {
        // Given
        val existingDate = LocalDate.of(2025, 12, 25)
        concertJpaRepository.save(
            ConcertEntity(name = "기존 콘서트", date = existingDate, totalSeats = 100)
        )

        val request = ConcertCreateRequest(
            name = "새 콘서트",
            date = existingDate,
            totalSeats = 50
        )

        // When & Then
        mockMvc.perform(
            post("/api/concerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `콘서트 ID로 조회할 수 있다`() {
        // Given
        val savedConcert = concertJpaRepository.save(
            ConcertEntity(name = "테스트 콘서트", date = LocalDate.of(2025, 12, 25), totalSeats = 50)
        )

        // When & Then
        mockMvc.perform(
            get("/api/concerts/${savedConcert.id}")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(savedConcert.id))
            .andExpect(jsonPath("$.name").value("테스트 콘서트"))
            .andExpect(jsonPath("$.date").value("2025-12-25"))
            .andExpect(jsonPath("$.totalSeats").value(50))
    }

    @Test
    fun `존재하지 않는 콘서트 ID로 조회하면 404를 반환한다`() {
        // When & Then
        mockMvc.perform(
            get("/api/concerts/999999")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `예약 가능한 콘서트 목록을 조회할 수 있다`() {
        // Given: 미래 날짜의 콘서트 저장
        val futureConcert1 = concertJpaRepository.save(
            ConcertEntity(name = "미래 콘서트1", date = LocalDate.now().plusDays(1), totalSeats = 50)
        )
        val futureConcert2 = concertJpaRepository.save(
            ConcertEntity(name = "미래 콘서트2", date = LocalDate.now().plusDays(2), totalSeats = 100)
        )
        // 과거 콘서트는 조회되지 않아야 함
        concertJpaRepository.save(
            ConcertEntity(name = "과거 콘서트", date = LocalDate.now().minusDays(1), totalSeats = 30)
        )

        // When & Then
        mockMvc.perform(
            get("/api/concerts")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.concerts.length()").value(2))
            .andExpect(jsonPath("$.concerts[0].name").value("미래 콘서트1"))
            .andExpect(jsonPath("$.concerts[1].name").value("미래 콘서트2"))
    }

    @Test
    fun `예약 가능한 콘서트가 없으면 빈 목록을 반환한다`() {
        // Given: 과거 콘서트만 존재
        concertJpaRepository.save(
            ConcertEntity(name = "과거 콘서트", date = LocalDate.now().minusDays(1), totalSeats = 50)
        )

        // When & Then
        mockMvc.perform(
            get("/api/concerts")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.concerts.length()").value(0))
    }

    @Test
    fun `오늘 날짜의 콘서트는 예약 가능 목록에 포함되지 않는다`() {
        // Given
        concertJpaRepository.save(
            ConcertEntity(name = "오늘 콘서트", date = LocalDate.now(), totalSeats = 50)
        )
        val tomorrowConcert = concertJpaRepository.save(
            ConcertEntity(name = "내일 콘서트", date = LocalDate.now().plusDays(1), totalSeats = 100)
        )

        // When & Then
        mockMvc.perform(
            get("/api/concerts")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.concerts.length()").value(1))
            .andExpect(jsonPath("$.concerts[0].name").value("내일 콘서트"))
    }
}
