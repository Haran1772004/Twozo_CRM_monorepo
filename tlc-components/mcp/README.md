# TLC MCP Module

An OSGi bundle that lets TLC product bundles register tools, resources, and prompt templates and exposes them over the Model Context Protocol (JSON-RPC 2.0) via a bundled HTTP transport. Any TLC application with `mcp` in `services.enabled` gets a working MCP server automatically — no product-side REST handler required.

---

## Starting the MCP server

The MCP server is not a separate process. It starts automatically as part of the host TLC application.

For TLC Flow (local dev):

```bash
./run-dev-start.sh
```

The MCP endpoint is then live at:

```
POST http://localhost:8182/rest/api/mcp
Content-Type: application/json
```

Verify it is running:

```bash
curl -s -X POST http://localhost:8182/rest/api/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}' | jq .
```

Expected response:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocolVersion": "2025-11-25",
    "capabilities": { "tools": {}, "resources": {}, "prompts": {} },
    "serverInfo": { "name": "tlc-mcp-server", "version": "1.0.0" }
  }
}
```

---

## Configuration

All keys live under the `mcp` namespace in `application.yml`.

```yaml
tlc:
  config:
    mcp:
      execution:
        max-concurrent-per-node: 50   # node-level concurrency cap (default 50)
        hard-timeout-seconds: 300     # hard ceiling for all tools (default 300)

      executors:
        - name: default
          threads: 10
          queue-capacity: 200
        - name: heavy
          threads: 4
          queue-capacity: 50

    services:
      enabled:
        - mcp                         # enables the MCP service
```

Assign a tool to a specific executor group with `@McpTool(executor = "heavy")`.

---

## Accessing the service

```java
McpService mcp = McpServiceProvider.get();
```

`McpServiceProvider` is the single product-facing entry point, following the same pattern as `CacheServiceProvider` and `I18nServiceProvider`.

---

## Quick start

### 1. Implement a tool

```java
@McpTool(name = "search_contacts", description = "Searches the CRM contact list")
public class SearchContactsTool extends AbstractMcpTool<MyIdentity> {

    @Override
    public boolean isAuthorized(final McpToolContext<MyIdentity> ctx) {
        return ctx.identity() != null;
    }

    public McpToolResult execute(
            @ToolParam(description = "Search term", minLength = 1) String query,
            @ToolParam(description = "Max results", defaultValue = "20", min = "1", max = "100") int limit,
            final McpToolContext<MyIdentity> ctx) {

        final long orgId = ctx.identity().orgId();   // no cast — type-safe
        final List<Contact> contacts = ContactService.search(orgId, query, limit);

        final JsonArray results = Json.array();
        contacts.forEach(c -> results.put(Json.object()
                .put("id", c.id())
                .put("name", c.name())
                .put("email", c.email())));
        return McpToolResult.json(results);
    }
}
```

### 2. Implement a resource

```java
@McpResourceDef(uri = "crm://contacts/schema", name = "contacts-schema", mimeType = "application/json")
public class ContactsSchemaResource implements McpResource {

    @Override
    public McpToolResult read(final McpToolContext<Object> ctx) {
        final JsonObject schema = Json.object()
                .put("type", "object")
                .put("description", "CRM contact schema");
        return McpToolResult.json(schema);
    }
}
```

### 3. Implement a prompt

```java
@McpPromptDef(name = "draft-welcome-email", description = "Drafts a welcome email for a new contact")
public class WelcomeEmailPrompt implements McpPrompt {

