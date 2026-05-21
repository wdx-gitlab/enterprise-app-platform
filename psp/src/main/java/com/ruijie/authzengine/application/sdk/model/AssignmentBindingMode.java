package com.ruijie.authzengine.application.sdk.model;

/**
 * SDK 授权分配绑定模式。
 */
public enum AssignmentBindingMode {

    /** 直接授权，不绑定策略模板与规则参数。 */
    DIRECT,

    /** 模板授权，绑定既有策略模板并可附带参数。 */
    TEMPLATE
}