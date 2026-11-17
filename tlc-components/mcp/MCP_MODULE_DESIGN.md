# MCP Module — Design

Hand-rolled JSON-RPC 2.0 MCP server. No external MCP SDK — only `com.tlc:commons` and `com.tlc:web`.
Developers implement the public `com.tlc.mcp.*` API only; all internal types live in
`com.tlc.mcp.internal.*` and are not exported.

Protocol version targeted: **2025-11-25** (Streamable HTTP transport).

---

## MCP protocol overview

Understanding the protocol shapes every design decision in this module.

### Architecture

```
MCP Host (AI application, e.g. Claude Desktop, VS Code)
    │
    ├── MCP Client ──── dedicated connection ──── MCP Server (this module)
    └── MCP Client ──── dedicated connection ──── another MCP Server
```

- **Host**: the AI application that manages one or more clients.
- **Client**: one connection to one server; the host creates one per server.
- **Server (us)**: exposes tools, resources, and prompts over JSON-RPC 2.0.

MCP is stateful. A session starts with an `initialize` / `initialized` handshake where client
and server negotiate which capabilities (primitives) each side supports.

### Protocol layers

| Layer | Responsibility |
|-------|---------------|
| **Data layer** | JSON-RPC 2.0 message structure; lifecycle; primitives (tools, resources, prompts); notifications |
| **Transport layer** | Communication channel; connection establishment; message framing; authentication |

### Primitives the server exposes

| Primitive | Protocol methods | Description |
|-----------|-----------------|-------------|
| **Tools** | `tools/list`, `tools/call` | Executable functions the AI can invoke |
| **Resources** | `resources/list`, `resources/read` | Data sources / context the AI can read |
| **Prompts** | `prompts/list`, `prompts/get` | Reusable prompt templates |

### Primitives the client exposes (for server use)

| Primitive | Protocol method | Description |
|-----------|----------------|-------------|
| **Sampling** | `sampling/createMessage` | Server requests an LLM completion from the host |
| **Elicitation** | `elicitation/create` | Server requests additional input from the user |
| **Logging** | `logging/message` (notification) | Server sends log messages to the client |

### Capability negotiation

The `initialize` request / response declares supported primitives on both sides.
The server's `capabilities` response tells the client exactly what it can call:

```json
{
  "capabilities": {
    "tools":     { "listChanged": true },
    "resources": {},
    "prompts":   {}
  },
  "serverInfo": { "name": "tlc-mcp-server", "version": "1.0.0" },
  "protocolVersion": "2025-11-25"
}
```

`McpServerImpl` builds this response from the registered tool/resource/prompt sets at startup.

### Notifications

Real-time server-to-client updates sent as JSON-RPC notifications (no `id`, no response expected):

| Notification | When sent |
|---|---|
| `notifications/tools/list_changed` | A tool is added or removed at runtime |
| `notifications/resources/list_changed` | A resource is added or removed |
| `notifications/prompts/list_changed` | A prompt is added or removed |

---

## Why a separate module

- MCP protocol details must not leak into business modules.
- Lifecycle (start / stop) needs to be managed by the component-starter like every other service.
- Tool / resource / prompt registration must be automatic — developers annotate their classes, the
  starter scans and registers them (same pattern as `@WebAction` scanning in `WebInitializer`).
- Authorization, versioning, and schema validation are cross-cutting — handled centrally so
  individual tool authors do not need to re-implement them.

---

## Phase overview

| Phase | Scope | Status |
|---|---|---|
| **1** | Core Framework + SYNC Tools | **Current** |
| **2** | ASYNC Execution | **Current** |
| **3** | Resources and Prompts | **Current** |
| **4** | SSE and Push Notifications | **[FUTURE]** |
| **5** | Execution History (DB / S3) | **[FUTURE]** |

Each phase section ends with an exit criteria checklist that must be satisfied before the phase
is considered complete and before any subsequent phase begins.

---

---

# Phase 1 — Core Framework + SYNC Tools

## Scope

Phase 1 delivers the foundational framework: the OSGi bundle, tool annotation API, reflection-based
schema generation, runtime validation, the execution pipeline, the concurrency guard, timeout
enforcement, observability, and tool versioning. Both synchronous (`ExecutionMode.SYNC`) and
asynchronous (`ExecutionMode.ASYNC`) tool invocation are in scope.
The bundled `McpHttpTransport` is shipped as part of the framework (`com.tlc:web` provided scope).

---

## Module layout

```
mcp/
│
├── MCP_MODULE_DESIGN.md                    ← .md  this file
├── readme.md                               ← .md  configuration guide (application.yml, transport options)
├── pom.xml                                 ← build descriptor (OSGi bundle packaging)
│
└── src/main/java/com/tlc/mcp/
    │
    ├── McpAccess.java                      ← .java  BundleActivator (OSGi path); start() constructs
    │                                                 McpServerImpl, registers McpBundleListener;
    │                                                 stop() removes listener and shuts down McpServerImpl
    ├── McpService.java                     ← .java  public interface extends Service; sync
    │                                                 invocation API; getMonitor() → McpMonitor
    │
    ├── tool/
    │   ├── AbstractMcpTool.java            ← .java  public abstract class<I> — sole base for all tools;
    │   │                                             isVisible(McpToolContext<I>) listing filter default true;
    │   │                                             isAuthorized(McpToolContext<I>) execution gate default true;
    │   │                                             inputSchema(generated, McpToolContext<I>) default pass-through;
    │   │                                             unauthorized() helper;
    │   │                                             checkVisible/checkAuthorized/resolveSchema bridge methods
    │   │                                             (public final, accept McpToolContext<?>) for internal pipeline
    │   ├── McpTool.java                    ← .java  public @interface — marks a class as an MCP tool;
    │   │                                             name, title, description, version, deprecated;
    │   │                                             ToolAnnotations hints: readOnly, destructive, idempotent;
    │   │                                             executor (group name), timeoutSeconds (soft limit),
    │   │                                             mode (SYNC | ASYNC)
    │   ├── ExecutionMode.java              ← .java  public enum — SYNC (execute and return result directly);
    │   │                                             ASYNC (Phase 2 — submit to background, return trackingId)
    │   ├── SchemaDefinition.java           ← .java  public final class — fluent builder for JSON Schema;
    │   │                                             singleSelect / multiSelect / dynamic options support;
    │   │                                             dual purpose: schema generation + runtime validation;
    │   │                                             toJsonSchema() is package-private
    │   ├── OptionsProvider.java            ← .java  public interface — returns List<String> options at
    │   │                                             registration time; used for dynamic picklist values
    │   │                                             loaded from DB or service; None inner class default
    │   ├── ToolParam.java                  ← .java  public @interface — parameter-level metadata;
    │   │                                             description, defaultValue, required, min, max,
    │   │                                             minLength, maxLength, pattern, options, multiSelect,
    │   │                                             dynamicOptions; applied to execute() params and POJO fields
    │   ├── McpToolContext.java             ← .java  public final class<I> — identity() returns I;
    │   │                                             toolName(), toolVersion(); injectable as an optional
    │   │                                             execute() parameter when needed
    │   └── McpToolResult.java              ← .java  public class — text/image/resource/error factory methods;
    │                                                 trackingId(taskId) factory added in Phase 2 for ASYNC
    │
    ├── executor/                           ← all public; exported as com.tlc.mcp.executor
    │   ├── McpToolInvocation.java          ← .java  public final class — command object (Command pattern);
    │   │                                             toolName, toolVersion, args[], context, schema, timeoutMillis,
    │   │                                             mode, tenantId; immutable; decouples pipeline layers from
    │   │                                             raw JSON-RPC
    │   ├── ExecutorStats.java              ← .java  public final class — live executor metrics: availableSlots(),
    │   │                                             activeCount(), queueDepth(); static of() factory
    │   ├── McpTaskHandle.java              ← .java  public final class — ASYNC task result handle (Phase 2);
    │   │                                             taskId(), state(), result(), submittedAt(), completedAt()
    │   ├── McpPipelineLayer.java           ← .java  public interface — Chain of Responsibility SPI;
    │   │                                             process(McpToolInvocation, McpPipelineChain) → McpToolResult;
    │   │                                             short-circuit before execute() OR capture result after
    │   │                                             chain.next() for post-execution work
    │   ├── McpPipelineChain.java           ← .java  public interface — next link; next(McpToolInvocation) → McpToolResult
    │   └── McpMonitor.java                 ← .java  public interface — live observability API;
    │                                                 totalAvailableSlots(), totalActiveCount(),
    │                                                 executorStats() → Map<groupName, ExecutorStats>,
    │                                                 tenantUsage() → Map<tenantId, activeCount>
    │
    ├── resource/
    │   ├── McpResource.java                ← .java  public interface — Phase 3
    │   └── McpResourceDef.java             ← .java  public @interface — Phase 3
    │
    ├── prompt/
    │   ├── McpPrompt.java                  ← .java  public interface — Phase 3
    │   └── McpPromptDef.java               ← .java  public @interface — Phase 3
    │
    ├── action/                             ← exported as com.tlc.mcp.action
    │   ├── McpIdentityExtractor.java       ← .java  @FunctionalInterface<I>; async callback:
    │   │                                             void extract(WebExchange, Consumer<I>);
    │   │                                             callback fires on I/O thread; framework dispatches
    │   │                                             to worker pool inside the callback — never blocks
    │   ├── McpIdentityProvider.java        ← .java  @Target(TYPE) marker; annotated class must implement
    │   │                                             McpIdentityExtractor<I>; discovered at startup by McpInitializer
    │   └── rest/                           ← exported as com.tlc.mcp.action.rest
    │       └── McpHttpTransport.java       ← .java  @RestApiHandler(path="/mcp"); implements Action,
    │                                                 FullBytesCallback; stores McpIdentityExtractor<Object>
    │                                                 internally (generic setter does one suppressed cast);
    │                                                 async handle(): I/O thread → extract() → workerPool;
    │                                                 setIdentityExtractor(); setToolsResolver()
    │
    ├── provider/
    │   └── McpServiceProvider.java         ← .java  public class — static get() → McpServiceRegistry.get()
    │
    └── internal/                           ← everything below is private; never exported
        │
        ├── McpServerImpl.java              ← .java  implements McpService; no external SDK;
        │                                             builds JSON-RPC capability response from registries;
        │                                             [FUTURE Phase 4] notifyToolsChanged() will push SSE events
        │
        ├── registry/
        │   ├── McpServiceRegistry.java     ← .java  ServiceRegistry<McpService> singleton
        │   ├── McpToolVersionRegistry.java ← .java  Map<name, NavigableMap<version, McpTool>>
        │   │                                         NavigableMap gives O(log n) canonical resolution (highest non-deprecated);
        │   │                                         also tracks Map<bundleSymbolicName, List<ToolKey>>
        │   │                                         for bundle-scoped cleanup on stop
        │   └── McpSessionRegistry.java     ← .java  [FUTURE Phase 4] ConcurrentHashMap<sessionId, McpSession>
        │
        ├── executor/
        │   ├── McpConcurrencyGuard.java    ← .java  Semaphore(maxConcurrentPerNode); tryAcquire() non-blocking;
        │   │                                         rejects immediately if full; released in finally;
        │   │                                         protects JVM heap under burst load
        │   ├── McpExecutorRegistry.java    ← .java  McpConcurrencyGuard nodeGuard (Semaphore);
        │   │                                         ScheduledExecutorService timeoutScheduler;
        │   │                                         ExecutorService asyncExecutor (virtual threads, ASYNC tools only)
        │   ├── McpTaskRegistry.java        ← .java  ConcurrentHashMap<taskId, McpTaskStatus>;
        │   ├── McpToolPipeline.java        ← .java  links McpPipelineLayer instances in fixed order;
        │   │                                         built-in order: McpAuthLayer → McpValidationLayer → McpExecutionLayer
        │   ├── McpAuthLayer.java           ← .java  calls tool.isAuthorized(ctx); short-circuits on false
        │   ├── McpValidationLayer.java     ← .java  calls McpToolValidator; short-circuits on constraint violation
        │   └── McpExecutionLayer.java      ← .java  resolves timeout; acquires McpConcurrencyGuard;
        │                                             SYNC → inline execution on calling mcp-worker thread
        │                                               with interrupt watchdog (ScheduledFuture cancels if timed out);
        │                                             ASYNC → submits to asyncExecutor (virtual threads),
        │                                               stores McpTaskHandle, returns trackingId immediately
        │
        ├── tool/
        │   ├── McpPingTool.java            ← .java  built-in smoke-test tool (name: mcp_ping);
        │   │                                         echoes a message + timestamp + caller identity;
        │   │                                         auto-discovered via @McpTool + com.tlc.mcp.internal.tool package
        │   └── McpEchoTool.java            ← .java  built-in echo tool (name: mcp_echo);
        │                                             returns all arguments back as JSON; useful for integration testing
        │
        ├── registrar/
        │   └── McpToolRegistrar.java       ← .java  resolves versions; reflects execute() for schema generation;
        │                                             maps @McpTool flags to ToolAnnotations; wires pipeline;
        │                                             registers with McpToolVersionRegistry and McpServerImpl
        │
        ├── schema/
        │   ├── McpSchemaGenerator.java     ← .java  reflects on execute() method parameters;
        │   │                                         maps Java types to JSON Schema types; reads @ToolParam;
        │   │                                         produces SchemaDefinition (not raw JSON)
        │   └── McpToolValidator.java       ← .java  validates deserialized arguments against SchemaDefinition;
        │                                             recurses into POJOs and Map<String,Object>;
        │                                             collects all errors before returning
        │
        └── listener/
            └── McpBundleListener.java      ← .java  extends commons BundleListener; handles STARTED/STOPPED;
                                                      bundleStarted → annotation scan filtered by .tool/.resource/.prompt packages;
                                                      bundleStopped → McpToolVersionRegistry.removeByBundle()
```

