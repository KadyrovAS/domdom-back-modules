package ru.domdom.metrics.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import ru.domdom.metrics.config.MethodMetricsAutoConfiguration;
import ru.domdom.metrics.config.MethodMetricsProperties;
import ru.domdom.metrics.service.TimedMethodProcessor;

import static org.assertj.core.api.Assertions.assertThat;

public class MethodMetricsConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MethodMetricsAutoConfiguration.class))
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

    @Test
    void shouldCreateDefaultBeansWhenEnabled() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TimedMethodProcessor.class);
            assertThat(context).hasSingleBean(TimedMethodAspect.class);
            assertThat(context).hasSingleBean(MethodMetricsProperties.class);
        });
    }

    @Test
    void shouldRespectEnabledFlag() {
        contextRunner.withPropertyValues("method.metrics.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(TimedMethodProcessor.class);
                    assertThat(context).doesNotHaveBean(TimedMethodAspect.class);
                });
    }

    @Test
    void shouldBindProperties() {
        contextRunner.withPropertyValues(
                "method.metrics.histogram=false",
                "method.metrics.percentiles=0.1,0.5,0.9"
        ).run(context -> {
            MethodMetricsProperties properties = context.getBean(MethodMetricsProperties.class);
            assertThat(properties.isHistogram()).isFalse();
            assertThat(properties.getPercentiles()).containsExactly(0.1, 0.5, 0.9);
        });
    }

    @Test
    void shouldFallbackToDefaultsWhenPropertiesMissing() {
        contextRunner.run(context -> {
            MethodMetricsProperties properties = context.getBean(MethodMetricsProperties.class);
            assertThat(properties.isHistogram()).isTrue(); // значение по умолчанию
        });
    }

    @Test
    void shouldNotCreateBeansWhenMeterRegistryMissing() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MethodMetricsAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(TimedMethodProcessor.class);
                    assertThat(context).doesNotHaveBean(TimedMethodAspect.class);
                });
    }
}