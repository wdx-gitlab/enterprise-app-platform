package com.ruijie.uspportal.eventbus.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.uspportal.config.EventBusProperties;
import com.ruijie.uspportal.eventbus.model.UspEventPayload;
import com.ruijie.uspportal.eventbus.service.EventChannelDispatcher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ 事件通道派发器。
 *
 * <p>在事件总线类型为 RabbitMQ 时负责将统一事件载荷序列化并投递到目标交换机。</p>
 */
@Service
@ConditionalOnProperty(prefix = "usp.portal.eventbus", name = "type", havingValue = "rabbitmq")
public class RabbitMqEventChannelDispatcher implements EventChannelDispatcher {

    private final RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper;

    private final EventBusProperties eventBusProperties;

    @Autowired
    public RabbitMqEventChannelDispatcher(RabbitTemplate rabbitTemplate,
                                          ObjectMapper objectMapper,
                                          EventBusProperties eventBusProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.eventBusProperties = eventBusProperties;
    }

    /**
     * 返回当前派发器的通道类型。
     *
     * @return 固定为 RABBITMQ
     */
    @Override
    public String channelType() {
        return "RABBITMQ";
    }

    /**
     * 返回当前派发器的消费者名称。
     *
     * @return RabbitMQ 交换机标识
     */
    @Override
    public String consumerName() {
        return "RABBITMQ_EXCHANGE";
    }

    /**
     * 将事件载荷投递到 RabbitMQ。
     *
     * @param payload 统一事件载荷
     */
    @Override
    public void dispatch(UspEventPayload payload) {
        try {
            rabbitTemplate.convertAndSend(
                    eventBusProperties.getRabbitmq().getExchange(),
                    eventBusProperties.getRabbitmq().getRoutingKeyPrefix() + "." + payload.getTopic().replace('.', '_'),
                    objectMapper.writeValueAsString(payload));
        } catch (Exception ex) {
            throw new IllegalStateException("RabbitMQ 事件投递失败: " + ex.getMessage(), ex);
        }
    }
}
