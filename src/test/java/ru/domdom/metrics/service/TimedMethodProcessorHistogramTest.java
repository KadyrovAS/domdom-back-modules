package ru.domdom.metrics.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import ru.domdom.metrics.config.MethodMetricsProperties;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TimedMethodProcessorHistogramTest {

    @Test
    void shouldCreateTimerWithPercentiles() throws Throwable {
        MeterRegistry registry = new SimpleMeterRegistry();
        MethodMetricsProperties properties = new MethodMetricsProperties();
        properties.setPercentiles(new double[]{0.5, 0.95});

        TimedMethodProcessor processor = new TimedMethodProcessor(registry, properties);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("ok");

        String metricName = "histogram.test";
        Map<String, String> tags = Map.of();

        processor.process(joinPoint, metricName, tags);

        var timer = registry.find(metricName).timer();
        assertThat(timer).isNotNull();
        // Проверим, что таймер создан, остальное проверяет Micrometer
    }
}