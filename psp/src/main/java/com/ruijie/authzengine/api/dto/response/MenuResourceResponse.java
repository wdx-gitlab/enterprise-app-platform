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
 * 菜单项响应。
 */
@Data
@Builder
@Schema(description = "菜单项响应")
public class MenuResourceResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键", example = "2001")
    private Long id;

    @Schema(description = "租户标识")
    private String tenantId;

    @Schema(description = "租户编码")
    private String tenantCode;

    @Schema(description = "应用标识")
    private String appCode;

    @Schema(description = "所属应用 ID")
    private Long appId;

    @Schema(description = "菜单编码")
    private String menuCode;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "菜单图标")
    private String menuIcon;

    @Schema(description = "菜单类型")
    private String menuType;

    @Schema(description = "路由路径")
    private String routePath;

    @Schema(description = "跳转链接")
    private String targetUrl;

    @Schema(description = "父菜单编码")
    private String parentMenuCode;

    @Schema(description = "排序号")
    private Integer sortNo;

    @Schema(description = "树层级")
    private Integer treeLevel;

    @Schema(description = "树路径")
    private String treePath;

    @Schema(description = "PSP 权限编码")
    private String permissionCode;

    @Schema(description = "显示表达式")
    private String visibleExpression;

    @Schema(description = "发布状态")
    private String publishStatus;

    @Schema(description = "状态")
    private String status;

    @Getter(value = AccessLevel.NONE)
    @JsonIgnore
    private Map<String, Object> attributes;

    /** 将动态扩展属性平铺到 JSON 顶层，供前端动态表头/表单使用。 */
    @JsonAnyGetter
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}