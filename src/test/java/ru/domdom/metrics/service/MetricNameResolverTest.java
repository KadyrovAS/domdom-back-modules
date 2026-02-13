package ru.domdom.metrics.service;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.domdom.metrics.annotation.TimedMethod;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Модульные тесты для {@link MetricNameResolver}.
 * <p>
 * Проверяют формирование ключа метрики из аннотации и имени метода/класса,
 * а также обработку ситуаций с прокси.
 * </p>
 *
 * @author Кадыров Андрей
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class MetricNameResolverTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    @InjectMocks
    private MetricNameResolver resolver;

    @Test
    void shouldUseAnnotationValueWhenPresent() throws NoSuchMethodException {
        Method method = getClass().getMethod("dummyMethod");
        TimedMethod annotation = mock(TimedMethod.class);
        when(annotation.value()).thenReturn("custom.metric.name");

        String result = resolver.resolve(joinPoint, annotation);

        assertThat(result).isEqualTo("custom.metric.name");
        verifyNoInteractions(joinPoint);
    }

    @Test
    void shouldBuildNameFromClassAndMethodWhenAnnotationValueEmpty() throws NoSuchMethodException {
        Method method = getClass().getMethod("dummyMethod");
        TimedMethod annotation = mock(TimedMethod.class);
        when(annotation.value()).thenReturn("");

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(this);

        String result = resolver.resolve(joinPoint, annotation);

        assertThat(result).isEqualTo("MetricNameResolverTest.dummyMethod");
    }

    @Test
    void shouldFallbackToDeclaringClassWhenTargetIsNull() throws NoSuchMethodException {
        Method method = getClass().getMethod("dummyMethod");
        TimedMethod annotation = mock(TimedMethod.class);
        when(annotation.value()).thenReturn("");

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(null);

        String result = resolver.resolve(joinPoint, annotation);

        assertThat(result).isEqualTo("MetricNameResolverTest.dummyMethod");
    }

    public void dummyMethod() {}
}