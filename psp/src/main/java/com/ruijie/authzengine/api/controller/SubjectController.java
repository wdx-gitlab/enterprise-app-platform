package com.ruijie.authzengine.api.controller;

import com.ruijie.authzengine.api.assembler.SubjectAssembler;
import com.ruijie.authzengine.api.dto.request.PositionRequest;
import com.ruijie.authzengine.api.dto.request.RoleRequest;
import com.ruijie.authzengine.api.dto.request.SubjectRelationRequest;
import com.ruijie.authzengine.api.dto.request.SysOrgRequest;
import com.ruijie.authzengine.api.dto.request.UserGroupRequest;
import com.ruijie.authzengine.api.dto.request.UserRequest;
import com.ruijie.authzengine.api.dto.response.PageResponse;
import com.ruijie.authzengine.api.dto.response.OperationAckResponse;
import com.ruijie.authzengine.api.dto.response.PositionResponse;
import com.ruijie.authzengine.api.dto.response.RoleResponse;
import com.ruijie.authzengine.api.dto.response.SubjectRelationResponse;
import com.ruijie.authzengine.api.dto.response.SysOrgResponse;
import com.ruijie.authzengine.api.dto.response.UserGroupResponse;
import com.ruijie.authzengine.api.dto.response.UserResponse;
import com.ruijie.authzengine.application.spi.DataItem;
import com.ruijie.authzengine.application.service.SubjectAppService;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.subject.AuthSubjectRelation;
import com.ruijie.authzengine.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 治理主体目录分域接口。
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/authz-engine/api/v1/governance/subjects")
@Tag(name = " Subject", description = "治理主体目录 CRUD 接口")
public class SubjectController {

    private final SubjectAppService subjectAppService;

    private final SubjectAssembler subjectAssembler;

    @GetMapping("/orgs")
    @Operation(summary = "分页查询组织目录")
    public ApiResponse<PageResponse<SysOrgResponse>> pageOrgs(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(
            subjectAssembler.toPageResponse(
                subjectAppService.pageOrgs(tenantId, appCode, keyword, pageNo, pageSize),
                subjectAssembler::toResponse
            )
        );
    }

    @PostMapping("/orgs")
    @Operation(summary = "创建组织目录")
    public ApiResponse<OperationAckResponse> createOrg(@Valid @RequestBody SysOrgRequest request) {
        SysOrgResponse response = subjectAssembler.toResponse(
            subjectAppService.createOrg(subjectAssembler.toDefinition(request))
        );
        log.info("创建组织目录 tenantId={}, appCode={}, departmentCode={}", request.getTenantId(), request.getAppCode(), request.getDepartmentCode());
        return ApiResponse.success(ack(response.getDepartmentCode(), "组织目录创建成功"));
    }

    @GetMapping("/orgs/{recordCode}")
    @Operation(summary = "查询组织目录详情")
    public ApiResponse<SysOrgResponse> getOrg(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @Parameter(description = "组织编码") @PathVariable("recordCode") String recordCode
    ) {
        return ApiResponse.success(
            subjectAssembler.toResponse(subjectAppService.getOrg(tenantId, appCode, recordCode))
        );
    }

    @PutMapping("/orgs/{recordCode}")
    @Operation(summary = "更新组织目录")
    public ApiResponse<OperationAckResponse> updateOrg(
        @Parameter(description = "组织编码") @PathVariable("recordCode") String recordCode,
        @Valid @RequestBody SysOrgRequest request
    ) {
        subjectAppService.updateOrg(
            request.getTenantId(),
            request.getAppCode(),
            recordCode,
            subjectAssembler.toDefinition(request)
        );
        return ApiResponse.success(ack(recordCode, "组织目录更新成功"));
    }

    @DeleteMapping("/orgs/{recordCode}")
    @Operation(summary = "删除组织目录")
    public ApiResponse<OperationAckResponse> deleteOrg(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("recordCode") String recordCode
    ) {
        subjectAppService.deleteOrg(tenantId, appCode, recordCode);
        return ApiResponse.success(ack(recordCode, "组织目录删除成功"));
    }

