package com.tlc.mcp.action.rest;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.Configurations;
import com.tlc.mcp.internal.McpServerImpl;
import com.tlc.mcp.internal.registry.McpServiceRegistry;
import com.tlc.mcp.internal.tool.McpPingTool;
import com.tlc.mcp.prompt.McpPrompt;
import com.tlc.mcp.prompt.McpPromptDef;
import com.tlc.mcp.resource.McpResource;
import com.tlc.mcp.resource.McpResourceDef;
import com.tlc.mcp.tool.AbstractMcpTool;
import com.tlc.mcp.tool.McpTool;
import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.McpToolResult;
import com.tlc.mcp.tool.ToolParam;
import com.tlc.web.ResponseSender;
import com.tlc.web.WebExchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class McpHttpTransportTest {

    private McpHttpTransport transport;
    private McpServerImpl server;
    private ExecutorService workerPool;

    @BeforeEach
    void setUp() {
        transport = new McpHttpTransport();
        workerPool = Executors.newSingleThreadExecutor();
        McpHttpTransport.setWorkerPool(workerPool);
        server = new McpServerImpl(new Configurations(Map.of()), 4);
        server.getToolRegistrar().register("mcp", McpPingTool.class);
        server.getToolRegistrar().register("test", EchoTool.class);
        server.getResourceRegistrar().register("test", TextResource.class);
        server.getPromptRegistrar().register("test", GreetingPrompt.class);
        McpServiceRegistry.getInstance().register(server);
        McpHttpTransport.setToolsResolver(server::toolsForIdentity);
    }

    @AfterEach
    void tearDown() {
        McpHttpTransport.setIdentityExtractor((exchange, cb) -> cb.accept(null));
        McpHttpTransport.setToolsResolver(identity -> List.of());
        McpServiceRegistry.getInstance().unregister();
        server.stop();
        workerPool.shutdown();
    }


    @Test
    void initialize_returnsProtocolVersionAndCapabilities() {
        final JsonObject resp = callHandle("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\"}");
        assertEquals("2.0", resp.getString("jsonrpc"));
        assertEquals(1, resp.getInt("id"));
        final JsonObject result = resp.getJsonObject("result");
        assertEquals("2025-11-25", result.getString("protocolVersion"));
        assertTrue(result.containsKey("capabilities"));
        assertTrue(result.containsKey("serverInfo"));
    }

    @Test
    void toolsList_returnsRegisteredTools() {
        final JsonObject resp = callHandle("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}");
        assertNull(resp.opt("error"));
        final var tools = resp.getJsonObject("result").getJsonArray("tools");
        assertTrue(tools.size() > 0);
        boolean found = false;
        for (int i = 0; i < tools.size(); i++) {
            if ("echo".equals(tools.getJsonObject(i).getString("name"))) { found = true; break; }
        }
        assertTrue(found, "echo tool should be listed");
    }

    @Test
    void toolsList_includesBuiltinMcpPing() {
        final JsonObject resp = callHandle("{\"jsonrpc\":\"2.0\",\"id\":99,\"method\":\"tools/list\"}");
        assertNull(resp.opt("error"));
        final var tools = resp.getJsonObject("result").getJsonArray("tools");
        boolean found = false;
        for (int i = 0; i < tools.size(); i++) {
            if ("mcp_ping".equals(tools.getJsonObject(i).getString("name"))) { found = true; break; }
        }
        assertTrue(found, "mcp_ping built-in must appear in tools/list. Got: " + tools);
    }

    @Test
    void mcpPing_noArgs_returnsDefaultEcho() {
        final String body = """
                {"jsonrpc":"2.0","id":100,"method":"tools/call",\
                "params":{"name":"mcp_ping","arguments":{}}}""";
        final JsonObject resp = callHandle(body);
        assertNull(resp.opt("error"));
        final String text = resp.getJsonObject("result").getJsonArray("content")
                .getJsonObject(0).getString("text");
        final JsonObject payload = Json.object(text);
        assertEquals("ping", payload.getString("echo"));
        assertEquals("tlc-mcp-server", payload.getString("server"));
        assertNotNull(payload.opt("timestamp"));
        assertEquals("anonymous", payload.getString("identity"));
    }

    @Test
    void mcpPing_withMessage_echoesMessage() {
        final String body = """
                {"jsonrpc":"2.0","id":101,"method":"tools/call",\
                "params":{"name":"mcp_ping","arguments":{"message":"hello-world"}}}""";
        final JsonObject resp = callHandle(body);
        assertNull(resp.opt("error"));
        final String text = resp.getJsonObject("result").getJsonArray("content")
                .getJsonObject(0).getString("text");
        assertEquals("hello-world", Json.object(text).getString("echo"));
    }

    @Test
    void mcpPing_withIdentity_showsIdentityInResponse() {
        McpHttpTransport.setIdentityExtractor((exchange, cb) -> cb.accept("org-42"));
        final String body = """
                {"jsonrpc":"2.0","id":102,"method":"tools/call",\
                "params":{"name":"mcp_ping","arguments":{}}}""";
        final JsonObject resp = callHandle(body);
        assertNull(resp.opt("error"));
        final String text = resp.getJsonObject("result").getJsonArray("content")
                .getJsonObject(0).getString("text");
        assertEquals("org-42", Json.object(text).getString("identity"));
    }

    @Test
    void toolsCall_happyPath_returnsContent() {
        final String body = """
                {"jsonrpc":"2.0","id":3,"method":"tools/call",\
                "params":{"name":"echo","arguments":{"message":"hi"}}}""";
        final JsonObject resp = callHandle(body);
        assertNull(resp.opt("error"));
        final var content = resp.getJsonObject("result").getJsonArray("content");
        assertEquals("hi", content.getJsonObject(0).getString("text"));
    }

    @Test
    void toolsCall_unknownTool_returnsIsErrorResult() {
        final String body = """
                {"jsonrpc":"2.0","id":4,"method":"tools/call",\
                "params":{"name":"no-such","arguments":{}}}""";
        final JsonObject resp = callHandle(body);
        assertNull(resp.opt("error"));
        assertTrue(resp.getJsonObject("result").getBoolean("isError"));
    }

    @Test
    void resourcesList_returnsRegisteredResources() {
        final JsonObject resp = callHandle("{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"resources/list\"}");
        assertNull(resp.opt("error"));
        final var resources = resp.getJsonObject("result").getJsonArray("resources");
        assertEquals(1, resources.size());
        assertEquals("test://text", resources.getJsonObject(0).getString("uri"));
    }

    @Test
    void resourcesRead_happyPath_returnsContents() {
        final String body = """
                {"jsonrpc":"2.0","id":6,"method":"resources/read",\
                "params":{"uri":"test://text"}}""";
        final JsonObject resp = callHandle(body);
        assertNull(resp.opt("error"));
        final var contents = resp.getJsonObject("result").getJsonArray("contents");
        assertEquals(1, contents.size());
        assertEquals("test://text", contents.getJsonObject(0).getString("uri"));
        assertEquals("text content", contents.getJsonObject(0).getString("text"));
    }

    @Test
    void resourcesRead_unknownUri_returnsInternalError() {
        final String body = """
                {"jsonrpc":"2.0","id":7,"method":"resources/read",\
                "params":{"uri":"test://no-such"}}""";
        final JsonObject resp = callHandle(body);
        assertNull(resp.opt("result"));
        assertEquals(-32603, resp.getJsonObject("error").getInt("code"));
    }

    @Test
    void resourcesRead_missingParams_returnsInvalidParamsError() {
        final String body = "{\"jsonrpc\":\"2.0\",\"id\":8,\"method\":\"resources/read\"}";
        final JsonObject resp = callHandle(body);
        assertEquals(-32602, resp.getJsonObject("error").getInt("code"));
    }

    @Test
    void promptsList_returnsRegisteredPrompts() {
        final JsonObject resp = callHandle("{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"prompts/list\"}");
        assertNull(resp.opt("error"));
        final var prompts = resp.getJsonObject("result").getJsonArray("prompts");
        assertEquals(1, prompts.size());
        assertEquals("greeting", prompts.getJsonObject(0).getString("name"));
    }

    @Test
    void promptsGet_happyPath_returnsMessages() {
        final String body = """
                {"jsonrpc":"2.0","id":10,"method":"prompts/get",\
                "params":{"name":"greeting","arguments":{"user":"Bob"}}}""";
        final JsonObject resp = callHandle(body);
        assertNull(resp.opt("error"));
        final var messages = resp.getJsonObject("result").getJsonArray("messages");
        assertEquals(1, messages.size());
        final JsonObject msg = messages.getJsonObject(0);
        assertEquals("user", msg.getString("role"));
        assertTrue(msg.getJsonObject("content").getString("text").contains("Bob"));
    }

    @Test
    void promptsGet_unknownName_returnsInternalError() {
        final String body = """
                {"jsonrpc":"2.0","id":11,"method":"prompts/get",\
                "params":{"name":"no-such"}}""";
        final JsonObject resp = callHandle(body);
        assertEquals(-32603, resp.getJsonObject("error").getInt("code"));
    }

    @Test
    void defaultIdentityExtractor_passesNullIdentity() {
        // No extractor set — identity in ctx must be null; IdentityAwareTool returns "null-identity"
        server.getToolRegistrar().register("test", IdentityAwareTool.class);
        final String body = """
                {"jsonrpc":"2.0","id":20,"method":"tools/call",\
                "params":{"name":"identity-check","arguments":{}}}""";
        final JsonObject resp = callHandle(body);
        assertNull(resp.opt("error"));
        assertEquals("null-identity",
                resp.getJsonObject("result").getJsonArray("content").getJsonObject(0).getString("text"));
    }

    @Test
    void setIdentityExtractor_identityFlowsIntoToolContext() {
        server.getToolRegistrar().register("test", IdentityAwareTool.class);
        McpHttpTransport.setIdentityExtractor((exchange, cb) -> cb.accept("test-org-42"));
        final String body = """
                {"jsonrpc":"2.0","id":21,"method":"tools/call",\
                "params":{"name":"identity-check","arguments":{}}}""";
        final JsonObject resp = callHandle(body);
        assertNull(resp.opt("error"));
        assertEquals("test-org-42",
                resp.getJsonObject("result").getJsonArray("content").getJsonObject(0).getString("text"));
    }

    @Test
    void unknownMethod_returnsMethodNotFoundError() {
        final String body = "{\"jsonrpc\":\"2.0\",\"id\":12,\"method\":\"unknown/method\"}";
        final JsonObject resp = callHandle(body);
        assertEquals(-32601, resp.getJsonObject("error").getInt("code"));
    }

    @Test
    void missingMethod_returnsInvalidRequestError() {
        final String body = "{\"jsonrpc\":\"2.0\",\"id\":13}";
        final JsonObject resp = callHandle(body);
        assertEquals(-32600, resp.getJsonObject("error").getInt("code"));
    }

    @Test
    void malformedJson_returnsInternalError() {
        final JsonObject resp = callHandle("{not-valid-json");
        assertNotNull(resp.opt("error"));
    }

    @Test
    void nullId_preservedInResponse() {
        final String body = "{\"jsonrpc\":\"2.0\",\"method\":\"initialize\"}";
        final JsonObject resp = callHandle(body);
        assertNull(resp.opt("id"));
    }


    private JsonObject callHandle(final String body) {
        final CompletableFuture<byte[]> future = new CompletableFuture<>();

        final ResponseSender fakeSender = (ResponseSender) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{ResponseSender.class},
                (proxy, method, args) -> {
                    if ("send".equals(method.getName()) && args != null && args.length >= 2) {
                        future.complete((byte[]) args[1]);
                    }
                    return null;
                });

        final WebExchange fakeExchange = (WebExchange) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{WebExchange.class},
                (proxy, method, args) -> {
                    if ("responseSender".equals(method.getName())) return fakeSender;
                    return null;
                });

        transport.handle(fakeExchange, body.getBytes(StandardCharsets.UTF_8));
        try {
            final byte[] bytes = future.get(5, TimeUnit.SECONDS);
            assertNotNull(bytes, "responseSender.send() was not called");
            return Json.object(new String(bytes, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return fail("Response not received within 5 seconds: " + e.getMessage());
        }
    }


    @McpTool(name = "echo", description = "Echoes input")
    public static class EchoTool extends AbstractMcpTool<Object> {
        public McpToolResult execute(@ToolParam(description = "message") String message) {
            return McpToolResult.text(message);
        }
    }

    @McpResourceDef(uri = "test://text", name = "Text", mimeType = "text/plain")
    public static class TextResource implements McpResource {
        @Override
        public McpToolResult read(McpToolContext<?> ctx) {
            return McpToolResult.text("text content");
        }
    }

    @McpPromptDef(name = "greeting", description = "A greeting prompt")
    public static class GreetingPrompt implements McpPrompt {
        @Override
        public McpToolResult get(Map<String, String> args, McpToolContext<?> ctx) {
            return McpToolResult.text("Hello, " + args.getOrDefault("user", "World") + "!");
        }
    }

    @McpTool(name = "identity-check", description = "Returns the identity string from ctx")
    public static class IdentityAwareTool extends AbstractMcpTool<Object> {
        public McpToolResult execute(final McpToolContext<Object> ctx) {
            final String identity = ctx.identity() != null ? ctx.identity().toString() : "null-identity";
            return McpToolResult.text(identity);
        }
    }
}
