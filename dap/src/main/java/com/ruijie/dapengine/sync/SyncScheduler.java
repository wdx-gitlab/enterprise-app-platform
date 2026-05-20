package com.ruijie.dapengine.sync;

/**
 * 定时同步任务调度器接口（Phase 4 实现）。
 * apply-schema 成功后，若该 Subject 存在启用的定时同步配置，则调用此接口重新注册任务。
 * <p>
 * Phase 3 阶段此 Bean 不存在，{@code DapEngineSchemaInitializer} 通过
 * {@code @Autowired(required=false)} 注入，为 null 时静默跳过。
 * </p>
 */
public interface SyncScheduler {

    /**
     * 重新注册指定 Subject 的定时同步任务。
     *
     * @param subjectCode Subject code（如 CUSTOMER）
     */
    void reschedule(String subjectCode);
}
