package ru.domdom.metrics.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.domdom.metrics.annotation.TimedMethod;
import ru.domdom.metrics.config.MethodMetricsProperties;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Модульные тесты для {@link MetricFactory}.
 * <p>
 * Проверяют создание и кэширование таймеров и счётчиков,
 * применение тегов и описаний, а также очистку кэша.
 * </p>
 *
 * @author Кадыров Андрей
 * @since 1.0.0
 */
class MetricFactoryTest {

    private MeterRegistry registry;
    private MethodMetricsProperties properties;
    private MetricFactory factory;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        properties = new MethodMetricsProperties();
        properties.setPrefix("test");
        properties.setHistogram(true);
        properties.setPercentiles(new double[]{0.5, 0.95});
        factory = new MetricFactory(registry, properties);
    }

    @Test
    void shouldCreateAndCacheTimer() throws NoSuchMethodException {
        Method method = getClass().getMethod("sampleMethod");
        TimedMethod annotation = mock(TimedMethod.class);
        when(annotation.extraTags()).thenReturn(new String[]{"env=test"});
        when(annotation.description()).thenReturn("Test description");

        Timer timer1 = factory.getTimer("metric.key", annotation, method);
        Timer timer2 = factory.getTimer("metric.key", annotation, method);

        assertThat(timer1).isSameAs(timer2);
        assertThat(registry.get("test.metric.key.duration").timer()).isNotNull();
    }

    @Test
    void shouldCreateAndCacheCounter() throws NoSuchMethodException {
        Method method = getClass().getMethod("sampleMethod");
        TimedMethod annotation = mock(TimedMethod.class);
        when(annotation.extraTags()).thenReturn(new String[]{"env=test"});
        when(annotation.description()).thenReturn("");

        Counter counter1 = factory.getCounter("metric.key", annotation, method);
        Counter counter2 = factory.getCounter("metric.key", annotation, method);

        assertThat(counter1).isSameAs(counter2);
        assertThat(registry.get("test.metric.key.calls").counter()).isNotNull();
    }

    @Test
    void shouldApplyTagsAndDescription() throws NoSuchMethodException {
        Method method = getClass().getMethod("sampleMethod");
        TimedMethod annotation = mock(TimedMethod.class);
        when(annotation.extraTags()).thenReturn(new String[]{"env=prod", "service=test"});
        when(annotation.description()).thenReturn("Custom desc");

        Timer timer = factory.getTimer("Class.method", annotation, method);
        Counter counter = factory.getCounter("Class.method", annotation, method);

        assertThat(timer.getId().getTag("env")).isEqualTo("prod");
        assertThat(timer.getId().getTag("service")).isEqualTo("test");
        assertThat(timer.getId().getTag("method")).isEqualTo("method");
        assertThat(timer.getId().getTag("class")).isEqualTo("Class");
        assertThat(timer.getId().getTag("signature")).isEqualTo("sampleMethod()");

        assertThat(counter.getId().getTag("env")).isEqualTo("prod");
        assertThat(counter.getId().getDescription()).isEqualTo("Number of calls for method: sampleMethod");
    }

    @Test
    void shouldClearCache() throws NoSuchMethodException {
        Method method = getClass().getMethod("sampleMethod");
        TimedMethod annotation = mock(TimedMethod.class);
        when(annotation.extraTags()).thenReturn(new String[0]);
        when(annotation.description()).thenReturn("");

        Timer timer = factory.getTimer("key", annotation, method);
        assertThat(factory.getTimer("key", annotation, method)).isSameAs(timer);

        factory.clearCache();
        Timer newTimer = factory.getTimer("key", annotation, method);
        assertThat(newTimer).isNotSameAs(timer);
    }

    public void sampleMethod() {}
}