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
        return "Restituisce data e ora correnti. Usalo quando l'utente chiede l'ora o la data.";
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
