package com.ruijie.dapengine.common.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * OSDS 部门扩展信息。
 */
@Data
public class DepartmentExtendInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String departmentCode;
    private String departmentName;
    private String departmentEnName;
    private Integer departmentLevel;
    private String departmentTypeCode;
    private String departmentType;
    private String departmentCategory;
    private String parentDepartmentCode;
    private String parentDepartmentName;
    private String orgPath;
    private String manageUserId;
    private String manageStaffNo;
    private String manageName;
    private String portionManageUserId;
    private String portionManageStaffNo;
    private String portionManageName;
    private Boolean isEnable;
    private String createTime;
    private List<Object> departmentHrbpList;
}

