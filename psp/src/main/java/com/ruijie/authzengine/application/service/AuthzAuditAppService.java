package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.domain.model.decision.AuthzDecision;
import com.ruijie.authzengine.domain.model.decision.DecisionType;
import com.ruijie.authzengine.domain.model.decision.AuthzRequest;
import com.ruijie.authzengine.domain.model.ops.AuthzAuditPage;
import com.ruijie.authzengine.domain.model.ops.AuthzAuditQuery;
import com.ruijie.authzengine.domain.model.ops.AuthzAuditRecord;
import com.ruijie.authzengine.domain.repository.AuthzAuditRepository;
import com.ruijie.authzengine.domain.repository.AuthzAuditWriteRepository;
import com.ruijie.authzengine.infrastructure.authz.HookExecutionAuditRecorder;
import java.util.Collections;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 鉴权审计应用服务。
 */
@Service
@Slf4j
public class AuthzAuditAppService {

    private final AuthzAuditRepository authzAuditReadRepository;

    private final AuthzAuditWriteRepository authzAuditWriteRepository;

    @Autowired
    public AuthzAuditAppService(
        AuthzAuditRepository authzAuditReadRepository,
        AuthzAuditWriteRepository authzAuditWriteRepository
    ) {
        this.authzAuditReadRepository = authzAuditReadRepository;
        this.authzAuditWriteRepository = authzAuditWriteRepository;
    }

    public AuthzAuditAppService(AuthzAuditRepository authzAuditReadRepository) {
        this(authzAuditReadRepository, null);
    }

    private AuthzAuditAppService() {
        this.authzAuditReadRepository = null;
        this.authzAuditWriteRepository = null;
    }

    public static AuthzAuditAppService noop() {
        return new AuthzAuditAppService();
    }

    /**
     * 记录单次鉴权决策。
     *
     * @param request 鉴权请求
     * @param decision 鉴权决策
     * @param costMs 决策耗时
     * @return 已保存审计记录
     */
    public AuthzAuditRecord recordDecision(AuthzRequest request, AuthzDecision decision, long costMs) {
        if (authzAuditWriteRepository == null) {
            log.debug("鉴权审计仓储未注入，跳过审计写入 requestId={}", request.getTraceId());
            return null;
        }
        AuthzAuditRecord authzAuditRecord = authzAuditWriteRepository.save(AuthzAuditRecord.builder()
            .requestId(request.getTraceId())
            .tenantId(request.getTenantId())
            .appCode(request.getAppCode())
            .subjectModel(request.getSubject().getType())
            .subjectId(request.getSubject().getId())
            .resourceModel(request.getResource().getResourceType())
            .resId(request.getResource().getResId())
            .actionCode(resolveActionCode(request))
            .decision(decision.getDecision().name())
            .matchedPermissionCodes(defaultList(decision.getMatchedPermissions()))
            .matchedAssignmentIds(defaultList(decision.getMatchedAssignmentIds()))
            .matchedDelegateIds(defaultList(decision.getMatchedDelegateIds()))
            .matchedPolicyTemplateCodes(defaultList(decision.getMatchedPolicyTemplateCodes()))
            .failureReason(resolveFailureReason(decision))
            .costMs(costMs)
            .hookStatus(resolveHookStatus(request))
            .hookCostMs(resolveHookCostMs(request))
            .attributeSnapshot(resolveHookAttrSnapshot(request))
            .build());
        log.info("鉴权审计写入完成 requestId={}, auditLogId={}", request.getTraceId(),
            authzAuditRecord == null ? null : authzAuditRecord.getAuditLogId());
        return authzAuditRecord;
    }

    /**
     * 按条件分页查询审计记录。
     *
     * @param query 查询条件
     * @return 审计分页结果
     */
    public AuthzAuditPage queryAuditLogs(AuthzAuditQuery query) {
        int pageNo = query.getPageNo();
        int pageSize = query.getPageSize();
        if (authzAuditReadRepository == null) {
            return AuthzAuditPage.builder()
                .records(Collections.emptyList())
                .pageNo(pageNo)
                .pageSize(pageSize)
                .total(0)
                .build();
        }
            return authzAuditReadRepository.query(query);
    }

    /**
     * 查询审计日志详情。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @param auditLogId 审计日志标识
     * @return 审计记录详情
     */
    public AuthzAuditRecord getAuditLog(String tenantId, String appCode, Long auditLogId) {
        if (authzAuditReadRepository == null) {
            return null;
        }
        return authzAuditReadRepository.findById(tenantId, appCode, auditLogId);
    }

    private String resolveHookStatus(AuthzRequest request) {
        Object value = getContextValue(request, HookExecutionAuditRecorder.CTX_HOOK_EXEC_STATUS);
        return value == null ? null : String.valueOf(value);
    }

    private Long resolveHookCostMs(AuthzRequest request) {
        Object value = getContextValue(request, HookExecutionAuditRecorder.CTX_HOOK_COST_MS);
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private String resolveHookAttrSnapshot(AuthzRequest request) {
        Object value = getContextValue(request, HookExecutionAuditRecorder.CTX_HOOK_ATTR_SNAPSHOT);
        return value == null ? null : String.valueOf(value);
    }

    private Object getContextValue(AuthzRequest request, String key) {
        Map<String, Object> ctx = request.getContext();
        return ctx == null ? null : ctx.get(key);
    }

    private java.util.List<String> defaultList(java.util.List<String> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private String resolveActionCode(AuthzRequest request) {
        if (request.getContext() != null) {
            Object normalizedActionCode = request.getContext().get("normalizedActionCode");
            if (normalizedActionCode != null && !String.valueOf(normalizedActionCode).trim().isEmpty()) {
                return String.valueOf(normalizedActionCode).trim();
            }
        }
        return request.getAction();
    }

    private String resolveFailureReason(AuthzDecision decision) {
        if (decision == null || decision.getDecision() == null) {
            return null;
        }
        return DecisionType.PERMIT.equals(decision.getDecision()) ? null : decision.getReason();
    }
}