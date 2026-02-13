package ru.domdom.metrics.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import ru.domdom.metrics.aspect.TimedMethodAspect;
import ru.domdom.metrics.service.MetricFactory;
import ru.domdom.metrics.service.MetricNameResolver;
import ru.domdom.metrics.service.TimedMethodProcessor;

/**
 * Автоконфигурация Spring Boot для стартера сбора метрик методов.
 *
 * <p>Создаёт все необходимые бины при наличии {@link MeterRegistry} в classpath
 * и включённом свойстве {@code method.metrics.enabled} (по умолчанию true).
 *
 * @author Кадыров Андрей
 * @since 1.0.0
 * @see MethodMetricsProperties
 * @see MetricNameResolver
 * @see MetricFactory
 * @see TimedMethodProcessor
 * @see TimedMethodAspect
 */
@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
@EnableConfigurationProperties(MethodMetricsProperties.class)
@ConditionalOnProperty(
        prefix = "method.metrics",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MethodMetricsAutoConfiguration {

    /**
     * Создаёт бин {@link MetricNameResolver}, если он отсутствует.
     *
     * @return экземпляр {@link MetricNameResolver}
     */
    @Bean
    @ConditionalOnMissingBean
    public MetricNameResolver metricNameResolver() {
        return new MetricNameResolver();
    }

    /**
     * Создаёт бин {@link MetricFactory}, если он отсутствует.
     *
     * @param meterRegistry реестр метрик Micrometer
     * @param properties    конфигурационные свойства
     * @return экземпляр {@link MetricFactory}
     */
    @Bean
    @ConditionalOnMissingBean
    public MetricFactory metricFactory(MeterRegistry meterRegistry,
                                       MethodMetricsProperties properties) {
        return new MetricFactory(meterRegistry, properties);
    }

    /**
     * Создаёт бин {@link TimedMethodProcessor}, если он отсутствует.
     *
     * @param nameResolver  резолвер имён метрик
     * @param metricFactory фабрика метрик
     * @return экземпляр {@link TimedMethodProcessor}
     */
    @Bean
    @ConditionalOnMissingBean
    public TimedMethodProcessor timedMethodProcessor(MetricNameResolver nameResolver,
                                                     MetricFactory metricFactory) {
        return new TimedMethodProcessor(nameResolver, metricFactory);
    }

    /**
     * Создаёт бин {@link TimedMethodAspect}, если он отсутствует.
     * Аспект помечен как инфраструктурный ({@code ROLE_INFRASTRUCTURE}).
     *
     * @param processor процессор метрик
     * @return экземпляр {@link TimedMethodAspect}
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean
    public TimedMethodAspect timedMethodAspect(TimedMethodProcessor processor) {
        return new TimedMethodAspect(processor);
    }
}