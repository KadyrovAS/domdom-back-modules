package ru.domdom.metrics.exception;

/**
 * Исключение, выбрасываемое при нечётном количестве элементов в extraTags аннотации {@link ru.domdom.metrics.annotation.TimedMethod}.
 */
public class InvalidExtraTagsException extends MetricsException {
    public InvalidExtraTagsException(int count) {
        super("extraTags must have even number of elements (key-value pairs), but got: " + count);
    }
}