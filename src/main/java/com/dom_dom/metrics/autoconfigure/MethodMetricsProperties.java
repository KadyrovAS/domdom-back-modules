package com.dom_dom.metrics.autoconfigure;

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
@ConfigurationProperties(prefix = "method.metrics", ignoreInvalidFields = true)
public class MethodMetricsProperties {

    /**
     * Включение/отключение сбора метрик методов.
     */
    private boolean enabled = true;

    /**
     * Префикс для всех метрик методов.
     */
    private String prefix = "method";

    /**
     * Включение/отключение сбора гистограмм.
     */
    private boolean histogram = true;

    /**
     * Процентили для гистограмм.
     */
    private double[] percentiles = {0.5, 0.95, 0.99};
    /**
     * Возвращает флаг включения сбора метрик.
     *
     * @return true если сбор метрик включен, false в противном случае
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Устанавливает флаг включения сбора метрик.
     *
     * @param enabled true для включения сбора метрик, false для отключения
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Возвращает префикс для метрик.
     *
     * @return префикс метрик
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Устанавливает префикс для метрик.
     *
     * @param prefix новый префикс для метрик
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Возвращает флаг включения гистограмм.
     *
     * @return true если гистограммы включены, false в противном случае
     */
    public boolean isHistogram() {
        return histogram;
    }

    /**
     * Устанавливает флаг включения гистограмм.
     *
     * @param histogram true для включения гистограмм, false для отключения
     */
    public void setHistogram(boolean histogram) {
        this.histogram = histogram;
    }

    /**
     * Возвращает массив процентилей для гистограмм.
     *
     * @return массив процентилей
     */
    public double[] getPercentiles() {
        return percentiles;
    }

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