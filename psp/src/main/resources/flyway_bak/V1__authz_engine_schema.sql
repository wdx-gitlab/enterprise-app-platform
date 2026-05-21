-- authz-engine 权限引擎完整表结构（最终态）
-- 包含所有系统表、资源表、主体表、审计表及索引

-- ============================================================
-- 元模型表
-- ============================================================
CREATE TABLE IF NOT EXISTS authz_meta_model (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    app_code VARCHAR(50) NOT NULL,
    model_code VARCHAR(50) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    category VARCHAR(20) NOT NULL,
    adapter_type VARCHAR(20) NOT NULL DEFAULT 'JAVA_BEAN',
    resolver VARCHAR(255) NOT NULL DEFAULT 'noopHook',
    schema_view TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0,
    CONSTRAINT uk_authz_meta_model UNIQUE (tenant_id, app_code, model_code)
);

-- ============================================================
-- 权限项表
-- ============================================================
CREATE TABLE IF NOT EXISTS authz_permission_item (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    app_code VARCHAR(50) NOT NULL,
    perm_code VARCHAR(100) NOT NULL,
    res_model_code VARCHAR(50) NOT NULL,
    res_id VARCHAR(100) NOT NULL DEFAULT '',
    act_code VARCHAR(50) NOT NULL,
    fail_strategy VARCHAR(10),
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0,
    CONSTRAINT uk_authz_permission_item UNIQUE (tenant_id, app_code, res_model_code, res_id, act_code)
);

-- ============================================================
-- 授权分配表
-- ============================================================
CREATE TABLE IF NOT EXISTS authz_assignment (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    app_code VARCHAR(50) NOT NULL,
    subject_id VARCHAR(100) NOT NULL,
    subject_model VARCHAR(50) NOT NULL,
    perm_item_id BIGINT NOT NULL,
    policy_tpl_id BIGINT,
    policy_params VARCHAR(1000),
    expire_time TIMESTAMP,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
);

-- ============================================================
-- 业务对象元模型表
-- ============================================================
CREATE TABLE IF NOT EXISTS authz_bo_meta_model (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    app_code VARCHAR(50) NOT NULL,
    bo_code VARCHAR(50) NOT NULL,
    bo_name VARCHAR(100) NOT NULL,
    schema_json TEXT,
    adapter_type VARCHAR(20) NOT NULL DEFAULT 'JAVA_BEAN',
    resolver VARCHAR(255) NOT NULL DEFAULT 'noopHook',
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0,
    CONSTRAINT uk_authz_bo_meta_model UNIQUE (tenant_id, app_code, bo_code)
);

-- ============================================================
-- 标准动作字典表
-- ============================================================
CREATE TABLE IF NOT EXISTS authz_std_act_dict (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    act_code VARCHAR(50) NOT NULL,
    act_name VARCHAR(100) NOT NULL,
    act_type VARCHAR(20) NOT NULL,
    res_category VARCHAR(50),
    risk_level INT DEFAULT 1,
    act_aliases VARCHAR(500),
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0,
    CONSTRAINT uk_authz_std_act_dict UNIQUE (tenant_id, act_code)
);

-- ============================================================
-- 策略模板表
-- ============================================================
CREATE TABLE IF NOT EXISTS authz_std_pol_template (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    template_code VARCHAR(50) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    pol_type VARCHAR(20) NOT NULL,
    expression_script TEXT NOT NULL,
    param_schema TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0,
    CONSTRAINT uk_authz_std_pol_template UNIQUE (tenant_id, template_code)
);

-- ============================================================
-- 组织表
-- ============================================================
CREATE TABLE IF NOT EXISTS dap_sys_org (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    app_code VARCHAR(50) NOT NULL,
    -- HCM 标识与名称
    department_code VARCHAR(100) NOT NULL,
    department_name VARCHAR(200),
    department_en_name VARCHAR(200),
    -- 层级与类型
    department_level INT,
    department_type_code VARCHAR(50),
    department_type VARCHAR(100),
    department_category VARCHAR(100),
    -- 父组织（引擎 FK + HCM 来源）
    parent_org_id BIGINT,
    parent_department_code VARCHAR(100),
    parent_department_name VARCHAR(200),
    org_path VARCHAR(500),
    -- 负责人
    manage_user_id VARCHAR(100),
    manage_staff_no VARCHAR(100),
    manage_name VARCHAR(200),
    portion_manage_user_id VARCHAR(100),
    portion_manage_staff_no VARCHAR(100),
    portion_manage_name VARCHAR(200),
    -- HCM 其他字段
    is_enable INT DEFAULT 1,
    create_time TIMESTAMP,
    department_hrbp_list TEXT,
    hcm_payload_json TEXT,
    -- 基础列
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0,
    CONSTRAINT uk_dap_sys_org UNIQUE (tenant_id, app_code, department_code)
);

