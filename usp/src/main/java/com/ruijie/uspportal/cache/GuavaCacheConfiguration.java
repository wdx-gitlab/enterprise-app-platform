package com.ruijie.uspportal.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Configuration
public class GuavaCacheConfiguration {

    @Bean
    public Cache<String, Object> portalGuavaCache() {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(1024)
                .build();
    }

    @Service
    public static class GuavaPortalCacheService implements PortalCacheService {

        private final Cache<String, Object> cache;

        public GuavaPortalCacheService(Cache<String, Object> cache) {
            this.cache = cache;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(String key, Class<T> type) {
            Object value = cache.getIfPresent(key);
            return value == null ? null : (T) value;
        }

        @Override
        public void put(String key, Object value) {
            cache.put(key, value);
        }

        @Override
        public void evict(String key) {
            cache.invalidate(key);
        }
    }
}
