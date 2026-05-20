package com.ruijie.uspportal.tenant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruijie.uspportal.config.mybatis.JsonStringTypeHandler;
import org.apache.ibatis.type.JdbcType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "usp_tenant", autoResultMap = true)
public class TenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("tenant_name")
    private String tenantName;

    @TableField("tenant_type")
    private String tenantType;

    @TableField(value = "capability_scope", typeHandler = JsonStringTypeHandler.class, jdbcType = JdbcType.VARCHAR)
    private String capabilityScope;

    private String status;

    private String remark;

    @TableField("activated_time")
    private LocalDateTime activatedTime;

    @TableField("is_deleted")
    private Integer deleted;
}
