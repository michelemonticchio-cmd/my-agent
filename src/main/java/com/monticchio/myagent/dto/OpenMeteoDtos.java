package com.monticchio.myagent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

public class OpenMeteoDtos {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenMeteoResponse(DailyForecast daily) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DailyForecast(
            List<String> time,
            List<Double> temperature_2m_max,
            List<Double> temperature_2m_min,
            List<Double> precipitation_sum
    ) {}
}
