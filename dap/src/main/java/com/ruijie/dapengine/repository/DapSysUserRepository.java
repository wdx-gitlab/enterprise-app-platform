package com.ruijie.dapengine.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruijie.dapengine.common.model.StaffExtendInfo;
import com.ruijie.dapengine.entity.DapSysUserEntity;
import com.ruijie.dapengine.mapper.DapSysUserMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * dap_sys_user 表数据访问层。
 *
 * <p>按 staff_no 进行员工主档幂等写入：不存在则插入，存在则整行覆盖更新。
 * 不采用“删除后重插”，避免主键漂移、创建时间丢失和额外写放大。</p>
 */
public class DapSysUserRepository {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DapSysUserMapper dapSysUserMapper;
    private final String tenantId;
    private final String appCode;

    public DapSysUserRepository(DapSysUserMapper dapSysUserMapper, String tenantId, String appCode) {
        this.dapSysUserMapper = dapSysUserMapper;
        this.tenantId = tenantId;
        this.appCode = appCode;
    }

    /**
     * 按 staffNo 查询有效员工记录。
     */
    public DapSysUserEntity findActiveByStaffNo(String staffNo) {
        return dapSysUserMapper.selectOne(new LambdaQueryWrapper<DapSysUserEntity>()
                .eq(DapSysUserEntity::getTenantId, tenantId)
                .eq(DapSysUserEntity::getAppCode, appCode)
                .eq(DapSysUserEntity::getStaffNo, staffNo)
                .eq(DapSysUserEntity::getIsDelete, 0)
                .last("LIMIT 1"));
    }

    /**
     * 保存或更新员工主档。
     */
    public void saveOrUpdate(StaffExtendInfo staff, Long orgId, String operator, String payloadJson) {
        DapSysUserEntity existing = findActiveByStaffNo(staff.getStaffNo());
        if (existing == null) {
            DapSysUserEntity entity = new DapSysUserEntity();
            fillInsertFields(entity, staff, orgId, operator, payloadJson);
            dapSysUserMapper.insert(entity);
            return;
        }
        overwriteExisting(existing.getId(), staff, orgId, operator, payloadJson);
    }

