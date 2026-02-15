package ru.domdom.metrics.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import ru.domdom.metrics.config.MethodMetricsProperties;
import ru.domdom.metrics.exception.MissingMetricNameException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TimedMethodProcessor {

    private final MeterRegistry meterRegistry;
    private final MethodMetricsProperties properties;

    public TimedMethodProcessor(MeterRegistry meterRegistry, MethodMetricsProperties properties) {
        this.meterRegistry = meterRegistry;
        this.properties = properties;
    }

    public Object process(ProceedingJoinPoint joinPoint, String metricName, Map<String, String> extraTags) throws Throwable {
        if (metricName == null || metricName.trim().isEmpty()) {
            throw new MissingMetricNameException("Metric name must not be empty");
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return joinPoint.proceed();
        } finally {
            List<Tag> tags = extraTags.entrySet().stream()
                    .map(entry -> Tag.of(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());

            Timer.Builder timerBuilder = Timer.builder(metricName)
                    .tags(tags)
                    .publishPercentiles(properties.getPercentiles()); // <-- теперь должно работать

            sample.stop(timerBuilder.register(meterRegistry));
        }
    }
}