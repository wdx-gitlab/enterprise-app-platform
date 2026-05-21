package com.ruijie.authzengine.application.sdk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 仅按租户维度分页查询的通用条件。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantPageQuery {

    private String tenantId;

    private String keyword;

    @Builder.Default
    private int pageNo = 1;

    @Builder.Default
    private int pageSize = 20;
}