-- ============================================================
-- 主体关系表
-- ============================================================
CREATE TABLE IF NOT EXISTS authz_subject_relation (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    app_code VARCHAR(50) NOT NULL,
    subject_model VARCHAR(50) NOT NULL,
    subject_id VARCHAR(100) NOT NULL,
    related_subject_model VARCHAR(50) NOT NULL,
    related_subject_id VARCHAR(100) NOT NULL,
    relation_type VARCHAR(50) NOT NULL,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0,
    CONSTRAINT uk_authz_subject_relation UNIQUE (
        tenant_id,
        app_code,
        subject_model,
        subject_id,
        related_subject_model,
        related_subject_id,
        relation_type
    )
);

-- ============================================================
-- 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS dap_sys_user (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    app_code VARCHAR(50) NOT NULL,
    org_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    staff_no VARCHAR(100),
    user_id VARCHAR(100),
    staff_company_name VARCHAR(200),
    staff_name VARCHAR(200),
    staff_en_name VARCHAR(200),
    staff_status INT DEFAULT 1,
    card_num VARCHAR(100),
    card_type_code VARCHAR(100),
    card_type VARCHAR(100),
    staff_email VARCHAR(200),
    staff_type_code VARCHAR(100),
    staff_type VARCHAR(100),
    staff_photo VARCHAR(500),
    personal_email VARCHAR(200),
    personal_mobile VARCHAR(50),
    join_date TIMESTAMP,
    join_job_date TIMESTAMP,
    nation_code VARCHAR(100),
    nation VARCHAR(100),
    country_and_area_code VARCHAR(100),
    country_and_area VARCHAR(100),
    work_place_code VARCHAR(100),
    work_place VARCHAR(200),
    office_place_code VARCHAR(100),
    office_place VARCHAR(200),
    birthday TIMESTAMP,
    educational_background_code VARCHAR(100),
    educational_background VARCHAR(100),
    marriage_code VARCHAR(100),
    marriage VARCHAR(100),
    native_place VARCHAR(100),
    gender_code VARCHAR(50),
    gender VARCHAR(50),
    actual_formal_date TIMESTAMP,
    last_work_date TIMESTAMP,
    manage_user_id VARCHAR(100),
    manage_staff_no VARCHAR(100),
    tutor_user_id VARCHAR(100),
    tutor_staff_no VARCHAR(100),
    tutor_staff_name VARCHAR(200),
    post_code VARCHAR(100),
    post_name VARCHAR(200),
    post_type_code VARCHAR(100),
    post_type VARCHAR(100),
    department_code VARCHAR(100),
    department_name VARCHAR(200),
    one_department_code VARCHAR(100),
    two_department_code VARCHAR(100),
    three_department_code VARCHAR(100),
    four_department_code VARCHAR(100),
    five_department_code VARCHAR(100),
    six_department_code VARCHAR(100),
    seven_department_code VARCHAR(100),
    eight_department_code VARCHAR(100),
    nine_department_code VARCHAR(100),
    ten_department_code VARCHAR(100),
    one_department_name VARCHAR(200),
    two_department_name VARCHAR(200),
    three_department_name VARCHAR(200),
    four_department_name VARCHAR(200),
    five_department_name VARCHAR(200),
    six_department_name VARCHAR(200),
    seven_department_name VARCHAR(200),
    eight_department_name VARCHAR(200),
    nine_department_name VARCHAR(200),
    ten_department_name VARCHAR(200),
    organization_type_code VARCHAR(100),
    organization_type VARCHAR(100),
    computer_type_code VARCHAR(100),
    computer_type VARCHAR(100),
    country_code VARCHAR(100),
    permanent_country_and_area VARCHAR(100),
    permanent_country_and_area_code VARCHAR(100),
    vender_company VARCHAR(200),
    employment_type_code VARCHAR(100),
    emp_id BIGINT,
    english_country_and_area VARCHAR(100),
    english_permanent_country_and_area VARCHAR(100),
    pay_subject_code VARCHAR(100),
    pay_subject VARCHAR(200),
    manage_staff_name VARCHAR(200),
    is_dept_manage BOOLEAN DEFAULT FALSE,
    is_dept_portion_manage BOOLEAN DEFAULT FALSE,
    staff_management_jurisdiction VARCHAR(100),
    hcm_payload_json TEXT,
    created_by VARCHAR(100) DEFAULT 'system',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0,
    CONSTRAINT uk_dap_sys_user_staff_no UNIQUE (tenant_id, app_code, staff_no),
    CONSTRAINT uk_dap_sys_user_user_id UNIQUE (tenant_id, app_code, user_id)
);

