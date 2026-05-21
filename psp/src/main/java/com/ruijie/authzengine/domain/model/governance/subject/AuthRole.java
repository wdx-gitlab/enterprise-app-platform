package com.ruijie.authzengine.domain.model.governance.subject;

import com.baomidou.mybatisplus.annotation.TableField;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色目录定义，对应 authz_role 表的一行记录。
 * <p>
 * 作为 SUB_ROLE 类型主数据，用户通过 authz_subject_relation (relation_type=ROLE) 关联角色。
 * 角色是典型 RBAC 场景中最核心的间接主体，
 * 大量授权分配记录（authz_assignment）以角色为 subject_model=SUB_ROLE。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRole {

    /** 主键。 */
    private Long id;

    /** 租户标识。 */
    private String tenantId;

    /** 应用标识。 */
    private String appCode;

    /** 角色编码，唯一约束 uk_authz_role(tenant_id, app_code, role_code)。 */
    private String roleCode;

    /** 角色名称。 */
    private String roleName;

    /** 角色作用域，如 APP（应用级）、TENANT（租户级）。 */
    private String roleScope;

    /** 状态：ENABLED / DISABLED。 */
    private String status;

    /** 宿主系统透传的动态扩展属性。 */
    @TableField(exist = false)
    private Map<String, Object> attributes;
}