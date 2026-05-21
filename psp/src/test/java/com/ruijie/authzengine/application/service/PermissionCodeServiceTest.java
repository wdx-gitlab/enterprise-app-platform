package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.resource.SysResApi;
import com.ruijie.authzengine.domain.repository.MetaRepository;
import com.ruijie.authzengine.domain.repository.ResourceRepository;
import com.ruijie.authzengine.shared.exception.BusinessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PermissionCodeServiceTest {

    @Test
    void shouldGenerateBoPermissionCodeFromBoId() {
        PermissionCodeService service = new PermissionCodeService(new StubMetaRepository(), new StubResourceRepository());

        String permCode = service.generatePermissionCode("T001", "CRM", "RES_DATA_BO", "900", "read");

        Assertions.assertEquals("CRM:bo:CUSTOMER:READ", permCode);
    }

    @Test
    void shouldGenerateApiPermissionCodeFromApiId() {
        PermissionCodeService service = new PermissionCodeService(new StubMetaRepository(), new StubResourceRepository());

        String permCode = service.generatePermissionCode("T001", "CRM", "RES_API", "12", "list");

        Assertions.assertEquals("CRM:api:customer.query:LIST", permCode);
    }

    @Test
    void shouldKeepThirdSegmentEmptyForModelLevelPermission() {
        PermissionCodeService service = new PermissionCodeService(new StubMetaRepository(), new StubResourceRepository());

        String permCode = service.generatePermissionCode("T001", "CRM", "RES_DATA_BO", "", "READ");

        Assertions.assertEquals("CRM:bo::READ", permCode);
    }

    @Test
    void shouldParseFourSegmentPermissionCode() {
        PermissionCodeService service = new PermissionCodeService(new StubMetaRepository(), new StubResourceRepository());

        PermissionCodeService.ParsedPermissionCode parsed = service.parsePermissionCode("CRM:api:customer.query:LIST");

        Assertions.assertEquals("CRM", parsed.getAppCode());
        Assertions.assertEquals("api", parsed.getResourceKind());
        Assertions.assertEquals("customer.query", parsed.getResourceCode());
        Assertions.assertEquals("LIST", parsed.getActionCode());
    }

    @Test
    void shouldResolveBoCodeFromFourSegmentPermissionCode() {
        PermissionCodeService service = new PermissionCodeService(new StubMetaRepository(), new StubResourceRepository());

        String boCode = service.resolveBoCodeFromPermissionCode("CRM:bo:CUSTOMER:READ");

        Assertions.assertEquals("CUSTOMER", boCode);
    }

    @Test
    void shouldRejectLegacyThreeSegmentPermissionCode() {
        PermissionCodeService service = new PermissionCodeService(new StubMetaRepository(), new StubResourceRepository());

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> service.parsePermissionCode("CRM:CUSTOMER:READ"));

        Assertions.assertEquals("AUTHZ-400", exception.getCode());
    }

    private static class StubMetaRepository implements MetaRepository {

        @Override
        public AuthMetaModelDefinition saveAuthMetaModel(AuthMetaModelDefinition definition) {
            return definition;
        }

        @Override
        public BoMetaModelDefinition saveBoMetaModel(BoMetaModelDefinition definition) {
            return definition;
        }

        @Override
        public BoMetaModelDefinition findBoMetaModel(String tenantId, String appCode, String boCode) {
            if (!"CUSTOMER".equalsIgnoreCase(boCode)) {
                return null;
            }
            return BoMetaModelDefinition.builder()
                .id(900L)
                .tenantId(tenantId)
                .appCode(appCode)
                .boCode("CUSTOMER")
                .boName("客户")
                .build();
        }

        @Override
        public BoMetaModelDefinition findBoMetaModelById(String tenantId, String appCode, Long boId) {
            if (!Long.valueOf(900L).equals(boId)) {
                return null;
            }
            return BoMetaModelDefinition.builder()
                .id(boId)
                .tenantId(tenantId)
                .appCode(appCode)
                .boCode("CUSTOMER")
                .boName("客户")
                .build();
        }
    }

    private static class StubResourceRepository implements ResourceRepository {

        @Override
        public SysResApi findApi(String tenantId, String appCode, String apiCode) {
            if (!"customer.query".equalsIgnoreCase(apiCode)) {
                return null;
            }
            return SysResApi.builder()
                .id(12L)
                .tenantId(tenantId)
                .appCode(appCode)
                .apiCode("customer.query")
                .apiName("客户查询")
                .build();
        }

        @Override
        public SysResApi findApiById(String tenantId, String appCode, Long apiId) {
            if (!Long.valueOf(12L).equals(apiId)) {
                return null;
            }
            return SysResApi.builder()
                .id(apiId)
                .tenantId(tenantId)
                .appCode(appCode)
                .apiCode("customer.query")
                .apiName("客户查询")
                .build();
        }
    }
}