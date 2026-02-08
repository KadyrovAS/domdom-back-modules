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
import ru.domdom.metrics.service.TimedMethodProcessor;

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

    @Bean
    @ConditionalOnMissingBean
    public TimedMethodProcessor timedMethodProcessor(
            MeterRegistry meterRegistry,
            MethodMetricsProperties properties) {
        return new TimedMethodProcessor(meterRegistry, properties);
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean
    public TimedMethodAspect timedMethodAspect(TimedMethodProcessor processor) {
        return new TimedMethodAspect(processor);
    }
}
