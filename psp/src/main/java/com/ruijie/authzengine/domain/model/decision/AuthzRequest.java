package com.ruijie.authzengine.domain.model.decision;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一鉴权请求，封装一次完整鉴权所需的全部入参。
 * <p>
 * 由 API 层 AuthzRequestAssembler 从 DTO 转换而来，
 * 或由 AuthzFacade.checkByPermCode 根据权限项自动构建。
 * 核心四元组：主体（subject）+ 资源（resource）+ 动作（action）+ 上下文（context），
 * 外加租户隔离维度 tenantId / appCode。
 * </p>
 *
 * @see AuthzSubject
 * @see AuthzResource
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthzRequest {

    /** 租户标识，所有鉴权数据按租户隔离存储和匹配。 */
    private String tenantId;

    /** 应用标识，同一租户下区分不同业务系统。 */
    private String appCode;

    /** 鉴权主体，描述"谁"在请求访问。 */
    private AuthzSubject subject;

    /** 鉴权资源，描述要访问"什么"。 */
    private AuthzResource resource;

    /**
     * 动作编码，如 VIEW、EDIT、APPROVE、DELETE 等。
     * <p>对应 authz_permission_item.act_code。</p>
     */
    private String action;

    /**
     * 额外上下文参数，可携带 roles、orgs、positions、groups 等预传身份集合，
     * 或 simulateHookError 等调试标记；PIP 阶段会合并到 AuthzContext.attributes。
     */
    private Map<String, Object> context;

    /** 链路追踪标识，贯穿整条鉴权链路及审计日志。 */
    private String traceId;
}