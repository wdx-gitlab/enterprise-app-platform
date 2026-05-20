package com.ruijie.uspportal.eventbus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ruijie.uspportal.common.BusinessException;
import com.ruijie.uspportal.config.EventBusProperties;
import com.ruijie.uspportal.eventbus.entity.EventDeliveryEntity;
import com.ruijie.uspportal.eventbus.entity.OutboxEventEntity;
import com.ruijie.uspportal.eventbus.mapper.EventDeliveryMapper;
import com.ruijie.uspportal.eventbus.mapper.OutboxEventMapper;
import com.ruijie.uspportal.eventbus.model.UspEventPayload;
import com.ruijie.uspportal.eventbus.service.EventChannelDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Outbox 事件派发执行器。
 *
 * <p>负责扫描待派发出站事件、执行加锁派发、回收过期锁以及处理重试与重放流程。</p>
 */
@Component
public class OutboxDispatchExecutor {

    private static final List<String> RETRYABLE_STATUSES = Arrays.asList("PENDING", "FAILED", "RETRYING");

    private final OutboxEventMapper outboxEventMapper;

    private final EventDeliveryMapper eventDeliveryMapper;

    private final List<EventChannelDispatcher> dispatchers;

    private final EventBusProperties eventBusProperties;

    private final PlatformTransactionManager transactionManager;

    @Autowired
    public OutboxDispatchExecutor(OutboxEventMapper outboxEventMapper,
                                  EventDeliveryMapper eventDeliveryMapper,
                                  List<EventChannelDispatcher> dispatchers,
                                  EventBusProperties eventBusProperties,
                                  @Qualifier("uengineTransactionManager") PlatformTransactionManager transactionManager) {
        this.outboxEventMapper = outboxEventMapper;
        this.eventDeliveryMapper = eventDeliveryMapper;
        this.dispatchers = dispatchers;
        this.eventBusProperties = eventBusProperties;
        this.transactionManager = transactionManager;
    }

    /**
     * 批量派发当前待处理的出站事件。
     */
    public void dispatchPendingBatch() {
        reclaimExpiredDispatchingLocks();

        QueryWrapper<OutboxEventEntity> wrapper = new QueryWrapper<>();
        wrapper.in("status", RETRYABLE_STATUSES)
                .le("next_retry_time", LocalDateTime.now())
                .orderByAsc("created_time")
                .last("LIMIT " + Math.max(1, eventBusProperties.getDispatcher().getBatchSize()));
        List<OutboxEventEntity> candidates = outboxEventMapper.selectList(wrapper);
        for (OutboxEventEntity candidate : candidates) {
            dispatchById(candidate.getId());
        }
    }

