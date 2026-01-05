package kr.hhplus.be.server.integration

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.Tuple
import jakarta.transaction.Transactional
import kr.hhplus.be.server.TestcontainersConfiguration
import kr.hhplus.be.server.application.point.controller.UsePointReq
import kr.hhplus.be.server.application.point.dto.PointResponse
import kr.hhplus.be.server.application.wating.service.WaitingQueueService
import kr.hhplus.be.server.auth.LoginRequest
import kr.hhplus.be.server.auth.TokenResponse
import kr.hhplus.be.server.exception.DuplicateResourceException
import kr.hhplus.be.server.member.infrastructure.MemberJpaRepository
import kr.hhplus.be.server.member.service.MemberService
import kr.hhplus.be.server.outbox.scheduler.OutboxScheduler
import kr.hhplus.be.server.reservation.controller.ReservationMakeRequest
import kr.hhplus.be.server.reservation.dto.ReservationResponse
import kr.hhplus.be.server.reservation.infrastructure.RedisReservationOperations
import kr.hhplus.be.server.reservation.infrastructure.ReservationJpaRepository
import kr.hhplus.be.server.reservation.infrastructure.ReservationStatus
import kr.hhplus.be.server.reservation.port.TempReservationPort
import kr.hhplus.be.server.reservation.service.ReservationFacade
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.util.NestedServletException
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
    lateinit var tempReservationAdaptor: TempReservationPort

    @Autowired
    lateinit var memberRepository: MemberJpaRepository

    @Autowired
    lateinit var redisOperation: RedisReservationOperations

    @Autowired
    lateinit var reservationFacade: ReservationFacade

    @Autowired
    lateinit var outboxScheduler: OutboxScheduler

    lateinit var accessToken: String
    lateinit var waitingToken: String

    @BeforeEach
    fun `로그인 후 액세스 토큰과 대기열 토큰 생성`() {
        val (access, waiting) = getAccessAndWaitingToken("testUser1")
        accessToken = access
        waitingToken = waiting
    }

    private fun getAccessAndWaitingToken(username: String) : Pair<String, String> {
        val saveMember = memberService.signUp(username, "testPassword")

        val loginRequest = LoginRequest(
            username,
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

        // 액세스 토큰을 이용해 대기열 토큰 발급
        val enterResult = mockMvc.perform(
            post("/api/wait/enter")
                .header("Authorization", "Bearer ${loginResponse.accessToken.toString()}")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk)
            .andReturn()

        val enterContent = enterResult.response.contentAsString
        val enterResponse = objectMapper.readValue(enterContent, TokenResponse::class.java)

        waitingQueueService.enterQueue() // 즉시 대기열에서 접속상태로 변경

        return Pair(loginResponse.accessToken.toString(), enterResponse.waitingToken.toString())
    }

    /**
     * Redis는 수동으로 초기화를 진행해줘야 함
     */
    @AfterEach
    fun cleanUpRedis() {
        redisOperation.cleanUp()
    }

    @Test
    fun `좌석 예약을 요청하고 충전 후 결제를 완료하면 예약이 완료된다`() {

        chargePoint(accessToken)

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

        val reservationResponse =
            makeReservation(request, accessToken, waitingToken) // 예약 생성

        validateReservationInRepo(
            reservationResponse.id,
            LocalDate.of(2025, 12, 1),
            10,
            ReservationStatus.PENDING
        )

        // 임시 예약 결제
        payReservation(reservationResponse.id, accessToken, waitingToken)

        validateReservationInRepo(
            reservationResponse.id,
            LocalDate.of(2025, 12, 1),
            10,
            ReservationStatus.RESERVE
        )

    }

    @Test
    fun `임시 예약 후 만료 시간이 지나면 다시 예약이 가능하다`() {
        //given
        chargePoint(accessToken)
        val request = ReservationMakeRequest(
            LocalDate.of(2025, 12, 1),
            10
        )
        val reservationResponse = makeReservation(request, accessToken, waitingToken) // 예약

        validateReservationInRepo(
            reservationResponse.id,
            LocalDate.of(2025, 12, 1),
            10,
            ReservationStatus.PENDING
        )

        tempReservationAdaptor.cleanupExpiredReservation(reservationResponse.id) // 예약 만료 작동

        validateReservationInRepo(
            reservationResponse.id,
            LocalDate.of(2025, 12, 1),
            10,
            ReservationStatus.CANCEL
        )

        //when

        val retryReservationResponse = makeReservation(request, accessToken, waitingToken) // 예약 다시 생성
        validateReservationInRepo(
            retryReservationResponse.id,
            LocalDate.of(2025, 12, 1),
            10,
            ReservationStatus.PENDING
        )

        payReservation(retryReservationResponse.id, accessToken, waitingToken)

        //then
        validateReservationInRepo(
            retryReservationResponse.id,
            LocalDate.of(2025, 12, 1),
            10,
            ReservationStatus.RESERVE
        )
    }

    @Test
    fun `여러 유저가 동시에 좌석을 요청해도 한 명만 성공한다`(){
        //given
        val (userB_AccessToken, userB_WaitingToken) = getAccessAndWaitingToken("testUser2") // 다른 유저 로그인
        chargePoint(accessToken)
        chargePoint(userB_AccessToken)

        val request = ReservationMakeRequest(
            LocalDate.of(2025, 12, 1),
            10
        )

        makeReservation(request, accessToken, waitingToken)

        //when & then
        mockMvc.perform(
            post("/api/reservation") // 예약 생성
                .header("Authorization", "Bearer $userB_AccessToken")
                .header("X-Waiting-Token", "Bearer $userB_WaitingToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isConflict)

    }

    private fun payReservation(reservationId : Long, access: String, waiting: String) {
        val reservation = reservationRepository.findById(reservationId).get()

        mockMvc.perform(
            post("/api/reservation/pay/$reservationId") // 임시 예약 결제
                .header("Authorization", "Bearer $access")
                .header("X-Waiting-Token", "Bearer $waiting")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.date").value(reservation.date.toString()))
            .andExpect(jsonPath("$.seatNumber").value(reservation.seatNumber.toString()))
    }

    private fun makeReservation(request: ReservationMakeRequest, access: String, waiting: String): ReservationResponse {
        val reservation = mockMvc.perform(
            post("/api/reservation") // 예약 생성
                .header("Authorization", "Bearer $access")
                .header("X-Waiting-Token", "Bearer $waiting")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.date").value(request.date.toString()))
            .andExpect(jsonPath("$.seatNumber").value(request.seatNumber.toString()))
            .andReturn()

        outboxScheduler.schedule() // 강제 스케줄링 실행

        val reservationResponse =
            objectMapper.readValue(reservation.response.contentAsString, ReservationResponse::class.java)

        return reservationResponse
    }

    private fun validateReservationInRepo(id: Long, date: LocalDate, seatNumber: Int, status: ReservationStatus) {
        val reservationInRepo = reservationRepository.findById(id)
        assertThat(reservationInRepo).isNotEmpty
        assertThat(reservationInRepo.get().id).isEqualTo(id)
        assertThat(reservationInRepo.get().date).isEqualTo(date)
        assertThat(reservationInRepo.get().status).isEqualTo(status)
        assertThat(reservationInRepo.get().seatNumber).isEqualTo(seatNumber)
    }

    private fun chargePoint(token: String) {
        val chargeReq = UsePointReq(
            10000
        )

        mockMvc.perform(
            post("/api/point/charge")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chargeReq))
        )
    }


}
