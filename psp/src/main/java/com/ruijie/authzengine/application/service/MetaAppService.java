package com.ruijie.authzengine.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.ruijie.authzengine.application.spi.BoMetaModelAdapter;
import com.ruijie.authzengine.application.spi.BoSchemaColumnInfo;
import com.ruijie.authzengine.application.spi.NativeBoSchemaCollector;
import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.StandardActionDefinition;
import com.ruijie.authzengine.domain.model.governance.StandardPolicyTemplateDefinition;
import com.ruijie.authzengine.domain.model.common.PolicyTemplateType;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import com.ruijie.authzengine.domain.repository.MetaRepository;
import com.ruijie.authzengine.domain.repository.PermissionRepository;
import com.ruijie.authzengine.infrastructure.authz.BoResolverRouter;
import com.ruijie.authzengine.infrastructure.authz.BoSchemaJsonValidator;
import com.ruijie.authzengine.shared.exception.BusinessException;
import com.ruijie.authzengine.shared.exception.ErrorCode;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 治理元模型与标准库应用服务。
 * <p>负责鉴权元模型（AuthMetaModel）、业务对象元模型（BoMetaModel）、
 * 标准动作（StandardAction）、标准策略模板（StandardPolicyTemplate）的 CRUD 编排辽辑验证。
 * BO 元模型保存时会额外校验 schemaJson 格式和 Resolver 配置合法性。</p>
 */
@Slf4j
@Service
public class MetaAppService {

    private static final String BO_PERMISSION_RES_MODEL_CODE = "RES_DATA_BO";

    private static final Set<String> FIELD_ACTIONS = new LinkedHashSet<>(Arrays.asList(
        "OPEN", "RESTRICTED", "MASK", "HIDE"
    ));

    private static final Set<String> ENV_ALLOWED_DOMAINS = new LinkedHashSet<>(Arrays.asList("env", "param"));

    private static final Set<String> STATE_ALLOWED_DOMAINS = new LinkedHashSet<>(Arrays.asList("sub", "res", "param"));

    private static final Set<String> DATA_ALLOWED_DOMAINS = new LinkedHashSet<>(Arrays.asList("sub", "tableName", "attributes", "param", "param()"));

    private static final Set<String> FIELD_ALLOWED_DOMAINS = new LinkedHashSet<>(Arrays.asList("fieldName", "originalValue", "param"));

    private static final Pattern ENV_ROOT_PATTERN = Pattern.compile("(^|[^A-Za-z0-9_#])env\\s*\\.");

    private static final Pattern ENV_VAR_PATTERN = Pattern.compile("#env\\s*\\[");

    private static final Pattern SUB_ROOT_PATTERN = Pattern.compile("(^|[^A-Za-z0-9_#])sub\\s*\\.");

    private static final Pattern SUB_VAR_PATTERN = Pattern.compile("#sub\\s*\\[");

    private static final Pattern RES_ROOT_PATTERN = Pattern.compile("(^|[^A-Za-z0-9_#])res\\s*\\.");

    private static final Pattern RES_VAR_PATTERN = Pattern.compile("#res\\s*\\[");

    private static final Pattern PARAM_ROOT_PATTERN = Pattern.compile("(^|[^A-Za-z0-9_#])param\\s*\\.");

    private static final Pattern PARAM_VAR_PATTERN = Pattern.compile("#param\\s*\\[");

    private static final Pattern PARAM_FUNCTION_PATTERN = Pattern.compile("(^|[^A-Za-z0-9_#.])param\\s*\\(");

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("#tableName\\b");

    private static final Pattern ATTRIBUTES_PATTERN = Pattern.compile("#attributes\\b");

    private static final Pattern FIELD_NAME_PATTERN = Pattern.compile("#fieldName\\b");

    private static final Pattern ORIGINAL_VALUE_PATTERN = Pattern.compile("#originalValue\\b");

