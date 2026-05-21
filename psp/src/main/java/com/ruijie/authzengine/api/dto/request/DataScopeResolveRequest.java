package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * data-scope 占位请求。
 */
@Data
@Schema(description = "data-scope 占位请求")
public class DataScopeResolveRequest {

    @NotBlank(message = "租户标识不能为空")
    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @NotBlank(message = "应用标识不能为空")
    @Schema(description = "应用标识", example = "CRM")
    private String appCode;

    @NotBlank(message = "策略模板编码不能为空")
    @Schema(description = "策略模板编码", example = "DATA_SCOPE_DEPT")
    private String policyTemplateCode;

    @Valid
    @NotNull(message = "主体信息不能为空")
    @Schema(description = "主体信息")
    private AuthzSubjectRequest subject;

    @Valid
    @NotNull(message = "资源信息不能为空")
    @Schema(description = "资源信息")
    private AuthzResourceRequest resource;

    @Schema(description = "待翻译的语义条件，仅在首版 SQL 翻译场景使用", example = "stage = 'APPROVING'")
    private String semanticCondition;

    @Schema(description = "额外上下文")
    private Map<String, Object> context;
}