package ru.domdom.metrics.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;
import ru.domdom.metrics.config.MethodMetricsProperties;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Component
@RequiredArgsConstructor
public class TimedMethodProcessor {

    private final MeterRegistry meterRegistry;
    private final MethodMetricsProperties properties;

    public Object process(ProceedingJoinPoint joinPoint, @NonNull String metricName, Map<String, String> extraTags) throws Throwable {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return joinPoint.proceed();
        } finally {
            List<Tag> tags = extraTags.entrySet().stream()
                    .map(entry -> Tag.of(entry.getKey(), entry.getValue()))
                    .collect(toList());

            Timer.Builder timerBuilder = Timer.builder(metricName)
                    .tags(tags)
                    .publishPercentiles(properties.getPercentiles());

            sample.stop(timerBuilder.register(meterRegistry));
        }
    }
}