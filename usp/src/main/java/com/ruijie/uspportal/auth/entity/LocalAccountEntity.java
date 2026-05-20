package com.ruijie.uspportal.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("usp_local_account")
public class LocalAccountEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("login_name")
    private String loginName;

    @TableField("display_name")
    private String displayName;

    @TableField("password_hash")
    private String passwordHash;

    @TableField("password_encode_type")
    private String passwordEncodeType;

    @TableField("password_salt")
    private String passwordSalt;

    @TableField("email")
    private String email;

    @TableField("phone_num")
    private String phoneNum;

    @TableField("tenant_code")
    private String tenantCode;

    private String status;

    @TableField("is_admin")
    private Boolean admin;

    @TableField("force_reset_password")
    private Boolean forceResetPassword;

    @TableField("created_by")
    private String createdBy;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_by")
    private String updatedBy;

    @TableField("updated_time")
    private LocalDateTime updatedTime;

    @TableField("is_deleted")
    private Integer deleted;

    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;
}
