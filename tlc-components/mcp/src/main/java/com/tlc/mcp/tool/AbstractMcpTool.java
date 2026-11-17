package com.tlc.mcp.tool;

/**
 * Base class for all MCP tools.
 *
 * <p>The type parameter {@code I} is the resolved caller identity type. Application
 * tools bind this to their own identity record (e.g. {@code AbstractMcpTool<FlowMcpIdentity>}),
 * while built-in/generic tools use {@code AbstractMcpTool<Object>}.</p>
 *
 * <h3>Extension points</h3>
 * <ul>
 *   <li>{@link #isVisible} — presentation filter; hides the tool from {@code tools/list}
 *       for callers that should not see it</li>
 *   <li>{@link #isAuthorized} — security gate; blocks {@code tools/call} execution
 *       regardless of visibility</li>
 *   <li>{@link #inputSchema} — allows per-identity schema customisation at list time</li>
 * </ul>
 *
 * <h3>Bridge methods</h3>
 * <p>The internal pipeline stores tool instances as {@code AbstractMcpTool<?>} (wildcard).
 * The package-private {@code check*} / {@code resolveSchema} methods perform the single
 * suppressed cast so that no call site outside this class needs {@code @SuppressWarnings}.</p>
 *
 * @param <I> the caller identity type
 */
public abstract class AbstractMcpTool<I> {

    /**
     * Returns {@code true} if this tool should appear in the {@code tools/list} response
     * for the given caller. Presentation-only — does not protect against direct invocation.
     * Always pair with {@link #isAuthorized} when hiding for security reasons.
     *
     * @param ctx the MCP tool context
     * @return {@code true} by default
     */
    public boolean isVisible(McpToolContext<I> ctx) {
        return true;
    }

    /**
     * Returns {@code true} if the caller is permitted to execute this tool.
     * This is the security gate — returning {@code false} blocks execution
     * regardless of {@link #isVisible}.
     *
     * @param ctx the MCP tool context
     * @return {@code true} by default
     */
    public boolean isAuthorized(McpToolContext<I> ctx) {
        return true;
    }

    /**
     * Returns the input schema to advertise for this tool at {@code tools/list} time.
     * The schema can vary per identity — hide fields, add admin-only params, etc.
     * Return {@code generated} unchanged for the default behaviour.
     *
     * @param generated the reflection-generated base schema
     * @param ctx       the MCP tool context
     * @return the schema to expose
     */
    public SchemaDefinition inputSchema(SchemaDefinition generated, McpToolContext<I> ctx) {
        return generated;
    }

    /**
     * Returns a standard "Unauthorized" error result.
     *
     * @return an error result with the message "Unauthorized"
     */
    protected final McpToolResult unauthorized() {
        return McpToolResult.error("Unauthorized");
    }

    // -------------------------------------------------------------------------
    // Bridge methods — for internal pipeline use only.
    // The single @SuppressWarnings cast lives here so no call site outside this
    // class needs it.
    // -------------------------------------------------------------------------

    /** Internal pipeline use only — do not call from tool implementations. */
    @SuppressWarnings("unchecked")
    public final boolean checkVisible(McpToolContext<?> ctx) {
        return isVisible((McpToolContext<I>) ctx);
    }

    /** Internal pipeline use only — do not call from tool implementations. */
    @SuppressWarnings("unchecked")
    public final boolean checkAuthorized(McpToolContext<?> ctx) {
        return isAuthorized((McpToolContext<I>) ctx);
    }

    /** Internal pipeline use only — do not call from tool implementations. */
    @SuppressWarnings("unchecked")
    public final SchemaDefinition resolveSchema(SchemaDefinition base, McpToolContext<?> ctx) {
        return inputSchema(base, (McpToolContext<I>) ctx);
    }
}
