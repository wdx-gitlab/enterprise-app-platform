-- authz-engine 引擎基线数据。
-- 包含全局标准动作字典和各元模型的 schema_view 视图定义。

-- ============================================================
-- 全局标准动作字典（含别名）
-- ============================================================
INSERT INTO authz_std_act_dict (id,tenant_id,act_code,act_name,act_type,res_category,risk_level,created_by,created_at,updated_by,updated_at,is_deleted,act_aliases)
VALUES
(1,'__GLOBAL__','READ','查看','STANDARD','ALL',1,'system',CURRENT_TIMESTAMP,'system',CURRENT_TIMESTAMP,0,'QUERY,VIEW'),
(2,'__GLOBAL__','UPDATE','审批','STANDARD','ALL',3,'system',CURRENT_TIMESTAMP,'system',CURRENT_TIMESTAMP,0,'APPROVE_REQUEST,AGREE'),
(3,'__GLOBAL__','DELETE','删除','STANDARD','ALL',3,'system',CURRENT_TIMESTAMP,'system',CURRENT_TIMESTAMP,0,'REMOVE,ERASE'),
(4,'__GLOBAL__','CREATE','创建','STANDARD','ALL',1,'system',CURRENT_TIMESTAMP,'system',CURRENT_TIMESTAMP,0,'QUERY_LIST,SEARCH_LIST');

-- ============================================================
-- 全局策略模板（最小可用基线）
-- ============================================================
INSERT INTO authz_std_pol_template (id,tenant_id,template_code,template_name,pol_type,expression_script,param_schema,status,created_by,created_at,updated_by,updated_at,is_deleted)
VALUES
(1,'__GLOBAL__','DATA_SCOPE_DEPT','按部门数据范围','DATA','true','{"type":"object"}','ENABLED','system',CURRENT_TIMESTAMP,NULL,NULL,0),
(2,'__GLOBAL__','ENV_WORK_HOUR','工作时间限制','ENV','env.hour >= 9 && env.hour <= 18','{"type":"object"}','ENABLED','system',CURRENT_TIMESTAMP,NULL,NULL,0),
(3,'__GLOBAL__','STATE_ACTIVE_ONLY','仅有效状态可操作','STATE','res.status == "ACTIVE"','{"type":"object"}','ENABLED','system',CURRENT_TIMESTAMP,'system',CURRENT_TIMESTAMP,0);


