package kr.hhplus.be.server.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.transaction.Transactional
import kr.hhplus.be.server.TestcontainersConfiguration
import kr.hhplus.be.server.application.point.controller.UsePointReq
import kr.hhplus.be.server.application.point.dto.PointResponse
import kr.hhplus.be.server.application.wating.service.WaitingQueueService
import kr.hhplus.be.server.auth.LoginRequest
import kr.hhplus.be.server.auth.TokenResponse
import kr.hhplus.be.server.member.domain.Member
import kr.hhplus.be.server.member.infrastructure.MemberJpaRepository
import kr.hhplus.be.server.member.port.MemberRepository
import kr.hhplus.be.server.member.service.MemberService
import kr.hhplus.be.server.reservation.controller.ReservationMakeRequest
import kr.hhplus.be.server.reservation.dto.ReservationResponse
import kr.hhplus.be.server.reservation.infrastructure.ReservationJpaRepository
import kr.hhplus.be.server.reservation.infrastructure.ReservationStatus
import kr.hhplus.be.server.reservation.port.ReservationRepository
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@ActiveProfiles("test")
@Transactional
class ApplicationIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var memberService: MemberService

    @Autowired
    lateinit var waitingQueueService: WaitingQueueService

    @Autowired
    lateinit var reservationRepository: ReservationJpaRepository

    @Autowired
    lateinit var memberRepository: MemberJpaRepository

    lateinit var accessToken: String
    lateinit var waitingToken: String

    @BeforeEach
    fun `로그인 후 액세스 토큰과 대기열 토큰 생성`() {
        val saveMember = memberService.signUp("testUser", "testPassword")

        val loginRequest = LoginRequest(
            "testUser",
            "testPassword"
        )

        // 로그인 후 액세스 토큰 발급 로직
        val result = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        ).andExpect(status().isOk)
            .andReturn()

        val content = result.response.contentAsString
        val loginResponse = objectMapper.readValue(content, TokenResponse::class.java)
        accessToken = loginResponse.accessToken.toString()

        // 액세스 토큰을 이용해 대기열 토큰 발급
        val enterResult = mockMvc.perform(
            post("/api/wait/enter")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk)
            .andReturn()

        val enterContent = enterResult.response.contentAsString
        val enterResponse = objectMapper.readValue(enterContent, TokenResponse::class.java)
        waitingToken = enterResponse.waitingToken.toString()
    }

    @Test
    fun `좌석 예약을 요청하고 충전 후 결제를 완료하면 예약이 완료된다`() {

        val chargeReq = UsePointReq(
            10000
        )

        mockMvc.perform(
            post("/api/point/charge")
                .header("Authorization", "Bearer $accessToken")
//                .header("X-Waiting-Token", "Bearer $waitingToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chargeReq))
        )

        val pointResult = mockMvc.perform(
            get("/api/point")
                .header("Authorization", "Bearer $accessToken")
//                .header("X-Waiting-Token", "Bearer $waitingToken")
        ).andExpect(status().isOk)
            .andReturn()

        val pointResponse = objectMapper.readValue(pointResult.response.contentAsString, PointResponse::class.java)
        assertThat(pointResponse.point).isEqualTo(10000)

        // 좌석 예약 요청
        val request = ReservationMakeRequest(
            LocalDate.of(2025, 12, 1),
            10
        )

        waitingQueueService.enterQueue() // 즉시 대기열에서 접속상태로 변경

        val reservation = mockMvc.perform(
            post("/api/reservation") // 예약 생성
                .header("Authorization", "Bearer $accessToken")
                .header("X-Waiting-Token", "Bearer $waitingToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.date").value("2025-12-01"))
            .andExpect(jsonPath("$.seatNumber").value("10"))
            .andReturn()

        val reservationResponse =
            objectMapper.readValue(reservation.response.contentAsString, ReservationResponse::class.java)

        validateReservationInRepo(ReservationStatus.PENDING)

        // 임시 예약 결제
        val reservationId = reservationResponse.id

        mockMvc.perform(
            post("/api/reservation/pay/$reservationId") // 임시 예약 결제
                .header("Authorization", "Bearer $accessToken")
                .header("X-Waiting-Token", "Bearer $waitingToken")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.date").value("2025-12-01"))
            .andExpect(jsonPath("$.seatNumber").value("10"))

        validateReservationInRepo(ReservationStatus.RESERVE)
    }

    private fun validateReservationInRepo(status: ReservationStatus) {
        val reservationInRepo = reservationRepository.findById(1L)
        assertThat(reservationInRepo).isNotEmpty
        assertThat(reservationInRepo.get().id).isEqualTo(1L)
        assertThat(reservationInRepo.get().date).isEqualTo(LocalDate.of(2025, 12, 1))
        assertThat(reservationInRepo.get().status).isEqualTo(status)
        assertThat(reservationInRepo.get().seatNumber).isEqualTo(10)
    }


}
