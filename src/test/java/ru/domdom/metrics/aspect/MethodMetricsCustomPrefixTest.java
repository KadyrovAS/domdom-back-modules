package ru.domdom.metrics.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import ru.domdom.metrics.annotation.TimedMethod;
import ru.domdom.metrics.config.MethodMetricsProperties;
import ru.domdom.metrics.service.TimedMethodProcessor;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(MethodMetricsCustomPrefixTest.TestConfig.class)
public class MethodMetricsCustomPrefixTest {

    @Autowired
    private TestService testService;

    @Autowired
    private MeterRegistry meterRegistry;

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestConfig {
        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        public MethodMetricsProperties methodMetricsProperties() {
            MethodMetricsProperties props = new MethodMetricsProperties();
            props.setEnabled(true);
            props.setHistogram(true);
            props.setPercentiles(new double[]{0.5, 0.95});
            return props;
        }

        @Bean
        public TimedMethodProcessor timedMethodProcessor(MeterRegistry meterRegistry,
                                                         MethodMetricsProperties properties) {
            return new TimedMethodProcessor(meterRegistry, properties);
        }

        @Bean
        public TimedMethodAspect timedMethodAspect(TimedMethodProcessor processor) {
            return new TimedMethodAspect(processor);
        }

        @Bean
        public TestService testService() {
            return new TestService();
        }
    }

    @Component
    static class TestService {
        @TimedMethod("custom.method")
        public String annotatedMethod() {
            return "hello";
        }
    }

    @Test
    void shouldUseExplicitMetricName() {
        testService.annotatedMethod();

        var timer = meterRegistry.find("custom.method").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }
}