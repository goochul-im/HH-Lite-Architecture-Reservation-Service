package kr.hhplus.be.server.config.redis

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.GenericToStringSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig(
    @param:Value("\${spring.data.redis.host}")
    private val host: String,
    @param:Value("\${spring.data.redis.port}")
    private val port: Int
) {

    @Bean
    fun redisJsonTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory

        // Key는 LocalDate 문자열로 저장
        template.keySerializer = StringRedisSerializer()
        // Value는 객체를 JSON 문자열로 변환하여 저장
        template.valueSerializer = GenericJackson2JsonRedisSerializer()

        // Hash Key/Value도 String과 JSON으로 설정하는 것이 일반적
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = GenericJackson2JsonRedisSerializer()

        template.afterPropertiesSet()
        return template
    }

    @Bean
    fun redisMessageListenerContainer(
        connectionFactory: RedisConnectionFactory // Spring Boot가 자동 등록한 연결 팩토리를 주입받음
    ): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        // Redis와의 연결 관리를 위해 ConnectionFactory를 설정합니다.
        container.setConnectionFactory(connectionFactory)

        // (선택 사항) 리스너 스레드 풀 설정을 통해 성능을 조정할 수 있습니다.
        // container.setTaskExecutor(Executors.newFixedThreadPool(4))

        return container
    }

}
