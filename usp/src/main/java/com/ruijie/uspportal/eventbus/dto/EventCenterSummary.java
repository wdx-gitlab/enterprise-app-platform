package com.ruijie.uspportal.eventbus.dto;

import lombok.Data;

@Data
/**
 * EventCenterSummary 类。
 */
public class EventCenterSummary {

    private String eventbusType;

    private Integer dispatcherBatchSize;

    private Long dispatcherDelayMs;

    private Long topicCount;

    private Long pendingCount;

    private Long retryingCount;

    private Long failedOutboxCount;

    private Long deliveredCount;

    private Long failedDeliveryCount;
}
