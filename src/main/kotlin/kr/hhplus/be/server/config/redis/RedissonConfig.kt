package kr.hhplus.be.server.config.redis

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.redisson.config.Config
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedissonConfig(
    @param:Value("\${spring.data.redis.host}") private val host: String,
    @param:Value("\${spring.data.redis.port}") private val port: Int
) {

    @Bean
    fun redissonClient(): RedissonClient {
        val config = Config()
        config.useSingleServer().address = "redis://$host:$port"
        config.codec = StringCodec.INSTANCE
        return Redisson.create(config)
    }

}
