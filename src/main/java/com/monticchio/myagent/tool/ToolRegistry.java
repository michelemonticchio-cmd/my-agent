package com.monticchio.myagent.tool;

import com.monticchio.myagent.dto.AnthropicDtos.ToolDefinition;
import com.monticchio.myagent.exception.LlmException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ToolRegistry {

    private final Map<String, Tool> toolsByName;

    public ToolRegistry(List<Tool> tools) {
        this.toolsByName = tools.stream()
                .collect(Collectors.toUnmodifiableMap(Tool::name, t -> t));
    }

    public List<ToolDefinition> toolDefinitions() {
        return toolsByName.values().stream()
                .map(t -> new ToolDefinition(t.name(), t.description(), t.inputSchema()))
                .toList();
    }

    public String execute(String name, Object rawInput) {
        Tool tool = toolsByName.get(name);
        if (tool == null) {
            throw new LlmException("Il modello ha richiesto un tool sconosciuto: " + name);
        }
        Map<String, Object> input = rawInput instanceof Map<?, ?> m ? castToStringObjectMap(m) : Map.of();
        try {
            return tool.execute(input);
        } catch (RuntimeException e) {
            throw new LlmException("Errore nell'esecuzione del tool '" + name + "'", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castToStringObjectMap(Map<?, ?> m) {
        return (Map<String, Object>) m;
    }
}
