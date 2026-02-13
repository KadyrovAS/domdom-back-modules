package ru.domdom.metrics.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import ru.domdom.metrics.annotation.TimedMethod;
import ru.domdom.metrics.service.TimedMethodProcessor;

import java.lang.reflect.Method;

/**
 * Аспект для перехвата методов, аннотированных {@link TimedMethod},
 * и записи метрик времени их выполнения.
 *
 * <p>Аспект получает ключ метрики через {@link TimedMethodProcessor#resolveMetricKey},
 * замеряет время выполнения и передаёт результат в процессор для записи.
 *
 * @author Кадыров Андрей
 * @since 1.0.0
 * @see TimedMethod
 * @see TimedMethodProcessor
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class TimedMethodAspect {

    private final TimedMethodProcessor processor;

    /**
     * Совет, выполняющийся вокруг метода с аннотацией {@link TimedMethod}.
     * Измеряет время выполнения и передаёт метрику в процессор.
     *
     * @param joinPoint точка соединения, представляющая выполнение метода
     * @param annotation экземпляр аннотации {@link TimedMethod}
     * @return результат выполнения целевого метода
     * @throws Throwable любое исключение, выброшенное целевым методом
     */
    @Around("@annotation(annotation)")
    public Object measure(ProceedingJoinPoint joinPoint, TimedMethod annotation) throws Throwable {
        log.info("Intercepted method: {}", joinPoint.getSignature());
        String metricKey = processor.resolveMetricKey(joinPoint, annotation);
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.nanoTime() - start;
            processor.record(metricKey, annotation, method, duration);
        }
    }
}