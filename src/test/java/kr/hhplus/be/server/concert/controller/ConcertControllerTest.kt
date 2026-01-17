package kr.hhplus.be.server.concert.controller

import kr.hhplus.be.server.concert.domain.Concert
import kr.hhplus.be.server.concert.dto.ConcertCreateRequest
import kr.hhplus.be.server.concert.service.ConcertService
import kr.hhplus.be.server.exception.DuplicateResourceException
import kr.hhplus.be.server.exception.ResourceNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.springframework.http.HttpStatus
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ConcertControllerTest {

    @Mock
    private lateinit var concertService: ConcertService

    private lateinit var concertController: ConcertController

    @BeforeEach
    fun setUp() {
        concertController = ConcertController(concertService)
    }

    @Test
    @DisplayName("콘서트 생성 API 테스트")
    fun `콘서트를 생성할 수 있다`() {
        // Given
        val request = ConcertCreateRequest(
            name = "테스트 콘서트",
            date = LocalDate.of(2025, 12, 25),
            totalSeats = 50
        )
        val concert = Concert(
            id = 1L,
            name = request.name,
            date = request.date,
            totalSeats = request.totalSeats
        )

        given(concertService.create(any())).willReturn(concert)

        // When
        val response = concertController.create(request)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.id).isEqualTo(1L)
        assertThat(response.body!!.name).isEqualTo("테스트 콘서트")
        assertThat(response.body!!.date).isEqualTo(LocalDate.of(2025, 12, 25))
        assertThat(response.body!!.totalSeats).isEqualTo(50)
    }

    @Test
    @DisplayName("중복 날짜 콘서트 생성 시 예외 발생")
    fun `중복 날짜에 콘서트 생성 시 DuplicateResourceException이 발생한다`() {
        // Given
        val request = ConcertCreateRequest(
            name = "테스트 콘서트",
            date = LocalDate.of(2025, 12, 25),
            totalSeats = 50
        )

        given(concertService.create(any())).willThrow(
            DuplicateResourceException("해당 날짜에 이미 콘서트가 존재합니다")
        )

        // When & Then
        assertThatThrownBy { concertController.create(request) }
            .isInstanceOf(DuplicateResourceException::class.java)
    }

    @Test
    @DisplayName("콘서트 단건 조회 API 테스트")
    fun `콘서트를 ID로 조회할 수 있다`() {
        // Given
        val concertId = 1L
        val concert = Concert(
            id = concertId,
            name = "테스트 콘서트",
            date = LocalDate.of(2025, 12, 25),
            totalSeats = 50
        )

        given(concertService.findById(concertId)).willReturn(concert)

        // When
        val response = concertController.findById(concertId)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.id).isEqualTo(concertId)
        assertThat(response.body!!.name).isEqualTo("테스트 콘서트")
        assertThat(response.body!!.date).isEqualTo(LocalDate.of(2025, 12, 25))
        assertThat(response.body!!.totalSeats).isEqualTo(50)
    }

    @Test
    @DisplayName("존재하지 않는 콘서트 조회 시 예외 발생")
    fun `존재하지 않는 콘서트 조회 시 ResourceNotFoundException이 발생한다`() {
        // Given
        val concertId = 999L

        given(concertService.findById(concertId)).willThrow(
            ResourceNotFoundException("콘서트를 찾을 수 없습니다: $concertId")
        )

        // When & Then
        assertThatThrownBy { concertController.findById(concertId) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    @DisplayName("예약 가능 콘서트 목록 조회 API 테스트")
    fun `예약 가능한 콘서트 목록을 조회할 수 있다`() {
        // Given
        val concerts = listOf(
            Concert(id = 1L, name = "콘서트1", date = LocalDate.of(2025, 12, 26), totalSeats = 50),
            Concert(id = 2L, name = "콘서트2", date = LocalDate.of(2025, 12, 27), totalSeats = 100)
        )

        given(concertService.findAvailableConcerts()).willReturn(concerts)

        // When
        val response = concertController.findAvailableConcerts()

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.concerts).hasSize(2)
        assertThat(response.body!!.concerts[0].id).isEqualTo(1L)
        assertThat(response.body!!.concerts[0].name).isEqualTo("콘서트1")
        assertThat(response.body!!.concerts[1].id).isEqualTo(2L)
        assertThat(response.body!!.concerts[1].name).isEqualTo("콘서트2")
    }

    @Test
    @DisplayName("예약 가능 콘서트가 없을 때 빈 목록 반환")
    fun `예약 가능한 콘서트가 없으면 빈 목록을 반환한다`() {
        // Given
        given(concertService.findAvailableConcerts()).willReturn(emptyList())

        // When
        val response = concertController.findAvailableConcerts()

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.concerts).isEmpty()
    }
}
