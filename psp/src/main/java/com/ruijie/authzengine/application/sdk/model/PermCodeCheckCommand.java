package com.ruijie.authzengine.application.sdk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 权限编码快捷鉴权命令。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermCodeCheckCommand {

    private String tenantId;

    private String appCode;

    /** 当前主体标识；当前实现与内部快捷鉴权入口保持一致，默认按用户主体语义解释。 */
    private String subjectId;

    private String permissionCode;

    /** 资源实例标识，可为空。 */
    private String resourceId;
}