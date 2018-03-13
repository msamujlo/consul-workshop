package weather.sensors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import weather.WeatherProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

public class SensorsHealthIndicator implements HealthIndicator {

    private static final Log logger = LogFactory.getLog(SensorsHealthIndicator.class);

    private final WeatherProperties weatherProperties;
    private final SensorsProperties sensorsProperties;

    public SensorsHealthIndicator(WeatherProperties weatherProperties, SensorsProperties sensorsProperties) {
        this.weatherProperties = weatherProperties;
        this.sensorsProperties = sensorsProperties;
    }

    @Override
    public Health health() {
        if (weatherProperties.getTemperature()>sensorsProperties.getMaxTemperature()) {
            logger.warn("too hot!");
            return Health.down().withDetail("reason", "Too hot!").build();
        }
        if (weatherProperties.getTemperature()<sensorsProperties.getMinTemperature()) {
            logger.warn("freezing cold!");
            return Health.down().withDetail("reason","Freezing cold!").build();
        }
        return Health.status(Status.UP).build();
    }
}
