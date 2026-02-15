package ru.domdom.metrics.exception;

/**
 * Базовое исключение для всех ошибок, связанных со стартером метрик.
 */
public class MetricsException extends RuntimeException {
    public MetricsException(String message) {
        super(message);
    }

    public MetricsException(String message, Throwable cause) {
        super(message, cause);
    }
}