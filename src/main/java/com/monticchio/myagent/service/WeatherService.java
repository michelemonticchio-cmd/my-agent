package com.monticchio.myagent.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.monticchio.myagent.dto.OpenMeteoDtos;
import com.monticchio.myagent.exception.LlmException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class WeatherService {

    private final RestClient restClient;

    public WeatherService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.open-meteo.com")
                .build();
    }

    public record CurrentWeather(double temperature, double windspeed) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ForecastResponse(CurrentWeatherBlock current_weather) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CurrentWeatherBlock(double temperature, double windspeed) {}

    // Not called by any tool yet — kept for a possible future "current conditions"
    // tool, distinct from the multi-day forecast used by GetWeatherDataTool.
    public CurrentWeather getCurrentWeather(double latitude, double longitude) {
        ForecastResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/forecast")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("current_weather", true)
                            .build())
                    .retrieve()
                    .body(ForecastResponse.class);
        } catch (RestClientException e) {
            throw new LlmException("Error calling weather API", e);
        }

        if (response == null || response.current_weather() == null) {
            throw new LlmException("Empty response from weather API");
        }

        return new CurrentWeather(
                response.current_weather().temperature(),
                response.current_weather().windspeed()
        );
    }

    public OpenMeteoDtos.OpenMeteoResponse getForecast(double latitude, double longitude, int days) {
        OpenMeteoDtos.OpenMeteoResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/forecast")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("daily", "temperature_2m_max,temperature_2m_min,precipitation_sum")
                            .queryParam("forecast_days", days)
                            .queryParam("timezone", "Europe/Rome")
                            .build())
                    .retrieve()
                    .body(OpenMeteoDtos.OpenMeteoResponse.class);
        } catch (RestClientException e) {
            throw new LlmException("Error calling weather API", e);
        }

        if (response == null || response.daily() == null) {
            throw new LlmException("Empty response from weather API");
        }

        return response;
    }
}