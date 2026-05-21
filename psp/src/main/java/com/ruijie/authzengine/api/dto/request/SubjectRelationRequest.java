package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 主体关系写入请求。
 */
@Data
@Schema(description = "主体关系写入请求")
public class SubjectRelationRequest {

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

    @NotBlank(message = "关联主体模型不能为空")
    @Schema(description = "关联主体模型", example = "SUB_ROLE")
    private String relatedSubjectModel;

    @NotBlank(message = "关联主体标识不能为空")
    @Schema(description = "关联主体标识", example = "ROLE-CONTRACT-ADMIN")
    private String relatedSubjectId;

    @NotBlank(message = "关系类型不能为空")
    @Schema(description = "关系类型", example = "MEMBER_OF")
    private String relationType;
}