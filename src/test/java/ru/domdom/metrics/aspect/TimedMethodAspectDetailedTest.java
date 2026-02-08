package ru.domdom.metrics.aspect;

import ru.domdom.metrics.annotation.TimedMethod;
import ru.domdom.metrics.service.TimedMethodProcessor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimedMethodAspectDetailedTest {

    @Mock
    private TimedMethodProcessor processor;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private TimedMethodAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new TimedMethodAspect(processor);
    }

    @Test
    void shouldMeasureVoidMethodExecution() throws Throwable {
        // Arrange
        TestService testService = new TestService();
        Method method = TestService.class.getMethod("voidMethod");

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(testService);
        when(joinPoint.proceed()).thenReturn(null);

        // Act
        Object result = aspect.measureExecutionTime(joinPoint);

        // Assert
        assertThat(result).isNull();
        verify(processor, times(1)).recordExecution(eq("TestService.voidMethod"), anyLong());
    }

    @Test
    void shouldMeasurePrimitiveReturnMethod() throws Throwable {
        // Arrange
        TestService testService = new TestService();
        Method method = TestService.class.getMethod("intMethod");

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(testService);
        when(joinPoint.proceed()).thenReturn(42);

        // Act
        Object result = aspect.measureExecutionTime(joinPoint);

        // Assert
        assertThat(result).isEqualTo(42);
        verify(processor, times(1)).recordExecution(eq("primitive.int"), anyLong());
    }

    @Test
    void shouldMeasureObjectReturnMethod() throws Throwable {
        // Arrange
        TestService testService = new TestService();
        Method method = TestService.class.getMethod("stringMethod");

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(testService);
        when(joinPoint.proceed()).thenReturn("result");

        // Act
        Object result = aspect.measureExecutionTime(joinPoint);

        // Assert
        assertThat(result).isEqualTo("result");
        verify(processor, times(1)).recordExecution(eq("TestService.stringMethod"), anyLong());
    }

    @Test
    void shouldHandleMethodWithCustomMetricName() throws Throwable {
        // Arrange
        TestService testService = new TestService();
        Method method = TestService.class.getMethod("customNameMethod");

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(testService);
        when(joinPoint.proceed()).thenReturn(null);

        // Act
        aspect.measureExecutionTime(joinPoint);

        // Assert
        verify(processor, times(1)).recordExecution(eq("custom.metric.name"), anyLong());
    }

    @Test
    void shouldHandleMethodExceptionAndRecordTime() throws Throwable {
        // Arrange
        TestService testService = new TestService();
        Method method = TestService.class.getMethod("exceptionMethod");

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(testService);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Test exception"));

        // Act & Assert
        assertThatThrownBy(() -> aspect.measureExecutionTime(joinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test exception");

        // Метрика должна быть записана даже при исключении
        verify(processor, times(1)).recordExecution(eq("TestService.exceptionMethod"), anyLong());
    }

    @Test
    void shouldHandleMethodWithParameters() throws Throwable {
        // Arrange
        TestService testService = new TestService();
        Method method = TestService.class.getMethod("methodWithParams", String.class, int.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(testService);
        when(joinPoint.proceed()).thenReturn("processed");

        // Act
        Object result = aspect.measureExecutionTime(joinPoint);

        // Assert
        assertThat(result).isEqualTo("processed");
        verify(processor, times(1)).recordExecution(eq("TestService.methodWithParams"), anyLong());
    }

    // Тестовый сервис для проверки аспекта
    static class TestService {
        @TimedMethod
        public void voidMethod() {}

        @TimedMethod(value = "primitive.int")
        public int intMethod() { return 42; }

        @TimedMethod
        public String stringMethod() { return "result"; }

        @TimedMethod(value = "custom.metric.name")
        public void customNameMethod() {}

        @TimedMethod
        public void exceptionMethod() { throw new RuntimeException("Test exception"); }

        @TimedMethod
        public String methodWithParams(String param1, int param2) { return "processed"; }
    }
}