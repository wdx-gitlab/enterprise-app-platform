package com.ruijie.authzengine.domain.model.governance.subject;

import com.baomidou.mybatisplus.annotation.TableField;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 岗位目录定义，对应 sys_position 表的一行记录。
 * <p>
 * 作为 SUB_POSITION 类型主数据，用户通过 authz_subject_relation (relation_type=POSITION) 关联岗位。
 * 岗位挂在组织下（orgCode），可用于"审批岗"等业务场景的权限分配。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysPosition {

    /** 主键。 */
    private Long id;

    /** 租户标识。 */
    private String tenantId;

    /** 应用标识。 */
    private String appCode;

    /** 岗位编码，唯一约束 uk_sys_position(tenant_id, app_code, position_code)。 */
    private String positionCode;

    /** 岗位名称。 */
    private String positionName;

    /** 所属组织编码，关联 SysOrgNode。 */
    private String orgCode;

    /** 状态：ENABLED / DISABLED。 */
    private String status;

    /** 宿主系统透传的动态扩展属性。 */
    @TableField(exist = false)
    private Map<String, Object> attributes;
}