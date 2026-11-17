package com.tlc.crm.core.mcp.tools.schema.deal;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.mcp.CrmMcpIdentity;

import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.mcp.tools.CrmSecureMcpTool;

import com.tlc.crm.security.permission.model.Action.Type;
import com.tlc.crm.security.permission.model.SubCategory;

import com.tlc.mcp.tool.McpTool;
import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.McpToolResult;
import com.tlc.mcp.tool.ToolParam;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * MCP Tool for deleting deal from CRM.
 * Accepts a deal ID and removes the deal from the system.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@McpTool(
        name = "delete_deal",
        title = "Delete Deal",
        description = "Delete a deal from the CRM system. This action is permanent and cannot be undone.",
        version = "1.0",
        destructive = true
)
public class DeleteDealTool extends CrmSecureMcpTool<Deal> {

    private final DealManager dealManager;

    public DeleteDealTool() {
        this.dealManager = DealManager.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConfigManager<Deal> getConfigManager() {
        return dealManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param ctx Refers the {@code McpToolContext<CrmMcpIdentity>}.
     */
    public McpToolResult execute(final McpToolContext<CrmMcpIdentity> ctx,
                                 @ToolParam(description = "Deal ID to delete", required = true) final Long id,
                                 @ToolParam(description = "Must be true to confirm deletion (safety check)", required = true,
                                         options = {"true", "false"}) final boolean confirmDelete) {
        try {
            if (id == null || id <= 0) {
                return errorResult("Invalid deal ID");
            }

            if (!confirmDelete) {
                return errorResult("Deletion must be explicitly confirmed. Set confirmDelete to true.");
            }

            final CrmUser crmUser = ctx.identity().crmUser();
            final long orgId = crmUser.orgId();
            final long userId = crmUser.id();

            if (!dealManager.exists(orgId, id)) {
                return errorResult("Deal not found");
            }

            final EventData eventData = new SecureEventData(orgId, userId);
            final Deal deal = dealManager.get(orgId, id);

            eventData.setLocale(Locale.getDefault());
            eventData.setPreviousValues(List.of(deal));
            final JsonObject jsonObject = delete(orgId, userId, eventData, List.of(deal));

            if (Objects.equals("error", jsonObject.optString("status", ""))) {
                return McpToolResult.error(jsonObject.optString("message", "Unknown error"));
            } else {
                return McpToolResult.json(jsonObject);
            }
        } catch (Exception e) {
            LOGGER.error("MCP: Deal delete action failed. Reason: {}", e.getMessage(), e);
            return errorResult("Failed to delete deal");
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
        return isAuthorized(ctx) && hasPermission(ctx, SubCategory.Type.DEALS, List.of(Type.DELETE, Type.VIEW));
    }
}
