package com.ruijie.uspportal.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("usp_login_session")
public class LoginSessionEntity {

    @TableId
    private String id;

    private String userId;

    private String tenantCode;

    private String orgCode;

    private String authMode;

    private String loginIp;

    private String userAgent;

    private String status;

    private LocalDateTime lastActiveTime;

    private LocalDateTime expireTime;

    private LocalDateTime logoutTime;

    private String logoutReason;

    private LocalDateTime createdTime;
}
