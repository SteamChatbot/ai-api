package com.sanavi.aiapi.common.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class MDCFilter implements Filter {

    private static final String SERVICE_NAME = "sanavi-ai-api";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        // backend-springboot가 X-Trace-Id 헤더로 넘겨준 trace_id 우선 사용
        // 없으면 자체 생성 (ai-api 직접 호출 케이스 대비)
        String traceId = resolveTraceId(req);
        try {
            MDC.put("traceId", traceId);
            chain.doFilter(req, res);
        } finally {
            // ThreadLocal 기반이라 반드시 cleanup — 없으면 스레드 풀에서 이전 trace_id가 다음 요청에 묻어남
            MDC.clear();
        }
    }

    private String resolveTraceId(ServletRequest req) {
        if (req instanceof jakarta.servlet.http.HttpServletRequest httpReq) {
            String upstream = httpReq.getHeader("X-Trace-Id");
            if (upstream != null && !upstream.isBlank()) {
                return upstream;
            }
        }
        // 직접 호출 시 자체 생성
        return SERVICE_NAME + ":" + resolveUserId(req) + ":" + randomId();
    }

    /**
     * 요청에서 user_id를 추출한다.
     *
     * TODO: JWT 구현 후 아래 한 줄로 교체
     *   Authentication auth = SecurityContextHolder.getContext().getAuthentication();
     *   if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal()))
     *       return auth.getName();
     *   return "anonymous";
     */
    private String resolveUserId(ServletRequest req) {
        return "anonymous";
    }

    private String randomId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
