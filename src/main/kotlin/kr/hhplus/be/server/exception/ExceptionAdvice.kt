package kr.hhplus.be.server.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ExceptionAdvice {

    @ExceptionHandler
    fun exceptionhandler(e: ResourceNotFoundException): ResponseEntity<*> {
        return errorResponse(HttpStatus.NOT_FOUND, e.message.toString())
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
