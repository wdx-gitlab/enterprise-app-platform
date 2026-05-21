package com.ruijie.authzengine.infrastructure.persistence;

import com.ruijie.authzengine.domain.model.ops.AuthzAuditPage;
import com.ruijie.authzengine.domain.model.ops.AuthzAuditQuery;
import com.ruijie.authzengine.domain.model.ops.AuthzAuditRecord;
import com.ruijie.authzengine.domain.repository.AuthzAuditRepository;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysAuthzAuditLogEntity;
import com.ruijie.authzengine.infrastructure.persistence.service.SysAuthzAuditLogPersistenceService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 鉴权审计仓储实现（只读查询）。
 * <p>主要为审计日志的分页查询和单条等详提供持久化访问。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DatabaseAuthzAuditRepository implements AuthzAuditRepository {

    private final SysAuthzAuditLogPersistenceService sysAuthzAuditLogPersistenceService;

    /**
     * 分页查询审计日志。
     * <p>count 和列表使用相同的过滤条件，请注意两次查询的开销。
     * 列表按创建时间倒序 + id 倒序排，使用原生 LIMIT/OFFSET 分页。</p>
     */
    @Override
    public AuthzAuditPage query(AuthzAuditQuery query) {
        String tenantId = query.getTenantId();
        String appCode = query.getAppCode();
        String subjectModel = query.getSubjectModel();
        String subjectId = query.getSubjectId();
        String resourceModel = query.getResourceModel();
        String resId = query.getResId();
        String actionCode = query.getActionCode();
        String decision = query.getDecision();
        int pageNo = query.getPageNo();
        int pageSize = query.getPageSize();
        log.debug("[审计查询] 分页查询: tenantId={}, appCode={}, subjectModel={}, subjectId={}, resourceModel={}, resId={}, actionCode={}, decision={}, pageNo={}, pageSize={}",
            tenantId, appCode, subjectModel, subjectId, resourceModel, resId, actionCode, decision, pageNo, pageSize);
        // 第一次查询：获取总数
        long total = sysAuthzAuditLogPersistenceService.lambdaQuery()
            .eq(SysAuthzAuditLogEntity::getTenantId, tenantId)
            .eq(SysAuthzAuditLogEntity::getAppCode, appCode)
            .eq(StringUtils.hasText(subjectModel), SysAuthzAuditLogEntity::getSubjectModel, subjectModel)
            .eq(StringUtils.hasText(subjectId), SysAuthzAuditLogEntity::getSubjectId, subjectId)
            .eq(StringUtils.hasText(resourceModel), SysAuthzAuditLogEntity::getResourceModel, resourceModel)
            .eq(StringUtils.hasText(resId), SysAuthzAuditLogEntity::getResId, resId)
            .eq(StringUtils.hasText(actionCode), SysAuthzAuditLogEntity::getActionCode, actionCode)
            .eq(StringUtils.hasText(decision), SysAuthzAuditLogEntity::getDecision, decision)
            .count();
        int offset = (pageNo - 1) * pageSize;
        // 第二次查询：获取当页数据，倒序排列保证新事件在前
        List<AuthzAuditRecord> records = sysAuthzAuditLogPersistenceService.lambdaQuery()
            .eq(SysAuthzAuditLogEntity::getTenantId, tenantId)
            .eq(SysAuthzAuditLogEntity::getAppCode, appCode)
            .eq(StringUtils.hasText(subjectModel), SysAuthzAuditLogEntity::getSubjectModel, subjectModel)
            .eq(StringUtils.hasText(subjectId), SysAuthzAuditLogEntity::getSubjectId, subjectId)
            .eq(StringUtils.hasText(resourceModel), SysAuthzAuditLogEntity::getResourceModel, resourceModel)
            .eq(StringUtils.hasText(resId), SysAuthzAuditLogEntity::getResId, resId)
            .eq(StringUtils.hasText(actionCode), SysAuthzAuditLogEntity::getActionCode, actionCode)
            .eq(StringUtils.hasText(decision), SysAuthzAuditLogEntity::getDecision, decision)
            .orderByDesc(SysAuthzAuditLogEntity::getCreatedAt)
            .orderByDesc(SysAuthzAuditLogEntity::getId)
            .last("LIMIT " + pageSize + " OFFSET " + offset)
            .list()
            .stream()
            .map(this::toDefinition)
            .collect(Collectors.toList());
        log.debug("[审计查询] 分页查询结果: total={}, pageSize={}, returned={}",
            total, pageSize, records.size());
        return AuthzAuditPage.builder()
            .records(records)
            .pageNo(pageNo)
            .pageSize(pageSize)
            .total(total)
            .build();
    }

    @Override
    public AuthzAuditRecord findById(String tenantId, String appCode, Long auditLogId) {
        SysAuthzAuditLogEntity entity = sysAuthzAuditLogPersistenceService.lambdaQuery()
            .eq(SysAuthzAuditLogEntity::getTenantId, tenantId)
            .eq(SysAuthzAuditLogEntity::getAppCode, appCode)
            .eq(SysAuthzAuditLogEntity::getId, auditLogId)
            .one();
        return entity == null ? null : toDefinition(entity);
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
            .build();
    }

    private List<String> split(String values) {
        if (!StringUtils.hasText(values)) {
            return Collections.emptyList();
        }
        return Arrays.stream(values.split(","))
            .filter(StringUtils::hasText)
            .collect(Collectors.toList());
    }
}