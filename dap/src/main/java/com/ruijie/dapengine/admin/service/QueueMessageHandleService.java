package com.ruijie.dapengine.admin.service;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ruijie.dapengine.common.model.MtmMsgContent;
import com.ruijie.dapengine.common.model.MtmMsgContentValue;
import com.ruijie.dapengine.common.model.RemoteResult;
import com.ruijie.dapengine.common.model.StaffExtendInfo;
import com.ruijie.dapengine.repository.DapSysUserRepository;
import com.ruijie.dapengine.sync.OSDSService;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 处理osds消息service
 */
@Slf4j
public class QueueMessageHandleService {

    private static final String OPERATOR = "hr-notify";

    private static final Set<String> STAFF_EVENT_TYPES = new HashSet<>(Arrays.asList(
            "UpdateStaffMainCard",
            "UpdateStaffEmail",
            "UpdateStaffPersonalMobile",
            "StaffEntry",
            "Formal",
            "UpdateStaffOrganization",
            "StaffReinstate",
            "UpdateStaffManage",
            "BeforehandLeave",
            "BeforeLeaveRevoke",
            "AfterLeaveRevoke",
            "StaffLeave"
    ));

    private final OSDSService osdsService;
    private final OrgSyncService orgSyncService;
    private final DapSysUserRepository dapSysUserRepository;

    public QueueMessageHandleService(OSDSService osdsService,
                                     OrgSyncService orgSyncService,
                                     DapSysUserRepository dapSysUserRepository) {
        this.osdsService = osdsService;
        this.orgSyncService = orgSyncService;
        this.dapSysUserRepository = dapSysUserRepository;
    }

    /**
     * 员工事件类型，数据结构【key: 事件名称，value: 是否触发更新】
     */
    private Set<String> staffEventTypeMap() {
        return STAFF_EVENT_TYPES;
    }

    /**
     * 处理员工信息变更事件
     *
     * @param msgContent 消息内容 JSON
     */
    public void updateStaffInfo(String msgContent) {
        if (msgContent == null || msgContent.trim().isEmpty()) {
            log.warn("忽略空的 HR 通知消息");
            return;
        }
        MtmMsgContent mtmMsgContent = JSONObject.parseObject(msgContent, MtmMsgContent.class);
        if (mtmMsgContent == null || mtmMsgContent.getEventType() == null) {
            log.warn("HR 通知消息缺少 eventType: {}", msgContent);
            return;
        }
        if (!staffEventTypeMap().contains(mtmMsgContent.getEventType())) {
            log.info("忽略未监听的 HR 事件: {}", mtmMsgContent.getEventType());
            return;
        }
        if (mtmMsgContent.getParameter() == null || mtmMsgContent.getParameter().isEmpty()) {
            log.warn("HR 事件参数为空, eventType={}", mtmMsgContent.getEventType());
            return;
        }
        for (MtmMsgContentValue mtmMsgContentValue : mtmMsgContent.getParameter()) {
            if (mtmMsgContentValue == null || mtmMsgContentValue.getValue() == null
                    || mtmMsgContentValue.getValue().trim().isEmpty()) {
                continue;
            }
            updateStaff(mtmMsgContentValue.getValue().trim());
        }
    }

    /**
     * 根据工号更新员工扩展信息
     *
     * @param staffNo 工号
     */
    private void updateStaff(String staffNo) {
        try {
            RemoteResult<StaffExtendInfo> result = osdsService.getStaff(staffNo);
            if (result == null || result.getData() == null) {
                log.warn("OSDS 未返回员工数据, staffNo={}, err={}", staffNo,
                        result != null ? result.getErr() : null);
                return;
            }

            StaffExtendInfo staffExtendInfo = result.getData();
            if (staffExtendInfo.getStaffNo() == null || staffExtendInfo.getStaffNo().trim().isEmpty()) {
                log.warn("OSDS 返回员工缺少 staffNo, 请求工号={}", staffNo);
                return;
            }
            Long orgId = null;
            if (staffExtendInfo.getDepartmentCode() != null && !staffExtendInfo.getDepartmentCode().trim().isEmpty()) {
                orgId = orgSyncService.ensureDepartmentChain(staffExtendInfo.getDepartmentCode().trim());
            }
            dapSysUserRepository.saveOrUpdate(staffExtendInfo, orgId, OPERATOR, JSON.toJSONString(staffExtendInfo));
            log.info("同步员工成功, staffNo={}, userId={}", staffExtendInfo.getStaffNo(), staffExtendInfo.getUserId());
        } catch (Exception e) {
            log.error("同步员工信息失败, staffNo={}", staffNo, e);
        }
    }
}