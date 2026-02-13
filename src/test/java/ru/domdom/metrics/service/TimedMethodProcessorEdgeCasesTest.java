package ru.domdom.metrics.service;

import ru.domdom.metrics.annotation.TimedMethod;
import ru.domdom.metrics.config.MethodMetricsProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TimedMethodProcessorEdgeCasesTest {

    private TimedMethodProcessor createTestProcessor() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        MethodMetricsProperties properties = new MethodMetricsProperties();
        properties.setEnabled(true);
        properties.setPrefix("method");
        properties.setHistogram(true);
        properties.setPercentiles(new double[]{0.5, 0.95, 0.99});
        return new TimedMethodProcessor(meterRegistry, properties);
    }

    // ... существующие тесты ...

    @Test
    void shouldCreateBothTimerAndCounter() throws Exception {
        class TestService {
            @TimedMethod(value = "test.method")
            public void testMethod() {}
        }

        TestService service = new TestService();
        TimedMethodProcessor processor = createTestProcessor();

        Method method = TestService.class.getMethod("testMethod");
        TimedMethod annotation = method.getAnnotation(TimedMethod.class);

        processor.createTimerForMethod("test.method", annotation, method);

        // Проверяем, что созданы и таймер, и счетчик
        assertThat(processor.getTimer("test.method")).isNotNull();
        assertThat(processor.getCounter("test.method")).isNotNull();
        assertThat(processor.getCallCount("test.method")).isEqualTo(0.0);
    }

    @Test
    void shouldIncrementCounterOnRecordExecution() throws Exception {
        class TestService {
            @TimedMethod(value = "test.method")
            public void testMethod() {}
        }

        TestService service = new TestService();
        TimedMethodProcessor processor = createTestProcessor();

        Method method = TestService.class.getMethod("testMethod");
        TimedMethod annotation = method.getAnnotation(TimedMethod.class);

        processor.createTimerForMethod("test.method", annotation, method);

        // Имитируем вызовы метода
        processor.recordExecution("test.method", 1000L);
        processor.recordExecution("test.method", 2000L);
        processor.recordExecution("test.method", 1500L);

        // Проверяем счетчик
        assertThat(processor.getCallCount("test.method")).isEqualTo(3.0);

        // Проверяем таймер
        assertThat(processor.getTimer("test.method").count()).isEqualTo(3L);
        assertThat(processor.getTotalTime("test.method")).isEqualTo(4500.0);
        assertThat(processor.getAverageTime("test.method")).isEqualTo(1500.0);
    }

    @Test
    void shouldReturnEmptyMetricsForNonExistingMethod() {
        TimedMethodProcessor processor = createTestProcessor();

        Map<String, Object> metrics = processor.getMethodMetrics("non.existing");
        assertThat(metrics).isEmpty();
    }

    @Test
    void shouldReturnCompleteMetricsForExistingMethod() throws Exception {
        class TestService {
            @TimedMethod(value = "test.method")
            public void testMethod() {}
        }

        TestService service = new TestService();
        TimedMethodProcessor processor = createTestProcessor();

        Method method = TestService.class.getMethod("testMethod");
        TimedMethod annotation = method.getAnnotation(TimedMethod.class);

        processor.createTimerForMethod("test.method", annotation, method);

        // Имитируем несколько вызовов
        processor.recordExecution("test.method", 1000L);
        processor.recordExecution("test.method", 2000L);

        // Получаем метрики
        Map<String, Object> metrics = processor.getMethodMetrics("test.method");

        // Проверяем структуру метрик
        assertThat(metrics)
                .containsKeys("calls", "totalTime", "averageTime", "maxTime", "method");

        // Проверяем значения
        assertThat((Double) metrics.get("calls")).isEqualTo(2.0);
        assertThat((String) metrics.get("method")).isEqualTo("test.method");
        assertThat((Double) metrics.get("totalTime")).isEqualTo(3000.0);
        assertThat((Double) metrics.get("averageTime")).isEqualTo(1500.0);
    }

    @Test
    void shouldClearAllCaches() throws Exception {
        class TestService {
            @TimedMethod(value = "test.method1")
            public void method1() {}

            @TimedMethod(value = "test.method2")
            public void method2() {}
        }

        TestService service = new TestService();
        TimedMethodProcessor processor = createTestProcessor();

        Method method1 = TestService.class.getMethod("method1");
        TimedMethod annotation1 = method1.getAnnotation(TimedMethod.class);

        Method method2 = TestService.class.getMethod("method2");
        TimedMethod annotation2 = method2.getAnnotation(TimedMethod.class);

        processor.createTimerForMethod("test.method1", annotation1, method1);
        processor.createTimerForMethod("test.method2", annotation2, method2);

        // Проверяем, что созданы
        assertThat(processor.getAllTimers()).hasSize(2);
        assertThat(processor.getAllCounters()).hasSize(2);

        // Очищаем
        processor.clear();

        // Проверяем, что все очищено
        assertThat(processor.getAllTimers()).isEmpty();
        assertThat(processor.getAllCounters()).isEmpty();
        assertThat(processor.getTimerCount()).isEqualTo(0);
        assertThat(processor.getCounterCount()).isEqualTo(0);
    }

    @Test
    void shouldHandleZeroCallsForAverageTime() throws Exception {
        class TestService {
            @TimedMethod(value = "test.method")
            public void testMethod() {}
        }

        TestService service = new TestService();
        TimedMethodProcessor processor = createTestProcessor();

        Method method = TestService.class.getMethod("testMethod");
        TimedMethod annotation = method.getAnnotation(TimedMethod.class);

        processor.createTimerForMethod("test.method", annotation, method);

        // Для метода без вызовов среднее время должно быть 0
        assertThat(processor.getAverageTime("test.method")).isEqualTo(0.0);
        assertThat(processor.getTotalTime("test.method")).isEqualTo(0.0);
        assertThat(processor.getCallCount("test.method")).isEqualTo(0.0);
    }
}