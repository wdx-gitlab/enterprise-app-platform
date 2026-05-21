package com.ruijie.authzengine.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.StandardPolicyTemplateDefinition;
import com.ruijie.authzengine.domain.model.governance.assignment.SysAuthAssignment;
import com.ruijie.authzengine.domain.model.governance.permission.AuthPermissionItem;
import com.ruijie.authzengine.domain.repository.AssignmentRepository;
import com.ruijie.authzengine.domain.repository.MetaRepository;
import com.ruijie.authzengine.domain.repository.PermissionRepository;
import com.ruijie.authzengine.shared.exception.BusinessException;
import com.ruijie.authzengine.shared.exception.ErrorCode;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 授权分配治理应用服务。
 * <p>职责：分配的 CRUD 编排，包括策略模板编码解析和变更约束校验（主体或权限项变更必须删除重建）。</p>
 */
@Slf4j
@Service
public class AssignmentAppService {

    private static final String FIELD_ACTION_MASK = "MASK";

    private static final String RES_DATA_BO = "RES_DATA_BO";

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

    private final AssignmentRepository assignmentRepository;

    private final MetaRepository metaRepository;

    private final PermissionRepository permissionRepository;

    private final PermissionCodeService permissionCodeService;

    @Autowired
    public AssignmentAppService(
        AssignmentRepository assignmentRepository,
        MetaRepository metaRepository,
        PermissionRepository permissionRepository,
        PermissionCodeService permissionCodeService
    ) {
        this.assignmentRepository = assignmentRepository;
        this.metaRepository = metaRepository;
        this.permissionRepository = permissionRepository;
        this.permissionCodeService = permissionCodeService;
    }

    public AssignmentAppService(
        AssignmentRepository assignmentRepository,
        MetaRepository metaRepository,
        PermissionRepository permissionRepository
    ) {
        this(
            assignmentRepository,
            metaRepository,
            permissionRepository,
            new PermissionCodeService(metaRepository, new com.ruijie.authzengine.domain.repository.ResourceRepository() {
            })
        );
    }

    AssignmentAppService(AssignmentRepository assignmentRepository, MetaRepository metaRepository) {
        this(
            assignmentRepository,
            metaRepository,
            NO_OP_PERMISSION_REPOSITORY,
            new PermissionCodeService(metaRepository, new com.ruijie.authzengine.domain.repository.ResourceRepository() {
            })
        );
    }

    public PageResult<SysAuthAssignment> pageAssignments(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        return assignmentRepository.pageAssignments(tenantId, appCode, keyword, pageNo, pageSize);
    }

