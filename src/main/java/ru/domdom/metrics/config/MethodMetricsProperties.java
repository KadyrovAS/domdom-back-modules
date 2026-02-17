package ru.domdom.metrics.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.domdom.metrics.exception.InvalidPercentilesException;

@Getter
@ConfigurationProperties(prefix = "method.metrics")
public class MethodMetricsProperties {

    @Setter
    private boolean enabled = true;
    @Setter
    private String prefix = "method";
    @Setter
    private boolean histogram = true;
    private double[] percentiles;

    public void setPercentiles(double[] percentiles) {
        if (percentiles == null || percentiles.length == 0) {
            throw new InvalidPercentilesException("percentiles must not be empty");
        }
        this.percentiles = percentiles;
    }
}