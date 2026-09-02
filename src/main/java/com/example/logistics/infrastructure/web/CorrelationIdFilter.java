package com.example.logistics.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Manages the correlation ID for every inbound HTTP request.
 *
 * <p>If the caller provides {@code X-Correlation-Id} it is propagated and echoed back;
 * otherwise a fresh one is generated. The value is bound to the {@link MDC} so all
 * structured logs and the problem+json error responses carry the same correlation ID,
 * enabling end-to-end request tracing across services.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        final String provided = request.getHeader(HEADER);
        final String correlationId = (provided == null || provided.isBlank())
                ? UUID.randomUUID().toString()
                : request.getHeader(HEADER);

        response.setHeader(HEADER, correlationId);
        if (provided == null || provided.isBlank()) {
            // also expose as request attribute for the handler to use if needed
            request.setAttribute(HEADER, correlationId);
        }

        try (MDC.MDCCloseable ignored = MDC.putCloseable(MDC_KEY, correlationId)) {
            filterChain.doFilter(request, response);
        }
    }
}
