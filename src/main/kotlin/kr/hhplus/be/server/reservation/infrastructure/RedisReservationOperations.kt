package kr.hhplus.be.server.reservation.infrastructure

interface RedisReservationOperations {

    /**
     * 임시 예약 정보를 Redis에 저장
     * - seatListKey: 만료 시간이 있는 좌석 정보
     * - reserveKey: 날짜별 예약된 좌석들
     */
    fun saveTempReservation(
        seatListKey: String,
        reserveKey: String,
        seatNumber: Int,
        timeoutSeconds: Long
    )

    /**
     * 특정 날짜의 임시 예약된 좌석 목록 조회
     */
    fun getTempReservedSeats(reserveKey: String): List<Int>

    /**
     * Redis에서 예약 정보 삭제
     */
    fun deleteReservation(seatListKey: String): Boolean

    /**
     * Redis에서 예약이 유효한지 확인
     */
    fun isReservationExists(seatListKey: String): Boolean

    /**
     * Redis Set에서 특정 좌석 제거
     */
    fun removeFromReserveSet(reserveKey: String, seatNumber: Int)
}
