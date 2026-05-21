package com.ruijie.authzengine.domain.repository;

import com.ruijie.authzengine.domain.model.common.SubjectKey;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.assignment.SysAuthAssignment;
import java.util.Collections;
import java.util.List;

/**
 * 授权分配治理仓储骨架。
 */
public interface AssignmentRepository {

    SysAuthAssignment saveAssignment(SysAuthAssignment assignment);

    PageResult<SysAuthAssignment> pageAssignments(String tenantId, String appCode, String keyword, int pageNo, int pageSize);

    SysAuthAssignment findAssignment(String tenantId, String appCode, Long assignmentId);

    void deleteAssignment(String tenantId, String appCode, Long assignmentId);

    boolean hasAssignmentReference(String tenantId, String appCode, Long assignmentId);

    /**
     * 按主体身份集合批量查询授权记录。
     *
     * <p>权限查询接口（Q1-Q4）主体展开后调用此方法，一次性加载该主体通过用户/角色/组织/岗位/用户组
     * 所有途径获得的全部授权记录，供后续权限项编码归并和资源过滤使用。
     *
     * @param tenantId    租户标识
     * @param appCode     应用标识
     * @param subjectKeys 主体身份键列表（每条包含 subjectType 和 subjectId）
     * @return 命中的授权记录列表，空时返回空列表
     */
    default List<SysAuthAssignment> findAssignmentsBySubjectSet(
            String tenantId, String appCode, List<SubjectKey> subjectKeys) {
        return Collections.emptyList();
    }
}