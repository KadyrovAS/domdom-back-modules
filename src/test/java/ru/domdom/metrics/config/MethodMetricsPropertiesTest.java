package ru.domdom.metrics.config;

import org.junit.jupiter.api.Test;
import ru.domdom.metrics.exception.InvalidPercentilesException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MethodMetricsPropertiesTest {

    @Test
    void shouldThrowExceptionWhenPercentilesIsEmpty() {
        MethodMetricsProperties properties = new MethodMetricsProperties();
        assertThatThrownBy(() -> properties.setPercentiles(new double[]{}))
                .isInstanceOf(InvalidPercentilesException.class)
                .hasMessage("percentiles must not be empty");
    }

    @Test
    void shouldThrowExceptionWhenPercentilesIsNull() {
        MethodMetricsProperties properties = new MethodMetricsProperties();
        assertThatThrownBy(() -> properties.setPercentiles(null))
                .isInstanceOf(InvalidPercentilesException.class)
                .hasMessage("percentiles must not be empty");
    }

    @Test
    void shouldAllowNonEmptyPercentiles() {
        MethodMetricsProperties properties = new MethodMetricsProperties();
        properties.setPercentiles(new double[]{0.5, 0.95}); // no exception
    }
}