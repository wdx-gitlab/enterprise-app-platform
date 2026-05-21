package com.ruijie.authzengine.application.spi;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shadow Mode 字段映射元数据。
 *
 * <p>既解决宿主字段名与引擎领域字段名之间的映射，也支持前端动态表头与表单的生成校验。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelFieldSchema {

    /** 字段定义列表。 */
    private List<FieldDefinition> fields;

    /**
     * 单个字段定义。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldDefinition {

        /**
         * 宿主系统返回的 JSON Key（如 "userId"、"phone"）。
         */
        private String code;

        /**
         * 字段数据类型（如 "STRING"、"INTEGER"、"BOOLEAN"）。
         */
        private String type;

        /**
         * 后端核心角色映射标识：
         * <ul>
         *   <li>"ID"    → 映射至 DataItem.id</li>
         *   <li>"CODE"  → 映射至 DataItem.code</li>
         *   <li>"NAME"  → 映射至 DataItem.name</li>
         *   <li>"STATUS" → 映射至 DataItem.status</li>
         *   <li>为空时作为扩展字段放入 attributes</li>
         * </ul>
         */
        private String role;

        /**
         * 引擎内置领域模型对应字段名（如 "orgCode"）。
         * 非空时，非角色扩展属性映射回强类型实体特定字段；否则放入 attributes Map。
         */
        private String domainField;

        /**
         * 前端展示名称（表头/Label）。
         */
        private String label;

        /**
         * 是否必填，后端映射前校验。
         */
        private boolean required;
    }
}
