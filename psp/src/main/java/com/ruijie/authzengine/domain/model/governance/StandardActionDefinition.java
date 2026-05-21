package com.ruijie.authzengine.domain.model.governance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标准动作定义，对应 authz_std_act_dict 表的一行记录。
 * <p>
 * 统一管理系统中所有可用的操作动作（如 READ、APPROVE、DELETE），
 * 通过 authz_permission_item.act_code 引用。
 * V3 迁移后增加了 act_aliases 别名字段，支撑 Shadow Mode 动作归一。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardActionDefinition {

    /** 主键。 */
    private Long id;

    /** 租户标识，全局动作用 __GLOBAL__。 */
    private String tenantId;

    /** 动作编码，唯一约束 uk_authz_std_act_dict(tenant_id, act_code)，如 READ、APPROVE。 */
    private String actCode;

    /** 动作名称（显示用），如"查看"、"审批"。 */
    private String actName;

    /** 动作类型：STANDARD（通用）/ BUSINESS（业务特定）/ EXTENDED（扩展）。 */
    private String actType;

    /** 适用的资源类别，如 ALL、RES_DATA_BO，为空表示不限。 */
    private String resCategory;

    /** 风险等级（1-5），用于审计和风控展示。 */
    private Integer riskLevel;
}