package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 角色目录写入请求。
 */
@Data
@Schema(description = "角色目录写入请求")
public class RoleRequest {

    @NotBlank(message = "租户标识不能为空")
    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @NotBlank(message = "应用标识不能为空")
    @Schema(description = "应用标识", example = "CRM")
    private String appCode;

    @NotBlank(message = "角色编码不能为空")
    @Schema(description = "角色编码", example = "ROLE-CONTRACT-ADMIN")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Schema(description = "角色名称", example = "合同管理员")
    private String roleName;

    @Schema(description = "角色范围", example = "APP")
    private String roleScope;

    @Schema(description = "状态", example = "ENABLED")
    private String status;
}