package com.dom_dom.metrics.aspect;

import com.dom_dom.metrics.annotation.TimedMethod;
import com.dom_dom.metrics.autoconfigure.MethodMetricsProperties;
import com.dom_dom.metrics.service.TimedMethodProcessor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = TimedMethodAspectIntegrationTest.TestConfig.class)
class TimedMethodAspectIntegrationTest {

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestConfig {

        @Bean
        public MethodMetricsProperties methodMetricsProperties() {
            MethodMetricsProperties properties = new MethodMetricsProperties();
            properties.setEnabled(true);
            properties.setPrefix("test");
            properties.setHistogram(true);
            properties.setPercentiles(new double[]{0.5, 0.95, 0.99});
            return properties;
        }

        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        public TimedMethodProcessor timedMethodProcessor() {
            return new TimedMethodProcessor(meterRegistry(), methodMetricsProperties());
        }

        @Bean
        public TimedMethodAspect timedMethodAspect() {
            return new TimedMethodAspect(timedMethodProcessor());
        }

        @Bean
        public ComplexTestService complexTestService() {
            return new ComplexTestService();
        }
    }

    static class ComplexTestService {
        @TimedMethod(value = "service.complex.operation")
        public String complexOperation(String input) {
            return "Processed: " + input;
        }

        @TimedMethod(value = "service.recursive.method")
        public int recursiveMethod(int n) {
            if (n <= 1) return 1;
            return n * recursiveMethod(n - 1);
        }

        @TimedMethod(value = "service.chained.method")
        public String chainedMethod() {
            return "Hello World!";
        }

        @TimedMethod(value = "service.with.exception")
        public void methodWithException(boolean shouldThrow) {
            if (shouldThrow) {
                throw new IllegalStateException("Test exception");
            }
        }

        // Метод без аннотации
        public String methodWithoutAnnotation() {
            return "No annotation";
        }
    }

    @Autowired
    private ComplexTestService complexTestService;

    @Autowired
    private TimedMethodProcessor processor;

    @Autowired
    private TimedMethodAspect aspect;

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        // Очищаем ВСЁ состояние перед каждым тестом
        processor.clear();
        aspect.clearCache();

