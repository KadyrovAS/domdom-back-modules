package ru.domdom.metrics.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import ru.domdom.metrics.aspect.TimedMethodAspect;
import ru.domdom.metrics.service.MetricFactory;
import ru.domdom.metrics.service.MetricNameResolver;
import ru.domdom.metrics.service.TimedMethodProcessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты автоконфигурации {@link MethodMetricsAutoConfiguration}.
 * <p>
 * Проверяет создание бинов при включённой конфигурации, их отсутствие при выключенной,
 * привязку свойств и обработку отсутствия {@link MeterRegistry}.
 * </p>
 *
 * @author Кадыров Андрей
 * @since 1.0.0
 */
public class MethodMetricsConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MethodMetricsAutoConfiguration.class))
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

    @Test
    void shouldCreateDefaultBeansWhenEnabled() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MetricNameResolver.class);
            assertThat(context).hasSingleBean(MetricFactory.class);
            assertThat(context).hasSingleBean(TimedMethodProcessor.class);
            assertThat(context).hasSingleBean(TimedMethodAspect.class);
        });
    }

    @Test
    void shouldRespectEnabledFlag() {
        contextRunner.withPropertyValues("method.metrics.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MetricNameResolver.class);
                    assertThat(context).doesNotHaveBean(MetricFactory.class);
                    assertThat(context).doesNotHaveBean(TimedMethodProcessor.class);
                    assertThat(context).doesNotHaveBean(TimedMethodAspect.class);
                });
    }

    @Test
    void shouldBindProperties() {
        contextRunner.withPropertyValues(
                "method.metrics.prefix=test",
                "method.metrics.histogram=false",
                "method.metrics.percentiles=0.1,0.5,0.9"
        ).run(context -> {
            MethodMetricsProperties properties = context.getBean(MethodMetricsProperties.class);
            assertThat(properties.getPrefix()).isEqualTo("test");
            assertThat(properties.isHistogram()).isFalse();
            assertThat(properties.getPercentiles()).containsExactly(0.1, 0.5, 0.9);
        });
    }

    @Test
    void shouldFallbackToDefaultsWhenPropertiesMissing() {
        contextRunner.run(context -> {
            MethodMetricsProperties properties = context.getBean(MethodMetricsProperties.class);
            assertThat(properties.getPrefix()).isEqualTo("method");
            assertThat(properties.isHistogram()).isTrue();
            assertThat(properties.getPercentiles()).containsExactly(0.5, 0.95, 0.99);
        });
    }

    @Test
    void shouldNotCreateBeansWhenMeterRegistryMissing() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MethodMetricsAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(UnsatisfiedDependencyException.class)
                            .hasMessageContaining("MeterRegistry");
                });
    }
}