package com.ruijie.authzengine.shared.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 统一错误码定义。
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    SUCCESS("0", "成功"),
    BAD_REQUEST("AUTHZ-400", "请求参数不合法"),
    BUSINESS_ERROR("AUTHZ-409", "业务处理失败"),
    RESOURCE_NOT_FOUND("AUTHZ-404", "治理对象不存在"),
    CONTROLLED_DELETE_CONFLICT("AUTHZ-409-DELETE", "对象仍被引用，禁止删除"),
    RELATION_RECREATE_REQUIRED("AUTHZ-409-RELATION", "身份字段变更必须删除后重建"),
    COMPAT_ALIAS_DEPRECATED("AUTHZ-299-COMPAT", "兼容入口已废弃，请迁移至新 REST 路由"),
    METHOD_NOT_ALLOWED("AUTHZ-405", "请求方法不支持"),
    READ_ONLY_RESOURCE("AUTHZ-405-READONLY", "只读资源不支持写操作"),
    DELEGATION_REVOKE_ONLY("AUTHZ-405-DELEGATION", "委托记录仅支持撤销，不支持通用编辑"),
    UNAUTHENTICATED("AUTHZ-401", "用户未认证"),
    FORBIDDEN("AUTHZ-403", "无访问权限"),
    SYSTEM_ERROR("AUTHZ-500", "系统内部错误"),
    INTEGRATION_ERROR("AUTHZ-503", "外部依赖暂不可用"),
    ADAPTER_NOT_IMPLEMENTED("AUTHZ-501-HOOK", "宿主适配器未实现该操作");

    private final String code;

    private final String message;
}