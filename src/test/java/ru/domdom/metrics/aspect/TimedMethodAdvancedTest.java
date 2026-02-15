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

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TimedMethodAdvancedTest.TestConfig.class)
public class TimedMethodAdvancedTest {

    @Autowired
    private AdvancedService advancedService;

    @Autowired
    private InterfaceImpl interfaceImpl;

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
        public AdvancedService advancedService() {
            return new AdvancedService();
        }

        @Bean
        public InterfaceImpl interfaceImpl() {
            return new InterfaceImpl();
        }
    }

    @Component
    static class AdvancedService {
        @TimedMethod("advanced.simple")
        public String simpleMethod() {
            return "ok";
        }

        @TimedMethod("advanced.throws")
        public void throwingMethod() {
            throw new RuntimeException("test exception");
        }

        @TimedMethod("advanced.withParams")
        public String methodWithParams(String arg, int num) {
            return arg + num;
        }
    }

    interface ServiceInterface {
        String interfaceMethod();
    }

    @Component
    static class InterfaceImpl implements ServiceInterface {
        @Override
        @TimedMethod("advanced.interface")
        public String interfaceMethod() {
            return "from interface";
        }
    }

    @Test
    void shouldRecordMetricsForSimpleMethod() {
        advancedService.simpleMethod();

        var timer = meterRegistry.find("advanced.simple").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void shouldRecordMetricsEvenWhenMethodThrowsException() {
        assertThatThrownBy(() -> advancedService.throwingMethod())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("test exception");

        var timer = meterRegistry.find("advanced.throws").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(TimeUnit.NANOSECONDS)).isPositive();
    }

    @Test
    void shouldRecordMetricsForMethodWithParameters() {
        advancedService.methodWithParams("test", 42);

        var timer = meterRegistry.find("advanced.withParams").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void shouldWorkWithInterfaceProxies() {
        interfaceImpl.interfaceMethod();

        var timer = meterRegistry.find("advanced.interface").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }
}