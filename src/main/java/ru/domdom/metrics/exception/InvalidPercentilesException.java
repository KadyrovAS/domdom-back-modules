package ru.domdom.metrics.exception;

/**
 * Исключение, выбрасываемое при попытке установить пустой или null массив percentiles в {@link ru.domdom.metrics.config.MethodMetricsProperties}.
 */
public class InvalidPercentilesException extends MetricsException {
    public InvalidPercentilesException(String message) {
        super(message);
    }
}