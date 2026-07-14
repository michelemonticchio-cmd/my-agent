package com.monticchio.myagent.tool;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class CurrentTimeTool implements Tool {

    @Override
    public String name() {
        return "che_ore_sono";
    }

    @Override
    public String description() {
        return "Returns the current date and time. Use it when the user asks for the time or the date, " +
                "and also proactively when giving agronomic advice, to ground your reasoning about the " +
                "current season and the growth phase (e.g. dormancy, flowering, fruit set, ripening, " +
                "harvest, or the planting/grafting window) a crop should currently be in for Salento.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(),
                "required", List.of()
        );
    }

    @Override
    public String execute(Map<String, Object> input) {
        return LocalDateTime.now().toString();
    }
}
