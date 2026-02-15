package ru.domdom.metrics.exception;

/**
 * Исключение, выбрасываемое при нечётном количестве элементов в extraTags аннотации {@link ru.domdom.metrics.annotation.TimedMethod}.
 */
public class InvalidExtraTagsException extends MetricsException {
    public InvalidExtraTagsException(String message) {
        super(message);
    }
}