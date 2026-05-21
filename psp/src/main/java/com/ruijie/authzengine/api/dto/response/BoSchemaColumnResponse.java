package com.ruijie.authzengine.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BO 元数据列信息响应 DTO，用于 schema-preview 接口返回。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "BO 元数据列信息")
public class BoSchemaColumnResponse {

    @Schema(description = "物理表名")
    private String tableName;

    @Schema(description = "列名（物理列名）")
    private String columnName;

    @Schema(description = "列类型，如 VARCHAR(64)、BIGINT、DATETIME 等")
    private String columnType;

    @Schema(description = "是否为主键列")
    private boolean primaryKey;

    @Schema(description = "是否允许为空")
    private boolean nullable;

    @Schema(description = "列注释")
    private String comment;
}
