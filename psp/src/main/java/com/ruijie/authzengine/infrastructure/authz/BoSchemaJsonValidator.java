package com.ruijie.authzengine.infrastructure.authz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.shared.exception.AuthzIntegrationException;
import com.ruijie.authzengine.shared.exception.BusinessException;
import com.ruijie.authzengine.shared.exception.ErrorCode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 业务对象 schemaJson 校验器。
 *
 * <p>治理保存链路仅接受新的 BO 元模型协议（`entities[]` + `operations[]`）；
 * 运行时仍兼容历史 V2 Lite（`fields[]`）协议，避免存量数据立刻失效。</p>
 */
@Component
public class BoSchemaJsonValidator {

    private static final String DEFAULT_RESOURCE_PREFIX = "res";

    private static final String SUPPORTED_VERSION = "2.0";

    private static final Set<String> SUPPORTED_ATTRIBUTE_TYPES = new LinkedHashSet<>();

    private static final Set<String> SUPPORTED_FIELD_CONTROL_STRATEGIES = new LinkedHashSet<>();

    static {
        Collections.addAll(
            SUPPORTED_ATTRIBUTE_TYPES,
            "STRING",
            "INTEGER",
            "LONG",
            "DECIMAL",
            "DATE",
            "DATETIME",
            "BOOLEAN"
        );
        Collections.addAll(
            SUPPORTED_FIELD_CONTROL_STRATEGIES,
            "OPEN",
            "RESTRICTED",
            "MASK",
            "HIDE"
        );
    }

    private final ObjectMapper objectMapper;

