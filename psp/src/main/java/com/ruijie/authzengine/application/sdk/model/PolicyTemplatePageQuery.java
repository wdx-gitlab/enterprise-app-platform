package com.ruijie.authzengine.application.sdk.model;

import com.ruijie.authzengine.domain.model.common.PolicyTemplateType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标准策略模板分页查询条件。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyTemplatePageQuery {

    private String tenantId;

    private String keyword;

    private PolicyTemplateType polType;

    @Builder.Default
    private int pageNo = 1;

    @Builder.Default
    private int pageSize = 20;
}