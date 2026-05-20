package com.ruijie.uspportal.eventbus.service;

import com.ruijie.uspportal.eventbus.model.UspEventPayload;

/**
 * 门户事件订阅器。
 *
 * <p>定义统一事件订阅规范，包括监听主题、载荷类型与消费回调。</p>
 */
public interface PortalEventSubscriber<T> {

    /**
     * 返回当前订阅器监听的事件主题。
     *
     * @return 事件主题
     */
    String topic();

    /**
     * 返回当前订阅器支持的载荷类型。
     *
     * @return 载荷类型
     */
    Class<T> payloadType();

    /**
     * 处理订阅到的统一事件。
     *
     * @param payload 反序列化后的载荷对象
     * @param event 原始统一事件载荷
     * @throws Exception 消费异常
     */
    void onEvent(T payload, UspEventPayload event) throws Exception;
}
