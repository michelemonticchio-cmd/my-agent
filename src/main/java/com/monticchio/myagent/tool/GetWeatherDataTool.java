package com.monticchio.myagent.tool;

import com.monticchio.myagent.dto.OpenMeteoDtos.DailyForecast;
import com.monticchio.myagent.dto.OpenMeteoDtos.OpenMeteoResponse;
import com.monticchio.myagent.service.WeatherService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GetWeatherDataTool implements Tool {

    private static final int DEFAULT_DAYS = 7;

    private final WeatherService weatherService;

    public GetWeatherDataTool(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @Override
    public String name() {
        return "get_weather_forecast";
    }

    @Override
    public String description() {
        return "Returns the weather forecast (max/min temperatures and precipitation) " +
                "for a location in Salento over the next few days. This tool does not perform " +
                "geocoding: you must estimate the latitude and longitude of the requested location " +
                "yourself, using your knowledge of the geographic coordinates of Salento towns " +
                "(e.g. Lecce, Gallipoli, Otranto, Nardo', Copertino).";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "latitude", Map.of(
                                "type", "number",
                                "description", "Estimated latitude of the requested location"
                        ),
                        "longitude", Map.of(
                                "type", "number",
                                "description", "Estimated longitude of the requested location"
                        ),
                        "days", Map.of(
                                "type", "integer",
                                "description", "Number of forecast days to return, default 7"
                        )
                ),
                "required", List.of("latitude", "longitude")
        );
    }

    @Override
    public String execute(Map<String, Object> input) {
        double latitude = ((Number) input.get("latitude")).doubleValue();
        double longitude = ((Number) input.get("longitude")).doubleValue();
        int days = input.get("days") instanceof Number n ? n.intValue() : DEFAULT_DAYS;

        OpenMeteoResponse forecast = weatherService.getForecast(latitude, longitude, days);
        return formatForecast(forecast.daily());
    }

    private String formatForecast(DailyForecast daily) {
        StringBuilder sb = new StringBuilder();
        List<String> dates = daily.time();
        List<Double> maxTemps = daily.temperature_2m_max();
        List<Double> minTemps = daily.temperature_2m_min();
        List<Double> precipitation = daily.precipitation_sum();

        for (int i = 0; i < dates.size(); i++) {
            sb.append(dates.get(i))
                    .append(": min ").append(minTemps.get(i)).append("°C")
                    .append(", max ").append(maxTemps.get(i)).append("°C")
                    .append(", precipitation ").append(precipitation.get(i)).append("mm")
                    .append(System.lineSeparator());
        }
        return sb.toString().stripTrailing();
    }
}
