package com.fixedasset.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * 缓存配置。
 *
 * <p>开发和测试默认使用 JVM 内存缓存，保证未安装 Redis 时也能启动；生产 profile
 * 将 {@code app.cache.provider} 切换为 redis 后启用 RedisCacheManager。</p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 本地缓存实现不依赖外部中间件，适合开发和集成测试。
     */
    @Bean
    @ConditionalOnProperty(name = "app.cache.provider", havingValue = "memory", matchIfMissing = true)
    public CacheManager memoryCacheManager() {
        return new ConcurrentMapCacheManager("dashboardSummary", "assetCategoryList");
    }

    @Bean
    @ConditionalOnProperty(name = "app.cache.provider", havingValue = "redis")
    public CacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {
        // Redis 中的 value 使用 JSON 序列化，TTL 统一为 5 分钟。
        // 对统计类数据采用较短 TTL，业务写操作同时主动清理缓存。
        GenericJackson2JsonRedisSerializer valueSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(valueSerializer));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(configuration)
                .build();
    }
}
