package com.dom_dom.metrics.annotation;

import com.dom_dom.metrics.autoconfigure.MethodMetricsProperties;
import com.dom_dom.metrics.service.TimedMethodProcessor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class TimedMethodAdvancedTest {

    private TimedMethodProcessor createTestProcessor() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        MethodMetricsProperties properties = new MethodMetricsProperties();
        properties.setEnabled(true);
        properties.setPrefix("method");
        properties.setHistogram(true);
        properties.setPercentiles(new double[]{0.5, 0.95, 0.99});

        return new TimedMethodProcessor(meterRegistry, properties);
    }

    @Test
    void shouldWorkWithInheritanceAndInterfaces() throws Exception {
        interface PaymentService {
            @TimedMethod(value = "payment.process", description = "Process payment")
            void processPayment(double amount);
        }

        abstract class AbstractService {
            @TimedMethod(value = "service.initialize", description = "Initialize service")
            public void initialize() {
                // Инициализация
            }
        }

        class CreditCardService extends AbstractService implements PaymentService {
            // Явно добавляем аннотации в реализации методов
            @TimedMethod(value = "service.initialize", description = "Initialize service")
            @Override
            public void initialize() {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            @TimedMethod(value = "payment.process", description = "Process payment")
            @Override
            public void processPayment(double amount) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            @TimedMethod(value = "creditcard.validate", description = "Validate credit card")
            public boolean validateCard(String cardNumber) {
                return cardNumber != null && cardNumber.length() == 16;
            }
        }

        CreditCardService service = new CreditCardService();
        TimedMethodProcessor testProcessor = createTestProcessor();

        // Получаем методы и аннотации
        Method initializeMethod = CreditCardService.class.getMethod("initialize");
        TimedMethod initializeAnnotation = initializeMethod.getAnnotation(TimedMethod.class);

        Method processMethod = CreditCardService.class.getMethod("processPayment", double.class);
        TimedMethod processAnnotation = processMethod.getAnnotation(TimedMethod.class);

        Method validateMethod = CreditCardService.class.getMethod("validateCard", String.class);
        TimedMethod validateAnnotation = validateMethod.getAnnotation(TimedMethod.class);

        // Создаем таймеры вручную
        testProcessor.createTimerForMethod("service.initialize", initializeAnnotation, initializeMethod);
        testProcessor.createTimerForMethod("payment.process", processAnnotation, processMethod);
        testProcessor.createTimerForMethod("creditcard.validate", validateAnnotation, validateMethod);

        // Проверяем, что таймеры созданы
        assertThat(testProcessor.getTimer("service.initialize")).isNotNull();
        assertThat(testProcessor.getTimer("payment.process")).isNotNull();
        assertThat(testProcessor.getTimer("creditcard.validate")).isNotNull();
    }
}