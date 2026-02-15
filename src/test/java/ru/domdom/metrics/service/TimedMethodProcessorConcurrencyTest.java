package ru.domdom.metrics.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import ru.domdom.metrics.config.MethodMetricsProperties;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TimedMethodProcessorConcurrencyTest {

    @Test
    void shouldBeThreadSafe() throws InterruptedException {
        MeterRegistry registry = new SimpleMeterRegistry();
        MethodMetricsProperties properties = new MethodMetricsProperties();
        properties.setPercentiles(new double[]{0.5, 0.95});
        TimedMethodProcessor processor = new TimedMethodProcessor(registry, properties);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        try {
            when(joinPoint.proceed()).thenReturn("ok");
        } catch (Throwable t) {
            // ignore
        }

        int threadCount = 10;
        int iterations = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        String metricName = "concurrent.test";
        Map<String, String> tags = Map.of();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < iterations; j++) {
                        processor.process(joinPoint, metricName, tags);
                    }
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        var timer = registry.find(metricName).timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(threadCount * iterations);
    }
}