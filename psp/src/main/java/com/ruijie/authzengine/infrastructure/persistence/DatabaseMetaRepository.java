package com.ruijie.authzengine.infrastructure.persistence;

import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.StandardActionDefinition;
import com.ruijie.authzengine.domain.model.governance.StandardPolicyTemplateDefinition;
import com.ruijie.authzengine.domain.repository.MetaRepository;
import com.ruijie.authzengine.infrastructure.persistence.entity.AuthMetaModelEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.AuthPermissionItemEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.BoMetaModelEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.StandardActionEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.StandardPolicyTemplateEntity;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysAuthAssignmentEntity;
import com.ruijie.authzengine.infrastructure.persistence.service.AuthMetaModelPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.AuthPermissionItemPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.BoMetaModelPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.mapper.BoMetaModelMapper;
import com.ruijie.authzengine.infrastructure.persistence.service.StandardActionPersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.StandardPolicyTemplatePersistenceService;
import com.ruijie.authzengine.infrastructure.persistence.service.SysAuthAssignmentPersistenceService;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 治理元模型与标准库仓储实现。
 * <p>负责鉴权元模型（AuthMetaModel）、业务对象元模型（BoMetaModel）、
 * 标准动作（StandardAction）、标准策略模板（StandardPolicyTemplate）的 CRUD。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DatabaseMetaRepository implements MetaRepository {

    private final AuthMetaModelPersistenceService authMetaModelPersistenceService;

    private final BoMetaModelPersistenceService boMetaModelPersistenceService;

    private final BoMetaModelMapper boMetaModelMapper;

    private final StandardActionPersistenceService standardActionPersistenceService;

    private final StandardPolicyTemplatePersistenceService standardPolicyTemplatePersistenceService;

    private final AuthPermissionItemPersistenceService authPermissionItemPersistenceService;

    private final SysAuthAssignmentPersistenceService sysAuthAssignmentPersistenceService;

    @Override
    public Map<String, List<String>> listDistinctTenantApps() {
        return authMetaModelPersistenceService.lambdaQuery()
            .select(AuthMetaModelEntity::getTenantId, AuthMetaModelEntity::getAppCode)
            .groupBy(AuthMetaModelEntity::getTenantId, AuthMetaModelEntity::getAppCode)
            .list()
            .stream()
            .collect(Collectors.groupingBy(
                AuthMetaModelEntity::getTenantId,
                LinkedHashMap::new,
                Collectors.mapping(AuthMetaModelEntity::getAppCode, Collectors.toList())
            ));
    }

    /**
     * 保存鉴权元模型：已存在则更新，不存在则新建。
     * <p>唯一键为 (tenantId, appCode, modelCode)。</p>
     */
    @Override
    public AuthMetaModelDefinition saveAuthMetaModel(AuthMetaModelDefinition definition) {
        AuthMetaModelEntity existing = authMetaModelPersistenceService.lambdaQuery()
            .eq(AuthMetaModelEntity::getTenantId, definition.getTenantId())
            .eq(AuthMetaModelEntity::getAppCode, definition.getAppCode())
            .eq(AuthMetaModelEntity::getModelCode, definition.getModelCode())
            .one();
        AuthMetaModelEntity entity = toEntity(definition);
        boolean isUpdate = existing != null;
        if (isUpdate) {
            entity.setId(existing.getId());
        }
        log.info("[元模型仓储] {}鉴权元模型: tenantId={}, appCode={}, modelCode={}",
            isUpdate ? "更新" : "新增",
            definition.getTenantId(), definition.getAppCode(), definition.getModelCode());
        authMetaModelPersistenceService.saveOrUpdate(entity);
        return toDefinition(entity);
    }

    @Override
    public PageResult<AuthMetaModelDefinition> pageAuthMetaModels(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        List<AuthMetaModelDefinition> records = authMetaModelPersistenceService.lambdaQuery()
            .eq(AuthMetaModelEntity::getTenantId, tenantId)
            .eq(AuthMetaModelEntity::getAppCode, appCode)
            .orderByAsc(AuthMetaModelEntity::getModelCode)
            .list()
            .stream()
            .map(this::toDefinition)
            .filter(item -> matchesKeyword(keyword, item.getModelCode(), item.getModelName(), item.getCategory()))
            .collect(Collectors.toList());
        return buildPage(records, pageNo, pageSize);
    }

    @Override
    public AuthMetaModelDefinition findAuthMetaModel(String tenantId, String appCode, String modelCode) {
        AuthMetaModelEntity entity = authMetaModelPersistenceService.lambdaQuery()
            .eq(AuthMetaModelEntity::getTenantId, tenantId)
            .eq(AuthMetaModelEntity::getAppCode, appCode)
            .eq(AuthMetaModelEntity::getModelCode, modelCode)
            .one();
        return entity != null ? toDefinition(entity) : null;
    }

    /**
     * 删除鉴权元模型。
     */
    @Override
    public void deleteAuthMetaModel(String tenantId, String appCode, String modelCode) {
        log.info("[元模型仓储] 删除鉴权元模型: tenantId={}, appCode={}, modelCode={}", tenantId, appCode, modelCode);
        authMetaModelPersistenceService.lambdaUpdate()
            .eq(AuthMetaModelEntity::getTenantId, tenantId)
            .eq(AuthMetaModelEntity::getAppCode, appCode)
            .eq(AuthMetaModelEntity::getModelCode, modelCode)
            .remove();
    }

    @Override
    public boolean hasAuthMetaModelReference(String tenantId, String appCode, String modelCode) {
        return authPermissionItemPersistenceService.lambdaQuery()
            .eq(AuthPermissionItemEntity::getTenantId, tenantId)
            .eq(AuthPermissionItemEntity::getAppCode, appCode)
            .eq(AuthPermissionItemEntity::getResModelCode, modelCode)
            .count() > 0;
    }

    /**
     * 保存业务对象元模型 (BoMetaModel)。
     * <p>唯一键为 (tenantId, appCode, boCode)。当同编码记录已软删除时执行恢复，避免唯一约束冲突。</p>
     */
    @Override
    public BoMetaModelDefinition saveBoMetaModel(BoMetaModelDefinition definition) {
        BoMetaModelEntity existing = boMetaModelPersistenceService.lambdaQuery()
            .eq(BoMetaModelEntity::getTenantId, definition.getTenantId())
            .eq(BoMetaModelEntity::getAppCode, definition.getAppCode())
            .eq(BoMetaModelEntity::getBoCode, definition.getBoCode())
            .one();
        BoMetaModelEntity entity = toEntity(definition);
        boolean isUpdate = existing != null;
        boolean isRestore = false;
        if (isUpdate) {
            entity.setId(existing.getId());
        } else {
            // 检查是否存在同编码的软删除记录，若有则恢复而非新增，避免唯一约束冲突
            BoMetaModelEntity deleted = boMetaModelMapper.findDeletedByCode(
                definition.getTenantId(), definition.getAppCode(), definition.getBoCode());
            if (deleted != null) {
                entity.setId(deleted.getId());
                boMetaModelMapper.reviveById(entity);
                isRestore = true;
            }
        }
        log.info("[元模型仓储] {}BO元模型: tenantId={}, appCode={}, boCode={}",
            isUpdate ? "更新" : (isRestore ? "恢复" : "新增"),
            definition.getTenantId(), definition.getAppCode(), definition.getBoCode());
        if (!isRestore) {
            boMetaModelPersistenceService.saveOrUpdate(entity);
        }
        return toDefinition(entity);
    }

    @Override
    public PageResult<BoMetaModelDefinition> pageBoMetaModels(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        List<BoMetaModelDefinition> records = boMetaModelPersistenceService.lambdaQuery()
            .eq(BoMetaModelEntity::getTenantId, tenantId)
            .eq(BoMetaModelEntity::getAppCode, appCode)
            .orderByAsc(BoMetaModelEntity::getBoCode)
            .list()
            .stream()
            .map(this::toDefinition)
            .filter(item -> matchesKeyword(keyword, item.getBoCode(), item.getBoName()))
            .collect(Collectors.toList());
        return buildPage(records, pageNo, pageSize);
    }

    @Override
    public BoMetaModelDefinition findBoMetaModel(String tenantId, String appCode, String boCode) {
        BoMetaModelEntity entity = boMetaModelPersistenceService.lambdaQuery()
            .eq(BoMetaModelEntity::getTenantId, tenantId)
            .eq(BoMetaModelEntity::getAppCode, appCode)
            .eq(BoMetaModelEntity::getBoCode, boCode)
            .one();
        return entity != null ? toDefinition(entity) : null;
    }

    @Override
    public BoMetaModelDefinition findBoMetaModelById(String tenantId, String appCode, Long boId) {
        if (boId == null) {
            return null;
        }
        BoMetaModelEntity entity = boMetaModelPersistenceService.lambdaQuery()
            .eq(BoMetaModelEntity::getTenantId, tenantId)
            .eq(BoMetaModelEntity::getAppCode, appCode)
            .eq(BoMetaModelEntity::getId, boId)
            .one();
        return entity != null ? toDefinition(entity) : null;
    }

    /**
     * 删除 BO 元模型。
     */
    @Override
    public void deleteBoMetaModel(String tenantId, String appCode, String boCode) {
        log.info("[元模型仓储] 删除BO元模型: tenantId={}, appCode={}, boCode={}", tenantId, appCode, boCode);
        boMetaModelPersistenceService.lambdaUpdate()
            .eq(BoMetaModelEntity::getTenantId, tenantId)
            .eq(BoMetaModelEntity::getAppCode, appCode)
            .eq(BoMetaModelEntity::getBoCode, boCode)
            .remove();
    }

    @Override
    public boolean hasBoMetaModelReference(String tenantId, String appCode, String boCode) {
        // 业务对象暂无直接外键引用
        return false;
    }

    @Override
    public List<StandardActionDefinition> listStandardActions(String tenantId) {
        return standardActionPersistenceService.lambdaQuery()
            .in(StandardActionEntity::getTenantId, Arrays.asList("__GLOBAL__", tenantId))
            .orderByAsc(StandardActionEntity::getTenantId)
            .orderByAsc(StandardActionEntity::getActCode)
            .list()
            .stream()
            .map(this::toDefinition)
            .collect(Collectors.toList());
    }

    @Override
    public PageResult<StandardActionDefinition> pageStandardActions(String tenantId, String keyword, int pageNo, int pageSize) {
        List<StandardActionDefinition> records = standardActionPersistenceService.lambdaQuery()
            .in(StandardActionEntity::getTenantId, Arrays.asList("__GLOBAL__", tenantId))
            .orderByAsc(StandardActionEntity::getTenantId)
            .orderByAsc(StandardActionEntity::getActCode)
            .list()
            .stream()
            .map(this::toDefinition)
            .filter(item -> matchesKeyword(keyword, item.getActCode(), item.getActName(), item.getActType()))
            .collect(Collectors.toList());
        return buildPage(records, pageNo, pageSize);
    }

    @Override
    public StandardActionDefinition findStandardAction(String tenantId, String actCode) {
        StandardActionEntity entity = standardActionPersistenceService.lambdaQuery()
            .in(StandardActionEntity::getTenantId, Arrays.asList("__GLOBAL__", tenantId))
            .eq(StandardActionEntity::getActCode, actCode)
            .one();
        return entity != null ? toDefinition(entity) : null;
    }

    /**
     * 保存标准动作定义。
     * <p>唯一键为 (tenantId, actCode)，不包含 appCode。</p>
     */
    @Override
    public StandardActionDefinition saveStandardAction(StandardActionDefinition definition) {
        StandardActionEntity existing = standardActionPersistenceService.lambdaQuery()
            .eq(StandardActionEntity::getTenantId, definition.getTenantId())
            .eq(StandardActionEntity::getActCode, definition.getActCode())
            .one();
        StandardActionEntity entity = toEntity(definition);
        boolean isUpdate = existing != null;
        if (isUpdate) {
            entity.setId(existing.getId());
        }
        log.info("[元模型仓储] {}标准动作: tenantId={}, actCode={}",
            isUpdate ? "更新" : "新增",
            definition.getTenantId(), definition.getActCode());
        standardActionPersistenceService.saveOrUpdate(entity);
        return toDefinition(entity);
    }

    @Override
    public void deleteStandardAction(String tenantId, String actCode) {
        standardActionPersistenceService.lambdaUpdate()
            .eq(StandardActionEntity::getTenantId, tenantId)
            .eq(StandardActionEntity::getActCode, actCode)
            .remove();
    }

    @Override
    public boolean hasStandardActionReference(String tenantId, String actCode) {
        return authPermissionItemPersistenceService.lambdaQuery()
            .eq(AuthPermissionItemEntity::getTenantId, tenantId)
            .eq(AuthPermissionItemEntity::getActCode, actCode)
            .count() > 0;
    }

    @Override
    public List<StandardPolicyTemplateDefinition> listStandardPolicyTemplates(String tenantId) {
        return standardPolicyTemplatePersistenceService.lambdaQuery()
            .in(StandardPolicyTemplateEntity::getTenantId, Arrays.asList("__GLOBAL__", tenantId))
            .orderByAsc(StandardPolicyTemplateEntity::getTenantId)
            .orderByAsc(StandardPolicyTemplateEntity::getTemplateCode)
            .list()
            .stream()
            .map(this::toDefinition)
            .collect(Collectors.toList());
    }

    @Override
    public PageResult<StandardPolicyTemplateDefinition> pageStandardPolicyTemplates(String tenantId, String keyword, int pageNo, int pageSize) {
        return pageStandardPolicyTemplates(tenantId, keyword, null, pageNo, pageSize);
    }

    @Override
    public PageResult<StandardPolicyTemplateDefinition> pageStandardPolicyTemplates(
        String tenantId,
        String keyword,
        String polType,
        int pageNo,
        int pageSize
    ) {
        List<StandardPolicyTemplateDefinition> records = standardPolicyTemplatePersistenceService.lambdaQuery()
            .in(StandardPolicyTemplateEntity::getTenantId, Arrays.asList("__GLOBAL__", tenantId))
            .orderByAsc(StandardPolicyTemplateEntity::getTenantId)
            .orderByAsc(StandardPolicyTemplateEntity::getTemplateCode)
            .list()
            .stream()
            .map(this::toDefinition)
            .filter(item -> matchesKeyword(keyword, item.getTemplateCode(), item.getTemplateName(), item.getPolType()))
            .filter(item -> matchesPolicyType(polType, item.getPolType()))
            .collect(Collectors.toList());
        return buildPage(records, pageNo, pageSize);
    }

    @Override
    public StandardPolicyTemplateDefinition findStandardPolicyTemplate(String tenantId, String templateCode) {
        StandardPolicyTemplateEntity entity = standardPolicyTemplatePersistenceService.lambdaQuery()
            .in(StandardPolicyTemplateEntity::getTenantId, Arrays.asList("__GLOBAL__", tenantId))
            .eq(StandardPolicyTemplateEntity::getTemplateCode, templateCode)
            .one();
        return entity != null ? toDefinition(entity) : null;
    }

    /**
     * 保存标准策略模板。
     * <p>唯一键为 (tenantId, templateCode)。</p>
     */
    @Override
    public StandardPolicyTemplateDefinition saveStandardPolicyTemplate(StandardPolicyTemplateDefinition definition) {
        StandardPolicyTemplateEntity existing = standardPolicyTemplatePersistenceService.lambdaQuery()
            .eq(StandardPolicyTemplateEntity::getTenantId, definition.getTenantId())
            .eq(StandardPolicyTemplateEntity::getTemplateCode, definition.getTemplateCode())
            .one();
        StandardPolicyTemplateEntity entity = toEntity(definition);
        boolean isUpdate = existing != null;
        if (isUpdate) {
            entity.setId(existing.getId());
        }
        log.info("[元模型仓储] {}策略模板: tenantId={}, templateCode={}",
            isUpdate ? "更新" : "新增",
            definition.getTenantId(), definition.getTemplateCode());
        standardPolicyTemplatePersistenceService.saveOrUpdate(entity);
        return toDefinition(entity);
    }

    @Override
    public void deleteStandardPolicyTemplate(String tenantId, String templateCode) {
        standardPolicyTemplatePersistenceService.lambdaUpdate()
            .eq(StandardPolicyTemplateEntity::getTenantId, tenantId)
            .eq(StandardPolicyTemplateEntity::getTemplateCode, templateCode)
            .remove();
    }

    @Override
    public boolean hasStandardPolicyTemplateReference(String tenantId, String templateCode) {
        StandardPolicyTemplateEntity entity = standardPolicyTemplatePersistenceService.lambdaQuery()
            .eq(StandardPolicyTemplateEntity::getTenantId, tenantId)
            .eq(StandardPolicyTemplateEntity::getTemplateCode, templateCode)
            .one();
        if (entity == null) {
            return false;
        }
        return sysAuthAssignmentPersistenceService.lambdaQuery()
            .eq(SysAuthAssignmentEntity::getPolicyTplId, entity.getId())
            .count() > 0;
    }

    private AuthMetaModelEntity toEntity(AuthMetaModelDefinition definition) {
        AuthMetaModelEntity entity = new AuthMetaModelEntity();
        entity.setTenantId(definition.getTenantId());
        entity.setAppCode(definition.getAppCode());
        entity.setModelCode(definition.getModelCode());
        entity.setModelName(StringUtils.hasText(definition.getModelName())
            ? definition.getModelName()
            : definition.getModelCode());
        entity.setCategory(definition.getCategory());
        entity.setAdapterType(definition.getAdapterType());
        entity.setResolver(definition.getResolver());
        entity.setSchemaView(definition.getSchemaView());
        return entity;
    }

    private AuthMetaModelDefinition toDefinition(AuthMetaModelEntity entity) {
        return AuthMetaModelDefinition.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .appCode(entity.getAppCode())
            .modelCode(entity.getModelCode())
            .modelName(entity.getModelName())
            .category(entity.getCategory())
            .adapterType(entity.getAdapterType())
            .resolver(entity.getResolver())
            .schemaView(entity.getSchemaView())
            .build();
    }

    private BoMetaModelEntity toEntity(BoMetaModelDefinition definition) {
        BoMetaModelEntity entity = new BoMetaModelEntity();
        entity.setTenantId(definition.getTenantId());
        entity.setAppCode(definition.getAppCode());
        entity.setBoCode(definition.getBoCode());
        entity.setBoName(definition.getBoName());
        entity.setSchemaJson(definition.getSchemaJson());
        entity.setAdapterType(definition.getAdapterType());
        entity.setResolver(definition.getResolver());
        return entity;
    }

    private BoMetaModelDefinition toDefinition(BoMetaModelEntity entity) {
        return BoMetaModelDefinition.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .appCode(entity.getAppCode())
            .boCode(entity.getBoCode())
            .boName(entity.getBoName())
            .schemaJson(entity.getSchemaJson())
            .adapterType(entity.getAdapterType())
            .resolver(entity.getResolver())
            .build();
    }

    private StandardActionDefinition toDefinition(StandardActionEntity entity) {
        return StandardActionDefinition.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .actCode(entity.getActCode())
            .actName(entity.getActName())
            .actType(entity.getActType())
            .resCategory(entity.getResCategory())
            .riskLevel(entity.getRiskLevel())
            .build();
    }

    private StandardPolicyTemplateDefinition toDefinition(StandardPolicyTemplateEntity entity) {
        return StandardPolicyTemplateDefinition.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .templateCode(entity.getTemplateCode())
            .templateName(entity.getTemplateName())
            .polType(entity.getPolType())
            .expressionScript(entity.getExpressionScript())
            .paramSchema(entity.getParamSchema())
            .status(entity.getStatus())
            .build();
    }

    private StandardActionEntity toEntity(StandardActionDefinition definition) {
        StandardActionEntity entity = new StandardActionEntity();
        entity.setTenantId(definition.getTenantId());
        entity.setActCode(definition.getActCode());
        entity.setActName(definition.getActName());
        entity.setActType(definition.getActType());
        entity.setResCategory(definition.getResCategory());
        entity.setRiskLevel(definition.getRiskLevel());
        return entity;
    }

    private StandardPolicyTemplateEntity toEntity(StandardPolicyTemplateDefinition definition) {
        StandardPolicyTemplateEntity entity = new StandardPolicyTemplateEntity();
        entity.setTenantId(definition.getTenantId());
        entity.setTemplateCode(definition.getTemplateCode());
        entity.setTemplateName(definition.getTemplateName());
        entity.setPolType(definition.getPolType());
        entity.setExpressionScript(definition.getExpressionScript());
        entity.setParamSchema(definition.getParamSchema());
        entity.setStatus(definition.getStatus());
        return entity;
    }

    private <T> PageResult<T> buildPage(List<T> records, int pageNo, int pageSize) {
        List<T> safeRecords = records == null ? Collections.<T>emptyList() : records;
        int safePageNo = pageNo > 0 ? pageNo : 1;
        int safePageSize = pageSize > 0 ? pageSize : 20;
        int fromIndex = Math.min((safePageNo - 1) * safePageSize, safeRecords.size());
        int toIndex = Math.min(fromIndex + safePageSize, safeRecords.size());
        return PageResult.<T>builder()
            .pageNo(safePageNo)
            .pageSize(safePageSize)
            .total(safeRecords.size())
            .records(safeRecords.subList(fromIndex, toIndex))
            .build();
    }

    private boolean matchesKeyword(String keyword, String... values) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ROOT).contains(normalizedKeyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesPolicyType(String polType, String actualPolType) {
        if (!StringUtils.hasText(polType)) {
            return true;
        }
        return polType.trim().equalsIgnoreCase(String.valueOf(actualPolType));
    }
}