package com.ruijie.authzengine.infrastructure.authz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.domain.model.common.HookExecutionStatus;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Hook 执行审计记录器。
 *
 * <p>在 PIP 完成 Subject Shadow Hook 或 BO Hook 调用后，
 * 将执行状态、耗时和属性快照写入请求上下文的保留键，
 * 供后续 {@link com.ruijie.authzengine.application.service.AuthzAuditAppService} 在审计写入阶段读取并持久化。
 *
 * <p>保留键约定：
 * <ul>
 *   <li>{@code __hookExecStatus__}：{@link HookExecutionStatus} 的 name() 字符串</li>
 *   <li>{@code __hookCostMs__}：Hook 执行耗时（毫秒，Long）</li>
 *   <li>{@code __hookAttrSnapshot__}：Hook 返回属性快照的 JSON 字符串（最长 2000 字符）</li>
 * </ul>
 */
@Slf4j
@Component
public class HookExecutionAuditRecorder {

    /** 属性快照序列化长度上限 */
    private static final int SNAPSHOT_MAX_LEN = 2000;

    /** 请求上下文中记录 Hook 执行状态的保留键 */
    public static final String CTX_HOOK_EXEC_STATUS = "__hookExecStatus__";

    /** 请求上下文中记录 Hook 执行耗时的保留键 */
    public static final String CTX_HOOK_COST_MS = "__hookCostMs__";

    /** 请求上下文中记录 Hook 属性快照的保留键 */
    public static final String CTX_HOOK_ATTR_SNAPSHOT = "__hookAttrSnapshot__";

    private final ObjectMapper objectMapper;

    @Autowired
    public HookExecutionAuditRecorder(@org.springframework.beans.factory.annotation.Qualifier("authzObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private HookExecutionAuditRecorder() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 创建不写任何数据的 noop 实例（用于不需要审计的测试场景）。
     *
     * @return noop 实例
     */
    public static HookExecutionAuditRecorder noop() {
        return new HookExecutionAuditRecorder();
    }

    /**
     * 将 Hook 执行状态、耗时与属性快照写入请求上下文属性集合。
     *
     * <p>若 {@code attributes} 为 null 或 {@code status} 为 null，则跳过写入。
     * 若目标键已存在（只记录第一次 Hook 的状态），则不覆盖。
     *
     * @param attributes 当前请求上下文属性集合（PIP 内部使用）
     * @param status     Hook 执行状态
     * @param costMs     Hook 耗时（毫秒）
     * @param hookAttrs  Hook 返回的原始属性（可为 null）
     */
    public void trace(
        Map<String, Object> attributes,
        HookExecutionStatus status,
        long costMs,
        Map<String, Object> hookAttrs
    ) {
        if (attributes == null || status == null) {
            return;
        }
        // 只记录第一次 Hook 的状态（Subject Hook 优先，BO Hook 不覆盖）
        if (attributes.containsKey(CTX_HOOK_EXEC_STATUS)) {
            return;
        }
        attributes.put(CTX_HOOK_EXEC_STATUS, status.name());
        attributes.put(CTX_HOOK_COST_MS, costMs);
        if (hookAttrs != null && !hookAttrs.isEmpty()) {
            attributes.put(CTX_HOOK_ATTR_SNAPSHOT, serializeSnapshot(hookAttrs));
        }
    }

    private String serializeSnapshot(Map<String, Object> hookAttrs) {
        try {
            String json = objectMapper.writeValueAsString(hookAttrs);
            if (json.length() > SNAPSHOT_MAX_LEN) {
                return json.substring(0, SNAPSHOT_MAX_LEN);
            }
            return json;
        } catch (JsonProcessingException exception) {
            log.warn("序列化 Hook 属性快照失败，跳过快照记录", exception);
            return null;
        }
    }
}
