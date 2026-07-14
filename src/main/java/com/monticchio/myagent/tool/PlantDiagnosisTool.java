package com.monticchio.myagent.tool;

import com.monticchio.myagent.dto.PlantDiseaseDtos.PlantDisease;
import com.monticchio.myagent.service.PlantDiagnosisService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PlantDiagnosisTool implements Tool {

    private final PlantDiagnosisService plantDiagnosisService;

    public PlantDiagnosisTool(PlantDiagnosisService plantDiagnosisService) {
        this.plantDiagnosisService = plantDiagnosisService;
    }

    @Override
    public String name() {
        return "diagnose_plant_disease";
    }

    @Override
    public String description() {
        return "Looks up known diseases and pests affecting the crops most common in Salento " +
                "(olive, grapevine, citrus, fig, almond) against a curated local knowledge base, given " +
                "the affected plant and a list of observed symptoms. " +
                "Identify the plant type from the user's message and translate it into one of: olive, " +
                "grapevine, citrus, fig, almond. Extract the symptoms described by the user and translate " +
                "them into English keywords chosen from this controlled vocabulary: wilting branches, " +
                "scorched leaves, tree drying in patches, black spots on olives, oviposition holes in " +
                "olives, fallen olives, olive rot, sooty mold on leaves, sticky honeydew on leaves, small " +
                "brown bumps on twigs and leaves, yellow-brown spots on upper leaf surface, premature leaf " +
                "drop, sudden wilting of shoots, one-sided branch dieback, white powdery coating on leaves, " +
                "yellow oily spots on upper leaf surface, white downy mold on leaf underside, stem pitting " +
                "on citrus, yellowing and stunted citrus trees, quick decline of grafted citrus, white " +
                "cottony masses on citrus branches, silvery serpentine trails on citrus leaves, curled " +
                "young citrus leaves, orange-brown pustules on underside of fig leaves, mottled yellow-green " +
                "mosaic pattern on fig leaves, distorted fig leaves, reduced fruit quality, marginal leaf " +
                "scorch on almond leaves, premature defoliation of almond trees, branch dieback on almond, " +
                "brown rot on almond fruit, blighted almond blossoms, gummy exudate on almond twigs. " +
                "Only use keywords from this list.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "plant", Map.of(
                                "type", "string",
                                "description", "The affected plant type, one of: olive, grapevine, citrus, fig, almond"
                        ),
                        "symptoms", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string"),
                                "description", "Symptoms observed by the user, expressed as English keywords " +
                                        "from the controlled vocabulary listed in the tool description"
                        )
                ),
                "required", List.of("plant", "symptoms")
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public String execute(Map<String, Object> input) {
        String plant = (String) input.get("plant");
        List<String> symptoms = (List<String>) input.getOrDefault("symptoms", List.of());
        List<PlantDisease> matches = plantDiagnosisService.findMatches(plant, symptoms);

        if (matches.isEmpty()) {
            return "No matching disease or pest found in the local knowledge base for the given symptoms. " +
                    "Consider consulting a local agronomist or the Regional Plant Health Observatory.";
        }

        StringBuilder sb = new StringBuilder();
        for (PlantDisease disease : matches) {
            sb.append(disease.name()).append(": ").append(disease.treatment()).append(System.lineSeparator());
        }
        return sb.toString().stripTrailing();
    }
}
