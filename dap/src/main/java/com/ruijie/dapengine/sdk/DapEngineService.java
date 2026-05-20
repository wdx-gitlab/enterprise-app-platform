package com.ruijie.dapengine.sdk;

/**
 * DAP Engine 业务系统主数据服务门面接口。
 *
 * <p>业务系统将此接口注入到自身 Service，通过 {@link #query()} 执行主数据查询，
 * 通过 {@link #meta()} 获取主题与字段元数据，通过 {@link #sync()} 手动触发同步，
 * 无需关心底层缓存或 SQL 细节。</p>
 *
 * <p>在 Spring Boot Starter 自动配置后，此接口有默认实现 {@code DapEngineServiceImpl}，
 * 可通过 {@code @ConditionalOnMissingBean} 替换。</p>
 */
public interface DapEngineService {

    /**
     * 获取主数据查询子服务，支持 getByCode / query / search / getTree。
     */
    MasterDataQueryService query();

    /**
     * 获取主数据元数据子服务，支持主题 / 字段 / schema 状态查询。
     */
    MasterDataMetaService meta();

    /**
     * 获取主数据同步子服务，支持手动触发 DELTA / FULL_REFRESH。
     */
    MasterDataSyncService sync();
}