    private static final PermissionRepository NO_OP_PERMISSION_REPOSITORY = new PermissionRepository() {
        @Override
        public AuthPermissionItem savePermissionItem(AuthPermissionItem permissionItem) {
            return permissionItem;
        }

        @Override
        public PageResult<AuthPermissionItem> pagePermissionItems(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
            return PageResult.<AuthPermissionItem>builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .total(0)
                .records(Collections.emptyList())
                .build();
        }

        @Override
        public AuthPermissionItem findPermissionItem(String tenantId, String appCode, String permCode) {
            return null;
        }

        @Override
        public void deletePermissionItem(String tenantId, String appCode, String permCode) {
        }

        @Override
        public boolean hasPermissionItemReference(String tenantId, String appCode, String permCode) {
            return false;
        }
    };

    private final MetaRepository metaRepository;

    private final BoSchemaJsonValidator boSchemaJsonValidator;

    private final PermissionRepository permissionRepository;

    /** BO Resolver 路由器，用于 Shadow 模式元数据采集；可选，缺省时 Shadow 采集退化为空结果。 */
    private final BoResolverRouter boResolverRouter;

    /** Native 模式元数据采集器；可选，缺省时 Native 采集退化为空结果。 */
    private final NativeBoSchemaCollector nativeBoSchemaCollector;

    @Autowired
    public MetaAppService(
        MetaRepository metaRepository,
        BoSchemaJsonValidator boSchemaJsonValidator,
        PermissionRepository permissionRepository,
        BoResolverRouter boResolverRouter,
        ObjectProvider<NativeBoSchemaCollector> nativeCollectorProvider
    ) {
        this.metaRepository = metaRepository;
        this.boSchemaJsonValidator = boSchemaJsonValidator;
        this.permissionRepository = permissionRepository;
        this.boResolverRouter = boResolverRouter;
        this.nativeBoSchemaCollector = nativeCollectorProvider != null ? nativeCollectorProvider.getIfAvailable() : null;
    }

    /** 向后兼容构造器：无 Resolver / 采集器注入（适用于不需要采集功能的场景和旧单测）。 */
    public MetaAppService(
        MetaRepository metaRepository,
        BoSchemaJsonValidator boSchemaJsonValidator,
        PermissionRepository permissionRepository
    ) {
        this.metaRepository = metaRepository;
        this.boSchemaJsonValidator = boSchemaJsonValidator;
        this.permissionRepository = permissionRepository;
        this.boResolverRouter = null;
        this.nativeBoSchemaCollector = null;
    }

    public MetaAppService(MetaRepository metaRepository, PermissionRepository permissionRepository) {
        this(metaRepository, new BoSchemaJsonValidator(new ObjectMapper()), permissionRepository);
    }

    public MetaAppService(MetaRepository metaRepository) {
        this(metaRepository, new BoSchemaJsonValidator(new ObjectMapper()), NO_OP_PERMISSION_REPOSITORY);
    }

    /** 测试用构造器：支持注入 BoResolverRouter 和 NativeBoSchemaCollector，其余依赖使用默认 noop。 */
    public MetaAppService(
        MetaRepository metaRepository,
        BoResolverRouter boResolverRouter,
        NativeBoSchemaCollector nativeBoSchemaCollector
    ) {
        this.metaRepository = metaRepository;
        this.boSchemaJsonValidator = new BoSchemaJsonValidator(new ObjectMapper());
        this.permissionRepository = NO_OP_PERMISSION_REPOSITORY;
        this.boResolverRouter = boResolverRouter;
        this.nativeBoSchemaCollector = nativeBoSchemaCollector;
    }

    /**
     * 查询数据库中已配置的所有租户-应用组合。
     *
     * @return tenantId -> appCode 列表的映射
     */
    public Map<String, List<String>> listTenantApps() {
        return metaRepository.listDistinctTenantApps();
    }

    /**
     * 注册或更新权限元模型。
     *
     * @param definition 元模型定义
     * @return 已保存结果
     */
    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public AuthMetaModelDefinition registerMetaModel(AuthMetaModelDefinition definition) {
        log.info("[元模型服务] 注册/更新鉴权元模型: tenantId={}, appCode={}, modelCode={}",
            definition.getTenantId(), definition.getAppCode(), definition.getModelCode());
        return metaRepository.saveAuthMetaModel(definition);
    }

