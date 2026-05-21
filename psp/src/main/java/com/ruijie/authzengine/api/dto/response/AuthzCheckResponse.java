package com.ruijie.authzengine.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * 单次鉴权响应 DTO。
 */
@Data
@Builder
@Schema(description = "单次鉴权响应")
public class AuthzCheckResponse {

    @Schema(description = "决策结果", example = "PERMIT")
    private String decision;

    @Schema(description = "决策原因", example = "PERMIT")
    private String reason;

    @Schema(description = "命中的权限项编码")
    private List<String> matchedPermissionCodes;

    @Schema(description = "命中的授权记录标识")
    private List<String> matchedAssignmentIds;

    @Schema(description = "命中的委托记录标识")
    private List<String> matchedDelegateIds;

    @Schema(description = "命中的策略模板编码")
    private List<String> matchedPolicyTemplateCodes;

    @Schema(description = "附带义务")
    private Map<String, Object> obligations;

    @Schema(description = "审计日志标识", example = "9000001")
    private String auditLogId;
}