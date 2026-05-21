package com.ruijie.authzengine.api.assembler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.authzengine.api.dto.request.AuthzCheckRequest;
import com.ruijie.authzengine.api.dto.request.AuthzResourceRequest;
import com.ruijie.authzengine.api.dto.request.AuthzSubjectRequest;
import com.ruijie.authzengine.domain.model.decision.AuthzRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AuthzRequestAssemblerTest {

    private final AuthzRequestAssembler assembler = new AuthzRequestAssembler();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldPassThroughResourceTypeForApiResource() {
        AuthzRequest request = assembler.toDomain(buildRequest("RES_API", "2042180987694223361"));

        Assertions.assertEquals("RES_API", request.getResource().getResourceType());
        Assertions.assertEquals("2042180987694223361", request.getResource().getResId());
    }

    @Test
    void shouldPassThroughResourceTypeForBoResource() {
        AuthzCheckRequest dto = buildRequest("RES_DATA_BO", "101");

        AuthzRequest request = assembler.toDomain(dto);

        Assertions.assertEquals("RES_DATA_BO", request.getResource().getResourceType());
        Assertions.assertEquals("101", request.getResource().getResId());
    }

    @Test
    void shouldDeserializeResourceIdField() throws Exception {
        String json = "{"
            + "\"tenantId\":\"T001\","
            + "\"appCode\":\"CRM\","
            + "\"action\":\"READ\","
            + "\"subject\":{\"subjectModel\":\"SUB_USER\",\"subjectId\":\"zhangsan\"},"
            + "\"resource\":{\"resourceModel\":\"RES_API\",\"resourceId\":\"2042180987694223361\"}"
            + "}";

        AuthzCheckRequest request = objectMapper.readValue(json, AuthzCheckRequest.class);

        Assertions.assertEquals("2042180987694223361", request.getResource().getResourceId());
    }

    private AuthzCheckRequest buildRequest(String resourceModel, String resourceId) {
        AuthzSubjectRequest subject = new AuthzSubjectRequest();
        subject.setSubjectModel("SUB_USER");
        subject.setSubjectId("zhangsan");

        AuthzResourceRequest resource = new AuthzResourceRequest();
        resource.setResourceModel(resourceModel);
        resource.setResourceId(resourceId);

        AuthzCheckRequest request = new AuthzCheckRequest();
        request.setTenantId("T001");
        request.setAppCode("CRM");
        request.setSubject(subject);
        request.setResource(resource);
        request.setAction("READ");
        return request;
    }
}