package com.ruijie.uspportal.eventbus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("usp_outbox_event")
/**
 * OutboxEvent 实体类。
 */
public class OutboxEventEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("event_id")
    private String eventId;

    private String topic;

    @TableField("payload_json")
    private String payloadJson;

    @TableField("payload_class")
    private String payloadClass;

    @TableField("source_system")
    private String sourceSystem;

    @TableField("source_module")
    private String sourceModule;

    @TableField("business_key")
    private String businessKey;

    private String status;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("next_retry_time")
    private LocalDateTime nextRetryTime;

    @TableField("last_error")
    private String lastError;

    @TableField("locked_by")
    private String lockedBy;

    @TableField("locked_time")
    private LocalDateTime lockedTime;

    @TableField("published_time")
    private LocalDateTime publishedTime;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_time")
    private LocalDateTime updatedTime;
}
