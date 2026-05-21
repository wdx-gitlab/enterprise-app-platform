package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.StandardActionDefinition;
import com.ruijie.authzengine.domain.model.governance.StandardPolicyTemplateDefinition;
import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserAccount;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 治理目录总装配应用服务。
 */
@Service
@RequiredArgsConstructor
public class CatalogAppService {

    private final MetaAppService metaAppService;

    private final SubjectAppService subjectAppService;

    private final ResourceAppService resourceAppService;

    /**
     * 注册权限元模型。
     *
     * @param definition 元模型定义
     * @return 已保存结果
     */
    public AuthMetaModelDefinition registerMetaModel(AuthMetaModelDefinition definition) {
        return metaAppService.registerMetaModel(definition);
    }

    /**
     * 注册业务对象元模型。
     *
     * @param definition 业务对象元模型定义
     * @return 已保存结果
     */
    public BoMetaModelDefinition registerBoMetaModel(BoMetaModelDefinition definition) {
        return metaAppService.registerBoMetaModel(definition);
    }

    /**
     * 查询标准动作。
     *
     * @param tenantId 租户标识
     * @return 标准动作列表
     */
    public List<StandardActionDefinition> listStandardActions(String tenantId) {
        return metaAppService.listStandardActions(tenantId);
    }

    /**
     * 查询标准策略模板。
     *
     * @param tenantId 租户标识
     * @return 标准策略模板列表
     */
    public List<StandardPolicyTemplateDefinition> listStandardPolicyTemplates(String tenantId) {
        return metaAppService.listStandardPolicyTemplates(tenantId);
    }

    /**
     * 保存或更新用户目录。
     *
     * @param userAccount 用户目录定义
     * @return 已保存结果
     */
    public SysUserAccount upsertUser(SysUserAccount userAccount) {
        return subjectAppService.upsertUser(userAccount);
    }

    /**
     * 查询用户目录。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @return 用户目录列表
     */
    public List<SysUserAccount> listUsers(String tenantId, String appCode) {
        return subjectAppService.listUsers(tenantId, appCode);
    }

    /**
     * 保存或更新 API 资源目录。
     *
     * @param sysResApi API 资源定义
     * @return 已保存结果
     */
    public SysResApi upsertApi(SysResApi sysResApi) {
        return resourceAppService.upsertApi(sysResApi);
    }

    /**
     * 查询 API 资源目录。
     *
     * @param tenantId 租户标识
     * @param appCode 应用标识
     * @return API 资源列表
     */
    public List<SysResApi> listApis(String tenantId, String appCode) {
        return resourceAppService.listApis(tenantId, appCode);
    }
}