    public BoSchemaJsonValidator(@org.springframework.beans.factory.annotation.Qualifier("authzObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 在治理保存阶段校验 schemaJson，非法配置按业务异常返回。
     *
     * @param schemaJson 业务对象 schemaJson
     */
    public void validateForSave(String schemaJson) {
        try {
            validateForWriteSchema(schemaJson);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                ErrorCode.BAD_REQUEST,
                "业务对象 schemaJson 不符合 BO 元模型协议: " + exception.getMessage()
            );
        }
    }

    /**
     * 在运行时加载阶段校验 schemaJson，非法配置按集成异常处理。
     *
     * @param schemaJson 业务对象 schemaJson
     */
    public void validateForRuntime(String schemaJson) {
        try {
            resolvePolicyExposure(schemaJson);
        } catch (IllegalArgumentException exception) {
            throw new AuthzIntegrationException("业务对象 schemaJson 配置非法: " + exception.getMessage(), exception);
        }
    }

    /**
     * 解析运行时可见字段与资源前缀配置。
     *
     * @param schemaJson 业务对象 schemaJson
     * @return 运行时暴露策略
     */
    public PolicyExposure resolvePolicyExposure(String schemaJson) {
        JsonNode root = readNonEmptyRoot(schemaJson);
        if (root == null) {
            return PolicyExposure.legacy();
        }
        if (!root.isObject()) {
            throw new IllegalArgumentException("顶层必须是 JSON 对象");
        }
        if (looksLikeNewSchema(root)) {
            return resolveNewSchemaPolicyExposure(root);
        }
        JsonNode fieldsNode = validateFieldsContainer(root);
        validateVersion(root);
        Map<String, FieldDefinition> fieldDefinitions = validateAndCollectFields(fieldsNode);
        JsonNode policyContextNode = validateOptionalObject(root, "policyContext");
        validateSqlTranslation(root);
        return buildPolicyExposure(policyContextNode, fieldDefinitions);
    }

    /**
     * 解析 PDP 运行时所需的主表名与字段元数据。
     *
     * @param schemaJson 业务对象 schemaJson
     * @return 主表与字段元数据
     */
    public GovernanceMetadata resolveGovernanceMetadata(String schemaJson) {
        JsonNode root = readNonEmptyRoot(schemaJson);
        if (root == null) {
            return GovernanceMetadata.empty();
        }
        if (!root.isObject()) {
            throw new IllegalArgumentException("顶层必须是 JSON 对象");
        }
        if (looksLikeNewSchema(root)) {
            validateAndCollectNewSchema(root);
            return resolveNewSchemaGovernanceMetadata(root.get("entities"));
        }
        JsonNode fieldsNode = validateFieldsContainer(root);
        validateVersion(root);
        return new GovernanceMetadata(null, collectLegacyGovernanceAttributes(fieldsNode));
    }

    private void validateForWriteSchema(String schemaJson) {
        JsonNode root = readNonEmptyRoot(schemaJson);
        if (root == null) {
            throw new IllegalArgumentException("schemaJson 不能为空，且必须包含 entities[] 与 operations[]");
        }
        if (!root.isObject()) {
            throw new IllegalArgumentException("顶层必须是 JSON 对象");
        }
        if (!looksLikeNewSchema(root)) {
            if (looksLikeLegacySchema(root)) {
                throw new IllegalArgumentException("治理保存链路仅接受 entities[] / operations[] 新协议，不再接受旧 fields[] 协议");
            }
            throw new IllegalArgumentException("顶层必须包含 entities[] 与 operations[]");
        }
        validateAndCollectNewSchema(root);
    }

    private JsonNode readNonEmptyRoot(String schemaJson) {
        if (!StringUtils.hasText(schemaJson)) {
            return null;
        }
        String trimmed = schemaJson.trim();
        if ("{}".equals(trimmed)) {
            return null;
        }
        JsonNode root = parseRoot(trimmed);
        if (root == null || root.isNull()) {
            return null;
        }
        if (root.isObject() && root.size() == 0) {
            return null;
        }
        return root;
    }

    private void validateVersion(JsonNode root) {
        JsonNode versionNode = root.get("version");
        if (versionNode == null || versionNode.isNull()) {
            return;
        }
        if (!versionNode.isTextual() || !StringUtils.hasText(versionNode.asText())) {
            throw new IllegalArgumentException("version 必须是非空字符串");
        }
        if (!SUPPORTED_VERSION.equals(versionNode.asText().trim())) {
            throw new IllegalArgumentException("version 当前仅支持 2.0");
        }
    }

    private JsonNode validateFieldsContainer(JsonNode root) {
        JsonNode fieldsNode = root.get("fields");
        if (fieldsNode == null || !fieldsNode.isArray()) {
            throw new IllegalArgumentException("fields 必须是数组");
        }
        return fieldsNode;
    }

    private Map<String, FieldDefinition> validateAndCollectFields(JsonNode fieldsNode) {
        Map<String, FieldDefinition> fieldDefinitions = new LinkedHashMap<>();
        for (int index = 0; index < fieldsNode.size(); index++) {
            FieldDefinition fieldDefinition = validateField(fieldsNode.get(index), index);
            if (fieldDefinitions.containsKey(fieldDefinition.getCode())) {
                throw new IllegalArgumentException("fields[" + index + "].code 不能重复");
            }
            fieldDefinitions.put(fieldDefinition.getCode(), fieldDefinition);
        }
        return fieldDefinitions;
    }

    private FieldDefinition validateField(JsonNode fieldNode, int index) {
        if (fieldNode == null || !fieldNode.isObject()) {
            throw new IllegalArgumentException("fields[" + index + "] 必须是对象");
        }
        String code = validateRequiredTextField(fieldNode, index, "code");
        validateRequiredTextField(fieldNode, index, "type");
        String source = validateOptionalTextField(fieldNode, index, "source");
        boolean filterable = validateOptionalBooleanField(fieldNode, index, "filterable", false);
        boolean exposedToPolicy = validateOptionalBooleanField(fieldNode, index, "exposedToPolicy", true);
        validateOptionalBooleanField(fieldNode, index, "computed", false);
        JsonNode operatorsNode = fieldNode.get("operators");
        if (operatorsNode != null && !operatorsNode.isNull() && !operatorsNode.isArray()) {
            throw new IllegalArgumentException("fields[" + index + "].operators 必须是数组");
        }
        if (operatorsNode != null && operatorsNode.isArray()) {
            for (int operatorIndex = 0; operatorIndex < operatorsNode.size(); operatorIndex++) {
                JsonNode operatorNode = operatorsNode.get(operatorIndex);
                if (operatorNode == null || !operatorNode.isTextual() || !StringUtils.hasText(operatorNode.asText())) {
                    throw new IllegalArgumentException(
                        "fields[" + index + "].operators[" + operatorIndex + "] 必须是非空字符串"
                    );
                }
            }
        }
        if (filterable && !StringUtils.hasText(source)) {
            throw new IllegalArgumentException("fields[" + index + "].source 在 filterable=true 时必须存在");
        }
        return new FieldDefinition(code, exposedToPolicy);
    }

    private String validateRequiredTextField(JsonNode fieldNode, int index, String fieldName) {
        JsonNode valueNode = fieldNode.get(fieldName);
        if (valueNode == null || !valueNode.isTextual() || !StringUtils.hasText(valueNode.asText())) {
            throw new IllegalArgumentException("fields[" + index + "]." + fieldName + " 必须是非空字符串");
        }
        return valueNode.asText().trim();
    }

    private String validateOptionalTextField(JsonNode fieldNode, int index, String fieldName) {
        JsonNode valueNode = fieldNode.get(fieldName);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        if (!valueNode.isTextual() || !StringUtils.hasText(valueNode.asText())) {
            throw new IllegalArgumentException("fields[" + index + "]." + fieldName + " 必须是非空字符串或 null");
        }
        return valueNode.asText().trim();
    }

    private boolean validateOptionalBooleanField(JsonNode fieldNode, int index, String fieldName, boolean defaultValue) {
        JsonNode valueNode = fieldNode.get(fieldName);
        if (valueNode == null || valueNode.isNull()) {
            return defaultValue;
        }
        if (!valueNode.isBoolean()) {
            throw new IllegalArgumentException("fields[" + index + "]." + fieldName + " 必须是布尔值");
        }
        return valueNode.asBoolean();
    }

    private JsonNode parseRoot(String schemaJson) {
        try {
            return objectMapper.readTree(schemaJson);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("不是合法 JSON", exception);
        }
    }

    private JsonNode validateOptionalObject(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node != null && !node.isNull() && !node.isObject()) {
            throw new IllegalArgumentException(fieldName + " 必须是对象");
        }
        return node;
    }

