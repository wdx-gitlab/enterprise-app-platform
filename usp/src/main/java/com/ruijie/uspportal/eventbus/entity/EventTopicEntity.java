package com.ruijie.uspportal.eventbus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("usp_event_topic")
/**
 * EventTopic 实体类。
 */
public class EventTopicEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("topic_code")
    private String topicCode;

    @TableField("topic_name")
    private String topicName;

    private String description;

    @TableField("publisher_type")
    private String publisherType;

    @TableField("payload_schema_json")
    private String payloadSchemaJson;

    private String status;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_time")
    private LocalDateTime updatedTime;
}
