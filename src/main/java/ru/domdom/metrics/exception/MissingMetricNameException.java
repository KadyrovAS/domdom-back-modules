package ru.domdom.metrics.exception;

/**
 * Исключение, выбрасываемое если имя метрики (value аннотации {@link ru.domdom.metrics.annotation.TimedMethod}) пустое.
 */
public class MissingMetricNameException extends MetricsException {
    public MissingMetricNameException(String message) {
        super(message);
    }
}