package ru.domdom.metrics.aspect;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import ru.domdom.metrics.annotation.TimedMethod;
import ru.domdom.metrics.service.TimedMethodProcessor;
import ru.domdom.metrics.service.TagUtils;

import java.util.Map;

@Aspect
@RequiredArgsConstructor
public class TimedMethodAspect {

    private final TimedMethodProcessor processor;

    @Around("@annotation(timed)")
    public Object measure(ProceedingJoinPoint joinPoint, TimedMethod timed) throws Throwable {
        String metricName = timed.value();
        Map<String, String> extraTags = TagUtils.fromExtraTags(timed.extraTags());
        return processor.process(joinPoint, metricName, extraTags);
    }
}