package com.tlc.mcp;

import com.tlc.commons.service.Service;
import com.tlc.mcp.executor.McpMonitor;
import com.tlc.mcp.prompt.McpPromptInfo;
import com.tlc.mcp.resource.McpResourceInfo;
import com.tlc.mcp.tool.McpToolInfo;
import com.tlc.mcp.tool.McpToolResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface McpService extends Service {

    McpToolResult invokeTool(String toolName, Map<String, Object> arguments, Object identity);

    default McpToolResult invokeTool(String toolName, Map<String, Object> arguments) {
        return invokeTool(toolName, arguments, null);
    }

    McpToolResult readResource(String uri, Object identity);

    default McpToolResult readResource(String uri) {
        return readResource(uri, null);
    }

    McpToolResult invokePrompt(String name, Map<String, String> arguments, Object identity);

    default McpToolResult invokePrompt(String name, Map<String, String> arguments) {
        return invokePrompt(name, arguments, null);
    }

    List<McpToolInfo> tools();

    Optional<McpToolInfo> tool(String name);

    List<McpResourceInfo> resources();

    Optional<McpResourceInfo> resource(String uri);

    List<McpPromptInfo> prompts();

    Optional<McpPromptInfo> prompt(String name);

    McpMonitor getMonitor();
}
