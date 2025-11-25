package kr.hhplus.be.server.domain.reservation

import kr.hhplus.be.server.TestcontainersConfiguration
import kr.hhplus.be.server.reservation.infrastructure.ReservationEntity
import kr.hhplus.be.server.reservation.infrastructure.ReservationJpaRepository
import kr.hhplus.be.server.reservation.infrastructure.ReservationStatus
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // DB 교체 방지
@Import(TestcontainersConfiguration::class)
class ReservationRepositoryTest {

    @Autowired
    lateinit var reservationJpaRepository: ReservationJpaRepository

    @Test
    fun `해당 날짜의 예약된 좌석번호들을 가져올 수 있다`(){
        //given
        for (i in 1..5) {
            reservationJpaRepository.save(
                ReservationEntity(
                    date = LocalDate.of(2021, 1, 1),
                    seatNumber = i,
                    status = ReservationStatus.RESERVE,
                    reserver = null
                )
            )
        }
        reservationJpaRepository.save(
            ReservationEntity(
                date = LocalDate.of(2021, 1, 2),
                seatNumber = 10,
                status = ReservationStatus.RESERVE,
                reserver = null
            )
        )

        //when
        val result = reservationJpaRepository.getReservedSeatNumber(LocalDate.of(2021, 1, 1))

        //then
        assertThat(result.size).isEqualTo(5)
        assertThat(result).containsExactlyInAnyOrder(1,2,3,4,5)
    }

}
