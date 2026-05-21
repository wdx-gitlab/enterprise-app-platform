package com.ruijie.authzengine.domain.model.governance.resource;

import com.baomidou.mybatisplus.annotation.TableField;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 页面资源定义，对应 usp_page 表的一行记录。
 * <p>
 * 资源模型编码为 RES_UI_PAGE。页面挂在菜单下（menuCode），
 * 鉴权时通过 page_code 匹配 AuthzResource.resourceCode。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysResPage {

    /** 主键。 */
    private Long id;

    /** 租户标识。 */
    private String tenantId;

    /** 应用标识。 */
    private String appCode;

    /** 页面编码，唯一约束 uk_usp_page(tenant_id, app_code, page_code)。 */
    private String pageCode;

    /** 页面名称。 */
    private String pageName;

    /** 所属菜单编码，关联 SysResMenu。 */
    private String menuCode;

    /** 前端页面路径，如 /contracts/list。 */
    private String pagePath;

    /** 状态：ENABLED / DISABLED。 */
    private String status;

    /** 显示排序号，数值越小越靠前，默认 0。 */
    private Integer sortOrder;

    /** 宿主系统透传的动态扩展属性。 */
    @TableField(exist = false)
    private Map<String, Object> attributes;
}