    @GetMapping("/users")
    @Operation(summary = "分页查询用户目录")
    public ApiResponse<PageResponse<UserResponse>> pageUsers(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(
            subjectAssembler.toPageResponse(
                subjectAppService.pageUsers(tenantId, appCode, keyword, pageNo, pageSize),
                subjectAssembler::toResponse
            )
        );
    }

    @PostMapping("/users")
    @Operation(summary = "创建用户目录")
    public ApiResponse<OperationAckResponse> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse response = subjectAssembler.toResponse(
            subjectAppService.createUser(subjectAssembler.toDefinition(request))
        );
        return ApiResponse.success(ack(resolveUserBusinessId(response), "用户目录创建成功"));
    }

    @GetMapping("/users/{recordCode}")
    @Operation(summary = "查询用户目录详情")
    public ApiResponse<UserResponse> getUser(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("recordCode") String recordCode
    ) {
        return ApiResponse.success(
            subjectAssembler.toResponse(subjectAppService.getUser(tenantId, appCode, recordCode))
        );
    }

    @PutMapping("/users/{recordCode}")
    @Operation(summary = "更新用户目录")
    public ApiResponse<OperationAckResponse> updateUser(
        @PathVariable("recordCode") String recordCode,
        @Valid @RequestBody UserRequest request
    ) {
        subjectAppService.updateUser(
            request.getTenantId(),
            request.getAppCode(),
            recordCode,
            subjectAssembler.toDefinition(request)
        );
        return ApiResponse.success(ack(recordCode, "用户目录更新成功"));
    }

    @DeleteMapping("/users/{recordCode}")
    @Operation(summary = "删除用户目录")
    public ApiResponse<OperationAckResponse> deleteUser(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("recordCode") String recordCode
    ) {
        subjectAppService.deleteUser(tenantId, appCode, recordCode);
        return ApiResponse.success(ack(recordCode, "用户目录删除成功"));
    }

    @GetMapping("/positions")
    @Operation(summary = "分页查询岗位目录")
    public ApiResponse<PageResponse<PositionResponse>> pagePositions(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(
            subjectAssembler.toPageResponse(
                subjectAppService.pagePositions(tenantId, appCode, keyword, pageNo, pageSize),
                subjectAssembler::toResponse
            )
        );
    }

    @PostMapping("/positions")
    @Operation(summary = "创建岗位目录")
    public ApiResponse<OperationAckResponse> createPosition(@Valid @RequestBody PositionRequest request) {
        PositionResponse response = subjectAssembler.toResponse(
            subjectAppService.createPosition(subjectAssembler.toDefinition(request))
        );
        return ApiResponse.success(ack(response.getPositionCode(), "岗位目录创建成功"));
    }

    @GetMapping("/positions/{recordCode}")
    @Operation(summary = "查询岗位目录详情")
    public ApiResponse<PositionResponse> getPosition(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("recordCode") String recordCode
    ) {
        return ApiResponse.success(
            subjectAssembler.toResponse(subjectAppService.getPosition(tenantId, appCode, recordCode))
        );
    }

    @PutMapping("/positions/{recordCode}")
    @Operation(summary = "更新岗位目录")
    public ApiResponse<OperationAckResponse> updatePosition(
        @PathVariable("recordCode") String recordCode,
        @Valid @RequestBody PositionRequest request
    ) {
        subjectAppService.updatePosition(
            request.getTenantId(),
            request.getAppCode(),
            recordCode,
            subjectAssembler.toDefinition(request)
        );
        return ApiResponse.success(ack(recordCode, "岗位目录更新成功"));
    }

    @DeleteMapping("/positions/{recordCode}")
    @Operation(summary = "删除岗位目录")
    public ApiResponse<OperationAckResponse> deletePosition(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("recordCode") String recordCode
    ) {
        subjectAppService.deletePosition(tenantId, appCode, recordCode);
        return ApiResponse.success(ack(recordCode, "岗位目录删除成功"));
    }

    @GetMapping("/groups")
    @Operation(summary = "分页查询用户组目录")
    public ApiResponse<PageResponse<UserGroupResponse>> pageUserGroups(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(
            subjectAssembler.toPageResponse(
                subjectAppService.pageUserGroups(tenantId, appCode, keyword, pageNo, pageSize),
                subjectAssembler::toResponse
            )
        );
    }

    @PostMapping("/groups")
    @Operation(summary = "创建用户组目录")
    public ApiResponse<OperationAckResponse> createUserGroup(@Valid @RequestBody UserGroupRequest request) {
        UserGroupResponse response = subjectAssembler.toResponse(
            subjectAppService.createUserGroup(subjectAssembler.toDefinition(request))
        );
        return ApiResponse.success(ack(response.getGroupCode(), "用户组目录创建成功"));
    }

    @GetMapping("/groups/{recordCode}")
    @Operation(summary = "查询用户组目录详情")
    public ApiResponse<UserGroupResponse> getUserGroup(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("recordCode") String recordCode
    ) {
        return ApiResponse.success(
            subjectAssembler.toResponse(subjectAppService.getUserGroup(tenantId, appCode, recordCode))
        );
    }

    @PutMapping("/groups/{recordCode}")
    @Operation(summary = "更新用户组目录")
    public ApiResponse<OperationAckResponse> updateUserGroup(
        @PathVariable("recordCode") String recordCode,
        @Valid @RequestBody UserGroupRequest request
    ) {
        subjectAppService.updateUserGroup(
            request.getTenantId(),
            request.getAppCode(),
            recordCode,
            subjectAssembler.toDefinition(request)
        );
        return ApiResponse.success(ack(recordCode, "用户组目录更新成功"));
    }

    @DeleteMapping("/groups/{recordCode}")
    @Operation(summary = "删除用户组目录")
    public ApiResponse<OperationAckResponse> deleteUserGroup(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("recordCode") String recordCode
    ) {
        subjectAppService.deleteUserGroup(tenantId, appCode, recordCode);
        return ApiResponse.success(ack(recordCode, "用户组目录删除成功"));
    }

    @GetMapping("/roles")
    @Operation(summary = "分页查询角色目录")
    public ApiResponse<PageResponse<RoleResponse>> pageRoles(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(
            subjectAssembler.toPageResponse(
                subjectAppService.pageRoles(tenantId, appCode, keyword, pageNo, pageSize),
                subjectAssembler::toResponse
            )
        );
    }

    @PostMapping("/roles")
    @Operation(summary = "创建角色目录")
    public ApiResponse<OperationAckResponse> createRole(@Valid @RequestBody RoleRequest request) {
        RoleResponse response = subjectAssembler.toResponse(
            subjectAppService.createRole(subjectAssembler.toDefinition(request))
        );
        return ApiResponse.success(ack(response.getRoleCode(), "角色目录创建成功"));
    }

    @GetMapping("/roles/{recordCode}")
    @Operation(summary = "查询角色目录详情")
    public ApiResponse<RoleResponse> getRole(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("recordCode") String recordCode
    ) {
        return ApiResponse.success(
            subjectAssembler.toResponse(subjectAppService.getRole(tenantId, appCode, recordCode))
        );
    }

    @PutMapping("/roles/{recordCode}")
    @Operation(summary = "更新角色目录")
    public ApiResponse<OperationAckResponse> updateRole(
        @PathVariable("recordCode") String recordCode,
        @Valid @RequestBody RoleRequest request
    ) {
        subjectAppService.updateRole(
            request.getTenantId(),
            request.getAppCode(),
            recordCode,
            subjectAssembler.toDefinition(request)
        );
        return ApiResponse.success(ack(recordCode, "角色目录更新成功"));
    }

    @DeleteMapping("/roles/{recordCode}")
    @Operation(summary = "删除角色目录")
    public ApiResponse<OperationAckResponse> deleteRole(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("recordCode") String recordCode
    ) {
        subjectAppService.deleteRole(tenantId, appCode, recordCode);
        return ApiResponse.success(ack(recordCode, "角色目录删除成功"));
    }

    @GetMapping("/associated-items")
    @Operation(summary = "跨类型关联查询（主体关系表）",
        description = "给定来源主体实例，基于引擎库 authz_subject_relation 查询与之关联的另一类型实例列表，如角色→用户、组织→岗位、用户组→组织")
    public ApiResponse<PageResponse<DataItem>> pageAssociatedItems(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @Parameter(description = "来源模型编码，如 SUB_ROLE / SUB_ORG")
        @RequestParam("sourceModel") @NotBlank(message = "sourceModel 不能为空") String sourceModel,
        @Parameter(description = "来源实例 ID（数据库主键）")
        @RequestParam("sourceId") @NotBlank(message = "sourceId 不能为空") String sourceId,
        @Parameter(description = "目标模型编码，如 SUB_USER / SUB_ORG")
        @RequestParam("targetModel") @NotBlank(message = "targetModel 不能为空") String targetModel,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        PageResult<DataItem> result = subjectAppService.pageAssociatedSubjectItems(
            tenantId, appCode, sourceModel, sourceId, targetModel, keyword, pageNo, pageSize);
        return ApiResponse.success(
            PageResponse.<DataItem>builder()
                .pageNo(result.getPageNo())
                .pageSize(result.getPageSize())
                .total(result.getTotal())
                .records(result.getRecords())
                .build()
        );
    }

    @GetMapping("/relations")
    @Operation(summary = "分页查询主体关系")
    public ApiResponse<PageResponse<SubjectRelationResponse>> pageSubjectRelations(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(
            subjectAssembler.toPageResponse(
                subjectAppService.pageSubjectRelations(tenantId, appCode, keyword, pageNo, pageSize),
                subjectAssembler::toResponse
            )
        );
    }

    @PostMapping("/relations")
    @Operation(summary = "创建主体关系")
    public ApiResponse<OperationAckResponse> createSubjectRelation(@Valid @RequestBody SubjectRelationRequest request) {
        AuthSubjectRelation relation = subjectAppService.createSubjectRelation(
            subjectAssembler.toDefinition(request)
        );
        return ApiResponse.success(ack(String.valueOf(relation.getId()), "主体关系创建成功"));
    }

    @GetMapping("/relations/{relationId}")
    @Operation(summary = "查询主体关系详情")
    public ApiResponse<SubjectRelationResponse> getSubjectRelation(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("relationId") Long relationId
    ) {
        return ApiResponse.success(
            subjectAssembler.toResponse(subjectAppService.getSubjectRelation(tenantId, appCode, relationId))
        );
    }

    @PutMapping("/relations/{relationId}")
    @Operation(summary = "更新主体关系")
    public ApiResponse<OperationAckResponse> updateSubjectRelation(
        @PathVariable("relationId") Long relationId,
        @Valid @RequestBody SubjectRelationRequest request
    ) {
        subjectAppService.updateSubjectRelation(
            request.getTenantId(),
            request.getAppCode(),
            relationId,
            subjectAssembler.toDefinition(request)
        );
        return ApiResponse.success(ack(String.valueOf(relationId), "主体关系更新成功"));
    }

    @DeleteMapping("/relations/{relationId}")
    @Operation(summary = "删除主体关系")
    public ApiResponse<OperationAckResponse> deleteSubjectRelation(
        @RequestParam("tenantId") @NotBlank(message = "tenantId 不能为空") String tenantId,
        @RequestParam("appCode") @NotBlank(message = "appCode 不能为空") String appCode,
        @PathVariable("relationId") Long relationId
    ) {
        subjectAppService.deleteSubjectRelation(tenantId, appCode, relationId);
        return ApiResponse.success(ack(String.valueOf(relationId), "主体关系删除成功"));
    }

    private OperationAckResponse ack(String businessId, String note) {
        return OperationAckResponse.builder()
            .accepted(true)
            .businessId(businessId)
            .note(note)
            .build();
    }

    private String resolveUserBusinessId(UserResponse response) {
        if (response == null) {
            return null;
        }
        if (StringUtils.hasText(response.getStaffNo())) {
            return response.getStaffNo();
        }
        return response.getUserId();
    }
}