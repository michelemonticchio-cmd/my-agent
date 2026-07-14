package com.monticchio.myagent.service;

import com.monticchio.myagent.dto.PlantDiseaseDtos.PlantDisease;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

@Service
public class PlantDiagnosisService {

    private final List<PlantDisease> diseases;

    public PlantDiagnosisService(ObjectMapper objectMapper) {
        try (InputStream in = getClass().getResourceAsStream("/plant-diseases.json")) {
            this.diseases = objectMapper.readValue(in, new TypeReference<List<PlantDisease>>() {});
        } catch (IOException | JacksonException e) {
            throw new IllegalStateException("Failed to load plant-diseases.json", e);
        }
    }

    public List<PlantDisease> findMatches(String plant, List<String> symptoms) {
        return diseases.stream()
                .filter(disease -> plant == null || plant.isBlank() || disease.plant().equalsIgnoreCase(plant))
                .filter(disease -> countMatches(disease, symptoms) > 0)
                .sorted(Comparator.comparingInt((PlantDisease disease) -> countMatches(disease, symptoms)).reversed())
                .toList();
    }

    private int countMatches(PlantDisease disease, List<String> symptoms) {
        return (int) symptoms.stream()
                .filter(symptom -> disease.symptoms().stream().anyMatch(known -> known.equalsIgnoreCase(symptom)))
                .count();
    }
}
