package com.ruijie.uspportal.eventbus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("usp_event_delivery")
/**
 * EventDelivery 实体类。
 */
public class EventDeliveryEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("event_id")
    private String eventId;

    @TableField("outbox_id")
    private Long outboxId;

    private String topic;

    @TableField("payload_json")
    private String payloadJson;

    @TableField("payload_class")
    private String payloadClass;

    @TableField("delivery_channel")
    private String deliveryChannel;

    @TableField("consumer_name")
    private String consumerName;

    @TableField("delivery_status")
    private String deliveryStatus;

    @TableField("retry_count")
    private Integer retryCount;

    private Integer replayed;

    @TableField("error_message")
    private String errorMessage;

    @TableField("published_time")
    private LocalDateTime publishedTime;

    @TableField("delivered_time")
    private LocalDateTime deliveredTime;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_time")
    private LocalDateTime updatedTime;
}