    private void validateSqlTranslation(JsonNode root) {
        JsonNode sqlTranslationNode = validateOptionalObject(root, "sqlTranslation");
        if (sqlTranslationNode == null || sqlTranslationNode.isNull()) {
            return;
        }
        JsonNode modeNode = sqlTranslationNode.get("mode");
        if (modeNode != null && !modeNode.isNull() && (!modeNode.isTextual() || !StringUtils.hasText(modeNode.asText()))) {
            throw new IllegalArgumentException("sqlTranslation.mode 必须是非空字符串");
        }
    }

    private PolicyExposure buildPolicyExposure(JsonNode policyContextNode, Map<String, FieldDefinition> fieldDefinitions) {
        if (fieldDefinitions.isEmpty()) {
            return PolicyExposure.legacy();
        }
        String resourcePrefix = resolveResourcePrefix(policyContextNode);
        Set<String> allowedFields = resolveAllowedFields(policyContextNode, fieldDefinitions);
        if (allowedFields.isEmpty()) {
            allowedFields = collectDefaultAllowedFields(fieldDefinitions);
        }
        return new PolicyExposure(false, resourcePrefix, allowedFields);
    }

    private PolicyExposure resolveNewSchemaPolicyExposure(JsonNode root) {
        NewSchemaOutline outline = validateAndCollectNewSchema(root);
        return new PolicyExposure(false, DEFAULT_RESOURCE_PREFIX, outline.getDeclaredAttributeCodes());
    }

