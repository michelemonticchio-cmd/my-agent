package com.monticchio.myagent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

public class PlantDiseaseDtos {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlantDisease(
            String name,
            String plant,
            List<String> symptoms,
            String treatment
    ) {}
}
