package com.ruijie.authzengine.infrastructure.persistence;

import com.ruijie.authzengine.domain.model.common.SubjectKey;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.assignment.SysAuthAssignment;
import com.ruijie.authzengine.domain.repository.AssignmentRepository;
import com.ruijie.authzengine.infrastructure.persistence.entity.SysAuthAssignmentEntity;
import com.ruijie.authzengine.infrastructure.persistence.service.SysAuthAssignmentPersistenceService;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 授权分配治理仓储实现。
 * <p>负责授权分配记录的增删改查，是权限分配管理的持久化入口。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DatabaseAssignmentRepository implements AssignmentRepository {

    private final SysAuthAssignmentPersistenceService sysAuthAssignmentPersistenceService;

    /**
     * 保存或更新授权分配记录。id 不为空时执行更新，否则新增。
     */
    @Override
    public SysAuthAssignment saveAssignment(SysAuthAssignment assignment) {
        SysAuthAssignmentEntity entity = toEntity(assignment);
        boolean isUpdate = assignment.getId() != null;
        if (isUpdate) {
            entity.setId(assignment.getId());
        }
        log.info("[分配仓储] {}授权分配: tenantId={}, appCode={}, subjectModel={}, subjectId={}, permItemId={}",
            isUpdate ? "更新" : "新增",
            assignment.getTenantId(), assignment.getAppCode(),
            assignment.getSubjectModel(), assignment.getSubjectId(), assignment.getPermItemId());
        sysAuthAssignmentPersistenceService.saveOrUpdate(entity);
        log.debug("[分配仓储] 授权分配保存完成: id={}", entity.getId());
        return toDefinition(entity);
    }

    /**
     * 分页查询授权分配列表，支持按 subjectId / subjectModel 关键字模糊过滤。
     * 注意：当前实现为内存分页（先全量加载再截取），数据量大时需关注性能。
     */
    @Override
    public PageResult<SysAuthAssignment> pageAssignments(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        log.debug("[分配仓储] 分页查询授权分配: tenantId={}, appCode={}, keyword={}, pageNo={}, pageSize={}",
            tenantId, appCode, keyword, pageNo, pageSize);
        List<SysAuthAssignment> records = sysAuthAssignmentPersistenceService.lambdaQuery()
            .eq(SysAuthAssignmentEntity::getTenantId, tenantId)
            .eq(SysAuthAssignmentEntity::getAppCode, appCode)
            .and(StringUtils.hasText(keyword), wrapper -> wrapper
                .like(SysAuthAssignmentEntity::getSubjectId, keyword)
                .or()
                .like(SysAuthAssignmentEntity::getSubjectModel, keyword))
            .orderByAsc(SysAuthAssignmentEntity::getId)
            .list()
            .stream()
            .map(this::toDefinition)
            .collect(Collectors.toList());
        log.debug("[分配仓储] 授权分配查询结果: total={}", records.size());
        return buildPage(records, pageNo, pageSize);
    }

    /**
     * 按 ID 查询单条授权分配，租户+应用范围隔离。
     */
    @Override
    public SysAuthAssignment findAssignment(String tenantId, String appCode, Long assignmentId) {
        log.debug("[分配仓储] 按ID查询授权分配: tenantId={}, appCode={}, assignmentId={}", tenantId, appCode, assignmentId);
        SysAuthAssignmentEntity entity = sysAuthAssignmentPersistenceService.lambdaQuery()
            .eq(SysAuthAssignmentEntity::getTenantId, tenantId)
            .eq(SysAuthAssignmentEntity::getAppCode, appCode)
            .eq(SysAuthAssignmentEntity::getId, assignmentId)
            .one();
        if (entity == null) {
            log.debug("[分配仓储] 授权分配不存在: assignmentId={}", assignmentId);
        }
        return entity == null ? null : toDefinition(entity);
    }

    /**
     * 删除指定授权分配记录，若记录不存在则静默跳过。
     */
    @Override
    public void deleteAssignment(String tenantId, String appCode, Long assignmentId) {
        log.info("[分配仓储] 删除授权分配: tenantId={}, appCode={}, assignmentId={}", tenantId, appCode, assignmentId);
        SysAuthAssignmentEntity entity = sysAuthAssignmentPersistenceService.lambdaQuery()
            .eq(SysAuthAssignmentEntity::getTenantId, tenantId)
            .eq(SysAuthAssignmentEntity::getAppCode, appCode)
            .eq(SysAuthAssignmentEntity::getId, assignmentId)
            .one();
        if (entity != null) {
            sysAuthAssignmentPersistenceService.removeById(entity.getId());
            log.info("[分配仓储] 授权分配已删除: id={}", entity.getId());
        } else {
            log.warn("[分配仓储] 待删除的授权分配不存在，跳过: assignmentId={}", assignmentId);
        }
    }

    @Override
    public boolean hasAssignmentReference(String tenantId, String appCode, Long assignmentId) {
        return false;
    }

    /**
     * 按主体身份键集合批量查询授权记录。
     *
     * <p>实现策略：先对主体 ID 集合执行宽泛查询，再在内存中按 (subjectType, subjectId) 精确过滤，
     * 避免 MyBatis-Plus 不支持复合列 IN 查询的限制。
     * 在授权记录数量有限（通常几百到几千条）的场景下性能可接受。
     */
    @Override
    public List<SysAuthAssignment> findAssignmentsBySubjectSet(
            String tenantId, String appCode, List<SubjectKey> subjectKeys) {
        if (subjectKeys == null || subjectKeys.isEmpty()) {
            return Collections.emptyList();
        }
        // 构建精确匹配用的二元组集合，格式：subjectType::subjectId
        Set<String> keySet = subjectKeys.stream()
            .map(k -> k.getSubjectType() + "::" + k.getSubjectId())
            .collect(Collectors.toSet());
        // 取出所有主体 ID，先做宽泛查询（避免 N+1）
        Set<String> subjectIdSet = subjectKeys.stream()
            .map(SubjectKey::getSubjectId)
            .collect(Collectors.toSet());
        return sysAuthAssignmentPersistenceService.lambdaQuery()
            .eq(SysAuthAssignmentEntity::getTenantId, tenantId)
            .eq(SysAuthAssignmentEntity::getAppCode, appCode)
            .in(SysAuthAssignmentEntity::getSubjectId, subjectIdSet)
            .list()
            .stream()
            // 内存中精确过滤 (subjectType, subjectId) 二元组
            .filter(e -> keySet.contains(e.getSubjectModel() + "::" + e.getSubjectId()))
            .map(this::toDefinition)
            .collect(Collectors.toList());
    }

    private SysAuthAssignmentEntity toEntity(SysAuthAssignment assignment) {
        SysAuthAssignmentEntity entity = new SysAuthAssignmentEntity();
        entity.setTenantId(assignment.getTenantId());
        entity.setAppCode(assignment.getAppCode());
        entity.setSubjectModel(assignment.getSubjectModel());
        entity.setSubjectId(assignment.getSubjectId());
        entity.setPermItemId(assignment.getPermItemId());
        entity.setPolicyTplId(assignment.getPolicyTplId());
        entity.setPolicyParams(assignment.getPolicyParams());
        entity.setExpireTime(assignment.getExpireTime());
        return entity;
    }

    private SysAuthAssignment toDefinition(SysAuthAssignmentEntity entity) {
        return SysAuthAssignment.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .appCode(entity.getAppCode())
            .subjectModel(entity.getSubjectModel())
            .subjectId(entity.getSubjectId())
            .permItemId(entity.getPermItemId())
            .policyTplId(entity.getPolicyTplId())
            .policyParams(entity.getPolicyParams())
            .expireTime(entity.getExpireTime())
            .build();
    }

    private <T> PageResult<T> buildPage(List<T> records, int pageNo, int pageSize) {
        List<T> safeRecords = records == null ? Collections.emptyList() : records;
        int safePageNo = pageNo <= 0 ? 1 : pageNo;
        int safePageSize = pageSize <= 0 ? 20 : pageSize;
        int fromIndex = Math.min((safePageNo - 1) * safePageSize, safeRecords.size());
        int toIndex = Math.min(fromIndex + safePageSize, safeRecords.size());
        return PageResult.<T>builder()
            .pageNo(safePageNo)
            .pageSize(safePageSize)
            .total(safeRecords.size())
            .records(safeRecords.subList(fromIndex, toIndex))
            .build();
    }
}