    private GovernanceMetadata resolveNewSchemaGovernanceMetadata(JsonNode entitiesNode) {
        List<Map<String, Object>> attributes = new ArrayList<>();
        String primaryTableName = null;
        for (int entityIndex = 0; entityIndex < entitiesNode.size(); entityIndex++) {
            JsonNode entityNode = entitiesNode.get(entityIndex);
            String entityCode = entityNode.path("code").asText().trim();
            String tableName = entityNode.path("tableName").asText().trim();
            boolean primary = entityNode.path("isPrimary").asBoolean(false);
            if (!StringUtils.hasText(primaryTableName) || primary) {
                primaryTableName = tableName;
            }
            JsonNode attributesNode = entityNode.path("attributes");
            for (int attributeIndex = 0; attributeIndex < attributesNode.size(); attributeIndex++) {
                attributes.add(buildNewSchemaAttributeMetadata(attributesNode.get(attributeIndex), entityCode, tableName));
            }
        }
        return new GovernanceMetadata(primaryTableName, attributes);
    }

    private List<Map<String, Object>> collectLegacyGovernanceAttributes(JsonNode fieldsNode) {
        List<Map<String, Object>> attributes = new ArrayList<>();
        for (int index = 0; index < fieldsNode.size(); index++) {
            JsonNode fieldNode = fieldsNode.get(index);
            validateField(fieldNode, index);
            attributes.add(buildLegacyAttributeMetadata(fieldNode));
        }
        return attributes;
    }

    private Map<String, Object> buildNewSchemaAttributeMetadata(JsonNode attributeNode, String entityCode, String tableName) {
        Map<String, Object> attribute = new LinkedHashMap<>();
        attribute.put("code", attributeNode.path("code").asText().trim());
        putIfHasText(attribute, "fieldName", attributeNode.path("fieldName").asText(null));
        putIfHasText(attribute, "columnName", attributeNode.path("columnName").asText(null));
        putIfHasText(attribute, "name", attributeNode.path("name").asText(null));
        putIfHasText(attribute, "type", attributeNode.path("type").asText(null));
        attribute.put("isPk", attributeNode.path("isPk").asBoolean(false));
        attribute.put("fieldControl", attributeNode.path("fieldControl").asBoolean(false));
        attribute.put("filterable", attributeNode.path("filterable").asBoolean(false));
        putIfHasText(attribute, "fieldControlStrategy", attributeNode.path("fieldControlStrategy").asText(null));
        attribute.put("entityCode", entityCode);
        attribute.put("tableName", tableName);
        return attribute;
    }

    private Map<String, Object> buildLegacyAttributeMetadata(JsonNode fieldNode) {
        String code = fieldNode.path("code").asText().trim();
        String source = fieldNode.path("source").asText(null);
        Map<String, Object> attribute = new LinkedHashMap<>();
        attribute.put("code", code);
        attribute.put("fieldName", code);
        attribute.put("columnName", StringUtils.hasText(source) ? source.trim() : code);
        putIfHasText(attribute, "type", fieldNode.path("type").asText(null));
        attribute.put("fieldControl", false);
        attribute.put("filterable", fieldNode.path("filterable").asBoolean(false));
        attribute.put("isPk", false);
        return attribute;
    }

    private void putIfHasText(Map<String, Object> target, String key, String value) {
        if (StringUtils.hasText(value)) {
            target.put(key, value.trim());
        }
    }

    private boolean looksLikeNewSchema(JsonNode root) {
        return root.has("entities") || root.has("operations");
    }

    private boolean looksLikeLegacySchema(JsonNode root) {
        return root.has("fields") || root.has("policyContext") || root.has("sqlTranslation") || root.has("version");
    }

