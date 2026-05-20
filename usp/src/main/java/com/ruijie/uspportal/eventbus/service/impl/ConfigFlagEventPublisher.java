package com.ruijie.uspportal.eventbus.service.impl;

import com.ruijie.uspportal.eventbus.service.PortalEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class ConfigFlagEventPublisher {

    private final PortalEventPublisher portalEventPublisher;

    public ConfigFlagEventPublisher(PortalEventPublisher portalEventPublisher) {
        this.portalEventPublisher = portalEventPublisher;
    }

    public void publishConfigChanged(String topic, Object payload) {
        portalEventPublisher.publish(topic, payload);
    }
}