    public PageResult<AuthMetaModelDefinition> pageMetaModels(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        return metaRepository.pageAuthMetaModels(tenantId, appCode, keyword, pageNo, pageSize);
    }

    public AuthMetaModelDefinition getMetaModel(String tenantId, String appCode, String modelCode) {
        return requireMetaModel(tenantId, appCode, modelCode);
    }

    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public AuthMetaModelDefinition createMetaModel(AuthMetaModelDefinition definition) {
        log.info("[元模型服务] 创建鉴权元模型: tenantId={}, appCode={}, modelCode={}",
            definition.getTenantId(), definition.getAppCode(), definition.getModelCode());
        return metaRepository.saveAuthMetaModel(definition);
    }

    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public AuthMetaModelDefinition updateMetaModel(String tenantId, String appCode, String modelCode, AuthMetaModelDefinition definition) {
        AuthMetaModelDefinition existing = requireMetaModel(tenantId, appCode, modelCode);
        definition.setId(existing.getId());
        definition.setTenantId(tenantId);
        definition.setAppCode(appCode);
        definition.setModelCode(modelCode);
        log.info("[元模型服务] 更新鉴权元模型: tenantId={}, appCode={}, modelCode={}", tenantId, appCode, modelCode);
        return metaRepository.saveAuthMetaModel(definition);
    }

    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public void deleteMetaModel(String tenantId, String appCode, String modelCode) {
        requireMetaModel(tenantId, appCode, modelCode);
        if (metaRepository.hasAuthMetaModelReference(tenantId, appCode, modelCode)) {
            throw new BusinessException(ErrorCode.CONTROLLED_DELETE_CONFLICT, "权限元模型仍被引用，禁止删除");
        }
        log.info("[元模型服务] 删除鉴权元模型: tenantId={}, appCode={}, modelCode={}", tenantId, appCode, modelCode);
        metaRepository.deleteAuthMetaModel(tenantId, appCode, modelCode);
    }

    /**
     * 注册或更新业务对象元模型。
     *
     * @param definition 业务对象元模型定义
     * @return 已保存结果
     */
    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public BoMetaModelDefinition registerBoMetaModel(BoMetaModelDefinition definition) {
        log.info("[元模型服务] 注册/更新BO元模型: tenantId={}, appCode={}, boCode={}",
            definition.getTenantId(), definition.getAppCode(), definition.getBoCode());
        return saveBoMetaModel(definition, metaRepository.findBoMetaModel(
            definition.getTenantId(),
            definition.getAppCode(),
            definition.getBoCode()
        ));
    }

    public PageResult<BoMetaModelDefinition> pageBoMetaModels(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        return metaRepository.pageBoMetaModels(tenantId, appCode, keyword, pageNo, pageSize);
    }

    public BoMetaModelDefinition getBoMetaModel(String tenantId, String appCode, String boCode) {
        return requireBoMetaModel(tenantId, appCode, boCode);
    }

    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public BoMetaModelDefinition createBoMetaModel(BoMetaModelDefinition definition) {
        log.info("[元模型服务] 创建BO元模型: tenantId={}, appCode={}, boCode={}",
            definition.getTenantId(), definition.getAppCode(), definition.getBoCode());
        return saveBoMetaModel(definition, null);
    }

    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public BoMetaModelDefinition updateBoMetaModel(String tenantId, String appCode, String boCode, BoMetaModelDefinition definition) {
        BoMetaModelDefinition existing = requireBoMetaModel(tenantId, appCode, boCode);
        definition.setId(existing.getId());
        definition.setTenantId(tenantId);
        definition.setAppCode(appCode);
        definition.setBoCode(boCode);
        log.info("[元模型服务] 更新BO元模型: tenantId={}, appCode={}, boCode={}", tenantId, appCode, boCode);
        return saveBoMetaModel(definition, existing);
    }

    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public void deleteBoMetaModel(String tenantId, String appCode, String boCode) {
        BoMetaModelDefinition existing = requireBoMetaModel(tenantId, appCode, boCode);
        if (metaRepository.hasBoMetaModelReference(tenantId, appCode, boCode)) {
            throw new BusinessException(ErrorCode.CONTROLLED_DELETE_CONFLICT, "业务对象元模型仍被引用，禁止删除");
        }
        Set<String> existingOperationCodes = extractOperationCodes(existing.getSchemaJson());
        validateOperationRemovalReferences(existing, existingOperationCodes);
        deletePermissionItems(existing, existingOperationCodes);
        log.info("[元模型服务] 删除BO元模型: tenantId={}, appCode={}, boCode={}", tenantId, appCode, boCode);
        metaRepository.deleteBoMetaModel(tenantId, appCode, boCode);
    }

