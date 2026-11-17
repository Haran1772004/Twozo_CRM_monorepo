package com.tlc.mcp.action.internal.registry;

import com.tlc.mcp.internal.registry.McpToolVersionRegistry;
import com.tlc.mcp.internal.registry.ToolKey;
import com.tlc.mcp.internal.registry.ToolRegistration;
import com.tlc.mcp.tool.AbstractMcpTool;
import com.tlc.mcp.tool.ExecutionMode;
import com.tlc.mcp.tool.McpToolResult;
import com.tlc.mcp.tool.SchemaDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class McpToolVersionRegistryTest {

    private McpToolVersionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new McpToolVersionRegistry();
    }


    @Test
    void register_singleVersion_resolvableByName() {
        registry.register("bundle-a", reg("search", "1", false));
        assertTrue(registry.resolve("search").isPresent());
        assertEquals("1", registry.resolve("search").get().version());
    }

    @Test
    void register_singleVersion_versionedAliasPresent() {
        registry.register("bundle-a", reg("search", "1", false));
        final Map<String, ToolRegistration> aliases = registry.listVersionedAliases();
        assertTrue(aliases.containsKey("search_v1"));
    }


    @Test
    void register_multipleVersions_canonicalIsHighest() {
        registry.register("bundle-a", reg("search", "1", false));
        registry.register("bundle-a", reg("search", "2", false));
        assertEquals("2", registry.resolve("search").get().version());
    }

    @Test
    void register_multipleVersions_bothAliasesPresent() {
        registry.register("bundle-a", reg("search", "1", false));
        registry.register("bundle-a", reg("search", "2", false));
        final Map<String, ToolRegistration> aliases = registry.listVersionedAliases();
        assertTrue(aliases.containsKey("search_v1"));
        assertTrue(aliases.containsKey("search_v2"));
    }

    @Test
    void resolve_versionedAlias_returnsExactVersion() {
        registry.register("bundle-a", reg("search", "1", false));
        registry.register("bundle-a", reg("search", "2", false));
        final Optional<ToolRegistration> v1 = registry.resolve("search_v1");
        assertTrue(v1.isPresent());
        assertEquals("1", v1.get().version());
    }

    @Test
    void canonicalSkipsDeprecatedVersions() {
        registry.register("bundle-a", reg("search", "1", false));
        registry.register("bundle-a", reg("search", "2", true));   // deprecated
        assertEquals("1", registry.resolve("search").get().version());
    }

    @Test
    void allDeprecated_fallsBackToHighestWithWarn() {
        registry.register("bundle-a", reg("search", "1", true));
        registry.register("bundle-a", reg("search", "2", true));
        final Optional<ToolRegistration> resolved = registry.resolve("search");
        assertTrue(resolved.isPresent());
        assertEquals("2", resolved.get().version());
    }


    @Test
    void listCanonical_oneEntryPerName() {
        registry.register("b", reg("search", "1", false));
        registry.register("b", reg("search", "2", false));
        registry.register("b", reg("delete", "1", false));
        final List<ToolRegistration> canonical = registry.listCanonical();
        assertEquals(2, canonical.size());
    }

    @Test
    void listCanonical_empty_whenNoTools() {
        assertTrue(registry.listCanonical().isEmpty());
    }


    @Test
    void unregisterBundle_removesToolAndAliases() {
        registry.register("bundle-a", reg("search", "1", false));
        registry.unregisterBundle("bundle-a");
        assertTrue(registry.resolve("search").isEmpty());
        assertFalse(registry.listVersionedAliases().containsKey("search_v1"));
    }

    @Test
    void unregisterBundle_onlyRemovesOwnedTools() {
        registry.register("bundle-a", reg("tool-a", "1", false));
        registry.register("bundle-b", reg("tool-b", "1", false));
        registry.unregisterBundle("bundle-a");
        assertTrue(registry.resolve("tool-a").isEmpty());
        assertTrue(registry.resolve("tool-b").isPresent());
    }

    @Test
    void unregisterBundle_nonExistentBundle_noException() {
        assertDoesNotThrow(() -> registry.unregisterBundle("no-such-bundle"));
    }


    @Test
    void resolve_unknownName_returnsEmpty() {
        assertTrue(registry.resolve("not-registered").isEmpty());
    }


    @Test
    void toolKey_equalWhenNameAndVersionMatch() {
        assertEquals(new ToolKey("search", "1"), new ToolKey("search", "1"));
    }

    @Test
    void toolKey_notEqualWhenVersionDiffers() {
        assertNotEquals(new ToolKey("search", "1"), new ToolKey("search", "2"));
    }


    private static ToolRegistration reg(String name, String version, boolean deprecated) {
        try {
            final Method method = Stub.class.getMethod("execute");
            return new ToolRegistration(
                    name, version, name, "desc",
                    false, false, false, deprecated,
                    "default", -1, ExecutionMode.SYNC,
                    new Stub(), method, List.of(),
                    SchemaDefinition.empty(), "{}"
            );
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    static class Stub extends AbstractMcpTool<Object> {
        public McpToolResult execute() { return McpToolResult.text("ok"); }
    }
}