---

## OSGi bundle configuration (pom.xml excerpt)

```xml
<plugin>
    <groupId>org.apache.felix</groupId>
    <artifactId>maven-bundle-plugin</artifactId>
    <configuration>
        <instructions>
            <Export-Package>
                com.tlc.mcp,
                com.tlc.mcp.tool,
                com.tlc.mcp.executor,
                com.tlc.mcp.resource,
                com.tlc.mcp.prompt,
                com.tlc.mcp.provider,
                com.tlc.mcp.action,
                com.tlc.mcp.action.rest
            </Export-Package>
            <Private-Package>com.tlc.mcp.internal.*</Private-Package>
            <Bundle-Activator>com.tlc.mcp.McpAccess</Bundle-Activator>
        </instructions>
    </configuration>
</plugin>
```

No third-party SDK embedded — only `com.tlc:commons` and `com.tlc:web` (provided scope).

---

## components-starter addition

```
components-starter/src/main/java/com/tlc/components/starter/initializer/components/
    └── McpInitializer.java                 ← .java  extends AbstractModuleInitializer
```

| Phase | What happens |
|-------|-------------|
| `initialize()` | Reads `mcp.*` config, constructs `McpServerImpl`, calls `server.start(config)`, creates mcp-worker pool from `mcp.worker.threads` (default 20) and wires it via `McpHttpTransport.setWorkerPool(pool)`, registers in `McpServiceRegistry`. |
| `postInitialize()` | `classScanner.findClassesWithAnnotation("com.tlc", McpTool.class)` filtered to packages containing `.tool` → `server.getToolRegistrar().register(MODULE, cls)` per class. Same pattern for `@McpResourceDef` (`.resource`) and `@McpPromptDef` (`.prompt`). Discovers and wires `@McpIdentityProvider` into `McpHttpTransport`. Wires `McpHttpTransport.setToolsResolver(server::toolsForIdentity)` so `tools/list` calls `toolsForIdentity(identity)` per request. |

---

## Product integration

The `mcp` module is a **framework only**. It exposes no HTTP endpoints, no REST APIs, and no MCP
server by default. Products own the MCP layer — the framework supplies the engine.

### Required — product MUST provide

```
1. Maven dependency on the mcp module
2. Enable in services configuration:
       services.enabled: [ mcp ]
3. Tool classes in *.mcp.tool.* package annotated with @McpTool
4. application.yml with mcp.worker.threads and mcp.execution.* configured
```

The HTTP transport (`POST /rest/api/mcp`) is bundled — no product-side transport class needed.

The HTTP transport is bundled (`McpHttpTransport`) — no `registerTransport()` call needed.

### Optional — product MAY do

```
- Define a product base class (e.g. CrmMcpTool extends AbstractMcpTool) for shared auth logic
- Override isVisible(ctx) on tools to filter visibility per identity (role-based tool list)
- Override inputSchema(generated, ctx) on tools for per-identity schema customisation
- Register McpPipelineLayer implementations (rate limiting, audit, circuit breaking)
```

### What the framework provides vs what the product supplies

| Framework provides | Product supplies |
|---|---|
| Tool registration, versioning, schema generation | Tool implementations in `*.mcp.tool.*` |
| Auth pipeline (`isAuthorized()` dispatch) | Product base class with shared auth logic |
| Schema validation before `execute()` | YAML configuration (`mcp.execution.*`, `mcp.worker.threads`) |
| mcp-worker pool (single shared pool, YAML-configured) | Pool size via `mcp.worker.threads` (default 20) |
| Bundled HTTP transport (`McpHttpTransport`) | Custom transport only if overriding default (optional) |
| `McpPipelineLayer` interception points | Pipeline layer implementations (optional) |
| Observability data via `McpMonitor` | How that data is exposed (REST, MCP tool, metrics bridge) |

### Observability data — product decides the surface

```java
McpMonitor monitor = McpServiceProvider.getMonitor();
monitor.totalAvailableSlots();   // node Semaphore available permits
monitor.totalActiveCount();      // currently executing across all groups
monitor.executorStats();         // node-level guard stats
monitor.tenantUsage();           // which tenant occupies most capacity
```

**As an MCP tool:**
```java
@McpTool(name = "get_executor_stats", description = "Query MCP execution health", readOnly = true)
public class ExecutorStatsTool extends CrmMcpTool {
    public McpToolResult execute() {
        return McpToolResult.json(McpServiceProvider.getMonitor().executorStats());
    }
}
```

**As a REST endpoint:**
```java
@ApiRoute(method = HttpMethod.GET, path = "/mcp/stats")
public void stats(WebExchange exchange) {
    exchange.sendJson(McpServiceProvider.getMonitor().executorStats());
}
```

---

## Transport — bundled HTTP transport (Phase 1 update, 2026-07-22)

> **Decision (2026-07-22):** The `mcp` module now ships a bundled HTTP transport:
> `com.tlc.mcp.action.rest.McpHttpTransport`. Products no longer need to write their
> own `@RestApiHandler`. The `web` module is now a `provided` dependency of `mcp`.
>
> The original "product-provided transport" model (via `McpTransport` SPI) is superseded
> for the HTTP case. Products can still override by registering a custom transport if needed.
>
> SSE (`GET /mcp`), session management, and `notifications/tools/list_changed` push remain
> **Phase 4 scope**. Do not implement without revisiting Phase 4 explicitly.

### Bundled transport — `McpHttpTransport`

`WebInitializer` (in `components-starter`) scans `com.tlc.*` for `@RestApiHandler` classes
whose class file path contains `/action/rest/`. `McpHttpTransport` is in
`com.tlc.mcp.action.rest` — it is discovered and registered automatically alongside all other
product REST handlers. **No change to `McpInitializer` is needed.**

```
POST /rest/api/mcp     ← JSON-RPC 2.0 endpoint (unauthenticated by default)
```

Claude Code config for any TLC app with `mcp: true`:

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

Identity is resolved from session credentials (cookie / `Authorization: Bearer`) inside the
`McpIdentityExtractor<I>` — never from `X-Org-Id` or `X-User-Id` headers.

### Authentication posture

`McpHttpTransport` resolves identity asynchronously via `McpIdentityExtractor<I>`.
The field stores `McpIdentityExtractor<Object>` internally; the generic `setIdentityExtractor<I>()`
setter does a single `@SuppressWarnings("unchecked")` cast so all other code stays type-safe.

Identity source is always from the session/token on the exchange — not from request headers.

### Dispatch model — target architecture

```
Undertow I/O thread
    │
    ├── receiveFullBytes callback fires
    │
    ├── [listing methods: initialize, ping, tools/list, resources/list, prompts/list]
    │       └── workerPool.execute( in-memory lookup → sendResponse() )
    │
    └── [identity-bearing methods: tools/call, resources/read, prompts/get]
            │
            └── IDENTITY_EXTRACTOR.extract(exchange, identity -> {
                        // callback fires on I/O thread
                        workerPool.execute(() -> {
                            //
                            // MCP Worker Pool thread
                            //
                            ├── McpAuthLayer.isAuthorized()     ← inline
                            ├── McpValidationLayer.validate()   ← inline
                            ├── McpExecutionLayer — tool.execute(args, ctx)
                            │       └── timeout watchdog: interrupt if exceeded
                            └── sendResponse()
                        });
                    });
                    // I/O thread returns immediately — never blocks for identity resolution
```

### Executor ownership

| Executor | Pool name | YAML key | Owner | Lifecycle |
|---|---|---|---|---|
| MCP worker pool | `mcp-worker` | `mcp.worker.threads` | `McpHttpTransport` (static field) | created at `McpInitializer.initialize()` / `McpAccess.start()`; shut down at stop |

Single pool. `McpExecutorRegistry` with named per-tool groups is removed — unnecessary complexity.
`McpConcurrencyGuard` semaphore provides the explicit concurrency bound (set to `mcp.worker.threads`).

### Dispatch table

| JSON-RPC method | Thread | Handler |
|---|---|---|
| `initialize` | mcp-worker — in-memory | (local) |
| `notifications/initialized`, `ping` | mcp-worker — in-memory | (local) |
| `resources/list` | mcp-worker — in-memory | `mcp.resources()` |
| `prompts/list` | mcp-worker — in-memory | `mcp.prompts()` |
| `tools/list` | mcp-worker — identity path | `TOOLS_RESOLVER.apply(identity)` → `server.toolsForIdentity(identity)` |
| `tools/call` | mcp-worker — inline | `mcp.invokeTool(name, args, identity)` |
| `resources/read` | mcp-worker — inline | `mcp.readResource(uri, identity)` |
| `prompts/get` | mcp-worker — inline | `mcp.invokePrompt(name, args, identity)` |

| Method | Path | Purpose | Phase |
|--------|------|---------|-------|
| `POST` | `/mcp` | JSON-RPC requests; `application/json` response | **1–3 — Current** |
| `GET` | `/mcp` | Persistent SSE stream for server-to-client push | **4 — [FUTURE]** |
| `DELETE` | `/mcp` | Explicit session termination | **4 — [FUTURE]** |

### Per-identity tool list — `TOOLS_RESOLVER`

`tools/list` goes through the identity path (same as `tools/call`). The identity extractor runs first,
then `TOOLS_RESOLVER.apply(identity)` is called. The resolver is wired at startup to `server::toolsForIdentity`.

