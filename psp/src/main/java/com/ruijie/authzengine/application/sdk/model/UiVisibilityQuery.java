package com.ruijie.authzengine.application.sdk.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UI 组件可见性查询条件。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UiVisibilityQuery {

    private String tenantId;

    private String appCode;

    private String subjectId;

    @Builder.Default
    private String subjectModel = "SUB_USER";

    private List<String> componentCodes;
}