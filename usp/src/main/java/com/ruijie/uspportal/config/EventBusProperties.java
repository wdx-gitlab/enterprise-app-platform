package com.ruijie.uspportal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "usp.portal.eventbus")
/**
 * 事件总线配置属性。
 *
 * <p>用于承载事件总线类型、派发调度参数以及 RabbitMQ 连接路由相关配置。</p>
 */
public class EventBusProperties {

    private String type = "local";

    private Dispatcher dispatcher = new Dispatcher();

    private Rabbitmq rabbitmq = new Rabbitmq();

    /**
        * 将配置中的事件总线类型归一化为内部枚举值。
        *
        * @return 归一化后的事件总线类型
     */
    public String normalizedType() {
        if (type == null) {
            return "LOCAL";
        }
        String normalized = type.trim().toUpperCase();
        if (normalized.startsWith("RABBIT")) {
            return "RABBITMQ";
        }
        return "LOCAL";
    }

    @Data
    /**
     * 派发调度配置。
     */
    public static class Dispatcher {
        private Integer batchSize = 20;
        private Long fixedDelayMs = 3000L;
        private Integer maxRetryCount = 6;
        private Long lockTimeoutSeconds = 60L;
        private String lockOwner = "usp-portal";
    }

    @Data
    /**
     * RabbitMQ 路由配置。
     */
    public static class Rabbitmq {
        private String exchange = "usp.portal.event.exchange";
        private String queue = "usp.portal.event.queue";
        private String routingKeyPrefix = "usp.portal.event";
        private String routingKeyPattern = "usp.portal.event.#";
    }
}
