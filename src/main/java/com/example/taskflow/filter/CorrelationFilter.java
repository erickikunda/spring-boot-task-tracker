package com.example.taskflow.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

// Runs before every other filter (HIGHEST_PRECEDENCE) so all downstream log statements
// automatically carry the correlation ID in MDC — including security, service, and repository layers.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-ID";
    private static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        var id = Optional.ofNullable(request.getHeader(HEADER))
                .filter(h -> !h.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());

        MDC.put(MDC_KEY, id);
        response.setHeader(HEADER, id);  // echo back so clients can correlate responses to logs

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();  // always clear — prevents leak into thread-pool reuse
        }
    }
}
