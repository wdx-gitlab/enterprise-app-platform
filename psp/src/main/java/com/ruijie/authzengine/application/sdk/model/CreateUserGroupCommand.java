package com.ruijie.authzengine.application.sdk.model;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户组创建命令。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserGroupCommand {

    private String tenantId;

    private String appCode;

    private String groupCode;

    private String groupName;

    private String status;

    private Map<String, Object> attributes;
}