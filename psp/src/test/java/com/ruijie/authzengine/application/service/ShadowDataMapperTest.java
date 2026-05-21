package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.application.spi.DataItem;
import com.ruijie.authzengine.application.spi.ModelFieldSchema;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.subject.AuthRole;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserAccount;
import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import com.ruijie.authzengine.shared.exception.BusinessException;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ShadowDataMapperTest {

    // ────────── resolveSchema ──────────

    @Test
    void resolveSchema_shouldReturnDefaultWhenSchemaViewIsEmpty() {
        ModelFieldSchema schema = ShadowDataMapper.resolveSchema("SUB_USER", null);

        Assertions.assertNotNull(schema);
        Assertions.assertFalse(schema.getFields().isEmpty());
        // 内置默认包含 ID、编码、名称、状态字段
        boolean hasId = schema.getFields().stream().anyMatch(f -> "ID".equals(f.getRole()));
        boolean hasCode = schema.getFields().stream().anyMatch(f -> "CODE".equals(f.getRole()));
        boolean hasName = schema.getFields().stream().anyMatch(f -> "NAME".equals(f.getRole()));
        Assertions.assertTrue(hasId);
        Assertions.assertTrue(hasCode);
        Assertions.assertTrue(hasName);
    }

    @Test
    void resolveSchema_shouldReturnDefaultWhenSchemaViewIsBlank() {
        ModelFieldSchema schema = ShadowDataMapper.resolveSchema("SUB_ROLE", "  ");

        Assertions.assertNotNull(schema);
        boolean hasCode = schema.getFields().stream().anyMatch(f -> "CODE".equals(f.getRole()));
        Assertions.assertTrue(hasCode);
    }

    @Test
    void resolveSchema_shouldParseCustomSchemaView() {
        String json = "{\"fields\":[{\"code\":\"uid\",\"type\":\"STRING\",\"role\":\"CODE\","
            + "\"domainField\":\"roleCode\",\"label\":\"角色编码\",\"required\":true}]}";

        ModelFieldSchema schema = ShadowDataMapper.resolveSchema("SUB_ROLE", json);

        Assertions.assertNotNull(schema);
        Assertions.assertEquals(1, schema.getFields().size());
        Assertions.assertEquals("uid", schema.getFields().get(0).getCode());
        Assertions.assertEquals("roleCode", schema.getFields().get(0).getDomainField());
    }

    @Test
    void resolveSchema_shouldThrowForUnknownModelCodeWithEmptyView() {
        Assertions.assertThrows(BusinessException.class,
            () -> ShadowDataMapper.resolveSchema("UNKNOWN_MODEL", null));
    }

    // ────────── toModel ──────────

    @Test
    void toModel_shouldMapIdCodeNameAndStatusForUser() {
        ModelFieldSchema schema = ShadowDataMapper.resolveSchema("SUB_USER", null);
        DataItem item = DataItem.builder()
            .id("100")
            .code("U100")
            .name("张三")
            .status("ENABLED")
            .build();

        SysUserAccount user = ShadowDataMapper.toModel(item, SysUserAccount.class, schema, "T001", "CRM");

        Assertions.assertEquals(100L, user.getId());
        Assertions.assertEquals("U100", user.getStaffNo());
        Assertions.assertEquals("张三", user.getStaffName());
        Assertions.assertEquals("ENABLED", user.getStatus());
        Assertions.assertEquals("T001", user.getTenantId());
        Assertions.assertEquals("CRM", user.getAppCode());
    }

    @Test
    void toModel_shouldMapCodeAndNameForRole() {
        ModelFieldSchema schema = ShadowDataMapper.resolveSchema("SUB_ROLE", null);
        DataItem item = DataItem.builder()
            .code("ROLE-SALES")
            .name("销售角色")
            .status("ENABLED")
            .build();

        AuthRole role = ShadowDataMapper.toModel(item, AuthRole.class, schema, "T001", "CRM");

        Assertions.assertEquals("ROLE-SALES", role.getRoleCode());
        Assertions.assertEquals("销售角色", role.getRoleName());
        Assertions.assertEquals("ENABLED", role.getStatus());
    }

    @Test
    void toModel_shouldStoreUnknownFieldsAsAttributes() {
        ModelFieldSchema schema = ShadowDataMapper.resolveSchema("SUB_USER", null);
        java.util.Map<String, Object> ext = new java.util.HashMap<>();
        ext.put("department", "技术部");
        ext.put("employeeId", "EMP-001");
        DataItem item = DataItem.builder()
            .code("U200")
            .name("李四")
            .status("ENABLED")
            .attributes(ext)
            .build();

        SysUserAccount user = ShadowDataMapper.toModel(item, SysUserAccount.class, schema, "T001", "CRM");

        Assertions.assertNotNull(user.getAttributes());
        Assertions.assertEquals("技术部", user.getAttributes().get("department"));
        Assertions.assertEquals("EMP-001", user.getAttributes().get("employeeId"));
    }

    @Test
    void toModel_shouldMapApiWithDomainFields() {
        ModelFieldSchema schema = ShadowDataMapper.resolveSchema("RES_API", null);
        java.util.Map<String, Object> ext = new java.util.HashMap<>();
        ext.put("httpMethod", "POST");
        ext.put("uriPattern", "/api/users");
        DataItem item = DataItem.builder()
            .code("API-USER-CREATE")
            .name("创建用户接口")
            .status("ENABLED")
            .attributes(ext)
            .build();

        SysResApi api = ShadowDataMapper.toModel(item, SysResApi.class, schema, "T001", "CRM");

        Assertions.assertEquals("API-USER-CREATE", api.getApiCode());
        Assertions.assertEquals("创建用户接口", api.getApiName());
        Assertions.assertEquals("POST", api.getHttpMethod());
        Assertions.assertEquals("/api/users", api.getUriPattern());
    }

    @Test
    void toModel_shouldThrowWhenCodeIsMissing() {
        ModelFieldSchema schema = ShadowDataMapper.resolveSchema("SUB_ROLE", null);
        DataItem item = DataItem.builder()
            .name("无编码角色")
            .build();

        Assertions.assertThrows(BusinessException.class,
            () -> ShadowDataMapper.toModel(item, AuthRole.class, schema, "T001", "CRM"));
    }

    // ────────── toModelPage ──────────

    @Test
    void toModelPage_shouldMapPageResultCorrectly() {
        ModelFieldSchema schema = ShadowDataMapper.resolveSchema("SUB_USER", null);
        DataItem item = DataItem.builder()
            .code("U300")
            .name("王五")
            .status("ENABLED")
            .build();
        PageResult<DataItem> raw = PageResult.<DataItem>builder()
            .pageNo(1)
            .pageSize(10)
            .total(1L)
            .records(Collections.singletonList(item))
            .build();

        PageResult<SysUserAccount> result =
            ShadowDataMapper.toModelPage(raw, SysUserAccount.class, schema, "T001", "CRM");

        Assertions.assertEquals(1, result.getTotal());
        Assertions.assertEquals(1, result.getRecords().size());
        Assertions.assertEquals("U300", result.getRecords().get(0).getStaffNo());
    }

    // ────────── fromModel ──────────

    @Test
    void fromModel_shouldConvertRoleToDataItem() {
        ModelFieldSchema schema = ShadowDataMapper.resolveSchema("SUB_ROLE", null);
        AuthRole role = AuthRole.builder()
            .id(99L)
            .roleCode("ROLE-ADMIN")
            .roleName("管理员")
            .status("ENABLED")
            .build();

        DataItem item = ShadowDataMapper.fromModel(role, schema);

        Assertions.assertEquals("99", item.getId());
        Assertions.assertEquals("ROLE-ADMIN", item.getCode());
        Assertions.assertEquals("管理员", item.getName());
        Assertions.assertEquals("ENABLED", item.getStatus());
    }

    @Test
    void fromModel_shouldIncludeExtrasForNonRoleFields() {
        ModelFieldSchema schema = ShadowDataMapper.resolveSchema("RES_API", null);
        SysResApi api = SysResApi.builder()
            .apiCode("API-LIST")
            .apiName("列表接口")
            .httpMethod("GET")
            .uriPattern("/api/list")
            .status("ENABLED")
            .build();

        DataItem item = ShadowDataMapper.fromModel(api, schema);

        Assertions.assertEquals("API-LIST", item.getCode());
        Assertions.assertNotNull(item.getAttributes());
        Assertions.assertEquals("GET", item.getAttributes().get("httpMethod"));
        Assertions.assertEquals("/api/list", item.getAttributes().get("uriPattern"));
    }
}
