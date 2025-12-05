package kr.hhplus.be.server.reservation.dto

import io.lettuce.core.KillArgs.Builder.id
import java.time.LocalDate

data class TempReservationPayload(
    val id: Long,
    val date: LocalDate,
    val seatNumber: Int
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "date" to date.toString(),
            "id" to id,
            "seatNumber" to seatNumber
        )
    }
}