`McpServerImpl.toolsForIdentity(Object identity)`:
1. Always includes the built-in `get_task_status` tool.
2. For every registered canonical tool: calls `tool.isVisible(ctx{identity})` — skip if false.
3. For each visible tool: calls `tool.inputSchema(baseSchema, ctx{identity})` — returns the identity-specific schema.
4. Returns an unmodifiable list of `McpToolInfo` with the filtered and schema-customised tools.

This means the tool list and the input schema shown to an AI client are both identity-aware.
An admin user sees more tools and more fields than an anonymous caller — with no product-side routing logic.

The static `TOOLS_RESOLVER` field (same pattern as `IDENTITY_EXTRACTOR`) is:
- `Function<Object, List<McpToolInfo>>` — default is `identity -> List.of()`
- Wired at `postInitialize()` / `McpAccess.start()` to `server::toolsForIdentity`

### Versioning — `@ApiVersion` (header-based)

The web module's `@ApiVersion` / `X-API-Version` convention is adopted directly — no new pattern:

| Token | Where | Controls |
|-------|-------|---------|
| `X-API-Version: 1` | Request header | TLC MCP wrapper API version |
| `MCP-Protocol-Version: 2025-11-25` | Request header | MCP spec wire protocol version |
| `@McpTool.version` | Annotation | Individual tool behaviour version |

### Security — authentication and identity

`McpEndpoint` uses the existing bearer token authentication already supported by all CRM REST
endpoints. By the time `isAuthorized()` is called, `SecurityContextHolder` is already loaded:

```
McpEndpoint.post()
    │
    ▼
existing CRM bearer auth (same as all REST endpoints)
    │── token resolved → CrmUser
    │── failure → 401 Unauthorized
    └── success
            │
            ▼
        SecurityContext.load(crmUser)    ← SecurityContextHolder populated on this thread
            │
            ▼
        dispatch JSON-RPC to McpServerImpl
            │── tool.isAuthorized(ctx)  ← SecurityContextHolder already attached
            │── tool.execute(params) ← SecurityContextHolder still attached
```

### Security — Origin header validation

`McpEndpoint` validates the `Origin` header on every request (DNS rebinding protection):
- `Origin` present but not in `mcp.security.allowed-origins` → `403 Forbidden`.
- Checked before JWT auth — no token processing on disallowed origins.

### Required request headers

```
Accept: application/json
MCP-Protocol-Version: 2025-11-25
```

---

## Server lifecycle

`McpServerImpl` starts immediately ready to serve requests — there is no state machine in the current implementation. Boot sequence:

```
McpAccess.start() / McpInitializer.initialize()
    │── constructs McpServerImpl
    │── server.start(configurations)          ← initialises McpExecutorRegistry (nodeGuard, timeoutScheduler, asyncExecutor)
    │── creates mcp-worker pool from mcp.worker.threads
    │── McpHttpTransport.setWorkerPool(pool)
    │── McpServiceRegistry.register()
    │
McpInitializer.postInitialize()
    │── annotation scan + package filter
    │── tools / resources / prompts registered
    │── identity extractor wired
    │── McpHttpTransport.setToolsResolver(server::toolsForIdentity)
    │
Server ready — McpHttpTransport begins accepting requests

McpAccess.stop() / McpInitializer @PreDestroy
    │── McpHttpTransport.shutdownWorkerPool()
    │── McpServiceRegistry.unregister()
    │── service.stop()                        ← shuts down McpExecutorRegistry + McpTaskRegistry
```

### [FUTURE] Lifecycle state machine

A `ServerState` enum (`INITIALIZING → READY → STOPPING`) with per-state request behaviour
(503 during init, drain window on shutdown) is planned but not yet implemented.

---

## Runtime tool discovery

### Package convention

Discovery is annotation-driven but gated by package name convention:

| Annotation | Package must contain | Example |
|---|---|---|
| `@McpTool` | `.tool` | `com.tlc.crm.tool`, `com.tlc.mcp.internal.tool` |
| `@McpResourceDef` | `.resource` | `com.tlc.crm.resource` |
| `@McpPromptDef` | `.prompt` | `com.tlc.crm.prompt` |

A class annotated with `@McpTool` but living outside a `.tool` package is ignored. This prevents accidental registration of classes in unrelated packages that happen to carry the annotation.

### Startup scan (static)

`McpInitializer.postInitialize()` calls `classScanner.findClassesWithAnnotation("com.tlc", McpTool.class)`
then filters by `cls.getPackageName().contains(".tool")`. Runs before any client connects —
the initial `tools/list` response is complete at startup.

### Runtime scan (dynamic — OSGi bundle events)

`McpBundleListener` is registered with `BundleContext` in `McpAccess.start()`:

#### New bundle installed and started

```
BundleEvent.STARTED  (only com.tlc.* bundles, de-duped by bundle ID)
    │
    ▼
McpBundleListener.bundleStarted(bundle)
    │
    ▼
findClassesWithAnnotation(bundle, McpTool.class) filtered to .tool packages
    │
    ▼
McpToolRegistrar.register(bundle.getSymbolicName(), cls)  ← called per class
    │   ├─ resolves versions (canonical + _v{n} aliases)
    │   ├─ generates inputSchema via McpSchemaGenerator
    │   ├─ wires isAuthorized() → execute() pipeline
    │   └─ registers with McpToolVersionRegistry + McpServerImpl
    │
    ▼
McpServerImpl.notifyToolsChanged()
    │   Phase 1: no-op (clients discover on next tools/list call)
    │   [FUTURE Phase 4]: pushes notifications/tools/list_changed to SSE streams
```

#### Bundle stopped (tool removed)

```
BundleEvent.STOPPED
    │
    ▼
McpBundleListener.bundleStopped(bundle)
    │
    ▼
McpToolVersionRegistry.removeByBundle(bundle.getSymbolicName())
    │   returns: List<ToolKey> that were actually removed
    │
    ▼
if list is non-empty:
    McpServerImpl.unregisterTools(removedToolKeys)
    McpServerImpl.notifyToolsChanged()
```

#### Bundle updated

An OSGi bundle update is a stop followed by a start — fires both events in sequence.
There is a brief window where the tool is absent between STOPPED and STARTED.

### `McpToolVersionRegistry` — data structures

```java
// version registry (query / resolution)
Map<String name, NavigableMap<String version, McpTool>> toolVersions;
// NavigableMap → lastKey() = canonical (highest); floorKey(v) = version resolution; O(log n)

// bundle ownership index (cleanup)
Map<String bundleSymbolicName, List<ToolKey>> bundleToolIndex;
```

`ToolKey` is a value type: `(String name, String version)`.
`removeByBundle(symbolicName)` atomically removes from both maps and returns the evicted keys.

**Why `bundleSymbolicName` and not `bundleId`**: Bundle IDs change across restarts. Symbolic
names are stable identifiers declared in the bundle manifest (`Bundle-SymbolicName`).

---

## Authorization

### Design intent

- Every tool invocation passes through a mandatory `isAuthorized()` check before `execute()`.
- Enforced by `McpToolRegistrar` — tool authors cannot bypass it.
- Authorization logic lives in a product-level abstract base class, not per tool.
- Individual tools can add extra checks by overriding `isAuthorized()` and calling `super`.

### Class hierarchy (Template Method pattern)

```
AbstractMcpTool<I>  (abstract class, mcp module)
│   isVisible(McpToolContext<I>)   : boolean           ← default true
│   isAuthorized(McpToolContext<I>): boolean           ← default true
│   inputSchema(generated, McpToolContext<I>)          ← default pass-through
│   unauthorized() : McpToolResult                    ← helper
│   checkVisible/checkAuthorized/resolveSchema         ← public final bridge methods;
│     accept McpToolContext<?> and cast to <I> internally (one @SuppressWarnings per method)
│
└── CrmMcpTool  (abstract class<CrmIdentity>, product module e.g. tlc-crm-server)
        @Override isAuthorized(McpToolContext<CrmIdentity> ctx) → ctx.identity() != null
        │
        ├── SearchContactsTool   (concrete — just implements execute(McpToolContext<CrmIdentity>))
        └── DeleteContactTool    (concrete — overrides isAuthorized() for admin check)
```

### Invocation pipeline

```
McpEndpoint.post() — JWTAuthenticator ran, SecurityContext.load(crmUser) already called
    │
    ▼
McpToolContext built  (inbound JSON arguments parsed)
    │
    ▼
tool.isAuthorized(ctx)  ────── false ──► McpToolResult.error("Unauthorized")
    │
   true
    │
    ▼
McpToolValidator.validate(args, schemaDefinition)
    │
    ├── invalid ──► McpToolResult.error("Validation failed: ...")
    │
   valid
    │
    ▼
ContextSnapshot.capture()
try (ContextScope = snapshot.attach())
    │
    ▼
tool.execute(args)
    │
    ▼
McpToolResult → converted to SDK content array → returned to client
```

### `AbstractMcpTool<I>`

```java
public abstract class AbstractMcpTool<I> {

    /** Listing filter. Return false to hide this tool from tools/list for this caller.
     *  Does not protect against direct invocation — always pair with isAuthorized() for security. */
    public boolean isVisible(McpToolContext<I> ctx) { return true; }

    /** Execution gate. Return false to block tools/call for this caller. */
    public boolean isAuthorized(McpToolContext<I> ctx) { return true; }

    /** Per-identity schema. Return a modified SchemaDefinition to hide or add fields for this caller. */
    public SchemaDefinition inputSchema(SchemaDefinition generated, McpToolContext<I> ctx) { return generated; }

    protected final McpToolResult unauthorized() { return McpToolResult.error("Unauthorized"); }

    // ── Bridge methods for the internal pipeline (McpToolContext<?> wildcard) ──
    // Each does a single @SuppressWarnings("unchecked") cast. All other framework code is cast-free.

    @SuppressWarnings("unchecked")
    public final boolean checkVisible(McpToolContext<?> ctx) { return isVisible((McpToolContext<I>) ctx); }

    @SuppressWarnings("unchecked")
    public final boolean checkAuthorized(McpToolContext<?> ctx) { return isAuthorized((McpToolContext<I>) ctx); }

    @SuppressWarnings("unchecked")
    public final SchemaDefinition resolveSchema(SchemaDefinition base, McpToolContext<?> ctx) {
        return inputSchema(base, (McpToolContext<I>) ctx);
    }
}
```

`isVisible()` controls whether the tool appears in `tools/list` for a given identity.
`inputSchema()` allows per-identity schema customisation — hide fields, add admin-only params,
or load runtime-defined fields from DB. Both are evaluated at request time by `toolsForIdentity(identity)`.

The bridge methods (`checkVisible`, `checkAuthorized`, `resolveSchema`) are the single point where
the internal pipeline (which uses `McpToolContext<?>` wildcards) crosses into the typed product API.
All `@SuppressWarnings("unchecked")` casts live here and nowhere else.

### Product-level base (in the product, e.g. `tlc-crm-server`)

```java
public abstract class CrmMcpTool extends AbstractMcpTool<CrmIdentity> {

    @Override
    public boolean isAuthorized(final McpToolContext<CrmIdentity> ctx) {
        return ctx.identity() != null;   // extractor returns null when no valid session
    }
}
```

### Individual tool — extra auth

