package ru.domdom.metrics.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import ru.domdom.metrics.annotation.TimedMethod;
import ru.domdom.metrics.config.MethodMetricsProperties;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тест проверяет потокобезопасность {@link MetricFactory}.
 * <p>
 * Множество потоков одновременно запрашивают таймеры и счётчики для одних и тех же ключей.
 * Проверяется, что не возникает исключений и кэш работает корректно.
 * </p>
 *
 * @author Кадыров Андрей
 * @since 1.0.0
 */
class MetricFactoryConcurrencyTest {

    @Test
    void shouldBeThreadSafe() throws InterruptedException {
        MeterRegistry registry = new SimpleMeterRegistry();
        MethodMetricsProperties properties = new MethodMetricsProperties();
        properties.setPrefix("test");
        MetricFactory factory = new MetricFactory(registry, properties);

        int threadCount = 10;
        int iterations = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < iterations; j++) {
                        Method method = MetricFactoryConcurrencyTest.class.getMethod("dummyMethod");
                        TimedMethod annotation = mock(TimedMethod.class);
                        when(annotation.extraTags()).thenReturn(new String[0]);
                        when(annotation.description()).thenReturn("");

                        factory.getTimer("key" + j, annotation, method);
                        factory.getCounter("key" + j, annotation, method);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        // Проверяем, что все ключи закэшированы
        for (int j = 0; j < iterations; j++) {
            assertThat(registry.find("test.key" + j + ".duration").timer()).isNotNull();
            assertThat(registry.find("test.key" + j + ".calls").counter()).isNotNull();
        }
    }

    public void dummyMethod() {}
}