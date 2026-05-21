package com.ruijie.authzengine.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.Data;

/**
 * 治理分页查询请求。
 */
@Data
@Schema(description = "治理分页查询请求")
public class PageQueryRequest {

    @Schema(description = "租户标识", example = "T001")
    private String tenantId;

    @Schema(description = "应用标识", example = "CRM")
    private String appCode;

    @Schema(description = "关键字，默认按编码和名称模糊匹配", example = "CONTRACT")
    private String keyword;

    @Schema(description = "状态", example = "ENABLED")
    private String status;

    @Min(value = 1, message = "页码必须大于等于 1")
    @Schema(description = "页码", example = "1", defaultValue = "1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "分页大小必须大于等于 1")
    @Max(value = 200, message = "分页大小不能超过 200")
    @Schema(description = "分页大小", example = "20", defaultValue = "20")
    private Integer pageSize = 20;
}