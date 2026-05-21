package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 授权分配写入请求。
 */
@Data
@Schema(description = "授权分配写入请求")
public class AuthAssignmentRequest {

    @NotBlank(message = "租户标识不能为空")
    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @NotBlank(message = "应用标识不能为空")
    @Schema(description = "应用标识", example = "CRM")
    private String appCode;

    @NotBlank(message = "主体模型不能为空")
    @Schema(description = "主体模型", example = "SUB_USER")
    private String subjectModel;

    @NotBlank(message = "主体标识不能为空")
    @Schema(description = "主体标识", example = "U100")
    private String subjectId;

    @Schema(description = "权限项主键", example = "3001")
    private Long permItemId;

    @Schema(description = "策略模板编码", example = "DATA_SCOPE_DEPT")
    private String policyTemplateCode;

    @Schema(description = "策略参数", example = "{\"dept\":\"FIN\"}")
    private String policyParams;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;
}
