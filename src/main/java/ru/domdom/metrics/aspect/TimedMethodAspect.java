package ru.domdom.metrics.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import ru.domdom.metrics.annotation.TimedMethod;
import ru.domdom.metrics.service.TimedMethodProcessor;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

@Aspect
@RequiredArgsConstructor
@Slf4j
public class TimedMethodAspect {

    private final TimedMethodProcessor processor;
    private final ConcurrentMap<String, Boolean> timerInitializationCache = new ConcurrentHashMap<>();
    
    @Around("@annotation(ru.domdom.metrics.annotation.TimedMethod)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // Получаем реальный метод (если вызван через прокси)
        Method realMethod = getRealMethod(joinPoint.getTarget(), method);
        TimedMethod annotation = realMethod.getAnnotation(TimedMethod.class);

        if (annotation == null) {
            return joinPoint.proceed();
        }

        String metricKey = getMetricKey(joinPoint, realMethod, annotation);

        this.initializeTimer(metricKey, annotation, realMethod);

        long startTime = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.nanoTime() - startTime;
            processor.recordExecution(metricKey, duration);
        }
    }

    private Method getRealMethod(Object target, Method method) {
        try {
            if (target != null) {
                Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);

                return targetClass.getDeclaredMethod(
                        method.getName(),
                        method.getParameterTypes()
                );
            }
        } catch (NoSuchMethodException e) {
            log.error("Method not found in target class, using original method \n{}", getRootCauseMessage(e));
            log.debug("Method not found in target class, using original method", e);
        }
        return method;
    }

    private String getMetricKey(ProceedingJoinPoint joinPoint, Method method, TimedMethod annotation) {
        if (annotation != null && !annotation.value().isEmpty()) {
            return annotation.value();
        }

        Object target = joinPoint.getTarget();
        String className;

        if (target != null) {
            className = AopProxyUtils.ultimateTargetClass(target).getSimpleName();
        } else {
            className = method.getDeclaringClass().getSimpleName();
        }

        return className + "." + method.getName();
    }

    private void initializeTimer(String metricKey, TimedMethod annotation, Method method) {
        if (timerInitializationCache.containsKey(metricKey)) {
            return;
        }

        synchronized (this) {
            if (!timerInitializationCache.containsKey(metricKey)) {
                try {
                    processor.createTimerForMethod(metricKey, annotation, method);
                    timerInitializationCache.put(metricKey, true);

                    if (log.isDebugEnabled()) {
                        log.info("Created timer for method: {}", metricKey);
                    }
                } catch (Exception e) {
                    log.error("Failed to create timer for method: {} {}", metricKey, getRootCauseMessage(e));
                    log.debug("Failed to create timer for method: {} ", metricKey, e);
                }
            }
        }
    }

    public void clearCache() {
        timerInitializationCache.clear();
    }
}