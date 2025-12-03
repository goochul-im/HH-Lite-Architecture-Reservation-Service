package kr.hhplus.be.server.common.util

import kr.hhplus.be.server.common.port.TimeUtil
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
class TimeProvider : TimeUtil {

    override fun nowDateTime(): LocalDateTime {
        return LocalDateTime.now()
    }
}
