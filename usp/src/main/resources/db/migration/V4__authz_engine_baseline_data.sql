-- Authz Engine 基线数据。
-- 本文件与 V3__authz_engine_schema.sql 一起构成 Authz 模块的基线链路。
-- 包含标准动作字典、策略模板及元模型 schema_view 初始化数据。

-- ============================================================
-- 全局标准动作字典（含别名）
-- ============================================================
UPDATE authz_std_act_dict
SET act_name = '查看',
    act_type = 'STANDARD',
    res_category = 'ALL',
    risk_level = 1,
    act_aliases = 'QUERY,VIEW',
    updated_by = 'system',
    updated_at = CURRENT_TIMESTAMP,
    is_deleted = 0
WHERE tenant_id = '__GLOBAL__' AND act_code = 'READ';

INSERT INTO authz_std_act_dict (id, tenant_id, act_code, act_name, act_type, res_category, risk_level, act_aliases, created_by, updated_by, is_deleted)
SELECT 201, '__GLOBAL__', 'READ', '查看', 'STANDARD', 'ALL', 1, 'QUERY,VIEW', 'system', 'system', 0
WHERE NOT EXISTS (SELECT 1 FROM authz_std_act_dict WHERE tenant_id = '__GLOBAL__' AND act_code = 'READ')
  AND NOT EXISTS (SELECT 1 FROM authz_std_act_dict WHERE id = 201);

UPDATE authz_std_act_dict
SET act_name = '审批',
    act_type = 'BUSINESS',
    res_category = 'RES_DATA_BO',
    risk_level = 3,
    act_aliases = 'APPROVE_REQUEST,AGREE',
    updated_by = 'system',
    updated_at = CURRENT_TIMESTAMP,
    is_deleted = 0
WHERE tenant_id = '__GLOBAL__' AND act_code = 'APPROVE';

INSERT INTO authz_std_act_dict (id, tenant_id, act_code, act_name, act_type, res_category, risk_level, act_aliases, created_by, updated_by, is_deleted)
SELECT 202, '__GLOBAL__', 'APPROVE', '审批', 'BUSINESS', 'RES_DATA_BO', 3, 'APPROVE_REQUEST,AGREE', 'system', 'system', 0
WHERE NOT EXISTS (SELECT 1 FROM authz_std_act_dict WHERE tenant_id = '__GLOBAL__' AND act_code = 'APPROVE')
  AND NOT EXISTS (SELECT 1 FROM authz_std_act_dict WHERE id = 202);

UPDATE authz_std_act_dict
SET act_name = '删除',
    act_type = 'STANDARD',
    res_category = 'ALL',
    risk_level = 3,
    act_aliases = 'REMOVE,ERASE',
    updated_by = 'system',
    updated_at = CURRENT_TIMESTAMP,
    is_deleted = 0
WHERE tenant_id = '__GLOBAL__' AND act_code = 'DELETE';

INSERT INTO authz_std_act_dict (id, tenant_id, act_code, act_name, act_type, res_category, risk_level, act_aliases, created_by, updated_by, is_deleted)
SELECT 203, '__GLOBAL__', 'DELETE', '删除', 'STANDARD', 'ALL', 3, 'REMOVE,ERASE', 'system', 'system', 0
WHERE NOT EXISTS (SELECT 1 FROM authz_std_act_dict WHERE tenant_id = '__GLOBAL__' AND act_code = 'DELETE')
  AND NOT EXISTS (SELECT 1 FROM authz_std_act_dict WHERE id = 203);

UPDATE authz_std_act_dict
SET act_name = '编辑',
    act_type = 'STANDARD',
    res_category = 'ALL',
    risk_level = 2,
    act_aliases = 'UPDATE,MODIFY',
    updated_by = 'system',
    updated_at = CURRENT_TIMESTAMP,
    is_deleted = 0
WHERE tenant_id = '__GLOBAL__' AND act_code = 'WRITE';

INSERT INTO authz_std_act_dict (id, tenant_id, act_code, act_name, act_type, res_category, risk_level, act_aliases, created_by, updated_by, is_deleted)
SELECT 204, '__GLOBAL__', 'WRITE', '编辑', 'STANDARD', 'ALL', 2, 'UPDATE,MODIFY', 'system', 'system', 0
WHERE NOT EXISTS (SELECT 1 FROM authz_std_act_dict WHERE tenant_id = '__GLOBAL__' AND act_code = 'WRITE')
  AND NOT EXISTS (SELECT 1 FROM authz_std_act_dict WHERE id = 204);

-- ============================================================
-- 全局策略模板（最小可用基线）
-- ============================================================
UPDATE authz_std_pol_template
SET template_name = '允许全部',
    pol_type = 'ALLOW',
    expression_script = 'return true;',
    status = 'ENABLED',
    updated_by = 'system',
    updated_at = CURRENT_TIMESTAMP,
    is_deleted = 0
