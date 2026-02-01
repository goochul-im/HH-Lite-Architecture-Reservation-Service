package kr.hhplus.be.server.external

import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class MockDataFlatform : DataFlatformPort {

    private val log = KotlinLogging.logger { }

    override fun transferData(reservation: CreateReservationInfo) {
        log.info { "Mock Transfer Success" }
    }
}
