package com.ruijie.authzengine.domain.repository;

import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.StandardActionDefinition;
import com.ruijie.authzengine.domain.model.governance.StandardPolicyTemplateDefinition;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 治理元模型与标准库仓储。
 */
public interface MetaRepository {

    /**
     * 查询数据库中已配置的所有租户-应用组合（去重）。
     *
     * @return tenantId -> appCode 列表的映射
     */
    default Map<String, List<String>> listDistinctTenantApps() {
        return Collections.emptyMap();
    }

    /**
     * 注册或更新权限元模型。
     *
     * @param definition 元模型定义
     * @return 已保存定义
     */
    AuthMetaModelDefinition saveAuthMetaModel(AuthMetaModelDefinition definition);

    /**
     * 分页查询权限元模型。
     */
    default PageResult<AuthMetaModelDefinition> pageAuthMetaModels(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        return emptyPage(pageNo, pageSize);
    }

    /**
     * 查询权限元模型详情。
     */
    default AuthMetaModelDefinition findAuthMetaModel(String tenantId, String appCode, String modelCode) {
        return null;
    }

    /**
     * 删除权限元模型。
     */
    default void deleteAuthMetaModel(String tenantId, String appCode, String modelCode) {
        throw new UnsupportedOperationException("当前仓储未实现权限元模型删除能力");
    }

    /**
     * 判断权限元模型是否仍被引用。
     */
    default boolean hasAuthMetaModelReference(String tenantId, String appCode, String modelCode) {
        return false;
    }

    /**
     * 注册或更新业务对象元模型。
     *
     * @param definition 业务对象元模型定义
     * @return 已保存定义
     */
    BoMetaModelDefinition saveBoMetaModel(BoMetaModelDefinition definition);

    /**
     * 分页查询业务对象元模型。
     */
    default PageResult<BoMetaModelDefinition> pageBoMetaModels(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
        return emptyPage(pageNo, pageSize);
    }

    /**
     * 查询业务对象元模型详情。
     */
    default BoMetaModelDefinition findBoMetaModel(String tenantId, String appCode, String boCode) {
        return null;
    }

    /**
     * 按主键查询业务对象元模型详情。
     */
    default BoMetaModelDefinition findBoMetaModelById(String tenantId, String appCode, Long boId) {
        return null;
    }

    /**
     * 删除业务对象元模型。
     */
    default void deleteBoMetaModel(String tenantId, String appCode, String boCode) {
        throw new UnsupportedOperationException("当前仓储未实现业务对象元模型删除能力");
    }

    /**
     * 判断业务对象元模型是否仍被引用。
     */
    default boolean hasBoMetaModelReference(String tenantId, String appCode, String boCode) {
        return false;
    }

    /**
     * 兼容 002 的标准动作列表查询入口。
     */
    default List<StandardActionDefinition> listStandardActions(String tenantId) {
        return Collections.emptyList();
    }

    /**
     * 查询标准动作目录。
     *
     * @param tenantId 租户标识
     * @return 标准动作列表
     */
    default PageResult<StandardActionDefinition> pageStandardActions(String tenantId, String keyword, int pageNo, int pageSize) {
        return pageOf(listStandardActions(tenantId), pageNo, pageSize);
    }

    /**
     * 查询标准动作详情。
     */
    default StandardActionDefinition findStandardAction(String tenantId, String actCode) {
        return null;
    }

    /**
     * 保存标准动作。
     */
    default StandardActionDefinition saveStandardAction(StandardActionDefinition definition) {
        throw new UnsupportedOperationException("当前仓储未实现标准动作写入能力");
    }

    /**
     * 删除标准动作。
     */
    default void deleteStandardAction(String tenantId, String actCode) {
        throw new UnsupportedOperationException("当前仓储未实现标准动作删除能力");
    }

    /**
     * 判断标准动作是否仍被引用。
     */
    default boolean hasStandardActionReference(String tenantId, String actCode) {
        return false;
    }

    /**
     * 兼容 002 的策略模板列表查询入口。
     */
    default List<StandardPolicyTemplateDefinition> listStandardPolicyTemplates(String tenantId) {
        return Collections.emptyList();
    }

    /**
     * 查询标准策略模板目录。
     *
     * @param tenantId 租户标识
     * @return 标准策略模板列表
     */
    default PageResult<StandardPolicyTemplateDefinition> pageStandardPolicyTemplates(String tenantId, String keyword, int pageNo, int pageSize) {
        return pageOf(listStandardPolicyTemplates(tenantId), pageNo, pageSize);
    }

    /**
     * 按策略类型分页查询标准策略模板目录。
     *
     * @param tenantId 租户标识
     * @param keyword  关键字
     * @param polType  策略类型
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 标准策略模板列表
     */
    default PageResult<StandardPolicyTemplateDefinition> pageStandardPolicyTemplates(
        String tenantId,
        String keyword,
        String polType,
        int pageNo,
        int pageSize
    ) {
        return pageStandardPolicyTemplates(tenantId, keyword, pageNo, pageSize);
    }

    /**
     * 查询策略模板详情。
     */
    default StandardPolicyTemplateDefinition findStandardPolicyTemplate(String tenantId, String templateCode) {
        return null;
    }

    /**
     * 保存策略模板。
     */
    default StandardPolicyTemplateDefinition saveStandardPolicyTemplate(StandardPolicyTemplateDefinition definition) {
        throw new UnsupportedOperationException("当前仓储未实现策略模板写入能力");
    }

    /**
     * 删除策略模板。
     */
    default void deleteStandardPolicyTemplate(String tenantId, String templateCode) {
        throw new UnsupportedOperationException("当前仓储未实现策略模板删除能力");
    }

    /**
     * 判断策略模板是否仍被引用。
     */
    default boolean hasStandardPolicyTemplateReference(String tenantId, String templateCode) {
        return false;
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