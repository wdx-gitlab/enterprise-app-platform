package com.ruijie.authzengine.domain.model.common;

/**
 * 标准资源模型编码，与 authz_meta_model.model_code（category=RESOURCE）一一对应。
 * <p>
 * 枚举值决定鉴权链中 AuthzResource.resolveResourceType() 的取值范围，
 * 以及 PDP 匹配时按哪类资源主数据表（usp_menu_item / usp_page / usp_component / usp_api）检索。
 * </p>
 *
 * @see com.ruijie.authzengine.domain.model.decision.AuthzResource
 */
public enum ResourceModelCode {

    /**
     * 菜单资源，对应 usp_menu_item 表。
     * <p>仅用于元模型注册与治理侧资源目录管理，不作为 authz_permission_item 的资源类型。</p>
     */
    RES_UI_MENU,

    /**
     * 页面资源，对应 usp_page 表。
     * <p>仅用于元模型注册与治理侧资源目录管理，不作为 authz_permission_item 的资源类型。</p>
     */
    RES_UI_PAGE,

    /**
     * 页面组件资源（按钮、区块等），对应 usp_component 表。
     * <p>仅用于元模型注册与治理侧资源目录管理，不作为 authz_permission_item 的资源类型。</p>
     */
    RES_UI_COMPONENT,

    /**
     * API 接口资源，对应 usp_api 表。
     * <p>authz_permission_item 合法资源类型之一。</p>
     */
    RES_API,

    /**
     * 业务对象数据资源，对应 authz_bo_meta_model 表 + BO Hook 扩展。
     * <p>authz_permission_item 合法资源类型之一。</p>
     */
    RES_DATA_BO
}