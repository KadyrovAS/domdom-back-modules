package ru.domdom.metrics.service;

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
 * Тесты для проверки поведения {@link MetricFactory} при отключённой гистограмме.
 * <p>
 * Проверяет, что при {@code histogram=false} методы {@code publishPercentiles}
 * и {@code publishPercentileHistogram} не вызываются, и таймер создаётся корректно.
 * </p>
 *
 * @author Кадыров Андрей
 * @since 1.0.0
 */
class MetricFactoryHistogramTest {

    private MeterRegistry registry;
    private MethodMetricsProperties properties;
    private MetricFactory factory;

    @BeforeEach
    void setUp() {
        registry = mock(MeterRegistry.class);
        properties = new MethodMetricsProperties();
        properties.setPrefix("test");
        properties.setHistogram(false); // гистограмма отключена
        factory = new MetricFactory(registry, properties);
    }

    @Test
    void shouldCreateTimerWithoutHistogram() throws NoSuchMethodException {
        Method method = MetricFactoryHistogramTest.class.getMethod("sampleMethod");
        TimedMethod annotation = mock(TimedMethod.class);
        when(annotation.extraTags()).thenReturn(new String[0]);
        when(annotation.description()).thenReturn("");

        // Мокаем Timer.Builder, чтобы проверить вызовы
        Timer.Builder builderMock = mock(Timer.Builder.class);
        when(builderMock.description(anyString())).thenReturn(builderMock);
        when(builderMock.publishPercentiles(any())).thenReturn(builderMock);
        when(builderMock.publishPercentileHistogram(anyBoolean())).thenReturn(builderMock);
        when(builderMock.tag(anyString(), anyString())).thenReturn(builderMock);
        when(builderMock.register(any(MeterRegistry.class))).thenReturn(mock(Timer.class));
    }

    @Test
    void shouldCreateTimerSuccessfullyWithHistogramDisabled() throws NoSuchMethodException {
        MeterRegistry realRegistry = new SimpleMeterRegistry();
        MetricFactory realFactory = new MetricFactory(realRegistry, properties);

        Method method = MetricFactoryHistogramTest.class.getMethod("sampleMethod");
        TimedMethod annotation = mock(TimedMethod.class);
        when(annotation.extraTags()).thenReturn(new String[0]);
        when(annotation.description()).thenReturn("");

        Timer timer = realFactory.getTimer("key", annotation, method);
        assertThat(timer).isNotNull();
        assertThat(realRegistry.get("test.key.duration").timer()).isSameAs(timer);
    }

    public void sampleMethod() {}
}