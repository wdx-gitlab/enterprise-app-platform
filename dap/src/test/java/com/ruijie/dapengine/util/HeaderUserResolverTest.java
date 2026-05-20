package com.ruijie.dapengine.util;

import com.ruijie.dapengine.common.util.HeaderUserResolver;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.*;

public class HeaderUserResolverTest {

    @Test
    public void resolve_with_x_user_id_header_should_return_header_value() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HeaderUserResolver.HEADER_USER_ID, "user001");
        assertEquals("user001", HeaderUserResolver.resolve(request));
    }

    @Test
    public void resolve_without_header_should_return_empty_string() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertEquals("", HeaderUserResolver.resolve(request));
    }

    @Test
    public void resolve_null_request_should_return_empty_string() {
        assertEquals("", HeaderUserResolver.resolve(null));
    }

    @Test
    public void resolve_request_attribute_should_take_priority_over_header() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HeaderUserResolver.HEADER_USER_ID, "header_user");
        request.setAttribute(HeaderUserResolver.REQUEST_ATTR_USER, "attr_user");
        assertEquals("attr_user 应优先于 Header", "attr_user", HeaderUserResolver.resolve(request));
    }

    @Test
    public void resolve_empty_attribute_should_fallback_to_header() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(HeaderUserResolver.REQUEST_ATTR_USER, "");
        request.addHeader(HeaderUserResolver.HEADER_USER_ID, "header_user");
        assertEquals("header_user", HeaderUserResolver.resolve(request));
    }
}
