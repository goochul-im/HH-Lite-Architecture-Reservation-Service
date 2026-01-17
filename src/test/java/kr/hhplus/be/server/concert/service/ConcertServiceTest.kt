package kr.hhplus.be.server.concert.service

import kr.hhplus.be.server.concert.domain.Concert
import kr.hhplus.be.server.concert.dto.ConcertCreateRequest
import kr.hhplus.be.server.concert.port.ConcertRepository
import kr.hhplus.be.server.exception.DuplicateResourceException
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
import org.mockito.kotlin.verify
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ConcertServiceTest {

    @Mock
    private lateinit var concertRepository: ConcertRepository

    private lateinit var concertService: ConcertService

    @BeforeEach
    fun setUp() {
        concertService = ConcertService(concertRepository)
    }

    @Test
    @DisplayName("콘서트 생성 테스트")
    fun `콘서트를 생성할 수 있다`() {
        // Given
        val request = ConcertCreateRequest(
            name = "테스트 콘서트",
            date = LocalDate.of(2025, 12, 25),
            totalSeats = 50
        )
        val savedConcert = Concert(
            id = 1L,
            name = request.name,
            date = request.date,
            totalSeats = request.totalSeats
        )

        given(concertRepository.findByDate(request.date)).willReturn(null)
        given(concertRepository.save(any())).willReturn(savedConcert)

        // When
        val result = concertService.create(request)

        // Then
        assertThat(result.id).isEqualTo(1L)
        assertThat(result.name).isEqualTo("테스트 콘서트")
        assertThat(result.date).isEqualTo(LocalDate.of(2025, 12, 25))
        assertThat(result.totalSeats).isEqualTo(50)

        verify(concertRepository).findByDate(request.date)
        verify(concertRepository).save(any())
    }

    @Test
    @DisplayName("같은 날짜에 콘서트가 이미 존재하면 예외를 던진다")
    fun `같은 날짜에 콘서트가 존재하면 DuplicateResourceException을 던진다`() {
        // Given
        val date = LocalDate.of(2025, 12, 25)
        val request = ConcertCreateRequest(
            name = "테스트 콘서트",
            date = date,
            totalSeats = 50
        )
        val existingConcert = Concert(
            id = 1L,
            name = "기존 콘서트",
            date = date,
            totalSeats = 50
        )

        given(concertRepository.findByDate(date)).willReturn(existingConcert)

        // When & Then
        assertThatThrownBy { concertService.create(request) }
            .isInstanceOf(DuplicateResourceException::class.java)
            .hasMessageContaining("해당 날짜에 이미 콘서트가 존재합니다")
    }

    @Test
    @DisplayName("콘서트 ID로 조회할 수 있다")
    fun `콘서트 ID로 조회할 수 있다`() {
        // Given
        val concertId = 1L
        val concert = Concert(
            id = concertId,
            name = "테스트 콘서트",
            date = LocalDate.of(2025, 12, 25),
            totalSeats = 50
        )

        given(concertRepository.findById(concertId)).willReturn(concert)

        // When
        val result = concertService.findById(concertId)

        // Then
        assertThat(result.id).isEqualTo(concertId)
        assertThat(result.name).isEqualTo("테스트 콘서트")
        assertThat(result.date).isEqualTo(LocalDate.of(2025, 12, 25))
        assertThat(result.totalSeats).isEqualTo(50)

        verify(concertRepository).findById(concertId)
    }

    @Test
    @DisplayName("예약 가능한 콘서트 목록을 조회할 수 있다")
    fun `예약 가능한 콘서트 목록을 조회할 수 있다`() {
        // Given
        val concerts = listOf(
            Concert(id = 1L, name = "콘서트1", date = LocalDate.now().plusDays(1), totalSeats = 50),
            Concert(id = 2L, name = "콘서트2", date = LocalDate.now().plusDays(2), totalSeats = 100),
            Concert(id = 3L, name = "콘서트3", date = LocalDate.now().plusDays(3), totalSeats = 30)
        )

        given(concertRepository.findAllAvailable(any())).willReturn(concerts)

        // When
        val result = concertService.findAvailableConcerts()

        // Then
        assertThat(result).hasSize(3)
        assertThat(result[0].name).isEqualTo("콘서트1")
        assertThat(result[1].name).isEqualTo("콘서트2")
        assertThat(result[2].name).isEqualTo("콘서트3")

        verify(concertRepository).findAllAvailable(any())
    }

    @Test
    @DisplayName("예약 가능한 콘서트가 없으면 빈 목록을 반환한다")
    fun `예약 가능한 콘서트가 없으면 빈 목록을 반환한다`() {
        // Given
        given(concertRepository.findAllAvailable(any())).willReturn(emptyList())

        // When
        val result = concertService.findAvailableConcerts()

        // Then
        assertThat(result).isEmpty()

        verify(concertRepository).findAllAvailable(any())
    }
}
