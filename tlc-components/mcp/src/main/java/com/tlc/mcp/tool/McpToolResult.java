package com.tlc.mcp.tool;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public final class McpToolResult {

    private final List<ContentItem> content;
    private final boolean isError;

    private McpToolResult(List<ContentItem> content, boolean isError) {
        this.content = List.copyOf(content);
        this.isError = isError;
    }

    public static McpToolResult text(String text) {
        return new McpToolResult(List.of(new ContentItem("text", text, null)), false);
    }

    public static McpToolResult error(String message) {
        return new McpToolResult(List.of(new ContentItem("text", message, null)), true);
    }

    public static McpToolResult image(byte[] data, String mimeType) {
        return new McpToolResult(
                List.of(new ContentItem("image", java.util.Base64.getEncoder().encodeToString(data), mimeType)),
                false);
    }

    public static McpToolResult of(List<ContentItem> content) {
        return new McpToolResult(content, false);
    }

    public static McpToolResult json(JsonObject data) {
        return text(data.toString());
    }

    public static McpToolResult json(JsonArray data) {
        return text(data.toString());
    }

    public static McpToolResult trackingId(String taskId) {
        return text(Json.orderedObject()
                .put("taskId", taskId)
                .put("status", "PENDING")
                .put("hint", "Call get_task_status with this taskId to retrieve the result.")
                .toString());
    }

    public McpToolResult and(McpToolResult other) {
        final List<ContentItem> combined = new ArrayList<>(content);
        combined.addAll(other.content);
        return new McpToolResult(combined, isError || other.isError);
    }

    public List<ContentItem> content() {
        return content;
    }

    public boolean isError() {
        return isError;
    }

    public record ContentItem(String type, String data, String mimeType) {}
}