    public SysAuthAssignment getAssignment(String tenantId, String appCode, Long assignmentId) {
        SysAuthAssignment assignment = assignmentRepository.findAssignment(tenantId, appCode, assignmentId);
        if (assignment == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "授权分配记录不存在");
        }
        return assignment;
    }

    /**
     * 创建授权分配。
     * <p>若指定了 policyTemplateCode，则先解析为 policyTplId 再落库；permItemId 为必填。</p>
     */
    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public SysAuthAssignment createAssignment(String policyTemplateCode, SysAuthAssignment assignment) {
        if (assignment.getPermItemId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "权限项主键 permItemId 不能为空");
        }
        StandardPolicyTemplateDefinition policyTemplate = resolvePolicyTemplate(assignment.getTenantId(), policyTemplateCode, assignment);
        validateFieldPolicyBinding(policyTemplateCode, assignment, policyTemplate);
        log.info("[分配服务] 创建授权分配: tenantId={}, appCode={}, subjectModel={}, subjectId={}, permItemId={}, policyTplCode={}",
            assignment.getTenantId(), assignment.getAppCode(),
            assignment.getSubjectModel(), assignment.getSubjectId(),
            assignment.getPermItemId(), policyTemplateCode);
        SysAuthAssignment saved = assignmentRepository.saveAssignment(assignment);
        log.debug("[分配服务] 授权分配创建成功: assignmentId={}", saved.getId());
        return saved;
    }

    /**
     * 更新授权分配。
     * <p>主体（subjectModel/subjectId）或权限项（permItemId）变更不允许原地更新，必须先删除再重建，
     * 避免引用关系被隐式破坏。</p>
     */
    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public SysAuthAssignment updateAssignment(String tenantId, String appCode, Long assignmentId, String policyTemplateCode, SysAuthAssignment assignment) {
        SysAuthAssignment existing = getAssignment(tenantId, appCode, assignmentId);
        Long targetPermItemId = assignment.getPermItemId();
        if (targetPermItemId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "权限项主键 permItemId 不能为空");
        }
        // 主体或权限项发生变更时强制要求删除重建，保证关联链路的一致性
        if (!existing.getSubjectModel().equals(assignment.getSubjectModel())
            || !existing.getSubjectId().equals(assignment.getSubjectId())
            || !existing.getPermItemId().equals(targetPermItemId)) {
            throw new BusinessException(ErrorCode.RELATION_RECREATE_REQUIRED, "主体或权限项变更必须删除后重建");
        }
        assignment.setId(existing.getId());
        assignment.setTenantId(tenantId);
        assignment.setAppCode(appCode);
        assignment.setPermItemId(targetPermItemId);
        StandardPolicyTemplateDefinition policyTemplate = resolvePolicyTemplate(tenantId, policyTemplateCode, assignment);
        validateFieldPolicyBinding(policyTemplateCode, assignment, policyTemplate);
        log.info("[分配服务] 更新授权分配: tenantId={}, appCode={}, assignmentId={}, policyTplCode={}",
            tenantId, appCode, assignmentId, policyTemplateCode);
        return assignmentRepository.saveAssignment(assignment);
    }

    /**
     * 删除授权分配。
     * <p>删除前先确认记录存在，再检查是否被外部引用（hasAssignmentReference），防止误删引用中的分配。</p>
     */
    @Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
    public void deleteAssignment(String tenantId, String appCode, Long assignmentId) {
        getAssignment(tenantId, appCode, assignmentId);
        if (assignmentRepository.hasAssignmentReference(tenantId, appCode, assignmentId)) {
            throw new BusinessException(ErrorCode.CONTROLLED_DELETE_CONFLICT, "授权分配仍被引用，禁止删除");
        }
        log.info("[分配服务] 删除授权分配: tenantId={}, appCode={}, assignmentId={}", tenantId, appCode, assignmentId);
        assignmentRepository.deleteAssignment(tenantId, appCode, assignmentId);
    }

    /**
     * 根据策略模板编码解析并设置 policyTplId，为空时跳过。
     */
    private StandardPolicyTemplateDefinition resolvePolicyTemplate(String tenantId, String policyTemplateCode, SysAuthAssignment assignment) {
        if (!StringUtils.hasText(policyTemplateCode)) {
            assignment.setPolicyTplId(null);
            return null;
        }
        StandardPolicyTemplateDefinition tpl = metaRepository.findStandardPolicyTemplate(tenantId, policyTemplateCode);
        if (tpl == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "策略模板不存在: " + policyTemplateCode);
        }
        assignment.setPolicyTplId(tpl.getId());
        return tpl;
    }

    private void validateFieldPolicyBinding(
        String policyTemplateCode,
        SysAuthAssignment assignment,
        StandardPolicyTemplateDefinition policyTemplate
    ) {
        if (policyTemplate == null || !"FIELD".equalsIgnoreCase(policyTemplate.getPolType())) {
            return;
        }
        Map<String, Object> policyParams = parsePolicyParams(assignment.getPolicyParams());
        String targetField = policyParams.get("targetField") == null ? null : String.valueOf(policyParams.get("targetField")).trim();
        if (!StringUtils.hasText(targetField)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "FIELD 策略绑定必须提供 policyParams.targetField");
        }
        AuthPermissionItem permissionItem = requirePermissionItem(assignment);
        if (!RES_DATA_BO.equalsIgnoreCase(permissionItem.getResModelCode())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "FIELD 策略只能绑定 RES_DATA_BO 权限项");
        }
        BoMetaModelDefinition boMetaModelDefinition = resolveBoMetaModel(assignment, permissionItem);
        FieldAttributeMetadata fieldAttributeMetadata = resolveFieldAttribute(boMetaModelDefinition, targetField);
        if (fieldAttributeMetadata == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "targetField 未指向已声明字段: " + targetField);
        }
        if (!fieldAttributeMetadata.fieldControl) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "targetField 未开启 fieldControl: " + targetField);
        }
        if (fieldAttributeMetadata.pk) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "主键字段不能配置 FIELD 策略: " + targetField);
        }
        String action = resolveFieldAction(policyTemplate);
        if (FIELD_ACTION_MASK.equals(action) && !"STRING".equalsIgnoreCase(fieldAttributeMetadata.type)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "MASK 策略只能绑定字符串字段: " + targetField);
        }
        if (FIELD_ACTION_MASK.equals(action)
            && FieldMaskScriptRules.isLegacyBrokenScript(policyParams.get("maskScript") == null
                ? null
                : String.valueOf(policyParams.get("maskScript")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "policyParams.maskScript 使用了已废弃的历史错误模板，请改用新的脱敏脚本");
        }
    }

    private AuthPermissionItem requirePermissionItem(SysAuthAssignment assignment) {
        List<AuthPermissionItem> permissionItems = permissionRepository.findPermissionItemsByIds(
            assignment.getTenantId(),
            assignment.getAppCode(),
            Collections.singletonList(assignment.getPermItemId())
        );
        if (permissionItems == null || permissionItems.isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "权限项不存在: " + assignment.getPermItemId());
        }
        return permissionItems.get(0);
    }

    private String resolveBoCode(AuthPermissionItem permissionItem) {
        if (!StringUtils.hasText(permissionItem.getPermCode())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "RES_DATA_BO 权限项缺少 permCode");
        }
        return permissionCodeService.resolveBoCodeFromPermissionCode(permissionItem.getPermCode());
    }

    private BoMetaModelDefinition resolveBoMetaModel(SysAuthAssignment assignment, AuthPermissionItem permissionItem) {
        Long boId = parseBoId(permissionItem.getResId());
        if (boId != null) {
            BoMetaModelDefinition boMetaModelDefinition = metaRepository.findBoMetaModelById(
                assignment.getTenantId(),
                assignment.getAppCode(),
                boId
            );
            if (boMetaModelDefinition != null) {
                return boMetaModelDefinition;
            }
            log.warn("[分配服务] FIELD 策略绑定按 BO 主键查找失败，回退 permCode 解析: tenantId={}, appCode={}, permItemId={}, boId={}",
                assignment.getTenantId(), assignment.getAppCode(), assignment.getPermItemId(), boId);
        }
        String boCode = resolveBoCode(permissionItem);
        BoMetaModelDefinition boMetaModelDefinition = metaRepository.findBoMetaModel(
            assignment.getTenantId(),
            assignment.getAppCode(),
            boCode
        );
        if (boMetaModelDefinition == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "业务对象元模型不存在: " + boCode);
        }
        return boMetaModelDefinition;
    }

    private Long parseBoId(String resId) {
        if (!StringUtils.hasText(resId)) {
            return null;
        }
        try {
            return Long.valueOf(resId.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Map<String, Object> parsePolicyParams(String policyParams) {
        if (!StringUtils.hasText(policyParams)) {
            return Collections.emptyMap();
        }
        try {
            return new ObjectMapper().readValue(policyParams, Map.class);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "policyParams 不是合法 JSON");
        }
    }

    private FieldAttributeMetadata resolveFieldAttribute(BoMetaModelDefinition boMetaModelDefinition, String targetField) {
        try {
            JsonNode root = new ObjectMapper().readTree(boMetaModelDefinition.getSchemaJson());
            JsonNode entitiesNode = root.path("entities");
            if (!entitiesNode.isArray()) {
                return null;
            }
            for (JsonNode entityNode : entitiesNode) {
                if (!entityNode.path("isPrimary").asBoolean(false)) {
                    continue;
                }
                JsonNode attributesNode = entityNode.path("attributes");
                if (!attributesNode.isArray()) {
                    return null;
                }
                for (JsonNode attributeNode : attributesNode) {
                    if (matchesField(attributeNode, targetField)) {
                        return new FieldAttributeMetadata(
                            attributeNode.path("type").asText(null),
                            attributeNode.path("fieldControl").asBoolean(false),
                            attributeNode.path("isPk").asBoolean(false)
                        );
                    }
                }
            }
            return null;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "业务对象 schemaJson 不是合法 JSON");
        }
    }

    private boolean matchesField(JsonNode attributeNode, String targetField) {
        return targetField.equals(attributeNode.path("code").asText())
            || targetField.equals(attributeNode.path("fieldName").asText())
            || targetField.equals(attributeNode.path("columnName").asText());
    }

    private String resolveFieldAction(StandardPolicyTemplateDefinition policyTemplate) {
        if (!StringUtils.hasText(policyTemplate.getParamSchema())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "FIELD 策略模板缺少 paramSchema");
        }
        try {
            JsonNode schemaRoot = new ObjectMapper().readTree(policyTemplate.getParamSchema());
            JsonNode actionNode = schemaRoot.get("action");
            if (actionNode != null && actionNode.isTextual()) {
                return actionNode.asText().trim().toUpperCase(Locale.ROOT);
            }
            JsonNode propertiesAction = schemaRoot.path("properties").path("action");
            if (propertiesAction.hasNonNull("const")) {
                return propertiesAction.get("const").asText().trim().toUpperCase(Locale.ROOT);
            }
            if (propertiesAction.hasNonNull("default")) {
                return propertiesAction.get("default").asText().trim().toUpperCase(Locale.ROOT);
            }
            JsonNode enumNode = propertiesAction.get("enum");
            if (enumNode != null && enumNode.isArray() && enumNode.size() > 0) {
                return enumNode.get(0).asText().trim().toUpperCase(Locale.ROOT);
            }
            throw new BusinessException(ErrorCode.BAD_REQUEST, "FIELD 策略模板缺少 action 定义: " + policyTemplateCodeOrName(policyTemplate));
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "FIELD 策略模板 paramSchema 不是合法 JSON");
        }
    }

    private String policyTemplateCodeOrName(StandardPolicyTemplateDefinition policyTemplate) {
        return StringUtils.hasText(policyTemplate.getTemplateCode())
            ? policyTemplate.getTemplateCode()
            : policyTemplate.getTemplateName();
    }

    private static final class FieldAttributeMetadata {

        private final String type;

        private final boolean fieldControl;

        private final boolean pk;

        private FieldAttributeMetadata(String type, boolean fieldControl, boolean pk) {
            this.type = type;
            this.fieldControl = fieldControl;
            this.pk = pk;
        }
    }
}
