package com.tlc.crm.core.mcp.tools;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.mcp.CrmMcpIdentity;

import com.tlc.crm.core.mcp.CrmMcpTool;
import com.tlc.crm.core.mcp.tools.schema.builder.ListSchemaBuilder;

import com.tlc.mcp.tool.McpTool;
import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.McpToolResult;
import com.tlc.mcp.tool.ToolParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MCP Tool — Get List Schema.
 *
 * <p>Returns filterable fields with embedded comparators, value formats, and criteria examples
 * for a CRM module in a single response. Replaces the need to call {@code get_filter_fields}
 * and {@code get_filter_comparators} separately before using list tools.</p>
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@McpTool(
        name = "get_list_schema",
        title = "Get List Schema",
        description = """
                Returns filterable fields with comparators, value formats, and dateIntervals for a CRM module.
                Call this ONCE before building filter criteria for list_* tools.
                Not required for unfiltered list requests or when using dateInterval shortcut on list_*.
                """,
        readOnly = true,
        idempotent = true
)
public class GetListSchemaTool extends CrmMcpTool {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetListSchemaTool.class);

    private final ListSchemaBuilder listSchemaBuilder;

    public GetListSchemaTool() {
        this.listSchemaBuilder = ListSchemaBuilder.getInstance();
    }

    /**
     * Returns the merged list schema for the specified module.
     *
     * @param ctx    MCP tool context
     * @param module CRM module name: contact, company, product, or deal
     * @return JSON with filterable fields and embedded comparators
     */
    public McpToolResult execute(final McpToolContext<CrmMcpIdentity> ctx,
                                 @ToolParam(
                                         name = "module",
                                         description = "CRM module to get schema for. Valid values: 'contact', 'company', 'product' and 'deal'."
                                 ) final String module) {
        try {
            if (module == null || module.isBlank()) {
                return errorResult("Parameter 'module' is required. Valid values: contact, company, product or deal.");
            }

            final CrmUser crmUser = ctx.identity().crmUser();
            final long orgId = crmUser.orgId();

            return McpToolResult.json(listSchemaBuilder.buildListSchema(orgId, module));
        } catch (Exception e) {
            LOGGER.error("MCP: Error in get_list_schema for module: {}, Reason: {}", module, e.getMessage(), e);
            return errorResult("Failed to get list schema: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isVisible(final McpToolContext<CrmMcpIdentity> ctx) {
        return isAuthorized(ctx);
    }
}
