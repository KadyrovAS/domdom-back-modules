package ru.domdom.metrics.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Конфигурационные свойства для сбора метрик методов.
 *
 * <p>Настройки позволяют контролировать поведение сбора метрик:
 * <ul>
 *   <li>{@code enabled} – включение/отключение сбора метрик</li>
 *   <li>{@code prefix} – префикс для всех метрик методов</li>
 *   <li>{@code histogram} – включение гистограмм для распределения времени</li>
 *   <li>{@code percentiles} – процентили для гистограмм</li>
 * </ul>
 *
 * <p>Пример конфигурации в application.yml:
 * <pre>
 * method:
 *   metrics:
 *     enabled: true
 *     prefix: "method"
 *     histogram: true
 *     percentiles: [0.5, 0.95, 0.99]
 * </pre>
 *
 * @author Кадыров Андрей
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "method.metrics", ignoreInvalidFields = true)
public class MethodMetricsProperties {

    /**
     * Включение или отключение сбора метрик методов.
     * По умолчанию {@code true}.
     */
    private boolean enabled = true;

    /**
     * Префикс для всех метрик методов. Добавляется перед именем метрики.
     * По умолчанию {@code "method"}.
     */
    private String prefix = "method";

    /**
     * Включение гистограмм для времени выполнения методов.
     * По умолчанию {@code true}.
     */
    private boolean histogram = true;

    /**
     * Процентили для гистограмм. Определяет, для каких процентилей
     * рассчитывать значения (например, p50, p95, p99).
     * По умолчанию {@code [0.5, 0.95, 0.99]}.
     */
    private double[] percentiles = {0.5, 0.95, 0.99};

    /**
     * Устанавливает массив процентилей.
     *
     * @param percentiles новый массив процентилей (не должен быть пустым)
     */
    public void setPercentiles(double[] percentiles) {
        if (percentiles != null && percentiles.length > 0) {
            this.percentiles = percentiles;
        }
    }
}