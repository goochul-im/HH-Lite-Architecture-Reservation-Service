package kr.hhplus.be.server.exception

import kr.hhplus.be.server.outbox.exception.OutboxException
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ExceptionAdvice {

    private val log = KotlinLogging.logger { }

    @ExceptionHandler
    fun exceptionhandler(e: ResourceNotFoundException): ResponseEntity<*> {
        log.error { "예외 발생 ${e.message}" }
        return errorResponse(HttpStatus.NOT_FOUND, e.message.toString())
    }

    @ExceptionHandler
    fun exceptionhandler(e: WaitingQueueException): ResponseEntity<*> {
        log.error { "예외 발생 ${e.message}" }
        return errorResponse(HttpStatus.NOT_ACCEPTABLE, e.message.toString())
    }

    @ExceptionHandler
    fun exceptionhandler(e: OutboxException): ResponseEntity<*> {
        log.error { "예외 발생 ${e.message}" }
        return errorResponse(HttpStatus.NOT_FOUND, e.message.toString())
    }

    @ExceptionHandler
    fun exceptionhandler(e: DuplicateResourceException): ResponseEntity<*> {
        log.error { "예외 발생 ${e.message}" }
        return errorResponse(HttpStatus.CONFLICT, e.message.toString())
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