    private void fillInsertFields(DapSysUserEntity entity, StaffExtendInfo staff, Long orgId,
                                  String operator, String payloadJson) {
        LocalDateTime now = LocalDateTime.now();
        entity.setTenantId(tenantId);
        entity.setAppCode(appCode);
        entity.setIsDelete(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(operator);
        entity.setUpdatedBy(operator);
        applyMutableFields(entity, staff, orgId, payloadJson, now);
    }

    private void overwriteExisting(Long id, StaffExtendInfo staff, Long orgId,
                                   String operator, String payloadJson) {
        LocalDateTime now = LocalDateTime.now();
        DapSysUserEntity entity = new DapSysUserEntity();
        applyMutableFields(entity, staff, orgId, payloadJson, now);
        dapSysUserMapper.update(null, new LambdaUpdateWrapper<DapSysUserEntity>()
                .eq(DapSysUserEntity::getId, id)
                .set(DapSysUserEntity::getCode, entity.getCode())
                .set(DapSysUserEntity::getName, entity.getName())
                .set(DapSysUserEntity::getParentCode, entity.getParentCode())
                .set(DapSysUserEntity::getDapVersion, entity.getDapVersion())
                .set(DapSysUserEntity::getDapSyncTime, entity.getDapSyncTime())
                .set(DapSysUserEntity::getUpdatedAt, now)
                .set(DapSysUserEntity::getUpdatedBy, operator)
                .set(DapSysUserEntity::getOrgId, entity.getOrgId())
                .set(DapSysUserEntity::getStaffNo, entity.getStaffNo())
                .set(DapSysUserEntity::getUserId, entity.getUserId())
                .set(DapSysUserEntity::getStaffCompanyName, entity.getStaffCompanyName())
                .set(DapSysUserEntity::getStaffName, entity.getStaffName())
                .set(DapSysUserEntity::getStaffEnName, entity.getStaffEnName())
                .set(DapSysUserEntity::getStaffStatus, entity.getStaffStatus())
                .set(DapSysUserEntity::getCardNum, entity.getCardNum())
                .set(DapSysUserEntity::getCardTypeCode, entity.getCardTypeCode())
                .set(DapSysUserEntity::getCardType, entity.getCardType())
                .set(DapSysUserEntity::getStaffEmail, entity.getStaffEmail())
                .set(DapSysUserEntity::getStaffTypeCode, entity.getStaffTypeCode())
                .set(DapSysUserEntity::getStaffType, entity.getStaffType())
                .set(DapSysUserEntity::getStaffPhoto, entity.getStaffPhoto())
                .set(DapSysUserEntity::getPersonalEmail, entity.getPersonalEmail())
                .set(DapSysUserEntity::getPersonalMobile, entity.getPersonalMobile())
                .set(DapSysUserEntity::getJoinDate, entity.getJoinDate())
                .set(DapSysUserEntity::getJoinJobDate, entity.getJoinJobDate())
                .set(DapSysUserEntity::getNationCode, entity.getNationCode())
                .set(DapSysUserEntity::getNation, entity.getNation())
                .set(DapSysUserEntity::getCountryAndAreaCode, entity.getCountryAndAreaCode())
                .set(DapSysUserEntity::getCountryAndArea, entity.getCountryAndArea())
                .set(DapSysUserEntity::getWorkPlaceCode, entity.getWorkPlaceCode())
                .set(DapSysUserEntity::getWorkPlace, entity.getWorkPlace())
                .set(DapSysUserEntity::getOfficePlaceCode, entity.getOfficePlaceCode())
                .set(DapSysUserEntity::getOfficePlace, entity.getOfficePlace())
                .set(DapSysUserEntity::getBirthday, entity.getBirthday())
                .set(DapSysUserEntity::getEducationalBackgroundCode, entity.getEducationalBackgroundCode())
                .set(DapSysUserEntity::getEducationalBackground, entity.getEducationalBackground())
                .set(DapSysUserEntity::getMarriageCode, entity.getMarriageCode())
                .set(DapSysUserEntity::getMarriage, entity.getMarriage())
                .set(DapSysUserEntity::getNativePlace, entity.getNativePlace())
                .set(DapSysUserEntity::getGenderCode, entity.getGenderCode())
                .set(DapSysUserEntity::getGender, entity.getGender())
                .set(DapSysUserEntity::getActualFormalDate, entity.getActualFormalDate())
                .set(DapSysUserEntity::getLastWorkDate, entity.getLastWorkDate())
                .set(DapSysUserEntity::getManageUserId, entity.getManageUserId())
                .set(DapSysUserEntity::getManageStaffNo, entity.getManageStaffNo())
                .set(DapSysUserEntity::getManageStaffName, entity.getManageStaffName())
                .set(DapSysUserEntity::getTutorUserId, entity.getTutorUserId())
                .set(DapSysUserEntity::getTutorStaffNo, entity.getTutorStaffNo())
                .set(DapSysUserEntity::getTutorStaffName, entity.getTutorStaffName())
                .set(DapSysUserEntity::getPostCode, entity.getPostCode())
                .set(DapSysUserEntity::getPostName, entity.getPostName())
                .set(DapSysUserEntity::getPostTypeCode, entity.getPostTypeCode())
                .set(DapSysUserEntity::getPostType, entity.getPostType())
                .set(DapSysUserEntity::getDepartmentCode, entity.getDepartmentCode())
                .set(DapSysUserEntity::getDepartmentName, entity.getDepartmentName())
                .set(DapSysUserEntity::getOneDepartmentCode, entity.getOneDepartmentCode())
                .set(DapSysUserEntity::getTwoDepartmentCode, entity.getTwoDepartmentCode())
                .set(DapSysUserEntity::getThreeDepartmentCode, entity.getThreeDepartmentCode())
                .set(DapSysUserEntity::getFourDepartmentCode, entity.getFourDepartmentCode())
                .set(DapSysUserEntity::getFiveDepartmentCode, entity.getFiveDepartmentCode())
                .set(DapSysUserEntity::getSixDepartmentCode, entity.getSixDepartmentCode())
                .set(DapSysUserEntity::getSevenDepartmentCode, entity.getSevenDepartmentCode())
                .set(DapSysUserEntity::getEightDepartmentCode, entity.getEightDepartmentCode())
                .set(DapSysUserEntity::getNineDepartmentCode, entity.getNineDepartmentCode())
                .set(DapSysUserEntity::getTenDepartmentCode, entity.getTenDepartmentCode())
                .set(DapSysUserEntity::getOneDepartmentName, entity.getOneDepartmentName())
                .set(DapSysUserEntity::getTwoDepartmentName, entity.getTwoDepartmentName())
                .set(DapSysUserEntity::getThreeDepartmentName, entity.getThreeDepartmentName())
                .set(DapSysUserEntity::getFourDepartmentName, entity.getFourDepartmentName())
                .set(DapSysUserEntity::getFiveDepartmentName, entity.getFiveDepartmentName())
                .set(DapSysUserEntity::getSixDepartmentName, entity.getSixDepartmentName())
                .set(DapSysUserEntity::getSevenDepartmentName, entity.getSevenDepartmentName())
                .set(DapSysUserEntity::getEightDepartmentName, entity.getEightDepartmentName())
                .set(DapSysUserEntity::getNineDepartmentName, entity.getNineDepartmentName())
                .set(DapSysUserEntity::getTenDepartmentName, entity.getTenDepartmentName())
                .set(DapSysUserEntity::getOrganizationTypeCode, entity.getOrganizationTypeCode())
                .set(DapSysUserEntity::getOrganizationType, entity.getOrganizationType())
                .set(DapSysUserEntity::getComputerTypeCode, entity.getComputerTypeCode())
                .set(DapSysUserEntity::getComputerType, entity.getComputerType())
                .set(DapSysUserEntity::getCountryCode, entity.getCountryCode())
                .set(DapSysUserEntity::getPermanentCountryAndArea, entity.getPermanentCountryAndArea())
                .set(DapSysUserEntity::getPermanentCountryAndAreaCode, entity.getPermanentCountryAndAreaCode())
                .set(DapSysUserEntity::getVenderCompany, entity.getVenderCompany())
                .set(DapSysUserEntity::getEmploymentTypeCode, entity.getEmploymentTypeCode())
                .set(DapSysUserEntity::getEmpId, entity.getEmpId())
                .set(DapSysUserEntity::getEnglishCountryAndArea, entity.getEnglishCountryAndArea())
                .set(DapSysUserEntity::getEnglishPermanentCountryAndArea, entity.getEnglishPermanentCountryAndArea())
                .set(DapSysUserEntity::getPaySubjectCode, entity.getPaySubjectCode())
                .set(DapSysUserEntity::getPaySubject, entity.getPaySubject())
                .set(DapSysUserEntity::getIsDeptManage, entity.getIsDeptManage())
                .set(DapSysUserEntity::getIsDeptPortionManage, entity.getIsDeptPortionManage())
                .set(DapSysUserEntity::getStaffManagementJurisdiction, entity.getStaffManagementJurisdiction())
                .set(DapSysUserEntity::getHcmPayloadJson, entity.getHcmPayloadJson()));
    }

    private void applyMutableFields(DapSysUserEntity entity, StaffExtendInfo staff, Long orgId,
                                    String payloadJson, LocalDateTime now) {
        entity.setCode(defaultIfBlank(staff.getStaffNo(), staff.getUserId()));
        entity.setName(defaultIfBlank(staff.getStaffCompanyName(), staff.getStaffName(), staff.getUserId(), staff.getStaffNo()));
        entity.setParentCode(defaultIfBlank(staff.getDepartmentCode(), ""));
        entity.setDapVersion(System.currentTimeMillis());
        entity.setDapSyncTime(now);
        entity.setOrgId(orgId);
        entity.setStaffNo(staff.getStaffNo());
        entity.setUserId(staff.getUserId());
        entity.setStaffCompanyName(staff.getStaffCompanyName());
        entity.setStaffName(staff.getStaffName());
        entity.setStaffEnName(staff.getStaffEnName());
        entity.setStaffStatus(toLong(staff.getStaffStatus()));
        entity.setCardNum(staff.getCardNum());
        entity.setCardTypeCode(staff.getCardTypeCode());
        entity.setCardType(staff.getCardType());
        entity.setStaffEmail(staff.getStaffEmail());
        entity.setStaffTypeCode(staff.getStaffTypeCode());
        entity.setStaffType(staff.getStaffType());
        entity.setStaffPhoto(staff.getStaffPhoto());
        entity.setPersonalEmail(staff.getPersonalEmail());
        entity.setPersonalMobile(staff.getPersonalMobile());
        entity.setJoinDate(parseDateTime(staff.getJoinDate()));
        entity.setJoinJobDate(parseDateTime(staff.getJoinJobDate()));
        entity.setNationCode(staff.getNationCode());
        entity.setNation(staff.getNation());
        entity.setCountryAndAreaCode(staff.getCountryAndAreaCode());
        entity.setCountryAndArea(staff.getCountryAndArea());
        entity.setWorkPlaceCode(staff.getWorkPlaceCode());
        entity.setWorkPlace(staff.getWorkPlace());
        entity.setOfficePlaceCode(staff.getOfficePlaceCode());
        entity.setOfficePlace(staff.getOfficePlace());
        entity.setBirthday(parseDateTime(staff.getBirthday()));
        entity.setEducationalBackgroundCode(staff.getEducationalBackgroundCode());
        entity.setEducationalBackground(staff.getEducationalBackground());
        entity.setMarriageCode(staff.getMarriageCode());
        entity.setMarriage(staff.getMarriage());
        entity.setNativePlace(staff.getNativePlace());
        entity.setGenderCode(staff.getGenderCode());
        entity.setGender(staff.getGender());
        entity.setActualFormalDate(parseDateTime(staff.getActualFormalDate()));
        entity.setLastWorkDate(parseDateTime(staff.getLastWorkDate()));
        entity.setManageUserId(staff.getManageUserId());
        entity.setManageStaffNo(staff.getManageStaffNo());
        entity.setManageStaffName(staff.getManageStaffName());
        entity.setTutorUserId(staff.getTutorUserId());
        entity.setTutorStaffNo(staff.getTutorStaffNo());
        entity.setTutorStaffName(staff.getTutorStaffName());
        entity.setPostCode(staff.getPostCode());
        entity.setPostName(staff.getPostName());
        entity.setPostTypeCode(staff.getPostTypeCode());
        entity.setPostType(staff.getPostType());
        entity.setDepartmentCode(staff.getDepartmentCode());
        entity.setDepartmentName(staff.getDepartmentName());
        entity.setOneDepartmentCode(staff.getOneDepartmentCode());
        entity.setTwoDepartmentCode(staff.getTwoDepartmentCode());
        entity.setThreeDepartmentCode(staff.getThreeDepartmentCode());
        entity.setFourDepartmentCode(staff.getFourDepartmentCode());
        entity.setFiveDepartmentCode(staff.getFiveDepartmentCode());
        entity.setSixDepartmentCode(staff.getSixDepartmentCode());
        entity.setSevenDepartmentCode(staff.getSevenDepartmentCode());
        entity.setEightDepartmentCode(staff.getEightDepartmentCode());
        entity.setNineDepartmentCode(staff.getNineDepartmentCode());
        entity.setTenDepartmentCode(staff.getTenDepartmentCode());
        entity.setOneDepartmentName(staff.getOneDepartmentName());
        entity.setTwoDepartmentName(staff.getTwoDepartmentName());
        entity.setThreeDepartmentName(staff.getThreeDepartmentName());
        entity.setFourDepartmentName(staff.getFourDepartmentName());
        entity.setFiveDepartmentName(staff.getFiveDepartmentName());
        entity.setSixDepartmentName(staff.getSixDepartmentName());
        entity.setSevenDepartmentName(staff.getSevenDepartmentName());
        entity.setEightDepartmentName(staff.getEightDepartmentName());
        entity.setNineDepartmentName(staff.getNineDepartmentName());
        entity.setTenDepartmentName(staff.getTenDepartmentName());
        entity.setOrganizationTypeCode(staff.getOrganizationTypeCode());
        entity.setOrganizationType(staff.getOrganizationType());
        entity.setComputerTypeCode(staff.getComputerTypeCode());
        entity.setComputerType(staff.getComputerType());
        entity.setCountryCode(staff.getCountryCode());
        entity.setPermanentCountryAndArea(staff.getPermanentCountryAndArea());
        entity.setPermanentCountryAndAreaCode(staff.getPermanentCountryAndAreaCode());
        entity.setVenderCompany(staff.getVenderCompany());
        entity.setEmploymentTypeCode(staff.getEmploymentTypeCode());
        entity.setEmpId(toLong(staff.getEmpId()));
        entity.setEnglishCountryAndArea(staff.getEnglishCountryAndArea());
        entity.setEnglishPermanentCountryAndArea(staff.getEnglishPermanentCountryAndArea());
        entity.setPaySubjectCode(staff.getPaySubjectCode());
        entity.setPaySubject(staff.getPaySubject());
        entity.setIsDeptManage(toFlag(staff.getIsDeptManage()));
        entity.setIsDeptPortionManage(toFlag(staff.getIsDeptPortionManage()));
        entity.setStaffManagementJurisdiction(staff.getStaffManagementJurisidiction());
        entity.setHcmPayloadJson(payloadJson);
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

