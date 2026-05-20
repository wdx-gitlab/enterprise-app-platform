-- Consolidated USP Portal MVP schema.
-- This file is the clean USP Portal baseline obtained by merging repeated
-- portal-side development iterations, including the former login-config repair.
-- The database is expected to be rebuilt from this initialization chain.

CREATE TABLE IF NOT EXISTS `usp_tenant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_code` VARCHAR(64) NOT NULL COMMENT '租户编码',
  `tenant_name` VARCHAR(128) NOT NULL COMMENT '租户名称',
  `tenant_type` VARCHAR(32) NOT NULL COMMENT '租户类型：事业部/分子公司/项目域',
  `parent_org_code` VARCHAR(64) DEFAULT NULL COMMENT '上级组织编码',
  `capability_scope` JSON DEFAULT NULL COMMENT '允许接入能力范围 JSON',
  `status` VARCHAR(32) NOT NULL COMMENT 'DRAFT/ACTIVE/SUSPENDED/INACTIVE',
  `activated_time` DATETIME DEFAULT NULL COMMENT '激活时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='USP-租户表';

CREATE TABLE IF NOT EXISTS `usp_login_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `internal_login_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用内置账号密码登录',
  `sso_login_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用 SSO 登录',
  `default_login_mode` VARCHAR(32) NOT NULL DEFAULT 'INTERNAL' COMMENT '默认登录方式：INTERNAL/SSO',
  `password_policy_json` JSON DEFAULT NULL COMMENT '密码策略 JSON',
  `sso_type` VARCHAR(32) DEFAULT NULL COMMENT 'SSO 类型：PORTAL_SID/CS05/SID 等',
  `sso_protocol_type` VARCHAR(32) DEFAULT NULL COMMENT 'SSO 协议类型：JWT/SAML/OIDC/SESSION',
  `sso_certification_name` VARCHAR(128) DEFAULT NULL COMMENT 'SSO 认证标识名称，如请求头名称',
  `sso_validate_url` VARCHAR(255) DEFAULT NULL COMMENT 'SSO 票据或会话校验地址',
  `sso_metadata_url` VARCHAR(255) DEFAULT NULL COMMENT 'SSO 元数据地址',
  `sso_button_text` VARCHAR(64) DEFAULT NULL COMMENT '登录页 SSO 按钮或 Tab 文案',
  `account_mapping_rule` TEXT DEFAULT NULL COMMENT '账号映射规则 JSON 文本',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='USP-登录配置表';

CREATE TABLE IF NOT EXISTS `usp_local_account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `login_name` VARCHAR(64) NOT NULL COMMENT '登录账号',
  `display_name` VARCHAR(128) NOT NULL COMMENT '显示名称',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
  `password_encode_type` VARCHAR(32) NOT NULL DEFAULT 'SHA256_UPPER' COMMENT '密码编码规则：SHA256_UPPER/BCRYPT 等',
  `password_salt` VARCHAR(128) DEFAULT NULL COMMENT '密码盐，可为空',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `phone_num` VARCHAR(32) DEFAULT NULL COMMENT '手机号',
  `tenant_code` VARCHAR(64) NOT NULL DEFAULT '_PLATFORM_' COMMENT '所属租户，默认平台租户',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '账号状态：ENABLED/DISABLED/LOCKED',
  `is_admin` TINYINT NOT NULL DEFAULT 0 COMMENT '是否平台管理员',
  `force_reset_password` TINYINT NOT NULL DEFAULT 0 COMMENT '是否首次登录强制改密',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_local_account_login_name` (`login_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='USP-本地账号表';

