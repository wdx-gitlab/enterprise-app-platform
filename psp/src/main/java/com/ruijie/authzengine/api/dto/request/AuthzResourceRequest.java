package com.ruijie.authzengine.api.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 资源请求 DTO。
 */
@Data
@Schema(description = "鉴权资源")
public class AuthzResourceRequest {

    @NotBlank(message = "资源模型不能为空")
    @JsonAlias("type")
    @Schema(description = "资源模型", example = "RES_DATA_BO")
    private String resourceModel;

    @JsonAlias("resId")
    @NotBlank(message = "资源ID不能为空")
    @Schema(description = "资源 ID，对应资源表主键或权限项表 authz_permission_item.res_id", example = "101")
    private String resourceId;
}