package com.ruijie.authzengine.domain.model.governance.resource;

import com.baomidou.mybatisplus.annotation.TableField;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 组件资源定义，对应 usp_component 表的一行记录。
 * <p>
 * 资源模型编码为 RES_UI_COMPONENT。组件挂在页面下（pageCode），
 * 典型场景为按钮级权限控制，鉴权时通过 component_code 匹配 AuthzResource.resourceCode。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysResComponent {

    /** 主键。 */
    private Long id;

    /** 租户标识。 */
    private String tenantId;

    /** 应用标识。 */
    private String appCode;

    /** 组件编码，唯一约束 uk_usp_component(tenant_id, app_code, component_code)。 */
    private String componentCode;

    /** 组件名称。 */
    private String componentName;

    /** 所属页面编码，关联 SysResPage。 */
    private String pageCode;

    /** 组件类型，默认 BUTTON，可扩展 TAB、SECTION 等。 */
    private String componentType;

    /** 状态：ENABLED / DISABLED。 */
    private String status;

    /** 宿主系统透传的动态扩展属性。 */
    @TableField(exist = false)
    private Map<String, Object> attributes;
}