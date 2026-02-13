package ru.domdom.metrics.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import ru.domdom.metrics.annotation.TimedMethod;
import ru.domdom.metrics.config.MethodMetricsAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционный тест для проверки использования кастомного префикса метрик.
 * <p>
 * Устанавливает свойство {@code method.metrics.prefix=myapp} и проверяет,
 * что имена созданных метрик начинаются с {@code myapp.}.
 * </p>
 *
 * @author Кадыров Андрей
 * @since 1.0.0
 */
@SpringBootTest(properties = "method.metrics.prefix=myapp")
@Import({ MethodMetricsAutoConfiguration.class, AopAutoConfiguration.class, MethodMetricsCustomPrefixTest.TestConfig.class })
public class MethodMetricsCustomPrefixTest {

    @Autowired
    private TestService testService;

    @Autowired
    private MeterRegistry meterRegistry;

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestConfig {
        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        public TestService testService() {
            return new TestService();
        }
    }

    @Component
    static class TestService {
        @TimedMethod(value = "custom.method")
        public String annotatedMethod() {
            return "hello";
        }
    }

    @Test
    void shouldUseCustomPrefix() {
        testService.annotatedMethod();

        var timer = meterRegistry.find("myapp.custom.method.duration").timer();
        var counter = meterRegistry.find("myapp.custom.method.calls").counter();

        assertThat(timer).isNotNull();
        assertThat(counter).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(counter.count()).isEqualTo(1);
    }
}