package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 单次鉴权请求 DTO。
 */
@Data
@Schema(description = "单次鉴权请求")
public class AuthzCheckRequest {

    @NotBlank(message = "租户标识不能为空")
    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @NotBlank(message = "应用标识不能为空")
    @Schema(description = "应用标识", example = "CRM")
    private String appCode;

    @Valid
    @NotNull(message = "主体信息不能为空")
    @Schema(description = "主体信息")
    private AuthzSubjectRequest subject;

    @Valid
    @NotNull(message = "资源信息不能为空")
    @Schema(description = "资源信息")
    private AuthzResourceRequest resource;

    @NotBlank(message = "动作编码不能为空")
    @Schema(description = "动作编码", example = "APPROVE")
    private String action;

    @Schema(description = "额外上下文，可传 roles、orgs、positions、groups 或 simulateHookError")
    private Map<String, Object> context;

    @Schema(description = "链路追踪标识", example = "TRACE-20260402-0001")
    private String traceId;
}