package kr.hhplus.be.server.common.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SerializeUtilTest {

    @Test
    fun `outboxMapToString으로 직렬화를 할 수 있다`() {
        // Given
        val map = mapOf("key1" to "value1", "key2" to 123)
        val expectedJsonString = """{"key1":"value1","key2":123}"""

        // When
        val jsonString = SerializeUtil.outboxMapToString(map)

        // Then
        assertEquals(expectedJsonString, jsonString)
    }

    @Test
    fun `outboxStringToMap으로 역직렬화를 할 수 있다`() {
        // Given
        val jsonString = """{"key1":"value1","key2":123}"""
        val expectedMap = mapOf("key1" to "value1", "key2" to 123)

        // When
        val map = SerializeUtil.outboxStringToMap(jsonString)

        // Then
        assertEquals(expectedMap, map)
    }
}

