package com.ruijie.authzengine.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.api.dto.request.AuthzResourceRequest;
import com.ruijie.authzengine.api.dto.request.BatchAuthzCheckRequest;
import com.ruijie.authzengine.api.dto.request.DataScopeResolveRequest;
import com.ruijie.authzengine.api.dto.response.BatchAuthzContractResponse;
import com.ruijie.authzengine.api.dto.response.DataScopeContractResponse;
import com.ruijie.authzengine.application.spi.BoMetaModelAdapter;
import com.ruijie.authzengine.application.spi.BoMetaModelRuntimeContext;
import com.ruijie.authzengine.application.spi.BoMetaModelRuntimeContextHolder;
import com.ruijie.authzengine.domain.model.common.CapabilityStatus;
import com.ruijie.authzengine.infrastructure.authz.BoResolverRouter;
import com.ruijie.authzengine.infrastructure.authz.BoSchemaJsonValidator;
import com.ruijie.authzengine.infrastructure.persistence.entity.BoMetaModelEntity;
import com.ruijie.authzengine.infrastructure.persistence.service.BoMetaModelPersistenceService;
import com.ruijie.authzengine.shared.exception.AuthzIntegrationException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 批量鉴权与 data-scope 占位应用服务。
 * <p>resolveDataScopeContract 负责将语义条件（semanticCondition）翻译为物理 SQL WHERE 片段，
 * 当任意前提条件不满足时提前返回 CONTRACT_ONLY 占位响应。</p>
 */
@Slf4j
@Service
public class AuthzContractAppService {

    private final BoMetaModelPersistenceService boMetaModelPersistenceService;

    private final BoResolverRouter boResolverRouter;

    private final BoSchemaJsonValidator boSchemaJsonValidator;

    @Autowired
    public AuthzContractAppService(
        BoMetaModelPersistenceService boMetaModelPersistenceService,
        BoResolverRouter boResolverRouter,
        BoSchemaJsonValidator boSchemaJsonValidator
    ) {
        this.boMetaModelPersistenceService = boMetaModelPersistenceService;
        this.boResolverRouter = boResolverRouter;
        this.boSchemaJsonValidator = boSchemaJsonValidator;
    }

    public AuthzContractAppService() {
        this(null, BoResolverRouter.noop(), new BoSchemaJsonValidator(new ObjectMapper()));
    }

    /**
     * 返回批量鉴权 CONTRACT_ONLY 占位结果。
     *
     * @param request 批量鉴权请求
     * @return 占位结果
     */
    public BatchAuthzContractResponse batchCheckContract(BatchAuthzCheckRequest request) {
        return BatchAuthzContractResponse.builder()
            .capabilityStatus(CapabilityStatus.CONTRACT_ONLY.name())
            .plannedScope("仅定义批量请求与统一响应结构，不包含执行算法")
            .results(Collections.emptyList())
            .build();
    }

    /**
     * 返回 data-scope 结果；当宿主已提供 BO Hook SQL 翻译能力时输出首版物理 SQL 片段。
     *
     * @param request data-scope 请求
     * @return data-scope 结果
     */
    public DataScopeContractResponse resolveDataScopeContract(DataScopeResolveRequest request) {
        // 前提检查 1：必须提供语义条件才能进行 SQL 翻译
        if (!StringUtils.hasText(request.getSemanticCondition())) {
            log.debug("[数据范围服务] 未提供 semanticCondition，返回 CONTRACT_ONLY: tenantId={}, appCode={}",
                request.getTenantId(), request.getAppCode());
            return contractOnlyResponse("未提供 semanticCondition，保持 CONTRACT_ONLY 占位模式");
        }
        // 前提检查 2：资源类型必须为 RES_DATA_BO
        if (!supportsBoSqlTranslation(request.getResource())) {
            log.debug("[数据范围服务] 资源类型不支持 SQL 翻译，返回 CONTRACT_ONLY: resourceModel={}",
                request.getResource() == null ? "null" : request.getResource().getResourceModel());
            return contractOnlyResponse("当前仅支持 RES_DATA_BO 业务对象的首版 SQL 翻译，其余场景保持 CONTRACT_ONLY");
        }
        // 前提检查 3：元模型持久化服务是否可用
        if (boMetaModelPersistenceService == null) {
            log.debug("[数据范围服务] BO元模型查询服务不可用，返回 CONTRACT_ONLY");
            return contractOnlyResponse("当前环境未启用业务对象元模型查询能力，保持 CONTRACT_ONLY");
        }
        // 前提检查 4：通过 resourceId（主键）查询 BO 元模型是否存在
        BoMetaModelEntity boMetaModelEntity = resolveBoMetaModel(request);
        if (boMetaModelEntity == null) {
            log.debug("[数据范围服务] 未找到BO元模型，返回 CONTRACT_ONLY: tenantId={}, appCode={}, resourceId={}",
                request.getTenantId(), request.getAppCode(), request.getResource().getResourceId());
            return contractOnlyResponse("未找到目标业务对象元模型，保持 CONTRACT_ONLY");
        }
        boSchemaJsonValidator.validateForRuntime(boMetaModelEntity.getSchemaJson());
        // 前提检查 5：解析 SQL 翻译 Hook 适配器
        BoMetaModelAdapter adapter = boResolverRouter.resolve(
            boMetaModelEntity.getAdapterType(),
            boMetaModelEntity.getResolver()
        );
        if (adapter == null) {
            log.debug("[数据范围服务] 未找到SQL翻译适配器，返回 CONTRACT_ONLY: boCode={}, resolver={}",
                boMetaModelEntity.getBoCode(), boMetaModelEntity.getResolver());
            return contractOnlyResponse("当前业务对象未配置 SQL 翻译 Hook，保持 CONTRACT_ONLY");
        }
        // SQL 翻译主流程
        String translatedSql = translateSemanticCondition(request, boMetaModelEntity, adapter);
        if (!StringUtils.hasText(translatedSql)) {
            log.debug("[数据范围服务] Hook 未返回 SQL 片段，返回 CONTRACT_ONLY: boCode={}",
                boMetaModelEntity.getBoCode());
            return contractOnlyResponse("当前 Hook 未实现 SQL 翻译能力，保持 CONTRACT_ONLY");
        }
        log.info("[数据范围服务] 语义条件翻译完成: tenantId={}, appCode={}, boCode={}",
            request.getTenantId(), request.getAppCode(), boMetaModelEntity.getBoCode());
        return DataScopeContractResponse.builder()
            .capabilityStatus(CapabilityStatus.AVAILABLE.name())
            .plannedScope("已通过 BO Hook 完成单条语义条件到物理 SQL 片段的翻译；策略模板聚合与多片段合并仍待后续迭代")
            .translatedSql(translatedSql)
            .scopeFragments(Collections.singletonList(buildSqlFragment(request, boMetaModelEntity, translatedSql)))
            .build();
    }

