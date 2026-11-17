package com.tlc.crm.core.mcp.tools.schema.company;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.mcp.CrmMcpIdentity;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;
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
 * MCP Tool for retrieving company details from CRM.
 * Accepts a company ID and returns all details of the company.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@McpTool(
        name = "get_company",
        title = "Get Company",
        description = "Retrieve detailed information about a specific company from the CRM system by its ID",
        version = "1.0",
        readOnly = true,
        idempotent = true
)
public class GetCompanyTool extends CrmSecureMcpTool<Company> {

    private final CompanyManager companyManager;
    private final CompanyConverter companyConverter;

    public GetCompanyTool() {
        this.companyManager = CompanyManager.getInstance();
        this.companyConverter = CompanyConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConfigManager<Company> getConfigManager() {
        return companyManager;
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
                                 @ToolParam(description = "Company ID to retrieve") final Long id) {
        try {
            if (id == null || id <= 0) {
                return errorResult("Invalid company ID");
            }
            final CrmUser crmUser = ctx.identity().crmUser();
            final Long orgId = crmUser.orgId();
            final Company company = companyManager.get(orgId, id);

            if (company == null) {
                return errorResult("Company not found");
            }

            final ConverterConfig converterConfig = new ConverterConfig(orgId, crmUser.id(), Locale.getDefault(), false, null);
            final JsonObject jsonObject = companyConverter.convertToRestJson(company, converterConfig);

            return McpToolResult.json(jsonObject);
        } catch (Exception e) {
            LOGGER.error("MCP: Company get action failed. Reason: {}", e.getMessage(), e);
            return errorResult("Failed to retrieve company: " + e.getMessage());
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
        return isAuthorized(ctx) && hasPermission(ctx, SubCategory.Type.COMPANIES, List.of(Action.Type.VIEW));
    }
}
