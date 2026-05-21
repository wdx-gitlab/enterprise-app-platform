package com.ruijie.authzengine.application.sdk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 按租户与应用维度分页查询的通用条件。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantAppPageQuery {

    private String tenantId;

    private String appCode;

    private String keyword;

    @Builder.Default
    private int pageNo = 1;

    @Builder.Default
    private int pageSize = 20;
}