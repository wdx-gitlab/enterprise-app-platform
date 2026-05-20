package com.ruijie.dapengine.sync;

import com.ruijie.dapengine.common.enums.SyncAction;

import java.util.List;
import java.util.Map;

/**
 * 同步拦截器 SPI 接口（扩展点）。
 *
 * <p>宿主应用可实现此接口并注册为 Spring Bean，{@code SyncExecutor} 在每次同步前后
 * 依次调用所有拦截器（按 Spring Bean 注册顺序）。常见用途：数据脱敏、额外校验、审计日志。</p>
 *
 * <p>拦截器实现应保持幂等且无副作用；{@code beforeSync} 的返回值将替换原始记录集
 * 传递给后续步骤，可用于数据转换。</p>
 */
public interface SyncInterceptor {

    /**
     * 同步写入前调用，可对数据进行过滤、转换或校验。
     *
     * @param subjectCode Subject 编码
     * @param action      本次同步动作
     * @param records     经字段映射后的原始记录列表
     * @return 处理后的记录列表（可修改内容，不能返回 null）
     */
    List<Map<String, Object>> beforeSync(String subjectCode, SyncAction action,
                                          List<Map<String, Object>> records);

    /**
     * 同步写入完成后调用（无论成功或失败均不调用此方法；仅写入成功后调用）。
     *
     * @param subjectCode Subject 编码
     * @param action      本次同步动作
     * @param records     实际写入的记录列表
     */
    void afterSync(String subjectCode, SyncAction action, List<Map<String, Object>> records);
}