    @Override
    public McpToolResult get(final Map<String, String> args, final McpToolContext<Object> ctx) {
        final String name = args.getOrDefault("name", "customer");
        return McpToolResult.text("Draft a warm welcome email to " + name + "...");
    }
}
```

### 4. Auto-discovery

Classes are discovered automatically when the server starts, provided they carry the right annotation (`@McpTool`, `@McpResourceDef`, `@McpPromptDef`) and live anywhere under the `com.tlc` package root. No manual registration is needed.

---

## HTTP transport

The MCP module ships a bundled HTTP transport — `McpHttpTransport` — that auto-registers with the TLC web layer.

```
POST /rest/api/mcp     ← JSON-RPC 2.0 endpoint
```

### Supported JSON-RPC methods

| Method | Description |
|---|---|
| `initialize` | MCP handshake — returns protocol version and capabilities |
| `tools/list` | Lists all registered tools with their input schemas |
| `tools/call` | Invokes a tool by name |
| `resources/list` | Lists all registered resources |
| `resources/read` | Reads a resource by URI |
| `prompts/list` | Lists all registered prompt templates |
| `prompts/get` | Retrieves a rendered prompt template |
| `notifications/initialized` | Acknowledgement notification — handled as no-op |
| `ping` | Liveness check — returns `{}` |

### Connecting Claude Code

Add to `.mcp.json` in your project root, or `~/.claude.json` for global access:

```json
{
  "mcpServers": {
    "tlc-app": {
      "type": "http",
      "url": "http://localhost:8182/rest/api/mcp"
    }
  }
}
```

Restart Claude Code after saving. Run `/mcp` to confirm the server appears as connected and tools are listed.

---

## Authentication / identity

By default the transport has no authentication — `initialize`, `tools/list`, `resources/list`, and `prompts/list` work without any session. For `tools/call`, `resources/read`, and `prompts/get` the identity is resolved before dispatch.

Identity resolution is pluggable via two pieces:

### McpIdentityExtractor

A generic, async SPI called once per identity-bearing request. `I` is the application-specific
identity type (e.g. a record holding `orgId` and `userId`):

```java
@FunctionalInterface
public interface McpIdentityExtractor<I> {
    void extract(WebExchange exchange, Consumer<I> callback);
}
```

`callback` must be invoked **exactly once** — with the resolved identity, or `null` if the
request carries no valid session. Failing to call it leaves the HTTP exchange permanently open.

The callback fires on the I/O thread; the framework dispatches to the worker pool *inside*
the callback, so the I/O thread is never blocked.

### @McpIdentityProvider

Annotate your implementation and the MCP initializer discovers and wires it automatically at startup via classpath scan — no startup code needed. Exactly one `@McpIdentityProvider` class should exist per application.

```java
@McpIdentityProvider
public class JwtMcpIdentityExtractor implements McpIdentityExtractor<MyIdentity> {

    @Override
    public void extract(final WebExchange exchange, final Consumer<MyIdentity> callback) {
        final HttpCookie tokenCookie = exchange.cookie("accessToken");
        if (tokenCookie == null) {
            callback.accept(null);
            return;
        }
        TokenResolver.get().resolve(
            new AuthRequest(tokenCookie, exchange.ipAddress()),
            new EventCallback<User>() {
                @Override
                public void onSuccess(final User user) {
                    callback.accept(user != null ? new MyIdentity(user.orgId(), user.id()) : null);
                }
                @Override
                public void onFailure(final Throwable t) {
                    callback.accept(null);
                }
            });
    }
}
```

The resolved identity is forwarded into every `invokeTool`, `readResource`, and `invokePrompt`
call. Tools access it type-safely via `McpToolContext<I>.identity()` — no cast needed:

```java
// Tool extending AbstractMcpTool<MyIdentity>
public McpToolResult execute(final String query, final McpToolContext<MyIdentity> ctx) {
    final long orgId = ctx.identity().orgId();   // direct field access — no instanceof
    // ...
}
```

Guards live in `isAuthorized()`, not in `execute()`:

```java
@Override
public boolean isAuthorized(final McpToolContext<MyIdentity> ctx) {
    return ctx.identity() != null;
}
```

---

## Programmatic invocation

```java
McpService mcp = McpServiceProvider.get();

// invoke a tool with caller identity (propagated into McpToolContext)
McpToolResult result = mcp.invokeTool("search_contacts",
        Map.of("query", "Smith", "limit", 10),
        currentUser);

// invoke without identity
McpToolResult result = mcp.invokeTool("search_contacts",
        Map.of("query", "Smith"));