WHERE tenant_id = '__GLOBAL__' AND template_code = 'ALLOW_ALL';

INSERT INTO authz_std_pol_template (id, tenant_id, template_code, template_name, pol_type, expression_script, param_schema, status, created_by, updated_by, is_deleted)
SELECT 301, '__GLOBAL__', 'ALLOW_ALL', '允许全部', 'ALLOW', 'return true;', '{}', 'ENABLED', 'system', 'system', 0
WHERE NOT EXISTS (SELECT 1 FROM authz_std_pol_template WHERE tenant_id = '__GLOBAL__' AND template_code = 'ALLOW_ALL')
  AND NOT EXISTS (SELECT 1 FROM authz_std_pol_template WHERE id = 301);

UPDATE authz_std_pol_template
SET template_name = '拒绝全部',
    pol_type = 'DENY',
    expression_script = 'return false;',
    status = 'ENABLED',
    updated_by = 'system',
    updated_at = CURRENT_TIMESTAMP,
    is_deleted = 0
WHERE tenant_id = '__GLOBAL__' AND template_code = 'DENY_ALL';

INSERT INTO authz_std_pol_template (id, tenant_id, template_code, template_name, pol_type, expression_script, param_schema, status, created_by, updated_by, is_deleted)
SELECT 302, '__GLOBAL__', 'DENY_ALL', '拒绝全部', 'DENY', 'return false;', '{}', 'ENABLED', 'system', 'system', 0
WHERE NOT EXISTS (SELECT 1 FROM authz_std_pol_template WHERE tenant_id = '__GLOBAL__' AND template_code = 'DENY_ALL')
  AND NOT EXISTS (SELECT 1 FROM authz_std_pol_template WHERE id = 302);

UPDATE authz_std_pol_template
SET template_name = '主体一致',
    pol_type = 'SCRIPT',
    expression_script = 'return subjectId != null && subjectId.equals(resourceOwnerId);',
    status = 'ENABLED',
    updated_by = 'system',
    updated_at = CURRENT_TIMESTAMP,
    is_deleted = 0
WHERE tenant_id = '__GLOBAL__' AND template_code = 'SUBJECT_MATCH';

INSERT INTO authz_std_pol_template (id, tenant_id, template_code, template_name, pol_type, expression_script, param_schema, status, created_by, updated_by, is_deleted)
SELECT 303, '__GLOBAL__', 'SUBJECT_MATCH', '主体一致', 'SCRIPT', 'return subjectId != null && subjectId.equals(resourceOwnerId);', '{"fields":[{"name":"resourceOwnerId","type":"STRING"}]}', 'ENABLED', 'system', 'system', 0
WHERE NOT EXISTS (SELECT 1 FROM authz_std_pol_template WHERE tenant_id = '__GLOBAL__' AND template_code = 'SUBJECT_MATCH')
  AND NOT EXISTS (SELECT 1 FROM authz_std_pol_template WHERE id = 303);

-- ============================================================
-- 元模型 schema_view 视图定义
-- ============================================================
UPDATE authz_meta_model
SET schema_view = '{"fields":[{"code":"staffNo","type":"STRING","role":"CODE","domainField":"staffNo","label":"员工工号","required":true},{"code":"staffName","type":"STRING","role":"NAME","domainField":"staffName","label":"员工姓名","required":true},{"code":"status","type":"STRING","role":"STATUS","domainField":"status","label":"状态","required":true},{"code":"userId","type":"STRING","role":"","domainField":"userId","label":"用户账号","required":false},{"code":"departmentCode","type":"STRING","role":"","domainField":"departmentCode","label":"部门编码","required":false},{"code":"departmentName","type":"STRING","role":"","domainField":"departmentName","label":"部门名称","required":false},{"code":"staffEmail","type":"STRING","role":"","domainField":"staffEmail","label":"工作邮箱","required":false},{"code":"personalMobile","type":"STRING","role":"","domainField":"personalMobile","label":"个人手机号","required":false}]}' ,
    updated_at = CURRENT_TIMESTAMP
WHERE model_code = 'SUB_USER' AND schema_view = '{}';

UPDATE authz_meta_model
SET schema_view = '{"fields":[{"code":"orgCode","type":"STRING","role":"CODE","domainField":"orgCode","label":"组织编码","required":true},{"code":"orgName","type":"STRING","role":"NAME","domainField":"orgName","label":"组织名称","required":true},{"code":"status","type":"STRING","role":"STATUS","domainField":"status","label":"状态","required":true},{"code":"parentOrgCode","type":"STRING","role":"","domainField":"parentOrgCode","label":"父级编码","required":false}]}' ,
    updated_at = CURRENT_TIMESTAMP
