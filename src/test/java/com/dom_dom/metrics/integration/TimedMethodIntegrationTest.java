package com.dom_dom.metrics.integration;

import com.dom_dom.metrics.annotation.TimedMethod;
import com.dom_dom.metrics.aspect.TimedMethodAspect;
import com.dom_dom.metrics.autoconfigure.MethodMetricsProperties;
import com.dom_dom.metrics.service.TimedMethodProcessor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TimedMethodIntegrationTest.TestConfig.class)
class TimedMethodIntegrationTest {

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestConfig {

        @Bean
        public MethodMetricsProperties methodMetricsProperties() {
            MethodMetricsProperties properties = new MethodMetricsProperties();
            properties.setEnabled(true);
            properties.setPrefix("method");
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
        public TestService testService() {
            return new TestService();
        }
    }

    static class TestService {
        @TimedMethod(value = "test.service.process",
                description = "Process test data",
                extraTags = {"service=test", "env=test"})
        public String process(String input) {
            return "Processed: " + input;
        }

        @TimedMethod(value = "test.service.validate", description = "Validate input data")
        public boolean validate(String input) {
            return input != null && !input.trim().isEmpty();
        }

        @TimedMethod // Без явного имени
        public void doWork() {
            // Пустой метод
        }
    }

    @Autowired
    private TestService testService;

    @Autowired
    private TimedMethodProcessor processor;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private TimedMethodAspect aspect;

    @BeforeEach
    void setUp() {
        // Очищаем ВСЁ состояние перед каждым тестом
        processor.clear();
        aspect.clearCache();

        // Также очищаем MeterRegistry
        if (meterRegistry instanceof SimpleMeterRegistry) {
            SimpleMeterRegistry simpleRegistry = (SimpleMeterRegistry) meterRegistry;
            // Удаляем все метрики из реестра
            simpleRegistry.getMeters().forEach(simpleRegistry::remove);
            simpleRegistry.clear();
        }
    }

    @Test
    void contextLoads() {
        assertThat(testService).isNotNull();
        assertThat(processor).isNotNull();
        assertThat(meterRegistry).isNotNull();
        assertThat(aspect).isNotNull();
    }

    @Test
    void shouldMeasureMethodExecutionTime() {
        // Сначала вызываем метод - таймер создастся автоматически
        String result = testService.process("test");
        assertThat(result).isEqualTo("Processed: test");

        // Проверяем, что таймер создан
        assertThat(processor.getTimer("test.service.process")).isNotNull();
        // Проверяем, что запись произошла
        assertThat(processor.getTimer("test.service.process").count()).isEqualTo(1L);
    }

    @Test
    void shouldRecordMultipleExecutions() {
        // Вызываем метод несколько раз
        testService.process("test1");
        testService.process("test2");

        // Проверяем счетчик вызовов
        assertThat(processor.getTimer("test.service.process").count()).isEqualTo(2L);
    }

    @Test
    void shouldUseDefaultMetricNameWhenValueNotSpecified() {
        // Вызываем метод без явного имени
        testService.doWork();

        // Проверяем, что таймер создан с именем по умолчанию
        assertThat(processor.getTimer("TestService.doWork")).isNotNull();
        assertThat(processor.getTimer("TestService.doWork").count()).isEqualTo(1L);
    }

    @Test
    void shouldProcessAllAnnotatedMethods() {
        // Вызываем все методы
        testService.process("test");
        testService.validate("test");
        testService.doWork();

        // Проверяем, что все таймеры созданы
        assertThat(processor.getTimer("test.service.process")).isNotNull();
        assertThat(processor.getTimer("test.service.validate")).isNotNull();
        assertThat(processor.getTimer("TestService.doWork")).isNotNull();
        assertThat(processor.getAllTimers()).hasSize(3);
    }

    @Test
    void shouldClearMetricsProperly() {
        // Вызываем метод
        testService.process("test");

        // Проверяем, что таймер создан и счетчик = 1
        assertThat(processor.getTimer("test.service.process")).isNotNull();
        assertThat(processor.getTimer("test.service.process").count()).isEqualTo(1L);

        // Очищаем кэш
        processor.clear();
        aspect.clearCache();

        // Удаляем метрики из реестра
        if (meterRegistry instanceof SimpleMeterRegistry) {
            SimpleMeterRegistry simpleRegistry = (SimpleMeterRegistry) meterRegistry;
            simpleRegistry.getMeters().forEach(simpleRegistry::remove);
            simpleRegistry.clear();
        }

        // После очистки таймеры должны быть удалены из кэша
        assertThat(processor.getAllTimers()).isEmpty();

        // Но при вызове метода таймер создастся заново
        String result = testService.process("test2");
        assertThat(result).isEqualTo("Processed: test2");

        // Таймер создастся заново
        assertThat(processor.getTimer("test.service.process")).isNotNull();
        // И счетчик начнется с 1
        assertThat(processor.getTimer("test.service.process").count()).isEqualTo(1L);
    }

    @Test
    void shouldHandleMetricsDataProperly() {
        // Вызываем метод несколько раз
        testService.process("test1");
        testService.process("test2");
        testService.process("test3");

        // Проверяем метрики через таймер
        var timer = processor.getTimer("test.service.process");
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(3L);
        assertThat(timer.totalTime(TimeUnit.NANOSECONDS)).isGreaterThan(0);
        assertThat(timer.mean(TimeUnit.NANOSECONDS)).isGreaterThan(0);
    }

    @Test
    void shouldCreateSeparateCounterForMethodCalls() {
        // Вызываем метод
        testService.process("test1");

        // Проверяем, что создан отдельный счетчик вызовов
        Counter counter = processor.getCounter("test.service.process");
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);

        // Проверяем, что таймер также создан
        assertThat(processor.getTimer("test.service.process")).isNotNull();

        // Проверяем, что имена метрик различаются
        assertThat(counter.getId().getName()).isEqualTo("method.test.service.process.calls");
        assertThat(processor.getTimer("test.service.process").getId().getName())
                .isEqualTo("method.test.service.process.duration");
    }

