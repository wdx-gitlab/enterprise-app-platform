package com.ruijie.uspportal.eventbus.service.impl;

import com.ruijie.uspportal.eventbus.model.UspEventPayload;
import com.ruijie.uspportal.eventbus.service.EventChannelDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * 本地事件通道派发器。
 *
 * <p>负责通过 Spring 应用事件机制分发统一事件载荷。</p>
 */
@Service
public class LocalEventChannelDispatcher implements EventChannelDispatcher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public LocalEventChannelDispatcher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    /**
     * 返回当前派发器的通道类型。
     *
     * @return 固定为 LOCAL
     */
    public String channelType() {
        return "LOCAL";
    }

    @Override
    /**
     * 返回当前派发器的消费者名称。
     *
     * @return Spring 事件总线名称
     */
    public String consumerName() {
        return "SPRING_EVENT_BUS";
    }

    @Override
    /**
     * 将事件载荷投递到本地 Spring 事件总线。
     *
     * @param payload 统一事件载荷
     */
    public void dispatch(UspEventPayload payload) {
        applicationEventPublisher.publishEvent(payload);
    }
}
