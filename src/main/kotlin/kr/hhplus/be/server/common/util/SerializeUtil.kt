package kr.hhplus.be.server.common.util

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper

private val objectMapper: ObjectMapper = ObjectMapper()

object SerializeUtil {

    fun outboxMapToString(map: Map<String, Any>): String {
        return objectMapper.writeValueAsString(map)
    }

    fun outboxStringToMap(string: String): Map<String, Any> {
        return objectMapper.readValue(string, object : TypeReference<Map<String, Any>>() {})
    }

}
