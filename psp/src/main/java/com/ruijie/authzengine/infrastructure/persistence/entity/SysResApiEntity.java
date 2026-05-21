package com.ruijie.authzengine.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API 资源持久化实体。
 */
@Data
@TableName("usp_api")
@EqualsAndHashCode(callSuper = true)
public class SysResApiEntity extends BaseEntity {

    @TableField("tenant_id")
    private String tenantId;

    @TableField("app_code")
    private String appCode;

    @TableField("api_code")
    private String apiCode;

    @TableField("api_name")
    private String apiName;

    @TableField("http_method")
    private String httpMethod;

    @TableField("uri_pattern")
    private String uriPattern;

    @TableField("status")
    private String status;
}