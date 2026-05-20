package com.ruijie.uspportal.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 缓存配置。
 *
 * <p>当门户缓存类型为 Caffeine 时，负责创建本地缓存实例与默认缓存服务实现。</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "usp.portal.cache", name = "type", havingValue = "caffeine", matchIfMissing = true)
public class CaffeineCacheConfiguration {

    @Value("${usp.portal.cache.caffeine.expire-after-write-seconds:30}")
    private long expireAfterWriteSeconds;

    @Value("${usp.portal.cache.caffeine.maximum-size:1024}")
    private long maximumSize;

    @Bean
    /**
     * 创建门户本地 Caffeine 缓存实例。
     *
     * @return Caffeine 缓存对象
     */
    public Cache<String, Object> portalCaffeineCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(expireAfterWriteSeconds, TimeUnit.SECONDS)
                .maximumSize(maximumSize)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(PortalCacheService.class)
    /**
     * 创建默认门户缓存服务实现。
     *
     * @param cache Caffeine 缓存对象
     * @return 门户缓存服务实现
     */
    public PortalCacheService portalCacheService(Cache<String, Object> cache) {
        return new PortalCacheService() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T get(String key, Class<T> type) {
                Object value = cache.getIfPresent(key);
                return value == null ? null : (T) value;
            }

            @Override
            /**
             * 写入缓存值。
             *
             * @param key 缓存键
             * @param value 缓存值
             */
            public void put(String key, Object value) {
                cache.put(key, value);
            }

            @Override
            /**
             * 删除指定缓存键。
             *
             * @param key 缓存键
             */
            public void evict(String key) {
                cache.invalidate(key);
            }
        };
    }
}