    private NewSchemaOutline validateAndCollectNewSchema(JsonNode root) {
        JsonNode entitiesNode = root.get("entities");
        if (entitiesNode == null || !entitiesNode.isArray()) {
            throw new IllegalArgumentException("entities 必须是数组");
        }
        if (entitiesNode.size() == 0) {
            throw new IllegalArgumentException("entities 至少包含一个实体");
        }
        JsonNode operationsNode = root.get("operations");
        if (operationsNode == null || !operationsNode.isArray()) {
            throw new IllegalArgumentException("operations 必须是数组");
        }

        Set<String> entityCodes = new LinkedHashSet<>();
        Set<String> declaredAttributeCodes = new LinkedHashSet<>();
        int primaryEntityCount = 0;
        boolean containsPkAttribute = false;
        for (int entityIndex = 0; entityIndex < entitiesNode.size(); entityIndex++) {
            JsonNode entityNode = entitiesNode.get(entityIndex);
            if (entityNode == null || !entityNode.isObject()) {
                throw new IllegalArgumentException("entities[" + entityIndex + "] 必须是对象");
            }
            String entityCode = validateRequiredText(entityNode, "entities[" + entityIndex + "].code");
            if (!entityCodes.add(entityCode)) {
                throw new IllegalArgumentException("entities[" + entityIndex + "].code 不能重复");
            }
            validateOptionalText(entityNode, "entities[" + entityIndex + "].name");
            if (validateOptionalBoolean(entityNode, "entities[" + entityIndex + "].isPrimary", false)) {
                primaryEntityCount++;
            }
            validateRequiredText(entityNode, "entities[" + entityIndex + "].tableName");
            validateOptionalStringArray(entityNode.get("rowLevelRuleEntries"), "entities[" + entityIndex + "].rowLevelRuleEntries");
            if (validateEntityAttributes(entityNode.get("attributes"), entityIndex, declaredAttributeCodes)) {
                containsPkAttribute = true;
            }
        }
        if (primaryEntityCount == 0) {
            throw new IllegalArgumentException("同一 BO 必须且只能有一个 isPrimary=true 的实体");
        }
        if (primaryEntityCount > 1) {
            throw new IllegalArgumentException("同一 BO 最多只能有一个 isPrimary=true 的实体");
        }
        if (!containsPkAttribute) {
            throw new IllegalArgumentException("至少需要一个 isPk=true 的属性");
        }
        validateOperations(operationsNode, entityCodes);
        return new NewSchemaOutline(entityCodes, declaredAttributeCodes);
    }

    private boolean validateEntityAttributes(JsonNode attributesNode, int entityIndex, Set<String> declaredAttributeCodes) {
        String pathPrefix = "entities[" + entityIndex + "].attributes";
        if (attributesNode == null || !attributesNode.isArray()) {
            throw new IllegalArgumentException(pathPrefix + " 必须是数组");
        }
        if (attributesNode.size() == 0) {
            throw new IllegalArgumentException(pathPrefix + " 至少包含一个属性");
        }
        Set<String> entityAttributeCodes = new LinkedHashSet<>();
        boolean containsPkAttribute = false;
        for (int attributeIndex = 0; attributeIndex < attributesNode.size(); attributeIndex++) {
            JsonNode attributeNode = attributesNode.get(attributeIndex);
            String attributePath = pathPrefix + "[" + attributeIndex + "]";
            if (attributeNode == null || !attributeNode.isObject()) {
                throw new IllegalArgumentException(attributePath + " 必须是对象");
            }
            String attributeCode = validateRequiredText(attributeNode, attributePath + ".code");
            if (!entityAttributeCodes.add(attributeCode)) {
                throw new IllegalArgumentException(attributePath + ".code 不能重复");
            }
            declaredAttributeCodes.add(attributeCode);
            validateOptionalText(attributeNode, attributePath + ".fieldName");
            validateOptionalText(attributeNode, attributePath + ".columnName");
            validateOptionalText(attributeNode, attributePath + ".name");
            String type = validateRequiredText(attributeNode, attributePath + ".type");
            if (!SUPPORTED_ATTRIBUTE_TYPES.contains(type)) {
                throw new IllegalArgumentException(attributePath + ".type 取值非法: " + type);
            }
            if (validateRequiredBoolean(attributeNode, attributePath + ".isPk")) {
                containsPkAttribute = true;
            }
            boolean fieldControl = validateOptionalBoolean(attributeNode, attributePath + ".fieldControl", false);
            validateOptionalBoolean(attributeNode, attributePath + ".filterable", false);
            String fieldControlStrategy = validateOptionalText(attributeNode, attributePath + ".fieldControlStrategy");
            if (fieldControlStrategy != null) {
                if (!fieldControl) {
                    throw new IllegalArgumentException(attributePath + ".fieldControlStrategy 仅允许在 fieldControl=true 时配置");
                }
                if (!SUPPORTED_FIELD_CONTROL_STRATEGIES.contains(fieldControlStrategy)) {
                    throw new IllegalArgumentException(attributePath + ".fieldControlStrategy 取值非法: " + fieldControlStrategy);
                }
            }
        }
        return containsPkAttribute;
    }

