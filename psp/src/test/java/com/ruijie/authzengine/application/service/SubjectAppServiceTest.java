package com.ruijie.authzengine.application.service;

import com.ruijie.authzengine.application.spi.AuthMetaModelAdapter;
import com.ruijie.authzengine.application.spi.DataItem;
import com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.subject.AuthRole;
import com.ruijie.authzengine.domain.model.governance.subject.AuthSubjectRelation;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserAccount;
import com.ruijie.authzengine.domain.repository.MetaRepository;
import com.ruijie.authzengine.domain.repository.SubjectRepository;
import com.ruijie.authzengine.infrastructure.authz.AuthMetaResolverRouter;
import com.ruijie.authzengine.shared.exception.BusinessException;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SubjectAppServiceTest {

    private static final MetaRepository NOOP_META_REPO = new MetaRepository() {
        @Override public com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition saveAuthMetaModel(com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition d) { return d; }
        @Override public com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition saveBoMetaModel(com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition d) { return d; }
    };

    @Test
    void shouldRejectDeletingReferencedRole() {
        InMemorySubjectRepository repository = new InMemorySubjectRepository();
        repository.role = AuthRole.builder()
            .tenantId("T001")
            .appCode("CRM")
            .roleCode("ROLE-SALES")
            .roleName("销售角色")
            .status("ENABLED")
            .build();
        repository.roleReferenced = true;
        SubjectAppService subjectAppService = new SubjectAppService(
            repository, NOOP_META_REPO, AuthMetaResolverRouter.noop());

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> subjectAppService.deleteRole("T001", "CRM", "ROLE-SALES"));

        Assertions.assertEquals("AUTHZ-409-DELETE", exception.getCode());
    }

    @Test
    void shouldRequireRelationRecreateWhenIdentityChanges() {
        InMemorySubjectRepository repository = new InMemorySubjectRepository();
        repository.relation = AuthSubjectRelation.builder()
            .id(1L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectModel("SUB_USER")
            .subjectId("U100")
            .relatedSubjectModel("SUB_ROLE")
            .relatedSubjectId("ROLE-SALES")
            .relationType("MEMBER")
            .build();
        SubjectAppService subjectAppService = new SubjectAppService(
            repository, NOOP_META_REPO, AuthMetaResolverRouter.noop());

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> subjectAppService.updateSubjectRelation("T001", "CRM", 1L, AuthSubjectRelation.builder()
                .subjectModel("SUB_USER")
                .subjectId("U101")
                .relatedSubjectModel("SUB_ROLE")
                .relatedSubjectId("ROLE-SALES")
                .relationType("MEMBER")
                .build()));

        Assertions.assertEquals("AUTHZ-409-RELATION", exception.getCode());
    }

    @Test
    void shouldRejectUserToUserSubjectRelation() {
        InMemorySubjectRepository repository = new InMemorySubjectRepository();
        SubjectAppService subjectAppService = new SubjectAppService(
            repository, NOOP_META_REPO, AuthMetaResolverRouter.noop());

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
            () -> subjectAppService.createSubjectRelation(AuthSubjectRelation.builder()
                .tenantId("T001")
                .appCode("CRM")
                .subjectModel("SUB_USER")
                .subjectId("U100")
                .relatedSubjectModel("SUB_USER")
                .relatedSubjectId("U101")
                .relationType("MEMBER_OF")
                .build()));

        Assertions.assertEquals("AUTHZ-400", exception.getCode());
        Assertions.assertEquals("主体关系的关联主体模型不支持 SUB_USER，用户间授权请走委托模型", exception.getMessage());
    }

    @Test
    void shouldPageUsers() {
        InMemorySubjectRepository repository = new InMemorySubjectRepository();
        repository.user = SysUserAccount.builder()
            .tenantId("T001")
            .appCode("CRM")
            .staffNo("U100")
            .userId("zhangsan")
            .staffName("张三")
            .status("ENABLED")
            .build();
        SubjectAppService subjectAppService = new SubjectAppService(
            repository, NOOP_META_REPO, AuthMetaResolverRouter.noop());

        PageResult<SysUserAccount> result = subjectAppService.pageUsers("T001", "CRM", "U1", 1, 20);

        Assertions.assertEquals(1, result.getTotal());
        Assertions.assertEquals("U100", result.getRecords().get(0).getStaffNo());
    }

    @Test
    void shouldPageAssociatedUsersWhenQueryingByRole() {
        InMemorySubjectRepository repository = new InMemorySubjectRepository();
        repository.user = SysUserAccount.builder()
            .id(100L)
            .tenantId("T001")
            .appCode("CRM")
            .staffNo("U100")
            .userId("zhangsan")
            .staffName("张三")
            .status("ENABLED")
            .build();
        repository.relations = Collections.singletonList(AuthSubjectRelation.builder()
            .id(1L)
            .tenantId("T001")
            .appCode("CRM")
            .subjectModel("SUB_USER")
            .subjectId("100")
            .relatedSubjectModel("SUB_ROLE")
            .relatedSubjectId("3005")
            .relationType("ROLE")
            .build());
        SubjectAppService subjectAppService = new SubjectAppService(
            repository, NOOP_META_REPO, AuthMetaResolverRouter.noop());

        PageResult<DataItem> result = subjectAppService.pageAssociatedSubjectItems(
            "T001", "CRM", "SUB_ROLE", "3005", "SUB_USER", null, 1, 20);

        Assertions.assertEquals(1, result.getTotal());
        Assertions.assertEquals("U100", result.getRecords().get(0).getCode());
        Assertions.assertEquals("张三", result.getRecords().get(0).getName());
    }

    @Test
    void shouldRoutePageUsersToAdapterInShadowMode() {
        InMemorySubjectRepository repository = new InMemorySubjectRepository();
        // 配置 Shadow Mode 元模型（resolver 非 noopHook）
        MetaRepository shadowMetaRepo = new MetaRepository() {
            @Override public com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition
                saveAuthMetaModel(com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition d) { return d; }
            @Override public com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition
                saveBoMetaModel(com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition d) { return d; }
            @Override public AuthMetaModelDefinition findAuthMetaModel(String tenantId, String appCode, String modelCode) {
                if ("SUB_USER".equals(modelCode)) {
                    return AuthMetaModelDefinition.builder()
                        .tenantId(tenantId).appCode(appCode).modelCode(modelCode)
                        .adapterType("JAVA_BEAN").resolver("testUserAdapter").build();
                }
                return null;
            }
        };
        // 适配器返回测试数据
        AuthMetaModelAdapter testAdapter = new AuthMetaModelAdapter() {
            @Override public com.ruijie.authzengine.application.spi.SubjectHookResult
                fetchInstanceAttributes(com.ruijie.authzengine.application.spi.ModelCode modelCode, String subjectId, Map<String, Object> ctx) { return null; }
            @Override public PageResult<DataItem> pageItems(
                com.ruijie.authzengine.application.spi.ModelCode modelCode, Map<String, String> queryParams, int pageNo, int pageSize) {
                return PageResult.<DataItem>builder()
                    .pageNo(pageNo).pageSize(pageSize).total(1L)
                    .records(Collections.singletonList(
                        DataItem.builder().code("U-SHADOW").name("Shadow用户").status("ENABLED").build()))
                    .build();
            }
        };
        AuthMetaResolverRouter router = new AuthMetaResolverRouter(null) {
            @Override public AuthMetaModelAdapter resolve(String adapterType, String resolver) { return testAdapter; }
        };
        SubjectAppService service = new SubjectAppService(repository, shadowMetaRepo, router);

        PageResult<SysUserAccount> result = service.pageUsers("T001", "CRM", null, 1, 20);

        Assertions.assertEquals(1L, result.getTotal());
        Assertions.assertEquals("U-SHADOW", result.getRecords().get(0).getStaffNo());
        Assertions.assertEquals("Shadow用户", result.getRecords().get(0).getStaffName());
    }

    @Test
    void shouldRouteGetRoleToAdapterInShadowMode() {
        InMemorySubjectRepository repository = new InMemorySubjectRepository();
        MetaRepository shadowMetaRepo = new MetaRepository() {
            @Override public com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition
                saveAuthMetaModel(com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition d) { return d; }
            @Override public com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition
                saveBoMetaModel(com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition d) { return d; }
            @Override public AuthMetaModelDefinition findAuthMetaModel(String tenantId, String appCode, String modelCode) {
                if ("SUB_ROLE".equals(modelCode)) {
                    return AuthMetaModelDefinition.builder()
                        .tenantId(tenantId).appCode(appCode).modelCode(modelCode)
                        .adapterType("JAVA_BEAN").resolver("testRoleAdapter").build();
                }
                return null;
            }
        };
        AuthMetaModelAdapter testAdapter = new AuthMetaModelAdapter() {
            @Override public com.ruijie.authzengine.application.spi.SubjectHookResult
                fetchInstanceAttributes(com.ruijie.authzengine.application.spi.ModelCode modelCode, String subjectId, Map<String, Object> ctx) { return null; }
            @Override public DataItem getItem(com.ruijie.authzengine.application.spi.ModelCode modelCode, String itemId) {
                return DataItem.builder()
                    .code("ROLE-SHADOW").name("Shadow角色").status("ENABLED").build();
            }
        };
        AuthMetaResolverRouter router = new AuthMetaResolverRouter(null) {
            @Override public AuthMetaModelAdapter resolve(String adapterType, String resolver) { return testAdapter; }
        };
        SubjectAppService service = new SubjectAppService(repository, shadowMetaRepo, router);

        AuthRole role = service.getRole("T001", "CRM", "ROLE-SHADOW");

        Assertions.assertEquals("ROLE-SHADOW", role.getRoleCode());
        Assertions.assertEquals("Shadow角色", role.getRoleName());
    }

    private static final class InMemorySubjectRepository implements SubjectRepository {

        private AuthRole role;

        private boolean roleReferenced;

        private AuthSubjectRelation relation;

        private java.util.List<AuthSubjectRelation> relations = Collections.emptyList();

        private SysUserAccount user;

        @Override
        public AuthRole findRole(String tenantId, String appCode, String roleCode) {
            return role;
        }

        @Override
        public boolean hasRoleReference(String tenantId, String appCode, String roleCode) {
            return roleReferenced;
        }

        @Override
        public AuthSubjectRelation findSubjectRelation(String tenantId, String appCode, Long relationId) {
            return relation;
        }

        @Override
        public AuthSubjectRelation saveSubjectRelation(AuthSubjectRelation authSubjectRelation) {
            this.relation = authSubjectRelation;
            return authSubjectRelation;
        }

        @Override
        public PageResult<AuthSubjectRelation> pageSubjectRelations(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
            return PageResult.<AuthSubjectRelation>builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .total(relations.size())
                .records(relations)
                .build();
        }

        @Override
        public PageResult<SysUserAccount> pageUsers(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
            return PageResult.<SysUserAccount>builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .total(user == null ? 0 : 1)
                .records(user == null ? java.util.Collections.emptyList() : java.util.Collections.singletonList(user))
                .build();
        }

        @Override
        public SysUserAccount saveUser(SysUserAccount userAccount) {
            this.user = userAccount;
            return userAccount;
        }

        @Override
        public java.util.List<SysUserAccount> listUsers(String tenantId, String appCode) {
            return user == null ? Collections.emptyList() : Collections.singletonList(user);
        }
    }
}