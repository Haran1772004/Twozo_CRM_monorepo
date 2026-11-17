package com.tlc.mcp.tool;

/**
 * Immutable context passed to every MCP tool method at invocation time.
 *
 * <p>The type parameter {@code I} is the resolved caller identity produced by the
 * application's {@link com.tlc.mcp.action.McpIdentityExtractor}. Tools that carry
 * their own {@code I} bound (e.g. {@code AbstractMcpTool<FlowMcpIdentity>}) receive
 * {@code identity()} already typed to their expected identity class — no cast or
 * {@code instanceof} check required.</p>
 *
 * <p>The internal pipeline stores contexts as {@code McpToolContext<?>} (wildcard).
 * The typed accessor is reached through the bridge methods on {@link AbstractMcpTool}
 * which perform a single suppressed cast in one place.</p>
 *
 * @param <I> the identity type; {@code Object} in generic/built-in tools
 */
public final class McpToolContext<I> {

    private final String toolName;
    private final String toolVersion;
    private final I identity;

    /**
     * @param toolName    the name of the tool being invoked
     * @param toolVersion the version of the tool being invoked
     * @param identity    the resolved caller identity, or {@code null} if unauthenticated
     */
    public McpToolContext(String toolName, String toolVersion, I identity) {
        this.toolName    = toolName;
        this.toolVersion = toolVersion;
        this.identity    = identity;
    }

    public String toolName()    { return toolName; }
    public String toolVersion() { return toolVersion; }

    /**
     * Returns the resolved caller identity.
     *
     * @return identity of type {@code I}, or {@code null} if unauthenticated
     */
    public I identity() { return identity; }
}
