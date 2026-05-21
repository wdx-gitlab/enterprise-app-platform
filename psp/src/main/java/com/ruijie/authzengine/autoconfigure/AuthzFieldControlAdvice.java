package com.ruijie.authzengine.autoconfigure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruijie.authzengine.domain.model.decision.AuthzDecision;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.infrastructure.authz.AuthzDecisionHolder;
import com.ruijie.authzengine.infrastructure.authz.FieldControlExecutor;
import com.ruijie.authzengine.shared.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * FIELD 策略字段控制响应拦截器。
 *
 * <p>实现 {@link ResponseBodyAdvice}，在宿主 Controller 响应序列化为 JSON 之前，
 * 自动读取当前请求线程绑定的 {@link AuthzDecision#getObligations() obligations.fieldControls}，
 * 调用 {@link FieldControlExecutor} 执行 HIDE / MASK / RESTRICTED / OPEN 四种字段控制动作，
 * 实现字段级数据权限的无侵入自动执行。
 *
 * <h3>支持的响应包装类型（§8.3）</h3>
 * <ul>
 *   <li>{@link ApiResponse}{@code <T>} — 处理 {@code data} 字段</li>
 *   <li>{@link PageResult}{@code <T>} — 处理 {@code records} 字段</li>
 *   <li>{@code Collection<T>} / 数组 — 处理每个元素</li>
 *   <li>单个 POJO — 直接处理为 ObjectNode</li>
 * </ul>
 *
 * <h3>工作流程</h3>
 * <ol>
 *   <li>{@code AuthzHttpPepFilter} 鉴权通过后将决策存入 {@link AuthzDecisionHolder}</li>
 *   <li>本 Advice 读取 {@code obligations.fieldControls}，无控制则直接放行</li>
 *   <li>将响应体序列化为 {@link JsonNode} 后执行字段控制</li>
 *   <li>将修改后的 JsonNode 作为新的响应体返回（仍以 JSON 序列化输出）</li>
 * </ol>
 *
 * <p><b>性能说明</b>：本 Advice 仅在 {@code fieldControls} 非空时执行序列化-修改-反序列化，
 * 绝大多数无字段控制的请求直接透传，开销可忽略不计。
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class AuthzFieldControlAdvice implements ResponseBodyAdvice<Object> {

    private static final String FIELD_CONTROLS_KEY = "fieldControls";

    private final FieldControlExecutor fieldControlExecutor;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 当 ThreadLocal 中有鉴权决策，且 fieldControls 非空时才介入，否则不修改响应
        AuthzDecision decision = AuthzDecisionHolder.get();
        if (decision == null || decision.getObligations() == null) {
            return false;
        }
        Object fieldControlsObj = decision.getObligations().get(FIELD_CONTROLS_KEY);
        if (!(fieldControlsObj instanceof List) || ((List<?>) fieldControlsObj).isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        if (body == null) {
            return null;
        }
        AuthzDecision decision = AuthzDecisionHolder.get();
        if (decision == null || decision.getObligations() == null) {
            return body;
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fieldControls = (List<Map<String, Object>>) decision.getObligations().get(FIELD_CONTROLS_KEY);
        if (fieldControls == null || fieldControls.isEmpty()) {
            return body;
        }
        try {
            Object modified = applyFieldControls(body, fieldControls);
            // 直接将修改后的 JsonNode 序列化写入响应流：
            // 若通过 return JsonNode，Spring 的 converter 调用 BigDecimalNode.serialize → gen.writeNumber(BigDecimal)
            // 该路径不受 SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN 控制，会输出科学计数法。
            // 直接写入并 return null 使 Spring 跳过 converter.write，彻底绕开该问题。
            byte[] bytes = objectMapper.writeValueAsBytes(modified);
            MediaType contentType = (selectedContentType != null) ? selectedContentType : MediaType.APPLICATION_JSON;
            response.getHeaders().setContentType(contentType);
            OutputStream out = response.getBody();
            out.write(bytes);
            out.flush();
            // 返回 null：Spring 检测到 body==null 后跳过 converter.write，响应体已由上方写入
            return null;
        } catch (Exception e) {
            // 字段控制执行失败时记录日志，降级返回原始响应，不影响业务可用性
            log.error("[FIELD-CTRL] 字段控制响应拦截失败，降级返回原始响应，cause={}", e.getMessage(), e);
            return body;
        }
    }

    // -------------------------------------------------------------------------
    // 内部处理
    // -------------------------------------------------------------------------

    private Object applyFieldControls(Object body, List<Map<String, Object>> fieldControls) throws Exception {
        // 将响应体转换为 JsonNode，便于节点操作
        JsonNode node = objectMapper.valueToTree(body);

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            // ApiResponse<T> 包装：处理 data 字段
            if (objectNode.has("data")) {
                JsonNode dataNode = objectNode.get("data");
                // data 可能是 PageResult（含 records 数组），需要下钻处理 records
                if (dataNode.isObject() && dataNode.has("records")) {
                    applyToNode(dataNode.get("records"), fieldControls);
                } else {
                    applyToNode(dataNode, fieldControls);
                }
            } else if (objectNode.has("records")) {
                // PageResult<T> 包装（无 ApiResponse 外层）：处理 records 数组
                JsonNode recordsNode = objectNode.get("records");
                applyToNode(recordsNode, fieldControls);
            } else {
                // 普通 POJO 直接处理
                fieldControlExecutor.apply(objectNode, fieldControls);
            }
            return objectNode;
        }

        if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            fieldControlExecutor.applyToArray(arrayNode, fieldControls);
            return arrayNode;
        }

        // 基本类型或 null，无法执行字段控制
        return body;
    }

    private void applyToNode(JsonNode targetNode, List<Map<String, Object>> fieldControls) {
        if (targetNode == null || targetNode.isNull()) {
            return;
        }
        if (targetNode.isArray()) {
            fieldControlExecutor.applyToArray(targetNode, fieldControls);
        } else if (targetNode.isObject()) {
            fieldControlExecutor.apply((ObjectNode) targetNode, fieldControls);
        }
    }
}
