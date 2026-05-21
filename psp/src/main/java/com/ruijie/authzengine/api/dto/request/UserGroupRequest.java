package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户组目录写入请求。
 */
@Data
@Schema(description = "用户组目录写入请求")
public class UserGroupRequest {

    @NotBlank(message = "租户标识不能为空")
    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @NotBlank(message = "应用标识不能为空")
    @Schema(description = "应用标识", example = "CRM")
    private String appCode;

    @NotBlank(message = "用户组编码不能为空")
    @Schema(description = "用户组编码", example = "GROUP-OPS")
    private String groupCode;

    @NotBlank(message = "用户组名称不能为空")
    @Schema(description = "用户组名称", example = "运营组")
    private String groupName;

    @Schema(description = "状态", example = "ENABLED")
    private String status;
}