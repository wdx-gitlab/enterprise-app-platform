
package com.ruijie.authzengine.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.application.spi.DataItem;
import com.ruijie.authzengine.application.spi.ModelFieldSchema;
import com.ruijie.authzengine.application.spi.ModelFieldSchema.FieldDefinition;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import com.ruijie.authzengine.domain.model.governance.resource.SysResComponent;
import com.ruijie.authzengine.domain.model.governance.resource.SysResMenu;
import com.ruijie.authzengine.domain.model.governance.resource.SysResPage;
import com.ruijie.authzengine.domain.model.governance.subject.AuthRole;
import com.ruijie.authzengine.domain.model.governance.subject.SysOrgNode;
import com.ruijie.authzengine.domain.model.governance.subject.SysPosition;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserAccount;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserGroup;
import com.ruijie.authzengine.shared.exception.BusinessException;
import com.ruijie.authzengine.shared.exception.ErrorCode;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Shadow Mode 双向映射工具。
 *
 * <p>负责：
 * <ul>
 *   <li>解析 schema：优先 schemaView JSON，为空则取内置默认</li>
 *   <li>逆向查询参数翻译：引擎标准字段 → 宿主适配器字段</li>
 *   <li>DataItem → 领域模型（读操作，含 UI 兜底守卫与数据校验）</li>
 *   <li>领域模型 → DataItem（写操作）</li>
 * </ul>
 */
public final class ShadowDataMapper {