```java
@McpTool(name = "delete_contact", description = "Permanently delete a contact", destructive = true)
public class DeleteContactTool extends CrmMcpTool {

    @Override
    public boolean isAuthorized(final McpToolContext<CrmIdentity> ctx) {
        return super.isAuthorized(ctx) && ctx.identity().isAdmin();
    }

    public McpToolResult execute(
        @ToolParam(description = "ID of the contact to delete") long contactId,
        final McpToolContext<CrmIdentity> ctx
    ) {
        // no instanceof check — CrmIdentity is guaranteed here by isAuthorized()
        final long orgId = ctx.identity().orgId();
        ...
    }
}
```

---

## Tool API

### `@McpTool`

```java
public @interface McpTool {
    String name();                    // stable identifier used in tools/call
    String title()        default ""; // friendly display name shown in AI client UIs
    String description();             // what the tool does — the LLM reads this to decide when to invoke
    String version()      default "1";
    boolean deprecated()  default false;

    // MCP spec ToolAnnotations — behavioural hints for AI clients
    boolean readOnly()    default false; // tool does not modify any state
    boolean destructive() default false; // tool may perform irreversible changes
    boolean idempotent()  default false; // same args → same result if called multiple times

    // Execution framework
    String executor()           default "default";     // reserved; named groups are not active
    int    timeoutSeconds()     default -1;            // soft limit; -1 = use hard-timeout-seconds
    ExecutionMode mode()        default ExecutionMode.SYNC;
}
```

`ToolAnnotations` are **hints** — they let AI clients make smarter decisions (e.g. ask the user
before calling a `destructive` tool, safely retry an `idempotent` one).

| Hint | Typical CRM use |
|---|---|
| `readOnly = true` | `get_contact`, `search_contacts`, `list_deals` |
| `destructive = true` | `delete_contact`, `merge_accounts`, `bulk_delete` |
| `idempotent = true` | `search_contacts`, `get_contact` |

### `@ToolParam`

```java
public @interface ToolParam {
    String name()          default "";    // override inferred parameter name
    String description()   default "";
    boolean required()     default true;  // false = optional; independent of defaultValue
    String defaultValue()  default "";    // used in schema; does not control required status

    // Numeric constraints
    String min()           default "";
    String max()           default "";

    // String constraints
    int    minLength()     default -1;
    int    maxLength()     default -1;
    String pattern()       default "";

    // Picklist
    String[] options()     default {};
    boolean  multiSelect() default false;
    Class<? extends OptionsProvider> dynamicOptions() default OptionsProvider.None.class;
}
```

### `McpToolResult` — content types

```java
public final class McpToolResult {
    public static McpToolResult text(String text)
    public static McpToolResult json(JsonObject data)
    public static McpToolResult json(JsonArray data)
    public static McpToolResult image(byte[] data, String mime)
    public static McpToolResult error(String message)
    public static McpToolResult trackingId(String taskId)   // ASYNC initial response
    public McpToolResult and(McpToolResult other)            // append to content array
}
```

### `McpToolContext<I>` — optional injectable

Injected by the framework into `execute()` if declared as a parameter; skipped by
`McpSchemaGenerator` (not part of the schema). `I` is the application-specific identity type.

```java
public final class McpToolContext<I> {
    public I      identity()     // caller identity from McpIdentityExtractor<I>; null if not wired
    public String toolName()
    public String toolVersion()
}
```

Most tools do not need it:
```java
public McpToolResult execute(String query, int limit) { ... }

// Tool that needs caller identity — no cast, type-safe
public McpToolResult execute(String query, int limit, McpToolContext<CrmIdentity> ctx) {
    final long orgId = ctx.identity().orgId();   // direct access — no instanceof
    auditLog(orgId, "search_contacts", query);
    ...
}
```

**Internal pipeline:** uses `McpToolContext<?>` (wildcard). Bridge methods on `AbstractMcpTool<I>`
(`checkVisible`, `checkAuthorized`, `resolveSchema`) perform the cast internally so no other
framework class needs `@SuppressWarnings("unchecked")`.

---

## Schema generation

Schemas are derived automatically from the `execute()` method signature at startup via
reflection. No schema class to write, no JSON to maintain by hand.

### How `McpSchemaGenerator` works

```
@McpTool class found
    │
    ▼
reflect on execute() method
    │
    ▼
for each parameter:
    │── Java type  ──► JSON Schema type (see mapping table below)
    │── @ToolParam  ──► description, defaultValue, min, max, pattern
    │── no @ToolParam.defaultValue → parameter is required
    │── POJO type  ──► recurse into declared fields
    │── McpToolContext parameter → skipped (injected by framework)
    │── Map<String,Object> parameter → skipped (catch-all for dynamic fields)
    │
    ▼
McpSchemaGenerator.buildInputSchema(method) → SchemaDefinition
```

### Java type → JSON Schema mapping

| Java type | JSON Schema type | Notes |
|---|---|---|
| `String` | `"string"` | |
| `int`, `Integer`, `long`, `Long` | `"integer"` | |
| `double`, `Double`, `float`, `Float` | `"number"` | |
| `boolean`, `Boolean` | `"boolean"` | |
| `enum` subclass | `"string"` + `"enum": [...]` | values via `Enum.values()` |
| `List<T>`, `T[]` | `"array"` + `"items": {type of T}` | T resolved recursively |
| POJO | `"object"` + `"properties": {...}` | fields reflected recursively |
| `Map<String, Object>` | _(skipped)_ | catch-all for dynamic fields from `McpSchemaProvider` |
| `McpToolContext` | _(skipped)_ | injected by framework |

### POJO parameter support

```java
public class ContactSearchRequest {

    @ToolParam(description = "Name or email to search")
    private String query;

    @ToolParam(description = "Max results", defaultValue = "10", max = "100")
    private int limit;

    @ToolParam(description = "Filter by contact status")
    private ContactStatus status;   // enum — values extracted automatically
}

@McpTool(name = "search_contacts", description = "...", readOnly = true, idempotent = true)
public class SearchContactsTool extends CrmMcpTool {

    public McpToolResult execute(
        @ToolParam(description = "Search criteria") ContactSearchRequest request
    ) {
        contactService.search(request.getQuery(), request.getLimit(), request.getStatus());
        ...
    }
}
```

Nesting is supported — a POJO field that is itself a POJO is reflected recursively.
Circular references are detected and rejected at startup with a clear error.

### `required` propagation through nested objects

In JSON Schema, `required` is an array on the **parent** object.
`SchemaDefinition` handles this: `.required()` sets a flag on the child; when the parent
calls `.property(name, def)`, it checks `def.isRequired()` and adds `name` to its own
required list. This propagates correctly at every nesting level.

### Per-identity schema — `AbstractMcpTool.inputSchema()`

Override `inputSchema()` on the tool class to customise the schema shown to each caller.
The framework calls this at `tools/list` time (not just at startup) so the schema is always
fresh for the requesting identity.

**Use case: hide a field from unauthenticated callers**

```java
@McpTool(name = "search_contacts", description = "Search CRM contacts", readOnly = true)
public class SearchContactsTool extends CrmMcpTool {

    @Override
    public SchemaDefinition inputSchema(final SchemaDefinition generated, final McpToolContext<CrmIdentity> ctx) {
        if (ctx.identity() == null) {
            return generated.withoutField("internalNotes");
        }
        return generated;
    }

    public McpToolResult execute(
        @ToolParam(description = "Search query") String query,
        @ToolParam(description = "Internal use only", required = false) String internalNotes
    ) { ... }
}
```

**Use case: expose admin-only parameter**

```java
@Override
public SchemaDefinition inputSchema(final SchemaDefinition generated, final McpToolContext<CrmIdentity> ctx) {
    if (ctx.identity() != null && ctx.identity().isAdmin()) {
        return generated.withField(FieldDef.builder("forceDelete")
                .type("boolean").description("Skip recycle bin").required(false).build());
    }
    return generated;
}
```

**Use case: CRM custom fields from DB**

```java
@McpTool(name = "create_contact", description = "Create a new CRM contact")
public class CreateContactTool extends CrmMcpTool {

    @Override
    public SchemaDefinition inputSchema(final SchemaDefinition generated, final McpToolContext<CrmIdentity> ctx) {
        SchemaDefinition schema = generated;
        for (final CustomField field : CustomFieldService.get().getFieldsForIdentity(ctx.identity())) {
            schema = schema.withField(toFieldDef(field));
        }
        return schema;
    }

    public McpToolResult execute(
        @ToolParam(description = "First name")    String firstName,
        @ToolParam(description = "Last name")     String lastName,
        @ToolParam(description = "Email address") String email,
        Map<String, Object> customFields
    ) {
        contactService.create(firstName, lastName, email, customFields);
        return McpToolResult.text("Contact created");
    }
}
```

### `ToolRegistration` — registration-time schema cache

At registration, the framework calls `tool.inputSchema(baseSchema, ctx{null})` once to compute
a `inputSchemaJson` for the no-identity path. Per-identity schemas are always computed live at
`tools/list` time.

| Field | Value |
|---|---|
| `baseSchema` | Reflection-generated `SchemaDefinition` (pre-customisation) |
| `inputSchemaJson` | JSON string from `tool.inputSchema(base, null-identity-ctx)` at registration |
| `toolInstance` | `AbstractMcpTool<?>` instance — pipeline calls bridge methods (`checkVisible`, `resolveSchema`) |

### `SchemaDefinition` fluent builder

```java
public final class SchemaDefinition {

    // primitive type factories
    public static SchemaDefinition object()
    public static SchemaDefinition string()
    public static SchemaDefinition integer()
    public static SchemaDefinition number()
    public static SchemaDefinition bool()
    public static SchemaDefinition array(SchemaDefinition items)

    // picklist type factories
    public static SchemaDefinition singleSelect(String... options)
    public static SchemaDefinition singleSelect(OptionsProvider provider)
    public static SchemaDefinition multiSelect(String... options)
    public static SchemaDefinition multiSelect(OptionsProvider provider)

    // fluent modifiers (all return `this`)
    public SchemaDefinition description(String desc)
    public SchemaDefinition required()
    public SchemaDefinition defaultValue(Object val)
    public SchemaDefinition min(Number val)
    public SchemaDefinition max(Number val)
    public SchemaDefinition minLength(int len)
    public SchemaDefinition maxLength(int len)
    public SchemaDefinition pattern(String regex)
    public SchemaDefinition property(String name, SchemaDefinition def)

    // package-private
    String toJsonSchema()
}
```

**Constraint → JSON Schema mapping:**

| `SchemaDefinition` | Generated JSON Schema |
|---|---|
| `string()` | `{"type":"string"}` |
| `string().minLength(2).maxLength(50)` | `{"type":"string","minLength":2,"maxLength":50}` |
| `string().pattern("^[A-Z]{2}$")` | `{"type":"string","pattern":"^[A-Z]{2}$"}` |
| `integer().min(1).max(100).defaultValue(10)` | `{"type":"integer","minimum":1,"maximum":100,"default":10}` |
| `singleSelect("A","B","C")` | `{"type":"string","enum":["A","B","C"]}` |
| `multiSelect("A","B","C")` | `{"type":"array","items":{"type":"string","enum":["A","B","C"]},"uniqueItems":true}` |
| `singleSelect(StatusProvider.class)` | options resolved at registration → same JSON as static |

### `OptionsProvider`

```java
public interface OptionsProvider {

    List<String> getOptions();

    final class None implements OptionsProvider {
        @Override public List<String> getOptions() { return List.of(); }
    }
}
```

Called at tool registration time (not per request). If options change at runtime, re-register
the tool (e.g., via bundle restart) to pick up new values.

---

## Runtime validation — `McpToolValidator`

