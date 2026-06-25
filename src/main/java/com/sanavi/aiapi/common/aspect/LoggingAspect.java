package com.sanavi.aiapi.common.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    // Controller + Service 전체를 포인트컷으로 잡음
    // 파라미터는 찍지 않음 — password, 파일 바이트 등 민감 데이터가 섞일 수 있음
    @Around("execution(* com.sanavi.aiapi..controller..*(..))" +
            " || execution(* com.sanavi.aiapi..service..*(..))")
    public Object log(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().toShortString();
        long start = System.currentTimeMillis();

        // trace_id는 MDC → 패턴에서 자동 출력되므로 메시지에 중복 포함 안 함
        log.info("START {}", method);
        try {
            Object result = pjp.proceed();
            log.info("END   {} — {}ms", method, System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("ERROR {} — {}ms ({})", method,
                    System.currentTimeMillis() - start, e.getClass().getSimpleName());
            throw e;
        }
    }
}
