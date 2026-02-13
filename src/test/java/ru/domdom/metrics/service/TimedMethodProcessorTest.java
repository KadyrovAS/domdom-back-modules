package ru.domdom.metrics.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.domdom.metrics.annotation.TimedMethod;

import java.lang.reflect.Method;

import static org.mockito.Mockito.*;

/**
 * Модульные тесты для {@link TimedMethodProcessor}.
 * <p>
 * Проверяют разрешение ключа метрики, запись выполнения и обработку исключений.
 * </p>
 *
 * @author Кадыров Андрей
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class TimedMethodProcessorTest {

    @Mock
    private MetricNameResolver nameResolver;

    @Mock
    private MetricFactory metricFactory;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private TimedMethod annotation;

    @Mock
    private Timer timer;

    @Mock
    private Counter counter;

    @InjectMocks
    private TimedMethodProcessor processor;

    private final String metricKey = "test.key";
    private Method realMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        realMethod = this.getClass().getMethod("dummyMethod");
        lenient().when(nameResolver.resolve(joinPoint, annotation)).thenReturn(metricKey);
        lenient().when(metricFactory.getTimer(metricKey, annotation, realMethod)).thenReturn(timer);
        lenient().when(metricFactory.getCounter(metricKey, annotation, realMethod)).thenReturn(counter);
    }

    @Test
    void shouldResolveMetricKey() {
        processor.resolveMetricKey(joinPoint, annotation);
        verify(nameResolver).resolve(joinPoint, annotation);
    }

    @Test
    void shouldRecordExecution() {
        long duration = 1_000_000L;

        processor.record(metricKey, annotation, realMethod, duration);

        verify(metricFactory).getTimer(metricKey, annotation, realMethod);
        verify(metricFactory).getCounter(metricKey, annotation, realMethod);
        verify(counter).increment();
        verify(timer).record(eq(duration), any());
    }

    @Test
    void shouldHandleExceptionGracefully() {
        when(metricFactory.getTimer(metricKey, annotation, realMethod)).thenThrow(new RuntimeException("test"));

        processor.record(metricKey, annotation, realMethod, 1000L);

        verify(counter, never()).increment();
        verify(timer, never()).record(anyLong(), any());
    }

    public void dummyMethod() {}
}