# API Versioning — Developer Guide

This package provides **header-based** API versioning using the `X-API-Version` header.

---

## Table of Contents

1. [Supported Approaches](#supported-approaches)
2. [Approach 1: Full Action-Level Versioning (WebAction)](#approach-1-full-action-level-versioning)
3. [Approach 2: Sparse/Partial Versioning (WebAction with handles + fallback)](#approach-2-sparse-versioning)
4. [Approach 3: REST Action Versioning (RestApiHandler)](#approach-3-rest-action-versioning)
5. [When to Use Which](#when-to-use-which)
6. [Component Reference](#component-reference)
7. [Developer Do's and Don'ts](#dos-and-donts)
8. [Use Case Scenarios](#use-case-scenarios)
9. [Common Mistakes & Troubleshooting](#common-mistakes--troubleshooting)

---

## Supported Approaches

| # | Approach | Annotation(s) | Use Case |
|---|----------|---------------|----------|
| 1 | Full Action-Level Versioning | `@WebAction` + `@ApiVersion` | Most/all operations in an action changed |
| 2 | Sparse/Partial Versioning | `@WebAction` + `@ApiVersion(handles, fallbackVersion)` | Only 1-2 operations changed |
| 3 | REST Action Versioning | `@RestApiHandler` + `@ApiRoute` + `@ApiVersion` | REST-style (resource-oriented) endpoints |

---

## Approach 1: Full Action-Level Versioning

Use when **most or all operations** in an action have changed significantly.

### Usage

```java
// V1 - Original action
@WebAction(path = "/contact/mgmt")
@ApiVersion(value = "1", isDefaultVersion = true)
public class ContactManagementV1 extends EntityConfigAction<Contact> {
    // V1 implementation — handles ALL operation types
}

// V2 - New action with significant changes
@WebAction(path = "/contact/mgmt")
@ApiVersion("2")
public class ContactManagementV2 extends EntityConfigAction<Contact> {
    // V2 implementation — handles ALL operation types independently
}
```

### How It Works

1. During startup, `VersionedActionRegistry` detects multiple classes with the same `@WebAction(path)` but different `@ApiVersion` values.
2. It groups them into a single `VersionedAction` composite.
3. At request time, the `X-API-Version` header selects which version's action to invoke.
4. If no header is provided, the version marked `isDefaultVersion = true` is used (falls back to `"1"` if none is marked).

### Components

| File | Description |
|------|-------------|
| `ApiVersion` | Annotation to mark version on an Action class |
| `VersionedAction` | Composite action that routes to the correct version |
| `VersionedActionRegistry` | Collects and builds versioned actions during startup |

---

## Approach 2: Sparse Versioning

Use when **only a few operations** within an action have changed.
The sparse version only defines the changed operations — unhandled operations automatically
fall back to a designated fallback version.

### Usage

```java
// V1 - Handles everything
@WebAction(path = "/contact/mgmt")
@ApiVersion(value = "1", isDefaultVersion = true)
public class ContactManagement extends EntityConfigAction<Contact> {

    @Override
    public void handleRequest(CrmRequest request, CrmResponse response) {
        final String type = request.getRequestJson().getString("type");
        switch (type) {
            case "profile" -> handleProfile(request, response);
            case "updateLifecycleStage" -> handleUpdateLifecycleStage(request, response);
            case "summary" -> handleSummary(request, response);
            // ... many more types
        }
    }
}

// V2 - Only handles "updateLifecycleStage"; everything else falls back to V1
@WebAction(path = "/contact/mgmt")
@ApiVersion(value = "2", handles = {"updateLifecycleStage"}, fallbackVersion = "1")
public class ContactManagementV2 extends EntityConfigAction<Contact> {

    @Override
    public void handleRequest(CrmRequest request, CrmResponse response) {
        final String type = request.getRequestJson().getString("type");
        switch (type) {
            case "updateLifecycleStage" -> handleUpdateLifecycleStageV2(request, response);
        }
    }
}
```

### How It Works

1. Client sends request with `X-API-Version: 2` and `type: "profile"`.
2. `VersionedAction` routes to `ContactManagementV2`.
3. Framework attaches a `VersionFallbackContext` because V2 declares `handles`.
4. `AbstractWebAction` (or your action) checks if V2 handles `"profile"` → **No**.
5. Request is automatically delegated to the fallback version (V1).
6. V1's `handleRequest` processes `"profile"`.

### Fallback Resolution Order

1. If `fallbackVersion` is explicitly set → redirect to that version.
2. If `fallbackVersion` is absent → redirect to the version with `isDefaultVersion = true`.
3. If neither exists → fall back to version `"1"`.

### Key Rule

- `handles()` and `fallbackVersion` are **independent**:
  - If `handles()` is empty and `fallbackVersion` is set → unconditional redirect to fallback.
  - If `handles()` is non-empty → fallback is only triggered for non-handled type requests.
  - If `handles()` is non-empty and `fallbackVersion` is absent → fallback uses default version resolution (step 2–3 above).

### Components

| File | Description |
|------|-------------|
| `ApiVersion#handles()` | Attribute listing handled operation types |
| `ApiVersion#fallbackVersion()` | Attribute specifying the fallback version |
| `VersionFallbackContext` | Request-scoped context for delegation (attached to WebExchange) |

---

## Approach 3: REST Action Versioning

Use when you have REST-style actions (`@RestApiHandler` + `@ApiRoute`) and need to version them.

### Usage

```java
// V1 - Original REST action
@RestApiHandler(path = "/contact")
@ApiVersion(value = "1", isDefaultVersion = true)
public class ContactRestManagement extends CrmSecureRestAction<Contact> {

    @ApiRoute(path = "/{id}", method = HttpMethod.GET)
    public void get(CrmRequest request, CrmResponse response) {
        // V1 implementation
    }

    @ApiRoute(method = HttpMethod.POST)
    public void post(CrmRequest request, CrmResponse response) {
        // V1 implementation
    }
}

// V2 - New REST action with changes
@RestApiHandler(path = "/contact")
@ApiVersion("2")
public class ContactRestManagementV2 extends CrmSecureRestAction<Contact> {

    @ApiRoute(path = "/{id}", method = HttpMethod.GET)
    public void get(CrmRequest request, CrmResponse response) {
        // V2 implementation
    }

    @ApiRoute(method = HttpMethod.POST)
    public void post(CrmRequest request, CrmResponse response) {
        // V2 implementation
    }
}
```

### How It Works

1. Multiple classes share the same `@RestApiHandler(path)` with different `@ApiVersion` values.
2. During startup, `VersionedRestActionRegistry` groups them and creates `VersionedRestAction` entries per route.
3. At request time, the `X-API-Version` header determines which version's action and method to invoke.
4. If no version header is provided, the version marked with `isDefaultVersion = true` is used (falls back to `"1"` if none is marked).

### Components

| File | Description |
|------|-------------|
| `VersionedRestAction` | REST equivalent of `VersionedAction` — holds version → RouteContext mappings |
| `VersionedRestActionRegistry` | REST equivalent of `VersionedActionRegistry` — collects and builds versioned REST actions |
| `RestWebService` | Interface with versioned registration method |
| `UndertowService` | Implements versioned REST route registration |

### Client Request

```http
GET /rest/contact/123
X-API-Version: 2
```

---

## When to Use Which

| Scenario | Approach |
|----------|----------|
| Most/all operations changed in an action | Approach 1 — Full Action-Level |
| Only 1-2 operations changed | Approach 2 — Sparse Versioning |
| Complete API rewrite for a path | Approach 1 — Full Action-Level |
| REST resource endpoints (CRUD) | Approach 3 — REST Action Versioning |
| Minor enhancements to specific types | Approach 2 — Sparse Versioning |

---

## Component Reference

### `@ApiVersion` Annotation Attributes

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `value` | `String` | *(required)* | Version identifier (e.g., `"1"`, `"2"`) |
| `isDefaultVersion` | `boolean` | `false` | Marks this as the default version for the path |
| `handles` | `String[]` | `{}` | Operation types this version handles (sparse versioning) |
| `fallbackVersion` | `String` | `""` | Version to redirect to for unhandled types |
| `deprecated` | `boolean` | `false` | Marks this version as deprecated |
| `deprecationMessage` | `String` | `""` | Message to include in deprecation warnings |
| `successor` | `String` | `""` | Version to redirect to when this is sunset |

### Core Classes

| Class | Role |
|-------|------|
| `VersionedAction` | Composite action that dispatches to the correct version at runtime |
| `VersionedAction.Builder` | Collects version entries and builds the composite |
| `VersionedActionRegistry` | Singleton registry; collects WebAction registrations, validates, and builds |
| `VersionedRestAction` | REST equivalent of `VersionedAction` |
| `VersionedRestActionRegistry` | REST equivalent registry |
| `VersionFallbackContext` | Attached to WebExchange for sparse delegation logic |
| `ApiVersionUtils` | Utility to resolve version from header |
| `DeprecationContext` | Holds deprecation metadata for response headers |

---

## Do's and Don'ts

### ✅ Do's

1. **DO** mark exactly one version per path as `isDefaultVersion = true`.
2. **DO** ensure version `"1"` exists if you don't mark any version as default.
3. **DO** use `handles` to list **only the types your sparse version actually implements**.
4. **DO** set `fallbackVersion` explicitly when using sparse versioning for clarity.
5. **DO** keep all versions for the same path in the same module/bundle to avoid ordering issues.
6. **DO** use `@ApiVersion` on every class that shares a path — even V1.
7. **DO** test both the versioned path and the fallback path for sparse versions.
8. **DO** use the same path string exactly (case-sensitive) across all versions.
9. **DO** ensure the fallback version actually exists in the registry.
10. **DO** keep REST versioned classes aligned — every `@ApiRoute` should be present in every version class (no partial route coverage unless intended).

### ❌ Don'ts

1. **DON'T** mark more than one version as `isDefaultVersion = true` for the same path — startup will fail.
2. **DON'T** omit `@ApiVersion` on any class sharing a versioned path — it defaults to version `"1"` implicitly, which may conflict.
3. **DON'T** put business logic in the versioning layer — keep `VersionedAction` and `VersionFallbackContext` as routing-only infrastructure.
4. **DON'T** mix Approach 1 and Approach 2 carelessly — if V2 declares `handles`, it is sparse; if it doesn't, it is a full replacement.
5. **DON'T** use `fallbackVersion` without ensuring the target version can handle those types.
6. **DON'T** rely on version ordering/comparison for routing — versions are matched by exact string equality first.
7. **DON'T** couple `handles()` and `fallbackVersion` — they are independent attributes with independent semantics.
8. **DON'T** change the `X-API-Version` header name — it's a system constant.
9. **DON'T** create circular fallbacks (e.g., V2 → V3, V3 → V2).
10. **DON'T** register the same version string twice for the same path+method — startup will fail.

---

## Use Case Scenarios

### Scenario 1: Simple V2 Upgrade (Full Replacement)

**Context:** The entire `/deal/mgmt` action has been rewritten for V2.

```java
@WebAction(path = "/deal/mgmt")
@ApiVersion(value = "1", isDefaultVersion = true)
public class DealManagementV1 extends EntityConfigAction<Deal> { /* ... */ }

@WebAction(path = "/deal/mgmt")
@ApiVersion("2")
public class DealManagementV2 extends EntityConfigAction<Deal> { /* ... */ }
```

**Behavior:**
- `X-API-Version: 1` → V1
- `X-API-Version: 2` → V2
- No header → V1 (default)

---

### Scenario 2: Sparse V2 (Partial Override)

**Context:** Only `"updateLifecycleStage"` changed in V2; all other types remain the same.

```java
@WebAction(path = "/contact/mgmt")
@ApiVersion(value = "1", isDefaultVersion = true)
public class ContactManagementV1 extends EntityConfigAction<Contact> {
    // Handles: profile, updateLifecycleStage, summary, etc.
}

@WebAction(path = "/contact/mgmt")
@ApiVersion(value = "2", handles = {"updateLifecycleStage"}, fallbackVersion = "1")
public class ContactManagementV2 extends EntityConfigAction<Contact> {
    // Only handles: updateLifecycleStage
}
```

**Behavior:**
- `X-API-Version: 2`, type=`"updateLifecycleStage"` → V2 processes it.
- `X-API-Version: 2`, type=`"profile"` → V2 does NOT handle it → falls back to V1.
- `X-API-Version: 1`, any type → V1.
- No header → V1 (default).

---

### Scenario 3: Unconditional Fallback (No handles, fallbackVersion set)

**Context:** V2 is registered but should unconditionally redirect to V1.

```java
@WebAction(path = "/account/mgmt")
@ApiVersion(value = "1", isDefaultVersion = true)
public class AccountManagementV1 extends EntityConfigAction<Account> { /* ... */ }

@WebAction(path = "/account/mgmt")
@ApiVersion(value = "2", fallbackVersion = "1")
public class AccountManagementV2 extends EntityConfigAction<Account> { /* ... */ }
```

**Behavior:**
- `X-API-Version: 2` → `VersionFallbackContext` attached with empty `handledTypes` → all requests fall back to V1.

---

### Scenario 4: REST API Versioning

**Context:** `/contact` REST resource has a V2 with improved GET response format.

```java
@RestApiHandler(path = "/contact")
@ApiVersion(value = "1", isDefaultVersion = true)
public class ContactRestV1 extends CrmSecureRestAction<Contact> {
    @ApiRoute(path = "/{id}", method = HttpMethod.GET)
    public void get(CrmRequest req, CrmResponse res) { /* V1 */ }
}

@RestApiHandler(path = "/contact")
@ApiVersion("2")
public class ContactRestV2 extends CrmSecureRestAction<Contact> {
    @ApiRoute(path = "/{id}", method = HttpMethod.GET)
    public void get(CrmRequest req, CrmResponse res) { /* V2 */ }
}
```

**Behavior:**
- `GET /rest/contact/123` with `X-API-Version: 2` → V2's `get()`.
- `GET /rest/contact/123` without header → V1's `get()`.

---

### Scenario 5: Deprecating a Version

**Context:** V1 is deprecated; users should migrate to V2.

```java
@WebAction(path = "/lead/mgmt")
@ApiVersion(value = "1", deprecated = true, deprecationMessage = "Use V2", successor = "2")
public class LeadManagementV1 extends EntityConfigAction<Lead> { /* ... */ }

@WebAction(path = "/lead/mgmt")
@ApiVersion(value = "2", isDefaultVersion = true)
public class LeadManagementV2 extends EntityConfigAction<Lead> { /* ... */ }
```

**Behavior:**
- `X-API-Version: 1` → Automatically redirects to V2 (successor). Deprecation headers added.
- `X-API-Version: 2` → V2 directly.
- No header → V2 (default).

---

### Scenario 6: Three Versions with Sparse V3

**Context:** V1 is the base, V2 is a full rewrite, V3 only changes one operation from V2.

```java
@WebAction(path = "/ticket/mgmt")
@ApiVersion("1")
public class TicketManagementV1 extends EntityConfigAction<Ticket> { /* ... */ }

@WebAction(path = "/ticket/mgmt")
@ApiVersion(value = "2", isDefaultVersion = true)
public class TicketManagementV2 extends EntityConfigAction<Ticket> { /* ... */ }

@WebAction(path = "/ticket/mgmt")
@ApiVersion(value = "3", handles = {"escalate"}, fallbackVersion = "2")
public class TicketManagementV3 extends EntityConfigAction<Ticket> {
    // Only handles "escalate" — everything else goes to V2
}
```

**Behavior:**
- `X-API-Version: 3`, type=`"escalate"` → V3.
- `X-API-Version: 3`, type=`"resolve"` → falls back to V2.
- `X-API-Version: 2` → V2 (all types).
- No header → V2 (default).

---

## Common Mistakes & Troubleshooting

| Problem | Cause | Fix |
|---------|-------|-----|
| Startup error: "isDefaultVersion already declared" | Two classes marked `isDefaultVersion = true` for the same path | Remove `isDefaultVersion` from one |
| Startup error: "Default version for this path not found" | No `isDefaultVersion = true` AND no version `"1"` registered | Either add version `"1"` or mark one as default |
| Request goes to wrong version | Path strings don't match exactly (case, trailing slash) | Ensure identical `path` across all versions |
| Fallback not triggered | `handles()` is empty and `fallbackVersion` not set | Set either `handles` or `fallbackVersion` |
| Fallback loops or NPE | `fallbackVersion` points to a non-existent version | Ensure the target version is registered |
| REST version not applied | Missing `@ApiVersion` on the REST class | Add the annotation |

---

## Client Request

All approaches use the same header:

```http
POST /contact/mgmt
X-API-Version: 2
Content-Type: application/json

{
    "type": "updateLifecycleStage",
    "data": { ... }
}
```

If no `X-API-Version` header is provided, the default version is used (resolved via `isDefaultVersion` or fallback to `"1"`).

