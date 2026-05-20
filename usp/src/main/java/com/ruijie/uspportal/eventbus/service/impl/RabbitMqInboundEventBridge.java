package com.ruijie.uspportal.eventbus.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.uspportal.eventbus.model.UspEventPayload;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "usp.portal.eventbus", name = "type", havingValue = "rabbitmq")
/**
 * RabbitMqInboundEventBridge 类。
 */
public class RabbitMqInboundEventBridge {

    private final ObjectMapper objectMapper;

    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public RabbitMqInboundEventBridge(ObjectMapper objectMapper,
                                      ApplicationEventPublisher applicationEventPublisher) {
        this.objectMapper = objectMapper;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @RabbitListener(queues = "${usp.portal.eventbus.rabbitmq.queue}")
    /**
     * 处理回调或事件。
     */
    public void onMessage(String message) throws Exception {
        UspEventPayload payload = objectMapper.readValue(message, UspEventPayload.class);
        applicationEventPublisher.publishEvent(payload);
    }
}
