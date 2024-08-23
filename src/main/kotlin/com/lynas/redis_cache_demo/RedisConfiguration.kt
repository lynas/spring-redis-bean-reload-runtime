package com.lynas.redis_cache_demo

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.support.NoOpCacheManager
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.data.redis.connection.jedis.JedisConnection
import org.springframework.stereotype.Component


@RefreshScope
@Component

class RedisConfiguration(
    val redisConfig: RedisConfig,
) {

    companion object {
        const val DEMO_INFORMATION_CACHE = "demoInformationCache"
    }

    @Bean("cacheManager")
    fun redisCacheManager(
        jedisConnectionFactory: JedisConnectionFactory,
        redisTemplate: RedisTemplate<String, Any>
    ): CacheManager {
        return createCacheManager(jedisConnectionFactory, redisTemplate)
    }

    fun createCacheManager(
        jedisConnectionFactory: JedisConnectionFactory,
        redisTemplate: RedisTemplate<String, Any>
    ): CacheManager {
        return try {
            val redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(Duration.ofDays(1))
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(redisTemplate.valueSerializer)
                )
            if (jedisConnectionFactory.connection !is JedisConnection) {
                throw RuntimeException("No connection to redis")
            }
            println("[Cache] Redis available, injecting TTL cache: enabled")
            RedisCacheManager
                .RedisCacheManagerBuilder.fromConnectionFactory(jedisConnectionFactory)
                .cacheDefaults(redisCacheConfiguration)
                .build()
        } catch (ex: Exception) {
            NoOpCacheManager()
        }
    }

    @Bean
    fun redisConnectionFactory(redisStandaloneConfiguration: RedisStandaloneConfiguration): JedisConnectionFactory {
        val jedisConnectionFactory = JedisConnectionFactory(redisStandaloneConfiguration)
        jedisConnectionFactory.afterPropertiesSet()
        return jedisConnectionFactory
    }

    @Bean
    fun redisStandaloneConfiguration(): RedisStandaloneConfiguration {
        val config = RedisStandaloneConfiguration(redisConfig.hostName, redisConfig.port)
        if (redisConfig.password.isNotBlank()) {
            config.password = RedisPassword.of(redisConfig.password)
        }
        return config
    }

    @Bean
    @Primary
    fun redisTemplate(jedisConnectionFactory: JedisConnectionFactory): RedisTemplate<String, Any> {
        return try {
            val template = RedisTemplate<String, Any>()
            val objectMapper = jacksonObjectMapper()
            objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY)
            objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder().build(),
                ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE
            )
            val stringSerializer = StringRedisSerializer()
            template.keySerializer = stringSerializer
//        template.valueSerializer = valueSerializer
            template.hashKeySerializer = stringSerializer
//        template.hashValueSerializer = valueSerializer
            template.connectionFactory = jedisConnectionFactory
            template
        } catch (ex: Exception) {
            return RedisTemplate<String, Any>()
        }

    }
}

@ConfigurationProperties("app.redis")
data class RedisConfig(
    val hostName: String = "",
    val port: Int = 0,
    val password: String = ""
)
