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
 * Интеграционный тест для проверки поведения аспекта с private методами.
 * <p>
 * Spring AOP не перехватывает private методы, поэтому метрики для них создаваться не должны.
 * </p>
 *
 * @author Кадыров Андрей
 * @since 1.0.0
 */
@SpringBootTest(properties = "method.metrics.prefix=method")
@Import({ MethodMetricsAutoConfiguration.class, AopAutoConfiguration.class, TimedMethodPrivateMethodTest.TestConfig.class })
public class TimedMethodPrivateMethodTest {

    @Autowired
    private PrivateMethodService service;

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
        public PrivateMethodService privateMethodService() {
            return new PrivateMethodService();
        }
    }

    @Component
    static class PrivateMethodService {
        @TimedMethod("private.method")
        private String privateMethod() {
            return "secret";
        }

        public String callPrivate() {
            return privateMethod();
        }
    }

    @Test
    void shouldNotInterceptPrivateMethod() {
        service.callPrivate(); // вызывает private метод внутри того же класса

        // Метрики не должны быть созданы
        var timer = meterRegistry.find("method.private.method.duration").timer();
        var counter = meterRegistry.find("method.private.method.calls").counter();

        assertThat(timer).isNull();
        assertThat(counter).isNull();
    }
}