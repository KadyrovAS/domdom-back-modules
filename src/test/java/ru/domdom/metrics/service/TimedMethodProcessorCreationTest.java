package ru.domdom.metrics.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.domdom.metrics.config.MethodMetricsProperties;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TimedMethodProcessorCreationTest {

    private MeterRegistry registry;
    private MethodMetricsProperties properties;
    private TimedMethodProcessor processor;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        properties = new MethodMetricsProperties();
        properties.setPercentiles(new double[]{0.5, 0.95});
        processor = new TimedMethodProcessor(registry, properties);
    }

    @Test
    void shouldCreateTimerAndRecord() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("result");

        String metricName = "test.key";
        Map<String, String> tags = Map.of("env", "test");

        Object result = processor.process(joinPoint, metricName, tags);
        assertThat(result).isEqualTo("result");

        // Преобразуем Map в Iterable<Tag> для поиска
        List<Tag> tagList = tags.entrySet().stream()
                .map(e -> Tag.of(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        var timer = registry.find(metricName)
                .tags(tagList)
                .timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void shouldApplyTagsCorrectly() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("ok");

        String metricName = "tags.test";
        Map<String, String> tags = Map.of("key1", "value1", "key2", "value2");

        processor.process(joinPoint, metricName, tags);

        List<Tag> tagList = tags.entrySet().stream()
                .map(e -> Tag.of(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        var timer = registry.find(metricName)
                .tags(tagList)
                .timer();
        assertThat(timer).isNotNull();
    }
}