-- ============================================================
-- 元模型 schema_view 视图定义
-- ============================================================
INSERT INTO authz_meta_model (id,tenant_id,app_code,model_code,model_name,category,adapter_type,resolver,created_by,created_at,updated_by,updated_at,is_deleted,schema_view)
VALUES
(1,'default','default','RES_DATA_BO','业务对象','RESOURCE','JAVA_BEAN','noopHook','system',CURRENT_TIMESTAMP,'system',CURRENT_TIMESTAMP,0,NULL),
(2,'default','default','SUB_USER','用户主体','SUBJECT','JAVA_BEAN','noopHook','system',CURRENT_TIMESTAMP,'system',CURRENT_TIMESTAMP,0,'{"fields":[{"code":"userId","type":"STRING","role":"CODE","domainField":"userId","label":"用户ID","required":true},{"code":"userName","type":"STRING","role":"NAME","domainField":"userName","label":"用户姓名","required":true},{"code":"status","type":"STRING","role":"STATUS","domainField":"status","label":"状态","required":true},{"code":"deptId","type":"STRING","role":"","domainField":"deptId","label":"部门ID","required":false}]}'),
(3,'default','default','SUB_ORG','组织主体','SUBJECT','JAVA_BEAN','noopHook','system',CURRENT_TIMESTAMP,'system',CURRENT_TIMESTAMP,0,'{"fields":[{"code":"orgCode","type":"STRING","role":"CODE","domainField":"orgCode","label":"组织编码","required":true},{"code":"orgName","type":"STRING","role":"NAME","domainField":"orgName","label":"组织名称","required":true},{"code":"status","type":"STRING","role":"STATUS","domainField":"status","label":"状态","required":true},{"code":"parentOrgCode","type":"STRING","role":"","domainField":"parentOrgCode","label":"父级编码","required":false}]}'),
(4,'default','default','SUB_POSITION','岗位主体','SUBJECT','JAVA_BEAN','noopHook','system',CURRENT_TIMESTAMP,'system',CURRENT_TIMESTAMP,0,'{"fields":[{"code":"positionCode","type":"STRING","role":"CODE","domainField":"positionCode","label":"岗位编码","required":true},{"code":"positionName","type":"STRING","role":"NAME","domainField":"positionName","label":"岗位名称","required":true},{"code":"status","type":"STRING","role":"STATUS","domainField":"status","label":"状态","required":true}]}'),
(5,'default','default','SUB_GROUP','用户组主体','SUBJECT','JAVA_BEAN','noopHook','system',CURRENT_TIMESTAMP,'system',CURRENT_TIMESTAMP,0,'{"fields":[{"code":"groupCode","type":"STRING","role":"CODE","domainField":"groupCode","label":"用户组编码","required":true},{"code":"groupName","type":"STRING","role":"NAME","domainField":"groupName","label":"用户组名称","required":true},{"code":"status","type":"STRING","role":"STATUS","domainField":"status","label":"状态","required":true}]}'),
(6,'default','default','SUB_ROLE','角色主体','SUBJECT','JAVA_BEAN','noopHook','system',CURRENT_TIMESTAMP,'system',CURRENT_TIMESTAMP,0,'{"fields":[{"code":"roleCode","type":"STRING","role":"CODE","domainField":"roleCode","label":"角色编码","required":true},{"code":"roleName","type":"STRING","role":"NAME","domainField":"roleName","label":"角色名称","required":true},{"code":"status","type":"STRING","role":"STATUS","domainField":"status","label":"状态","required":true},{"code":"dataScope","type":"STRING","role":"","domainField":"dataScope","label":"数据权限范围","required":false}]}'),
(7,'default','default','RES_UI_MENU','菜单资源','RESOURCE','JAVA_BEAN','noopHook','system',CURRENT_TIMESTAMP,'system',CURRENT_TIMESTAMP,0,'{"fields":[{"code":"menuCode","type":"STRING","role":"CODE","domainField":"menuCode","label":"菜单编码","required":true},{"code":"menuName","type":"STRING","role":"NAME","domainField":"menuName","label":"菜单名称","required":true},{"code":"status","type":"STRING","role":"STATUS","domainField":"status","label":"状态","required":true},{"code":"parentMenuCode","type":"STRING","role":"","domainField":"parentMenuCode","label":"父级菜单","required":false},{"code":"routePath","type":"STRING","role":"","domainField":"routePath","label":"路由地址","required":false}]}'),
(8,'default','default','RES_UI_PAGE','页面资源','RESOURCE','JAVA_BEAN','noopHook','system',CURRENT_TIMESTAMP,'system',CURRENT_TIMESTAMP,0,'{"fields":[
  {"code":"pageCode","type":"STRING","role":"CODE","domainField":"pageCode","label":"页面编码","required":true},
  {"code":"pageName","type":"STRING","role":"NAME","domainField":"pageName","label":"页面名称","required":true},
  {"code":"status","type":"STRING","role":"STATUS","domainField":"status","label":"状态","required":true},
  {"code":"menuCode","type":"STRING","role":"","domainField":"menuCode","label":"所属菜单","required":false}
]}'),
                                                                                                                                                                                                     (9,'default','default','RES_UI_COMPONENT','组件资源','RESOURCE','JAVA_BEAN','noopHook','system',CURRENT_TIMESTAMP,'system',CURRENT_TIMESTAMP,0,'{"fields":[
  {"code":"componentCode","type":"STRING","role":"CODE","domainField":"componentCode","label":"组件编码","required":true},
  {"code":"componentName","type":"STRING","role":"NAME","domainField":"componentName","label":"组件名称","required":true},
  {"code":"status","type":"STRING","role":"STATUS","domainField":"status","label":"状态","required":true},
  {"code":"pageCode","type":"STRING","role":"","domainField":"pageCode","label":"所属页面","required":false}
]}'),
                                                                                                                                                                                                     (10,'default','default','RES_API','接口资源','RESOURCE','JAVA_BEAN','noopHook','system',CURRENT_TIMESTAMP,NULL,NULL,0,'{
  "fields": [
    {
      "code": "apiCode",
      "type": "STRING",
      "role": "CODE",
      "domainField": "apiCode",
      "label": "权限标识",
      "required": true
    },
    {
      "code": "apiName",
      "type": "STRING",
      "role": "NAME",
      "domainField": "apiName",
      "label": "权限名称",
      "required": true
    },
    {
      "code": "status",
      "type": "STRING",
      "role": "STATUS",
      "domainField": "status",
      "label": "状态",
      "required": true
    }
  ]
}');
INSERT INTO authz_meta_model (id,tenant_id,app_code,model_code,model_name,category,adapter_type,resolver,created_by,created_at,updated_by,updated_at,is_deleted,schema_view)
VALUES
(11,'default','default','ACT_STANDARD','标准动作','ACTION','JAVA_BEAN','noopHook','system',CURRENT_TIMESTAMP,'system',CURRENT_TIMESTAMP,0,'{}'),
(12,'default','default','POL_DATA','数据策略','POLICY','JAVA_BEAN','noopHook','system',CURRENT_TIMESTAMP,'system',CURRENT_TIMESTAMP,0,'{}'),
(13,'default','default','POL_ENV','环境策略','POLICY','JAVA_BEAN','noopHook','system',CURRENT_TIMESTAMP,'system',CURRENT_TIMESTAMP,0,'{}'),
(14,'default','default','POL_STATE','状态策略','POLICY','JAVA_BEAN','noopHook','system',CURRENT_TIMESTAMP,'system',CURRENT_TIMESTAMP,0,'{}');