-- ============================================================
-- 用户组表
-- ============================================================
CREATE TABLE IF NOT EXISTS authz_usergroup (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    app_code VARCHAR(50) NOT NULL,
    group_code VARCHAR(100) NOT NULL,
    group_name VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_by VARCHAR(100) DEFAULT 'system',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0,
    CONSTRAINT uk_authz_usergroup UNIQUE (tenant_id, app_code, group_code)
);

-- ============================================================
-- 岗位表
-- ============================================================
CREATE TABLE IF NOT EXISTS authz_position (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    app_code VARCHAR(50) NOT NULL,
    position_code VARCHAR(100) NOT NULL,
    position_name VARCHAR(200) NOT NULL,
    org_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_by VARCHAR(100) DEFAULT 'system',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0,
    CONSTRAINT uk_authz_position UNIQUE (tenant_id, app_code, position_code)
);

-- ============================================================
-- 角色表
-- ============================================================
CREATE TABLE IF NOT EXISTS authz_role (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    app_code VARCHAR(50) NOT NULL,
    role_code VARCHAR(100) NOT NULL,
    role_name VARCHAR(200) NOT NULL,
    role_scope VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_by VARCHAR(100) DEFAULT 'system',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0,
    CONSTRAINT uk_authz_role UNIQUE (tenant_id, app_code, role_code)
);

-- ============================================================
-- 菜单项表（USP 菜单资源）
-- ============================================================
CREATE TABLE IF NOT EXISTS usp_menu_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    tenant_code VARCHAR(64) NOT NULL,
    app_code VARCHAR(64) NOT NULL,
    app_id BIGINT,
    menu_code VARCHAR(64) NOT NULL,
    menu_name VARCHAR(128) NOT NULL,
    menu_icon VARCHAR(255),
    menu_type VARCHAR(32) NOT NULL DEFAULT 'MENU',
    route_path VARCHAR(255),
    target_url VARCHAR(255),
    parent_id BIGINT,
    sort_no INT NOT NULL DEFAULT 0,
    tree_level INT NOT NULL DEFAULT 1,
    tree_path VARCHAR(500),
    permission_code VARCHAR(128),
    visible_expression VARCHAR(500),
    publish_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    created_by VARCHAR(64) DEFAULT 'system',
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64),
    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT NOT NULL DEFAULT 0,
    CONSTRAINT uk_menu_item_tenant_app_code UNIQUE (tenant_code, app_code, menu_code)
);

-- ============================================================
-- 页面资源表
-- ============================================================
CREATE TABLE IF NOT EXISTS usp_page (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    app_code VARCHAR(50) NOT NULL,
    page_code VARCHAR(100) NOT NULL,
    page_name VARCHAR(200) NOT NULL,
    menu_id BIGINT,
    page_path VARCHAR(500),
    sort_order INT NOT NULL DEFAULT 0 COMMENT '显示排序号，数值越小越靠前',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_by VARCHAR(100) DEFAULT 'system',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0,
    CONSTRAINT uk_usp_page UNIQUE (tenant_id, app_code, page_code)
);

-- ============================================================
-- 组件资源表
-- ============================================================
CREATE TABLE IF NOT EXISTS usp_component (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    app_code VARCHAR(50) NOT NULL,
    component_code VARCHAR(100) NOT NULL,
    component_name VARCHAR(200) NOT NULL,
    page_id BIGINT,
    component_type VARCHAR(50) NOT NULL DEFAULT 'BUTTON',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_by VARCHAR(100) DEFAULT 'system',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0,
    CONSTRAINT uk_usp_component UNIQUE (tenant_id, app_code, component_code)
);

-- ============================================================
-- API 资源表
-- ============================================================
CREATE TABLE IF NOT EXISTS usp_api (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    app_code VARCHAR(50) NOT NULL,
    api_code VARCHAR(100) NOT NULL,
    api_name VARCHAR(200) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    uri_pattern VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_by VARCHAR(100) DEFAULT 'system',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0,
    CONSTRAINT uk_usp_api UNIQUE (tenant_id, app_code, api_code)
);

