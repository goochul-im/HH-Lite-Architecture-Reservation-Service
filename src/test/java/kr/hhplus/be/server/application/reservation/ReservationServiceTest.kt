package kr.hhplus.be.server.application.reservation

import kr.hhplus.be.server.application.reservation.dto.ReservationRequest
import kr.hhplus.be.server.auth.AuthService
import kr.hhplus.be.server.domain.member.Member
import kr.hhplus.be.server.domain.reservation.Reservation
import kr.hhplus.be.server.domain.reservation.ReservationRepository
import kr.hhplus.be.server.domain.reservation.ReservationStatus
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.*
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ReservationServiceTest {

    @Mock
    private lateinit var reservationRepository: ReservationRepository

    @Mock
    private lateinit var tempReservationComponent: TempReservationComponent

    @Mock
    private lateinit var authService: AuthService

    @InjectMocks
    private lateinit var reservationService: ReservationService

    @Test
    @DisplayName("예약 생성 테스트")
    fun make_reservation_test() {
        // Given
        val memberId = "testmember"
        val date = LocalDate.now()
        val seatNumber = 5
        val request = ReservationRequest(date, memberId, seatNumber)

        val member = Member(id = memberId, username = "Test User", password = "testpassword" )
        val reservation = Reservation(
            id = 1L,
            date = date,
            seatNumber = seatNumber,
            status = ReservationStatus.PENDING,
            reserver = member
        )

        given(authService.getById(memberId)).willReturn(member)
        given(reservationRepository.save(any())).willReturn(reservation)

        // When
        reservationService.make(request)

        // Then
        verify(authService).getById(memberId)
        verify(reservationRepository).save(any())
        verify(tempReservationComponent).save(date, reservation.id!!, seatNumber)
    }
}
