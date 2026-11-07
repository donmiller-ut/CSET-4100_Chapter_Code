package com.example.northstarmarket.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTests {
    private final RequestIdFilter filter = new RequestIdFilter();

    @Test void generatesAnIdInsteadOfTrustingClientInputAndClearsContext() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "untrusted-client-value");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {
            assertThat(MDC.get("requestId")).matches("req-[0-9a-f]{32}");
            assertThat(MDC.get("requestId")).isEqualTo(request.getAttribute(RequestIdFilter.ATTRIBUTE));
            assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(MDC.get("requestId"));
        });
        assertThat(MDC.get("requestId")).isNull();
        var next = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), next, (req, res) -> {});
        assertThat(next.getHeader(RequestIdFilter.HEADER)).isNotEqualTo(response.getHeader(RequestIdFilter.HEADER));
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test void clearsContextWhenTheRequestFails() {
        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(),
                new MockHttpServletResponse(), (req, res) -> {
                    assertThat(MDC.get("requestId")).isNotNull();
                    throw new ServletException("deliberate test failure");
                })).isInstanceOf(ServletException.class);
        assertThat(MDC.get("requestId")).isNull();
    }
}
