package com.ruijie.authzengine.domain.model.governance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 业务对象（BO）元模型定义，对应 authz_bo_meta_model 表的一行记录。
 * <p>
 * BO 元模型是数据权限（data-scope）场景的核心配置，
 * 通过 schemaJson 描述业务对象的字段结构，
 * 通过 resolver 指定对应的 Hook Bean 用于运行时属性补全。
 * PDP 匹配到 RES_DATA_BO 类型权限后，会通过 BoMetaModelAdapter 调用对应 Hook。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoMetaModelDefinition {

    /** 主键。 */
    private Long id;

    /** 租户标识。 */
    private String tenantId;

    /** 应用标识。 */
    private String appCode;

    /** BO 编码，唯一约束 uk_authz_bo_meta_model(tenant_id, app_code, bo_code)，如 CONTRACT。 */
    private String boCode;

    /** BO 名称（显示用），如"合同"。 */
    private String boName;

    /**
     * BO 字段结构 Schema（JSON）。
     * <p>示例：{"fields":[{"code":"deptId","type":"string"},{"code":"status","type":"string"}]}。</p>
     */
    private String schemaJson;

    /** 适配器类型，默认 JAVA_BEAN。 */
    private String adapterType;

    /** Hook Bean 名称，用于运行时数据属性补全，默认 noopHook。 */
    private String resolver;
}