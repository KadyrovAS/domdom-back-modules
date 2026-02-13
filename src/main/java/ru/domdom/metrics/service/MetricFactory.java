package ru.domdom.metrics.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.domdom.metrics.annotation.TimedMethod;
import ru.domdom.metrics.config.MethodMetricsProperties;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Фабрика для создания и кэширования метрик Micrometer (таймеров и счётчиков).
 *
 * <p>Отвечает за создание {@link Timer} и {@link Counter} для каждого уникального ключа метрики.
 * Применяет глобальные настройки из {@link MethodMetricsProperties} и теги из аннотации {@link TimedMethod}.
 * Кэширует созданные метрики для повторного использования.
 *
 * @author Кадыров Андрей
 * @since 1.0.0
 * @see TimedMethod
 * @see MethodMetricsProperties
 * @see TagParser
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricFactory {

    private final MeterRegistry meterRegistry;
    private final MethodMetricsProperties properties;
    private final ConcurrentHashMap<String, Timer> timerCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> counterCache = new ConcurrentHashMap<>();

    /**
     * Возвращает таймер для заданного ключа метрики. Если таймер ещё не создан,
     * он будет создан с использованием переданной аннотации и метода.
     *
     * @param metricKey  ключ метрики (имя)
     * @param annotation аннотация {@link TimedMethod}
     * @param method     метод, для которого создаётся метрика (может быть {@code null})
     * @return таймер Micrometer
     */
    public Timer getTimer(String metricKey, TimedMethod annotation, Method method) {
        log.debug("Getting timer for key: {}", metricKey);
        return timerCache.computeIfAbsent(metricKey, key -> createTimer(key, annotation, method));
    }

    /**
     * Возвращает счётчик для заданного ключа метрики. Если счётчик ещё не создан,
     * он будет создан с использованием переданной аннотации и метода.
     *
     * @param metricKey  ключ метрики (имя)
     * @param annotation аннотация {@link TimedMethod}
     * @param method     метод, для которого создаётся метрика (может быть {@code null})
     * @return счётчик Micrometer
     */
    public Counter getCounter(String metricKey, TimedMethod annotation, Method method) {
        log.debug("Getting counter for key: {}", metricKey);
        return counterCache.computeIfAbsent(metricKey, key -> createCounter(key, annotation, method));
    }

    /**
     * Создаёт новый таймер и регистрирует его в {@link MeterRegistry}.
     *
     * @param metricKey  ключ метрики
     * @param annotation аннотация
     * @param method     метод
     * @return созданный таймер
     */
    private Timer createTimer(String metricKey, TimedMethod annotation, Method method) {
        String fullName = properties.getPrefix() + "." + metricKey + ".duration";
        log.info("Creating timer with name: {}", fullName);
        Timer.Builder builder = Timer.builder(fullName)
                .description(buildDescription(annotation, method))
                .publishPercentiles(properties.isHistogram() ? properties.getPercentiles() : null)
                .publishPercentileHistogram(properties.isHistogram());

        applyCommonTags(builder, metricKey, annotation, method);
        return builder.register(meterRegistry);
    }

    /**
     * Создаёт новый счётчик и регистрирует его в {@link MeterRegistry}.
     *
     * @param metricKey  ключ метрики
     * @param annotation аннотация
     * @param method     метод
     * @return созданный счётчик
     */
    private Counter createCounter(String metricKey, TimedMethod annotation, Method method) {
        String fullName = properties.getPrefix() + "." + metricKey + ".calls";
        log.info("Creating counter with name: {}", fullName);
        Counter.Builder builder = Counter.builder(fullName)
                .description("Number of calls for method: " + (method != null ? method.getName() : metricKey));

        applyCommonTags(builder, metricKey, annotation, method);
        return builder.register(meterRegistry);
    }

    /**
     * Применяет общие теги к строителю метрики: теги из аннотации, имя метода, класс и сигнатуру.
     *
     * @param builder    строитель метрики (либо {@link Timer.Builder}, либо {@link Counter.Builder})
     * @param metricKey  ключ метрики
     * @param annotation аннотация
     * @param method     метод
     */
    private void applyCommonTags(Object builder, String metricKey, TimedMethod annotation, Method method) {
        Map<String, String> tags = TagParser.parse(annotation.extraTags());
        String methodName = extractMethodName(metricKey);
        String className = extractClassName(metricKey);

        if (builder instanceof Timer.Builder timerBuilder) {
            tags.forEach(timerBuilder::tag);
            timerBuilder.tag("method", methodName).tag("class", className);
            if (method != null) {
                timerBuilder.tag("signature", getMethodSignature(method));
            }
        } else if (builder instanceof Counter.Builder counterBuilder) {
            tags.forEach(counterBuilder::tag);
            counterBuilder.tag("method", methodName).tag("class", className);
            if (method != null) {
                counterBuilder.tag("signature", getMethodSignature(method));
            }
        }
    }

    /**
     * Формирует описание для таймера на основе аннотации или метода.
     *
     * @param annotation аннотация
     * @param method     метод
     * @return описание
     */
    private String buildDescription(TimedMethod annotation, Method method) {
        if (annotation != null && annotation.description() != null && !annotation.description().isEmpty()) {
            return annotation.description();
        }
        if (method != null) {
            return String.format("Execution time of method %s in class %s",
                    method.getName(), method.getDeclaringClass().getSimpleName());
        }
        return "Method execution time";
    }

    /**
     * Извлекает имя метода из ключа метрики (часть после последней точки).
     *
     * @param metricKey ключ метрики
     * @return имя метода
     */
    private String extractMethodName(String metricKey) {
        int lastDot = metricKey.lastIndexOf('.');
        return lastDot == -1 ? metricKey : metricKey.substring(lastDot + 1);
    }

    /**
     * Извлекает имя класса из ключа метрики (часть до последней точки).
     *
     * @param metricKey ключ метрики
     * @return имя класса или "unknown"
     */
    private String extractClassName(String metricKey) {
        int lastDot = metricKey.lastIndexOf('.');
        return lastDot == -1 ? "unknown" : metricKey.substring(0, lastDot);
    }

    /**
     * Формирует сигнатуру метода в виде {@code имя(тип1,тип2,...)}.
     *
     * @param method метод
     * @return сигнатура
     */
    private String getMethodSignature(Method method) {
        StringBuilder sb = new StringBuilder(method.getName()).append('(');
        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            sb.append(params[i].getSimpleName());
            if (i < params.length - 1) sb.append(',');
        }
        sb.append(')');
        return sb.toString();
    }

    /**
     * Очищает кэш таймеров и счётчиков, удаляя их из реестра метрик.
     * Используется в тестах или при перезагрузке конфигурации.
     */
    public void clearCache() {
        timerCache.forEach((key, timer) -> meterRegistry.remove(timer));
        counterCache.forEach((key, counter) -> meterRegistry.remove(counter));
        timerCache.clear();
        counterCache.clear();
        log.info("MetricFactory cache cleared and meters removed from registry");
    }
}