package ru.domdom.metrics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.domdom.metrics.exception.InvalidPercentilesException;

@ConfigurationProperties(prefix = "method.metrics")
public class MethodMetricsProperties {

    private boolean enabled = true;
    private String prefix = "method";
    private boolean histogram = true;
    private double[] percentiles;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public boolean isHistogram() {
        return histogram;
    }

    public void setHistogram(boolean histogram) {
        this.histogram = histogram;
    }

    public double[] getPercentiles() {
        return percentiles;
    }

    public void setPercentiles(double[] percentiles) {
        if (percentiles == null || percentiles.length == 0) {
            throw new InvalidPercentilesException("percentiles must not be empty");
        }
        this.percentiles = percentiles;
    }
}