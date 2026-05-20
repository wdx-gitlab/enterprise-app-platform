package com.ruijie.uspportal.eventbus.controller;

import com.ruijie.uspportal.common.ApiResponse;
import com.ruijie.uspportal.eventbus.dto.EventCenterSummary;
import com.ruijie.uspportal.eventbus.entity.EventDeliveryEntity;
import com.ruijie.uspportal.eventbus.entity.EventTopicEntity;
import com.ruijie.uspportal.eventbus.entity.OutboxEventEntity;
import com.ruijie.uspportal.eventbus.service.EventCenterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 事件中心控制器。
 *
 * <p>提供事件主题、出站事件、投递记录以及重试重放操作的查询与运维接口。</p>
 */
@RestController
@RequestMapping("/api/event-center")
public class EventCenterController {

    private final EventCenterService eventCenterService;

    @Autowired
    public EventCenterController(EventCenterService eventCenterService) {
        this.eventCenterService = eventCenterService;
    }

    /**
     * 查询事件中心概览信息。
     *
     * @return 事件中心汇总结果
     */
    @GetMapping("/summary")
    public ApiResponse<EventCenterSummary> summary() {
        return ApiResponse.success(eventCenterService.summary());
    }

    /**
     * 查询事件主题列表。
     *
     * @return 事件主题集合
     */
    @GetMapping("/topics")
    public ApiResponse<List<EventTopicEntity>> topics() {
        return ApiResponse.success(eventCenterService.listTopics());
    }

    /**
     * 查询出站事件列表。
     *
     * @return 出站事件集合
     */
    @GetMapping("/outbox")
    public ApiResponse<List<OutboxEventEntity>> outbox() {
        return ApiResponse.success(eventCenterService.listOutboxEvents());
    }

    /**
     * 重试指定出站事件的投递。
     *
     * @param id 出站事件主键
     * @return 空响应
     */
    @PostMapping("/outbox/{id}/retry")
    public ApiResponse<Void> retry(@PathVariable Long id) {
        eventCenterService.retryOutbox(id);
        return ApiResponse.success("重试成功", null);
    }

    /**
     * 查询事件投递记录列表。
     *
     * @return 投递记录集合
     */
    @GetMapping("/deliveries")
    public ApiResponse<List<EventDeliveryEntity>> deliveries() {
        return ApiResponse.success(eventCenterService.listDeliveries());
    }

    /**
     * 重放指定投递记录。
     *
     * @param id 投递记录主键
     * @return 空响应
     */
    @PostMapping("/deliveries/{id}/replay")
    public ApiResponse<Void> replay(@PathVariable Long id) {
        eventCenterService.replayDelivery(id);
        return ApiResponse.success("重放成功", null);
    }
}
