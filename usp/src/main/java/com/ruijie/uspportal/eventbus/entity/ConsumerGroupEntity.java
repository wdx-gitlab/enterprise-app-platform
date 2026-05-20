package com.ruijie.uspportal.eventbus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("usp_consumer_group")
/**
 * ConsumerGroup 实体类。
 */
public class ConsumerGroupEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("group_code")
    private String groupCode;

    @TableField("group_name")
    private String groupName;

    @TableField("delivery_channel")
    private String deliveryChannel;

    private String status;

    private String description;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_time")
    private LocalDateTime updatedTime;
}
