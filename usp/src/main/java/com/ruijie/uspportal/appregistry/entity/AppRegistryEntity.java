package com.ruijie.uspportal.appregistry.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应用注册实体。
 *
 * <p>映射门户应用注册表，用于保存宿主接入应用的入口、类型与发布状态信息。</p>
 */
@Data
@TableName("usp_app_registry")
public class AppRegistryEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("app_code")
    private String appCode;

    @TableField("app_name")
    private String appName;

    @TableField("app_icon")
    private String appIcon;

    @TableField("app_desc")
    private String appDesc;

    @TableField("entry_url")
    private String entryUrl;

    @TableField("app_type")
    private String appType;

    @TableField("route_prefix")
    private String routePrefix;

    @TableField("publish_status")
    private String publishStatus;

    @TableField("is_deleted")
    private Integer deleted;

    @TableField("published_time")
    private LocalDateTime publishedTime;
}
