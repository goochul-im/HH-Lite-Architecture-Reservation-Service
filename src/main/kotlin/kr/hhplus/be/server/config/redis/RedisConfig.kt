package kr.hhplus.be.server.config.redis

import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration


@Configuration
@EnableCaching
class RedisConfig(
    @param:Value("\${spring.data.redis.host}")
    private val host: String,
    @param:Value("\${spring.data.redis.port}")
    private val port: Int
) {

    @Bean
    fun stringRedisTemplate(cf: RedisConnectionFactory) : StringRedisTemplate {
        return StringRedisTemplate(cf)
    }

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

    @Bean("cacheManager")
    fun redisCacheManager(connectionFactory: RedisConnectionFactory): CacheManager {
        val config: RedisCacheConfiguration =
            RedisCacheConfiguration.defaultCacheConfig() // 1. 데이터 직렬화 설정 (JSON 형태로 저장)
                .serializeKeysWith(
                    RedisSerializationContext.SerializationPair.fromSerializer<String?>(
                        StringRedisSerializer()
                    )
                )
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer<Any?>(
                        GenericJackson2JsonRedisSerializer()
                    )
                ) // 2. 캐시 만료 시간(TTL) 설정 (예: 10분)
                .entryTtl(Duration.ofMinutes(10)) // 3. Null 값은 캐싱하지 않음 (선택)
                .disableCachingNullValues()

        return RedisCacheManager.RedisCacheManagerBuilder
            .fromConnectionFactory(connectionFactory)
            .cacheDefaults(config)
            .build()
    }

}
