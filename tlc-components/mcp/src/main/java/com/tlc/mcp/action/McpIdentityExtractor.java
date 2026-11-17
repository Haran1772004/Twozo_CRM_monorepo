package com.tlc.mcp.action;

import com.tlc.web.WebExchange;

import java.util.function.Consumer;

/**
 * Async identity extractor for MCP requests.
 *
 * <p>Called on the Undertow I/O thread for every identity-bearing MCP method.
 * Implementations must not block — fire the callback asynchronously when the
 * identity is resolved. The callback is invoked with the resolved identity object,
 * or {@code null} if the caller is unauthenticated.</p>
 *
 * <p>The type parameter {@code I} is the application-defined identity type.
 * {@link com.tlc.mcp.action.rest.McpHttpTransport} stores it as
 * {@code McpIdentityExtractor<Object>} internally (single suppressed cast in the
 * setter) so the static field stays unparameterised at the transport level while
 * callers see the typed API.</p>
 *
 * @param <I> the resolved identity type
 * @author Selvakumar
 * @version 2.0
 */
@FunctionalInterface
public interface McpIdentityExtractor<I> {

    /**
     * Extracts the caller identity from the HTTP exchange asynchronously.
     *
     * @param exchange the inbound HTTP exchange
     * @param callback receives the resolved identity, or {@code null} if unauthenticated;
     *                 must be called exactly once
     */
    void extract(WebExchange exchange, Consumer<I> callback);
}
