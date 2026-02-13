package ru.domdom.metrics.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;

/**
 * Конфигурационные свойства для сбора метрик методов.
 *
 * <p>Настройки позволяют контролировать поведение сбора метрик:</p>
 * <ul>
 *   <li>Включение/отключение сбора метрик</li>
 *   <li>Настройка префикса для имен метрик</li>
 *   <li>Включение/отключение гистограмм</li>
 *   <li>Настройка процентилей для гистограмм</li>
 * </ul>
 *
 * <p>Пример конфигурации в application.yml:</p>
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

    private boolean enabled = true;
    private String prefix = "method";
    private boolean histogram = true;
    private double[] percentiles = {0.5, 0.95, 0.99};

    /**
     * Устанавливает массив процентилей для гистограмм.
     *
     * @param percentiles новый массив процентилей
     * @throws IllegalArgumentException если массив пустой или содержит недопустимые значения
     */
    public void setPercentiles(double[] percentiles) {
        if (percentiles != null && percentiles.length > 0) {
            this.percentiles = percentiles;
        }
    }

    /**
     * Устанавливает процентили из строкового представления.
     *
     * <p>Формат строки: значения, разделенные запятыми, например: "0.5,0.95,0.99"</p>
     *
     * @param percentiles строка с процентилями, разделенными запятыми
     */
    public void setPercentiles(String percentiles) {
        if (percentiles != null && !percentiles.trim().isEmpty()) {
            try {
                String[] parts = percentiles.split(",");
                this.percentiles = Arrays.stream(parts)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .mapToDouble(Double::parseDouble)
                        .toArray();
            } catch (NumberFormatException e) {
                // При ошибке парсинга используем значения по умолчанию
                this.percentiles = new double[]{0.5, 0.95, 0.99};
            }
        }
    }

    /**
     * Возвращает строковое представление объекта.
     *
     * @return строковое представление конфигурационных свойств
     */
    @Override
    public String toString() {
        return "MethodMetricsProperties{" +
                "enabled=" + enabled +
                ", prefix='" + prefix + '\'' +
                ", histogram=" + histogram +
                ", percentiles=" + Arrays.toString(percentiles) +
                '}';
    }
}