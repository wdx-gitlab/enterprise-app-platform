package com.ruijie.authzengine.domain.model.governance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 权限元模型定义，对应 authz_meta_model 表的一行记录。
 * <p>
 * 元模型是权限引擎最基础的注册单元，用于声明系统中有哪些主体类型、资源类型、动作类型、策略类型。
 * category 字段区分大类（SUBJECT / RESOURCE / ACTION / POLICY），
 * model_code 为该类型的唯一编码（如 SUB_USER、RES_UI_MENU、RES_DATA_BO 等）。
 * </p>
 *
 * @see com.ruijie.authzengine.domain.model.common.ResourceModelCode
 * @see com.ruijie.authzengine.domain.model.common.SubjectModelCode
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthMetaModelDefinition {

    /** 主键。 */
    private Long id;

    /** 租户标识。 */
    private String tenantId;

    /** 应用标识。 */
    private String appCode;

    /** 模型编码，如 SUB_USER、RES_UI_MENU、RES_DATA_BO，唯一约束 uk_authz_meta_model(tenant_id, app_code, model_code)。 */
    private String modelCode;

    /** 模型名称（显示用）。 */
    private String modelName;

    /** 大类：SUBJECT / RESOURCE / ACTION / POLICY。 */
    private String category;

    /**
     * 适配器类型，放开扩展预留，当前默认 JAVA_BEAN。
     * <p>后续可支持 GROOVY_SCRIPT、REST_API 等扩展。</p>
     */
    private String adapterType;

    /**
     * 解析器标识，默认 noopHook。
     * <p>当 category=RESOURCE 且为 BO 类型时，此字段指向具体的 Hook Bean 名称。</p>
     */
    private String resolver;

    /**
     * 模型视图 Schema（JSON），V2 迁移后新增。
     * <p>用于存储模型的结构化视图定义，治理界面可据此动态渲染。</p>
     */
    private String schemaView;
}