    private boolean supportsBoSqlTranslation(AuthzResourceRequest resource) {
        return resource != null
            && "RES_DATA_BO".equals(resource.getResourceModel())
            && StringUtils.hasText(resource.getResourceId());
    }

    /**
     * 通过 resourceId（主键）查询 BO 元模型。
     */
    private BoMetaModelEntity resolveBoMetaModel(DataScopeResolveRequest request) {
        String resourceId = request.getResource().getResourceId();
        if (!StringUtils.hasText(resourceId)) {
            return null;
        }
        try {
            Long boId = Long.parseLong(resourceId.trim());
            BoMetaModelEntity entity = boMetaModelPersistenceService.getById(boId);
            if (entity != null
                    && entity.getTenantId().equals(request.getTenantId())
                    && entity.getAppCode().equals(request.getAppCode())) {
                return entity;
            }
            return null;
        } catch (NumberFormatException e) {
            log.debug("[数据范围服务] resourceId={} 非数字主键", resourceId);
            return null;
        }
    }

    private String translateSemanticCondition(
        DataScopeResolveRequest request,
        BoMetaModelEntity boMetaModelEntity,
        BoMetaModelAdapter adapter
    ) {
        try {
            BoMetaModelRuntimeContextHolder.bind(BoMetaModelRuntimeContext.builder()
                .tenantId(request.getTenantId())
                .appCode(request.getAppCode())
                .boCode(boMetaModelEntity.getBoCode())
                .resolver(boMetaModelEntity.getResolver())
                .build());
            String translatedSql = adapter.translateToPhysicalSql(
                request.getSemanticCondition().trim(),
                boMetaModelEntity.getSchemaJson()
            );
            if (!StringUtils.hasText(translatedSql)) {
                return null;
            }
            validateSqlFragment(translatedSql);
            return translatedSql.trim();
        } catch (UnsupportedOperationException exception) {
            // Hook 声明不支持该翻译操作，正常降级
            log.debug("[数据范围服务] Hook 声明不支持翻译，降级为 CONTRACT_ONLY: boCode={}, resolver={}",
                boMetaModelEntity.getBoCode(), boMetaModelEntity.getResolver());
            return null;
        } catch (AuthzIntegrationException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("[数据范围服务] SQL翻译执行异常: boCode={}, resolver={}",
                boMetaModelEntity.getBoCode(), boMetaModelEntity.getResolver(), exception);
            throw new AuthzIntegrationException(
                "执行业务对象 SQL 翻译失败，resolver=" + boMetaModelEntity.getResolver(),
                exception
            );
        } finally {
            BoMetaModelRuntimeContextHolder.clear();
        }
    }

    private void validateSqlFragment(String translatedSql) {
        String normalized = translatedSql == null ? "" : translatedSql.trim().toLowerCase();
        if (normalized.contains(";")) {
            throw new AuthzIntegrationException("业务对象 Hook 返回的 SQL 片段不允许包含分号");
        }
        if (normalized.startsWith("select ")
            || normalized.startsWith("update ")
            || normalized.startsWith("delete ")
            || normalized.startsWith("insert ")) {
            throw new AuthzIntegrationException("业务对象 Hook 只允许返回 WHERE 片段，禁止返回完整 SQL 语句");
        }
    }

    private Map<String, Object> buildSqlFragment(
        DataScopeResolveRequest request,
        BoMetaModelEntity boMetaModelEntity,
        String translatedSql
    ) {
        Map<String, Object> fragment = new LinkedHashMap<>();
        fragment.put("type", "SQL_WHERE");
        fragment.put("policyTemplateCode", request.getPolicyTemplateCode());
        fragment.put("boCode", boMetaModelEntity.getBoCode());
        fragment.put("resolver", boMetaModelEntity.getResolver());
        fragment.put("semanticCondition", request.getSemanticCondition());
        fragment.put("translatedSql", translatedSql);
        return fragment;
    }

    private DataScopeContractResponse contractOnlyResponse(String plannedScope) {
        return DataScopeContractResponse.builder()
            .capabilityStatus(CapabilityStatus.CONTRACT_ONLY.name())
            .plannedScope(plannedScope)
            .translatedSql(null)
            .scopeFragments(Collections.emptyList())
            .build();
    }
}