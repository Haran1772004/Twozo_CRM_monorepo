package com.tlc.crm.core.mcp;

import com.tlc.crm.common.mcp.CrmMcpIdentity;

import com.tlc.crm.security.context.SecurityContext;

import com.tlc.mcp.tool.AbstractMcpTool;
import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.McpToolResult;

/**
 * CRM-specific base class for all MCP tools.
 * Provides CRM authorization, identity extraction, and context handling.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public abstract class CrmMcpTool extends AbstractMcpTool<CrmMcpIdentity> {

    /**
     * Override authorization to validate CRM user context.
     * Ensures tool can only be executed with valid CRM user identity.
     *
     * @param ctx MCP tool context containing identity
     * @return true if authorized, false otherwise
     */
    @Override
    public boolean isAuthorized(final McpToolContext<CrmMcpIdentity> ctx) {
        try {
            final CrmMcpIdentity crmMcpIdentity = ctx.identity();

            if (null == crmMcpIdentity) {
                return false;
            }
            SecurityContext.load(crmMcpIdentity.crmUser());
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

    /**
     * Helper to generate error response for invalid input.
     *
     * @param message error message
     * @return error McpToolResult
     */
    protected McpToolResult errorResult(String message) {
        return McpToolResult.error(message);
    }

    /**
     * Helper to generate text response.
     *
     * @param text response text
     * @return success McpToolResult
     */
    protected McpToolResult textResult(String text) {
        return McpToolResult.text(text);
    }
}
