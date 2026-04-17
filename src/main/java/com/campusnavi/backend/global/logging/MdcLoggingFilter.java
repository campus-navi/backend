package com.campusnavi.backend.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
@Slf4j
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";
    private static final String METHOD = "method";
    private static final String URI = "uri";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        MDC.put(TRACE_ID, UUID.randomUUID().toString().substring(0,8));
        MDC.put(METHOD, request.getMethod());
        MDC.put(URI, request.getRequestURI());
        long startTime = System.currentTimeMillis();
        try {
            log.info("HTTP Request");
            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("HTTP Response - {}({}ms)", response.getStatus(), elapsed);
            MDC.clear();
        }
    }
}