    /**
     * 按主键派发指定出站事件。
     *
     * @param outboxId 出站事件主键
     */
    public void dispatchById(Long outboxId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            OutboxEventEntity event = outboxEventMapper.selectById(outboxId);
            if (event == null || !RETRYABLE_STATUSES.contains(event.getStatus())) {
                return;
            }
            if (event.getNextRetryTime() != null && event.getNextRetryTime().isAfter(LocalDateTime.now())) {
                return;
            }
            processOutboxEvent(event);
        });
    }

    /**
     * 回收超时未释放的派发锁。
     */
    private void reclaimExpiredDispatchingLocks() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.minusSeconds(Math.max(1L, eventBusProperties.getDispatcher().getLockTimeoutSeconds()));

        QueryWrapper<OutboxEventEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "DISPATCHING")
                .and(nested -> nested.isNull("locked_time").or().le("locked_time", deadline))
                .orderByAsc("locked_time")
                .last("LIMIT " + Math.max(1, eventBusProperties.getDispatcher().getBatchSize()));

        List<OutboxEventEntity> expiredLocks = outboxEventMapper.selectList(wrapper);
        for (OutboxEventEntity expiredLock : expiredLocks) {
            UpdateWrapper<OutboxEventEntity> reclaimWrapper = new UpdateWrapper<>();
            reclaimWrapper.eq("id", expiredLock.getId())
                    .eq("status", "DISPATCHING")
                    .and(nested -> nested.isNull("locked_time").or().le("locked_time", deadline))
                    .set("status", "RETRYING")
                    .set("next_retry_time", now)
                    .set("last_error", trim("Dispatch lock expired and was reclaimed by scheduler", 1000))
                    .set("locked_by", null)
                    .set("locked_time", null)
                    .set("updated_time", now);
            outboxEventMapper.update(null, reclaimWrapper);
        }
    }

    /**
     * 按投递记录重放一次事件。
     *
     * @param deliveryId 投递记录主键
     */
    public void replayByDeliveryId(Long deliveryId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            EventDeliveryEntity delivery = eventDeliveryMapper.selectById(deliveryId);
            if (delivery == null) {
                throw new BusinessException(404, "投递记录不存在");
            }
            UspEventPayload payload = UspEventPayload.builder()
                    .eventId(delivery.getEventId())
                    .topic(delivery.getTopic())
                    .payloadJson(delivery.getPayloadJson())
                    .payloadClass(delivery.getPayloadClass())
                    .publishedTime(delivery.getPublishedTime())
                    .build();
            EventChannelDispatcher dispatcher = resolveDispatcher();
            try {
                dispatcher.dispatch(payload);
                eventDeliveryMapper.insert(buildSuccessDelivery(null, payload, dispatcher, delivery.getRetryCount(), 1));
            } catch (Exception ex) {
                eventDeliveryMapper.insert(buildFailureDelivery(null, payload, dispatcher, delivery.getRetryCount(), 1, ex));
                throw new BusinessException(500, "重放失败: " + ex.getMessage());
            }
        });
    }

    /**
     * 处理单条出站事件的派发流程。
     *
     * @param event 出站事件实体
     */
    private void processOutboxEvent(OutboxEventEntity event) {
        LocalDateTime now = LocalDateTime.now();
        UpdateWrapper<OutboxEventEntity> lockWrapper = new UpdateWrapper<>();
        lockWrapper.eq("id", event.getId())
                .in("status", RETRYABLE_STATUSES)
                .set("status", "DISPATCHING")
                .set("locked_by", eventBusProperties.getDispatcher().getLockOwner())
                .set("locked_time", now)
                .set("updated_time", now);
        if (outboxEventMapper.update(null, lockWrapper) == 0) {
            return;
        }

        EventChannelDispatcher dispatcher = resolveDispatcher();
        UspEventPayload payload = buildPayload(event);
        try {
            dispatcher.dispatch(payload);
            eventDeliveryMapper.insert(buildSuccessDelivery(event, payload, dispatcher, event.getRetryCount(), 0));
            outboxEventMapper.deleteById(event.getId());
        } catch (Exception ex) {
            Integer nextRetryCount = (event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1;
            boolean exhausted = nextRetryCount >= Math.max(1, eventBusProperties.getDispatcher().getMaxRetryCount());
            UpdateWrapper<OutboxEventEntity> failureWrapper = new UpdateWrapper<>();
            failureWrapper.eq("id", event.getId())
                    .set("status", exhausted ? "FAILED" : "RETRYING")
                    .set("retry_count", nextRetryCount)
                    .set("next_retry_time", now.plusSeconds(exhausted ? 0L : Math.min(300L, nextRetryCount * 15L)))
                    .set("last_error", trim(ex.getMessage(), 1000))
                    .set("locked_by", null)
                    .set("locked_time", null)
                    .set("updated_time", now);
            outboxEventMapper.update(null, failureWrapper);
            eventDeliveryMapper.insert(buildFailureDelivery(event, payload, dispatcher, nextRetryCount, 0, ex));
        }
    }

    /**
     * 将出站事件转换为统一事件载荷。
     *
     * @param event 出站事件实体
     * @return 统一事件载荷
     */
    private UspEventPayload buildPayload(OutboxEventEntity event) {
        return UspEventPayload.builder()
                .eventId(event.getEventId())
                .topic(event.getTopic())
                .payloadJson(event.getPayloadJson())
                .payloadClass(event.getPayloadClass())
                .sourceSystem(event.getSourceSystem())
                .sourceModule(event.getSourceModule())
                .businessKey(event.getBusinessKey())
                .publishedTime(event.getPublishedTime())
                .build();
    }

    /**
     * 构造成功投递记录。
     *
     * @param outbox 出站事件实体
     * @param payload 统一事件载荷
     * @param dispatcher 实际派发器
     * @param retryCount 重试次数
     * @param replayed 是否为重放
     * @return 投递记录实体
     */
    private EventDeliveryEntity buildSuccessDelivery(OutboxEventEntity outbox,
                                                     UspEventPayload payload,
                                                     EventChannelDispatcher dispatcher,
                                                     Integer retryCount,
                                                     Integer replayed) {
        EventDeliveryEntity entity = new EventDeliveryEntity();
        entity.setEventId(payload.getEventId());
        entity.setOutboxId(outbox == null ? null : outbox.getId());
        entity.setTopic(payload.getTopic());
        entity.setPayloadJson(payload.getPayloadJson());
        entity.setPayloadClass(payload.getPayloadClass());
        entity.setDeliveryChannel(dispatcher.channelType());
        entity.setConsumerName(dispatcher.consumerName());
        entity.setDeliveryStatus("SUCCESS");
        entity.setRetryCount(retryCount == null ? 0 : retryCount);
        entity.setReplayed(replayed == null ? 0 : replayed);
        entity.setPublishedTime(payload.getPublishedTime() == null ? LocalDateTime.now() : payload.getPublishedTime());
        entity.setDeliveredTime(LocalDateTime.now());
        return entity;
    }

    /**
     * 构造失败投递记录。
     *
     * @param outbox 出站事件实体
     * @param payload 统一事件载荷
     * @param dispatcher 实际派发器
     * @param retryCount 重试次数
     * @param replayed 是否为重放
     * @param ex 派发异常
     * @return 投递记录实体
     */
    private EventDeliveryEntity buildFailureDelivery(OutboxEventEntity outbox,
                                                     UspEventPayload payload,
                                                     EventChannelDispatcher dispatcher,
                                                     Integer retryCount,
                                                     Integer replayed,
                                                     Exception ex) {
        EventDeliveryEntity entity = buildSuccessDelivery(outbox, payload, dispatcher, retryCount, replayed);
        entity.setDeliveryStatus("FAILED");
        entity.setErrorMessage(trim(ex.getMessage(), 1000));
        return entity;
    }

    /**
     * 解析当前可用的事件派发器。
     *
     * @return 与当前事件总线类型匹配的派发器
     */
    private EventChannelDispatcher resolveDispatcher() {
        Map<String, EventChannelDispatcher> dispatcherMap = dispatchers.stream()
                .collect(Collectors.toMap(dispatcher -> dispatcher.channelType().toUpperCase(), Function.identity(), (left, right) -> left));
        EventChannelDispatcher dispatcher = dispatcherMap.get(eventBusProperties.normalizedType());
        if (dispatcher != null) {
            return dispatcher;
        }
        dispatcher = dispatcherMap.get("LOCAL");
        if (dispatcher != null) {
            return dispatcher;
        }
        throw new BusinessException(500, "未找到可用的事件派发器: " + eventBusProperties.normalizedType());
    }

    /**
     * 截断过长的错误消息。
     *
     * @param value 原始字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串
     */
    private String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
