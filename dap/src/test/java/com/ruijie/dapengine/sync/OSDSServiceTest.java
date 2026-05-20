package com.ruijie.dapengine.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruijie.dapengine.common.model.DapEngineProperties;
import com.ruijie.dapengine.common.model.DepartmentExtendInfo;
import com.ruijie.dapengine.common.model.RemoteResult;
import com.ruijie.dapengine.common.model.RuijieAuthProperties;
import com.ruijie.dapengine.common.model.StaffExtendInfo;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OSDSServiceTest {

    @Test
    void getStaffShouldParseDataToStaffExtendInfo() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        OSDSService service = new OSDSService(new ObjectMapper(), "http://service-gw.ruijie.com.cn/api",
                "8c83f96462784edeb3997a803fb8b8df", "3183AC9698E14885934D24E30676365D", restTemplate);

        server.expect(requestTo("http://service-gw.ruijie.com.cn/api/osds-api/staff/R13174/data"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("sysid", "8c83f96462784edeb3997a803fb8b8df"))
                .andExpect(header("sign-server-auth", org.hamcrest.Matchers.matchesPattern(
                        "8c83f96462784edeb3997a803fb8b8df\\|\\d{13}\\|[A-F0-9]{32}")))
                .andRespond(withSuccess("{\"code\":200,\"message\":\"success\",\"data\":{\"staffNo\":\"R13174\",\"userId\":\"wangdaoxin\",\"staffCompanyName\":\"王道鑫\",\"staffStatus\":1,\"departmentCode\":\"000023662002\",\"isDeptManage\":false}}",
                        MediaType.APPLICATION_JSON));

        RemoteResult<StaffExtendInfo> result = service.getStaff("R13174");

        assertNotNull(result);
        assertEquals("200", result.getStatus());
        assertNotNull(result.getData());
        assertEquals("R13174", result.getData().getStaffNo());
        assertEquals("wangdaoxin", result.getData().getUserId());
        assertEquals("王道鑫", result.getData().getStaffCompanyName());
        assertEquals(Integer.valueOf(1), result.getData().getStaffStatus());
        assertEquals("000023662002", result.getData().getDepartmentCode());
        assertEquals(Boolean.FALSE, result.getData().getIsDeptManage());
        server.verify();
    }

    @Test
    void getDepartmentShouldParseDataToDepartmentExtendInfo() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        OSDSService service = new OSDSService(new ObjectMapper(), "http://service-gw.ruijie.com.cn/api",
                "8c83f96462784edeb3997a803fb8b8df", "3183AC9698E14885934D24E30676365D", restTemplate);

        server.expect(requestTo("http://service-gw.ruijie.com.cn/api/osds-api/department/000023662002/data"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("sysid", "8c83f96462784edeb3997a803fb8b8df"))
                .andExpect(header("sign-server-auth", org.hamcrest.Matchers.matchesPattern(
                        "8c83f96462784edeb3997a803fb8b8df\\|\\d{13}\\|[A-F0-9]{32}")))
                .andRespond(withSuccess("{\"status\":\"200\",\"err\":\"成功\",\"data\":{\"departmentName\":\"技术架构部\",\"departmentCode\":\"000023662002\",\"parentDepartmentCode\":\"000023662\",\"departmentLevel\":4,\"departmentTypeCode\":\"BM0400\",\"departmentCategory\":null,\"departmentType\":\"总部职能\",\"isEnable\":true,\"createTime\":\"2025-01-05 00:00:00\",\"departmentEnName\":null,\"parentDepartmentName\":\"信息资源IT研发中心\",\"manageUserId\":\"zhangchunhui\",\"manageStaffNo\":\"R10377\",\"manageName\":\"张春晖\",\"portionManageUserId\":\"sunxiaobo\",\"portionManageStaffNo\":\"R04391\",\"portionManageName\":\"孙小波\",\"departmentHrbpList\":[]}}",
                        MediaType.APPLICATION_JSON));

        RemoteResult<DepartmentExtendInfo> result = service.getDepartment("000023662002");

        assertNotNull(result);
        assertEquals("200", result.getStatus());
        assertNotNull(result.getData());
        assertEquals("000023662002", result.getData().getDepartmentCode());
        assertEquals("技术架构部", result.getData().getDepartmentName());
        assertEquals("000023662", result.getData().getParentDepartmentCode());
        assertEquals(Integer.valueOf(4), result.getData().getDepartmentLevel());
        assertEquals("BM0400", result.getData().getDepartmentTypeCode());
        assertEquals(Boolean.TRUE, result.getData().getIsEnable());
        assertNotNull(result.getData().getDepartmentHrbpList());
        assertEquals(0, result.getData().getDepartmentHrbpList().size());
        server.verify();
    }

    @Test
    void shouldFailWhenAuthConfigMissing() {
        RuijieAuthProperties authProperties = new RuijieAuthProperties();
        OSDSService service = new OSDSService(new ObjectMapper(), new DapEngineProperties(), authProperties);

        RemoteResult<StaffExtendInfo> result = service.getStaff("R13174");

        assertNotNull(result);
        assertEquals("500", result.getStatus());
        assertEquals("OSDS鉴权配置缺失，请配置 spring.ruijie.auth.sys.id 和 spring.ruijie.auth.sys.accessKeySecret", result.getErr());
    }

    @Test
    void shouldUseConfiguredOsdsBaseUrl() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        DapEngineProperties properties = new DapEngineProperties();
        properties.getSync().getOsds().setBaseUrl("http://custom-osds.test/api");
        RuijieAuthProperties authProperties = new RuijieAuthProperties();
        authProperties.setId("8c83f96462784edeb3997a803fb8b8df");
        authProperties.setAccessKeySecret("3183AC9698E14885934D24E30676365D");
        OSDSService service = new OSDSService(new ObjectMapper(), properties, authProperties, restTemplate);

        server.expect(requestTo("http://custom-osds.test/api/osds-api/staff/R13174/data"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("sysid", "8c83f96462784edeb3997a803fb8b8df"))
                .andRespond(withSuccess("{\"code\":200,\"message\":\"success\",\"data\":{\"staffNo\":\"R13174\"}}",
                        MediaType.APPLICATION_JSON));

        RemoteResult<StaffExtendInfo> result = service.getStaff("R13174");

        assertNotNull(result);
        assertEquals("200", result.getStatus());
        assertEquals("R13174", result.getData().getStaffNo());
        server.verify();
    }
}