    private void validateOperations(JsonNode operationsNode, Set<String> entityCodes) {
        Set<String> operationCodes = new LinkedHashSet<>();
        for (int operationIndex = 0; operationIndex < operationsNode.size(); operationIndex++) {
            JsonNode operationNode = operationsNode.get(operationIndex);
            String operationPath = "operations[" + operationIndex + "]";
            if (operationNode == null || !operationNode.isObject()) {
                throw new IllegalArgumentException(operationPath + " 必须是对象");
            }
            String operationCode = validateRequiredText(operationNode, operationPath + ".code");
            if (!operationCodes.add(operationCode)) {
                throw new IllegalArgumentException(operationPath + ".code 不能重复");
            }
            validateRequiredText(operationNode, operationPath + ".name");
            String scope = validateRequiredText(operationNode, operationPath + ".scope");
            if (!"BO".equals(scope) && !"ENTITY".equals(scope)) {
                throw new IllegalArgumentException(operationPath + ".scope 仅支持 BO 或 ENTITY");
            }
            String entityCode = validateOptionalText(operationNode, operationPath + ".entityCode");
            if ("ENTITY".equals(scope)) {
                if (!StringUtils.hasText(entityCode)) {
                    throw new IllegalArgumentException(operationPath + ".entityCode 在 scope=ENTITY 时必填");
                }
                if (!entityCodes.contains(entityCode)) {
                    throw new IllegalArgumentException(operationPath + ".entityCode 未声明对应实体: " + entityCode);
                }
            }
        }
    }

    private String validateRequiredText(JsonNode node, String path) {
        if (node == null) {
            throw new IllegalArgumentException(path + " 必须是非空字符串");
        }
        String fieldName = extractFieldName(path);
        JsonNode valueNode = node.get(fieldName);
        if (valueNode == null || !valueNode.isTextual() || !StringUtils.hasText(valueNode.asText())) {
            throw new IllegalArgumentException(path + " 必须是非空字符串");
        }
        return valueNode.asText().trim();
    }

    private String validateOptionalText(JsonNode node, String path) {
        if (node == null) {
            return null;
        }
        String fieldName = extractFieldName(path);
        JsonNode valueNode = node.get(fieldName);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        if (!valueNode.isTextual() || !StringUtils.hasText(valueNode.asText())) {
            throw new IllegalArgumentException(path + " 必须是非空字符串或 null");
        }
        return valueNode.asText().trim();
    }

    private boolean validateRequiredBoolean(JsonNode node, String path) {
        if (node == null) {
            throw new IllegalArgumentException(path + " 必须是布尔值");
        }
        String fieldName = extractFieldName(path);
        JsonNode valueNode = node.get(fieldName);
        if (valueNode == null || !valueNode.isBoolean()) {
            throw new IllegalArgumentException(path + " 必须是布尔值");
        }
        return valueNode.asBoolean();
    }

