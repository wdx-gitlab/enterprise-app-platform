package com.ruijie.authzengine.domain.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.domain.model.decision.AuthzContext;
import com.ruijie.authzengine.domain.model.decision.AuthzDecision;
import com.ruijie.authzengine.domain.model.decision.AuthzRequest;
import com.ruijie.authzengine.domain.model.decision.DataScopeFragment;
import com.ruijie.authzengine.domain.model.decision.PermissionGrant;
import com.ruijie.authzengine.domain.model.common.PolicyTemplateType;
import com.ruijie.authzengine.infrastructure.authz.BoFieldMappingSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 权限决策领域服务，负责资源与动作匹配后的最终授权归并。
 *
 * <p>被 PDP 调用，核心流程（{@link #evaluate}）：
 * <ol>
 *   <li>前置校验：主体上下文是否完整，主体/资源/动作是否已注册</li>
 *   <li>从授权记录中逐条匹配：资源类型 + 资源模型编码 + 资源编码 + 动作编码四段全等</li>
 *   <li>对命中且绑定了策略模板的授权记录，执行策略表达式评估（布尔门控）</li>
 *   <li>归并命中权限项、分配记录 ID、委托记录 ID、策略模板编码</li>
 *   <li>生成最终决策：PERMIT / NOT_PERMIT / INDETERMINATE</li>
 * </ol>
 */
@Slf4j
@Service
public class PermissionDecisionService {

    private static final String POLICY_STATUS_ENABLED = "ENABLED";

    private static final String FAIL_STRATEGY_ALLOW = "ALLOW";

    private static final String REASON_POLICY_DENIED = "POLICY_DENIED";

    private static final String REASON_POLICY_EVALUATION_ERROR = "POLICY_EVALUATION_ERROR";

    private static final String REASON_INVALID_FIELD_POLICY = "INVALID_FIELD_POLICY";

    private static final String OBLIGATION_ROW_FILTER = "rowFilter";

    private static final String OBLIGATION_FIELD_CONTROLS = "fieldControls";

    private static final String FIELD_ACTION_OPEN = "OPEN";

    private static final String FIELD_ACTION_RESTRICTED = "RESTRICTED";

    private static final String FIELD_ACTION_MASK = "MASK";

    private static final String FIELD_ACTION_HIDE = "HIDE";

    private final PolicyExpressionEvaluator policyExpressionEvaluator;

    private final DataScopeExpressionEvaluator dataScopeExpressionEvaluator;

    private final ObjectMapper objectMapper;

    /**
     * 构造器注入策略表达式评估器和 Jackson ObjectMapper。
     *
     * @param policyExpressionEvaluator 策略表达式评估器
     * @param objectMapper             authz-engine 专属 ObjectMapper，与宿主隔离
     */
    @Autowired
    public PermissionDecisionService(PolicyExpressionEvaluator policyExpressionEvaluator,
                                     DataScopeExpressionEvaluator dataScopeExpressionEvaluator,
                                     @Qualifier("authzObjectMapper") ObjectMapper objectMapper) {
        this.policyExpressionEvaluator = policyExpressionEvaluator;
        this.dataScopeExpressionEvaluator = dataScopeExpressionEvaluator;
        this.objectMapper = objectMapper;
    }

    public PermissionDecisionService(PolicyExpressionEvaluator policyExpressionEvaluator,
                                     ObjectMapper objectMapper) {
        this(policyExpressionEvaluator, new DataScopeExpressionEvaluator(), objectMapper);
    }

    /**
     * 基于主体上下文和授权记录执行最小决策逻辑。
     *
     * @param request 鉴权请求
     * @param context PIP 补全上下文
     * @param grants 命中的授权记录
     * @return 决策结果
     */
    public AuthzDecision evaluate(AuthzRequest request, AuthzContext context, List<PermissionGrant> grants) {
        // ── 前置校验：上下文完整性 ──
        // 主体上下文为空说明 PIP 加载失败，返回 INDETERMINATE（无法判断）
        if (context == null || context.getSubjectKeys() == null || context.getSubjectKeys().isEmpty()) {
            return AuthzDecision.indeterminate("SUBJECT_CONTEXT_EMPTY");
        }
        // 治理三段校验：主体、资源、动作是否在治理目录中注册
        // 未注册的直接拒绝，避免对未纳管实体执行权限判断
        if (!isGovernanceRegistered(context, "subjectRegistered", true)) {
            return AuthzDecision.notPermit("SUBJECT_NOT_REGISTERED");
        }
        if (!isGovernanceRegistered(context, "resourceRegistered", true)) {
            return AuthzDecision.notPermit("RESOURCE_NOT_REGISTERED");
        }
        if (!isGovernanceRegistered(context, "actionRegistered", true)) {
            return AuthzDecision.notPermit("ACTION_NOT_REGISTERED");
        }
        // 无任何授权记录，直接拒绝
        if (grants == null || grants.isEmpty()) {
            return AuthzDecision.notPermit("NO_ASSIGNMENT");
        }

        // ── 核心匹配：逐条比对授权记录与请求的四段式（资源类型 + 资源模型编码 + 资源编码 + 动作编码）──
        List<PermissionGrant> matchedGrants = grants.stream()
            .filter(grant -> matches(grant, request, context))
            .collect(Collectors.toList());
        // 提取命中的权限项编码（去重）
        List<String> matchedPermissions = matchedGrants.stream()
            .map(PermissionGrant::getPermissionCode)
            .distinct()
            .collect(Collectors.toList());
        // 四段式全等匹配无命中，拒绝
        if (matchedPermissions.isEmpty()) {
            return AuthzDecision.notPermit("NO_PERMISSION_ITEM");
        }

        // ── 策略模板评估：对命中且绑定了策略模板的授权记录执行布尔门控 ──
        PolicyEvaluationState policyEvaluationState = evaluatePolicyGates(matchedGrants, context);
        if (policyEvaluationState.isIndeterminate()) {
            return AuthzDecision.indeterminate(policyEvaluationState.getIndeterminateReason());
        }
        if (policyEvaluationState.isRejected()) {
            return AuthzDecision.notPermit(policyEvaluationState.getRejectReason());
        }
        List<PermissionGrant> policyPassedGrants = policyEvaluationState.getPassedGrants();
        // 策略评估后无通过的授权记录，拒绝
        if (policyPassedGrants.isEmpty()) {
            log.info("[权限决策] 四段匹配命中 {} 条，但策略模板评估后均未通过", matchedGrants.size());
            return AuthzDecision.notPermit(REASON_POLICY_DENIED);
        }

        // ── 归并结果：汇总权限项、分配记录ID、委托记录ID、策略模板编码 ──
        return AuthzDecision.permit(
            policyPassedGrants.stream()
                .map(PermissionGrant::getPermissionCode)
                .distinct()
                .collect(Collectors.toList()),
            policyPassedGrants.stream()
                .map(PermissionGrant::getAssignmentId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .distinct()
                .collect(Collectors.toList()),
            policyPassedGrants.stream()
                .map(PermissionGrant::getDelegateId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList()),
            policyPassedGrants.stream()
                .map(PermissionGrant::getPolicyTemplateCode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList()),
            policyEvaluationState.getObligations()
        );
    }

    /**
     * 对四段式匹配命中的授权记录，按策略模板进行布尔门控过滤。
     *
     * <p>规则：
    * <ul>
    *   <li>按固定优先级执行：ENV → STATE → DATA → FIELD</li>
    *   <li>ENV / STATE 组内按 OR 语义评估：任一 PASS 即通过当前分组；全部 FAIL 则拒绝；无 PASS 且存在 ERROR 时返回 INDETERMINATE</li>
    *   <li>DATA 仅在 ENV / STATE 都通过后才归并 obligations；多个 DATA 片段按 OR 拼接</li>
    *   <li>FIELD 保持现有最严动作归并规则，仅在前置分组都通过后执行</li>
    *   <li>未绑定策略模板的普通授权记录仍按原语义直接通过；历史旧类型继续按兼容布尔门控处理</li>
    * </ul>
     * </p>
     */
    private PolicyEvaluationState evaluatePolicyGates(List<PermissionGrant> matchedGrants, AuthzContext context) {
        PolicyEvaluationState state = new PolicyEvaluationState();
        if (matchedGrants == null || matchedGrants.isEmpty()) {
            return state;
        }

        Map<String, Object> sub = extractNamespace(context, "sub");
        Map<String, Object> res = extractNamespace(context, "res");
        Map<String, Object> env = extractNamespace(context, "env");
        String tableName = extractPrimaryTableName(context);
        List<Map<String, Object>> attributes = extractPrimaryAttributes(context);

        if (!evaluatePriorityBooleanGroup(matchedGrants, PolicyTemplateType.ENV, sub, res, env, state)) {
            return state;
        }
        if (!evaluatePriorityBooleanGroup(matchedGrants, PolicyTemplateType.STATE, sub, res, env, state)) {
            return state;
        }

        PolicyExpressionEvaluator.EvalResult legacyBooleanResult =
            evaluateLegacyBooleanGates(matchedGrants, sub, res, env, state);
        if (legacyBooleanResult == PolicyExpressionEvaluator.EvalResult.FAIL) {
            state.reject(REASON_POLICY_DENIED);
            return state;
        }
        if (legacyBooleanResult == PolicyExpressionEvaluator.EvalResult.ERROR) {
            state.indeterminate(REASON_POLICY_EVALUATION_ERROR);
            return state;
        }

        addPassedNonDataFieldGrants(matchedGrants, state);
        applyDataGrants(matchedGrants, sub, tableName, attributes, state);
        if (!applyFieldGrants(matchedGrants, context, state)) {
            return state;
        }
        state.setObligations(buildObligations(state.getDataFragments(), state.getFieldControls(), state.getPrimaryTableName(), attributes));
        return state;
    }

    private boolean evaluatePriorityBooleanGroup(
        List<PermissionGrant> matchedGrants,
        PolicyTemplateType targetType,
        Map<String, Object> sub,
        Map<String, Object> res,
        Map<String, Object> env,
        PolicyEvaluationState state
    ) {
        PolicyExpressionEvaluator.EvalResult groupResult = evaluateBooleanGroup(matchedGrants, targetType, sub, res, env);
        if (groupResult == PolicyExpressionEvaluator.EvalResult.FAIL) {
            state.reject(REASON_POLICY_DENIED);
            return false;
        }
        if (groupResult == PolicyExpressionEvaluator.EvalResult.ERROR) {
            state.indeterminate(REASON_POLICY_EVALUATION_ERROR);
            return false;
        }
        addPassedBooleanGroupGrants(matchedGrants, targetType, sub, res, env, state);
        return true;
    }

    private PolicyExpressionEvaluator.EvalResult evaluateBooleanGroup(
        List<PermissionGrant> matchedGrants,
        PolicyTemplateType targetType,
        Map<String, Object> sub,
        Map<String, Object> res,
        Map<String, Object> env
    ) {
        boolean hasApplicableGrant = false;
        boolean hasPass = false;
        boolean hasError = false;
        for (PermissionGrant grant : matchedGrants) {
            PolicyTemplateType policyTemplateType = resolvePolicyTemplateType(grant);
            if (!shouldEvaluateBooleanGroupGrant(grant, policyTemplateType, targetType)) {
                continue;
            }
            BooleanGateEvalStatus evalStatus = evaluateBooleanGateStatus(grant, sub, res, env);
            if (evalStatus == BooleanGateEvalStatus.SKIP) {
                continue;
            }
            hasApplicableGrant = true;
            if (evalStatus == BooleanGateEvalStatus.PASS) {
                hasPass = true;
                continue;
            }
            if (evalStatus == BooleanGateEvalStatus.ERROR) {
                hasError = true;
            }
        }
        if (!hasApplicableGrant) {
            return PolicyExpressionEvaluator.EvalResult.PASS;
        }
        if (hasPass) {
            return PolicyExpressionEvaluator.EvalResult.PASS;
        }
        return hasError ? PolicyExpressionEvaluator.EvalResult.ERROR : PolicyExpressionEvaluator.EvalResult.FAIL;
    }

    private PolicyExpressionEvaluator.EvalResult evaluateLegacyBooleanGates(
        List<PermissionGrant> matchedGrants,
        Map<String, Object> sub,
        Map<String, Object> res,
        Map<String, Object> env,
        PolicyEvaluationState state
    ) {
        boolean hasError = false;
        for (PermissionGrant grant : matchedGrants) {
            PolicyTemplateType policyTemplateType = resolvePolicyTemplateType(grant);
            if (!shouldEvaluateAsBooleanGate(grant, policyTemplateType) || isPriorityBooleanType(policyTemplateType)) {
                continue;
            }
            BooleanGateEvalStatus evalStatus = evaluateBooleanGateStatus(grant, sub, res, env);
            if (evalStatus == BooleanGateEvalStatus.SKIP) {
                continue;
            }
            if (evalStatus == BooleanGateEvalStatus.FAIL) {
                return PolicyExpressionEvaluator.EvalResult.FAIL;
            }
            if (evalStatus == BooleanGateEvalStatus.ERROR) {
                hasError = true;
                continue;
            }
            state.addPassedGrant(grant);
        }
        return hasError ? PolicyExpressionEvaluator.EvalResult.ERROR : PolicyExpressionEvaluator.EvalResult.PASS;
    }

    private void addPassedBooleanGroupGrants(
        List<PermissionGrant> matchedGrants,
        PolicyTemplateType targetType,
        Map<String, Object> sub,
        Map<String, Object> res,
        Map<String, Object> env,
        PolicyEvaluationState state
    ) {
        for (PermissionGrant grant : matchedGrants) {
            PolicyTemplateType policyTemplateType = resolvePolicyTemplateType(grant);
            if (!shouldEvaluateBooleanGroupGrant(grant, policyTemplateType, targetType)) {
                continue;
            }
            if (evaluateBooleanGateStatus(grant, sub, res, env) == BooleanGateEvalStatus.PASS) {
                state.addPassedGrant(grant);
            }
        }
    }

    private boolean shouldEvaluateAsBooleanGate(PermissionGrant grant, PolicyTemplateType policyTemplateType) {
        if (grant == null) {
            return false;
        }
        if (policyTemplateType == PolicyTemplateType.DATA || policyTemplateType == PolicyTemplateType.FIELD) {
            return false;
        }
        return StringUtils.hasText(grant.getPolicyTemplateType())
            || StringUtils.hasText(grant.getExpressionScript())
            || StringUtils.hasText(grant.getPolicyTemplateCode());
    }

    private boolean shouldEvaluateBooleanGroupGrant(
        PermissionGrant grant,
        PolicyTemplateType policyTemplateType,
        PolicyTemplateType targetType
    ) {
        return shouldEvaluateAsBooleanGate(grant, policyTemplateType) && targetType == policyTemplateType;
    }

    private boolean isPriorityBooleanType(PolicyTemplateType policyTemplateType) {
        return policyTemplateType == PolicyTemplateType.ENV || policyTemplateType == PolicyTemplateType.STATE;
    }

    private BooleanGateEvalStatus evaluateBooleanGateStatus(
        PermissionGrant grant,
        Map<String, Object> sub,
        Map<String, Object> res,
        Map<String, Object> env
    ) {
        if (grant == null) {
            return BooleanGateEvalStatus.FAIL;
        }
        if (!isPolicyTemplateEnabled(grant) || !StringUtils.hasText(grant.getExpressionScript())) {
            return BooleanGateEvalStatus.SKIP;
        }
        if (policyExpressionEvaluator == null) {
            log.debug("[权限决策] 策略评估器未注入，跳过布尔门控: templateCode={}", grant.getPolicyTemplateCode());
            return BooleanGateEvalStatus.SKIP;
        }
        Map<String, Object> param = parsePolicyParams(grant.getPolicyParams());
        PolicyExpressionEvaluator.EvalResult evalResult =
            policyExpressionEvaluator.evaluate(grant.getExpressionScript(), sub, res, env, param);
        log.debug("[权限决策] 策略评估: permCode={}, templateCode={}, script='{}', result={}",
            grant.getPermissionCode(), grant.getPolicyTemplateCode(), grant.getExpressionScript(), evalResult);
        if (evalResult == PolicyExpressionEvaluator.EvalResult.FAIL) {
            return BooleanGateEvalStatus.FAIL;
        }
        if (evalResult == PolicyExpressionEvaluator.EvalResult.ERROR) {
            return BooleanGateEvalStatus.ERROR;
        }
        return BooleanGateEvalStatus.PASS;
    }

    private void addPassedNonDataFieldGrants(List<PermissionGrant> matchedGrants, PolicyEvaluationState state) {
        for (PermissionGrant grant : matchedGrants) {
            PolicyTemplateType policyTemplateType = resolvePolicyTemplateType(grant);
            if (policyTemplateType == PolicyTemplateType.DATA || policyTemplateType == PolicyTemplateType.FIELD) {
                continue;
            }
            if (shouldEvaluateAsBooleanGate(grant, policyTemplateType)) {
                continue;
            }
            state.addPassedGrant(grant);
        }
    }

    private void applyDataGrants(
        List<PermissionGrant> matchedGrants,
        Map<String, Object> sub,
        String tableName,
        List<Map<String, Object>> attributes,
        PolicyEvaluationState state
    ) {
        for (PermissionGrant grant : matchedGrants) {
            PolicyTemplateType policyTemplateType = resolvePolicyTemplateType(grant);
            if (policyTemplateType != PolicyTemplateType.DATA) {
                continue;
            }
            applyDataGrant(grant, sub, tableName, attributes, state);
            state.setPrimaryTableName(tableName);
        }
    }

    private boolean applyFieldGrants(List<PermissionGrant> matchedGrants, AuthzContext context, PolicyEvaluationState state) {
        for (PermissionGrant grant : matchedGrants) {
            PolicyTemplateType policyTemplateType = resolvePolicyTemplateType(grant);
            if (policyTemplateType != PolicyTemplateType.FIELD) {
                continue;
            }
            if (!applyFieldGrant(grant, context, state)) {
                return false;
            }
        }
        return true;
    }

    private void applyDataGrant(
        PermissionGrant grant,
        Map<String, Object> sub,
        String tableName,
        List<Map<String, Object>> attributes,
        PolicyEvaluationState state
    ) {
        if (!isPolicyTemplateEnabled(grant) || !StringUtils.hasText(grant.getExpressionScript())) {
            state.addPassedGrant(grant);
            return;
        }
        if (dataScopeExpressionEvaluator == null) {
            log.debug("[权限决策] DATA 策略评估器未注入，按无过滤放行: templateCode={}", grant.getPolicyTemplateCode());
            state.addPassedGrant(grant);
            return;
        }
        try {
            DataScopeFragment fragment = dataScopeExpressionEvaluator.evaluate(
                grant.getExpressionScript(),
                sub,
                tableName,
                attributes,
                parsePolicyParams(grant.getPolicyParams())
            );
            state.addPassedGrant(grant);
            if (fragment != null && StringUtils.hasText(fragment.getSql())) {
                state.addDataFragment(fragment);
            }
        } catch (RuntimeException exception) {
            if (FAIL_STRATEGY_ALLOW.equalsIgnoreCase(normalizeFailStrategy(grant.getFailStrategy()))) {
                log.warn("[权限决策] DATA 策略执行失败但 failStrategy=ALLOW，按无过滤放行: permCode={}, error={}",
                    grant.getPermissionCode(), exception.getMessage());
                state.addPassedGrant(grant);
                return;
            }
            log.warn("[权限决策] DATA 策略执行失败且 failStrategy=DENY，拒绝该授权: permCode={}, error={}",
                grant.getPermissionCode(), exception.getMessage());
        }
    }

    private boolean applyFieldGrant(PermissionGrant grant, AuthzContext context, PolicyEvaluationState state) {
        FieldControlInstruction instruction = resolveFieldControlInstruction(grant, context);
        if (instruction == null) {
            state.reject(REASON_INVALID_FIELD_POLICY);
            return false;
        }
        state.addPassedGrant(grant);
        state.mergeFieldControl(instruction.toObligation());
        return true;
    }

    private FieldControlInstruction resolveFieldControlInstruction(PermissionGrant grant, AuthzContext context) {
        Map<String, Object> policyParams = parsePolicyParams(grant.getPolicyParams());
        String targetField = extractFieldTarget(policyParams);
        if (!StringUtils.hasText(targetField)) {
            log.warn("[权限决策] FIELD 策略缺少 targetField: permCode={}, templateCode={}",
                grant.getPermissionCode(), grant.getPolicyTemplateCode());
            return null;
        }
        Map<String, Object> attribute = findGovernanceAttribute(context, targetField.trim());
        if (attribute == null || !Boolean.TRUE.equals(toBoolean(attribute.get("fieldControl")))) {
            log.warn("[权限决策] FIELD 策略目标字段未声明 fieldControl=true: permCode={}, targetField={}",
                grant.getPermissionCode(), targetField);
            return null;
        }
        String action = resolveFieldAction(grant, policyParams);
        if (!StringUtils.hasText(action)) {
            log.warn("[权限决策] FIELD 策略缺少 action: permCode={}, templateCode={}",
                grant.getPermissionCode(), grant.getPolicyTemplateCode());
            return null;
        }
        String normalizedAction = action.trim().toUpperCase();
        if (!isSupportedFieldAction(normalizedAction)) {
            log.warn("[权限决策] FIELD 策略 action 非法: permCode={}, action={}",
                grant.getPermissionCode(), action);
            return null;
        }
        if (FIELD_ACTION_MASK.equals(normalizedAction)
            && !"STRING".equalsIgnoreCase(String.valueOf(attribute.get("type")))) {
            log.warn("[权限决策] 非字符串字段不允许绑定 MASK 策略: permCode={}, fieldCode={}",
                grant.getPermissionCode(), attribute.get("code"));
            return null;
        }
        // MASK 脱敏脚本：policyParams.maskScript 优先，回退到 grant.expressionScript
        String maskScript = null;
        if (FIELD_ACTION_MASK.equals(normalizedAction)) {
            Object paramMaskScript = policyParams.get("maskScript");
            if (paramMaskScript != null && StringUtils.hasText(String.valueOf(paramMaskScript))) {
                maskScript = String.valueOf(paramMaskScript).trim();
            } else if (StringUtils.hasText(grant.getExpressionScript())) {
                maskScript = grant.getExpressionScript().trim();
            }
        }
        // code 必存在；fieldName 可选（未配置时回退到 code，确保能匹配响应 JSON 的驼峰 key）；columnName 可选
        String attrCode     = safeAttrStr(attribute, "code");
        String attrFieldName = StringUtils.hasText(safeAttrStr(attribute, "fieldName"))
            ? safeAttrStr(attribute, "fieldName") : attrCode;
        String attrColumnName = safeAttrStr(attribute, "columnName");
        return new FieldControlInstruction(
            attrCode,
            attrFieldName,
            attrColumnName,
            normalizedAction,
            maskScript,
            grant.getPolicyTemplateCode(),
            policyParams
        );
    }

    /**
     * 安全地从 attribute map 中读取字符串值：null 或字面量 "null" 均返回 null。
     */
    private static String safeAttrStr(Map<String, Object> attr, String key) {
        Object val = attr.get(key);
        if (val == null) {
            return null;
        }
        String s = String.valueOf(val).trim();
        return (s.isEmpty() || "null".equals(s)) ? null : s;
    }

    private String extractFieldTarget(Map<String, Object> policyParams) {
        Object targetField = policyParams.get("targetField");
        return targetField == null ? null : String.valueOf(targetField);
    }

    private String resolveFieldAction(PermissionGrant grant, Map<String, Object> policyParams) {
        Object policyAction = policyParams.get("action");
        if (policyAction != null && StringUtils.hasText(String.valueOf(policyAction))) {
            return String.valueOf(policyAction).trim();
        }
        if (!StringUtils.hasText(grant.getParamSchema())) {
            return null;
        }
        try {
            JsonNode schemaNode = objectMapper.readTree(grant.getParamSchema());
            JsonNode actionNode = schemaNode.get("action");
            if (actionNode != null && actionNode.isTextual()) {
                return actionNode.asText();
            }
            JsonNode propertiesActionNode = schemaNode.path("properties").path("action");
            if (propertiesActionNode.isMissingNode()) {
                return null;
            }
            if (propertiesActionNode.hasNonNull("const")) {
                return propertiesActionNode.get("const").asText();
            }
            if (propertiesActionNode.hasNonNull("default")) {
                return propertiesActionNode.get("default").asText();
            }
            JsonNode enumNode = propertiesActionNode.get("enum");
            if (enumNode != null && enumNode.isArray() && enumNode.size() > 0) {
                return enumNode.get(0).asText();
            }
            return null;
        } catch (Exception exception) {
            log.warn("[权限决策] FIELD 策略 paramSchema 解析失败: templateCode={}, error={}",
                grant.getPolicyTemplateCode(), exception.getMessage());
            return null;
        }
    }

    private Map<String, Object> findGovernanceAttribute(AuthzContext context, String targetField) {
        for (Map<String, Object> attribute : extractPrimaryAttributes(context)) {
            if (matchesAttributeTarget(attribute, targetField)) {
                return attribute;
            }
        }
        return null;
    }

    private boolean matchesAttributeTarget(Map<String, Object> attribute, String targetField) {
        return targetField.equals(attribute.get("code"))
            || targetField.equals(attribute.get("fieldName"))
            || targetField.equals(attribute.get("columnName"));
    }

    private Map<String, Object> buildObligations(
        List<DataScopeFragment> dataFragments,
        List<Map<String, Object>> fieldControls,
        String primaryTableName,
        List<Map<String, Object>> primaryAttributes
    ) {
        Map<String, Object> obligations = new LinkedHashMap<>();
        if (dataFragments != null && !dataFragments.isEmpty()) {
            List<Object> params = new ArrayList<>();
            List<String> clauses = new ArrayList<>();
            for (DataScopeFragment fragment : dataFragments) {
                if (fragment == null || !StringUtils.hasText(fragment.getSql())) {
                    continue;
                }
                clauses.add("(" + fragment.getSql().trim() + ")");
                if (fragment.getParams() != null && !fragment.getParams().isEmpty()) {
                    params.addAll(fragment.getParams());
                }
            }
            if (!clauses.isEmpty()) {
                Map<String, Object> rowFilter = new LinkedHashMap<>();
                rowFilter.put("whereClause", String.join(" OR ", clauses));
                rowFilter.put("params", params);
                if (StringUtils.hasText(primaryTableName)) {
                    rowFilter.put("tableName", primaryTableName);
                }
                List<String> pkColumnNames = BoFieldMappingSupport.resolvePrimaryKeyColumns(primaryAttributes, primaryTableName);
                if (!pkColumnNames.isEmpty()) {
                    rowFilter.put(BoFieldMappingSupport.ROW_FILTER_PK_COLUMNS_KEY, pkColumnNames);
                }
                obligations.put(OBLIGATION_ROW_FILTER, rowFilter);
            }
        }
        if (fieldControls != null && !fieldControls.isEmpty()) {
            obligations.put(OBLIGATION_FIELD_CONTROLS, fieldControls);
        }
        return obligations;
    }

    /**
     * 从 AuthzContext.attributes 中提取指定命名空间的 Map。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractNamespace(AuthzContext context, String namespace) {
        if (context == null || context.getAttributes() == null) {
            return Collections.emptyMap();
        }
        Object value = context.getAttributes().get(namespace);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return Collections.emptyMap();
    }

    /**
     * 解析 authz_assignment.policy_params JSON 为 Map。
     * 格式非法时返回空 Map 并记录警告。
     */
    private Map<String, Object> parsePolicyParams(String policyParamsJson) {
        if (policyParamsJson == null || policyParamsJson.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> result = objectMapper.readValue(
                policyParamsJson, new TypeReference<Map<String, Object>>() {});
            return result == null ? Collections.<String, Object>emptyMap() : result;
        } catch (Exception e) {
            log.warn("[权限决策] 策略参数 JSON 解析失败，按空参数处理: json='{}', error={}",
                policyParamsJson, e.getMessage());
            return Collections.emptyMap();
        }
    }

    private boolean isPolicyTemplateEnabled(PermissionGrant grant) {
        return grant == null
            || grant.getPolicyTemplateStatus() == null
            || POLICY_STATUS_ENABLED.equalsIgnoreCase(grant.getPolicyTemplateStatus().trim());
    }

    private PolicyTemplateType resolvePolicyTemplateType(PermissionGrant grant) {
        if (grant == null || !StringUtils.hasText(grant.getPolicyTemplateType())) {
            return null;
        }
        try {
            return PolicyTemplateType.valueOf(grant.getPolicyTemplateType().trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            log.warn("[权限决策] 未识别的策略模板类型，按布尔门控处理: permCode={}, polType={}",
                grant.getPermissionCode(), grant.getPolicyTemplateType());
            return null;
        }
    }

    private String extractPrimaryTableName(AuthzContext context) {
        if (context == null || context.getGovernanceAttributes() == null) {
            return null;
        }
        Object tableName = context.getGovernanceAttributes().get("tableName");
        return tableName == null ? null : String.valueOf(tableName);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractPrimaryAttributes(AuthzContext context) {
        if (context == null || context.getGovernanceAttributes() == null) {
            return Collections.emptyList();
        }
        Object attributes = context.getGovernanceAttributes().get("attributes");
        if (attributes == null) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.convertValue(attributes, new TypeReference<List<Map<String, Object>>>() {});
        } catch (IllegalArgumentException exception) {
            log.warn("[权限决策] governance attributes 转换失败，忽略字段元数据: error={}", exception.getMessage());
            return Collections.emptyList();
        }
    }

    private Boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value == null) {
            return Boolean.FALSE;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private boolean isSupportedFieldAction(String action) {
        return FIELD_ACTION_OPEN.equals(action)
            || FIELD_ACTION_RESTRICTED.equals(action)
            || FIELD_ACTION_MASK.equals(action)
            || FIELD_ACTION_HIDE.equals(action);
    }

    private String normalizeFailStrategy(String failStrategy) {
        return StringUtils.hasText(failStrategy) ? failStrategy.trim() : "DENY";
    }

    /**
     * 三段式全等匹配：resourceType + resId + action。
     * 动作编码优先使用治理属性中的标准化编码（normalizedActionCode），不存在时回退到请求原始动作。
     * 当授权记录的 resId 为空时，表示类别级权限，只需 resourceType + action 匹配即可。
     */
    private boolean matches(PermissionGrant grant, AuthzRequest request, AuthzContext context) {
        return safeEquals(grant.getResourceType(), request.getResource().getResourceType())
            && matchesResourceCode(grant, request)
            && safeEquals(grant.getAction(), normalizeActionCode(request, context));
    }

    private boolean matchesResourceCode(PermissionGrant grant, AuthzRequest request) {
        // 类别级权限（resId 为空）只需 resourceType + action 匹配
        String grantResId = grant.getResId();
        if (grantResId == null || grantResId.trim().isEmpty()) {
            return true;
        }
        return safeEquals(grantResId, normalizeResId(request));
    }

    private boolean isGovernanceRegistered(AuthzContext context, String key, boolean defaultValue) {
        if (context.getGovernanceAttributes() == null) {
            return defaultValue;
        }
        Object rawValue = context.getGovernanceAttributes().get(key);
        if (rawValue == null) {
            return defaultValue;
        }
        if (rawValue instanceof Boolean) {
            return (Boolean) rawValue;
        }
        return Boolean.parseBoolean(String.valueOf(rawValue));
    }

    private String normalizeActionCode(AuthzRequest request, AuthzContext context) {
        if (context != null && context.getGovernanceAttributes() != null) {
            Object normalizedActionCode = context.getGovernanceAttributes().get("normalizedActionCode");
            if (normalizedActionCode != null && !String.valueOf(normalizedActionCode).trim().isEmpty()) {
                return String.valueOf(normalizedActionCode).trim();
            }
        }
        return request.getAction();
    }

    /**
     * 从请求中提取有效资源标识，用于与授权记录的 res_id 做全等比较。
     */
    private String normalizeResId(AuthzRequest request) {
        String resId = request.getResource().getResId();
        if (resId != null && !resId.trim().isEmpty()) {
            return resId.trim();
        } else {
            return null;
        }
    }

    private boolean safeEquals(String left, String right) {
        return left != null && left.equals(right);
    }

    private int fieldActionPriority(String action) {
        if (FIELD_ACTION_HIDE.equals(action)) {
            return 4;
        }
        if (FIELD_ACTION_MASK.equals(action)) {
            return 3;
        }
        if (FIELD_ACTION_RESTRICTED.equals(action)) {
            return 2;
        }
        return 1;
    }

    private enum BooleanGateEvalStatus {
        PASS,
        FAIL,
        ERROR,
        SKIP
    }

    private final class PolicyEvaluationState {

        private final List<PermissionGrant> passedGrants = new ArrayList<>();

        private final List<DataScopeFragment> dataFragments = new ArrayList<>();

        private final Map<String, Map<String, Object>> fieldControlMap = new LinkedHashMap<>();

        private Map<String, Object> obligations = Collections.emptyMap();

        private String rejectReason;

        private String indeterminateReason;

        private void addPassedGrant(PermissionGrant grant) {
            passedGrants.add(grant);
        }

        private void addDataFragment(DataScopeFragment fragment) {
            dataFragments.add(fragment);
        }

        private void mergeFieldControl(Map<String, Object> fieldControl) {
            String fieldCode = String.valueOf(fieldControl.get("code"));
            Map<String, Object> existing = fieldControlMap.get(fieldCode);
            if (existing == null) {
                fieldControlMap.put(fieldCode, fieldControl);
                return;
            }
            String existingAction = String.valueOf(existing.get("action"));
            String incomingAction = String.valueOf(fieldControl.get("action"));
            if (fieldActionPriority(incomingAction) > fieldActionPriority(existingAction)) {
                fieldControlMap.put(fieldCode, fieldControl);
                return;
            }
            if (fieldActionPriority(incomingAction) == fieldActionPriority(existingAction)
                && FIELD_ACTION_MASK.equals(incomingAction)
                && !StringUtils.hasText((String) existing.get("maskScript"))
                && StringUtils.hasText((String) fieldControl.get("maskScript"))) {
                // 同优先级 MASK：incoming 有脱敏脚本而 existing 没有，则用 incoming 覆盖
                fieldControlMap.put(fieldCode, fieldControl);
            }
        }

        private void reject(String reason) {
            this.rejectReason = reason;
        }

        private void indeterminate(String reason) {
            this.indeterminateReason = reason;
        }

        private boolean isRejected() {
            return rejectReason != null;
        }

        private boolean isIndeterminate() {
            return indeterminateReason != null;
        }

        private String getRejectReason() {
            return rejectReason;
        }

        private String getIndeterminateReason() {
            return indeterminateReason;
        }

        private List<PermissionGrant> getPassedGrants() {
            return passedGrants;
        }

        private List<DataScopeFragment> getDataFragments() {
            return dataFragments;
        }

        private List<Map<String, Object>> getFieldControls() {
            return new ArrayList<>(fieldControlMap.values());
        }

        private Map<String, Object> getObligations() {
            return obligations;
        }

        private void setObligations(Map<String, Object> obligations) {
            this.obligations = obligations;
        }

        /** 主表名（由 DATA 策略的 #tableName 上下文提取，用于行过滤精准定位）。 */
        private String primaryTableName;

        private void setPrimaryTableName(String tableName) {
            if (tableName != null && !tableName.trim().isEmpty()) {
                this.primaryTableName = tableName.trim();
            }
        }

        private String getPrimaryTableName() {
            return primaryTableName;
        }
    }

    private static final class FieldControlInstruction {

        private final String code;

        private final String fieldName;

        private final String columnName;

        private final String action;

        /** MASK 类型的脱敏 SpEL 脚本；其他 action 时为 null。 */
        private final String maskScript;

        /** 生成该字段控制义务的策略模板编码。 */
        private final String policyCode;

        /** FIELD 脱敏运行时可访问的策略参数。 */
        private final Map<String, Object> params;

        private FieldControlInstruction(
            String code,
            String fieldName,
            String columnName,
            String action,
            String maskScript,
            String policyCode,
            Map<String, Object> params
        ) {
            this.code = code;
            this.fieldName = fieldName;
            this.columnName = columnName;
            this.action = action;
            this.maskScript = maskScript;
            this.policyCode = policyCode;
            this.params = params == null || params.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(params));
        }

        private Map<String, Object> toObligation() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("code", code);
            result.put("fieldName", fieldName);
            result.put("columnName", columnName);
            result.put("action", action);
            if (StringUtils.hasText(maskScript)) {
                result.put("maskScript", maskScript);
            }
            if (StringUtils.hasText(policyCode)) {
                result.put("policyCode", policyCode);
            }
            if (!params.isEmpty()) {
                result.put("params", params);
            }
            return result;
        }
    }
}