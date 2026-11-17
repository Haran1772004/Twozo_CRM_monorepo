package com.tlc.mcp.action.internal;

import com.tlc.commons.util.Configurations;
import com.tlc.mcp.internal.McpServerImpl;
import com.tlc.mcp.prompt.McpPrompt;
import com.tlc.mcp.prompt.McpPromptDef;
import com.tlc.mcp.prompt.McpPromptInfo;
import com.tlc.mcp.resource.McpResource;
import com.tlc.mcp.resource.McpResourceDef;
import com.tlc.mcp.resource.McpResourceInfo;
import com.tlc.mcp.tool.AbstractMcpTool;
import com.tlc.mcp.tool.ExecutionMode;
import com.tlc.mcp.tool.McpTool;
import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.McpToolInfo;
import com.tlc.mcp.tool.McpToolResult;
import com.tlc.mcp.tool.SchemaDefinition;
import com.tlc.mcp.tool.ToolParam;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class McpServiceUtilsIT {

    private McpServerImpl server;

    @BeforeEach
    void setUp() {
        server = new McpServerImpl(new Configurations(Map.of()), 4);
        server.getToolRegistrar().register("bundle-a", GreetTool.class);
        server.getToolRegistrar().register("bundle-a", AuthFailTool.class);
        server.getResourceRegistrar().register("bundle-a", HelloResource.class);
        server.getPromptRegistrar().register("bundle-a", WelcomePrompt.class);
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }


    @Test
    void tools_includesRegisteredAndBuiltin() {
        final List<McpToolInfo> list = server.tools();
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(t -> "greet".equals(t.name())));
        assertTrue(list.stream().anyMatch(t -> "get_task_status".equals(t.name())));
    }

    @Test
    void tools_returnsCorrectMetadata() {
        final McpToolInfo info = server.tools().stream()
                .filter(t -> "greet".equals(t.name()))
                .findFirst()
                .orElseThrow();
        assertEquals("greet", info.name());
        assertEquals("1", info.version());
        assertEquals("Greet user", info.description());
        assertEquals(ExecutionMode.SYNC, info.mode());
        assertTrue(info.readOnly());
        assertFalse(info.deprecated());
        assertNotNull(info.schema());
        assertNotNull(info.inputSchemaJson());
    }

    @Test
    void tools_isUnmodifiable() {
        assertThrows(UnsupportedOperationException.class, () -> server.tools().add(null));
    }


    @Test
    void tool_knownName_returnsInfo() {
        final Optional<McpToolInfo> opt = server.tool("greet");
        assertTrue(opt.isPresent());
        assertEquals("greet", opt.get().name());
    }

    @Test
    void tool_builtin_returnsInfo() {
        final Optional<McpToolInfo> opt = server.tool("get_task_status");
        assertTrue(opt.isPresent());
        assertEquals("get_task_status", opt.get().name());
        assertTrue(opt.get().readOnly());
    }

    @Test
    void tool_unknownName_returnsEmpty() {
        assertTrue(server.tool("no-such-tool").isEmpty());
    }


    @Test
    void resources_includesRegistered() {
        final List<McpResourceInfo> list = server.resources();
        assertEquals(1, list.size());
        assertEquals("crm://hello", list.get(0).uri());
        assertEquals("Hello", list.get(0).name());
        assertEquals("text/plain", list.get(0).mimeType());
    }


    @Test
    void resource_knownUri_returnsInfo() {
        final Optional<McpResourceInfo> opt = server.resource("crm://hello");
        assertTrue(opt.isPresent());
        assertEquals("crm://hello", opt.get().uri());
    }

    @Test
    void resource_unknownUri_returnsEmpty() {
        assertTrue(server.resource("crm://no-such").isEmpty());
    }


    @Test
    void prompts_includesRegistered() {
        final List<McpPromptInfo> list = server.prompts();
        assertEquals(1, list.size());
        assertEquals("welcome", list.get(0).name());
        assertEquals("Welcoming prompt", list.get(0).description());
    }


    @Test
    void prompt_knownName_returnsInfo() {
        final Optional<McpPromptInfo> opt = server.prompt("welcome");
        assertTrue(opt.isPresent());
        assertEquals("welcome", opt.get().name());
    }

    @Test
    void prompt_unknownName_returnsEmpty() {
        assertTrue(server.prompt("no-such").isEmpty());
    }


    @Test
    void invokeTool_happyPath_returnsResult() {
        final McpToolResult result = server.invokeTool("greet", Map.of("name", "Alice"));
        assertFalse(result.isError());
        assertEquals("Hello, Alice!", result.content().get(0).data());
    }

    @Test
    void invokeTool_nullArguments_treatedAsEmpty() {
        final McpToolResult result = server.invokeTool("greet", null);
        assertTrue(result.isError());
    }

    @Test
    void invokeTool_withIdentity_propagatedToContext() {
        final Object identity = new Object();
        AuthCaptureTool.capturedIdentity = null;
        server.getToolRegistrar().register("bundle-b", AuthCaptureTool.class);

        server.invokeTool("capture_auth", Map.of(), identity);
        assertSame(identity, AuthCaptureTool.capturedIdentity);
    }

    @Test
    void invokeTool_defaultOverload_passesNullIdentity() {
        AuthCaptureTool.capturedIdentity = "NOT_NULL";
        server.getToolRegistrar().register("bundle-b", AuthCaptureTool.class);

        server.invokeTool("capture_auth", Map.of());
        assertNull(AuthCaptureTool.capturedIdentity);
    }

    @Test
    void invokeTool_unknownTool_returnsError() {
        final McpToolResult result = server.invokeTool("no-such-tool", Map.of());
        assertTrue(result.isError());
        assertTrue(result.content().get(0).data().contains("Unknown tool"));
    }

    @Test
    void invokeTool_authFails_returnsUnauthorizedError() {
        final McpToolResult result = server.invokeTool("auth_fail", Map.of());
        assertTrue(result.isError());
        assertTrue(result.content().get(0).data().contains("Unauthorized"));
    }

    @Test
    void invokeTool_validationFails_returnsValidationError() {
        final McpToolResult result = server.invokeTool("greet", Map.of());
        assertTrue(result.isError());
        assertTrue(result.content().get(0).data().contains("Validation failed"));
    }

    @Test
    void invokeTool_builtin_getTaskStatus_unknownId_returnsError() {
        final McpToolResult result = server.invokeTool("get_task_status", Map.of("taskId", "nope"));
        assertTrue(result.isError());
    }


    @McpTool(name = "greet", description = "Greet user", readOnly = true)
    public static class GreetTool extends AbstractMcpTool<Object> {
        public McpToolResult execute(
                @ToolParam(description = "name") String name) {
            return McpToolResult.text("Hello, " + name + "!");
        }
    }

    @McpTool(name = "auth_fail", description = "Always unauthorized")
    public static class AuthFailTool extends AbstractMcpTool<Object> {
        @Override
        public boolean isAuthorized(McpToolContext<Object> ctx) { return false; }

        public McpToolResult execute() {
            return McpToolResult.text("should not reach");
        }
    }

    @McpTool(name = "capture_auth", description = "Captures identity from context")
    public static class AuthCaptureTool extends AbstractMcpTool<Object> {
        static Object capturedIdentity = "NOT_CALLED";

        public McpToolResult execute(McpToolContext<Object> ctx) {
            capturedIdentity = ctx.identity();
            return McpToolResult.text("captured");
        }
    }

    @Test
    void readResource_happyPath_returnsContent() {
        final McpToolResult result = server.readResource("crm://hello");
        assertFalse(result.isError());
        assertEquals("hello", result.content().get(0).data());
    }

    @Test
    void readResource_unknownUri_returnsError() {
        final McpToolResult result = server.readResource("crm://no-such");
        assertTrue(result.isError());
        assertTrue(result.content().get(0).data().contains("Unknown resource"));
    }

    @Test
    void readResource_authFails_returnsError() {
        server.getResourceRegistrar().register("bundle-a", AuthFailResource.class);
        final McpToolResult result = server.readResource("crm://auth-fail");
        assertTrue(result.isError());
        assertTrue(result.content().get(0).data().contains("Unauthorized"));
    }

    @Test
    void readResource_withIdentity_propagatedToContext() {
        final Object identity = new Object();
        AuthCaptureResource.capturedIdentity = null;
        server.getResourceRegistrar().register("bundle-b", AuthCaptureResource.class);
        server.readResource("crm://capture", identity);
        assertSame(identity, AuthCaptureResource.capturedIdentity);
    }

    @Test
    void readResource_defaultOverload_passesNullIdentity() {
        AuthCaptureResource.capturedIdentity = "NOT_NULL";
        server.getResourceRegistrar().register("bundle-b", AuthCaptureResource.class);
        server.readResource("crm://capture");
        assertNull(AuthCaptureResource.capturedIdentity);
    }

    @Test
    void invokePrompt_happyPath_returnsResult() {
        final McpToolResult result = server.invokePrompt("welcome", Map.of());
        assertFalse(result.isError());
        assertEquals("Welcome!", result.content().get(0).data());
    }

    @Test
    void invokePrompt_unknownName_returnsError() {
        final McpToolResult result = server.invokePrompt("no-such-prompt", Map.of());
        assertTrue(result.isError());
        assertTrue(result.content().get(0).data().contains("Unknown prompt"));
    }

    @Test
    void invokePrompt_authFails_returnsError() {
        server.getPromptRegistrar().register("bundle-a", AuthFailPrompt.class);
        final McpToolResult result = server.invokePrompt("prompt_auth_fail", Map.of());
        assertTrue(result.isError());
        assertTrue(result.content().get(0).data().contains("Unauthorized"));
    }

    @Test
    void invokePrompt_withArgs_passedThrough() {
        server.getPromptRegistrar().register("bundle-b", ArgCapturePrompt.class);
        ArgCapturePrompt.capturedArgs = null;
        server.invokePrompt("arg_capture", Map.of("key", "value"));
        assertNotNull(ArgCapturePrompt.capturedArgs);
        assertEquals("value", ArgCapturePrompt.capturedArgs.get("key"));
    }

    @Test
    void invokePrompt_nullArgs_treatedAsEmpty() {
        server.getPromptRegistrar().register("bundle-b", ArgCapturePrompt.class);
        ArgCapturePrompt.capturedArgs = null;
        server.invokePrompt("arg_capture", null);
        assertNotNull(ArgCapturePrompt.capturedArgs);
        assertTrue(ArgCapturePrompt.capturedArgs.isEmpty());
    }

    @Test
    void invokePrompt_withIdentity_propagatedToContext() {
        final Object identity = new Object();
        AuthCapturePrompt.capturedIdentity = null;
        server.getPromptRegistrar().register("bundle-b", AuthCapturePrompt.class);
        server.invokePrompt("prompt_capture_auth", Map.of(), identity);
        assertSame(identity, AuthCapturePrompt.capturedIdentity);
    }

    @Test
    void invokePrompt_defaultOverload_passesNullIdentity() {
        AuthCapturePrompt.capturedIdentity = "NOT_NULL";
        server.getPromptRegistrar().register("bundle-b", AuthCapturePrompt.class);
        server.invokePrompt("prompt_capture_auth", Map.of());
        assertNull(AuthCapturePrompt.capturedIdentity);
    }


    @McpResourceDef(uri = "crm://hello", name = "Hello", mimeType = "text/plain")
    public static class HelloResource implements McpResource {
        @Override
        public McpToolResult read(McpToolContext<?> ctx) {
            return McpToolResult.text("hello");
        }
    }

    @McpResourceDef(uri = "crm://auth-fail", name = "AuthFail", mimeType = "text/plain")
    public static class AuthFailResource implements McpResource {
        @Override
        public boolean isAuthorized(McpToolContext<?> ctx) { return false; }

        @Override
        public McpToolResult read(McpToolContext<?> ctx) {
            return McpToolResult.text("should not reach");
        }
    }

    @McpResourceDef(uri = "crm://capture", name = "Capture", mimeType = "text/plain")
    public static class AuthCaptureResource implements McpResource {
        static Object capturedIdentity = "NOT_CALLED";

        @Override
        public McpToolResult read(McpToolContext<?> ctx) {
            capturedIdentity = ctx.identity();
            return McpToolResult.text("captured");
        }
    }

    @McpPromptDef(name = "welcome", description = "Welcoming prompt")
    public static class WelcomePrompt implements McpPrompt {
        @Override
        public McpToolResult get(Map<String, String> args, McpToolContext<?> ctx) {
            return McpToolResult.text("Welcome!");
        }
    }

    @McpPromptDef(name = "prompt_auth_fail", description = "Always unauthorized")
    public static class AuthFailPrompt implements McpPrompt {
        @Override
        public boolean isAuthorized(McpToolContext<?> ctx) { return false; }

        @Override
        public McpToolResult get(Map<String, String> args, McpToolContext<?> ctx) {
            return McpToolResult.text("should not reach");
        }
    }

    @McpPromptDef(name = "arg_capture", description = "Captures args")
    public static class ArgCapturePrompt implements McpPrompt {
        static Map<String, String> capturedArgs;

        @Override
        public McpToolResult get(Map<String, String> args, McpToolContext<?> ctx) {
            capturedArgs = args;
            return McpToolResult.text("captured");
        }
    }

    @McpPromptDef(name = "prompt_capture_auth", description = "Captures identity")
    public static class AuthCapturePrompt implements McpPrompt {
        static Object capturedIdentity = "NOT_CALLED";

        @Override
        public McpToolResult get(Map<String, String> args, McpToolContext<?> ctx) {
            capturedIdentity = ctx.identity();
            return McpToolResult.text("captured");
        }
    }


    @Test
    void toolsForIdentity_nullIdentity_includesDefaultTools() {
        server.getToolRegistrar().register("bundle-c", IncludeFilteredTool.class);
        final List<McpToolInfo> list = server.toolsForIdentity(null);
        assertTrue(list.stream().anyMatch(t -> "greet".equals(t.name())));
        assertTrue(list.stream().anyMatch(t -> "get_task_status".equals(t.name())));
        assertTrue(list.stream().noneMatch(t -> "identity_only".equals(t.name())));
    }

    @Test
    void toolsForIdentity_withIdentity_includesFilteredTool() {
        server.getToolRegistrar().register("bundle-c", IncludeFilteredTool.class);
        final List<McpToolInfo> list = server.toolsForIdentity("authenticated-user");
        assertTrue(list.stream().anyMatch(t -> "identity_only".equals(t.name())));
    }

    @Test
    void toolsForIdentity_isUnmodifiable() {
        assertThrows(UnsupportedOperationException.class,
                () -> server.toolsForIdentity(null).add(null));
    }

    @Test
    void toolsForIdentity_customSchema_hidesFieldForAnonymous() {
        server.getToolRegistrar().register("bundle-c", SchemaCustomTool.class);
        final List<McpToolInfo> anonList = server.toolsForIdentity(null);
        final McpToolInfo anonInfo = anonList.stream()
                .filter(t -> "schema_custom".equals(t.name()))
                .findFirst().orElseThrow();
        assertNotNull(anonInfo.schema());
        assertTrue(anonInfo.schema().field("secretParam").isEmpty());
        assertTrue(anonInfo.schema().field("query").isPresent());
    }

    @Test
    void toolsForIdentity_customSchema_exposesFieldForAuthenticated() {
        server.getToolRegistrar().register("bundle-c", SchemaCustomTool.class);
        final List<McpToolInfo> authedList = server.toolsForIdentity("power-user");
        final McpToolInfo authedInfo = authedList.stream()
                .filter(t -> "schema_custom".equals(t.name()))
                .findFirst().orElseThrow();
        assertNotNull(authedInfo.schema());
        assertTrue(authedInfo.schema().field("secretParam").isPresent());
        assertTrue(authedInfo.schema().field("query").isPresent());
    }

    @Test
    void tools_delegatesToToolsForIdentity_nullIdentity() {
        server.getToolRegistrar().register("bundle-c", IncludeFilteredTool.class);
        final List<McpToolInfo> tools = server.tools();
        assertTrue(tools.stream().noneMatch(t -> "identity_only".equals(t.name())));
    }


    @McpTool(name = "identity_only", description = "Only visible to authenticated users")
    public static class IncludeFilteredTool extends AbstractMcpTool<Object> {
        @Override
        public boolean isVisible(McpToolContext<Object> ctx) {
            return ctx.identity() != null;
        }

        public McpToolResult execute() {
            return McpToolResult.text("ok");
        }
    }

    @McpTool(name = "schema_custom", description = "Schema varies by identity")
    public static class SchemaCustomTool extends AbstractMcpTool<Object> {
        @Override
        public SchemaDefinition inputSchema(SchemaDefinition generated, McpToolContext<Object> ctx) {
            if (ctx.identity() == null) {
                return generated.withoutField("secretParam");
            }
            return generated;
        }

        public McpToolResult execute(
                @ToolParam(description = "query") String query,
                @ToolParam(description = "secret") String secretParam) {
            return McpToolResult.text("ok");
        }
    }
}
