package com.ruijie.authzengine.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.api.assembler.SubjectAssembler;
import com.ruijie.authzengine.application.service.SubjectAppService;
import com.ruijie.authzengine.domain.model.governance.PageResult;
import com.ruijie.authzengine.domain.model.governance.subject.AuthRole;
import com.ruijie.authzengine.domain.model.governance.subject.AuthSubjectRelation;
import com.ruijie.authzengine.domain.model.governance.subject.SysOrgNode;
import com.ruijie.authzengine.domain.model.governance.subject.SysPosition;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserAccount;
import com.ruijie.authzengine.domain.model.governance.subject.SysUserGroup;
import com.ruijie.authzengine.domain.repository.SubjectRepository;
import com.ruijie.authzengine.shared.exception.GlobalExceptionHandler;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SubjectControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    private InMemorySubjectRepository repository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        repository = new InMemorySubjectRepository();
        mockMvc = buildMockMvc(repository);
    }

    @Test
    void shouldCrudSubjectDirectories() throws Exception {
        mockMvc.perform(post("/authz-engine/api/v1/governance/subjects/orgs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"departmentCode\":\"ORG-SALES\",\"departmentName\":\"销售组织\",\"status\":\"ENABLED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.businessId").value("ORG-SALES"));

        mockMvc.perform(post("/authz-engine/api/v1/governance/subjects/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"staffNo\":\"U100\",\"userId\":\"zhangsan\",\"staffName\":\"张三\",\"orgCode\":\"ORG-SALES\",\"status\":\"ENABLED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.businessId").value("U100"));

        mockMvc.perform(post("/authz-engine/api/v1/governance/subjects/positions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"positionCode\":\"POS-SALES\",\"positionName\":\"销售经理\",\"orgCode\":\"ORG-SALES\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.businessId").value("POS-SALES"));

        mockMvc.perform(post("/authz-engine/api/v1/governance/subjects/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"groupCode\":\"GROUP-SALES\",\"groupName\":\"销售组\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.businessId").value("GROUP-SALES"));

        mockMvc.perform(post("/authz-engine/api/v1/governance/subjects/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"roleCode\":\"ROLE-SALES\",\"roleName\":\"销售角色\",\"roleScope\":\"APP\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.businessId").value("ROLE-SALES"));

        mockMvc.perform(post("/authz-engine/api/v1/governance/subjects/relations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"subjectModel\":\"SUB_USER\",\"subjectId\":\"U100\",\"relatedSubjectModel\":\"SUB_ROLE\",\"relatedSubjectId\":\"ROLE-SALES\",\"relationType\":\"MEMBER\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.businessId").value("6"));

        mockMvc.perform(get("/authz-engine/api/v1/governance/subjects/orgs")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.records[0].departmentCode").value("ORG-SALES"));

        mockMvc.perform(get("/authz-engine/api/v1/governance/subjects/users/U100")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.staffName").value("张三"));

        mockMvc.perform(put("/authz-engine/api/v1/governance/subjects/users/U100")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"staffNo\":\"U100\",\"userId\":\"zhangsan\",\"staffName\":\"张三-更新\",\"orgCode\":\"ORG-SALES\",\"status\":\"ENABLED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.businessId").value("U100"));

        mockMvc.perform(get("/authz-engine/api/v1/governance/subjects/relations")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.records[0].relationType").value("MEMBER"));
    }

    @Test
    void shouldCreateUserWithStaffNoAndQueryByUserId() throws Exception {
        mockMvc.perform(post("/authz-engine/api/v1/governance/subjects/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"staffNo\":\"R13174\",\"userId\":\"wangdaoxin\",\"staffName\":\"王道鑫\",\"departmentCode\":\"000023662002\",\"status\":\"ENABLED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.businessId").value("R13174"));

        mockMvc.perform(get("/authz-engine/api/v1/governance/subjects/users/wangdaoxin")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.staffNo").value("R13174"))
            .andExpect(jsonPath("$.data.userId").value("wangdaoxin"))
            .andExpect(jsonPath("$.data.staffName").value("\u738b\u9053\u946b"))
            .andExpect(jsonPath("$.data.departmentCode").value("000023662002"));
    }

    @Test
    void shouldRejectDeletingReferencedUser() throws Exception {
        repository.saveUser(SysUserAccount.builder()
            .tenantId("T001")
            .appCode("CRM")
            .staffNo("U100")
            .userId("zhangsan")
            .staffName("张三")
            .status("ENABLED")
            .build());
        repository.userReferences.add("U100");

        mockMvc.perform(delete("/authz-engine/api/v1/governance/subjects/users/U100")
                .param("tenantId", "T001")
                .param("appCode", "CRM"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("AUTHZ-409-DELETE"))
            .andExpect(jsonPath("$.message", containsString("用户仍被关系、授权或委托引用")));
    }

    @Test
    void shouldRequireRelationRecreateWhenIdentityChanges() throws Exception {
        repository.saveSubjectRelation(AuthSubjectRelation.builder()
            .tenantId("T001")
            .appCode("CRM")
            .subjectModel("SUB_USER")
            .subjectId("U100")
            .relatedSubjectModel("SUB_ROLE")
            .relatedSubjectId("ROLE-SALES")
            .relationType("MEMBER")
            .build());

        mockMvc.perform(put("/authz-engine/api/v1/governance/subjects/relations/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"T001\",\"appCode\":\"CRM\",\"subjectModel\":\"SUB_USER\",\"subjectId\":\"U101\",\"relatedSubjectModel\":\"SUB_ROLE\",\"relatedSubjectId\":\"ROLE-SALES\",\"relationType\":\"MEMBER\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("AUTHZ-409-RELATION"))
            .andExpect(jsonPath("$.message", containsString("主体关系身份字段变更必须删除后重建")));
    }

    private MockMvc buildMockMvc(SubjectRepository subjectRepository) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        SubjectController controller = new SubjectController(
            new SubjectAppService(subjectRepository,
                new com.ruijie.authzengine.domain.repository.MetaRepository() {
                    @Override public com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition saveAuthMetaModel(com.ruijie.authzengine.domain.model.governance.AuthMetaModelDefinition d) { return d; }
                    @Override public com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition saveBoMetaModel(com.ruijie.authzengine.domain.model.governance.BoMetaModelDefinition d) { return d; }
                },
                com.ruijie.authzengine.infrastructure.authz.AuthMetaResolverRouter.noop()),
            new SubjectAssembler()
        );
        return MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
    }

    private static final class InMemorySubjectRepository implements SubjectRepository {

        private final AtomicLong idGenerator = new AtomicLong(1L);

        private final Map<String, SysOrgNode> orgs = new LinkedHashMap<>();

        private final Map<String, SysUserAccount> users = new LinkedHashMap<>();

        private final Map<String, SysPosition> positions = new LinkedHashMap<>();

        private final Map<String, SysUserGroup> groups = new LinkedHashMap<>();

        private final Map<String, AuthRole> roles = new LinkedHashMap<>();

        private final Map<Long, AuthSubjectRelation> relations = new LinkedHashMap<>();

        private final LinkedHashSet<String> userReferences = new LinkedHashSet<>();

        @Override
        public SysOrgNode saveOrg(SysOrgNode sysOrgNode) {
            if (sysOrgNode.getId() == null) {
                sysOrgNode.setId(idGenerator.getAndIncrement());
            }
            orgs.put(sysOrgNode.getDepartmentCode(), sysOrgNode);
            return sysOrgNode;
        }

        @Override
        public PageResult<SysOrgNode> pageOrgs(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
            return page(filterByTenant(orgs.values(), tenantId, appCode));
        }

        @Override
        public SysOrgNode findOrg(String tenantId, String appCode, String orgCode) {
            return orgs.get(orgCode);
        }

        @Override
        public void deleteOrg(String tenantId, String appCode, String orgCode) {
            orgs.remove(orgCode);
        }

        @Override
        public SysUserAccount saveUser(SysUserAccount userAccount) {
            if (userAccount.getId() == null) {
                userAccount.setId(idGenerator.getAndIncrement());
            }
            indexUser(userAccount);
            return userAccount;
        }

        @Override
        public PageResult<SysUserAccount> pageUsers(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
            return page(filterByTenant(new java.util.ArrayList<>(new java.util.LinkedHashSet<>(users.values())), tenantId, appCode));
        }

        @Override
        public SysUserAccount findUser(String tenantId, String appCode, String subjectKey) {
            return users.get(subjectKey);
        }

        @Override
        public void deleteUser(String tenantId, String appCode, String subjectKey) {
            SysUserAccount userAccount = users.get(subjectKey);
            if (userAccount == null) {
                return;
            }
            for (String identifier : resolveUserIdentifiers(userAccount)) {
                users.remove(identifier);
            }
        }

        @Override
        public boolean hasUserReference(String tenantId, String appCode, String subjectKey) {
            SysUserAccount userAccount = users.get(subjectKey);
            if (userAccount == null) {
                return userReferences.contains(subjectKey);
            }
            return resolveUserIdentifiers(userAccount).stream().anyMatch(userReferences::contains);
        }

        private void indexUser(SysUserAccount userAccount) {
            for (String identifier : resolveUserIdentifiers(userAccount)) {
                users.put(identifier, userAccount);
            }
        }

        private List<String> resolveUserIdentifiers(SysUserAccount userAccount) {
            java.util.LinkedHashSet<String> identifiers = new java.util.LinkedHashSet<>();
            addIfHasText(identifiers, userAccount.getStaffNo());
            addIfHasText(identifiers, userAccount.getUserId());
            return new java.util.ArrayList<>(identifiers);
        }

        private void addIfHasText(java.util.Set<String> identifiers, String value) {
            if (value != null && !value.trim().isEmpty()) {
                identifiers.add(value);
            }
        }

        @Override
        public SysPosition savePosition(SysPosition sysPosition) {
            if (sysPosition.getId() == null) {
                sysPosition.setId(idGenerator.getAndIncrement());
            }
            positions.put(sysPosition.getPositionCode(), sysPosition);
            return sysPosition;
        }

        @Override
        public PageResult<SysPosition> pagePositions(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
            return page(filterByTenant(positions.values(), tenantId, appCode));
        }

        @Override
        public SysPosition findPosition(String tenantId, String appCode, String positionCode) {
            return positions.get(positionCode);
        }

        @Override
        public void deletePosition(String tenantId, String appCode, String positionCode) {
            positions.remove(positionCode);
        }

        @Override
        public SysUserGroup saveUserGroup(SysUserGroup sysUserGroup) {
            if (sysUserGroup.getId() == null) {
                sysUserGroup.setId(idGenerator.getAndIncrement());
            }
            groups.put(sysUserGroup.getGroupCode(), sysUserGroup);
            return sysUserGroup;
        }

        @Override
        public PageResult<SysUserGroup> pageUserGroups(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
            return page(filterByTenant(groups.values(), tenantId, appCode));
        }

        @Override
        public SysUserGroup findUserGroup(String tenantId, String appCode, String groupCode) {
            return groups.get(groupCode);
        }

        @Override
        public void deleteUserGroup(String tenantId, String appCode, String groupCode) {
            groups.remove(groupCode);
        }

        @Override
        public AuthRole saveRole(AuthRole authRole) {
            if (authRole.getId() == null) {
                authRole.setId(idGenerator.getAndIncrement());
            }
            roles.put(authRole.getRoleCode(), authRole);
            return authRole;
        }

        @Override
        public PageResult<AuthRole> pageRoles(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
            return page(filterByTenant(roles.values(), tenantId, appCode));
        }

        @Override
        public AuthRole findRole(String tenantId, String appCode, String roleCode) {
            return roles.get(roleCode);
        }

        @Override
        public void deleteRole(String tenantId, String appCode, String roleCode) {
            roles.remove(roleCode);
        }

        @Override
        public AuthSubjectRelation saveSubjectRelation(AuthSubjectRelation authSubjectRelation) {
            if (authSubjectRelation.getId() == null) {
                authSubjectRelation.setId(idGenerator.getAndIncrement());
            }
            relations.put(authSubjectRelation.getId(), authSubjectRelation);
            return authSubjectRelation;
        }

        @Override
        public PageResult<AuthSubjectRelation> pageSubjectRelations(String tenantId, String appCode, String keyword, int pageNo, int pageSize) {
            return page(filterByTenant(relations.values(), tenantId, appCode));
        }

        @Override
        public AuthSubjectRelation findSubjectRelation(String tenantId, String appCode, Long relationId) {
            return relations.get(relationId);
        }

        @Override
        public void deleteSubjectRelation(String tenantId, String appCode, Long relationId) {
            relations.remove(relationId);
        }

        @Override
        public List<SysUserAccount> listUsers(String tenantId, String appCode) {
            return filterByTenant(users.values(), tenantId, appCode);
        }

        private <T> PageResult<T> page(List<T> records) {
            return PageResult.<T>builder()
                .pageNo(1)
                .pageSize(records.size() == 0 ? 20 : records.size())
                .total(records.size())
                .records(records)
                .build();
        }

        private <T> List<T> filterByTenant(java.util.Collection<T> values, String tenantId, String appCode) {
            return values.stream()
                .filter(item -> {
                    if (item instanceof SysOrgNode) {
                        SysOrgNode node = (SysOrgNode) item;
                        return tenantId.equals(node.getTenantId()) && appCode.equals(node.getAppCode());
                    }
                    if (item instanceof SysUserAccount) {
                        SysUserAccount account = (SysUserAccount) item;
                        return tenantId.equals(account.getTenantId()) && appCode.equals(account.getAppCode());
                    }
                    if (item instanceof SysPosition) {
                        SysPosition position = (SysPosition) item;
                        return tenantId.equals(position.getTenantId()) && appCode.equals(position.getAppCode());
                    }
                    if (item instanceof SysUserGroup) {
                        SysUserGroup group = (SysUserGroup) item;
                        return tenantId.equals(group.getTenantId()) && appCode.equals(group.getAppCode());
                    }
                    if (item instanceof AuthRole) {
                        AuthRole role = (AuthRole) item;
                        return tenantId.equals(role.getTenantId()) && appCode.equals(role.getAppCode());
                    }
                    AuthSubjectRelation relation = (AuthSubjectRelation) item;
                    return tenantId.equals(relation.getTenantId()) && appCode.equals(relation.getAppCode());
                })
                .collect(Collectors.toList());
        }
    }
}