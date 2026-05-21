package com.ruijie.authzengine.domain.model.governance.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 权限项骨架领域对象，对应 authz_permission_item 表的一行记录。
 * <p>
 * 权限项是授权的最小单元，定义了"对哪个资源的哪个动作"的编码映射。
 * 授权分配（authz_assignment）通过 perm_item_id 引用权限项，
 * PDP 匹配时联合 res_model_code + res_id + act_code 与 AuthzRequest 进行比对。
 * </p>
 * <p>uk_authz_permission_item(tenant_id, app_code, res_model_code, res_id, act_code) 保证同一资源+动作不重复定义。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthPermissionItem {

    /** 主键。 */
    private Long id;

    /** 租户标识。 */
    private String tenantId;

    /** 应用标识。 */
    private String appCode;

    /**
     * 权限项编码，全局唯一的业务可读标识。
     * <p>示例：CONTRACT_APPROVE、CONTRACT_READ。AuthzFacade.checkByPermCode 直接通过此编码查找权限项。</p>
     */
    private String permCode;

    /** 资源模型编码，如 CONTRACT、RES_UI_MENU，关联 authz_meta_model.model_code。 */
    private String resModelCode;

    /**
     * 资源标识，空字符串表示模型级权限。
     * <p>非空时表示实例级权限，值为具体资源编码（如菜单编码、API 编码）。</p>
     */
    private String resId;

    /** 动作编码，如 APPROVE、READ、DELETE，关联 authz_std_act_dict.act_code。 */
    private String actCode;

    /**
     * 失败策略，控制权限项匹配失败时的行为。
     * <p>可选值如 DENY（直接拒绝）、SKIP（跳过继续）等，为空时采用默认策略。</p>
     */
    private String failStrategy;
}