WHERE model_code = 'SUB_ORG' AND schema_view = '{}';

UPDATE authz_meta_model
SET schema_view = '{"fields":[{"code":"positionCode","type":"STRING","role":"CODE","domainField":"positionCode","label":"岗位编码","required":true},{"code":"positionName","type":"STRING","role":"NAME","domainField":"positionName","label":"岗位名称","required":true},{"code":"status","type":"STRING","role":"STATUS","domainField":"status","label":"状态","required":true}]}' ,
    updated_at = CURRENT_TIMESTAMP
WHERE model_code = 'SUB_POSITION' AND schema_view = '{}';

UPDATE authz_meta_model
SET schema_view = '{"fields":[{"code":"groupCode","type":"STRING","role":"CODE","domainField":"groupCode","label":"用户组编码","required":true},{"code":"groupName","type":"STRING","role":"NAME","domainField":"groupName","label":"用户组名称","required":true},{"code":"status","type":"STRING","role":"STATUS","domainField":"status","label":"状态","required":true}]}' ,
    updated_at = CURRENT_TIMESTAMP
WHERE model_code = 'SUB_GROUP' AND schema_view = '{}';

UPDATE authz_meta_model
SET schema_view = '{"fields":[{"code":"roleCode","type":"STRING","role":"CODE","domainField":"roleCode","label":"角色编码","required":true},{"code":"roleName","type":"STRING","role":"NAME","domainField":"roleName","label":"角色名称","required":true},{"code":"status","type":"STRING","role":"STATUS","domainField":"status","label":"状态","required":true},{"code":"description","type":"STRING","role":"","domainField":"description","label":"角色描述","required":false}]}' ,
    updated_at = CURRENT_TIMESTAMP
WHERE model_code = 'SUB_ROLE' AND schema_view = '{}';

UPDATE authz_meta_model
SET schema_view = '{"fields":[{"code":"menuCode","type":"STRING","role":"CODE","domainField":"menuCode","label":"菜单编码","required":true},{"code":"menuName","type":"STRING","role":"NAME","domainField":"menuName","label":"菜单名称","required":true},{"code":"status","type":"STRING","role":"STATUS","domainField":"status","label":"状态","required":true},{"code":"parentMenuCode","type":"STRING","role":"","domainField":"parentMenuCode","label":"父级菜单","required":false},{"code":"routePath","type":"STRING","role":"","domainField":"routePath","label":"路由地址","required":false}]}' ,
    updated_at = CURRENT_TIMESTAMP
WHERE model_code = 'RES_UI_MENU' AND schema_view = '{}';

UPDATE authz_meta_model
SET schema_view = '{"fields":[{"code":"pageCode","type":"STRING","role":"CODE","domainField":"pageCode","label":"页面编码","required":true},{"code":"pageName","type":"STRING","role":"NAME","domainField":"pageName","label":"页面名称","required":true},{"code":"status","type":"STRING","role":"STATUS","domainField":"status","label":"状态","required":true},{"code":"menuCode","type":"STRING","role":"","domainField":"menuCode","label":"所属菜单","required":false}]}' ,
    updated_at = CURRENT_TIMESTAMP
WHERE model_code = 'RES_UI_PAGE' AND schema_view = '{}';

UPDATE authz_meta_model
SET schema_view = '{"fields":[{"code":"componentCode","type":"STRING","role":"CODE","domainField":"componentCode","label":"组件编码","required":true},{"code":"componentName","type":"STRING","role":"NAME","domainField":"componentName","label":"组件名称","required":true},{"code":"status","type":"STRING","role":"STATUS","domainField":"status","label":"状态","required":true},{"code":"pageCode","type":"STRING","role":"","domainField":"pageCode","label":"所属页面","required":false}]}' ,
    updated_at = CURRENT_TIMESTAMP
WHERE model_code = 'RES_UI_COMPONENT' AND schema_view = '{}';

UPDATE authz_meta_model
SET schema_view = '{"fields":[{"code":"apiCode","type":"STRING","role":"CODE","domainField":"apiCode","label":"接口编码","required":true},{"code":"apiName","type":"STRING","role":"NAME","domainField":"apiName","label":"接口名称","required":true},{"code":"status","type":"STRING","role":"STATUS","domainField":"status","label":"状态","required":true},{"code":"httpMethod","type":"STRING","role":"","domainField":"httpMethod","label":"请求方法","required":false},{"code":"uriPattern","type":"STRING","role":"","domainField":"uriPattern","label":"请求路径","required":true}]}' ,
    updated_at = CURRENT_TIMESTAMP
WHERE model_code = 'RES_API' AND schema_view = '{}';