// read a resource
McpToolResult schema = mcp.readResource("crm://contacts/schema", currentUser);

// invoke a prompt template
McpToolResult prompt = mcp.invokePrompt("draft-welcome-email",
        Map.of("name", "Alice"), currentUser);
```

The full pipeline (auth layer, validation layer, execution layer) runs on every call.

---

## Registry query APIs

```java
McpService mcp = McpServiceProvider.get();

List<McpToolInfo>     tools     = mcp.tools();
List<McpResourceInfo> resources = mcp.resources();
List<McpPromptInfo>   prompts   = mcp.prompts();

Optional<McpToolInfo>     tool     = mcp.tool("search_contacts");
Optional<McpResourceInfo> resource = mcp.resource("crm://contacts/schema");
Optional<McpPromptInfo>   prompt   = mcp.prompt("draft-welcome-email");
```

`McpToolInfo`, `McpResourceInfo`, and `McpPromptInfo` are immutable records — safe to cache or serialize.

---

## Monitoring

```java
McpMonitor monitor = McpServiceProvider.get().getMonitor();

monitor.totalAvailableSlots();   // node-level concurrency headroom
monitor.totalActiveCount();      // in-flight tool calls right now
monitor.executorStats();         // per-executor-group active/queue counts
monitor.tenantUsage();           // per-tenant active count (if populated)
```

---

## Concepts

### Tools

Tools are executable functions invoked via `invokeTool()`.

| Annotation attribute | Purpose |
|---|---|
| `name` | Unique tool identifier |
| `description` | Describes what the tool does |
| `version` | Numeric string; defaults to `"1"`. Higher versions take precedence. |
| `mode` | `SYNC` (default) blocks until done; `ASYNC` returns a `taskId` immediately |
| `readOnly` | Hint that the tool does not modify state |
| `deprecated` | Marks the tool as deprecated in the registry |
| `timeoutSeconds` | Per-tool timeout; capped at `execution.hard-timeout-seconds` |
| `executor` | Executor group name; defaults to `"default"` |

Override `isAuthorized(McpToolContext<I> ctx)` in your `AbstractMcpTool<I>` subclass to add role or tenant checks. Return `false` to deny.

**Parameter annotations (`@ToolParam`):**

| Attribute | Purpose |
|---|---|
| `description` | Included in the generated JSON Schema |
| `required` | Whether the field is required (default `true`) |
| `defaultValue` | Makes the field optional |
| `min` / `max` | Numeric range constraints |
| `minLength` / `maxLength` | String length constraints |
| `pattern` | Regex pattern constraint |
| `options` | Allowed string values (single-select enum) |
| `multiSelect` | When `true`, accepts an array of the declared options |

### Resources

Resources are read-only data sources identified by a URI, accessible via `resource(uri)` and readable via `readResource(uri)`.

Override `isAuthorized(McpToolContext<Object> ctx)` (default `true`) to restrict access. Resources receive `McpToolContext<Object>` because they are not typed at the framework level.

### Prompts

Prompt templates are accessible via `prompt(name)` and retrievable via `invokePrompt(name, args)`. Arguments are passed as `Map<String, String>`.

Override `isAuthorized(McpToolContext<Object> ctx)` (default `true`) to restrict access. Prompts receive `McpToolContext<Object>` — use `instanceof` to check the concrete type if needed.

### ASYNC execution

When `mode = ExecutionMode.ASYNC`, `execute()` runs in a background thread and `invokeTool()` returns immediately with a `taskId`:

```json
{"taskId": "550e8400-...", "status": "PENDING", "hint": "Call get_task_status with this taskId to retrieve the result."}
```

Poll status via the built-in tool:

```java
McpToolResult status = mcp.invokeTool("get_task_status", Map.of("taskId", "550e8400-..."));
```

States: `PENDING` → `RUNNING` → `COMPLETED` | `FAILED` | `TIMEOUT`.

### McpToolResult

`McpToolResult` is **final** — always use the factory methods:

| Factory method | When to use |
|---|---|
| `McpToolResult.text(String)` | Plain text or pre-serialized JSON string |
| `McpToolResult.json(JsonObject)` | Structured JSON object response |
| `McpToolResult.json(JsonArray)` | Structured JSON array response |
| `McpToolResult.image(byte[], mimeType)` | Base64-encoded image data |
| `McpToolResult.error(String)` | Tool-level error (sets `isError = true`) |
| `McpToolResult.trackingId(String)` | Used internally for ASYNC mode |

`McpToolResult.content()` returns `List<ContentItem>`; each item has `type`, `data`, and `mimeType`. Combine multiple items with `result.and(otherResult)`.

**JSON construction** uses `com.tlc.commons.json.Json`:

```java
JsonObject obj = Json.object().put("id", contact.id()).put("name", contact.name());
return McpToolResult.json(obj);