    @Test
    void shouldIncrementCounterOnEachMethodCall() {
        // Вызываем метод несколько раз
        testService.process("test1");
        testService.process("test2");
        testService.process("test3");

        // Проверяем счетчик
        Counter counter = processor.getCounter("test.service.process");
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(3.0);

        // Проверяем через метод getCallCount
        assertThat(processor.getCallCount("test.service.process")).isEqualTo(3.0);

        // Проверяем, что таймер также имеет правильный счетчик
        assertThat(processor.getTimer("test.service.process").count()).isEqualTo(3L);
    }

    @Test
    void shouldCalculateAverageTimeCorrectly() {
        // Вызываем метод несколько раз
        testService.process("test1");
        testService.process("test2");
        testService.process("test3");

        // Получаем среднее время
        double avgTime = processor.getAverageTime("test.service.process");

        // Проверяем, что среднее время > 0
        assertThat(avgTime).isGreaterThan(0);

        // Проверяем расчет: общее время / количество вызовов
        double totalTime = processor.getTotalTime("test.service.process");
        double callCount = processor.getCallCount("test.service.process");
        assertThat(avgTime).isEqualTo(totalTime / callCount);
    }

    @Test
    void shouldGetAllCounters() {
        // Вызываем методы
        testService.process("test");
        testService.validate("test");
        testService.doWork();

        // Проверяем, что все счетчики созданы
        Map<String, Counter> counters = processor.getAllCounters();
        assertThat(counters).hasSize(3);

        // Проверяем конкретные счетчики
        assertThat(counters.get("test.service.process")).isNotNull();
        assertThat(counters.get("test.service.validate")).isNotNull();
        assertThat(counters.get("TestService.doWork")).isNotNull();
    }

    @Test
    void shouldGetMethodMetrics() {
        // Вызываем метод несколько раз
        testService.process("test1");
        testService.process("test2");

        // Получаем все метрики метода
        Map<String, Object> metrics = processor.getMethodMetrics("test.service.process");

        // Проверяем, что все метрики присутствуют
        assertThat(metrics).isNotEmpty();
        assertThat(metrics).containsKeys("calls", "totalTime", "averageTime", "maxTime", "method");

        // Проверяем значения
        assertThat((Double) metrics.get("calls")).isEqualTo(2.0);
        assertThat((String) metrics.get("method")).isEqualTo("test.service.process");
        assertThat((Double) metrics.get("totalTime")).isGreaterThan(0);
        assertThat((Double) metrics.get("averageTime")).isGreaterThan(0);
    }

    @Test
    void shouldHandleEmptyMetricsForNonExistingMethod() {
        // Получаем метрики для несуществующего метода
        Map<String, Object> metrics = processor.getMethodMetrics("non.existing.method");

        // Должна вернуться пустая Map
        assertThat(metrics).isEmpty();

        // Проверяем методы получения отдельных значений
        assertThat(processor.getCallCount("non.existing.method")).isEqualTo(0.0);
        assertThat(processor.getTotalTime("non.existing.method")).isEqualTo(0.0);
        assertThat(processor.getAverageTime("non.existing.method")).isEqualTo(0.0);
    }
}