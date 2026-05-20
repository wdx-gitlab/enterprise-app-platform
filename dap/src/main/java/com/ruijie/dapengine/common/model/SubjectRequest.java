package com.ruijie.dapengine.common.model;

import lombok.Data;

import java.util.List;

/**
 * 创建/更新 Subject 的请求体，包含 Subject 基本信息与字段列表。
 */
@Data
public class SubjectRequest {

    private SubjectInfo subject;
    private List<FieldConfigRequest> fields;

    @Data
    public static class SubjectInfo {
        /** Subject 唯一标识，创建必填，正则 ^[A-Z][A-Z0-9_]{1,29}$ */
        private String code;
        /** 显示名称，必填，≤128 字符 */
        private String name;
        /** 可选描述 */
        private String description;
        /** 是否树形主数据，默认 false */
        @com.fasterxml.jackson.annotation.JsonProperty("isTree")
        private boolean isTree = false;
        /** 状态：1=启用，0=停用，默认 1 */
        private int status = 1;
    }
}
