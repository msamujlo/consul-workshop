package weather;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.stereotype.Service;

@Service
public class WeatherForecastService {

    private final WeatherStationClient weatherStationClient;

    public WeatherForecastService(WeatherStationClient weatherStationClient) {
        this.weatherStationClient = weatherStationClient;
    }

    @HystrixCommand(fallbackMethod = "defaultForecast")
    public WeatherForecast getForecast() {
        final Weather weather = weatherStationClient.getWeather();
        return new WeatherForecast(weather.getTemperature(),
                weather.getTemperature() * weather.getHumidity(),
                "forecast based on " + weather.getStationId()
        );
    }

    public WeatherForecast defaultForecast() {
        return new WeatherForecast(22, 22, "cloudy with a chance of meatballs");
    }

}
