package com.tlc.crm.core.mcp.tools.schema.company;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;

import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.mcp.CrmMcpIdentity;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;
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
 * MCP Tool — List Companies.
 *
 * <p>Lists company records from the CRM with optional filter criteria, sorting,
 * and pagination. Built on the same {@link com.tlc.crm.entity.list.EntitySQLListInterpreter}
 * and {@link com.tlc.crm.entity.list.EntityListAction} infrastructure used by the
 * standard Company List REST API.</p>
 *
 * <p><b>IMPORTANT for calling LLMs:</b>
 * <ul>
 *   <li>For relative date filters, use {@code dateInterval} directly (optional {@code dateField}, default createdTime).</li>
 *   <li>When using other filter criteria, call {@code get_list_schema(module='company')} once — includes dateIntervals.</li>
 *   <li>For dropdown/owner fields, call {@code get_field_options} or {@code search_users} to resolve value IDs.</li>
 * </ul>
 * </p>
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@McpTool(
        name = "list_companies",
        title = "List Companies",
        description = "Search and list company records from the CRM with optional filter criteria, sorting, and pagination. "
                + "For unfiltered lists, call directly with start/count. "
                + "For relative date filters (today, yesterday, this week, this month), use dateInterval directly (optional dateField, default created_at). "
                + "When using other filter criteria, call get_list_schema(module='company') first — it includes dateIntervals. "
                + "For dropdown/owner fields, call get_field_options or search_users to resolve value IDs. "
                + "Supports sorting by any sortable field and pagination with start/count.",
        readOnly = true,
        idempotent = true
)
public class CompanyListTool extends AbstractEntityListTool {

    private final CompanyManager companyManager;
    private final CompanyConverter companyConverter;

    public CompanyListTool() {
        this.companyManager = CompanyManager.getInstance();
        this.companyConverter = CompanyConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Provider getProvider() {
        return Provider.COMPANY_LIST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModuleName() {
        return "company";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JsonArray fetchDataRows(final long orgId, final long userId, final Collection<Long> entityIds) {
        final Map<Long, Company> companies = companyManager.get(orgId, entityIds).stream().collect(Collectors.toMap(Company::id, Function.identity()));
        final JsonArray array = Json.array();
        final ConverterConfig converterConfig = new ConverterConfig(orgId, userId, Locale.getDefault(), false, null);

        for (final Long modelId : entityIds) {
            final Company company = companies.get(modelId);

            array.put(companyConverter.convertToRestJson(company, converterConfig));
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
        return isAuthorized(ctx) && hasPermission(ctx, SubCategory.Type.COMPANIES, List.of(Action.Type.VIEW));
    }
}