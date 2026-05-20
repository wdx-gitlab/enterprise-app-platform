package com.ruijie.uspportal.eventbus.service;

import com.ruijie.uspportal.eventbus.model.UspEventPayload;

/**
 * 事件通道派发器。
 *
 * <p>抽象统一事件在不同通道上的投递行为，例如本地事件总线或 RabbitMQ。</p>
 */
public interface EventChannelDispatcher {

    /**
     * 返回当前派发器的通道类型。
     *
     * @return 通道类型编码
     */
    String channelType();

    /**
     * 返回当前派发器的消费者名称。
     *
     * @return 消费者名称
     */
    String consumerName();

    /**
     * 派发统一事件载荷。
     *
     * @param payload 统一事件载荷
     * @throws Exception 派发异常
     */
    void dispatch(UspEventPayload payload) throws Exception;
}