`SchemaDefinition` serves dual purpose: generates the `inputSchema` shown in `tools/list` AND
drives runtime validation in `tools/call`. Same constraints apply in both directions.

Runs **after `isAuthorized()` and before `execute()`**. Validation failures short-circuit to
`McpToolResult.error()` without calling `execute()`.

**What is validated:**

| Constraint | Applies to | Source |
|---|---|---|
| required (no `defaultValue`) | all types | `@ToolParam` / `SchemaDefinition` |
| `min` / `max` | `integer`, `number` | `@ToolParam` / `SchemaDefinition` |
| `minLength` / `maxLength` | `String` | `@ToolParam` / `SchemaDefinition` |
| `pattern` | `String` | `@ToolParam` / `SchemaDefinition` |
| single-select options | `String` | `@ToolParam.options` or `singleSelect()` |
| multi-select options | `List<String>` | `@ToolParam.options + multiSelect=true` or `multiSelect()` |
| dynamic options | `String` / `List<String>` | `@ToolParam.dynamicOptions` or `singleSelect/multiSelect(OptionsProvider)` |
| POJO field constraints | nested object fields | `@ToolParam` on POJO fields — recursed |
| `Map<String,Object>` custom fields | dynamic fields | `SchemaDefinition` from `McpSchemaProvider` |

**All constraint violations collected before returning** — LLM sees the full picture, not a
fail-fast on the first error.

---

## Execution pipeline — Chain of Responsibility

`McpPipelineLayer` is the **single extension point** for all cross-cutting concerns. The
`chain.next()` return value gives layers access to the result, enabling both pre- and
post-execution work in one abstraction.

```java
public interface McpPipelineLayer {
    McpToolResult process(McpToolInvocation invocation, McpPipelineChain chain);
}

public interface McpPipelineChain {
    McpToolResult next(McpToolInvocation invocation);
}
```

### Built-in layers (internal, always present, fixed order)

| Position | Layer class | What it does |
|---|---|---|
| 1 | `McpAuthLayer` | Calls `tool.isAuthorized(ctx)`; short-circuits on false |
| 2 | `McpValidationLayer` | Runs `McpToolValidator`; short-circuits on constraint violation |
| 3 | `McpExecutionLayer` | Resolves timeout; acquires `McpConcurrencyGuard`; SYNC inline on mcp-worker thread with interrupt watchdog; ASYNC submits to virtual-thread asyncExecutor |

### Product layer — pre-execution gate

```java
public class TenantRateLimitLayer implements McpPipelineLayer {

    @Override
    public McpToolResult process(final McpToolInvocation inv, final McpPipelineChain chain) {
        if (rateLimiter.isExceeded(inv.tenantId())) {
            return McpToolResult.error("Rate limit exceeded for tenant " + inv.tenantId());
        }
        return chain.next(inv);
    }
}
```

### Product layer — post-execution work

```java
public class AuditLayer implements McpPipelineLayer {

    @Override
    public McpToolResult process(final McpToolInvocation inv, final McpPipelineChain chain) {
        final long start = System.currentTimeMillis();
        final McpToolResult result = chain.next(inv);
        auditService.record(inv.toolName(), inv.tenantId(),
                result.isError(), System.currentTimeMillis() - start);
        return result;
    }
}
```

### Registration

The pipeline (`auth → validation → execution`) is fixed and internal to `McpServerImpl`. Custom layer insertion via a public API is not yet supported — planned for a future phase.

### Common layer use cases

| Use case | Pattern |
|---|---|
| Rate limiting, auth gate | Short-circuit before `chain.next()` |
| Audit logging | Capture result after `chain.next()`, record, return unchanged |
| Metrics | Measure duration around `chain.next()` |
| Cache invalidation | Inspect `inv.toolName()` + result after `chain.next()` |
| Circuit breaking | Short-circuit before `chain.next()` when open; reset on success |
| Result transformation | Replace / enrich `chain.next()` return value |

### Command object — `McpToolInvocation`

```java
public record McpToolInvocation(
    String               toolName,
    String               toolVersion,
    String               requestId,
    Map<String, Object>  arguments,
    McpToolContext<?>    context,      // wildcard — pipeline is identity-type-agnostic
    SchemaDefinition     schema,
    ExecutionMode        mode,
    long                 timeoutMillis,
    Object               tenantId
) {}
```

---

## Executor model

Single mcp-worker pool — no named per-tool executor groups. Every tool call runs inline on the
mcp-worker thread that received it. The `@McpTool.executor` field is reserved but currently
ignored; all tools share one pool.

| Pool | Name | YAML key | Owner | Lifecycle |
|---|---|---|---|---|
| mcp-worker | `mcp-worker` | `mcp.worker.threads` (default 20) | `McpHttpTransport` (static field) | created at `McpInitializer.initialize()` / `McpAccess.start()`; shut down at stop |
| async executor | virtual threads | (built-in) | `McpExecutorRegistry` | created at `server.start()`; shut down at `service.stop()` |

The async executor (virtual threads via `Executors.newVirtualThreadPerTaskExecutor()`) is used
only by ASYNC-mode tools. SYNC tools execute inline; no second pool is needed for them.

---

## Global concurrency guard — `McpConcurrencyGuard`

A node-level `Semaphore` caps total concurrent tool calls across all tools.
This is JVM heap protection — without it, a burst of requests can fill all group queues
and exhaust memory before any timeout fires.

```
Inbound tools/call
    │
    ▼
McpConcurrencyGuard.tryAcquire()   ← non-blocking; does NOT queue
    │
    ├── false (at capacity)
    │     → McpToolResult.error("MCP server at capacity. Try again shortly.")
    │       returned immediately; LLM retries on next turn
    │
    └── true (permit acquired)
          │
          ▼
        enter pipeline → auth → validate → executor dispatch
          │
          ▼ (always, in finally block)
        McpConcurrencyGuard.release()
```

**Back-pressure:**

| Level | Mechanism | Behaviour when full |
|---|---|---|
| Node-wide | `McpConcurrencyGuard` (`Semaphore`) | Reject immediately — no queue |

`McpMonitor.totalAvailableSlots()` = `guard.availablePermits()`.
`McpMonitor.totalActiveCount()` = `maxConcurrentPerNode - guard.availablePermits()`.

---

## Timeout — soft limit and hard limit

**Soft limit** — `@McpTool.timeoutSeconds`:
- `-1` (default) = use `hard-timeout-seconds`.
- When exceeded: current thread interrupted by watchdog; LLM sees `McpToolResult.error("Tool '...' timed out after Ns")`.

**Hard limit** — `mcp.execution.hard-timeout-seconds` in YAML:
- Global ceiling. No tool can exceed it regardless of its annotation.
- Resolved timeout: `min(@McpTool.timeoutSeconds, hard-timeout-seconds)`.

| `@McpTool.timeoutSeconds` | `hard-timeout-seconds` | Resolved |
|---|---|---|
| `30` (explicit) | `300` | `30s` — soft limit wins |
| `-1` | `300` | `300s` — hard limit applies |
| `600` | `300` | `300s` — hard limit wins |
| `-1` | `-1` (not set) | no timeout (not recommended) |

---

## Observability — `McpMonitor`

```java
public interface McpMonitor {
    int totalAvailableSlots();              // = guard.availablePermits()
    int totalActiveCount();                 // = maxConcurrentPerNode - availablePermits()
    Map<String, ExecutorStats> executorStats();
    Map<Object, Integer> tenantUsage();
}

public final class ExecutorStats {
    int availableSlots();    // poolSize - activeCount
    int activeCount();       // ThreadPoolExecutor.getActiveCount()
    int queueDepth();        // ThreadPoolExecutor.getQueue().size()
}
```

---

## Tool versioning

### `McpToolVersionRegistry`

```
Map<name, NavigableMap<version, McpTool>>
  "search_contacts" → { "1" → SearchContactsToolV1, "2" → SearchContactsToolV2 }
  "delete_contact"  → { "1" → DeleteContactTool }
```

`NavigableMap` gives O(log n) canonical resolution via `lastKey()` (highest non-deprecated version).

### Registration strategy

| Exposed tool name | Resolves to |
|---|---|
| `{name}` | Canonical — highest non-deprecated version |
| `{name}_v{version}` | Explicit version pin — every registered version |

Example:

| Exposed name | Points to |
|---|---|
| `search_contacts` | v2 (canonical) |
| `search_contacts_v1` | v1 (deprecated — description prefixed `[DEPRECATED]`) |
| `search_contacts_v2` | v2 (also accessible via explicit name) |

### Version resolution at startup

```
grouped by @McpTool.name()
for each group:
    sort versions ascending
    canonical = highest version where deprecated == false
                (fallback: highest deprecated version + WARN log)
    register canonical as {name}
    register each version as {name}_v{version}
```

---

## End-to-end operational flows

### Tool invocation — `tools/call` complete pipeline

```
Client (LLM host)          McpEndpoint       McpServerImpl    McpToolVersionRegistry   Tool
  │                             │                  │                   │                │
  │── POST /mcp ──────────────► │                  │                   │                │
  │   {"method":"tools/call",   │                  │                   │                │
  │    "params":{               │                  │                   │                │
  │      "name":"search_contacts",                 │                   │                │
  │      "arguments":{"query":"John","limit":5}}}  │                   │                │
  │                             │── handleToolCall►│                   │                │
  │                             │                  │── getCanonical()─►│                │
  │                             │                  │◄── tool ref ──────│                │
  │                             │                  │── build McpToolContext             │
  │                             │                  │── McpConcurrencyGuard.tryAcquire() │
  │                             │                  │── pipeline: auth → validate        │
  │                             │                  │── ContextSnapshot.capture()        │
  │                             │                  │── try(ContextScope=snapshot.attach())
  │                             │                  │── execute(args) ──────────────────►│
  │                             │                  │◄── McpToolResult.text("Found 2...")│
  │                             │                  │── ContextScope.close()             │
  │                             │                  │── McpConcurrencyGuard.release()    │
  │◄── 200 application/json ────│◄── ToolCallResult│                   │                │
```

### Tool invocation order

**The LLM decides the order.** Each `tools/call` is an independent POST. Tool authors must
not assume sequential ordering or shared per-turn state. Tools must be idempotent where
possible; they must not write shared mutable state.

### New tools reaching the LLM

In Phase 1: the LLM discovers new tools on the next `tools/list` call after bundle startup.
In Phase 4: `notifications/tools/list_changed` will be pushed via SSE immediately.

---

## Configuration (application.yml)

```yaml
services:
  enabled:
    - mcp

mcp:
  endpoint: /mcp
  worker:
    threads: 20                     # mcp-worker pool size; owned by McpHttpTransport
  server:
    name:    tlc-mcp-server
    version: 1.0.0
  security:
    allowed-origins:
      - https://claude.ai
      - http://localhost:3000
  session:                          # [FUTURE Phase 4]
    enabled: false                  # always false until SSE push implemented
    timeout-seconds: 1800
    event-buffer-size: 100
  versioning:
    expose-all-versions: true       # register {name}_v{n} aliases alongside canonical {name}
  shutdown:
    drain-seconds: 5
  execution:
    max-concurrent-per-node: 50     # node-level Semaphore; rejected immediately when full
    hard-timeout-seconds: 300
    task-retention-seconds: 3600    # [Phase 2] ASYNC task entry expiry
```

---

## `McpService` — product-facing API

