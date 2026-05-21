package com.ruijie.authzengine.domain.model.decision;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 鉴权资源，描述当前鉴权动作所作用的目标对象。
 * <p>
 * 资源由 {@code resourceType}（大类）+ {@code resId}（对应资源表主键）二段定位。
 * resourceType 取值为 RES_DATA_BO、RES_UI_MENU、RES_UI_PAGE、RES_UI_COMPONENT、RES_API。
 * resId 为空表示类别级权限，非空时指向对应资源表的主键 ID。
 * </p>
 *
 * @see AuthzRequest
 * @see PermissionGrant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthzResource {

    /**
     * 资源大类，如 RES_DATA_BO、RES_UI_MENU、RES_UI_PAGE、RES_UI_COMPONENT、RES_API。
     */
    private String resourceType;

    /**
     * 资源标识，对应资源表的主键 ID。
     * <p>
     * 空字符串或 null 表示类别级权限，非空时指向对应资源表（authz_bo_meta_model、usp_menu_item 等）的主键。
     * </p>
     */
    private String resId;
}