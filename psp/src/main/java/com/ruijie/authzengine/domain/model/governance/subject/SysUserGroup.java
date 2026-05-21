package com.ruijie.authzengine.domain.model.governance.subject;

import com.baomidou.mybatisplus.annotation.TableField;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户组目录定义，对应 authz_usergroup 表的一行记录。
 * <p>
 * 作为 SUB_GROUP 类型主数据，用户通过 authz_subject_relation (relation_type=GROUP) 关联用户组。
 * 用户组适用于跨组织、跨部门的权限分配场景。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysUserGroup {

    /** 主键。 */
    private Long id;

    /** 租户标识。 */
    private String tenantId;

    /** 应用标识。 */
    private String appCode;

    /** 用户组编码，唯一约束 uk_authz_usergroup(tenant_id, app_code, group_code)。 */
    private String groupCode;

    /** 用户组名称。 */
    private String groupName;

    /** 状态：ENABLED / DISABLED。 */
    private String status;

    /** 宿主系统透传的动态扩展属性。 */
    @TableField(exist = false)
    private Map<String, Object> attributes;
}