```java
public interface McpService extends Service {

    McpToolResult invokeTool(String toolName, Map<String, Object> arguments, Object identity);
    McpToolResult readResource(String uri, Object identity);
    McpToolResult invokePrompt(String name, Map<String, String> arguments, Object identity);

    List<McpToolInfo> tools();
    Optional<McpToolInfo> tool(String name);
    List<McpResourceInfo> resources();
    Optional<McpResourceInfo> resource(String uri);
    List<McpPromptInfo> prompts();
    Optional<McpPromptInfo> prompt(String name);

    McpMonitor getMonitor();
}
```

---

## In-memory state and release behaviour

| Store | Contents | Source of truth |
|-------|----------|----------------|
| `McpToolVersionRegistry` | Tool instances, version map, bundle index | Code (`@McpTool` classes) |
| `McpExecutorRegistry` | `McpConcurrencyGuard`, `timeoutScheduler`, `asyncExecutor` | YAML (`mcp.execution.*`) |
| `McpSessionRegistry` | Active sessions, SSE OutputStreams — Phase 4 only | Runtime only |
| `McpTaskRegistry` | ASYNC task handles | Runtime; expire after `task-retention-seconds` |

**On restart:** Tool registry is rebuilt from code at startup. Sessions and tasks are lost.
AI clients reconnect and re-initialize; the MCP protocol is built for reconnection.

**Multi-instance:** `McpSessionRegistry` is per-process. Use sticky sessions at the load
balancer, or keep `session.enabled: false` (stateless mode, recommended for multi-instance).

---

## Existing components used

| Component | Role | Required |
|-----------|------|----------|
| **commons** | `Service` / `ServiceRegistry`; `ContextPropagator`; `ExecutorsHandler` (executor creation) | Yes |
| **web** | `McpHttpTransport` uses `@RestApiHandler`, `Action`, `FullBytesCallback`, `WebExchange`; `provided` scope | Yes |
| **scheduler** | Optional: ASYNC task-registry expiry cleanup; Phase 4 session cleanup | No |
| **cache** | Optional: result caching for expensive read-heavy tools | No |
| **observability** | Optional: metrics bridge that polls `McpMonitor` | No |
| **sql** | Phase 5: execution history storage | No |

---

## Developer experience — Phase 1 SYNC tool example

Two classes per tool — one for shared auth, one for the tool. No schema class, no JSON, no SDK imports.

```java
// Product base — shared auth for all CRM tools (written once in tlc-crm-server)
// CrmIdentity is a product record, e.g. record CrmIdentity(long orgId, long userId) {}
public abstract class CrmMcpTool extends AbstractMcpTool<CrmIdentity> {
    @Override
    public boolean isAuthorized(final McpToolContext<CrmIdentity> ctx) {
        return ctx.identity() != null;   // extractor returns null on auth failure
    }
}


// SYNC tool — result returned directly
@McpTool(
    name           = "search_contacts",
    title          = "Search Contacts",
    version        = "2",
    description    = "Search CRM contacts by name or email. Returns matching contact records.",
    readOnly       = true,
    idempotent     = true,
    executor       = "default",
    timeoutSeconds = 10
)
public class SearchContactsTool extends CrmMcpTool {

    public McpToolResult execute(
        @ToolParam(description = "Name or email to search") String query,
        @ToolParam(description = "Maximum results", required = false, defaultValue = "10", max = "100") int limit,
        final McpToolContext<CrmIdentity> ctx
    ) {
        final long orgId = ctx.identity().orgId();   // type-safe, no cast needed
        final List<Contact> contacts = contactService.search(orgId, query, limit);
        final JsonArray result = Json.array();
        contacts.forEach(c -> result.put(Json.object()
                .put("id",    c.id())
                .put("name",  c.name())
                .put("email", c.email())));
        return McpToolResult.json(result);
    }
}
```

---

## Phase 1 — Exit Criteria

All items below must be satisfied before Phase 2 work begins.

### DSA and design patterns

- [ ] **Chain of Responsibility**: `McpToolPipeline` links layers in declared order; each layer calls `chain.next()` or short-circuits; no layer directly calls the next concrete class
- [ ] **Command**: `McpToolInvocation` is immutable — all pipeline state travels in the command object; no raw JSON-RPC after the entry point
- [ ] **Template Method**: `AbstractMcpTool.isAuthorized()` provides a default; product subclasses override without touching the pipeline wiring
- [ ] **Registry** (`McpToolVersionRegistry`): `NavigableMap<version, McpTool>` — canonical version resolved via `lastKey()` in O(log n); NOT a linear scan
- [ ] **Semaphore** (`McpConcurrencyGuard`): bounded `tryAcquire()` (non-blocking) guards node concurrency; `release()` always in `finally`

### Unit tests

- [ ] `McpSchemaGenerator`: Java type → JSON Schema for all mapped types (String, int, long, double, boolean, enum, List, POJO, nested POJO); circular reference detection at startup; `McpToolContext` and `Map<String,Object>` excluded from schema
- [ ] `McpToolValidator`: required field missing, min/max violation, minLength/maxLength violation, pattern mismatch, singleSelect invalid value, multiSelect invalid element, nested POJO validation, `Map<String,Object>` custom field validation, all errors collected (not fail-fast)
- [ ] `SchemaDefinition`: each factory method, each fluent modifier, `required()` propagation into parent `required[]`, `toJsonSchema()` output for each constraint combination
- [ ] `McpToolVersionRegistry`: register single version, register multiple versions, canonical resolution (highest non-deprecated), deprecated-only fallback with WARN, `removeByBundle()` cleans both maps atomically, `ToolKey` equality
- [ ] `McpConcurrencyGuard`: acquire succeeds when under limit, `tryAcquire()` returns false when at capacity (no blocking), `release()` restores permit, concurrent callers at boundary
- [ ] `McpToolPipeline`: layers execute in declared registration order, `BEFORE_AUTH` layer short-circuits before `McpAuthLayer`, `AFTER_VALIDATION` layer captures result, multiple layers at same position ordered by registration
- [ ] `McpBundleListener`: `bundleStarted` triggers scan and registration, `bundleStopped` triggers removal and `notifyToolsChanged()` if tools removed, no-op `bundleStopped` when bundle had no tools

### Integration tests

- [ ] Full `tools/call` round-trip SYNC: POST → auth → validation → execute → result in JSON-RPC response
- [ ] Auth failure: `isAuthorized()` returns false → error result, `execute()` never called
- [ ] Validation failure: constraint violated → all violations in single error result, `execute()` never called
- [ ] Concurrency rejection: semaphore exhausted → immediate error, no queuing
- [ ] Server state: `POST /mcp` during `INITIALIZING` returns `HTTP 503`; during `READY` returns normal result; during `STOPPING` returns JSON-RPC error
- [ ] Bundle install/remove: bundle install → tool appears in next `tools/call`; bundle remove → tool absent
- [ ] Origin validation: request with disallowed `Origin` returns `403` before JWT processing
- [ ] POJO parameter deserialization: nested POJO correctly mapped from JSON → typed object → `execute()` receives populated POJO
- [ ] `McpSchemaProvider`: custom fields appended to generated schema; `Map<String,Object>` catch-all receives unmatched LLM arguments

### Extensibility

- [ ] New `McpPipelineLayer` can be implemented without modifying any framework class (pipeline insertion via public API is [FUTURE])
- [ ] Bundled `McpHttpTransport` overridable — product can place its own `@RestApiHandler(path = "/mcp")` class in `/action/rest/` to shadow the bundled one if a custom transport is needed
- [ ] mcp-worker pool size adjustable via `mcp.worker.threads` in YAML — zero code change
- [ ] `McpSchemaProvider` allows dynamic schema extension without framework changes

### Bundled transport

- [ ] `POST /rest/api/mcp` returns valid JSON-RPC 2.0 `initialize` response
- [ ] `POST /rest/api/mcp` with `tools/list` returns all registered tools with `inputSchema`
- [ ] `POST /rest/api/mcp` with `tools/call` delegates to `McpService.invokeTool()` and wraps result in JSON-RPC envelope
- [ ] `POST /rest/api/mcp` with `resources/list` returns all registered resources
- [ ] `POST /rest/api/mcp` with `resources/read` delegates to `McpService.readResource()`, returns `contents` array
- [ ] `POST /rest/api/mcp` with `prompts/list` returns all registered prompts
- [ ] `POST /rest/api/mcp` with `prompts/get` delegates to `McpService.invokePrompt()`, returns `messages` array
- [ ] `resources/read` auth failure (resource `isAuthorized()` returns false) returns error result, not 500
- [ ] `prompts/get` auth failure (prompt `isAuthorized()` returns false) returns error result, not 500
- [ ] Unknown `method` returns JSON-RPC error `-32601`
- [ ] Missing `method` field returns JSON-RPC error `-32600`
- [ ] Malformed JSON body returns JSON-RPC error `-32603`
- [ ] `McpHttpTransport` discovered automatically by `WebInitializer` — no `McpInitializer` change needed

### Memory leaks

- [ ] Tool class instances are removed from `McpToolVersionRegistry` when their bundle stops (`removeByBundle()` verified by IT test)
- [ ] `ContextScope` always closed: every call to `snapshot.attach()` is inside `try-with-resources`; no `ContextScope` leaked on exception
- [ ] No tool class instance retained in pipeline or layer reference after bundle `STOPPED`
- [ ] `McpConcurrencyGuard` permit released in `finally` — verified under exception paths

### Data leaks

- [ ] `SecurityContext` cleared after request completes — responsibility of `McpEndpoint` (transport layer); verified by IT test
- [ ] Tool call arguments (`@ToolParam` values) must not be logged in raw form — PII concern; only tool name and version logged at framework level
- [ ] `ContextScope.close()` always in `try-with-resources` — verified: context not leaked to next request on same thread (Karaf thread pool reuse)
- [ ] `SchemaDefinition.toJsonSchema()` is package-private — SDK internal JSON format not accessible outside `com.tlc.mcp.internal`

### Access specifiers

- [ ] `com.tlc.mcp.internal.*` in `<Private-Package>` in `pom.xml` — OSGi enforces it; no internal class importable by products
- [ ] `io.modelcontextprotocol.*` in `<Private-Package>` — SDK completely hidden; zero leakage into exported API
- [ ] `SchemaDefinition.toJsonSchema()` has package-private visibility — confirmed by compilation test from outside the package
- [ ] `McpServerImpl` constructor is package-private — cannot be instantiated by products
- [ ] `McpExecutorRegistry`, `McpTaskRegistry`, `McpSchemaGenerator`, `McpToolValidator` are all package-private classes in `internal` subpackage

---

---

# Phase 2 — ASYNC Execution

## Scope

Phase 2 adds `ExecutionMode.ASYNC`: tools that submit long-running work to a background
thread and return a `trackingId` immediately. The LLM polls the built-in `get_task_status`
tool to retrieve the result when ready. All infrastructure from Phase 1 is in place; Phase 2
extends `McpExecutionLayer` and adds `McpTaskRegistry`.

---

## `ExecutionMode.ASYNC`

```java
public enum ExecutionMode {
    SYNC,   // Phase 1 — execute now and return result directly
    ASYNC   // Phase 2 — submit to background executor, return trackingId immediately;
            // LLM polls get_task_status to retrieve result
}
```

ASYNC is for long-running work (report generation, bulk imports, AI-heavy pipelines) where
execution time exceeds the HTTP/LLM timeout or where the LLM should continue working in parallel.

---

## `McpTaskHandle` (public)

```java
public final class McpTaskHandle {
    String        taskId();         // random UUID; not predictable
    TaskState     state();          // PENDING, RUNNING, COMPLETED, FAILED, TIMEOUT
    McpToolResult result();         // null until COMPLETED or FAILED
    Instant       submittedAt();
    Instant       completedAt();    // null until terminal state
}
```

