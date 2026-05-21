package com.ruijie.authzengine.application.sdk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 权限项分页查询条件。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionItemPageQuery {

    private String tenantId;

    private String appCode;

    private String keyword;

    private PermissionResourceModel resModelCode;

    private String resId;

    @Builder.Default
    private int pageNo = 1;

    @Builder.Default
    private int pageSize = 20;
}