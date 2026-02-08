package ru.domdom.metrics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.domdom.metrics.annotation.TimedMethod;
import ru.domdom.metrics.config.MethodMetricsProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class TimedMethodProcessor {

    private final MeterRegistry meterRegistry;
    private final MethodMetricsProperties properties;
    private final ConcurrentMap<String, Timer> timerCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> counterCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TimedMethod> annotationCache = new ConcurrentHashMap<>();

    public void createTimerForMethod(String metricKey, TimedMethod annotation, Method method) {
        if (timerCache.containsKey(metricKey) && counterCache.containsKey(metricKey)) {
            return; // Таймер и счетчик уже созданы
        }

        String fullMetricName = properties.getPrefix() + "." + metricKey;

        // Создаем таймер
        Timer.Builder timerBuilder = Timer.builder(fullMetricName + ".duration")
                .description(getMethodDescription(annotation, method))
                .publishPercentiles(properties.isHistogram() ? properties.getPercentiles() : null)
                .publishPercentileHistogram(properties.isHistogram());

        // Создаем счетчик
        Counter.Builder counterBuilder = Counter.builder(fullMetricName + ".calls")
                .description("Number of calls for method: " +
                        (method != null ? method.getName() : metricKey));

        // Добавляем теги из аннотации
        Map<String, String> tags = parseTags(annotation.extraTags());
        tags.forEach(timerBuilder::tag);
        tags.forEach(counterBuilder::tag);

        // Добавляем стандартные теги
        String methodName = extractMethodName(metricKey);
        String className = extractClassName(metricKey);

        timerBuilder.tag("method", methodName);
        timerBuilder.tag("class", className);

        counterBuilder.tag("method", methodName);
        counterBuilder.tag("class", className);

        if (method != null) {
            String signature = getMethodSignature(method);
            timerBuilder.tag("signature", signature);
            counterBuilder.tag("signature", signature);
        }

        // Регистрируем метрики
        Timer timer = timerBuilder.register(meterRegistry);
        Counter counter = counterBuilder.register(meterRegistry);

        // Сохраняем в кэш
        Timer existingTimer = timerCache.putIfAbsent(metricKey, timer);
        Counter existingCounter = counterCache.putIfAbsent(metricKey, counter);

        // Сохраняем аннотацию для будущего использования
        annotationCache.put(metricKey, annotation);

        // Удаляем дубликаты если были
        if (existingTimer != null) {
            meterRegistry.remove(timer);
        }
        if (existingCounter != null) {
            meterRegistry.remove(counter);
        }

        log.debug("Created timer and counter for method: {}", metricKey);
    }

    public void recordExecution(String metricKey, long durationNanos) {
        if (metricKey == null || metricKey.trim().isEmpty()) {
            return;
        }

        Timer timer = timerCache.get(metricKey);
        Counter counter = counterCache.get(metricKey);

        if (timer != null && counter != null) {
            // Увеличиваем счетчик вызовов
            counter.increment();
            // Записываем время выполнения
            timer.record(durationNanos, TimeUnit.NANOSECONDS);
        } else {
            log.warn("Timer or counter not found for method: {}. Metric will not be recorded.", metricKey);
        }
    }

    /**
     * Возвращает количество вызовов метода.
     */
    public double getCallCount(String metricKey) {
        Counter counter = counterCache.get(metricKey);
        return counter != null ? counter.count() : 0;
    }

    /**
     * Возвращает общее время выполнения метода в наносекундах.
     */
    public double getTotalTime(String metricKey) {
        Timer timer = timerCache.get(metricKey);
        return timer != null ? timer.totalTime(TimeUnit.NANOSECONDS) : 0;
    }

    /**
     * Возвращает среднее время выполнения метода в наносекундах.
     */
    public double getAverageTime(String metricKey) {
        Timer timer = timerCache.get(metricKey);
        Counter counter = counterCache.get(metricKey);

        if (timer != null && counter != null && counter.count() > 0) {
            return timer.totalTime(TimeUnit.NANOSECONDS) / counter.count();
        }
        return 0;
    }

    /**
     * Возвращает все метрики для метода в виде Map.
     */
    public Map<String, Object> getMethodMetrics(String metricKey) {
        Map<String, Object> metrics = new HashMap<>();

        Timer timer = timerCache.get(metricKey);
        Counter counter = counterCache.get(metricKey);

        if (timer != null && counter != null) {
            metrics.put("calls", counter.count());
            metrics.put("totalTime", timer.totalTime(TimeUnit.NANOSECONDS));
            metrics.put("averageTime", getAverageTime(metricKey));
            metrics.put("maxTime", timer.max(TimeUnit.NANOSECONDS));
            metrics.put("method", metricKey);
        }

        return metrics;
    }

    private String getMethodDescription(TimedMethod annotation, Method method) {
        if (annotation != null && !annotation.description().isEmpty()) {
            return annotation.description();
        }

        if (method != null) {
            return String.format("Execution time of method %s in class %s",
                    method.getName(),
                    method.getDeclaringClass().getSimpleName());
        }

        return "Method execution time";
    }

    private String extractMethodName(String metricKey) {
        int lastDotIndex = metricKey.lastIndexOf('.');
        if (lastDotIndex != -1) {
            return metricKey.substring(lastDotIndex + 1);
        }
        return metricKey;
    }

    private String extractClassName(String metricKey) {
        int lastDotIndex = metricKey.lastIndexOf('.');
        if (lastDotIndex != -1) {
            return metricKey.substring(0, lastDotIndex);
        }
        return "unknown";
    }

    private String getMethodSignature(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getName()).append("(");

        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            sb.append(params[i].getSimpleName());
            if (i < params.length - 1) {
                sb.append(",");
            }
        }
        sb.append(")");

        return sb.toString();
    }

    private Map<String, String> parseTags(String[] tagStrings) {
        Map<String, String> tags = new HashMap<>();
        if (tagStrings != null) {
            for (String tagString : tagStrings) {
                if (tagString != null) {
                    String[] parts = tagString.split("=", 2);
                    if (parts.length == 2) {
                        tags.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        }
        return tags;
    }

    public Timer getTimer(String metricKey) {
        return timerCache.get(metricKey);
    }

    public Counter getCounter(String metricKey) {
        return counterCache.get(metricKey);
    }

    public Map<String, Timer> getAllTimers() {
        return new HashMap<>(timerCache);
    }

    public Map<String, Counter> getAllCounters() {
        return new HashMap<>(counterCache);
    }

    public void clear() {
        timerCache.clear();
        counterCache.clear();
        annotationCache.clear();
    }

    public int getTimerCount() {
        return timerCache.size();
    }

    public int getCounterCount() {
        return counterCache.size();
    }
}