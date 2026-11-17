package com.tlc.mcp.action.internal.registry;

import com.tlc.mcp.internal.registry.McpResourceRegistry;
import com.tlc.mcp.internal.registry.ResourceRegistration;
import com.tlc.mcp.resource.McpResource;
import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.McpToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class McpResourceRegistryTest {

    private McpResourceRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new McpResourceRegistry();
    }

    @Test
    void register_get_byUri() {
        registry.register("bundle-a", resource("crm://schema", "CRM Schema", "application/json"));
        final Optional<ResourceRegistration> reg = registry.get("crm://schema");
        assertTrue(reg.isPresent());
        assertEquals("crm://schema", reg.get().uri());
        assertEquals("CRM Schema", reg.get().name());
        assertEquals("application/json", reg.get().mimeType());
    }

    @Test
    void get_unknownUri_returnsEmpty() {
        assertTrue(registry.get("crm://unknown").isEmpty());
    }

    @Test
    void register_duplicateUri_replacesEntry() {
        registry.register("bundle-a", resource("crm://schema", "Old", "text/plain"));
        registry.register("bundle-b", resource("crm://schema", "New", "application/json"));
        assertEquals("New", registry.get("crm://schema").get().name());
    }

    @Test
    void list_returnsAllRegistered() {
        registry.register("bundle-a", resource("crm://r1", "R1", "application/json"));
        registry.register("bundle-a", resource("crm://r2", "R2", "text/plain"));
        final List<ResourceRegistration> all = registry.list();
        assertEquals(2, all.size());
    }

    @Test
    void unregisterBundle_removesOwnedResources() {
        registry.register("bundle-a", resource("crm://a", "A", "application/json"));
        registry.register("bundle-b", resource("crm://b", "B", "application/json"));
        registry.unregisterBundle("bundle-a");
        assertTrue(registry.get("crm://a").isEmpty());
        assertTrue(registry.get("crm://b").isPresent());
    }

    @Test
    void unregisterBundle_nonExistent_noException() {
        assertDoesNotThrow(() -> registry.unregisterBundle("no-such-bundle"));
    }

    private static ResourceRegistration resource(String uri, String name, String mime) {
        return new ResourceRegistration(uri, name, name, mime, new StubResource());
    }

    static class StubResource implements McpResource {
        @Override
        public McpToolResult read(McpToolContext<?> ctx) {
            return McpToolResult.text("content");
        }
    }
}
