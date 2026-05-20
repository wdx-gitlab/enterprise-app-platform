package com.ruijie.uspportal.eventbus.service;

/**
 * 门户事件发布器。
 *
 * <p>定义统一事件发布入口，供各业务模块向事件中心提交领域事件。</p>
 */
public interface PortalEventPublisher {

    /**
     * 发布一条统一事件。
     *
     * @param topic 事件主题
     * @param payload 事件载荷对象
     */
    void publish(String topic, Object payload);
}
