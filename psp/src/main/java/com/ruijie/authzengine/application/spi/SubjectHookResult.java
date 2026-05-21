package com.ruijie.authzengine.application.spi;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 主体 Shadow Hook 返回结果。
 *
 * <p>当前正式口径下，Hook 只负责补充主体属性；主体关系统一来自引擎库
 * {@code authz_subject_relation}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectHookResult {

    /** 主体运行时属性。 */
    private Map<String, Object> attributes;
}