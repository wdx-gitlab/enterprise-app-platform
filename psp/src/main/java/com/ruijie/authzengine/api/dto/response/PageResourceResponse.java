package com.ruijie.authzengine.api.dto.response;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

/**
 * 页面资源响应。
 */
@Data
@Builder
@Schema(description = "页面资源响应")
public class PageResourceResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键", example = "2002")
    private Long id;

    @Schema(description = "租户标识")
    private String tenantId;

    @Schema(description = "应用标识")
    private String appCode;

    @Schema(description = "页面编码")
    private String pageCode;

    @Schema(description = "页面名称")
    private String pageName;

    @Schema(description = "菜单编码")
    private String menuCode;

    @Schema(description = "页面路径")
    private String pagePath;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "显示排序号")
    private Integer sortOrder;

    @Getter(value = AccessLevel.NONE)
    @JsonIgnore
    private Map<String, Object> attributes;

    /** 将动态扩展属性平铺到 JSON 顶层，供前端动态表头/表单使用。 */
    @JsonAnyGetter
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}