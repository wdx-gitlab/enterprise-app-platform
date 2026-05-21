package com.ruijie.authzengine.application.sdk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 主体范围查询条件。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectScopedQuery {

    private String tenantId;

    private String appCode;

    private String subjectId;

    @Builder.Default
    private String subjectModel = "SUB_USER";
}