---

## `McpTaskRegistry` (internal)

```java
ConcurrentHashMap<String taskId, McpTaskStatus> tasks;
```

- Entries expire after `mcp.execution.task-retention-seconds` (default `3600`).
- A `scheduler` task (optional component) performs periodic expiry cleanup.
- Thread-safe reads; no lock for polling — `ConcurrentHashMap` provides lock-free get.

**State machine:**

```
PENDING → RUNNING → COMPLETED
                  → FAILED
                  → TIMEOUT
```

Transitions are monotonic — once a terminal state is set, it is never changed.

---

## Built-in `get_task_status` tool

Auto-registered by the framework at startup — not written by product developers.

```
tools/call  →  get_task_status { "taskId": "task-7f3a-b12c" }
```

| `TaskState` | `get_task_status` returns |
|---|---|
| `PENDING` | `{"taskId":"...","status":"PENDING"}` |
| `RUNNING` | `{"taskId":"...","status":"RUNNING","startedAt":"..."}` |
| `COMPLETED` | original `McpToolResult` from `execute()` |
| `FAILED` | `McpToolResult.error("...")` with the exception message |
| `TIMEOUT` | `McpToolResult.error("Execution timed out after Ns")` |
| expired / not found | `McpToolResult.error("Task not found or expired")` |

---

## ASYNC execution flow

```
McpExecutionLayer.dispatch() — mode = ASYNC
    │
    ▼
McpConcurrencyGuard.tryAcquire()   ← same guard as SYNC
    │
    └── true
          │
          ▼
        McpTaskRegistry.register(taskId, PENDING)
          │
          ▼
        Executor.submit(() → {
            McpTaskRegistry.update(taskId, RUNNING)
            try {
                result = tool.execute(args)    ← runs in background thread
                McpTaskRegistry.update(taskId, COMPLETED, result)
            } catch (TimeoutException t) {
                McpTaskRegistry.update(taskId, TIMEOUT, error)
            } catch (Exception e) {
                McpTaskRegistry.update(taskId, FAILED, error)
            } finally {
                McpConcurrencyGuard.release()  ← released at terminal state
            }
        })
          │
          ▼
        return McpToolResult.trackingId(taskId) immediately to LLM

────────────────────────────────
JSON returned to LLM immediately:
{
  "content": [{
    "type": "text",
    "text": "{\"taskId\":\"task-7f3a-b12c\",\"status\":\"PENDING\",
             \"hint\":\"Call get_task_status with this taskId to retrieve the result.\"}"
  }]
}
```

---

## Developer experience — ASYNC tool example

```java
@McpTool(
    name           = "generate_sales_report",
    title          = "Generate Sales Report",
    description    = "Generate a full sales report. Takes 30-120 seconds. Returns a download URL when complete.",
    executor       = "reporting",
    mode           = ExecutionMode.ASYNC,
    timeoutSeconds = 180
)
public class GenerateSalesReportTool extends CrmMcpTool {

    public McpToolResult execute(
        @ToolParam(description = "Reporting period e.g. 2026-Q1") String period,
        @ToolParam(description = "Output format", options = {"PDF","CSV","XLSX"}, defaultValue = "PDF") String format
    ) {
        // runs in the "reporting" executor pool (background thread)
        // framework already returned trackingId to the LLM before this executes
        final String url = reportService.generate(period, format);
        return McpToolResult.text("Report ready: " + url);  // stored in McpTaskRegistry
    }
}
```

The ASYNC tool's `get_task_status` companion is auto-registered by the framework — developers
write zero polling code.

---

## Phase 2 — Exit Criteria

### DSA and design patterns

- [ ] **State Machine** (`TaskState`): transitions are monotonic (PENDING→RUNNING→terminal); no backward transition; `update()` is atomic via `ConcurrentHashMap.replace()`
- [ ] **Null Object** (`get_task_status` for expired/unknown taskId): returns a well-formed error result — no null, no exception propagated to LLM
- [ ] **ConcurrentHashMap** for `McpTaskRegistry`: lock-free reads for polling (hot path); entries removed by scheduled expiry, not on every read

### Unit tests

- [ ] `McpTaskRegistry`: register entry (state = PENDING), update to RUNNING, update to COMPLETED with result, update to FAILED with error, update to TIMEOUT, expired entries removed by cleanup, `get()` on expired entry returns empty
- [ ] `McpExecutionLayer` ASYNC path: submit without blocking returns trackingId immediately, background thread updates registry to RUNNING then COMPLETED, exception path updates to FAILED, timeout path updates to TIMEOUT
- [ ] `McpTaskHandle`: immutability — `result()` null before terminal, non-null after; `completedAt()` null before terminal
- [ ] `get_task_status` tool: each `TaskState` maps to correct response, unknown taskId returns error, expired taskId returns error
- [ ] Expiry cleanup: entries older than `task-retention-seconds` removed; entries within window retained

### Integration tests

- [ ] ASYNC tool submission → immediate trackingId response → poll `get_task_status` → `COMPLETED` with result
- [ ] ASYNC tool timeout: `McpTaskRegistry` transitions to `TIMEOUT`; `get_task_status` returns timeout error
- [ ] `get_task_status` on expired taskId returns `"Task not found or expired"` (not NPE, not 500)
- [ ] Concurrent ASYNC submissions: 20 parallel ASYNC calls — each gets unique taskId, each polls correctly with own result, no cross-contamination
- [ ] `McpConcurrencyGuard` released at ASYNC terminal state — `totalAvailableSlots()` restored after completion verified via `McpMonitor`
- [ ] ASYNC + validation failure: invalid args → validation error returned synchronously, no task registered in `McpTaskRegistry`

### Extensibility

- [ ] `task-retention-seconds` configurable via YAML only — no code change

### Memory leaks

- [ ] `McpTaskRegistry` entries expire after `task-retention-seconds` — IT test verifies no stale entries after expiry window
- [ ] `McpConcurrencyGuard` permit released on ASYNC terminal state (COMPLETED, FAILED, TIMEOUT) — even if `execute()` throws
- [ ] `McpToolResult` stored in registry is released when entry expires — no long-lived reference to result objects beyond the retention window
- [ ] Background thread holds no reference to `McpToolInvocation` after `execute()` completes

### Data leaks

- [ ] `taskId` is a random UUID — not predictable, not sequential; callers cannot enumerate tasks of other tenants
- [ ] `McpTaskRegistry` does not expose task entries to callers from different tenants — `get_task_status` tool must verify the requesting tenant matches the submitting tenant
- [ ] ASYNC task result stored in memory only — not persisted in this phase; documented retention window (default 1 hour)

### Access specifiers

- [ ] `McpTaskRegistry` is a package-private class inside `com.tlc.mcp.internal.executor`
- [ ] `McpTaskHandle` is a public final class in `com.tlc.mcp.executor` — products may inspect handle via `get_task_status` response
- [ ] `TaskState` is a public enum in `com.tlc.mcp.executor`
- [ ] `get_task_status` is auto-registered as an internal tool; its implementation class is in `com.tlc.mcp.internal` — not product-visible

---

---

# Phase 3 — Resources and Prompts

## Scope

Phase 3 exposes MCP **Resources** (`resources/list`, `resources/read`) and **Prompts**
(`prompts/list`, `prompts/get`). The same package-based discovery, bundle lifecycle, and
authorization model from Phase 1 is extended to cover resources and prompts.

---

## Resources

### `McpResourceDef`

```java
public @interface McpResourceDef {
    String uri();            // stable URI identifying the resource (e.g. "crm://contacts/schema")
    String name();           // human-readable name
    String title()  default "";
    String mimeType() default "application/json";
    String version() default "1";
}
```

### `McpResource`

```java
public interface McpResource {
    McpToolResult read(McpToolContext<Object> ctx);    // returns resource content
    default boolean isAuthorized(McpToolContext<Object> ctx) { return true; }
}
```

Resources receive `McpToolContext<Object>` because the resource interface is not parameterised
at the framework level. Use `instanceof` to check the concrete identity type if needed.

### Package convention

Resources must live in `*.mcp.resource.*` — same discovery pattern as tools. `McpBundleListener`
scans this package subtree on `BundleEvent.STARTED`.

### Protocol flow — `resources/read`

```
Client
  │── POST /mcp: resources/read { "uri": "crm://contacts/schema" }
  │
  ▼
McpServerImpl dispatches to registered McpResource for that URI
  │── resource.isAuthorized(ctx)  → false → error result
  │── resource.isAuthorized(ctx)  → true  → resource.read(ctx)
  │                                     → McpToolResult with content
  │
  ▼
200 JSON-RPC response with resource content
```

---

## Prompts

### `McpPromptDef`

```java
public @interface McpPromptDef {
    String name();
    String title()       default "";
    String description() default "";
    String version()     default "1";
}
```

### `McpPrompt`

```java
public interface McpPrompt {
    McpToolResult get(Map<String, String> arguments, McpToolContext<Object> ctx);
    default boolean isAuthorized(McpToolContext<Object> ctx) { return true; }
}
```

Prompts receive `McpToolContext<Object>` for the same reason as resources — use `instanceof`
to inspect the concrete identity type when needed.

### Package convention

Prompts must live in `*.mcp.prompt.*`.

### Protocol flow — `prompts/get`

```
Client
  │── POST /mcp: prompts/get { "name": "summarise_deal", "arguments": { "dealId": "42" } }
  │
  ▼
McpServerImpl dispatches to registered McpPrompt
  │── prompt.isAuthorized(ctx) → false → error result
  │── prompt.get(arguments, ctx) → McpToolResult with prompt content
  │
  ▼
200 JSON-RPC response with prompt messages
```

---

## Phase 3 — Exit Criteria

### DSA and design patterns

- [ ] Resource and prompt registries follow the same Registry pattern as `McpToolVersionRegistry` — `NavigableMap` for version resolution
- [ ] `McpBundleListener` extended to scan `*.mcp.resource.*` and `*.mcp.prompt.*` alongside `*.mcp.tool.*` without duplicating scan logic
- [ ] `isAuthorized()` on resources and prompts enforced by the framework — same Template Method structure as tool authorization

### Unit tests

- [ ] `McpResourceDef` / `McpResource` registration: URI → resource mapping, version resolution, duplicate URI detection at startup
- [ ] `McpPromptDef` / `McpPrompt` registration: name → prompt mapping, version resolution
- [ ] `resources/list` response: all registered resources serialised with correct URI, name, mimeType
- [ ] `prompts/list` response: all registered prompts serialised with correct name, description
- [ ] `resources/read`: correct resource dispatched by URI, isAuthorized() called before read(), unauthorized returns error
- [ ] `prompts/get`: correct prompt dispatched by name, arguments passed through, isAuthorized() called

### Integration tests

- [ ] `resources/read` round-trip: client sends URI → resource `read()` called → content returned in JSON-RPC response
- [ ] `prompts/get` round-trip: client sends name + arguments → prompt `get()` called → prompt messages returned
- [ ] Auth failure on resource: `isAuthorized()` returns false → error result, `read()` never called
- [ ] Bundle install/remove for resources: bundle start → resource appears in `resources/list`; bundle stop → resource absent
- [ ] Bundle install/remove for prompts: same lifecycle verified

### Extensibility

- [ ] New resource added via `@McpResourceDef` on a class in `*.mcp.resource.*` — zero framework change
- [ ] New prompt added via `@McpPromptDef` on a class in `*.mcp.prompt.*` — zero framework change

### Memory leaks

- [ ] Resource and prompt instances removed from their registries when their bundle stops
- [ ] No resource content (`McpToolResult`) cached at framework level — each `read()` call is fresh

