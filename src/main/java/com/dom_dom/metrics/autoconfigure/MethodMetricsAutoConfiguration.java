package com.dom_dom.metrics.autoconfigure;

import com.dom_dom.metrics.aspect.TimedMethodAspect;
import com.dom_dom.metrics.service.TimedMethodProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Автоконфигурация Spring Boot для сбора метрик методов с ленивой инициализацией.
 *
 * @author Кадыров Андрей
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
@EnableConfigurationProperties(MethodMetricsProperties.class)
@ConditionalOnProperty(prefix = "method.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MethodMetricsAutoConfiguration {

    /**
     * Создает бин TimedMethodProcessor.
     */
    @Bean
    @ConditionalOnMissingBean
    public TimedMethodProcessor timedMethodProcessor(
            io.micrometer.core.instrument.MeterRegistry meterRegistry,
            MethodMetricsProperties properties) {
        return new TimedMethodProcessor(meterRegistry, properties);
    }

    /**
     * Создает бин TimedMethodAspect.
     */
    @Bean
    @ConditionalOnMissingBean
    public TimedMethodAspect timedMethodAspect(TimedMethodProcessor processor) {
        return new TimedMethodAspect(processor);
    }
}