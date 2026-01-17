package kr.hhplus.be.server.domain.reservation

import kr.hhplus.be.server.TestcontainersConfiguration
import kr.hhplus.be.server.concert.infrastructure.ConcertEntity
import kr.hhplus.be.server.concert.infrastructure.ConcertJpaRepository
import kr.hhplus.be.server.reservation.infrastructure.ReservationEntity
import kr.hhplus.be.server.reservation.infrastructure.ReservationJpaRepository
import kr.hhplus.be.server.reservation.infrastructure.ReservationStatus
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration::class)
class ReservationRepositoryTest {

    @Autowired
    lateinit var reservationJpaRepository: ReservationJpaRepository

    @Autowired
    lateinit var concertJpaRepository: ConcertJpaRepository

    private lateinit var testConcert1: ConcertEntity
    private lateinit var testConcert2: ConcertEntity

    @BeforeEach
    fun setUp() {
        reservationJpaRepository.deleteAll()
        concertJpaRepository.deleteAll()

        testConcert1 = concertJpaRepository.save(
            ConcertEntity(name = "테스트 콘서트 1", date = LocalDate.of(2021, 1, 1), totalSeats = 50)
        )
        testConcert2 = concertJpaRepository.save(
            ConcertEntity(name = "테스트 콘서트 2", date = LocalDate.of(2021, 1, 2), totalSeats = 50)
        )
    }

    @Test
    fun `해당 콘서트의 예약된 좌석번호들을 가져올 수 있다`() {
        //given
        for (i in 1..5) {
            reservationJpaRepository.save(
                ReservationEntity(
                    concert = testConcert1,
                    seatNumber = i,
                    status = ReservationStatus.RESERVE,
                    reserver = null
                )
            )
        }
        reservationJpaRepository.save(
            ReservationEntity(
                concert = testConcert2,
                seatNumber = 10,
                status = ReservationStatus.RESERVE,
                reserver = null
            )
        )

        //when
        val result = reservationJpaRepository.getReservedSeatNumbers(testConcert1.id!!)

        //then
        assertThat(result.size).isEqualTo(5)
        assertThat(result).containsExactlyInAnyOrder(1, 2, 3, 4, 5)
    }

}