CREATE TABLE IF NOT EXISTS `usp_login_session` (
  `id` VARCHAR(64) NOT NULL COMMENT '会话 ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户 ID',
  `tenant_code` VARCHAR(64) NOT NULL COMMENT '租户编码',
  `org_code` VARCHAR(64) DEFAULT NULL COMMENT '组织编码',
  `auth_mode` VARCHAR(32) NOT NULL COMMENT 'INTERNAL/CS05/SID/USP',
  `login_ip` VARCHAR(64) DEFAULT NULL COMMENT '登录 IP',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '用户代理',
  `status` VARCHAR(32) NOT NULL COMMENT 'ACTIVE/LOGOUT/EXPIRED',
  `last_active_time` DATETIME DEFAULT NULL COMMENT '最后活跃时间',
  `expire_time` DATETIME NOT NULL COMMENT '过期时间',
  `logout_time` DATETIME DEFAULT NULL COMMENT '注销时间',
  `logout_reason` VARCHAR(128) DEFAULT NULL COMMENT '注销原因',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='USP-登录会话表';

CREATE TABLE IF NOT EXISTS `usp_access_token` (
  `id` VARCHAR(64) NOT NULL COMMENT '主键',
  `session_id` VARCHAR(64) NOT NULL COMMENT '关联会话 ID',
  `token_type` VARCHAR(32) NOT NULL COMMENT 'ACCESS/REFRESH',
  `token_hash` VARCHAR(128) NOT NULL COMMENT 'Token 哈希',
  `subject_id` VARCHAR(64) NOT NULL COMMENT '主体用户 ID',
  `user_info_json` JSON DEFAULT NULL COMMENT '令牌用户信息快照 JSON',
  `audience` VARCHAR(128) DEFAULT NULL COMMENT 'aud',
  `issuer` VARCHAR(128) DEFAULT NULL COMMENT 'iss',
  `scope` VARCHAR(500) DEFAULT NULL COMMENT 'scope',
  `issued_time` DATETIME NOT NULL COMMENT '签发时间',
  `expire_time` DATETIME NOT NULL COMMENT '过期时间',
  `revoked` TINYINT NOT NULL DEFAULT 0 COMMENT '是否吊销',
  `revoked_time` DATETIME DEFAULT NULL COMMENT '吊销时间',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_access_token_hash` (`token_hash`),
  KEY `idx_access_token_subject_id` (`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='USP-访问令牌表';

CREATE TABLE IF NOT EXISTS `usp_app_registry` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `app_code` VARCHAR(64) NOT NULL COMMENT '应用编码',
  `app_name` VARCHAR(128) NOT NULL COMMENT '应用名称',
  `app_icon` VARCHAR(255) DEFAULT NULL COMMENT '图标地址',
  `app_desc` VARCHAR(500) DEFAULT NULL COMMENT '应用描述',
  `entry_url` VARCHAR(255) NOT NULL COMMENT '入口 URL',
  `static_base_url` VARCHAR(255) DEFAULT NULL COMMENT '静态资源基路径',
  `app_type` VARCHAR(32) NOT NULL COMMENT 'INTERNAL/INTERNAL_PAGE/IFRAME/EXTERNAL',
  `route_prefix` VARCHAR(128) DEFAULT NULL COMMENT '路由前缀',
  `context_protocol` VARCHAR(32) NOT NULL DEFAULT 'JWT' COMMENT 'JWT/POST_MESSAGE/NONE',
  `permission_mode` VARCHAR(32) DEFAULT NULL COMMENT '权限继承模式',
  `publish_status` VARCHAR(32) NOT NULL COMMENT 'DRAFT/PUBLISHED/OFFLINE',
  `visible_scope` TEXT DEFAULT NULL COMMENT '可见范围 JSON 文本',
  `published_time` DATETIME DEFAULT NULL COMMENT '发布时间',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_registry_code` (`app_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='USP-应用注册表';

CREATE TABLE IF NOT EXISTS `usp_menu_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` VARCHAR(64) NOT NULL COMMENT '租户主键快照',
  `tenant_code` VARCHAR(64) NOT NULL COMMENT '租户编码',
  `app_code` VARCHAR(64) NOT NULL COMMENT '应用编码',
  `app_id` BIGINT DEFAULT NULL COMMENT '所属应用 ID',
  `menu_code` VARCHAR(64) NOT NULL COMMENT '菜单编码',
  `menu_name` VARCHAR(128) NOT NULL COMMENT '菜单名称',
  `menu_icon` VARCHAR(255) DEFAULT NULL COMMENT '图标',
  `menu_type` VARCHAR(32) NOT NULL DEFAULT 'DIRECTORY' COMMENT 'DIRECTORY/MENU/LINK',
  `route_path` VARCHAR(255) DEFAULT NULL COMMENT '路由路径',
  `target_url` VARCHAR(255) DEFAULT NULL COMMENT '跳转链接',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父节点 ID',
  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `tree_level` INT NOT NULL DEFAULT 1 COMMENT '层级',
  `tree_path` VARCHAR(500) DEFAULT NULL COMMENT '树路径',
  `permission_code` VARCHAR(128) DEFAULT NULL COMMENT 'PSP 权限编码',
  `visible_expression` VARCHAR(500) DEFAULT NULL COMMENT '显示表达式',
  `publish_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_menu_item_tenant_app_code` (`tenant_code`, `app_code`, `menu_code`),
  KEY `idx_menu_item_parent_sort` (`parent_id`, `sort_no`),
  KEY `idx_menu_item_app_id` (`app_id`),
  KEY `idx_menu_item_tenant_publish` (`tenant_code`, `publish_status`),
  KEY `idx_menu_item_permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='USP-菜单项表';

CREATE TABLE IF NOT EXISTS `usp_workbench` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `workbench_code` VARCHAR(64) NOT NULL COMMENT '工作台编码',
  `workbench_name` VARCHAR(128) NOT NULL COMMENT '工作台名称',
  `workbench_type` VARCHAR(32) NOT NULL COMMENT 'PERSONAL/TEAM',
  `owner_user_id` VARCHAR(64) DEFAULT NULL COMMENT '归属用户 ID',
  `owner_org_code` VARCHAR(64) DEFAULT NULL COMMENT '归属组织编码',
  `layout_template` VARCHAR(64) DEFAULT NULL COMMENT '布局模板编码',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  `tenant_code` VARCHAR(64) NOT NULL COMMENT '租户编码',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workbench_code` (`workbench_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='USP-工作台表';

CREATE TABLE IF NOT EXISTS `usp_workbench_widget` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `workbench_id` BIGINT NOT NULL COMMENT '工作台 ID',
  `widget_code` VARCHAR(64) NOT NULL COMMENT '组件编码',
  `widget_name` VARCHAR(128) NOT NULL COMMENT '组件名称',
  `widget_type` VARCHAR(32) NOT NULL COMMENT 'WEB_COMPONENT/IFRAME',
  `source_app_id` BIGINT DEFAULT NULL COMMENT '来源应用 ID',
  `component_version` VARCHAR(32) DEFAULT NULL COMMENT '组件版本',
  `props_json` JSON DEFAULT NULL COMMENT '组件参数 JSON',
  `refresh_policy` VARCHAR(64) DEFAULT NULL COMMENT '刷新策略',
  `row_no` INT NOT NULL DEFAULT 1 COMMENT '行位置',
  `col_no` INT NOT NULL DEFAULT 1 COMMENT '列位置',
  `width` INT NOT NULL DEFAULT 1 COMMENT '宽度占位',
  `height` INT NOT NULL DEFAULT 1 COMMENT '高度占位',
  `permission_code` VARCHAR(128) DEFAULT NULL COMMENT '权限编码',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='USP-工作台组件表';

CREATE TABLE IF NOT EXISTS `usp_portal_param` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `param_key` VARCHAR(128) NOT NULL COMMENT '参数键',
  `param_name` VARCHAR(128) NOT NULL COMMENT '参数名称',
  `param_group` VARCHAR(64) NOT NULL COMMENT '参数分组',
  `value_type` VARCHAR(32) NOT NULL COMMENT '值类型',
  `param_value` TEXT DEFAULT NULL COMMENT '当前参数值',
  `default_value` TEXT DEFAULT NULL COMMENT '默认值',
  `editable_scope` VARCHAR(64) DEFAULT NULL COMMENT '允许编辑范围',
  `encrypted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否加密',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '参数说明',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_portal_param_key` (`param_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='USP-门户参数表';

CREATE TABLE IF NOT EXISTS `usp_portal_param_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `param_id` BIGINT NOT NULL COMMENT '参数 ID',
  `old_value` TEXT DEFAULT NULL COMMENT '旧值',
  `new_value` TEXT DEFAULT NULL COMMENT '新值',
  `changed_by` VARCHAR(64) DEFAULT NULL COMMENT '变更人',
  `changed_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '变更说明',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='USP-门户参数历史表';

CREATE TABLE IF NOT EXISTS `usp_feature_flag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `flag_key` VARCHAR(128) NOT NULL COMMENT '功能开关键',
  `flag_name` VARCHAR(128) NOT NULL COMMENT '功能开关名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '说明',
  `status` VARCHAR(32) NOT NULL DEFAULT 'DISABLED' COMMENT 'ENABLED/DISABLED',
  `created_version` VARCHAR(32) DEFAULT NULL COMMENT '引入版本',
  `deprecated_version` VARCHAR(32) DEFAULT NULL COMMENT '废弃版本',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_feature_flag_key` (`flag_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='USP-功能开关表';

CREATE TABLE IF NOT EXISTS `usp_feature_flag_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `flag_id` BIGINT NOT NULL COMMENT '功能开关 ID',
  `rule_type` VARCHAR(32) NOT NULL COMMENT '规则类型：TENANTCODE/ORGCODE/USERID/APPCODE',
  `rule_operator` VARCHAR(32) NOT NULL COMMENT '规则操作符：EQ/IN',
  `rule_value` TEXT NOT NULL COMMENT '规则匹配值',
  `priority_no` INT NOT NULL DEFAULT 0 COMMENT '优先级，值越小越先匹配',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='USP-功能开关规则表';

CREATE TABLE IF NOT EXISTS `usp_event_topic` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `topic_code` VARCHAR(128) NOT NULL COMMENT '事件主题编码',
  `topic_name` VARCHAR(128) NOT NULL COMMENT '事件主题名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '主题说明',
  `publisher_type` VARCHAR(32) NOT NULL DEFAULT 'PORTAL' COMMENT '发布方类型：PORTAL/HOST',
  `payload_schema_json` TEXT DEFAULT NULL COMMENT '事件载荷结构 JSON',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_topic_code` (`topic_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='USP-事件主题表';

CREATE TABLE IF NOT EXISTS `usp_outbox_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `event_id` VARCHAR(64) NOT NULL COMMENT '事件 ID',
  `topic` VARCHAR(128) NOT NULL COMMENT '事件主题',
  `payload_json` LONGTEXT NOT NULL COMMENT '事件载荷 JSON',
  `payload_class` VARCHAR(255) DEFAULT NULL COMMENT '载荷类型全名',
  `source_system` VARCHAR(64) NOT NULL DEFAULT 'USP_PORTAL' COMMENT '来源系统',
  `source_module` VARCHAR(64) DEFAULT NULL COMMENT '来源模块',
  `business_key` VARCHAR(128) DEFAULT NULL COMMENT '业务主键',
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RETRYING/DISPATCHING/FAILED',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  `next_retry_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次重试时间',
  `last_error` VARCHAR(1000) DEFAULT NULL COMMENT '最近一次错误信息',
  `locked_by` VARCHAR(128) DEFAULT NULL COMMENT '锁持有者',
  `locked_time` DATETIME DEFAULT NULL COMMENT '加锁时间',
  `published_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_outbox_event_id` (`event_id`),
  KEY `idx_outbox_status_retry` (`status`, `next_retry_time`),
  KEY `idx_outbox_topic` (`topic`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='USP-Outbox 事件表';

CREATE TABLE IF NOT EXISTS `usp_event_delivery` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `event_id` VARCHAR(64) NOT NULL COMMENT '事件 ID',
  `outbox_id` BIGINT DEFAULT NULL COMMENT '关联出站事件 ID',
  `topic` VARCHAR(128) NOT NULL COMMENT '事件主题',
  `payload_json` LONGTEXT NOT NULL COMMENT '事件载荷 JSON',
  `payload_class` VARCHAR(255) DEFAULT NULL COMMENT '载荷类型全名',
  `delivery_channel` VARCHAR(32) NOT NULL DEFAULT 'LOCAL' COMMENT '投递通道：LOCAL/RABBITMQ',
  `consumer_name` VARCHAR(128) NOT NULL COMMENT '消费者名称',
  `delivery_status` VARCHAR(32) NOT NULL COMMENT 'SUCCESS/FAILED',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  `replayed` TINYINT NOT NULL DEFAULT 0 COMMENT '是否为重放事件',
  `error_message` VARCHAR(1000) DEFAULT NULL COMMENT '失败错误信息',
  `published_time` DATETIME NOT NULL COMMENT '发布时间',
  `delivered_time` DATETIME DEFAULT NULL COMMENT '投递时间',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_event_delivery_event_id` (`event_id`),
  KEY `idx_event_delivery_topic_status` (`topic`, `delivery_status`),
  KEY `idx_event_delivery_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='USP-事件投递记录表';

CREATE TABLE IF NOT EXISTS `usp_consumer_group` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_code` VARCHAR(128) NOT NULL COMMENT '消费组编码',
  `group_name` VARCHAR(128) NOT NULL COMMENT '消费组名称',
  `delivery_channel` VARCHAR(32) NOT NULL DEFAULT 'LOCAL' COMMENT '投递通道：LOCAL/RABBITMQ',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '说明',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_consumer_group_code` (`group_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='USP-消费组表';

INSERT INTO `usp_tenant` (`id`, `tenant_code`, `tenant_name`, `tenant_type`, `status`, `activated_time`, `remark`, `created_by`)
VALUES (1, '_PLATFORM_', '平台管理租户', 'PLATFORM', 'ACTIVE', NOW(), 'portal 默认平台租户', 'SYSTEM')
ON DUPLICATE KEY UPDATE `tenant_name` = VALUES(`tenant_name`), `status` = VALUES(`status`), `updated_by` = 'SYSTEM';

INSERT INTO `usp_login_config` (`id`, `internal_login_enabled`, `sso_login_enabled`, `default_login_mode`, `password_policy_json`, `sso_type`, `sso_protocol_type`, `sso_certification_name`, `sso_button_text`, `account_mapping_rule`, `status`, `updated_by`)
VALUES (1, 1, 1, 'INTERNAL', JSON_OBJECT('minLength', 8, 'maxRetry', 5), 'PORTAL_SID', 'JWT', 'SIDLOGINJWT', 'SSO 单点登录', JSON_OBJECT('matchMode', 'BY_LOGIN_NAME'), 'ENABLED', 'SYSTEM')
ON DUPLICATE KEY UPDATE `internal_login_enabled` = VALUES(`internal_login_enabled`), `sso_login_enabled` = VALUES(`sso_login_enabled`), `default_login_mode` = VALUES(`default_login_mode`), `sso_button_text` = VALUES(`sso_button_text`), `status` = VALUES(`status`), `updated_by` = 'SYSTEM';

INSERT INTO `usp_local_account` (`id`, `login_name`, `display_name`, `password_hash`, `password_encode_type`, `tenant_code`, `status`, `is_admin`, `force_reset_password`, `created_by`)
VALUES (1, 'admin', '平台管理员', UPPER(SHA2('Admin@123456', 256)), 'SHA256_UPPER', '_PLATFORM_', 'ENABLED', 1, 0, 'SYSTEM')
ON DUPLICATE KEY UPDATE `display_name` = VALUES(`display_name`), `password_hash` = VALUES(`password_hash`), `status` = VALUES(`status`), `updated_by` = 'SYSTEM';

INSERT INTO `usp_app_registry` (`app_code`, `app_name`, `app_icon`, `app_desc`, `entry_url`, `app_type`, `route_prefix`, `context_protocol`, `publish_status`, `published_time`, `created_by`)
VALUES ('demo-app-01', '演示应用一号', 'ri:apps-2-line', '用于宿主联调与最小初始化验证的默认演示应用', '/crm-dashboard', 'INTERNAL_PAGE', '/crm-dashboard', 'JWT', 'PUBLISHED', NOW(), 'SYSTEM')
ON DUPLICATE KEY UPDATE `app_name` = VALUES(`app_name`), `entry_url` = VALUES(`entry_url`), `route_prefix` = VALUES(`route_prefix`), `publish_status` = VALUES(`publish_status`), `published_time` = VALUES(`published_time`), `updated_by` = 'SYSTEM';

INSERT INTO `usp_menu_item` (`tenant_id`, `tenant_code`, `app_code`, `app_id`, `menu_code`, `menu_name`, `menu_icon`, `menu_type`, `route_path`, `parent_id`, `sort_no`, `tree_level`, `tree_path`, `publish_status`, `status`, `created_by`)
VALUES (
  CAST((SELECT `id` FROM `usp_tenant` WHERE `tenant_code` = '_PLATFORM_' LIMIT 1) AS CHAR),
  '_PLATFORM_',
  'demo-app-01',
  (SELECT `id` FROM `usp_app_registry` WHERE `app_code` = 'demo-app-01' LIMIT 1),
  'demo-menu-root',
  '演示导航目录',
  'ri:stack-line',
  'MENU',
  '/crm-dashboard',
  NULL,
  10,
  1,
  '/demo-menu-root',
  'PUBLISHED',
  'ENABLED',
  'SYSTEM'
)
ON DUPLICATE KEY UPDATE `menu_name` = VALUES(`menu_name`), `route_path` = VALUES(`route_path`), `publish_status` = VALUES(`publish_status`), `status` = VALUES(`status`), `app_id` = (SELECT `id` FROM `usp_app_registry` WHERE `app_code` = 'demo-app-01' LIMIT 1), `tenant_id` = CAST((SELECT `id` FROM `usp_tenant` WHERE `tenant_code` = '_PLATFORM_' LIMIT 1) AS CHAR), `updated_by` = 'SYSTEM';

INSERT INTO `usp_portal_param` (`param_key`, `param_name`, `param_group`, `value_type`, `param_value`, `default_value`, `status`, `description`)
VALUES ('login.defaultMode', '默认登录方式', 'login', 'STRING', 'INTERNAL', 'INTERNAL', 'ENABLED', '登录页默认方式')
ON DUPLICATE KEY UPDATE `param_value` = VALUES(`param_value`), `description` = VALUES(`description`);

INSERT INTO `usp_portal_param` (`param_key`, `param_name`, `param_group`, `value_type`, `param_value`, `default_value`, `status`, `description`)
VALUES ('portal.defaultHome', '默认首页', 'portal.runtime', 'STRING', '/usp-overview', '/usp-overview', 'ENABLED', '门户默认首页路由')
ON DUPLICATE KEY UPDATE `param_value` = VALUES(`param_value`), `default_value` = VALUES(`default_value`), `description` = VALUES(`description`);

INSERT INTO `usp_feature_flag` (`flag_key`, `flag_name`, `description`, `status`)
VALUES ('portal.workbench', '工作台开关', '控制工作台页面是否可用', 'ENABLED')
ON DUPLICATE KEY UPDATE `flag_name` = VALUES(`flag_name`), `status` = VALUES(`status`);

INSERT INTO `usp_event_topic` (`topic_code`, `topic_name`, `description`, `publisher_type`, `payload_schema_json`, `status`)
VALUES
('tenant.CREATED', '租户创建事件', '租户创建后触发，默认本地总线消费', 'PORTAL', JSON_OBJECT('tenantCode', 'string', 'tenantName', 'string', 'status', 'string'), 'ENABLED'),
('tenant.ACTIVE', '租户启用事件', '租户恢复或启用后触发', 'PORTAL', JSON_OBJECT('tenantCode', 'string', 'status', 'string'), 'ENABLED'),
('tenant.SUSPENDED', '租户暂停事件', '租户暂停后触发', 'PORTAL', JSON_OBJECT('tenantCode', 'string', 'status', 'string'), 'ENABLED'),
('config.PARAM_UPDATED', '门户参数变更事件', '门户参数变更后触发', 'PORTAL', JSON_OBJECT('paramKey', 'string', 'paramValue', 'string'), 'ENABLED'),
('config.FLAG_UPDATED', '功能开关变更事件', '功能开关变更后触发', 'PORTAL', JSON_OBJECT('flagKey', 'string', 'status', 'string'), 'ENABLED'),
('demo.crm.customer.CREATED', 'CRM 演示客户创建事件', '由 demo 项目接口触发，验证宿主接入事件中心能力', 'HOST', JSON_OBJECT('customerCode', 'string', 'customerName', 'string', 'createdBy', 'string'), 'ENABLED')
ON DUPLICATE KEY UPDATE `topic_name` = VALUES(`topic_name`), `description` = VALUES(`description`), `publisher_type` = VALUES(`publisher_type`), `payload_schema_json` = VALUES(`payload_schema_json`), `status` = VALUES(`status`);

INSERT INTO `usp_consumer_group` (`group_code`, `group_name`, `delivery_channel`, `status`, `description`)
VALUES ('usp.portal.local.default', 'USP Portal 本地默认消费组', 'LOCAL', 'ENABLED', '单体部署默认本地消费组，后续可平滑切换至 MQ 消费组')
ON DUPLICATE KEY UPDATE `group_name` = VALUES(`group_name`), `delivery_channel` = VALUES(`delivery_channel`), `status` = VALUES(`status`), `description` = VALUES(`description`);