    private boolean validateOptionalBoolean(JsonNode node, String path, boolean defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        String fieldName = extractFieldName(path);
        JsonNode valueNode = node.get(fieldName);
        if (valueNode == null || valueNode.isNull()) {
            return defaultValue;
        }
        if (!valueNode.isBoolean()) {
            throw new IllegalArgumentException(path + " 必须是布尔值");
        }
        return valueNode.asBoolean();
    }

    private void validateOptionalStringArray(JsonNode node, String path) {
        if (node == null || node.isNull()) {
            return;
        }
        if (!node.isArray()) {
            throw new IllegalArgumentException(path + " 必须是字符串数组");
        }
        for (int index = 0; index < node.size(); index++) {
            JsonNode itemNode = node.get(index);
            if (itemNode == null || !itemNode.isTextual() || !StringUtils.hasText(itemNode.asText())) {
                throw new IllegalArgumentException(path + "[" + index + "] 必须是非空字符串");
            }
        }
    }

    private String extractFieldName(String path) {
        int separatorIndex = path.lastIndexOf('.');
        return separatorIndex >= 0 ? path.substring(separatorIndex + 1) : path;
    }

    private String resolveResourcePrefix(JsonNode policyContextNode) {
        if (policyContextNode == null || policyContextNode.isNull()) {
            return DEFAULT_RESOURCE_PREFIX;
        }
        JsonNode resourcePrefixNode = policyContextNode.get("resourcePrefix");
        if (resourcePrefixNode == null || resourcePrefixNode.isNull()) {
            return DEFAULT_RESOURCE_PREFIX;
        }
        if (!resourcePrefixNode.isTextual() || !StringUtils.hasText(resourcePrefixNode.asText())) {
            throw new IllegalArgumentException("policyContext.resourcePrefix 必须是非空字符串");
        }
        return resourcePrefixNode.asText().trim();
    }

    private Set<String> resolveAllowedFields(JsonNode policyContextNode, Map<String, FieldDefinition> fieldDefinitions) {
        Set<String> allowedFields = new LinkedHashSet<>();
        if (policyContextNode == null || policyContextNode.isNull()) {
            return allowedFields;
        }
        JsonNode allowFieldsNode = policyContextNode.get("allowFields");
        if (allowFieldsNode == null || allowFieldsNode.isNull()) {
            return allowedFields;
        }
        if (!allowFieldsNode.isArray()) {
            throw new IllegalArgumentException("policyContext.allowFields 必须是数组");
        }
        for (int index = 0; index < allowFieldsNode.size(); index++) {
            String fieldCode = readAllowedFieldCode(allowFieldsNode.get(index), index);
            ensureAllowedFieldExists(fieldCode, fieldDefinitions);
            allowedFields.add(fieldCode);
        }
        return allowedFields;
    }

    private String readAllowedFieldCode(JsonNode fieldNode, int index) {
        if (fieldNode == null || !fieldNode.isTextual() || !StringUtils.hasText(fieldNode.asText())) {
            throw new IllegalArgumentException("policyContext.allowFields[" + index + "] 必须是非空字符串");
        }
        return fieldNode.asText().trim();
    }

    private void ensureAllowedFieldExists(String fieldCode, Map<String, FieldDefinition> fieldDefinitions) {
        FieldDefinition definition = fieldDefinitions.get(fieldCode);
        if (definition == null) {
            throw new IllegalArgumentException("policyContext.allowFields 包含未声明字段: " + fieldCode);
        }
        if (!definition.isExposedToPolicy()) {
            throw new IllegalArgumentException("policyContext.allowFields 不能包含 exposedToPolicy=false 的字段: " + fieldCode);
        }
    }

    private Set<String> collectDefaultAllowedFields(Map<String, FieldDefinition> fieldDefinitions) {
        Set<String> defaultAllowedFields = new LinkedHashSet<>();
        for (FieldDefinition definition : fieldDefinitions.values()) {
            if (definition.isExposedToPolicy()) {
                defaultAllowedFields.add(definition.getCode());
            }
        }
        return defaultAllowedFields;
    }

