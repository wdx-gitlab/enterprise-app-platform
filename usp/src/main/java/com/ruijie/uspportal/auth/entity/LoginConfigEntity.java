package com.ruijie.uspportal.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruijie.uspportal.config.mybatis.JsonStringTypeHandler;
import org.apache.ibatis.type.JdbcType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "usp_login_config", autoResultMap = true)
public class LoginConfigEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("internal_login_enabled")
    private Boolean internalLoginEnabled;

    @TableField("sso_login_enabled")
    private Boolean ssoLoginEnabled;

    @TableField("default_login_mode")
    private String defaultLoginMode;

    @TableField(value = "password_policy_json", typeHandler = JsonStringTypeHandler.class, jdbcType = JdbcType.VARCHAR)
    private String passwordPolicyJson;

    @TableField("sso_button_text")
    private String ssoButtonText;

    @TableField("account_mapping_rule")
    private String accountMappingRule;

    private String status;

    @TableField("updated_time")
    private LocalDateTime updatedTime;
}
