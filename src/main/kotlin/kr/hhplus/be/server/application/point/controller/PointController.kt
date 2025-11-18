package kr.hhplus.be.server.application.point.controller

import kr.hhplus.be.server.application.point.service.PointService
import org.apache.catalina.User
import org.apache.coyote.Response
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/point")
class PointController(
    private val pointService: PointService
) {

    @GetMapping
    fun inquiry(@AuthenticationPrincipal user: User): ResponseEntity<*> {
        return ResponseEntity.ok(mapOf(
            "point" to pointService.inquiry(user.username)
        ))
    }

    @PostMapping("/charge")
    fun charge(
        @AuthenticationPrincipal user: User,
        @RequestBody req: UsePointReq
    ) : ResponseEntity<*> {
        return ResponseEntity.ok(mapOf(
            "point" to pointService.charge(user.username, req.point)
        ))
    }

}

data class UsePointReq(
    val point: Int
)

