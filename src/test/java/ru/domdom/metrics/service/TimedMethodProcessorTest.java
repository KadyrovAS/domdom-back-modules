package ru.domdom.metrics.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.domdom.metrics.config.MethodMetricsProperties;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimedMethodProcessorTest {

    private MeterRegistry meterRegistry;
    private MethodMetricsProperties properties;
    private TimedMethodProcessor processor;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        properties = new MethodMetricsProperties();
        properties.setPercentiles(new double[]{0.5, 0.95});
        processor = new TimedMethodProcessor(meterRegistry, properties);
    }

    @Test
    void shouldRecordExecutionTimeAndCreateTimer() throws Throwable {
        when(joinPoint.proceed()).thenReturn("result");

        String metricName = "test.metric";
        Map<String, String> tags = Map.of("env", "test");

        Object result = processor.process(joinPoint, metricName, tags);

        assertThat(result).isEqualTo("result");

        // Преобразуем Map в List<Tag> вручную
        List<Tag> tagList = tags.entrySet().stream()
                .map(entry -> Tag.of(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        var timer = meterRegistry.find(metricName)
                .tags(tagList)  // передаём List<Tag>
                .timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void shouldRecordExecutionEvenIfMethodThrows() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new RuntimeException("error"));

        String metricName = "test.metric";
        Map<String, String> tags = Map.of();

        assertThatThrownBy(() -> processor.process(joinPoint, metricName, tags))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("error");

        var timer = meterRegistry.find(metricName).timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }
}