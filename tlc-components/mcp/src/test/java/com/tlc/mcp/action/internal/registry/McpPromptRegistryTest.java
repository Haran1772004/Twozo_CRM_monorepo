package com.tlc.mcp.action.internal.registry;

import com.tlc.mcp.internal.registry.McpPromptRegistry;
import com.tlc.mcp.internal.registry.PromptRegistration;
import com.tlc.mcp.prompt.McpPrompt;
import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.McpToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class McpPromptRegistryTest {

    private McpPromptRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new McpPromptRegistry();
    }

    @Test
    void register_resolve_byName() {
        registry.register("bundle-a", prompt("greet", "1"));
        final Optional<PromptRegistration> reg = registry.resolve("greet");
        assertTrue(reg.isPresent());
        assertEquals("greet", reg.get().name());
        assertEquals("1", reg.get().version());
    }

    @Test
    void resolve_unknownName_returnsEmpty() {
        assertTrue(registry.resolve("no-such-prompt").isEmpty());
    }

    @Test
    void register_multipleVersions_resolvesHighest() {
        registry.register("bundle-a", prompt("greet", "1"));
        registry.register("bundle-a", prompt("greet", "2"));
        assertEquals("2", registry.resolve("greet").get().version());
    }

    @Test
    void listAll_returnsCanonicalPerName() {
        registry.register("bundle-a", prompt("greet", "1"));
        registry.register("bundle-a", prompt("greet", "2"));
        registry.register("bundle-a", prompt("farewell", "1"));
        final List<PromptRegistration> all = registry.listAll();
        assertEquals(2, all.size());
    }

    @Test
    void unregisterBundle_removesOwnedPrompts() {
        registry.register("bundle-a", prompt("greet", "1"));
        registry.register("bundle-b", prompt("farewell", "1"));
        registry.unregisterBundle("bundle-a");
        assertTrue(registry.resolve("greet").isEmpty());
        assertTrue(registry.resolve("farewell").isPresent());
    }

    @Test
    void unregisterBundle_nonExistent_noException() {
        assertDoesNotThrow(() -> registry.unregisterBundle("no-such-bundle"));
    }

    private static PromptRegistration prompt(String name, String version) {
        return new PromptRegistration(name, name, "A prompt", version, new StubPrompt());
    }

    static class StubPrompt implements McpPrompt {
        @Override
        public McpToolResult get(Map<String, String> arguments, McpToolContext<?> ctx) {
            return McpToolResult.text("prompt content");
        }
    }
}
