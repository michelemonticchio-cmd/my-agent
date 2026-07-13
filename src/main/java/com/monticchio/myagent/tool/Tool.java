package com.monticchio.myagent.tool;

import java.util.Map;

public interface Tool {
    String name();
    String description();
    Map<String, Object> inputSchema();
    String execute(Map<String, Object> input);
}
