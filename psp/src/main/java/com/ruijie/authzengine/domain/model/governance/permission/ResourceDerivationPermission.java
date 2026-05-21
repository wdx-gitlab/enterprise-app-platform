package com.ruijie.authzengine.domain.model.governance.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 派生权限关联定义。
 *
 * <p>用于描述页面、组件、API 与权限项之间的派生关系；菜单不直接出现在本模型中，
 * 菜单可见性由页面派生关系反向推导。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDerivationPermission {

    /** 主键。 */
    private Long id;

    /** 租户标识。 */
    private String tenantId;

    /** 应用标识。 */
    private String appCode;

    /** 资源类型：RES_UI_PAGE / RES_UI_COMPONENT / RES_API。 */
    private String resType;

    /** 资源主键 ID，指向对应 usp_* 表。 */
    private Long resId;

    /** 派生来源权限项 ID。 */
    private Long permItemId;

    /** 同一资源下的展示或归并顺序。 */
    private Integer sortOrder;

    /** 资源编码（从 usp_* 反查，仅用于展示层，不持久化）。 */
    private transient String resCode;

    /** 资源名称（从 usp_* 反查，仅用于展示层，不持久化）。 */
    private transient String resName;

    /** 权限项编码（从 authz_permission_item 反查，仅用于展示层，不持久化）。 */
    private transient String permCode;
}