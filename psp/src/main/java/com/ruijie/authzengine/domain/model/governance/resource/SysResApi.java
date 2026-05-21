package com.ruijie.authzengine.domain.model.governance.resource;

import com.baomidou.mybatisplus.annotation.TableField;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API 资源目录定义，对应 usp_api 表的一行记录。
 * <p>
 * 资源模型编码为 RES_API。记录接口的 HTTP 方法 + URI 模式，
 * PEP 拦截器可据此自动匹配当前请求应鉴权的 API 资源。
 * 索引 idx_usp_api_uri 支撑按 URI 快速查找。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysResApi {

    /** 主键。 */
    private Long id;

    /** 租户标识。 */
    private String tenantId;

    /** 应用标识。 */
    private String appCode;

    /** API 编码，唯一约束 uk_usp_api(tenant_id, app_code, api_code)。 */
    private String apiCode;

    /** API 名称。 */
    private String apiName;

    /** HTTP 方法，如 GET、POST、PUT、DELETE。 */
    private String httpMethod;

    /** URI 匹配模式，如 /authz-engine/api/v1/authz/check。 */
    private String uriPattern;

    /** 状态：ENABLED / DISABLED。 */
    private String status;

    /** 宿主系统透传的动态扩展属性。 */
    @TableField(exist = false)
    private Map<String, Object> attributes;
}