JsonArray arr = Json.array();
contacts.forEach(c -> arr.put(Json.object().put("id", c.id()).put("name", c.name())));
return McpToolResult.json(arr);
```

### Schema customisation

Implement `McpSchemaProvider` on your tool class to modify the auto-generated schema:

```java
@McpTool(name = "my_tool", description = "...")
public class MyTool extends AbstractMcpTool<MyIdentity> {

    @Override
    public SchemaDefinition inputSchema(final SchemaDefinition generated, final McpToolContext<MyIdentity> ctx) {
        return generated.withField(
            SchemaDefinition.FieldDef.builder("extra")
                .description("Extra field not on execute()")
                .required(false)
                .build()
        );
    }
}
```

`SchemaDefinition.fromJsonObject(JsonObject)` parses a JSON Schema object into a `SchemaDefinition` — useful when a schema arrives over the wire or from config.

---

## Package layout

```
com.tlc.mcp                        ← public API (OSGi exported)
  McpService                       ← product-facing service interface

com.tlc.mcp.provider               ← service access (OSGi exported)
  McpServiceProvider               ← McpServiceProvider.get() — product entry point

com.tlc.mcp.action                 ← identity SPI (OSGi exported)
  McpIdentityExtractor<I>          ← @FunctionalInterface: extract(WebExchange, Consumer<I>); async
  @McpIdentityProvider             ← marks the application's extractor for auto-discovery

com.tlc.mcp.action.rest            ← bundled HTTP transport (OSGi exported)
  McpHttpTransport                 ← POST /rest/api/mcp; JSON-RPC 2.0 dispatch; async identity-aware

com.tlc.mcp.tool                   ← tool authoring API (OSGi exported)
  @McpTool, @ToolParam             ← annotations
  AbstractMcpTool<I>               ← generic base class; override isAuthorized(McpToolContext<I>)
  McpToolResult                    ← factory: text(), json(), image(), error(), trackingId()
  McpToolContext<I>                ← toolName, toolVersion, identity (typed as I)
  McpToolInfo                      ← immutable descriptor returned by McpService.tools()
  SchemaDefinition                 ← immutable schema; build/mutate/parse
  ExecutionMode                    ← SYNC | ASYNC

com.tlc.mcp.resource               ← resource authoring API (OSGi exported)
  McpResource, @McpResourceDef
  McpResourceInfo                  ← immutable descriptor

com.tlc.mcp.prompt                 ← prompt authoring API (OSGi exported)
  McpPrompt, @McpPromptDef
  McpPromptInfo                    ← immutable descriptor

com.tlc.mcp.executor               ← executor and monitoring API (OSGi exported)
  McpTaskHandle, TaskState         ← async task lifecycle
  McpMonitor, ExecutorStats        ← runtime metrics via McpService.getMonitor()

com.tlc.mcp.internal.*             ← implementation (OSGi private; do not import)
```

---

## Testing

```bash
mvn test -pl mcp
```

`McpHttpTransportTest` exercises the full JSON-RPC dispatch through `McpHttpTransport.handle()` with a lightweight proxy-based `WebExchange` stub — no running web server or database required.