        // Также очищаем MeterRegistry
        if (meterRegistry instanceof SimpleMeterRegistry) {
            SimpleMeterRegistry simpleRegistry = (SimpleMeterRegistry) meterRegistry;
            simpleRegistry.getMeters().forEach(simpleRegistry::remove);
            simpleRegistry.clear();
        }
    }

    @Test
    void shouldMeasureComplexOperation() {
        String result = complexTestService.complexOperation("test");
        assertThat(result).isEqualTo("Processed: test");

        // Таймер должен быть создан после первого вызова
        assertThat(processor.getTimer("service.complex.operation")).isNotNull();
        assertThat(processor.getTimer("service.complex.operation").count()).isEqualTo(1L);

        // Счетчик также должен быть создан
        assertThat(processor.getCounter("service.complex.operation")).isNotNull();
        assertThat(processor.getCallCount("service.complex.operation")).isEqualTo(1.0);
    }

    @Test
    void shouldHandleRecursiveMethods() {
        int result = complexTestService.recursiveMethod(5);
        assertThat(result).isEqualTo(120);

        // Таймер должен быть создан
        assertThat(processor.getTimer("service.recursive.method")).isNotNull();
        assertThat(processor.getTimer("service.recursive.method").count()).isEqualTo(1L);

        // Счетчик должен показывать 1 вызов
        assertThat(processor.getCallCount("service.recursive.method")).isEqualTo(1.0);
    }

    @Test
    void shouldHandleChainedMethods() {
        String result = complexTestService.chainedMethod();
        assertThat(result).isEqualTo("Hello World!");

        // Таймер должен быть создан
        assertThat(processor.getTimer("service.chained.method")).isNotNull();
        assertThat(processor.getTimer("service.chained.method").count()).isEqualTo(1L);

        // Счетчик должен показывать 1 вызов
        assertThat(processor.getCallCount("service.chained.method")).isEqualTo(1.0);
    }

    @Test
    void shouldHandleMethodExceptionsAndRecordTime() {
        // Успешный вызов
        complexTestService.methodWithException(false);
        assertThat(processor.getTimer("service.with.exception")).isNotNull();
        assertThat(processor.getTimer("service.with.exception").count()).isEqualTo(1L);
        assertThat(processor.getCallCount("service.with.exception")).isEqualTo(1.0);

        // Вызов с исключением
        assertThatThrownBy(() -> complexTestService.methodWithException(true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Test exception");

        // Метрика должна быть записана и для исключения
        assertThat(processor.getTimer("service.with.exception").count()).isEqualTo(2L);
        // Счетчик также должен увеличиться
        assertThat(processor.getCallCount("service.with.exception")).isEqualTo(2.0);
    }

    @Test
    void shouldNotMeasureMethodsWithoutAnnotation() {
        String result = complexTestService.methodWithoutAnnotation();
        assertThat(result).isEqualTo("No annotation");

        // Метод без аннотации не должен создавать таймер
        // Проверяем, что нет таймера с именем метода без аннотации
        boolean hasTimerForMethodWithoutAnnotation = processor.getAllTimers()
                .keySet()
                .stream()
                .anyMatch(key -> key.contains("methodWithoutAnnotation"));
        assertThat(hasTimerForMethodWithoutAnnotation).isFalse();

        // Также не должно быть счетчика
        boolean hasCounterForMethodWithoutAnnotation = processor.getAllCounters()
                .keySet()
                .stream()
                .anyMatch(key -> key.contains("methodWithoutAnnotation"));
        assertThat(hasCounterForMethodWithoutAnnotation).isFalse();
    }

    @Test
    void shouldHandleMultipleConcurrentCalls() throws InterruptedException {
        int threadCount = 5;
        int callsPerThread = 10;
        AtomicInteger completedThreads = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                try {
                    startLatch.await(); // Ожидание старта всех потоков
                    for (int j = 0; j < callsPerThread; j++) {
                        complexTestService.complexOperation("call-" + j);
                    }
                    completedThreads.incrementAndGet();
                } catch (Exception e) {
                    // Игнорируем исключения
                } finally {
                    endLatch.countDown();
                }
            });
            threads[i].start();
        }

        // Разрешаем старт
        startLatch.countDown();
        // Ждем завершения всех потоков
        endLatch.await();

        // Все вызовы должны быть учтены (допускаем погрешность +-1 из-за race condition)
        long actualCount = processor.getTimer("service.complex.operation").count();
        long expectedCount = threadCount * callsPerThread;

        // Проверяем с допустимой погрешностью
        assertThat(actualCount)
                .isBetween(expectedCount - 1, expectedCount + 1);

        // Счетчик также должен показывать примерно такое же количество
        double counterCount = processor.getCallCount("service.complex.operation");
        assertThat(counterCount)
                .isBetween(expectedCount - 1.0, expectedCount + 1.0);

        // Проверяем, что все потоки завершились
        assertThat(completedThreads.get()).isEqualTo(threadCount);
    }

    @Test
    void shouldReturnZeroForNonExistentCounter() {
        // Для несуществующего метода счетчик должен возвращать 0
        assertThat(processor.getCallCount("non.existent.method")).isEqualTo(0.0);
        assertThat(processor.getCounter("non.existent.method")).isNull();
    }

    @Test
    void shouldClearBothTimersAndCounters() {
        // Вызываем метод
        complexTestService.complexOperation("test");

        // Проверяем, что метрики созданы
        assertThat(processor.getTimer("service.complex.operation")).isNotNull();
        assertThat(processor.getCounter("service.complex.operation")).isNotNull();
        assertThat(processor.getAllTimers()).hasSize(1);
        assertThat(processor.getAllCounters()).hasSize(1);

        // Очищаем
        processor.clear();

        // Проверяем, что все очищено
        assertThat(processor.getAllTimers()).isEmpty();
        assertThat(processor.getAllCounters()).isEmpty();
    }
}