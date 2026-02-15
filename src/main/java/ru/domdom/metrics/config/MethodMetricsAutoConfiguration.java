package ru.domdom.metrics.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.domdom.metrics.aspect.TimedMethodAspect;
import ru.domdom.metrics.service.TimedMethodProcessor;

@Configuration
@EnableConfigurationProperties(MethodMetricsProperties.class)
@ConditionalOnProperty(name = "method.metrics.enabled", havingValue = "true", matchIfMissing = true)
public class MethodMetricsAutoConfiguration {

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    public TimedMethodProcessor timedMethodProcessor(MeterRegistry meterRegistry,
                                                     MethodMetricsProperties properties) {
        return new TimedMethodProcessor(meterRegistry, properties);
    }

    @Bean
    @ConditionalOnBean(TimedMethodProcessor.class)
    public TimedMethodAspect timedMethodAspect(TimedMethodProcessor processor) {
        return new TimedMethodAspect(processor);
    }
}