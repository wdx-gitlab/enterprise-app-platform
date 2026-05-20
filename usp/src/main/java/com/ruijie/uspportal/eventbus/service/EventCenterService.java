package com.ruijie.uspportal.eventbus.service;

import com.ruijie.uspportal.eventbus.dto.EventCenterSummary;
import com.ruijie.uspportal.eventbus.entity.EventDeliveryEntity;
import com.ruijie.uspportal.eventbus.entity.EventTopicEntity;
import com.ruijie.uspportal.eventbus.entity.OutboxEventEntity;

import java.util.List;

/**
 * 事件中心服务。
 *
 * <p>定义事件中心统计、主题查询、出站事件管理、投递记录查询以及重试重放能力。</p>
 */
public interface EventCenterService {

    /**
     * 查询事件中心概览。
     *
     * @return 事件中心汇总结果
     */
    EventCenterSummary summary();

    /**
     * 查询事件主题列表。
     *
     * @return 事件主题集合
     */
    List<EventTopicEntity> listTopics();

    /**
     * 查询出站事件列表。
     *
     * @return 出站事件集合
     */
    List<OutboxEventEntity> listOutboxEvents();

    /**
     * 查询投递记录列表。
     *
     * @return 投递记录集合
     */
    List<EventDeliveryEntity> listDeliveries();

    /**
     * 重试指定出站事件。
     *
     * @param id 出站事件主键
     */
    void retryOutbox(Long id);

    /**
     * 重放指定投递记录。
     *
     * @param id 投递记录主键
     */
    void replayDelivery(Long id);

    /**
     * 派发当前待处理的出站事件。
     */
    void dispatchPendingEvents();
}