### Data leaks

- [ ] `isAuthorized()` on resources enforced — unauthorized callers cannot read resource content via URI guessing
- [ ] Resource URIs not included in `resources/list` for unauthorized callers (list filtered by authorization)

### Access specifiers

- [ ] `com.tlc.mcp.resource` and `com.tlc.mcp.prompt` in `<Export-Package>` in `pom.xml`
- [ ] Internal resource/prompt registries in `com.tlc.mcp.internal.registry` (private package)

---

---

# Phase 4 — SSE and Push Notifications [FUTURE]

> **Decision (2026-07-18):** All content in this phase is deferred. The current implementation
> (Phases 1–3) is POST-only, stateless. `GET /mcp`, `DELETE /mcp`, `McpSessionRegistry`,
> session management, and `notifications/tools/list_changed` push are not implemented.
> **Do not implement any item in this phase without an explicit requirement and a decision
> to revisit this note.**

## Scope

Phase 4 adds server-to-client push notifications via a persistent SSE stream (`GET /mcp`),
session management (`McpSessionRegistry`), and explicit session termination (`DELETE /mcp`).
This enables real-time tool catalogue updates when bundles are installed or removed.

---

## GET /mcp — SSE stream

```java
// Added to product McpHttpEndpoint when Phase 4 is implemented
@ApiRoute(method = HttpMethod.GET)
public void get(final WebExchange exchange) {
    McpServiceProvider.get().handleGet(exchange);
}
```

Steps:
1. Set response headers: `Content-Type: text/event-stream`, `Cache-Control: no-cache`
2. Validate or create `MCP-Session-Id`
3. Write a priming event with an `id` field (for reconnect support via `Last-Event-ID`)
4. Register the `OutputStream` with `McpSessionRegistry`
5. Keep connection open; close when client disconnects or session expires

SSE wire format:
```
id: evt-00042
data: {"jsonrpc":"2.0","method":"notifications/tools/list_changed"}

```

---

## DELETE /mcp — session termination

```java
@ApiRoute(method = HttpMethod.DELETE)
public void delete(final WebExchange exchange) {
    McpServiceProvider.get().handleDelete(exchange);
}
```

- Reads `MCP-Session-Id` header
- Removes session from `McpSessionRegistry`
- Returns `200 OK`

---

## `McpSessionRegistry` (internal)

```java
ConcurrentHashMap<String sessionId, McpSession> sessions;
```

- `McpSession`: holds `OutputStream`, last-event-id, event buffer (`LinkedList<SseEvent>`)
- Session ID: cryptographically secure random UUID via `SecureRandom`
- Idle session expiry: `scheduler` task evicts sessions older than `session.timeout-seconds`

---

## `notifications/tools/list_changed` push

When `McpServerImpl.notifyToolsChanged()` is called (Phase 4 implementation):

```
McpServerImpl.notifyToolsChanged()
  │
  ▼
for each active session in McpSessionRegistry:
    write to session's SSE OutputStream:
    "id: evt-{n}\ndata: {\"jsonrpc\":\"2.0\",\"method\":\"notifications/tools/list_changed\"}\n\n"
    add event to session's replay buffer
```

---

## Session management

| Phase in session lifecycle | Action |
|---|---|
| First `POST` (`initialize`) | `McpEndpoint` generates session ID, writes `MCP-Session-Id` response header, stores in registry |
| Subsequent `POST` requests | `McpEndpoint` validates `MCP-Session-Id`; returns `400` if missing, `404` if expired |
| `DELETE /mcp` | Session removed immediately |
| Idle timeout | `scheduler` evicts session after `session.timeout-seconds` |

---

## Resumability

`McpEndpoint.get()` reads `Last-Event-ID` header on reconnect. Missed events are replayed
from the session's event buffer (default 100 events, configurable via `session.event-buffer-size`).

---

## Multi-instance concern

`McpSessionRegistry` is per-process. Mitigations:
- **Sticky sessions** at load balancer (recommended)
- **Stateless mode** (`session.enabled: false`) for multi-instance: no SSE push, tools still fully functional
- **Distributed sessions** (future): back `McpSessionRegistry` with `cache` module (Redis)

---

## Phase 4 — Exit Criteria

### DSA and design patterns

- [ ] **Observer pattern**: `McpSessionRegistry` is the subject; each `McpSession.OutputStream` is an observer; `notifyToolsChanged()` dispatches to all active observers
- [ ] **Event buffer** (`LinkedList<SseEvent>` per session, bounded by `event-buffer-size`): FIFO replay on reconnect with `Last-Event-ID`; oldest events evicted when buffer full
- [ ] **SecureRandom** for session ID generation — not `UUID.randomUUID()` (which uses `Random`)

### Unit tests

- [ ] `McpSessionRegistry`: register session, lookup by ID, remove on DELETE, expire idle sessions, concurrent register/remove
- [ ] SSE event format: correct wire format (`id:`, `data:`, double newline), correct JSON-RPC notification body
- [ ] Event buffer: replay events after `Last-Event-ID`, evict oldest when full, no replay when `Last-Event-ID` not in buffer (fresh stream)
- [ ] Session ID generation: output is cryptographically random, no collision in 10,000 generated IDs
- [ ] `notifyToolsChanged()`: dispatches to all registered sessions, skips sessions with closed streams

### Integration tests

- [ ] `GET /mcp` → SSE stream established → `notifications/tools/list_changed` event received when bundle deployed
- [ ] Client reconnects with `Last-Event-ID` → missed events replayed from buffer
- [ ] `DELETE /mcp` → session removed → subsequent `POST` with same session ID returns `404`
- [ ] Session expiry: idle session removed after `timeout-seconds`; POST with expired ID returns `404`
- [ ] Multi-session concurrent push: 10 active sessions — each receives notification independently
- [ ] `POST` without `MCP-Session-Id` (after sessions enabled) returns `400`

### Extensibility

- [ ] `session.event-buffer-size` configurable via YAML
- [ ] `session.timeout-seconds` configurable via YAML
- [ ] SSE write to `OutputStream` is isolated from notification dispatch — failure on one stream does not stop dispatch to others

### Memory leaks

- [ ] `OutputStream` removed from `McpSessionRegistry` when client disconnects (TCP close detected)
- [ ] Event buffer per session is bounded — `event-buffer-size` prevents unbounded growth
- [ ] Expired sessions evicted by scheduler — `McpSessionRegistry` does not grow unboundedly

### Data leaks

- [ ] `MCP-Session-Id` is generated via `SecureRandom` — not predictable, not enumerable
- [ ] Session validated on every POST — `MCP-Session-Id` from one client cannot access another client's session
- [ ] `Origin` header validated before session creation (already in Phase 1 transport; verified still applies to `GET /mcp`)
- [ ] SSE stream closed before session expires — no event data sent to stale connections

### Access specifiers

- [ ] `McpSessionRegistry` is package-private in `com.tlc.mcp.internal.registry`
- [ ] `McpSession` (internal session object with `OutputStream`) is package-private
- [ ] `SseEvent` (event buffer entry) is package-private
- [ ] `MCP-Session-Id` header name is a package-private constant — not a magic string in product code

---

---

# Phase 5 — Execution History [FUTURE]

> **Decision:** Not implemented in current phases. Design deferred to a future release.
> The framework collects no history in memory. When Phase 5 is implemented it must not
> change the Phase 1–3 public API.

## Scope

Phase 5 adds persistent execution history for every tool invocation: audit, analytics,
debugging, and compliance. Storage options are DB (via `sql` module) or S3/object store.

---

## `mcp_tool_execution_history` table

| Field | Type | Description |
|---|---|---|
| `task_id` | UUID | Correlates ASYNC polling; surrogate key |
| `tool_name` | VARCHAR | Which tool was called |
| `tool_version` | VARCHAR | Which version of the tool |
| `tenant_id` | VARCHAR | From `SecurityContext` |
| `caller_identity` | VARCHAR | User or service account ID (not token) |
| `args_digest` | CHAR(64) | SHA-256 of serialised arguments — PII protection |
| `status` | VARCHAR | `COMPLETED`, `FAILED`, `TIMEOUT` |
| `duration_ms` | BIGINT | Wall-clock execution time |
| `executor_group` | VARCHAR | Which pool handled the call |
| `execution_mode` | VARCHAR | `SYNC` or `ASYNC` |
| `started_at` | TIMESTAMP | UTC start time |

**`args_digest`**: SHA-256 hash of serialised arguments, not raw arguments. Raw arguments may
contain PII (names, emails, account numbers) and must never be stored in plain text.

---

## Storage options

**DB table** — via `sql` module:
- Fast queries; suitable for short retention (7–30 days)
- Requires `meta.xml` in the `mcp` module (see below)
- Adds `sql` module as a required dependency when history is enabled

**S3 / object store** — via observability or a dedicated store module:
- Longer retention at lower cost; less queryable
- Suitable for compliance archival beyond 30 days

---

## Retention policy

`mcp.history.retention-days` (YAML) configures the retention window.
A `scheduler` task runs daily to purge entries older than the retention window.

---

## `meta.xml` requirement

When the DB option is chosen, the `mcp` module must carry a `meta.xml` that declares the
`mcp_tool_execution_history` table for schema migration. This makes `sql` a required module
dependency for the DB variant.

---

## Phase 5 — Exit Criteria

### DSA and design patterns

- [ ] **Write-behind async write**: history records written in a background thread (fire-and-forget) — execution latency of the tool itself is not impacted by the history write
- [ ] **SHA-256 digest** for args: `MessageDigest.getInstance("SHA-256")` — deterministic, irreversible, collision-resistant
- [ ] **Retention cleanup**: index on `started_at` — `DELETE WHERE started_at < NOW() - interval` executes efficiently; no table scan

### Unit tests

- [ ] `HistoryWriter`: correct fields written for SYNC tool, ASYNC tool, failed tool, timed-out tool
- [ ] `args_digest`: same arguments produce same digest, different arguments produce different digest, raw args not stored
- [ ] Retention cleanup: entries older than `retention-days` deleted, entries within window preserved
- [ ] `meta.xml`: table DDL correct for target DB (schema migration test)

### Integration tests

- [ ] Tool execution → history record written with correct `tenant_id`, `tool_name`, `status`, `duration_ms`
- [ ] Auth failure → no history record written (history captures only calls that reached `execute()`)
- [ ] Validation failure → no history record written
- [ ] History write failure (DB down) does not propagate to tool caller — fire-and-forget confirmed
- [ ] Retention cleanup removes records older than configured window

### Extensibility

- [ ] Storage backend is pluggable via SPI — DB and S3 are two implementations of the same interface
- [ ] `retention-days` configurable via YAML; no code change required to adjust retention

### Memory leaks

- [ ] History writer holds no reference to `McpToolResult` after write completes
- [ ] Write queue for fire-and-forget writes is bounded — unbounded queue would exhaust heap under sustained load

### Data leaks

- [ ] `args_digest` is SHA-256 — raw arguments never stored in DB or S3
- [ ] `caller_identity` stored as user/service account ID, not as bearer token or password
- [ ] `tenant_id` is always populated — no record written without a tenant (anonymous calls blocked at auth)
- [ ] DB writes use parameterised queries — no SQL injection via tool call arguments

### Access specifiers

- [ ] History SPI (storage interface) is in `com.tlc.mcp` (public) — products can add custom backends
- [ ] History writer implementation is in `com.tlc.mcp.internal` (private)
- [ ] `meta.xml` declares table; no raw `CREATE TABLE` SQL in Java code
