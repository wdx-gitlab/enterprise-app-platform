package com.ruijie.uspportal.eventbus.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.uspportal.eventbus.model.UspEventPayload;
import com.ruijie.uspportal.eventbus.service.PortalEventSubscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 门户事件订阅分发器。
 *
 * <p>负责监听本地事件总线中的统一事件，并按 Topic 路由给匹配的订阅器处理。</p>
 */
@Component
public class PortalEventSubscriberDispatcher {

    private final ObjectMapper objectMapper;

    private final List<PortalEventSubscriber<?>> subscribers;

    @Autowired
    public PortalEventSubscriberDispatcher(ObjectMapper objectMapper,
                                           List<PortalEventSubscriber<?>> subscribers) {
        this.objectMapper = objectMapper;
        this.subscribers = subscribers;
    }

    /**
     * 分发统一事件到匹配的订阅器。
     *
     * @param event 统一事件载荷
     */
    @EventListener
    public void dispatch(UspEventPayload event) {
        List<PortalEventSubscriber<?>> subscriberList = subscribers == null ? Collections.emptyList() : subscribers;
        for (PortalEventSubscriber<?> subscriber : subscriberList) {
            if (!subscriber.topic().equals(event.getTopic())) {
                continue;
            }
            invokeSubscriber(subscriber, event);
        }
    }

    /**
     * 执行单个订阅器的事件消费。
     *
     * @param subscriber 订阅器
     * @param event 统一事件载荷
     * @param <T> 订阅器载荷类型
     */
    private <T> void invokeSubscriber(PortalEventSubscriber<T> subscriber, UspEventPayload event) {
        T payload = event.readPayload(objectMapper, subscriber.payloadType());
        try {
            subscriber.onEvent(payload, event);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("事件消费失败: " + ex.getMessage(), ex);
        }
    }
}
