package ru.domdom.metrics.exception;

import org.junit.jupiter.api.Test;
import ru.domdom.metrics.config.MethodMetricsProperties;
import ru.domdom.metrics.service.TagUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetricsExceptionTest {

    @Test
    void shouldThrowInvalidExtraTagsExceptionForOddArray() {
        String[] tags = {"key1", "value1", "key2"};
        assertThatThrownBy(() -> TagUtils.fromExtraTags(tags))
                .isInstanceOf(InvalidExtraTagsException.class)
                .hasMessageContaining("even number");
    }

    @Test
    void shouldThrowInvalidPercentilesExceptionForEmptyArray() {
        MethodMetricsProperties properties = new MethodMetricsProperties();
        assertThatThrownBy(() -> properties.setPercentiles(new double[]{}))
                .isInstanceOf(InvalidPercentilesException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    void shouldThrowInvalidPercentilesExceptionForNullArray() {
        MethodMetricsProperties properties = new MethodMetricsProperties();
        assertThatThrownBy(() -> properties.setPercentiles(null))
                .isInstanceOf(InvalidPercentilesException.class)
                .hasMessageContaining("must not be empty");
    }
}