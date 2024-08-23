package com.lynas.redis_cache_demo

import com.lynas.redis_cache_demo.RedisConfiguration.Companion.DEMO_INFORMATION_CACHE
import jakarta.websocket.server.PathParam
import java.util.Date
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cloud.context.refresh.ContextRefresher
import org.springframework.context.ApplicationContext
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
class RedisCacheDemoApplication{

	@Autowired
	lateinit var applicationContext:ApplicationContext ;
	@Autowired
	lateinit var context: ContextRefresher;

	@Scheduled(fixedRateString = "PT10S")
	fun refreshCacheManager() {
		val jedisConnectionFactory = applicationContext.getBean(JedisConnectionFactory::class.java)
		val demoCacheService = applicationContext.getBean(DemoCacheService::class.java)
		val redisTemplate = applicationContext.getBean(RedisTemplate::class.java) as RedisTemplate<String, Any>

		val cacheManager = (applicationContext.getBean(RedisConfiguration::class.java)
			.createCacheManager(jedisConnectionFactory, redisTemplate))


		val configurableApplicationContext = applicationContext as ConfigurableApplicationContext
		val beanFactory = configurableApplicationContext.beanFactory as DefaultListableBeanFactory
		beanFactory.destroySingleton("democacheservice")
		beanFactory.destroySingleton("cacheManager")

		configurableApplicationContext.beanFactory.registerSingleton("cacheManager", cacheManager)
		configurableApplicationContext.beanFactory.registerSingleton("democacheservice", demoCacheService)
		context.refresh()

		println("CacheManager has been refreshed")
	}
}

fun main(args: Array<String>) {
	runApplication<RedisCacheDemoApplication>(*args)
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
class DemoCacheService{

	@Cacheable(
		cacheNames = [DEMO_INFORMATION_CACHE],
		key = "#time"
	)
	fun demo(time:String) : String{
		return Date().toString();
	}
}