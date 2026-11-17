package com.tlc.mcp.action.rest;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.mcp.McpService;
import com.tlc.mcp.action.McpIdentityExtractor;
import com.tlc.mcp.internal.status.McpErrorCodes;
import com.tlc.mcp.prompt.McpPromptInfo;
import com.tlc.mcp.provider.McpServiceProvider;
import com.tlc.mcp.resource.McpResourceInfo;
import com.tlc.mcp.tool.McpToolInfo;
import com.tlc.mcp.tool.McpToolResult;
import com.tlc.web.Action;
import com.tlc.web.FullBytesCallback;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpStatus;
import com.tlc.web.WebExchange;
import com.tlc.web.undertow.rest.annotation.ApiRoute;
import com.tlc.web.undertow.rest.annotation.RestApiHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

@RestApiHandler(path = "/mcp")
public class McpHttpTransport implements Action, FullBytesCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpHttpTransport.class);

    private static final String JSONRPC_VERSION = "2.0";
    private static final String MCP_PROTOCOL_VERSION = "2025-11-25";
    private static final Map<String, String> JSON_HEADERS = Map.of("Content-Type", "application/json");
    private static final byte[] EMPTY_BODY = new byte[0];

    private static volatile ExecutorService workerPool;
    private static volatile McpIdentityExtractor<Object> IDENTITY_EXTRACTOR = (exchange, cb) -> cb.accept(null);
    private static volatile Function<Object, List<McpToolInfo>> TOOLS_RESOLVER = identity -> List.of();

    public static void setWorkerPool(final ExecutorService pool) {
        workerPool = pool;
    }

    public static void shutdownWorkerPool() {
        final ExecutorService pool = workerPool;
        if (pool != null) {
            pool.shutdown();
        }
    }

    @SuppressWarnings("unchecked")
    public static <I> void setIdentityExtractor(final McpIdentityExtractor<I> extractor) {
        IDENTITY_EXTRACTOR = (McpIdentityExtractor<Object>) extractor;
    }

    public static void setToolsResolver(final Function<Object, List<McpToolInfo>> resolver) {
        TOOLS_RESOLVER = resolver;
    }

    @ApiRoute(method = HttpMethod.POST)
    @Override
    public void process(final WebExchange exchange) {
        exchange.requestReceiver().readBody(this);
    }

    @Override
    public void handle(final WebExchange exchange, final byte[] body) {
        Object id = null;
        try {
            final JsonObject req = Json.object(new String(body, StandardCharsets.UTF_8));

            LOGGER.debug("MCP Request: {}",  req);
            id = req.opt("id");
            final String method = req.optString("method", null);
            if (method == null) {
                sendError(exchange, null, McpErrorCodes.INVALID_REQUEST.getCode(), "Invalid Request: method is required");
                return;
            }

            if (isNotification(method)) {
                sendAccepted(exchange);
                return;
            }
            final JsonObject params = req.optJsonObject("params");
            final Object capturedId = id;
            if (needsIdentity(method)) {
                dispatchWithIdentity(exchange, capturedId, method, params);
            } else {
                executeAndSend(exchange, capturedId, method, () -> dispatchSync(method, params));
            }
        } catch (UnsupportedOperationException e) {
            sendError(exchange, id, McpErrorCodes.METHOD_NOT_FOUND.getCode(), e.getMessage());
        } catch (IllegalArgumentException e) {
            sendError(exchange, id, McpErrorCodes.INVALID_PARAMS.getCode(), e.getMessage());
        } catch (Exception e) {
            sendError(exchange, id, McpErrorCodes.INTERNAL_ERROR.getCode(),
                    e.getMessage() != null ? e.getMessage() : "Internal error");
        }
    }

    /**
     * Dispatches the request after extracting the identity from the exchange.
     *
     * @param exchange The WebExchange to send the response to.
     * @param id       The JSON-RPC request ID.
     * @param method   The method name for logging purposes.
     * @param params   The parameters for the method call.
     */
    private void dispatchWithIdentity(final WebExchange exchange, final Object id,
                                      final String method, final JsonObject params) {
        try {
            IDENTITY_EXTRACTOR.extract(exchange, identity -> {
                try {
                    executeAndSend(exchange, id, method, () -> dispatchIdentity(method, params, identity));
                } catch (Exception e) {
                    LOGGER.error("MCP: Failed after identity resolution for method={}", method, e);
                    sendError(exchange, id, McpErrorCodes.INTERNAL_ERROR.getCode(),
                            e.getMessage() != null ? e.getMessage() : "Internal error");
                }
            });
        } catch (Exception e) {
            LOGGER.error("MCP: Identity extraction failed for method={}", method, e);
            sendError(exchange, id, McpErrorCodes.INTERNAL_ERROR.getCode(),
                    e.getMessage() != null ? e.getMessage() : "Identity extraction failed");
        }
    }

    /**
     * Executes the given action in a worker thread and sends the result back to the client.
     *
     * @param exchange The WebExchange to send the response to.
     * @param id       The JSON-RPC request ID.
     * @param method   The method name for logging purposes.
     * @param action   The action to execute, which returns a JsonObject result.
     */
    private void executeAndSend(final WebExchange exchange, final Object id, final String method,
                                final Supplier<JsonObject> action) {
        try {
            workerPool.execute(() -> {
                try {
                    final long startedAt = System.currentTimeMillis();
                    final JsonObject result = action.get();

                    LOGGER.info("MCP: Execute and send method={} id={} durationMs={}", method, id, System.currentTimeMillis() - startedAt);
                    sendSuccess(exchange, id, method, result);
                } catch (UnsupportedOperationException e) {
                    sendError(exchange, id, McpErrorCodes.METHOD_NOT_FOUND.getCode(), e.getMessage());
                } catch (IllegalArgumentException e) {
                    sendError(exchange, id, McpErrorCodes.INVALID_PARAMS.getCode(), e.getMessage());
                } catch (Exception e) {
                    LOGGER.error("MCP: Worker failed for method={} id={}", method, id, e);
                    sendError(exchange, id, McpErrorCodes.INTERNAL_ERROR.getCode(), e.getMessage() != null ? e.getMessage() : "Internal error");
                }
            });
        } catch (Exception e) {
            LOGGER.error("MCP: Failed to submit work to worker pool", e);
            sendError(exchange, id, McpErrorCodes.INTERNAL_ERROR.getCode(), e.getMessage() != null ? e.getMessage() : "Failed to schedule MCP work");
        }
    }

    /**
     * Determines if the given method is a JSON-RPC notification.
     *
     * <p>According to the JSON-RPC 2.0 specification, notifications are requests that do not require a response.
     * In this implementation, we consider any method that starts with "notifications/" as a notification.</p>
     *
     * @param method The method name to check.
     * @return {@code true} if the method is a notification; {@code false} otherwise.
     */
    private static boolean isNotification(final String method) {
        return method.startsWith("notifications/");
    }

    private static boolean needsIdentity(final String method) {
        return switch (method) {
            case "tools/list", "tools/call", "resources/read", "prompts/get" -> true;
            default -> false;
        };
    }

    private static JsonObject dispatchSync(final String method, final JsonObject params) {
        final McpService mcp = McpServiceProvider.get();
        return switch (method) {
            case "initialize" -> handleInitialize();
            case "ping" -> Json.object();
            case "resources/list" -> handleResourcesList(mcp);
            case "prompts/list" -> handlePromptsList(mcp);
            default -> throw new UnsupportedOperationException("Method not found: " + method);
        };
    }

    private static JsonObject dispatchIdentity(final String method, final JsonObject params,
                                               final Object identity) {
        final McpService mcp = McpServiceProvider.get();
        return switch (method) {
            case "tools/list" -> handleToolsList(TOOLS_RESOLVER.apply(identity));
            case "tools/call" -> {
                requireParams(params, "tools/call");
                final String name = params.getString("name");
                final JsonObject argsJson = params.optJsonObject("arguments");
                final Map<String, Object> args = argsJson != null ? argsJson.toMap() : Collections.emptyMap();
                yield toolResultToJson(mcp.invokeTool(name, args, identity));
            }
            case "resources/read" -> {
                requireParams(params, "resources/read");
                final String uri = params.getString("uri");
                final McpToolResult result = mcp.readResource(uri, identity);
                if (result.isError()) {
                    throw new RuntimeException(result.content().isEmpty()
                            ? "Resource read failed" : result.content().get(0).data());
                }
                final JsonArray contents = Json.array();
                for (final McpToolResult.ContentItem item : result.content()) {
                    final JsonObject entry = Json.object().put("uri", uri).put("text", item.data());
                    if (item.mimeType() != null) entry.put("mimeType", item.mimeType());
                    contents.put(entry);
                }
                yield Json.object().put("contents", contents);
            }
            case "prompts/get" -> {
                requireParams(params, "prompts/get");
                final String name = params.getString("name");
                final JsonObject argsJson = params.optJsonObject("arguments");
                final Map<String, String> args = new HashMap<>();
                if (argsJson != null) argsJson.forEach((k, v) -> args.put(k, String.valueOf(v)));
                final McpToolResult result = mcp.invokePrompt(name, args, identity);
                if (result.isError()) {
                    throw new RuntimeException(result.content().isEmpty()
                            ? "Prompt failed" : result.content().get(0).data());
                }
                final JsonArray messages = Json.array();
                for (final McpToolResult.ContentItem item : result.content()) {
                    messages.put(Json.object()
                            .put("role",    "user")
                            .put("content", Json.object().put("type", "text").put("text", item.data())));
                }
                yield Json.object().put("messages", messages);
            }
            default -> throw new UnsupportedOperationException("Method not found: " + method);
        };
    }

    private static JsonObject handleInitialize() {
        return Json.orderedObject()
                .put("protocolVersion", MCP_PROTOCOL_VERSION)
                .put("capabilities", Json.object()
                        .put("tools",     Json.object())
                        .put("resources", Json.object())
                        .put("prompts",   Json.object()))
                .put("serverInfo", Json.object()
                        .put("name",    "tlc-mcp-server")
                        .put("version", "1.0.0"));
    }

    private static JsonObject handleToolsList(final List<McpToolInfo> tools) {
        final JsonArray toolsArray = Json.array();

        LOGGER.debug("MCP: Tools list names={}", tools.stream().map(McpToolInfo::name).toList());

        for (final McpToolInfo info : tools) {
            final JsonObject tool = Json.object()
                    .put("name",        info.name())
                    .put("description", info.description());
            if (info.schema() != null) {
                tool.put("inputSchema", info.schema().toJsonSchema());
            } else {
                tool.put("inputSchema", Json.object()
                        .put("type",       "object")
                        .put("properties", Json.object()));
            }
            toolsArray.put(tool);
        }
        LOGGER.info("MCP: Returning tools list. Total count={}", toolsArray.size());
        return Json.object().put("tools", toolsArray);
    }

    private static JsonObject handleResourcesList(final McpService mcp) {
        final JsonArray resources = Json.array();
        for (final McpResourceInfo info : mcp.resources()) {
            resources.put(Json.object()
                    .put("uri",      info.uri())
                    .put("name",     info.name())
                    .put("mimeType", info.mimeType()));
        }
        return Json.object().put("resources", resources);
    }

    private static JsonObject handlePromptsList(final McpService mcp) {
        final JsonArray prompts = Json.array();
        for (final McpPromptInfo info : mcp.prompts()) {
            prompts.put(Json.object()
                    .put("name",        info.name())
                    .put("description", info.description()));
        }
        return Json.object().put("prompts", prompts);
    }

    private static JsonObject toolResultToJson(final McpToolResult result) {
        final JsonArray content = Json.array();
        for (final McpToolResult.ContentItem item : result.content()) {
            final JsonObject c = Json.object().put("type", item.type()).put("text", item.data());
            if (item.mimeType() != null) {
                c.put("mimeType", item.mimeType());
            }
            content.put(c);
        }
        return Json.object()
                .put("content", content)
                .put("isError", result.isError());
    }

    private static void requireParams(final JsonObject params, final String method) {
        if (params == null) {
            throw new IllegalArgumentException("params is required for " + method);
        }
    }

    /**
     * Sends a 202 Accepted response for JSON-RPC notifications.
     *
     * <p>According to the JSON-RPC 2.0 specification, notifications must not receive a response body.
     * Therefore, we send an empty body with a 202 status code.</p>
     *
     * @param exchange The WebExchange to send the response to.
     */
    private static void sendAccepted(final WebExchange exchange) {
        try {
            exchange.responseSender().send(Map.of(), EMPTY_BODY, HttpStatus.ACCEPTED);
        } catch (Exception e) {
            LOGGER.error("MCP: Failed to send 202 Accepted for notification", e);
        }
    }

    /**
     * Sends a successful JSON-RPC response with the given result.
     *
     * @param exchange The WebExchange to send the response to.
     * @param id       The JSON-RPC request ID.
     * @param method   The method name for logging purposes.
     * @param result   The result object to include in the response.
     */
    private static void sendSuccess(final WebExchange exchange, final Object id,
                                    final String method, final JsonObject result) {
        try {
            final JsonObject response = Json.orderedObject()
                    .put("jsonrpc", JSONRPC_VERSION)
                    .put("id", id)
                    .put("result", result);
            final byte[] body = response.getBytes();

            LOGGER.info("MCP: Successfully sent response for method={} iD={}", method, id);
            exchange.responseSender().send(JSON_HEADERS, body);
        } catch (Exception e) {
            LOGGER.error("MCP: Failed to send success response for method={} id={}", method, id, e);
        }
    }

    /**
     * Sends an error JSON-RPC response with the given error code and message.
     *
     * @param exchange The WebExchange to send the response to.
     * @param id       The JSON-RPC request ID.
     * @param code     The error code.
     * @param message  The error message.
     */
    private static void sendError(final WebExchange exchange, final Object id,
                                  final int code, final String message) {
        try {
            final JsonObject response = Json.orderedObject()
                    .put("jsonrpc", JSONRPC_VERSION)
                    .put("id", id)
                    .put("error", Json.orderedObject()
                            .put("code", code)
                            .put("message", message));

            LOGGER.error("MCP Error: code={} message={}", code, message);
            exchange.responseSender().send(JSON_HEADERS, response.getBytes());
        } catch (Exception e) {
            LOGGER.error("MCP: Failed to send error response for id={}, originalMessage={}", id, message, e);
        }
    }
}
