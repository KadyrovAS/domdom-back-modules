package ru.domdom.metrics.config;

import ru.domdom.metrics.aspect.TimedMethodAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.domdom.metrics.service.TimedMethodProcessor;

import static org.assertj.core.api.Assertions.assertThat;

class MethodMetricsAutoConfigurationTest {

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
    void shouldCreateBeansWhenEnabled() {
        contextRunner
                .withPropertyValues("method.metrics.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(MethodMetricsProperties.class);
                    assertThat(context.getBean(MethodMetricsProperties.class).isEnabled()).isTrue();
                    assertThat(context).hasSingleBean(TimedMethodProcessor.class);
                    assertThat(context).hasSingleBean(TimedMethodAspect.class);
                });
    }

    @Test
    void shouldNotCreateBeansWhenDisabled() {
        contextRunner
                .withPropertyValues("method.metrics.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MethodMetricsProperties.class);
                    assertThat(context).doesNotHaveBean(TimedMethodProcessor.class);
                    assertThat(context).doesNotHaveBean(TimedMethodAspect.class);
                });
    }

    @Test
    void shouldHaveDefaultProperties() {
        contextRunner
                .withPropertyValues("method.metrics.enabled=true")
                .run(context -> {
                    MethodMetricsProperties properties = context.getBean(MethodMetricsProperties.class);
                    assertThat(properties.getPrefix()).isEqualTo("method");
                    assertThat(properties.isHistogram()).isTrue();
                    assertThat(properties.getPercentiles()).containsExactly(0.5, 0.95, 0.99);
                });
    }
}