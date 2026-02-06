package com.dom_dom.metrics.aspect;

import com.dom_dom.metrics.annotation.TimedMethod;
import com.dom_dom.metrics.service.TimedMethodProcessor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Aspect
@Component
public class TimedMethodAspect {

    private final TimedMethodProcessor processor;
    private final ConcurrentMap<String, Boolean> timerInitializationCache = new ConcurrentHashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(TimedMethodAspect.class);

    @Autowired
    public TimedMethodAspect(TimedMethodProcessor processor) {
        this.processor = processor;
    }

    @Around("@annotation(com.dom_dom.metrics.annotation.TimedMethod)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // Получаем реальный метод (если вызван через прокси)
        Method realMethod = getRealMethod(joinPoint.getTarget(), method);
        TimedMethod annotation = realMethod.getAnnotation(TimedMethod.class);

        if (annotation == null) {
            // Если аннотация не найдена, просто выполняем метод
            return joinPoint.proceed();
        }

        // Формируем уникальный ключ для метода
        String metricKey = getMetricKey(joinPoint, realMethod, annotation);

        // Ленивая инициализация таймера при первом вызове
        initializeTimerLazily(metricKey, annotation, realMethod);

        // Измеряем время выполнения
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
                // Получаем реальный класс (без прокси)
                Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);

                // Ищем метод с той же сигнатурой в реальном классе
                return targetClass.getDeclaredMethod(
                        method.getName(),
                        method.getParameterTypes()
                );
            }
        } catch (NoSuchMethodException e) {
            logger.debug("Method not found in target class, using original method", e);
        }
        return method;
    }

    private String getMetricKey(ProceedingJoinPoint joinPoint, Method method, TimedMethod annotation) {
        // Если в аннотации указано кастомное имя, используем его
        if (annotation != null && !annotation.value().isEmpty()) {
            return annotation.value();
        }

        // Иначе формируем имя на основе класса и метода
        Object target = joinPoint.getTarget();
        String className;

        if (target != null) {
            className = AopProxyUtils.ultimateTargetClass(target).getSimpleName();
        } else if (method.getDeclaringClass() != null) {
            className = method.getDeclaringClass().getSimpleName();
        } else {
            className = "UnknownClass";
        }

        return className + "." + method.getName();
    }

    private void initializeTimerLazily(String metricKey, TimedMethod annotation, Method method) {
        // Быстрая проверка без блокировки
        if (timerInitializationCache.containsKey(metricKey)) {
            return;
        }

        synchronized (this) {
            // Повторная проверка под блокировкой
            if (!timerInitializationCache.containsKey(metricKey)) {
                try {
                    // Создаем таймер для метода
                    processor.createTimerForMethod(metricKey, annotation, method);
                    timerInitializationCache.put(metricKey, true);

                    if (logger.isDebugEnabled()) {
                        logger.debug("Created timer for method: {}", metricKey);
                    }
                } catch (Exception e) {
                    logger.error("Failed to create timer for method: {}", metricKey, e);
                    // Не добавляем в кэш при ошибке, чтобы попробовать снова при следующем вызове
                }
            }
        }
    }

    public void clearCache() {
        timerInitializationCache.clear();
    }
}