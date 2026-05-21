package com.ruijie.authzengine.domain.model.governance.resource;

import com.baomidou.mybatisplus.annotation.TableField;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 菜单资源定义，对应 usp_menu_item 表的一行记录。
 * <p>
 * 资源模型编码为 RES_UI_MENU。菜单支持树形层级（通过 parentId 关联），
 * 治理界面按菜单树分配权限，鉴权时通过 menu_code 匹配 AuthzResource.resourceCode。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysResMenu {

    /** 主键。 */
    private Long id;

    /** 租户标识。 */
    private String tenantId;

    /** 租户编码。 */
    private String tenantCode;

    /** 应用标识。 */
    private String appCode;

    /** 所属应用 ID，可选。 */
    private Long appId;

    /** 菜单编码，唯一约束 uk_menu_item_tenant_app_code(tenant_code, app_code, menu_code)。 */
    private String menuCode;

    /** 菜单名称。 */
    private String menuName;

    /** 菜单图标。 */
    private String menuIcon;

    /** 菜单类型：DIRECTORY / MENU / LINK。 */
    private String menuType;

    /** 前端路由路径，如 /contracts。 */
    private String routePath;

    /** 跳转链接（LINK 类型使用）。 */
    private String targetUrl;

    /** 父菜单编码，根菜单为 null；用于领域层语义传递，持久化时解析为 parentId。 */
    private String parentMenuCode;

    /** 父节点主键 ID，根节点为 null。 */
    private Long parentId;

    /** 排序号，数值越小越靠前，默认 0。 */
    private Integer sortNo;

    /** 树层级，根节点为 1。 */
    private Integer treeLevel;

    /** 树路径，如 /1/2/3。 */
    private String treePath;

    /** 关联的 PSP 权限编码。 */
    private String permissionCode;

    /** 显示表达式，用于动态控制菜单可见性。 */
    private String visibleExpression;

    /** 发布状态：DRAFT / PUBLISHED。 */
    private String publishStatus;

    /** 状态：ENABLED / DISABLED。 */
    private String status;

    /** 宿主系统透传的动态扩展属性。 */
    @TableField(exist = false)
    private Map<String, Object> attributes;
}