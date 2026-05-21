package com.ruijie.authzengine.infrastructure.persistence;

import com.ruijie.authzengine.domain.model.ops.AuthzAuditRecord;
import com.ruijie.authzengine.domain.repository.AuthzAuditWriteRepository;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysAuthzAuditLogEntity;
import com.ruijie.authzengine.infrastructure.persistence.service.SysAuthzAuditLogPersistenceService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 鉴权审计写入仓储实现。
 * <p>审计日志写入具备幂等性：当 requestId 已存在时不重复写入，防止重试/重放导致重复记录。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DatabaseAuthzAuditWriteRepository implements AuthzAuditWriteRepository {

    private final SysAuthzAuditLogPersistenceService sysAuthzAuditLogPersistenceService;

    /**
     * 幂等写入鉴权审计记录。
     * <p>若同一 requestId 已存在则直接返回已有记录，不重复写入。</p>
     */
    @Override
    public AuthzAuditRecord save(AuthzAuditRecord authzAuditRecord) {
        // 幂等性检查：同一 requestId 不重复记录
        SysAuthzAuditLogEntity existingEntity = findByRequestId(
            authzAuditRecord.getTenantId(),
            authzAuditRecord.getAppCode(),
            authzAuditRecord.getRequestId()
        );
        if (existingEntity != null) {
            log.debug("[审计写入] requestId 已存在，跳过写入: requestId={}, auditLogId={}",
                authzAuditRecord.getRequestId(), existingEntity.getId());
            return toDefinition(existingEntity);
        }
        log.debug("[审计写入] 写入审计记录: tenantId={}, appCode={}, subjectId={}, requestId={}, decision={}",
            authzAuditRecord.getTenantId(), authzAuditRecord.getAppCode(),
            authzAuditRecord.getSubjectId(), authzAuditRecord.getRequestId(), authzAuditRecord.getDecision());
        SysAuthzAuditLogEntity entity = toEntity(authzAuditRecord);
        sysAuthzAuditLogPersistenceService.save(entity);
        // 部分数据库方言 save 后 id 可能不回写，再次查询确保得到持久化后的 id
        if (entity.getId() == null && StringUtils.hasText(entity.getRequestId())) {
            SysAuthzAuditLogEntity persistedEntity = findByRequestId(entity.getTenantId(), entity.getAppCode(), entity.getRequestId());
            if (persistedEntity != null) {
                entity = persistedEntity;
            }
        }
        log.debug("[审计写入] 审计记录写入完成: auditLogId={}", entity.getId());
        return toDefinition(entity);
    }

    private SysAuthzAuditLogEntity toEntity(AuthzAuditRecord authzAuditRecord) {
        SysAuthzAuditLogEntity entity = new SysAuthzAuditLogEntity();
        entity.setTenantId(authzAuditRecord.getTenantId());
        entity.setAppCode(authzAuditRecord.getAppCode());
        entity.setRequestId(authzAuditRecord.getRequestId());
        entity.setSubjectModel(authzAuditRecord.getSubjectModel());
        entity.setSubjectId(authzAuditRecord.getSubjectId());
        entity.setResourceModel(authzAuditRecord.getResourceModel());
        entity.setResId(authzAuditRecord.getResId());
        entity.setActionCode(authzAuditRecord.getActionCode());
        entity.setDecision(authzAuditRecord.getDecision());
        entity.setMatchedPermissionCodes(join(authzAuditRecord.getMatchedPermissionCodes()));
        entity.setMatchedAssignmentIds(join(authzAuditRecord.getMatchedAssignmentIds()));
        entity.setMatchedDelegateIds(join(authzAuditRecord.getMatchedDelegateIds()));
        entity.setMatchedPolicyTemplateCodes(join(authzAuditRecord.getMatchedPolicyTemplateCodes()));
        entity.setFailureReason(authzAuditRecord.getFailureReason());
        entity.setCostMs(authzAuditRecord.getCostMs());
        entity.setHookStatus(authzAuditRecord.getHookStatus());
        entity.setHookCostMs(authzAuditRecord.getHookCostMs());
        entity.setAttributeSnapshot(authzAuditRecord.getAttributeSnapshot());
        return entity;
    }

    private AuthzAuditRecord toDefinition(SysAuthzAuditLogEntity entity) {
        return AuthzAuditRecord.builder()
            .auditLogId(entity.getId())
            .requestId(entity.getRequestId())
            .tenantId(entity.getTenantId())
            .appCode(entity.getAppCode())
            .subjectModel(entity.getSubjectModel())
            .subjectId(entity.getSubjectId())
            .resourceModel(entity.getResourceModel())
            .resId(entity.getResId())
            .actionCode(entity.getActionCode())
            .decision(entity.getDecision())
            .matchedPermissionCodes(split(entity.getMatchedPermissionCodes()))
            .matchedAssignmentIds(split(entity.getMatchedAssignmentIds()))
            .matchedDelegateIds(split(entity.getMatchedDelegateIds()))
            .matchedPolicyTemplateCodes(split(entity.getMatchedPolicyTemplateCodes()))
            .failureReason(entity.getFailureReason())
            .costMs(entity.getCostMs())
            .hookStatus(entity.getHookStatus())
            .hookCostMs(entity.getHookCostMs())
            .attributeSnapshot(entity.getAttributeSnapshot())
            .build();
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream().filter(StringUtils::hasText).collect(Collectors.joining(","));
    }

    private SysAuthzAuditLogEntity findByRequestId(String tenantId, String appCode, String requestId) {
        if (!StringUtils.hasText(requestId)) {
            return null;
        }
        return sysAuthzAuditLogPersistenceService.lambdaQuery()
            .eq(SysAuthzAuditLogEntity::getTenantId, tenantId)
            .eq(SysAuthzAuditLogEntity::getAppCode, appCode)
            .eq(SysAuthzAuditLogEntity::getRequestId, requestId)
            .one();
    }

    private List<String> split(String values) {
        if (!StringUtils.hasText(values)) {
            return java.util.Collections.emptyList();
        }
        return java.util.Arrays.stream(values.split(","))
            .filter(StringUtils::hasText)
            .collect(Collectors.toList());
    }
}