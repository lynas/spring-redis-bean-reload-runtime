package com.lynas.redis_cache_demo

import com.lynas.redis_cache_demo.RedisConfiguration.Companion.DEMO_INFORMATION_CACHE
import java.util.Date
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
@EnableCaching
@ConfigurationPropertiesScan
@EnableScheduling
class RedisCacheDemoApplication {

    @Scheduled(fixedDelay = 5_000)
    fun refreshCacheManager() {
        val currentCacheManager = springContext.getBean(CacheManager::class.java)
        val currentCacheManagerStr = getCacheManagerName(currentCacheManager)
        val jedisConnectionFactory = springContext.getBean(JedisConnectionFactory::class.java)
        val redisTemplate = springContext.getBean(RedisTemplate::class.java) as RedisTemplate<String, Any>
        val newCacheManager = (springContext.getBean(RedisConfiguration::class.java)
            .createCacheManager(jedisConnectionFactory, redisTemplate))
        val newCacheManagerStr = getCacheManagerName(newCacheManager)
        println("currentCacheManagerStr = $currentCacheManagerStr , newCacheManagerStr = $newCacheManagerStr")
        if (currentCacheManagerStr != newCacheManagerStr) {
            restartApplication()
        }
    }

    fun restartApplication() {
        val restartThread = Thread {
            try {
                Thread.sleep(1_000)
                restart()
            } catch (exception: InterruptedException) {
                exception.printStackTrace()
            }
        }
        restartThread.isDaemon = false
        restartThread.start()
    }

    fun getCacheManagerName(cacheManager: CacheManager): String {
        return if (cacheManager is RedisCacheManager) {
            "RedisCacheManager"
        } else {
            "NoOpCacheManager"
        }
    }
}

private lateinit var appArgs: Array<String>
private lateinit var springContext: ConfigurableApplicationContext
fun main(args: Array<String>) {
    appArgs = args
    springContext = runApplication<RedisCacheDemoApplication>(*args)
}

fun restart() {
    springContext.close()
    springContext = runApplication<RedisCacheDemoApplication>(*appArgs)
}

@RestController
@RequestMapping("/redis")
class DemoController(
    val demoCacheService: DemoCacheService
) {
    @GetMapping("/{time}")
    fun demo(@PathVariable time: String) = "Hello Redis ${demoCacheService.demo(time)}";
}

@Service("democacheservice")
class DemoCacheService {

    @Cacheable(
        cacheNames = [DEMO_INFORMATION_CACHE],
        key = "#time"
    )
    fun demo(time: String): String {
        return Date().toString();
    }
}