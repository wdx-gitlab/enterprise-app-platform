package com.ruijie.dapengine.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 试连接结果 DTO（POST /dap/{subjectCode}/sync/test-connect）。
 *
 * <p>接口本身始终返回 HTTP 200；连接失败通过 {@code success=false} + {@code errorMsg} 表示，
 * 不使用 HTTP 错误状态码。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestConnectResultDTO {

    /** 数据提供者类型：HTTP / DB / MQ */
    private String providerType;

    /** 是否连接成功 */
    private boolean success;

    /** 从外部数据源识别到的字段名列表（MQ 类型始终为空列表） */
    private List<String> sourceFields;

    /** 样例数据行（最多 5 条，MQ 类型始终为空列表） */
    private List<Map<String, Object>> sampleRows;

    /** 提示信息列表（MQ 类型用于告知用户需手动配置字段） */
    private List<String> warnings;

    /** 连接失败时的错误描述；成功时为 null */
    private String errorMsg;

    /**
     * 构建一个成功结果（无 warnings）。
     */
    public static TestConnectResultDTO success(String providerType,
                                                List<String> sourceFields,
                                                List<Map<String, Object>> sampleRows) {
        return TestConnectResultDTO.builder()
                .providerType(providerType)
                .success(true)
                .sourceFields(sourceFields)
                .sampleRows(sampleRows)
                .warnings(Collections.emptyList())
                .build();
    }

    /**
     * 构建一个失败结果。
     */
    public static TestConnectResultDTO failure(String providerType, String errorMsg) {
        return TestConnectResultDTO.builder()
                .providerType(providerType)
                .success(false)
                .sourceFields(Collections.emptyList())
                .sampleRows(Collections.emptyList())
                .warnings(Collections.emptyList())
                .errorMsg(errorMsg)
                .build();
    }
}
