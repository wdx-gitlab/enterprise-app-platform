package com.ruijie.uspportal.eventbus.service.impl;

import com.ruijie.uspportal.eventbus.service.PortalEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class OutboxEventPublisher implements PortalEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public OutboxEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(String topic, Object payload) {
        applicationEventPublisher.publishEvent(new PortalDomainEvent(topic, payload));
    }

    @lombok.Value
    public static class PortalDomainEvent {
        String topic;
        Object payload;
    }
}
