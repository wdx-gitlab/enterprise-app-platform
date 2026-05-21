package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.domain.model.decision.AuthzDecision;
import com.ruijie.authzengine.domain.model.decision.AuthzRequest;
import com.ruijie.authzengine.domain.model.ops.AuthzAuditRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 鉴权应用服务（Application 层），位于 Controller 与 AuthzFacade 之间。
 *
 * <p>职责：
 * <ol>
 *   <li>把鉴权请求委托给 {@link AuthzFacade} 执行</li>
 *   <li>鉴权完成后异步写入审计日志（审计失败不影响鉴权结果）</li>
 * </ol>
 */
@Service
@Slf4j
public class AuthzDecisionAppService {

    private final AuthzFacade authzFacade;

    private final AuthzAuditAppService authzAuditAppService;

    @Autowired
    public AuthzDecisionAppService(AuthzFacade authzFacade, AuthzAuditAppService authzAuditAppService) {
        this.authzFacade = authzFacade;
        this.authzAuditAppService = authzAuditAppService;
    }

    public AuthzDecisionAppService(AuthzFacade authzFacade) {
        this(authzFacade, AuthzAuditAppService.noop());
    }

    /**
     * 触发治理增强鉴权流程，鉴权完成后写审计日志。
     *
     * @param request 鉴权请求
     * @return 鉴权决策结果
     */
    public AuthzDecision checkWithGovernance(AuthzRequest request) {
        long startAt = System.currentTimeMillis();
        log.debug("[鉴权应用服务] 开始鉴权: traceId={}, tenantId={}, subjectId={}",
            request.getTraceId(),
            request.getTenantId(),
            request.getSubject() == null ? null : request.getSubject().getId());
        // 步骤 1：委托给 AuthzFacade 执行核心鉴权链路
        AuthzDecision decision = authzFacade.checkWithGovernance(request);
        // 步骤 2：若决策已关联审计记录则直接返回（避免重复写入）
        if (decision.getAuditLogId() != null) {
            return decision;
        }
        // 步骤 3：写入鉴权审计日志，失败仅记录错误，不影响鉴权结果
        try {
            AuthzAuditRecord authzAuditRecord = authzAuditAppService.recordDecision(
                request,
                decision,
                System.currentTimeMillis() - startAt
            );
            if (authzAuditRecord != null && authzAuditRecord.getAuditLogId() != null) {
                decision.setAuditLogId(String.valueOf(authzAuditRecord.getAuditLogId()));
            }
        } catch (Exception exception) {
            log.error("鉴权审计写入失败 requestId={}", request.getTraceId(), exception);
        }
        long costMs = System.currentTimeMillis() - startAt;
        log.info("[鉴权应用服务] 鉴权完成: traceId={}, decision={}, costMs={}",
            request.getTraceId(),
            decision.getDecision(),
            costMs);
        return decision;
    }
}