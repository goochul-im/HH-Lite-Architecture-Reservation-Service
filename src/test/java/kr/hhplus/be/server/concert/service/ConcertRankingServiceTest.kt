package kr.hhplus.be.server.concert.service

import kr.hhplus.be.server.concert.domain.Concert
import kr.hhplus.be.server.concert.dto.ConcertRanking
import kr.hhplus.be.server.concert.port.ConcertRepository
import kr.hhplus.be.server.concert.port.ConcertSoldOutPort
import kr.hhplus.be.server.reservation.port.ReservationRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ConcertRankingServiceTest {

    @Mock
    private lateinit var soldOutPort: ConcertSoldOutPort

    @Mock
    private lateinit var concertRepository: ConcertRepository

    @Mock
    private lateinit var reservationRepository: ReservationRepository

    private lateinit var concertRankingService: ConcertRankingService

    @BeforeEach
    fun setUp() {
        concertRankingService = ConcertRankingService(
            soldOutPort,
            concertRepository,
            reservationRepository
        )
    }

    @Nested
    @DisplayName("checkAndMarkSoldOut 테스트")
    inner class CheckAndMarkSoldOutTest {

        @Test
        @DisplayName("이미 매진된 콘서트는 추가 처리하지 않는다")
        fun `이미 매진된 콘서트는 추가 처리하지 않는다`() {
            // Given
            val concertId = 1L
            given(soldOutPort.isSoldOut(concertId)).willReturn(true)

            // When
            concertRankingService.checkAndMarkSoldOut(concertId)

            // Then
            verify(soldOutPort).isSoldOut(concertId)
            verify(concertRepository, never()).findById(any())
            verify(reservationRepository, never()).countByConcert(any())
            verify(soldOutPort, never()).markSoldOut(any(), any())
        }

        @Test
        @DisplayName("예약 수가 총 좌석 수보다 많으면 매진으로 표시한다")
        fun `예약 수가 총 좌석 수보다 많으면 매진으로 표시한다`() {
            // Given
            val concertId = 1L
            val concert = Concert(
                id = concertId,
                name = "테스트 콘서트",
                date = LocalDate.of(2025, 12, 25),
                totalSeats = 50
            )

            given(soldOutPort.isSoldOut(concertId)).willReturn(false)
            given(concertRepository.findById(concertId)).willReturn(concert)
            given(reservationRepository.countByConcert(concert)).willReturn(51L)

            // When
            concertRankingService.checkAndMarkSoldOut(concertId)

            // Then
            verify(soldOutPort).isSoldOut(concertId)
            verify(concertRepository).findById(concertId)
            verify(reservationRepository).countByConcert(concert)
            verify(soldOutPort).markSoldOut(any(), any())
        }

        @Test
        @DisplayName("예약 수가 총 좌석 수 이하면 매진으로 표시하지 않는다")
        fun `예약 수가 총 좌석 수 이하면 매진으로 표시하지 않는다`() {
            // Given
            val concertId = 1L
            val concert = Concert(
                id = concertId,
                name = "테스트 콘서트",
                date = LocalDate.of(2025, 12, 25),
                totalSeats = 50
            )

            given(soldOutPort.isSoldOut(concertId)).willReturn(false)
            given(concertRepository.findById(concertId)).willReturn(concert)
            given(reservationRepository.countByConcert(concert)).willReturn(50L)

            // When
            concertRankingService.checkAndMarkSoldOut(concertId)

            // Then
            verify(soldOutPort).isSoldOut(concertId)
            verify(concertRepository).findById(concertId)
            verify(reservationRepository).countByConcert(concert)
            verify(soldOutPort, never()).markSoldOut(any(), any())
        }

        @Test
        @DisplayName("예약이 없으면 매진으로 표시하지 않는다")
        fun `예약이 없으면 매진으로 표시하지 않는다`() {
            // Given
            val concertId = 1L
            val concert = Concert(
                id = concertId,
                name = "테스트 콘서트",
                date = LocalDate.of(2025, 12, 25),
                totalSeats = 50
            )

            given(soldOutPort.isSoldOut(concertId)).willReturn(false)
            given(concertRepository.findById(concertId)).willReturn(concert)
            given(reservationRepository.countByConcert(concert)).willReturn(0L)

            // When
            concertRankingService.checkAndMarkSoldOut(concertId)

            // Then
            verify(soldOutPort, never()).markSoldOut(any(), any())
        }
    }

    @Nested
    @DisplayName("getRanking 테스트")
    inner class GetRankingTest {

        @Test
        @DisplayName("매진 콘서트 랭킹을 조회할 수 있다")
        fun `매진 콘서트 랭킹을 조회할 수 있다`() {
            // Given
            val topN = 3
            val rankings = listOf(
                ConcertRanking(1L, 1000L),
                ConcertRanking(2L, 2000L),
                ConcertRanking(3L, 3000L)
            )
            val concerts = listOf(
                Concert(1L, "콘서트1", LocalDate.of(2025, 12, 25), 50),
                Concert(2L, "콘서트2", LocalDate.of(2025, 12, 26), 50),
                Concert(3L, "콘서트3", LocalDate.of(2025, 12, 27), 50)
            )

            given(soldOutPort.getTopN(topN)).willReturn(rankings)
            given(concertRepository.findById(1L)).willReturn(concerts[0])
            given(concertRepository.findById(2L)).willReturn(concerts[1])
            given(concertRepository.findById(3L)).willReturn(concerts[2])

            // When
            val result = concertRankingService.getRanking(topN)

            // Then
            assertThat(result).hasSize(3)
            assertThat(result[0].concertId).isEqualTo(1L)
            assertThat(result[0].concertName).isEqualTo("콘서트1")
            assertThat(result[0].rank).isEqualTo(1L)

            assertThat(result[1].concertId).isEqualTo(2L)
            assertThat(result[1].concertName).isEqualTo("콘서트2")
            assertThat(result[1].rank).isEqualTo(2L)

            assertThat(result[2].concertId).isEqualTo(3L)
            assertThat(result[2].concertName).isEqualTo("콘서트3")
            assertThat(result[2].rank).isEqualTo(3L)
        }

        @Test
        @DisplayName("매진된 콘서트가 없으면 빈 목록을 반환한다")
        fun `매진된 콘서트가 없으면 빈 목록을 반환한다`() {
            // Given
            val topN = 10
            given(soldOutPort.getTopN(topN)).willReturn(emptyList())

            // When
            val result = concertRankingService.getRanking(topN)

            // Then
            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("요청한 수보다 적은 매진 콘서트가 있으면 있는 만큼만 반환한다")
        fun `요청한 수보다 적은 매진 콘서트가 있으면 있는 만큼만 반환한다`() {
            // Given
            val topN = 10
            val rankings = listOf(
                ConcertRanking(1L, 1000L),
                ConcertRanking(2L, 2000L)
            )
            val concerts = listOf(
                Concert(1L, "콘서트1", LocalDate.of(2025, 12, 25), 50),
                Concert(2L, "콘서트2", LocalDate.of(2025, 12, 26), 50)
            )

            given(soldOutPort.getTopN(topN)).willReturn(rankings)
            given(concertRepository.findById(1L)).willReturn(concerts[0])
            given(concertRepository.findById(2L)).willReturn(concerts[1])

            // When
            val result = concertRankingService.getRanking(topN)

            // Then
            assertThat(result).hasSize(2)
        }
    }
}