    private static final class FieldDefinition {

        private final String code;

        private final boolean exposedToPolicy;

        private FieldDefinition(String code, boolean exposedToPolicy) {
            this.code = code;
            this.exposedToPolicy = exposedToPolicy;
        }

        private String getCode() {
            return code;
        }

        private boolean isExposedToPolicy() {
            return exposedToPolicy;
        }
    }

    private static final class NewSchemaOutline {

        private final Set<String> entityCodes;

        private final Set<String> declaredAttributeCodes;

        private NewSchemaOutline(Set<String> entityCodes, Set<String> declaredAttributeCodes) {
            this.entityCodes = Collections.unmodifiableSet(new LinkedHashSet<>(entityCodes));
            this.declaredAttributeCodes = Collections.unmodifiableSet(new LinkedHashSet<>(declaredAttributeCodes));
        }

        private Set<String> getEntityCodes() {
            return entityCodes;
        }

        private Set<String> getDeclaredAttributeCodes() {
            return declaredAttributeCodes;
        }
    }

    /**
     * schemaJson 运行时暴露策略。
     */
    public static final class PolicyExposure {

        private final boolean legacySchema;

        private final String resourcePrefix;

        private final Set<String> allowedFields;

        private PolicyExposure(boolean legacySchema, String resourcePrefix, Set<String> allowedFields) {
            this.legacySchema = legacySchema;
            this.resourcePrefix = resourcePrefix;
            this.allowedFields = Collections.unmodifiableSet(new LinkedHashSet<>(allowedFields));
        }

        private static PolicyExposure legacy() {
            return new PolicyExposure(true, DEFAULT_RESOURCE_PREFIX, Collections.<String>emptySet());
        }

        public boolean isLegacySchema() {
            return legacySchema;
        }

        public String getResourcePrefix() {
            return resourcePrefix;
        }

        public Set<String> getAllowedFields() {
            return allowedFields;
        }

        /**
         * 按白名单裁剪 Hook 返回属性，避免未声明字段直接暴露给策略上下文。
         *
         * @param attributes Hook 返回属性
         * @return 裁剪后的属性
         */
        public Map<String, Object> projectAttributes(Map<String, Object> attributes) {
            if (attributes == null || attributes.isEmpty()) {
                return Collections.emptyMap();
            }
            if (legacySchema) {
                return new LinkedHashMap<>(attributes);
            }
            Map<String, Object> projected = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                if (allowedFields.contains(entry.getKey())) {
                    projected.put(entry.getKey(), entry.getValue());
                }
            }
            return projected;
        }
    }

    /**
     * schemaJson 对 PDP 暴露的主表与字段元数据。
     */
    public static final class GovernanceMetadata {

        private static final GovernanceMetadata EMPTY = new GovernanceMetadata(null, Collections.<Map<String, Object>>emptyList());

        private final String tableName;

        private final List<Map<String, Object>> attributes;

        private GovernanceMetadata(String tableName, List<Map<String, Object>> attributes) {
            this.tableName = StringUtils.hasText(tableName) ? tableName.trim() : null;
            List<Map<String, Object>> safeAttributes = new ArrayList<>();
            if (attributes != null) {
                for (Map<String, Object> attribute : attributes) {
                    safeAttributes.add(Collections.unmodifiableMap(new LinkedHashMap<>(attribute)));
                }
            }
            this.attributes = Collections.unmodifiableList(safeAttributes);
        }

        public static GovernanceMetadata empty() {
            return EMPTY;
        }

        public String getTableName() {
            return tableName;
        }

        public List<Map<String, Object>> getAttributes() {
            return attributes;
        }

        public boolean isEmpty() {
            return !StringUtils.hasText(tableName) && attributes.isEmpty();
        }
    }
}