    private static final Logger log = LoggerFactory.getLogger(ShadowDataMapper.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 各领域模型 CODE 角色对应的 Java 字段名。 */
    private static final Map<Class<?>, String> CODE_FIELD_MAP;
    /** 各领域模型 NAME 角色对应的 Java 字段名。 */
    private static final Map<Class<?>, String> NAME_FIELD_MAP;
    /** 内置默认 Schema 注册表（modelCode → ModelFieldSchema）。 */
    private static final Map<String, ModelFieldSchema> DEFAULT_SCHEMA_REGISTRY;
    /** 引擎保留字段，平铺宿主 attributes 时跳过，防止与引擎注入字段碰撞。 */
    private static final Set<String> RESERVED_FIELDS =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList("id", "tenantId", "appCode")));

    static {
        Map<Class<?>, String> codeMap = new HashMap<>();
        codeMap.put(SysUserAccount.class, "staffNo");
        codeMap.put(SysOrgNode.class, "departmentCode");
        codeMap.put(SysPosition.class, "positionCode");
        codeMap.put(SysUserGroup.class, "groupCode");
        codeMap.put(AuthRole.class, "roleCode");
        codeMap.put(SysResMenu.class, "menuCode");
        codeMap.put(SysResPage.class, "pageCode");
        codeMap.put(SysResComponent.class, "componentCode");
        codeMap.put(SysResApi.class, "apiCode");
        CODE_FIELD_MAP = Collections.unmodifiableMap(codeMap);

        Map<Class<?>, String> nameMap = new HashMap<>();
        nameMap.put(SysUserAccount.class, "staffName");
        nameMap.put(SysOrgNode.class, "departmentName");
        nameMap.put(SysPosition.class, "positionName");
        nameMap.put(SysUserGroup.class, "groupName");
        nameMap.put(AuthRole.class, "roleName");
        nameMap.put(SysResMenu.class, "menuName");
        nameMap.put(SysResPage.class, "pageName");
        nameMap.put(SysResComponent.class, "componentName");
        nameMap.put(SysResApi.class, "apiName");
        NAME_FIELD_MAP = Collections.unmodifiableMap(nameMap);

        Map<String, ModelFieldSchema> registry = new LinkedHashMap<>();
        registry.put("SUB_USER", buildSchema(
            fd("id", "STRING", "ID", "id", "唯一标识", false),
            fd("staffNo", "STRING", "CODE", "staffNo", "员工工号", true),
            fd("staffName", "STRING", "NAME", "staffName", "员工姓名", true),
            fd("status", "STRING", "STATUS", "status", "状态", true),
            fd("userId", "STRING", "", "userId", "用户账号", false),
            fd("staffName", "STRING", "", "staffName", "员工姓名", false),
            fd("departmentCode", "STRING", "", "departmentCode", "部门编码", false),
            fd("departmentName", "STRING", "", "departmentName", "部门名称", false),
            fd("staffEmail", "STRING", "", "staffEmail", "工作邮箱", false),
            fd("personalMobile", "STRING", "", "personalMobile", "个人手机号", false),
            fd("orgCode", "STRING", "", "orgCode", "所属组织", false)
        ));
        registry.put("SUB_ORG", buildSchema(
            fd("id", "STRING", "ID", "id", "唯一标识", false),
            fd("departmentCode", "STRING", "CODE", "departmentCode", "组织编码", true),
            fd("departmentName", "STRING", "NAME", "departmentName", "组织名称", true),
            fd("status", "STRING", "STATUS", "status", "状态", true),
            fd("parentDepartmentCode", "STRING", "", "parentDepartmentCode", "父级编码", false)
        ));
        registry.put("SUB_POSITION", buildSchema(
            fd("id", "STRING", "ID", "id", "唯一标识", false),
            fd("positionCode", "STRING", "CODE", "positionCode", "岗位编码", true),
            fd("positionName", "STRING", "NAME", "positionName", "岗位名称", true),
            fd("status", "STRING", "STATUS", "status", "状态", true),
            fd("orgCode", "STRING", "", "orgCode", "所属组织", false)
        ));
        registry.put("SUB_GROUP", buildSchema(
            fd("id", "STRING", "ID", "id", "唯一标识", false),
            fd("groupCode", "STRING", "CODE", "groupCode", "用户组编码", true),
            fd("groupName", "STRING", "NAME", "groupName", "用户组名称", true),
            fd("status", "STRING", "STATUS", "status", "状态", true)
        ));
        registry.put("SUB_ROLE", buildSchema(
            fd("id", "STRING", "ID", "id", "唯一标识", false),
            fd("roleCode", "STRING", "CODE", "roleCode", "角色编码", true),
            fd("roleName", "STRING", "NAME", "roleName", "角色名称", true),
            fd("status", "STRING", "STATUS", "status", "状态", true),
            fd("roleScope", "STRING", "", "roleScope", "角色范围", false)
        ));
        registry.put("RES_UI_MENU", buildSchema(
            fd("id", "STRING", "ID", "id", "唯一标识", false),
            fd("menuCode", "STRING", "CODE", "menuCode", "菜单编码", true),
            fd("menuName", "STRING", "NAME", "menuName", "菜单名称", true),
            fd("status", "STRING", "STATUS", "status", "状态", true),
            fd("parentMenuCode", "STRING", "", "parentMenuCode", "父级菜单", false),
            fd("routePath", "STRING", "", "routePath", "路由地址", false)
        ));
        registry.put("RES_UI_PAGE", buildSchema(
            fd("id", "STRING", "ID", "id", "唯一标识", false),
            fd("pageCode", "STRING", "CODE", "pageCode", "页面编码", true),
            fd("pageName", "STRING", "NAME", "pageName", "页面名称", true),
            fd("status", "STRING", "STATUS", "status", "状态", true),
            fd("menuCode", "STRING", "", "menuCode", "所属菜单", false)
        ));
        registry.put("RES_UI_COMPONENT", buildSchema(
            fd("id", "STRING", "ID", "id", "唯一标识", false),
            fd("componentCode", "STRING", "CODE", "componentCode", "组件编码", true),
            fd("componentName", "STRING", "NAME", "componentName", "组件名称", true),
            fd("status", "STRING", "STATUS", "status", "状态", true),
            fd("pageCode", "STRING", "", "pageCode", "所属页面", false)
        ));
        registry.put("RES_API", buildSchema(
            fd("id", "STRING", "ID", "id", "唯一标识", false),
            fd("apiCode", "STRING", "CODE", "apiCode", "接口编码", true),
            fd("apiName", "STRING", "NAME", "apiName", "接口名称", true),
            fd("status", "STRING", "STATUS", "status", "状态", true),
            fd("httpMethod", "STRING", "", "httpMethod", "请求方法", false),
            fd("uriPattern", "STRING", "", "uriPattern", "请求路径", true)
        ));
        DEFAULT_SCHEMA_REGISTRY = Collections.unmodifiableMap(registry);
    }

    private ShadowDataMapper() {
    }

    /**
     * 解析字段 Schema：优先用 schemaView JSON，为空时取内置默认。
     *
     * @param modelCode  模型编码
     * @param schemaView DB 中的 schema_view JSON 文本
     * @return 解析后的 ModelFieldSchema
     */
    public static ModelFieldSchema resolveSchema(String modelCode, String schemaView) {
        if (StringUtils.hasText(schemaView) && !"{}" .equals(schemaView.trim())) {
            try {
                return OBJECT_MAPPER.readValue(schemaView, ModelFieldSchema.class);
            } catch (Exception e) {
                log.warn("[ShadowDataMapper] schemaView 解析失败，回退至内置默认. modelCode={}", modelCode, e);
            }
        }
        ModelFieldSchema def = DEFAULT_SCHEMA_REGISTRY.get(modelCode);
        if (def == null) {
            throw new BusinessException(ErrorCode.INTEGRATION_ERROR,
                "未找到 modelCode=" + modelCode + " 的内置 Schema，请在 schema_view 中配置字段映射");
        }
        return def;
    }

    /**
     * 逆向查询参数翻译：引擎标准字段名 → 宿主适配器字段名。
     *
     * <p>例如 {"orgCode": "ORG-001"} 根据 domainField 映射翻译为 {"departmentId": "ORG-001"}。
     *
     * @param standardParams 引擎标准参数（以 domainField 为 key）
     * @param schema         字段 Schema
     * @return 宿主适配器参数（以 code 为 key）
     */
    public static Map<String, String> toShadowQueryParams(Map<String, String> standardParams,
                                                           ModelFieldSchema schema) {
        if (standardParams == null || standardParams.isEmpty()) {
            return Collections.emptyMap();
        }
        // 构建 domainField -> code 的反向索引
        Map<String, String> domainToCode = new HashMap<>();
        for (FieldDefinition fd : schema.getFields()) {
            if (StringUtils.hasText(fd.getDomainField()) && StringUtils.hasText(fd.getCode())) {
                domainToCode.put(fd.getDomainField(), fd.getCode());
            }
        }
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : standardParams.entrySet()) {
            String shadowKey = domainToCode.getOrDefault(entry.getKey(), entry.getKey());
            result.put(shadowKey, entry.getValue());
        }
        return result;
    }

    /**
     * DataItem → 领域模型（读操作）。
     *
     * <p>步骤：
     * <ol>
     *   <li>UI 兜底守卫：CODE/NAME 不能为空，否则抛 INTEGRATION_ERROR</li>
     *   <li>数据有效性约束：必填项、类型校验</li>
     *   <li>赋值映射：role → 核心字段；domainField → 反射赋值；纯扩展 → attributes</li>
     *   <li>容错转换：status 多值兼容解析</li>
     * </ol>
     *
     * @param item      宿主返回的数据项
     * @param modelClass 目标领域模型 Class
     * @param schema    字段 Schema
     * @param tenantId  租户标识（用于日志）
     * @param appCode   应用标识（用于日志）
     * @return 填充后的领域模型实例
     */
    public static <T> T toModel(DataItem item, Class<T> modelClass,
                                 ModelFieldSchema schema, String tenantId, String appCode) {
        // 1. UI 兜底守卫
        if (!StringUtils.hasText(item.getCode())) {
            throw new BusinessException(ErrorCode.INTEGRATION_ERROR,
                "宿主适配器返回数据缺少 code 字段（role=CODE），可能导致前端白屏. tenantId=" + tenantId + " appCode=" + appCode);
        }
        if (!StringUtils.hasText(item.getName())) {
            throw new BusinessException(ErrorCode.INTEGRATION_ERROR,
                "宿主适配器返回数据缺少 name 字段（role=NAME），可能导致前端白屏. tenantId=" + tenantId + " appCode=" + appCode);
        }

        // 2 & 3. 根据 Schema 校验并映射
        validateRequiredFields(item, schema);

        T instance = newInstance(modelClass);
        if (StringUtils.hasText(item.getId())) {
            trySetFieldValue(instance, "id", item.getId());
        }
        Map<String, Object> attributes = new HashMap<>();
        // 记录 schema 已处理的 attribute key，防止 passthrough 将其再次写入 attributes
        // 避免与 @JsonAnyGetter 平铺字段和 POJO 字段发生重复序列化（如 menuCode 出现两次）
        Set<String> schemaHandledKeys = new HashSet<>();

        for (FieldDefinition fd : schema.getFields()) {
            String value = resolveFieldValue(item, fd);
            if (value == null) {
                continue;
            }
            String role = fd.getRole();
            if ("ID".equals(role)) {
                setFieldValue(instance, "id", value);
                schemaHandledKeys.add(fd.getCode());
            } else if ("CODE".equals(role)) {
                // 通过类级别映射表确定 CODE 字段名
                setFieldValue(instance, CODE_FIELD_MAP.getOrDefault(modelClass, "code"), value);
                schemaHandledKeys.add(fd.getCode());
            } else if ("NAME".equals(role)) {
                // 通过类级别映射表确定 NAME 字段名
                setFieldValue(instance, NAME_FIELD_MAP.getOrDefault(modelClass, "name"), value);
                schemaHandledKeys.add(fd.getCode());
            } else if ("STATUS".equals(role)) {
                setFieldValue(instance, "status", normalizeStatus(value));
                schemaHandledKeys.add(fd.getCode());
            } else if (StringUtils.hasText(fd.getDomainField())) {
                // 有 domainField：反射设值，失败则降级到 attributes；无论成败均标记为已处理
                if (!trySetFieldValue(instance, fd.getDomainField(), value)) {
                    attributes.put(fd.getCode(), value);
                }
                schemaHandledKeys.add(fd.getCode());
            } else {
                // 纯扩展字段
                attributes.put(fd.getCode(), value);
                schemaHandledKeys.add(fd.getCode());
            }
        }

        // 将宿主的原始 attributes 里未被 schema 覆盖的字段透传
        // 处理规则：
        //   ① 引擎保留字段（id/tenantId/appCode）：跳过
        //   ② schema 已显式处理的字段 key：跳过（已写入 POJO 字段或 attributes）
        //   ③ POJO 中存在同名字段：直接反射赋值到 POJO 字段
        //      —— 避免 @JsonAnyGetter 平铺 attributes 时与 POJO 字段产生重复 JSON key
        //      —— 同时保留字段值（如 menuCode、pageCode 等父级关联字段）
        //   ④ 其余字段：放入 attributes 扩展透传
        if (item.getAttributes() != null) {
            item.getAttributes().forEach((k, v) -> {
                if (RESERVED_FIELDS.contains(k) || schemaHandledKeys.contains(k)) {
                    return;
                }
                if (findField(instance.getClass(), k) != null) {
                    // POJO 有同名字段：直接赋值，不放入 attributes，防止 @JsonAnyGetter 重复序列化
                    trySetFieldValue(instance, k, v != null ? v.toString() : null);
                } else {
                    attributes.putIfAbsent(k, v);
                }
            });
        }

        if (!attributes.isEmpty()) {
            trySetFieldValue(instance, "attributes", attributes);
        }

        // 补充租户与应用标识（任意领域模型公共字段）
        trySetFieldValue(instance, "tenantId", tenantId);
        trySetFieldValue(instance, "appCode", appCode);

        return instance;
    }

    /**
     * DataItem 分页结果 → 领域模型分页结果。
     *
     * @param src       宿主分页结果
     * @param modelClass 目标领域模型 Class
     * @param schema    字段 Schema
     * @param tenantId  租户标识
     * @param appCode   应用标识
     * @return 领域模型分页结果
     */
    public static <T> PageResult<T> toModelPage(
            PageResult<DataItem> src, Class<T> modelClass,
            ModelFieldSchema schema, String tenantId, String appCode) {
        List<T> records = src.getRecords() == null ? Collections.emptyList() :
            src.getRecords().stream()
                .map(item -> toModel(item, modelClass, schema, tenantId, appCode))
                .collect(Collectors.toList());
        return PageResult.<T>builder()
            .pageNo(src.getPageNo())
            .pageSize(src.getPageSize())
            .total(src.getTotal())
            .records(records)
            .build();
    }

    /**
     * 领域模型 → DataItem（写操作）。
     *
     * @param domainModel 领域模型实例
     * @param schema      字段 Schema
     * @return 宿主适配器接收的数据载体
     */
    public static DataItem fromModel(Object domainModel, ModelFieldSchema schema) {
        Class<?> modelClass = domainModel.getClass();
        DataItem.DataItemBuilder builder = DataItem.builder();
        Map<String, Object> attributes = new HashMap<>();

        for (FieldDefinition fd : schema.getFields()) {
            String role = fd.getRole();
            if ("ID".equals(role)) {
                Object v = getFieldValue(domainModel, "id");
                if (v != null) {
                    builder.id(v.toString());
                }
            } else if ("CODE".equals(role)) {
                String codeField = CODE_FIELD_MAP.getOrDefault(modelClass, "code");
                Object v = getFieldValue(domainModel, codeField);
                if (v != null) {
                    builder.code(v.toString());
                }
            } else if ("NAME".equals(role)) {
                String nameField = NAME_FIELD_MAP.getOrDefault(modelClass, "name");
                Object v = getFieldValue(domainModel, nameField);
                if (v != null) {
                    builder.name(v.toString());
                }
            } else if ("STATUS".equals(role)) {
                Object v = getFieldValue(domainModel, "status");
                if (v != null) {
                    builder.status(v.toString());
                }
            } else if (StringUtils.hasText(fd.getDomainField())) {
                Object v = getFieldValue(domainModel, fd.getDomainField());
                if (v != null) {
                    attributes.put(fd.getCode(), v);
                }
            }
        }

        if (!attributes.isEmpty()) {
            builder.attributes(attributes);
        }
        return builder.build();
    }

    // ────────── 私有方法 ──────────

    /** 校验必填项。 */
    private static void validateRequiredFields(DataItem item, ModelFieldSchema schema) {
        for (FieldDefinition fd : schema.getFields()) {
            if (!fd.isRequired()) {
                continue;
            }
            String value = resolveFieldValue(item, fd);
            if (!StringUtils.hasText(value)) {
                throw new BusinessException(ErrorCode.INTEGRATION_ERROR,
                    "宿主适配器返回数据缺少必填字段: " + fd.getCode() + "（label=" + fd.getLabel() + "）");
            }
        }
    }

    /** 从 DataItem 提取某字段的原始值（优先核心字段，次选 attributes）。 */
    private static String resolveFieldValue(DataItem item, FieldDefinition fd) {
        String role = fd.getRole();
        if ("ID".equals(role)) {
            return item.getId();
        }
        if ("CODE".equals(role)) {
            return item.getCode();
        }
        if ("NAME".equals(role)) {
            return item.getName();
        }
        if ("STATUS".equals(role)) {
            return item.getStatus();
        }
        // 从 attributes 取
        if (item.getAttributes() != null) {
            Object v = item.getAttributes().get(fd.getCode());
            return v == null ? null : v.toString();
        }
        return null;
    }

    /** status 兼容解析：1/"1"/true → ENABLED；0/"0"/false → DISABLED。 */
    private static String normalizeStatus(String raw) {
        if ("1".equals(raw) || "true".equalsIgnoreCase(raw) || "ACTIVE".equalsIgnoreCase(raw)) {
            return "ENABLED";
        }
        if ("0".equals(raw) || "false".equalsIgnoreCase(raw) || "INACTIVE".equalsIgnoreCase(raw)) {
            return "DISABLED";
        }
        return StringUtils.hasText(raw) ? raw : "ENABLED";
    }

    /** 反射设值，失败时静默忽略（用于核心角色字段，不应静默忽略，故不调用此方法处理 CODE/NAME/STATUS）。 */
    private static void setFieldValue(Object target, String fieldName, Object value) {
        if (!trySetFieldValue(target, fieldName, value)) {
            log.warn("[ShadowDataMapper] 未找到字段 '{}'，赋值跳过. class={}", fieldName,
                target.getClass().getSimpleName());
        }
    }

    /** 反射设值，返回是否成功。 */
    private static boolean trySetFieldValue(Object target, String fieldName, Object value) {
        if (!StringUtils.hasText(fieldName) || value == null) {
            return false;
        }
        Field field = findField(target.getClass(), fieldName);
        if (field == null) {
            return false;
        }
        try {
            field.setAccessible(true);
            field.set(target, coerce(value, field.getType()));
            return true;
        } catch (Exception e) {
            log.debug("[ShadowDataMapper] 字段 '{}' 赋值异常: {}", fieldName, e.getMessage());
            return false;
        }
    }

    /** 反射取值。 */
    private static Object getFieldValue(Object source, String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return null;
        }
        Field field = findField(source.getClass(), fieldName);
        if (field == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            return field.get(source);
        } catch (Exception e) {
            return null;
        }
    }

    /** 递归向上查找字段（包含父类）。 */
    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /** 简单类型强制转换。 */
    @SuppressWarnings("unchecked")
    private static Object coerce(Object value, Class<?> targetType) {
        if (value == null || targetType.isInstance(value)) {
            return value;
        }
        String strVal = value.toString();
        if (targetType == String.class) {
            return strVal;
        }
        if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(strVal);
        }
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(strVal);
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(strVal);
        }
        return value;
    }

    /** 无参构造创建实例。 */
    private static <T> T newInstance(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                "无法实例化领域模型: " + clazz.getSimpleName());
        }
    }

    // ────────── Schema 构建辅助 ──────────

    private static ModelFieldSchema buildSchema(FieldDefinition... fields) {
        return new ModelFieldSchema(Arrays.asList(fields));
    }

    private static FieldDefinition fd(String code, String type, String role,
                                       String domainField, String label, boolean required) {
        return new FieldDefinition(code, type, role, domainField, label, required);
    }
}
