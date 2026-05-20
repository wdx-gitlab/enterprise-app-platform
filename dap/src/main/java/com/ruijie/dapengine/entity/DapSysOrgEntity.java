package com.ruijie.dapengine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * dap_sys_org 表持久化实体。
 */
@Data
@TableName("dap_sys_org")
public class DapSysOrgEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String tenantId;
    private String appCode;
    private String code;
    private String name;
    private String parentCode;
    private Long dapVersion;
    private LocalDateTime dapSyncTime;
    private Integer isDelete;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private String departmentCode;
    private String departmentName;
    private String departmentEnName;
    private Long departmentLevel;
    private String departmentTypeCode;
    private String departmentType;
    private String departmentCategory;
    private Long parentOrgId;
    private String parentDepartmentCode;
    private String parentDepartmentName;
    private String orgPath;
    private String manageUserId;
    private String manageStaffNo;
    private String manageName;
    private String portionManageUserId;
    private String portionManageStaffNo;
    private String portionManageName;
    private Long isEnable;
    private LocalDateTime createTime;
    private String departmentHrbpList;
    private String hcmPayloadJson;
}

