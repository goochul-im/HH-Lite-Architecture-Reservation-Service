package kr.hhplus.be.server.common.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ExceptionAdvice {

    @ExceptionHandler(SessionTokenNotFoundException::class)
    fun exceptionHandler(e: SessionTokenNotFoundException): ResponseEntity<Map<String,String>> {



        return errorResponse(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.")
    }

    private fun errorResponse(status : HttpStatus, message: String): ResponseEntity<Map<String, String>> = ResponseEntity
        .status(status)
        .body(
            mapOf(
                "status" to status.toString(),
                "message" to message
            )
        )

}
