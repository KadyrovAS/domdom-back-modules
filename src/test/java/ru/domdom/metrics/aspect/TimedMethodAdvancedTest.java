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

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Интеграционный тест для расширенных сценариев работы аспекта {@link TimedMethodAspect}.
 * <p>
 * Проверяет:
 * <ul>
 *   <li>запись метрик для методов с явным и автоматическим именем</li>
 *   <li>обработку исключений</li>
 *   <li>работу с параметрами</li>
 *   <li>прокси интерфейсов (используется JDK-прокси для корректного распознавания аннотаций)</li>
 * </ul>
 * Префикс метрик фиксирован через свойство {@code method.metrics.prefix=method}.
 * Поле для бина, реализующего интерфейс, объявлено как тип интерфейса, чтобы избежать проблем с инжекцией прокси.
 * </p>
 *
 * @author Кадыров Андрей
 * @since 1.0.0
 */
@SpringBootTest(properties = {
        "method.metrics.prefix=method",
        "spring.aop.proxy-target-class=false" // гарантирует использование JDK-прокси
})
@Import({ MethodMetricsAutoConfiguration.class, AopAutoConfiguration.class, TimedMethodAdvancedTest.TestConfig.class })
public class TimedMethodAdvancedTest {

    @Autowired
    private AdvancedService advancedService;

    @Autowired
    private ServiceInterface interfaceImpl; // тип интерфейса, а не конкретного класса

    @Autowired
    private MeterRegistry meterRegistry;

    @TestConfiguration
    @EnableAspectJAutoProxy // используем JDK-прокси по умолчанию
    static class TestConfig {
        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
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
        @TimedMethod(value = "advanced.method")
        public String simpleMethod() {
            return "ok";
        }

        @TimedMethod
        public String methodWithoutValue() {
            return "ok";
        }

        @TimedMethod(value = "advanced.throws")
        public void throwingMethod() {
            throw new RuntimeException("test exception");
        }

        @TimedMethod(value = "advanced.withParams")
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
        @TimedMethod("advanced.interface") // аннотация на методе реализации
        public String interfaceMethod() {
            return "from interface";
        }
    }

    @Test
    void shouldRecordMetricsForSimpleMethod() {
        advancedService.simpleMethod();

        var timer = meterRegistry.find("method.advanced.method.duration").timer();
        var counter = meterRegistry.find("method.advanced.method.calls").counter();

        assertThat(timer).isNotNull();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    void shouldGenerateMetricNameWhenValueIsEmpty() {
        advancedService.methodWithoutValue();

        String expectedBase = "AdvancedService.methodWithoutValue";
        var timer = meterRegistry.find("method." + expectedBase + ".duration").timer();
        var counter = meterRegistry.find("method." + expectedBase + ".calls").counter();

        assertThat(timer).isNotNull();
        assertThat(counter).isNotNull();
    }

    @Test
    void shouldRecordMetricsEvenWhenMethodThrowsException() {
        assertThatThrownBy(() -> advancedService.throwingMethod())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("test exception");

        var timer = meterRegistry.find("method.advanced.throws.duration").timer();
        var counter = meterRegistry.find("method.advanced.throws.calls").counter();

        assertThat(timer).isNotNull();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1);
        assertThat(timer.totalTime(TimeUnit.NANOSECONDS)).isPositive();
    }

    @Test
    void shouldRecordMetricsForMethodWithParameters() {
        advancedService.methodWithParams("test", 42);

        var timer = meterRegistry.find("method.advanced.withParams.duration").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void shouldWorkWithInterfaceProxies() {
        interfaceImpl.interfaceMethod();

        var timer = meterRegistry.find("method.advanced.interface.duration").timer();
        var counter = meterRegistry.find("method.advanced.interface.calls").counter();

        assertThat(timer).isNotNull();
        assertThat(counter).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(counter.count()).isEqualTo(1);
    }
}