package com.ruijie.dapengine.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

/**
 * 主数据缓存操作封装。
 *
 * <p>支持两种模式。
 * <ul>
 *   <li><b>Redis 模式</b>：当 {@code redisTemplate} 。null 时激活。
 *       使用 Redis Hash，key = {@code dap:cache:{subject}}，field = {@code code}，value = JSON。
 *       驱逐时 DEL 整个 hash key，禁止使。KEYS 命令。/li>
 *   <li><b>Caffeine 降级模式</b>：{@code redisTemplate} 。null 时激活。
 *       通过 {@code dapCacheManager} 。subject 创建独立 Cache 实例（名称 = {@code dap_{subject}}）。/li>
 * </ul>
 * </p>
 *
 * <p>所。cache 操作捕获异常并降级：缓存不可用时不影响正常读写数据库流程。/p>
 */
public class MasterDataCacheService {

    private static final Logger log = LoggerFactory.getLogger(MasterDataCacheService.class);
    private static final String KEY_PREFIX = "dap:cache:";
    private static final String CACHE_REGION_PREFIX = "dap_";

    private final RedisTemplate<String, String> redisTemplate;
    private final CacheManager dapCacheManager;
    private final ObjectMapper objectMapper;

    public MasterDataCacheService(RedisTemplate<String, String> redisTemplate,
                                   CacheManager dapCacheManager,
                                   ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.dapCacheManager = dapCacheManager;
        this.objectMapper = objectMapper;
    }

    /**
     * 读缓存。未命中或异常时返回 null，调用方需回源 DB。
     */
    public Map<String, Object> get(String subject, String code) {
        try {
            if (redisTemplate != null) {
                Object raw = redisTemplate.opsForHash().get(hashKey(subject), code);
                if (raw != null) {
                    return objectMapper.readValue(raw.toString(),
                            new TypeReference<Map<String, Object>>() {});
                }
                return null;
            }
            Cache cache = dapCacheManager.getCache(cacheRegion(subject));
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(code);
                if (wrapper != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> val = (Map<String, Object>) wrapper.get();
                    return val;
                }
            }
        } catch (Exception e) {
            log.warn("[DAP Cache] get failed for subject={}, code={}: {}", subject, code, e.getMessage());
        }
        return null;
    }

    /**
     * 写缓存。失败时仅打。WARN，不影响业务流程。
     */
    public void put(String subject, String code, Map<String, Object> value) {
        try {
            if (redisTemplate != null) {
                String json = objectMapper.writeValueAsString(value);
                redisTemplate.opsForHash().put(hashKey(subject), code, json);
                return;
            }
            Cache cache = dapCacheManager.getCache(cacheRegion(subject));
            if (cache != null) {
                cache.put(code, value);
            }
        } catch (Exception e) {
            log.warn("[DAP Cache] put failed for subject={}, code={}: {}", subject, code, e.getMessage());
        }
    }

    /**
     * 驱。subject 的全部缓存条目（整组 DEL / invalidateAll）。
     * 同步完成后由 SyncExecutor 调用。
     */
    public void evict(String subject) {
        try {
            if (redisTemplate != null) {
                redisTemplate.delete(hashKey(subject));
                log.debug("[DAP Cache] Redis hash evicted for subject={}", subject);
                return;
            }
            Cache cache = dapCacheManager.getCache(cacheRegion(subject));
            if (cache != null) {
                cache.invalidate();
                log.debug("[DAP Cache] Caffeine cache invalidated for subject={}", subject);
            }
        } catch (Exception e) {
            log.warn("[DAP Cache] evict failed for subject={}: {}", subject, e.getMessage());
        }
    }

    private static String hashKey(String subject) {
        return KEY_PREFIX + subject;
    }

    private static String cacheRegion(String subject) {
        return CACHE_REGION_PREFIX + subject;
    }
}
