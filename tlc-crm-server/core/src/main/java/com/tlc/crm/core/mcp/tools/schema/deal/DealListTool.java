package com.tlc.crm.core.mcp.tools.schema.deal;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;

import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.mcp.CrmMcpIdentity;

import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.converter.DealConverter;
import com.tlc.crm.core.mcp.tools.AbstractEntityListTool;

import com.tlc.crm.entity.list.model.Provider;

import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.SubCategory;

import com.tlc.mcp.tool.McpTool;
import com.tlc.mcp.tool.McpToolContext;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MCP Tool — List Deals.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@McpTool(
        name = "list_deals",
        title = "List Deals",
        description = "Search and list deal records from the CRM with optional filter criteria, sorting, and pagination. "
                + "For unfiltered lists, call directly with start/count. "
                + "For relative date filters (today, yesterday, this week, this month), use dateInterval directly (optional dateField, default created_at). "
                + "When using other filter criteria, call get_list_schema(module='deal') first — it includes dateIntervals. "
                + "For dropdown/owner fields, call get_field_options or search_users to resolve value IDs. "
                + "Supports sorting by any sortable field and pagination with start/count.",
        readOnly = true,
        idempotent = true
)
public class DealListTool extends AbstractEntityListTool {

    private final DealManager dealManager;
    private final DealConverter dealConverter;

    public DealListTool() {
        this.dealManager = DealManager.getInstance();
        this.dealConverter = DealConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Provider getProvider() {
        return Provider.DEAL_LIST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModuleName() {
        return "deal";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JsonArray fetchDataRows(final long orgId, final long userId, final Collection<Long> entityIds) {
        final Map<Long, Deal> deals = dealManager.get(orgId, entityIds).stream().collect(Collectors.toMap(Deal::id, Function.identity()));
        final JsonArray array = Json.array();
        final ConverterConfig converterConfig = new ConverterConfig(orgId, userId, Locale.getDefault(), false, null);

        for (final Long modelId : entityIds) {
            final Deal deal = deals.get(modelId);

            array.put(dealConverter.convertToRestJson(deal, converterConfig));
        }
        return array;
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
