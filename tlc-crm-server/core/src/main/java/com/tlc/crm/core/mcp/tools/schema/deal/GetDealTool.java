package com.tlc.crm.core.mcp.tools.schema.deal;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.mcp.CrmMcpIdentity;

import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.converter.DealConverter;
import com.tlc.crm.core.mcp.tools.CrmSecureMcpTool;

import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.SubCategory;

import com.tlc.mcp.tool.McpTool;
import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.McpToolResult;
import com.tlc.mcp.tool.ToolParam;

import java.util.List;
import java.util.Locale;

/**
 * MCP Tool for retrieving deal details from CRM.
 * Accepts a deal ID and returns all details of the deal.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@McpTool(
        name = "get_deal",
        title = "Get Deal",
        description = "Retrieve detailed information about a specific deal from the CRM system by its ID",
        version = "1.0",
        readOnly = true,
        idempotent = true
)
public class GetDealTool extends CrmSecureMcpTool<Deal> {

    private final DealManager dealManager;
    private final DealConverter dealConverter;

    public GetDealTool() {
        this.dealManager = DealManager.getInstance();
        this.dealConverter = DealConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConfigManager<Deal> getConfigManager() {
        return dealManager;
    }

    /**
     * Execute product retrieval.
     * Fetches product details by ID.
     *
     * @param ctx MCP tool context containing identity
     * @param id  Product ID to retrieve (required)
     * @return Tool result with product details
     */
    public McpToolResult execute(final McpToolContext<CrmMcpIdentity> ctx,
                                 @ToolParam(description = "Deal ID to retrieve") final Long id) {
        try {

            if (id == null || id <= 0) {
                return errorResult("Invalid Deal ID");
            }
            final CrmUser crmUser = ctx.identity().crmUser();
            final Long orgId = crmUser.orgId();
            final Deal deal = dealManager.get(orgId, id);

            if (deal == null) {
                return errorResult("Deal not found");
            }

            final ConverterConfig converterConfig = new ConverterConfig(orgId, crmUser.id(), Locale.getDefault(), false, null);
            final JsonObject jsonObject = dealConverter.convertToRestJson(deal, converterConfig);

            return McpToolResult.json(jsonObject);
        } catch (Exception e) {
            LOGGER.error("MCP: Deal get action failed. Reason: {}", e.getMessage(), e);
            return errorResult("Failed to retrieve deal: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param ctx the MCP tool context
     * @return
     */
    @Override
    public boolean isVisible(final McpToolContext<CrmMcpIdentity> ctx) {
        return isAuthorized(ctx) && hasPermission(ctx, SubCategory.Type.DEALS, List.of(Action.Type.VIEW));
    }
}
