package com.dom_dom.metrics.autoconfigure;

import com.dom_dom.metrics.annotation.TimedMethod;
import com.dom_dom.metrics.service.TimedMethodProcessor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class MethodMetricsConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MethodMetricsAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @Configuration
    static class TestConfig {
        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Test
    void shouldUseCustomPrefixInMetrics() throws Exception {
        MethodMetricsProperties properties = new MethodMetricsProperties();
        properties.setPrefix("custom.prefix");
        properties.setEnabled(true);

        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        TimedMethodProcessor processor = new TimedMethodProcessor(meterRegistry, properties);

        // Получаем метод и аннотацию
        Method method = TestService.class.getMethod("testMethod");
        TimedMethod annotation = method.getAnnotation(TimedMethod.class);

        // Создаем таймер для метода
        processor.createTimerForMethod("test.service.method", annotation, method);

        // Проверяем, что метрика создана с правильным префиксом и суффиксом .duration
        assertThat(meterRegistry.find("custom.prefix.test.service.method.duration").timer())
                .isNotNull()
                .satisfies(timer -> {
                    // Проверяем, что у таймера есть нужные теги
                    assertThat(timer.getId().getTag("method")).isEqualTo("method");
                    assertThat(timer.getId().getTag("class")).isEqualTo("test.service");
                });

        // Проверяем, что счетчик создан с правильным префиксом и суффиксом .calls
        assertThat(meterRegistry.find("custom.prefix.test.service.method.calls").counter())
                .isNotNull()
                .satisfies(counter -> {
                    // Проверяем, что у счетчика есть нужные теги
                    assertThat(counter.getId().getTag("method")).isEqualTo("method");
                    assertThat(counter.getId().getTag("class")).isEqualTo("test.service");
                });
    }

    @Test
    void shouldDisableHistogramWhenConfigured() {
        contextRunner
                .withPropertyValues(
                        "method.metrics.histogram=false",
                        "method.metrics.enabled=true"
                )
                .run(context -> {
                    MethodMetricsProperties properties = context.getBean(MethodMetricsProperties.class);
                    assertThat(properties.isHistogram()).isFalse();
                });
    }

    @Test
    void shouldUseCustomPercentiles() {
        contextRunner
                .withPropertyValues(
                        "method.metrics.percentiles=0.3,0.6,0.9",
                        "method.metrics.enabled=true"
                )
                .run(context -> {
                    MethodMetricsProperties properties = context.getBean(MethodMetricsProperties.class);
                    assertThat(properties.getPercentiles()).containsExactly(0.3, 0.6, 0.9);
                });
    }

    @Test
    void shouldHandleEmptyPercentilesArray() {
        contextRunner
                .withPropertyValues(
                        "method.metrics.percentiles=",
                        "method.metrics.enabled=true"
                )
                .run(context -> {
                    MethodMetricsProperties properties = context.getBean(MethodMetricsProperties.class);
                    // При пустом значении должны использоваться значения по умолчанию
                    assertThat(properties.getPercentiles()).containsExactly(0.5, 0.95, 0.99);
                });
    }

    @Test
    void shouldDisableMetricsCollection() {
        ApplicationContextRunner disabledRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MethodMetricsAutoConfiguration.class))
                .withUserConfiguration(TestConfig.class)
                .withPropertyValues("method.metrics.enabled=false");

        disabledRunner.run(context -> {
            // При отключенных метриках автоконфигурация не должна создавать бины
            assertThat(context).doesNotHaveBean(MethodMetricsProperties.class);
            assertThat(context).doesNotHaveBean(TimedMethodProcessor.class);
            assertThat(context).doesNotHaveBean(com.dom_dom.metrics.aspect.TimedMethodAspect.class);
            // MeterRegistry из TestConfig должен быть
            assertThat(context).hasSingleBean(MeterRegistry.class);
        });
    }

    @Test
    void shouldWorkWithDefaultPropertiesWhenNotConfigured() {
        ApplicationContextRunner defaultRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MethodMetricsAutoConfiguration.class))
                .withUserConfiguration(TestConfig.class);

        defaultRunner.run(context -> {
            // При отсутствии конфигурации должны использоваться значения по умолчанию
            assertThat(context).hasSingleBean(MethodMetricsProperties.class);
            MethodMetricsProperties properties = context.getBean(MethodMetricsProperties.class);
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getPrefix()).isEqualTo("method");
            assertThat(properties.isHistogram()).isTrue();
            assertThat(properties.getPercentiles()).containsExactly(0.5, 0.95, 0.99);
        });
    }

    @Test
    void shouldHandleInvalidPercentileValues() {
        // Тест должен проверить, что приложение запускается даже с невалидными значениями
        // благодаря ignoreInvalidFields = true
        assertThatCode(() -> {
            contextRunner
                    .withPropertyValues(
                            "method.metrics.percentiles=0.5,invalid,0.95",
                            "method.metrics.enabled=true"
                    )
                    .run(context -> {
                        // Контекст должен запуститься благодаря ignoreInvalidFields
                        assertThat(context).hasNotFailed();
                        // Значения по умолчанию должны быть использованы
                        assertThat(context.getBean(MethodMetricsProperties.class).getPercentiles())
                                .containsExactly(0.5, 0.95, 0.99);
                    });
        }).doesNotThrowAnyException();
    }

    // Тестовый сервис для проверки конфигурации
    static class TestService {
        @TimedMethod(value = "test.service.method")
        public void testMethod() {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}