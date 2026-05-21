package com.ruijie.authzengine.application.sdk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SDK 主体引用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthzSubjectRef {

    /** 主体标识。 */
    private String subjectId;

    /** 主体类型，默认 SUB_USER。 */
    @Builder.Default
    private String subjectModel = "SUB_USER";
}