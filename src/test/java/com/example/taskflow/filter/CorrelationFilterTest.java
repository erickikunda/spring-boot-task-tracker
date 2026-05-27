package com.example.taskflow.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationFilterTest {

    private final CorrelationFilter filter = new CorrelationFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void echoesExistingCorrelationIdHeader() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationFilter.HEADER, "my-trace-id");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationFilter.HEADER)).isEqualTo("my-trace-id");
    }

    @Test
    void generatesUuidWhenHeaderAbsent() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        var id = response.getHeader(CorrelationFilter.HEADER);
        assertThat(id).isNotBlank();
        // valid UUID format
        assertThat(id).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void clearsMdcAfterRequest() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationFilter.HEADER, "trace-123");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(MDC.get("correlationId")).isNull();
    }
}
