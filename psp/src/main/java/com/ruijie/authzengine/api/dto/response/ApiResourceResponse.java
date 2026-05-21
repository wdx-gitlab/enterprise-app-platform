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
 * API 资源目录响应。
 */
@Data
@Builder
@Schema(description = "API 资源目录响应")
public class ApiResourceResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "租户标识")
    private String tenantId;

    @Schema(description = "应用标识")
    private String appCode;

    @Schema(description = "API 编码")
    private String apiCode;

    @Schema(description = "API 名称")
    private String apiName;

    @Schema(description = "HTTP 方法")
    private String httpMethod;

    @Schema(description = "URI 模式")
    private String uriPattern;

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