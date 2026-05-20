package com.ruijie.dapengine.repository;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruijie.dapengine.common.model.DepartmentExtendInfo;
import com.ruijie.dapengine.entity.DapSysOrgEntity;
import com.ruijie.dapengine.mapper.DapSysOrgMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * dap_sys_org 表数据访问层。
 */
public class DapSysOrgRepository {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DapSysOrgMapper dapSysOrgMapper;
    private final String tenantId;
    private final String appCode;

    public DapSysOrgRepository(DapSysOrgMapper dapSysOrgMapper, String tenantId, String appCode) {
        this.dapSysOrgMapper = dapSysOrgMapper;
        this.tenantId = tenantId;
        this.appCode = appCode;
    }

    /**
     * 按部门编码查询有效组织节点。
     */
    public DapSysOrgEntity findActiveByDepartmentCode(String departmentCode) {
        return dapSysOrgMapper.selectOne(new LambdaQueryWrapper<DapSysOrgEntity>()
                .eq(DapSysOrgEntity::getTenantId, tenantId)
                .eq(DapSysOrgEntity::getAppCode, appCode)
                .eq(DapSysOrgEntity::getDepartmentCode, departmentCode)
                .eq(DapSysOrgEntity::getIsDelete, 0)
                .last("LIMIT 1"));
    }

    /**
     * 新增组织节点。
     */
    public DapSysOrgEntity insert(DepartmentExtendInfo department,
                                  DapSysOrgEntity parent,
                                  String operator,
                                  String payloadJson) {
        LocalDateTime now = LocalDateTime.now();
        DapSysOrgEntity entity = new DapSysOrgEntity();
        entity.setTenantId(tenantId);
        entity.setAppCode(appCode);
        entity.setCode(defaultIfBlank(department.getDepartmentCode(), ""));
        entity.setName(defaultIfBlank(department.getDepartmentName(), department.getDepartmentCode(), ""));
        entity.setParentCode(parent != null ? defaultIfBlank(parent.getDepartmentCode(), "")
                : defaultIfBlank(department.getParentDepartmentCode(), ""));
        entity.setDapVersion(System.currentTimeMillis());
        entity.setDapSyncTime(now);
        entity.setIsDelete(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(operator);
        entity.setUpdatedBy(operator);
        entity.setDepartmentCode(department.getDepartmentCode());
        entity.setDepartmentName(department.getDepartmentName());
        entity.setDepartmentEnName(department.getDepartmentEnName());
        entity.setDepartmentLevel(toLong(department.getDepartmentLevel()));
        entity.setDepartmentTypeCode(department.getDepartmentTypeCode());
        entity.setDepartmentType(department.getDepartmentType());
        entity.setDepartmentCategory(department.getDepartmentCategory());
        entity.setParentOrgId(parent != null ? parent.getId() : null);
        entity.setParentDepartmentCode(department.getParentDepartmentCode());
        entity.setParentDepartmentName(department.getParentDepartmentName());
        entity.setOrgPath(buildOrgPath(parent, department));
        entity.setManageUserId(department.getManageUserId());
        entity.setManageStaffNo(department.getManageStaffNo());
        entity.setManageName(department.getManageName());
        entity.setPortionManageUserId(department.getPortionManageUserId());
        entity.setPortionManageStaffNo(department.getPortionManageStaffNo());
        entity.setPortionManageName(department.getPortionManageName());
        entity.setIsEnable(toFlag(department.getIsEnable()));
        entity.setCreateTime(parseDateTime(department.getCreateTime()));
        entity.setDepartmentHrbpList(department.getDepartmentHrbpList() == null
                ? null
                : JSON.toJSONString(department.getDepartmentHrbpList()));
        entity.setHcmPayloadJson(payloadJson);
        dapSysOrgMapper.insert(entity);
        return entity;
    }

    private String buildOrgPath(DapSysOrgEntity parent, DepartmentExtendInfo department) {
        if (department.getOrgPath() != null && !department.getOrgPath().trim().isEmpty()) {
            return department.getOrgPath().trim();
        }
        String current = defaultIfBlank(department.getDepartmentCode(), "");
        if (parent == null || parent.getOrgPath() == null || parent.getOrgPath().trim().isEmpty()) {
            return current;
        }
        return parent.getOrgPath() + "/" + current;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(value.trim(), DATETIME_FORMATTER);
    }

    private Long toLong(Number value) {
        return value == null ? null : value.longValue();
    }

    private Long toFlag(Boolean value) {
        return value == null ? null : (value ? 1L : 0L);
    }

    private String defaultIfBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.trim().isEmpty()) {
                return candidate.trim();
            }
        }
        return null;
    }
}

