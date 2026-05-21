package com.ruijie.authzengine.infrastructure.authz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruijie.authzengine.domain.service.FieldMaskExpressionEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 字段控制执行器，按 {@code fieldControls} 指令对响应 JSON 节点执行 HIDE/MASK/RESTRICTED/OPEN 操作。
 *
 * <p>由 {@link com.ruijie.authzengine.autoconfigure.AuthzFieldControlAdvice} 在响应序列化前调用。
 * 本类仅操作 {@link ObjectNode} 节点，对 arrayNode 中的每个对象元素递归执行。
 *
 * <h3>字段定位规则</h3>
 * <ol>
 *   <li>优先按 {@code fieldName}（驼峰属性名）匹配 JSON key</li>
 *   <li>若 {@code fieldName} 为空，回退到 {@code columnName}（下划线数据库列名）</li>
 * </ol>
 *
 * <h3>控制动作语义（§8.3）</h3>
 * <ul>
 *   <li>{@code OPEN} — 直接透传，无操作</li>
 *   <li>{@code RESTRICTED} — 将字段值替换为固定占位符 {@code ***}</li>
 *   <li>{@code MASK} — 调用 {@link FieldMaskExpressionEvaluator} 执行脱敏脚本；脚本为空时退化为 {@code RESTRICTED}</li>
 *   <li>{@code HIDE} — 从 ObjectNode 中完全删除该 key（而非置为 null）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FieldControlExecutor {

    /** RESTRICTED 固定占位符，§8.3 约定。 */
    public static final String RESTRICTED_PLACEHOLDER = "***";

    private static final String ACTION_OPEN = "OPEN";
    private static final String ACTION_RESTRICTED = "RESTRICTED";
    private static final String ACTION_MASK = "MASK";
    private static final String ACTION_HIDE = "HIDE";

    private final FieldMaskExpressionEvaluator maskEvaluator;

    /**
     * 对单个 {@link ObjectNode} 执行全部字段控制指令。
     *
     * @param objectNode    待处理的 JSON 对象节点（原地修改）
     * @param fieldControls PDP 输出的字段控制列表，每项含 {@code fieldName}、{@code columnName}、
     *                      {@code action}、{@code maskScript}
     */
    @SuppressWarnings("unchecked")
    public void apply(ObjectNode objectNode, List<Map<String, Object>> fieldControls) {
        if (objectNode == null || fieldControls == null || fieldControls.isEmpty()) {
            return;
        }
        for (Map<String, Object> ctrl : fieldControls) {
            String code = (String) ctrl.get("code");
            String action = (String) ctrl.get("action");
            String maskScript = (String) ctrl.get("maskScript");
            Map<String, Object> params = ctrl.get("params") instanceof Map ? (Map<String, Object>) ctrl.get("params") : null;

            // 目标 key 统一走字段映射工具解析：fieldName -> columnName -> code
            String targetKey = BoFieldMappingSupport.resolveTargetKey(ctrl);
            if (!StringUtils.hasText(targetKey)) {
                log.debug("[FIELD-CTRL] 字段控制项无法确定目标 key (fieldName/columnName/code 均为空)，跳过: action={}", action);
                continue;
            }
            if (!objectNode.has(targetKey)) {
                // 响应中没有该字段，直接跳过（前端可能已经过滤了该字段）
                continue;
            }
            applyAction(objectNode, targetKey, action, maskScript, code, params);
        }
    }

    /**
     * 对 JSON 数组的每个元素执行字段控制。
     *
     * @param arrayNode     JSON 数组节点
     * @param fieldControls 字段控制指令列表
     */
    public void applyToArray(Iterable<JsonNode> arrayNode, List<Map<String, Object>> fieldControls) {
        if (arrayNode == null || fieldControls == null || fieldControls.isEmpty()) {
            return;
        }
        for (JsonNode element : arrayNode) {
            if (element.isObject()) {
                apply((ObjectNode) element, fieldControls);
            }
        }
    }

    // -------------------------------------------------------------------------
    // 内部执行逻辑
    // -------------------------------------------------------------------------

    private void applyAction(ObjectNode node, String key, String action, String maskScript, String code, Map<String, Object> params) {
        if (!StringUtils.hasText(action)) {
            log.debug("[FIELD-CTRL] action 为空，目标字段 {} 不作处理", key);
            return;
        }
        switch (action.toUpperCase()) {
            case ACTION_OPEN:
                // 透传，无操作
                break;
            case ACTION_HIDE:
                node.remove(key);
                log.debug("[FIELD-CTRL] HIDE: 已移除字段 {}", key);
                break;
            case ACTION_RESTRICTED:
                node.put(key, RESTRICTED_PLACEHOLDER);
                log.debug("[FIELD-CTRL] RESTRICTED: 字段 {} 已替换为占位符", key);
                break;
            case ACTION_MASK:
                applyMask(node, key, maskScript, code, params);
                break;
            default:
                log.warn("[FIELD-CTRL] 未知 action={}, 字段 {} 不作处理", action, key);
                break;
        }
    }

    private void applyMask(ObjectNode node, String key, String maskScript, String code, Map<String, Object> params) {
        JsonNode valueNode = node.get(key);
        if (!valueNode.isTextual()) {
            // 非字符串字段无法脱敏，退化为 RESTRICTED
            log.debug("[FIELD-CTRL] MASK 目标字段 {} 非字符串类型，退化为 RESTRICTED", key);
            node.put(key, RESTRICTED_PLACEHOLDER);
            return;
        }
        if (!StringUtils.hasText(maskScript)) {
            // 未配置脱敏脚本，退化为 RESTRICTED
            log.debug("[FIELD-CTRL] MASK 目标字段 {} 未配置脱敏脚本，退化为 RESTRICTED", key);
            node.put(key, RESTRICTED_PLACEHOLDER);
            return;
        }
        String original = valueNode.asText();
        String masked = maskEvaluator.evaluate(maskScript, original, StringUtils.hasText(code) ? code : key, params);
        node.put(key, masked);
        log.debug("[FIELD-CTRL] MASK: 字段 {} 已脱敏", key);
    }
}