-- ============================================================
-- 派生权限关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS authz_res_derivation_perm (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    app_code VARCHAR(50) NOT NULL,
    res_type VARCHAR(30) NOT NULL COMMENT '资源类型：RES_UI_PAGE / RES_UI_COMPONENT / RES_API',
    res_id BIGINT NOT NULL COMMENT '资源主键 ID，对应 usp_* 表的主键',
    perm_item_id BIGINT NOT NULL COMMENT '派生来源权限项 ID，对应 authz_permission_item.id',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '同一资源下派生关系的排序号',
    created_by VARCHAR(100) DEFAULT 'system',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0,
    CONSTRAINT uk_authz_res_derivation_perm UNIQUE (tenant_id, app_code, res_type, res_id, perm_item_id)
);

-- ============================================================
-- 授权委派表
-- ============================================================
CREATE TABLE IF NOT EXISTS authz_assignment_delegate (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    app_code VARCHAR(50) NOT NULL,
    grantor_subject_model VARCHAR(50) NOT NULL,
    grantor_subject_id VARCHAR(100) NOT NULL,
    delegate_subject_model VARCHAR(50) NOT NULL,
    delegate_subject_id VARCHAR(100) NOT NULL,
    perm_item_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    reason VARCHAR(500),
    created_by VARCHAR(100) DEFAULT 'system',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
);

-- ============================================================
-- 鉴权审计日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS authz_audit_log (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    app_code VARCHAR(50) NOT NULL,
    request_id VARCHAR(100) NOT NULL,
    subject_model VARCHAR(50) NOT NULL,
    subject_id VARCHAR(100) NOT NULL,
    resource_model VARCHAR(50) NOT NULL,
    res_id VARCHAR(100) NOT NULL,
    action_code VARCHAR(50) NOT NULL,
    decision VARCHAR(20) NOT NULL,
    matched_permission_codes TEXT,
    matched_assignment_ids TEXT,
    matched_delegate_ids TEXT,
    matched_policy_template_codes TEXT,
    failure_reason VARCHAR(500),
    cost_ms BIGINT,
    hook_status VARCHAR(32),
    hook_cost_ms BIGINT,
    attribute_snapshot TEXT,
    created_by VARCHAR(100) DEFAULT 'system',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0,
    CONSTRAINT uk_authz_audit_log UNIQUE (tenant_id, app_code, request_id)
);

-- ============================================================
-- 索引（幂等创建，兼容已有生产数据）
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_dap_sys_org_parent
    ON dap_sys_org (tenant_id, app_code, parent_org_id);

CREATE INDEX IF NOT EXISTS idx_dap_sys_org_parent_dept_code
    ON dap_sys_org (tenant_id, app_code, parent_department_code);

CREATE INDEX IF NOT EXISTS idx_authz_subject_relation_subject
    ON authz_subject_relation (tenant_id, app_code, subject_model, subject_id);

CREATE INDEX IF NOT EXISTS idx_dap_sys_user_org
    ON dap_sys_user (tenant_id, app_code, org_id);

CREATE INDEX IF NOT EXISTS idx_dap_sys_user_dept_code
    ON dap_sys_user (tenant_id, app_code, department_code);

CREATE INDEX IF NOT EXISTS idx_usp_api_uri
    ON usp_api (tenant_id, app_code, uri_pattern);

CREATE INDEX IF NOT EXISTS idx_authz_rdp_res
    ON authz_res_derivation_perm (tenant_id, app_code, res_type, res_id);

CREATE INDEX IF NOT EXISTS idx_authz_rdp_perm
    ON authz_res_derivation_perm (tenant_id, app_code, res_type, perm_item_id);

CREATE INDEX IF NOT EXISTS idx_authz_assignment_delegate_target
    ON authz_assignment_delegate (tenant_id, app_code, delegate_subject_model, delegate_subject_id, status);

CREATE INDEX IF NOT EXISTS idx_authz_audit_subject
    ON authz_audit_log (tenant_id, app_code, subject_id, created_at);

CREATE INDEX IF NOT EXISTS idx_authz_audit_resource
    ON authz_audit_log (tenant_id, app_code, res_id, action_code, created_at);

CREATE INDEX IF NOT EXISTS idx_authz_audit_hook_status
    ON authz_audit_log (tenant_id, app_code, hook_status);