    /**
     * 查询标准动作库。
     *
     * @param tenantId 租户标识
     * @return 标准动作列表
     */
    public List<StandardActionDefinition> listStandardActions(String tenantId) {
        return metaRepository.pageStandardActions(tenantId, null, 1, 200).getRecords();
    }

    public PageResult<StandardActionDefinition> pageStandardActions(String tenantId, String keyword, int pageNo, int pageSize) {
        return metaRepository.pageStandardActions(tenantId, keyword, pageNo, pageSize);
    }

    public StandardActionDefinition getStandardAction(String tenantId, String actCode) {
        StandardActionDefinition definition = metaRepository.findStandardAction(tenantId, actCode);
        if (definition == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "标准动作不存在");
        }
        return definition;
    }

    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public StandardActionDefinition createStandardAction(StandardActionDefinition definition) {
        log.info("[元模型服务] 创建标准动作: tenantId={}, actCode={}",
            definition.getTenantId(), definition.getActCode());
        return metaRepository.saveStandardAction(definition);
    }

    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public StandardActionDefinition updateStandardAction(String tenantId, String actCode, StandardActionDefinition definition) {
        StandardActionDefinition existing = getStandardAction(tenantId, actCode);
        definition.setId(existing.getId());
        definition.setTenantId(tenantId);
        definition.setActCode(actCode);
        log.info("[元模型服务] 更新标准动作: tenantId={}, actCode={}", tenantId, actCode);
        return metaRepository.saveStandardAction(definition);
    }

    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public void deleteStandardAction(String tenantId, String actCode) {
        getStandardAction(tenantId, actCode);
        if (metaRepository.hasStandardActionReference(tenantId, actCode)) {
            throw new BusinessException(ErrorCode.CONTROLLED_DELETE_CONFLICT, "标准动作仍被权限项引用，禁止删除");
        }
        log.info("[元模型服务] 删除标准动作: tenantId={}, actCode={}", tenantId, actCode);
        metaRepository.deleteStandardAction(tenantId, actCode);
    }

    /**
     * 查询标准策略模板库。
     *
     * @param tenantId 租户标识
     * @return 标准策略模板列表
     */
    public List<StandardPolicyTemplateDefinition> listStandardPolicyTemplates(String tenantId) {
        return metaRepository.pageStandardPolicyTemplates(tenantId, null, 1, 200).getRecords();
    }

    public PageResult<StandardPolicyTemplateDefinition> pageStandardPolicyTemplates(String tenantId, String keyword, int pageNo, int pageSize) {
        return metaRepository.pageStandardPolicyTemplates(tenantId, keyword, pageNo, pageSize);
    }

    public PageResult<StandardPolicyTemplateDefinition> pageStandardPolicyTemplates(
        String tenantId,
        String keyword,
        String polType,
        int pageNo,
        int pageSize
    ) {
        return metaRepository.pageStandardPolicyTemplates(tenantId, keyword, polType, pageNo, pageSize);
    }

    public StandardPolicyTemplateDefinition getStandardPolicyTemplate(String tenantId, String templateCode) {
        StandardPolicyTemplateDefinition definition = metaRepository.findStandardPolicyTemplate(tenantId, templateCode);
        if (definition == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "策略模板不存在");
        }
        return definition;
    }

    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public StandardPolicyTemplateDefinition createStandardPolicyTemplate(StandardPolicyTemplateDefinition definition) {
        validatePolicyTemplateDefinition(definition);
        log.info("[元模型服务] 创建策略模板: tenantId={}, templateCode={}",
            definition.getTenantId(), definition.getTemplateCode());
        return metaRepository.saveStandardPolicyTemplate(definition);
    }

    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public StandardPolicyTemplateDefinition updateStandardPolicyTemplate(String tenantId, String templateCode, StandardPolicyTemplateDefinition definition) {
        StandardPolicyTemplateDefinition existing = getStandardPolicyTemplate(tenantId, templateCode);
        definition.setId(existing.getId());
        definition.setTenantId(tenantId);
        definition.setTemplateCode(templateCode);
        validatePolicyTemplateDefinition(definition);
        log.info("[元模型服务] 更新策略模板: tenantId={}, templateCode={}", tenantId, templateCode);
        return metaRepository.saveStandardPolicyTemplate(definition);
    }

    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public void deleteStandardPolicyTemplate(String tenantId, String templateCode) {
        getStandardPolicyTemplate(tenantId, templateCode);
        if (metaRepository.hasStandardPolicyTemplateReference(tenantId, templateCode)) {
            throw new BusinessException(ErrorCode.CONTROLLED_DELETE_CONFLICT, "策略模板仍被授权分配引用，禁止删除");
        }
        log.info("[元模型服务] 删除策略模板: tenantId={}, templateCode={}", tenantId, templateCode);
        metaRepository.deleteStandardPolicyTemplate(tenantId, templateCode);
    }

    private AuthMetaModelDefinition requireMetaModel(String tenantId, String appCode, String modelCode) {
        AuthMetaModelDefinition definition = metaRepository.findAuthMetaModel(tenantId, appCode, modelCode);
        if (definition == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "权限元模型不存在");
        }
        return definition;
    }

    private BoMetaModelDefinition requireBoMetaModel(String tenantId, String appCode, String boCode) {
        BoMetaModelDefinition definition = metaRepository.findBoMetaModel(tenantId, appCode, boCode);
        if (definition == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "业务对象元模型不存在");
        }
        return definition;
    }

    private void validateBoSchema(BoMetaModelDefinition definition) {
        if (definition == null) {
            return;
        }
        boSchemaJsonValidator.validateForSave(definition.getSchemaJson());
        validateBoResolver(definition);
    }

    private BoMetaModelDefinition saveBoMetaModel(BoMetaModelDefinition definition, BoMetaModelDefinition existingDefinition) {
        validateBoSchema(definition);
        Set<String> targetOperationCodes = extractOperationCodes(definition.getSchemaJson());
        validateOperationCatalog(definition.getTenantId(), targetOperationCodes);
        Set<String> existingOperationCodes = existingDefinition == null
            ? Collections.emptySet()
            : extractOperationCodes(existingDefinition.getSchemaJson());
        Set<String> removedOperationCodes = new LinkedHashSet<>(existingOperationCodes);
        removedOperationCodes.removeAll(targetOperationCodes);
        validateOperationRemovalReferences(
            existingDefinition == null ? definition : existingDefinition,
            removedOperationCodes
        );
        BoMetaModelDefinition savedDefinition = metaRepository.saveBoMetaModel(definition);
        syncPermissionItems(savedDefinition, targetOperationCodes);
        deletePermissionItems(savedDefinition, removedOperationCodes);
        return savedDefinition;
    }

    private void validateBoResolver(BoMetaModelDefinition definition) {
        if (!StringUtils.hasText(definition.getResolver()) || "noopHook".equalsIgnoreCase(definition.getResolver().trim())) {
            return;
        }
        if (!StringUtils.hasText(definition.getAdapterType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "业务对象 Hook 配置缺少 adapterType");
        }
        if (!"JAVA_BEAN".equalsIgnoreCase(definition.getAdapterType().trim())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前阶段仅支持 JAVA_BEAN 类型的业务对象 Hook");
        }
    }

    private void validateOperationCatalog(String tenantId, Set<String> operationCodes) {
        if (operationCodes.isEmpty()) {
            return;
        }
        Set<String> standardActionCodes = metaRepository.listStandardActions(tenantId)
            .stream()
            .map(StandardActionDefinition::getActCode)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        for (String operationCode : operationCodes) {
            if (!standardActionCodes.contains(operationCode)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "operations 包含未注册的标准动作: " + operationCode);
            }
        }
    }

    private Set<String> extractOperationCodes(String schemaJson) {
        if (!StringUtils.hasText(schemaJson)) {
            return Collections.emptySet();
        }
        try {
            JsonNode root = new ObjectMapper().readTree(schemaJson);
            JsonNode operationsNode = root == null ? null : root.get("operations");
            if (operationsNode == null || !operationsNode.isArray()) {
                return Collections.emptySet();
            }
            Set<String> operationCodes = new LinkedHashSet<>();
            for (int index = 0; index < operationsNode.size(); index++) {
                JsonNode operationNode = operationsNode.get(index);
                if (operationNode == null) {
                    continue;
                }
                JsonNode codeNode = operationNode.get("code");
                if (codeNode != null && codeNode.isTextual() && StringUtils.hasText(codeNode.asText())) {
                    operationCodes.add(codeNode.asText().trim());
                }
            }
            return operationCodes;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "业务对象 schemaJson 无法解析 operations[]");
        }
    }

    private void validateOperationRemovalReferences(BoMetaModelDefinition definition, Set<String> removedOperationCodes) {
        if (definition == null || removedOperationCodes.isEmpty()) {
            return;
        }
        for (String operationCode : removedOperationCodes) {
            String permissionCode = buildPermissionCode(definition, operationCode);
            if (permissionRepository.hasPermissionItemReference(definition.getTenantId(), definition.getAppCode(), permissionCode)) {
                throw new BusinessException(
                    ErrorCode.CONTROLLED_DELETE_CONFLICT,
                    "操作仍被授权分配或委托引用，禁止删除: " + operationCode
                );
            }
        }
    }

    private void syncPermissionItems(BoMetaModelDefinition definition, Set<String> operationCodes) {
        if (definition == null || operationCodes.isEmpty()) {
            return;
        }
        String resourceId = definition.getId() == null ? null : String.valueOf(definition.getId());
        for (String operationCode : operationCodes) {
            permissionRepository.savePermissionItem(AuthPermissionItem.builder()
                .tenantId(definition.getTenantId())
                .appCode(definition.getAppCode())
                .permCode(buildPermissionCode(definition, operationCode))
                .resModelCode(BO_PERMISSION_RES_MODEL_CODE)
                .resId(resourceId)
                .actCode(operationCode)
                .build());
        }
    }

    private void deletePermissionItems(BoMetaModelDefinition definition, Set<String> operationCodes) {
        if (definition == null || operationCodes.isEmpty()) {
            return;
        }
        for (String operationCode : operationCodes) {
            permissionRepository.deletePermissionItem(
                definition.getTenantId(),
                definition.getAppCode(),
                buildPermissionCode(definition, operationCode)
            );
        }
    }

    private String buildPermissionCode(BoMetaModelDefinition definition, String operationCode) {
        return definition.getAppCode() + ":bo:" + definition.getBoCode() + ":" + operationCode.trim().toUpperCase(Locale.ROOT);
    }

    private void validatePolicyTemplateDefinition(StandardPolicyTemplateDefinition definition) {
        if (definition == null || !StringUtils.hasText(definition.getPolType())) {
            return;
        }
        String normalizedType = definition.getPolType().trim().toUpperCase(Locale.ROOT);
        try {
            PolicyTemplateType.valueOf(normalizedType);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "策略类型不合法，仅支持 DATA/ENV/STATE/FIELD");
        }
        definition.setPolType(normalizedType);
        validatePolicyTemplateExpressionDomains(definition);
        if (PolicyTemplateType.FIELD.name().equals(normalizedType)) {
            validateFieldPolicyTemplateDefinition(definition);
        }
    }

    private void validatePolicyTemplateExpressionDomains(StandardPolicyTemplateDefinition definition) {
        if (definition == null || !StringUtils.hasText(definition.getPolType()) || !StringUtils.hasText(definition.getExpressionScript())) {
            return;
        }
        Set<String> referencedDomains = detectReferencedDomains(definition.getExpressionScript());
        if (referencedDomains.isEmpty()) {
            return;
        }
        Set<String> allowedDomains = resolveAllowedDomains(definition.getPolType());
        Set<String> invalidDomains = new LinkedHashSet<>(referencedDomains);
        invalidDomains.removeAll(allowedDomains);
        if (!invalidDomains.isEmpty()) {
            throw new BusinessException(
                ErrorCode.BAD_REQUEST,
                String.format(
                    "%s 策略模板仅允许引用 %s，检测到非法变量域: %s",
                    definition.getPolType(),
                    String.join("/", allowedDomains),
                    String.join(", ", invalidDomains)
                )
            );
        }
    }

    private Set<String> resolveAllowedDomains(String normalizedType) {
        if (PolicyTemplateType.ENV.name().equals(normalizedType)) {
            return ENV_ALLOWED_DOMAINS;
        }
        if (PolicyTemplateType.STATE.name().equals(normalizedType)) {
            return STATE_ALLOWED_DOMAINS;
        }
        if (PolicyTemplateType.DATA.name().equals(normalizedType)) {
            return DATA_ALLOWED_DOMAINS;
        }
        if (PolicyTemplateType.FIELD.name().equals(normalizedType)) {
            return FIELD_ALLOWED_DOMAINS;
        }
        return Collections.emptySet();
    }

    private Set<String> detectReferencedDomains(String expressionScript) {
        Set<String> referenced = new LinkedHashSet<>();
        addDomainIfReferenced(referenced, "env", ENV_ROOT_PATTERN, expressionScript);
        addDomainIfReferenced(referenced, "env", ENV_VAR_PATTERN, expressionScript);
        addDomainIfReferenced(referenced, "sub", SUB_ROOT_PATTERN, expressionScript);
        addDomainIfReferenced(referenced, "sub", SUB_VAR_PATTERN, expressionScript);
        addDomainIfReferenced(referenced, "res", RES_ROOT_PATTERN, expressionScript);
        addDomainIfReferenced(referenced, "res", RES_VAR_PATTERN, expressionScript);
        addDomainIfReferenced(referenced, "param", PARAM_ROOT_PATTERN, expressionScript);
        addDomainIfReferenced(referenced, "param", PARAM_VAR_PATTERN, expressionScript);
        addDomainIfReferenced(referenced, "param()", PARAM_FUNCTION_PATTERN, expressionScript);
        addDomainIfReferenced(referenced, "tableName", TABLE_NAME_PATTERN, expressionScript);
        addDomainIfReferenced(referenced, "attributes", ATTRIBUTES_PATTERN, expressionScript);
        addDomainIfReferenced(referenced, "fieldName", FIELD_NAME_PATTERN, expressionScript);
        addDomainIfReferenced(referenced, "originalValue", ORIGINAL_VALUE_PATTERN, expressionScript);
        return referenced;
    }

    private void addDomainIfReferenced(Set<String> referenced, String domain, Pattern pattern, String expressionScript) {
        if (pattern.matcher(expressionScript).find()) {
            referenced.add(domain);
        }
    }

    private void validateFieldPolicyTemplateDefinition(StandardPolicyTemplateDefinition definition) {
        if (!StringUtils.hasText(definition.getParamSchema())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "FIELD 策略模板必须声明 paramSchema");
        }
        JsonNode schemaRoot = parseParamSchema(definition.getParamSchema());
        JsonNode targetFieldNode = schemaRoot.path("properties").path("targetField");
        if (targetFieldNode.isMissingNode() || targetFieldNode.isNull() || !targetFieldNode.isObject()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "FIELD 策略模板必须声明 targetField 参数元数据");
        }
        String action = resolveFieldTemplateAction(schemaRoot);
        if (!StringUtils.hasText(action)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "FIELD 策略模板必须声明 action");
        }
        if (!FIELD_ACTIONS.contains(action)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "FIELD 策略模板 action 仅支持 OPEN/RESTRICTED/MASK/HIDE");
        }
        if ("MASK".equals(action) && FieldMaskScriptRules.isLegacyBrokenScript(definition.getExpressionScript())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "FIELD/MASK 脚本使用了已废弃的历史错误模板，请重新选择新的脱敏模板");
        }
    }

    private JsonNode parseParamSchema(String paramSchema) {
        try {
            JsonNode schemaRoot = new ObjectMapper().readTree(paramSchema);
            if (schemaRoot == null || !schemaRoot.isObject()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "策略模板 paramSchema 必须是 JSON 对象");
            }
            return schemaRoot;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "策略模板 paramSchema 不是合法 JSON");
        }
    }

    private String resolveFieldTemplateAction(JsonNode schemaRoot) {
        JsonNode rootAction = schemaRoot.get("action");
        if (rootAction != null && !rootAction.isNull()) {
            return normalizeFieldTemplateAction(rootAction);
        }
        JsonNode actionNode = schemaRoot.path("properties").path("action");
        if (actionNode.isMissingNode() || actionNode.isNull()) {
            return null;
        }
        if (actionNode.hasNonNull("const")) {
            return normalizeFieldTemplateAction(actionNode.get("const"));
        }
        if (actionNode.hasNonNull("default")) {
            return normalizeFieldTemplateAction(actionNode.get("default"));
        }
        JsonNode enumNode = actionNode.get("enum");
        if (enumNode != null && enumNode.isArray()) {
            if (enumNode.size() != 1) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "FIELD 策略模板 action 必须固定为单一枚举值");
            }
            return normalizeFieldTemplateAction(enumNode.get(0));
        }
        return null;
    }

    private String normalizeFieldTemplateAction(JsonNode actionNode) {
        if (actionNode == null || !actionNode.isTextual() || !StringUtils.hasText(actionNode.asText())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "FIELD 策略模板 action 必须是非空字符串");
        }
        return actionNode.asText().trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 预览 BO 元数据列信息。
     *
     * <p>SHADOW 模式通过宿主主数据源 JDBC 直接采集（业务库）；MANUAL 返回空列表。
     *
     * @param tableName 物理表名，SHADOW 模式必填
     * @param mode      采集模式：SHADOW | MANUAL
     */
    public List<BoSchemaColumnInfo> previewBoSchema(
        String tenantId, String appCode, String boCode, String tableName, String mode
    ) {
        if (!StringUtils.hasText(mode) || "MANUAL".equalsIgnoreCase(mode.trim())) {
            log.debug("[previewBoSchema] 手工模式，跳过自动采集 bo={}", boCode);
            return Collections.emptyList();
        }
        // SHADOW 及已废弃的 NATIVE 均走 JDBC 采集宿主主数据源
        if ("SHADOW".equalsIgnoreCase(mode.trim()) || "NATIVE".equalsIgnoreCase(mode.trim())) {
            return collectFromBusinessDb(tableName, boCode);
        }
        log.warn("[previewBoSchema] 未知采集模式，降级返回空列表 bo={} mode={}", boCode, mode);
        return Collections.emptyList();
    }

    /**
     * 通过宿主主数据源（业务库）JDBC 采集表元数据。
     *
     * <p>schema-preview 的唯一采集路径。
     * {@link NativeBoSchemaCollector} 注入的是宿主无 qualifier 的主 {@link DataSource}，
     * 即业务库而非权限引擎专属库。
     */
    private List<BoSchemaColumnInfo> collectFromBusinessDb(String tableName, String boCode) {
        if (nativeBoSchemaCollector == null || !StringUtils.hasText(tableName)) {
            log.debug("[previewBoSchema] NativeBoSchemaCollector 未注入或表名为空 bo={}", boCode);
            return Collections.emptyList();
        }
        List<BoSchemaColumnInfo> columns = nativeBoSchemaCollector.fetchColumns(tableName);
        if (columns == null) {
            return Collections.emptyList();
        }
        log.info("[previewBoSchema] JDBC 采集完成 bo={} table={} columns={}", boCode, tableName, columns.size());
        return columns;
    }
}