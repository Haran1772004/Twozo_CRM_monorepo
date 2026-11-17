package com.tlc.crm.core.mcp.tools.schema.company;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.mcp.CrmMcpIdentity;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;
import com.tlc.crm.core.formatter.CompanyRestReqFormatter;
import com.tlc.crm.core.mcp.tools.CrmSecureMcpTool;
import com.tlc.crm.core.mcp.tools.schema.builder.CompanyFieldSchemaBuilder;

import com.tlc.crm.security.permission.model.Action.Type;
import com.tlc.crm.security.permission.model.SubCategory;

import com.tlc.i18n.I18nResolver;

import com.tlc.mcp.tool.McpTool;
import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.McpToolResult;
import com.tlc.mcp.tool.SchemaDefinition;

import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.type.Group;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * MCP tool for updating a company in the CRM.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@McpTool(
        name = "update_company",
        title = "Update Company", description = "Update a company in the CRM. Pass a single `input` object containing default fields and org-specific custom fields."
)
public class UpdateCompanyTool extends CrmSecureMcpTool<Company> {

    private final CompanyManager companyManager;
    private final CompanyConverter companyConverter;
    private final CompanyRestReqFormatter companyRestReqFormatter;
    private final CompanyFieldSchemaBuilder fieldSchemaBuilder;

    public UpdateCompanyTool() {
        this.companyManager = CompanyManager.getInstance();
        this.companyConverter = CompanyConverter.getInstance();
        this.companyRestReqFormatter = CompanyRestReqFormatter.getInstance();
        this.fieldSchemaBuilder = CompanyFieldSchemaBuilder.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConfigManager<Company> getConfigManager() {
        return companyManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param ctx Refers the {@code McpToolContext<CrmMcpIdentity>}.
     */
    public McpToolResult execute(final McpToolContext<CrmMcpIdentity> ctx, final Map<String, Object> input) {
        try {
            final CrmUser crmUser = ctx.identity().crmUser();
            final long orgId = crmUser.orgId();
            final long userId = crmUser.id();
            final JsonObject requestJson = buildRequestJson(input);
            final Locale locale = Locale.getDefault();
            final I18nResolver i18nResolver = i18nService.getResolver(locale);
            final Company company = convertPartialData(crmUser, requestJson, i18nResolver);
            final Collection<ErrorDescriptor> errorDescriptors = validate(List.of(company), Group.Update.class);

            if (!errorDescriptors.isEmpty()) {
                final List<String> errors = errorDescriptors.stream().map(ErrorDescriptor::message).toList();

                return errorResult("Validation failed:\n• " + String.join("\n• ", errors));
            }
            final EventData eventData = new SecureEventData(orgId, userId);

            eventData.setLocale(locale);
            eventData.setPreviousValues(List.of(companyManager.get(orgId, company.id())));
            final JsonObject jsonObject = update(orgId, userId, eventData, List.of(company));

            if (Objects.equals("error", jsonObject.optString("status", ""))) {
                return McpToolResult.error(jsonObject.optString("message", "Unknown error"));
            } else {
                return McpToolResult.json(jsonObject);
            }
        } catch (Exception e) {
            LOGGER.error("MCP: Update company failed. Reason: {}", e.getMessage(), e);
            return errorResult("Failed to update company: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param crmUser      The authenticated CRM user.
     * @param jsonObject   The partial input JSON payload.
     * @param i18nResolver The internationalization resolver.
     * @return
     */
    protected Company convertPartialData(final CrmUser crmUser, final JsonObject jsonObject, final I18nResolver i18nResolver) {
        final JsonObject companyJson = companyRestReqFormatter.format(crmUser.orgId(), jsonObject, i18nResolver);
        final ConverterConfig converterConfig = new ConverterConfig(crmUser.orgId(), crmUser.id(), Locale.getDefault(), true, companyJson);

        return companyConverter.convert(converterConfig, companyJson);
    }

    /**
     * {@inheritDoc}
     *
     * @param generated the reflection-generated base schema
     * @param ctx       the MCP tool context
     * @return
     */
    @Override
    public SchemaDefinition inputSchema(final SchemaDefinition generated, final McpToolContext<CrmMcpIdentity> ctx) {
        try {
            final CrmUser crmUser = ctx.identity().crmUser();
            final long orgId = crmUser.orgId();
            final List<SchemaDefinition.FieldDef> fieldDefList = fieldSchemaBuilder.build(orgId, Action.UPDATE, i18nService.getResolver(Locale.getDefault()));

            return SchemaDefinition.of(fieldDefList);
        } catch (Exception e) {
            LOGGER.error("MCP: Dynamic schema generation failed for update_company: {}", e.getMessage());
            return generated;
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
        return isAuthorized(ctx) && hasPermission(ctx, SubCategory.Type.COMPANIES, List.of(Type.EDIT, Type.VIEW));
    }

}
