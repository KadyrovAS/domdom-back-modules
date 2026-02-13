package ru.domdom.metrics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Service;
import ru.domdom.metrics.annotation.TimedMethod;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Оркестратор записи метрик времени выполнения методов.
 *
 * <p>Получает ключ метрики через {@link MetricNameResolver}, затем получает или создаёт
 * таймер и счётчик через {@link MetricFactory}, и записывает время выполнения.
 * Обрабатывает исключения, логируя ошибки, но не прерывая выполнение метода.
 *
 * @author Кадыров Андрей
 * @since 1.0.0
 * @see MetricNameResolver
 * @see MetricFactory
 * @see TimedMethod
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimedMethodProcessor {

    private final MetricNameResolver nameResolver;
    private final MetricFactory metricFactory;

    /**
     * Формирует ключ метрики для точки соединения и аннотации.
     *
     * @param joinPoint  точка соединения
     * @param annotation аннотация {@link TimedMethod}
     * @return ключ метрики
     */
    public String resolveMetricKey(ProceedingJoinPoint joinPoint, TimedMethod annotation) {
        return nameResolver.resolve(joinPoint, annotation);
    }

    /**
     * Записывает метрику выполнения метода.
     *
     * @param metricKey     ключ метрики
     * @param annotation    аннотация {@link TimedMethod}
     * @param method        выполняемый метод (нужен для сигнатуры)
     * @param durationNanos время выполнения в наносекундах
     */
    public void record(String metricKey, TimedMethod annotation, Method method, long durationNanos) {
        try {
            var timer = metricFactory.getTimer(metricKey, annotation, method);
            var counter = metricFactory.getCounter(metricKey, annotation, method);
            counter.increment();
            timer.record(durationNanos, TimeUnit.NANOSECONDS);
            log.debug("Recorded execution of {}: {} ns", metricKey, durationNanos);
        } catch (Exception e) {
            log.error("Failed to record metric for key: {}", metricKey, e);
        }
    }
}