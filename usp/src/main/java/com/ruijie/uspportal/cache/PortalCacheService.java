package com.ruijie.uspportal.cache;

/**
 * 门户缓存服务。
 *
 * <p>抽象门户运行时所需的缓存读写能力，便于在 Caffeine、Redis 等实现之间切换。</p>
 */
public interface PortalCacheService {

    /**
     * 按键读取缓存值。
     *
     * @param key 缓存键
     * @param type 目标类型
     * @param <T> 返回值类型
     * @return 缓存值
     */
    <T> T get(String key, Class<T> type);

    /**
     * 写入缓存值。
     *
     * @param key 缓存键
     * @param value 缓存值
     */
    void put(String key, Object value);

    /**
     * 删除指定缓存键。
     *
     * @param key 缓存键
     */
    void evict(String key);
}
