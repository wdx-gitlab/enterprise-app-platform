package com.ruijie.dapengine.admin.service;

import com.alibaba.fastjson.JSON;
import com.ruijie.dapengine.common.model.DepartmentExtendInfo;
import com.ruijie.dapengine.common.model.RemoteResult;
import com.ruijie.dapengine.entity.DapSysOrgEntity;
import com.ruijie.dapengine.repository.DapSysOrgRepository;
import com.ruijie.dapengine.sync.OSDSService;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * 组织架构同步服务。
 */
public class OrgSyncService {

    private static final String OPERATOR = "hr-notify";

    private final OSDSService osdsService;
    private final DapSysOrgRepository dapSysOrgRepository;

    public OrgSyncService(OSDSService osdsService, DapSysOrgRepository dapSysOrgRepository) {
        this.osdsService = osdsService;
        this.dapSysOrgRepository = dapSysOrgRepository;
    }

    /**
     * 确保部门及其父级链路已存在于 dap_sys_org。
     */
    @Transactional("dapTransactionManager")
    public Long ensureDepartmentChain(String departmentCode) {
        return ensureDepartmentChain(departmentCode, new HashSet<>());
    }

    private Long ensureDepartmentChain(String departmentCode, Set<String> visiting) {
        if (departmentCode == null || departmentCode.trim().isEmpty()) {
            return null;
        }
        String normalizedCode = departmentCode.trim();
        DapSysOrgEntity existing = dapSysOrgRepository.findActiveByDepartmentCode(normalizedCode);
        if (existing != null) {
            return existing.getId();
        }
        if (!visiting.add(normalizedCode)) {
            throw new IllegalStateException("部门父级链路存在循环引用: " + normalizedCode);
        }

        RemoteResult<DepartmentExtendInfo> result = osdsService.getDepartment(normalizedCode);
        if (result == null || result.getData() == null) {
            throw new IllegalStateException("OSDS 未返回部门信息: " + normalizedCode);
        }
        DepartmentExtendInfo department = result.getData();
        DapSysOrgEntity parent = null;
        if (department.getParentDepartmentCode() != null
                && !department.getParentDepartmentCode().trim().isEmpty()
                && !normalizedCode.equals(department.getParentDepartmentCode().trim())) {
            Long parentId = ensureDepartmentChain(department.getParentDepartmentCode().trim(), visiting);
            if (parentId != null) {
                parent = dapSysOrgRepository.findActiveByDepartmentCode(department.getParentDepartmentCode().trim());
            }
        }
        DapSysOrgEntity created = dapSysOrgRepository.insert(department, parent, OPERATOR, JSON.toJSONString(department));
        visiting.remove(normalizedCode);
        return created.getId();
    }
}

