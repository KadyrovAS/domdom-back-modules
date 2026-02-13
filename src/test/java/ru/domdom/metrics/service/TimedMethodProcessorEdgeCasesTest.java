package ru.domdom.metrics.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.domdom.metrics.annotation.TimedMethod;
import ru.domdom.metrics.config.MethodMetricsProperties;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Тесты граничных случаев для {@link TimedMethodProcessor}.
 * <p>
 * Проверяют устойчивость к null-значениям, отсутствию тегов,
 * исключениям при создании метрик и формированию сигнатуры метода.
 * </p>
 *
 * @author Кадыров Андрей
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
public class TimedMethodProcessorEdgeCasesTest {

    private MeterRegistry meterRegistry;
    private MethodMetricsProperties properties;
    private MetricNameResolver nameResolver;
    private MetricFactory metricFactory;
    private TimedMethodProcessor processor;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private TimedMethod annotation;

    @Mock
    private Method method;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        properties = new MethodMetricsProperties();
        nameResolver = mock(MetricNameResolver.class);
        metricFactory = new MetricFactory(meterRegistry, properties);
        processor = new TimedMethodProcessor(nameResolver, metricFactory);

        lenient().when(annotation.description()).thenReturn("");
        lenient().when(annotation.extraTags()).thenReturn(new String[0]);
    }

    @Test
    void shouldResolveMetricKeyEvenIfAnnotationIsNull() {
        when(nameResolver.resolve(joinPoint, null)).thenReturn("some.key");
        String key = processor.resolveMetricKey(joinPoint, null);
        assertThat(key).isEqualTo("some.key");
        verify(nameResolver).resolve(joinPoint, null);
    }

    @Test
    void shouldNotThrowWhenRecordingWithNullMetricKey() {
        processor.record(null, annotation, method, 1000L);
    }

    @Test
    void shouldNotThrowWhenTimerCreationFails() {
        MetricFactory failingFactory = mock(MetricFactory.class);
        when(failingFactory.getCounter(anyString(), any(), any())).thenReturn(mock(Counter.class));

        TimedMethodProcessor failingProcessor = new TimedMethodProcessor(nameResolver, failingFactory);
        failingProcessor.record("key", annotation, method, 1000L);
    }

    @Test
    void shouldHandleEmptyExtraTags() throws NoSuchMethodException {
        when(annotation.extraTags()).thenReturn(new String[0]);
        Method realMethod = this.getClass().getMethod("dummyMethod");
        Timer timer = metricFactory.getTimer("key", annotation, realMethod);
        assertThat(timer).isNotNull();
        Counter counter = metricFactory.getCounter("key", annotation, realMethod);
        assertThat(counter).isNotNull();
    }

    @Test
    void shouldHandleNullExtraTags() throws NoSuchMethodException {
        when(annotation.extraTags()).thenReturn(null);
        Method realMethod = this.getClass().getMethod("dummyMethod");
        Timer timer = metricFactory.getTimer("key", annotation, realMethod);
        assertThat(timer).isNotNull();
        Counter counter = metricFactory.getCounter("key", annotation, realMethod);
        assertThat(counter).isNotNull();
    }

    @Test
    void shouldGenerateMethodSignature() throws NoSuchMethodException {
        Method realMethod = this.getClass().getMethod("dummyMethod", String.class, int.class);
        when(annotation.extraTags()).thenReturn(new String[0]);
        Timer timer = metricFactory.getTimer("key", annotation, realMethod);
        assertThat(timer.getId().getTag("signature")).isEqualTo("dummyMethod(String,int)");
    }

    public void dummyMethod() {}
    public void dummyMethod(String s, int i) {}
}