package com.pageon.backend.common.aop;

import com.pageon.backend.common.annotation.ExecutionTimer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Order(1)
@Component
public class ExecutionTimeAspect {

    @Pointcut("@annotation(com.pageon.backend.common.annotation.ExecutionTimer)")
    private void executionTimer() {};

    @Around("executionTimer()")
    public Object measureTime(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();

        stopWatch.start();
        Object proceed = joinPoint.proceed();
        stopWatch.stop();

        String method = joinPoint.getSignature().getName();

        log.info("[ExecutionTimer] Method Description: {}, Time: {}ms", method, stopWatch.getTotalTimeMillis());

        return proceed;
    }

}
