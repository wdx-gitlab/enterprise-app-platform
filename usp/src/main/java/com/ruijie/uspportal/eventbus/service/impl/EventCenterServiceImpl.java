package com.ruijie.uspportal.eventbus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ruijie.uspportal.common.BusinessException;
import com.ruijie.uspportal.config.EventBusProperties;
import com.ruijie.uspportal.eventbus.dto.EventCenterSummary;
import com.ruijie.uspportal.eventbus.entity.EventDeliveryEntity;
import com.ruijie.uspportal.eventbus.entity.EventTopicEntity;
import com.ruijie.uspportal.eventbus.entity.OutboxEventEntity;
import com.ruijie.uspportal.eventbus.mapper.EventDeliveryMapper;
import com.ruijie.uspportal.eventbus.mapper.EventTopicMapper;
import com.ruijie.uspportal.eventbus.mapper.OutboxEventMapper;
import com.ruijie.uspportal.eventbus.service.EventCenterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 事件中心服务实现类。
 *
 * <p>负责汇总事件中心统计信息，并提供出站事件和投递记录的查询、重试、重放能力。</p>
 */
@Service
public class EventCenterServiceImpl implements EventCenterService {

    private final EventTopicMapper eventTopicMapper;

    private final OutboxEventMapper outboxEventMapper;

    private final EventDeliveryMapper eventDeliveryMapper;

    private final OutboxDispatchExecutor outboxDispatchExecutor;

    private final EventBusProperties eventBusProperties;

    @Autowired
    public EventCenterServiceImpl(EventTopicMapper eventTopicMapper,
                                  OutboxEventMapper outboxEventMapper,
                                  EventDeliveryMapper eventDeliveryMapper,
                                  OutboxDispatchExecutor outboxDispatchExecutor,
                                  EventBusProperties eventBusProperties) {
        this.eventTopicMapper = eventTopicMapper;
        this.outboxEventMapper = outboxEventMapper;
        this.eventDeliveryMapper = eventDeliveryMapper;
        this.outboxDispatchExecutor = outboxDispatchExecutor;
        this.eventBusProperties = eventBusProperties;
    }

    /**
     * 查询事件中心概览。
     *
     * @return 事件中心汇总结果
     */
    @Override
    public EventCenterSummary summary() {
        EventCenterSummary summary = new EventCenterSummary();
        summary.setEventbusType(eventBusProperties.normalizedType());
        summary.setDispatcherBatchSize(eventBusProperties.getDispatcher().getBatchSize());
        summary.setDispatcherDelayMs(eventBusProperties.getDispatcher().getFixedDelayMs());
        summary.setTopicCount(countTopics());
        summary.setPendingCount(countOutboxByStatus("PENDING"));
        summary.setRetryingCount(countOutboxByStatus("RETRYING"));
        summary.setFailedOutboxCount(countOutboxByStatus("FAILED"));
        summary.setDeliveredCount(countDeliveriesByStatus("SUCCESS"));
        summary.setFailedDeliveryCount(countDeliveriesByStatus("FAILED"));
        return summary;
    }

    /**
     * 查询事件主题列表。
     *
     * @return 事件主题集合
     */
    @Override
    public List<EventTopicEntity> listTopics() {
        QueryWrapper<EventTopicEntity> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("topic_code");
        return eventTopicMapper.selectList(wrapper);
    }

    /**
     * 查询出站事件列表。
     *
     * @return 出站事件集合
     */
    @Override
    public List<OutboxEventEntity> listOutboxEvents() {
        QueryWrapper<OutboxEventEntity> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("created_time");
        return outboxEventMapper.selectList(wrapper);
    }

    /**
     * 查询投递记录列表。
     *
     * @return 投递记录集合
     */
    @Override
    public List<EventDeliveryEntity> listDeliveries() {
        QueryWrapper<EventDeliveryEntity> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("created_time").last("LIMIT 200");
        return eventDeliveryMapper.selectList(wrapper);
    }

    /**
     * 重试指定出站事件。
     *
     * @param id 出站事件主键
     */
    @Override
    public void retryOutbox(Long id) {
        OutboxEventEntity entity = outboxEventMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "出站事件不存在");
        }
        UpdateWrapper<OutboxEventEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id)
                .set("status", "PENDING")
                .set("next_retry_time", LocalDateTime.now())
                .set("last_error", null)
                .set("locked_by", null)
                .set("locked_time", null)
                .set("updated_time", LocalDateTime.now());
        outboxEventMapper.update(null, wrapper);
        outboxDispatchExecutor.dispatchById(id);
    }

    /**
     * 重放指定投递记录。
     *
     * @param id 投递记录主键
     */
    @Override
    public void replayDelivery(Long id) {
        if (eventDeliveryMapper.selectById(id) == null) {
            throw new BusinessException(404, "投递记录不存在");
        }
        outboxDispatchExecutor.replayByDeliveryId(id);
    }

    /**
     * 派发当前待处理的出站事件批次。
     */
    @Override
    public void dispatchPendingEvents() {
        outboxDispatchExecutor.dispatchPendingBatch();
    }

    /**
     * 统计事件主题数量。
     *
     * @return 事件主题数量
     */
    private Long countTopics() {
        return Long.valueOf(eventTopicMapper.selectCount(new QueryWrapper<EventTopicEntity>()));
    }

    /**
     * 按状态统计出站事件数量。
     *
     * @param status 出站事件状态
     * @return 对应状态的出站事件数量
     */
    private Long countOutboxByStatus(String status) {
        QueryWrapper<OutboxEventEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", status);
        return Long.valueOf(outboxEventMapper.selectCount(wrapper));
    }

    /**
     * 按状态统计投递记录数量。
     *
     * @param status 投递状态
     * @return 对应状态的投递记录数量
     */
    private Long countDeliveriesByStatus(String status) {
        QueryWrapper<EventDeliveryEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("delivery_status", status);
        return Long.valueOf(eventDeliveryMapper.selectCount(wrapper));
    }
}
