package com.ruijie.authzengine.domain.repository;

import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.subject.AuthRole;
import com.ruijie.authzengine.domain.model.governance.subject.AuthSubjectRelation;
import com.ruijie.authzengine.domain.model.governance.subject.SysOrgNode;
import com.ruijie.authzengine.domain.model.governance.subject.SysPosition;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserAccount;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserGroup;
import java.util.Collections;
import java.util.List;

/**
 * 治理主体目录仓储。
 */
public interface SubjectRepository {

    default SysOrgNode saveOrg(SysOrgNode sysOrgNode) {
        throw new UnsupportedOperationException("当前仓储未实现组织写入能力");
    }

    default PageResult<SysOrgNode> pageOrgs(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        return emptyPage(pageNo, pageSize);
    }

    default SysOrgNode findOrg(String tenantId, String appCode, String orgCode) {
        return null;
    }

    default void deleteOrg(String tenantId, String appCode, String orgCode) {
        throw new UnsupportedOperationException("当前仓储未实现组织删除能力");
    }

    default boolean hasOrgReference(String tenantId, String appCode, String orgCode) {
        return false;
    }

    /**
     * 保存或更新用户目录。
     *
     * @param userAccount 用户目录定义
     * @return 已保存定义
     */
    SysUserAccount saveUser(SysUserAccount userAccount);

    default PageResult<SysUserAccount> pageUsers(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        return pageOf(listUsers(tenantId, appCode), pageNo, pageSize);
    }

    default SysUserAccount findUser(String tenantId, String appCode, String subjectKey) {
        return null;
    }

    default void deleteUser(String tenantId, String appCode, String subjectKey) {
        throw new UnsupportedOperationException("当前仓储未实现用户删除能力");
    }

    default boolean hasUserReference(String tenantId, String appCode, String subjectKey) {
        return false;
    }

    default SysPosition savePosition(SysPosition sysPosition) {
        throw new UnsupportedOperationException("当前仓储未实现岗位写入能力");
    }

    default PageResult<SysPosition> pagePositions(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        return emptyPage(pageNo, pageSize);
    }

    default SysPosition findPosition(String tenantId, String appCode, String positionCode) {
        return null;
    }

    default void deletePosition(String tenantId, String appCode, String positionCode) {
        throw new UnsupportedOperationException("当前仓储未实现岗位删除能力");
    }

    default boolean hasPositionReference(String tenantId, String appCode, String positionCode) {
        return false;
    }

    default SysUserGroup saveUserGroup(SysUserGroup sysUserGroup) {
        throw new UnsupportedOperationException("当前仓储未实现用户组写入能力");
    }

    default PageResult<SysUserGroup> pageUserGroups(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        return emptyPage(pageNo, pageSize);
    }

    default SysUserGroup findUserGroup(String tenantId, String appCode, String groupCode) {
        return null;
    }

    default void deleteUserGroup(String tenantId, String appCode, String groupCode) {
        throw new UnsupportedOperationException("当前仓储未实现用户组删除能力");
    }

    default boolean hasUserGroupReference(String tenantId, String appCode, String groupCode) {
        return false;
    }

    default AuthRole saveRole(AuthRole authRole) {
        throw new UnsupportedOperationException("当前仓储未实现角色写入能力");
    }

    default PageResult<AuthRole> pageRoles(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        return emptyPage(pageNo, pageSize);
    }

    default AuthRole findRole(String tenantId, String appCode, String roleCode) {
        return null;
    }

    default void deleteRole(String tenantId, String appCode, String roleCode) {
        throw new UnsupportedOperationException("当前仓储未实现角色删除能力");
    }

    default boolean hasRoleReference(String tenantId, String appCode, String roleCode) {
        return false;
    }

    default AuthSubjectRelation saveSubjectRelation(AuthSubjectRelation authSubjectRelation) {
        throw new UnsupportedOperationException("当前仓储未实现主体关系写入能力");
    }

    default PageResult<AuthSubjectRelation> pageSubjectRelations(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        return emptyPage(pageNo, pageSize);
    }

    default AuthSubjectRelation findSubjectRelation(String tenantId, String appCode, Long relationId) {
        return null;
    }

    default void deleteSubjectRelation(String tenantId, String appCode, Long relationId) {
        throw new UnsupportedOperationException("当前仓储未实现主体关系删除能力");
    }

    /**
     * 查询用户目录。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @return 用户目录列表
     */
    default List<SysUserAccount> listUsers(String tenantId, String appCode) {
        return Collections.emptyList();
    }

    /**
     * 查询指定用户的所有主体关联关系（角色/岗位/组织/用户组）。
     *
     * <p>权限查询接口（Q1-Q4）用于主体身份展开：
     * 将用户 ID 展开为完整的身份集合（角色列表、组织链路、岗位列表、用户组列表），
     * 再按身份集合查询授权记录，实现角色/组织等间接授权的归并。
     *
     * @param tenantId  租户标识
     * @param appCode   应用标识
     * @param subjectId 用户 ID（通常为 SUB_USER 类型的主体标识）
     * @return 主体关联关系列表，空时返回空列表
     */
    default List<AuthSubjectRelation> findRelationsByUserId(String tenantId, String appCode, String subjectId) {
        return Collections.emptyList();
    }

    static <T> PageResult<T> emptyPage(int pageNo, int pageSize) {
        return PageResult.<T>builder()
            .pageNo(pageNo)
            .pageSize(pageSize)
            .total(0)
            .records(Collections.emptyList())
            .build();
    }

    static <T> PageResult<T> pageOf(List<T> records, int pageNo, int pageSize) {
        List<T> safeRecords = records == null ? Collections.<T>emptyList() : records;
        return PageResult.<T>builder()
            .pageNo(pageNo)
            .pageSize(pageSize)
            .total(safeRecords.size())
            .records(safeRecords)
            .build();
    }
}