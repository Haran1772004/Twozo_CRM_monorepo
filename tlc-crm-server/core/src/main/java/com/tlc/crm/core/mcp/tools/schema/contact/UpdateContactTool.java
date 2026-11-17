package com.tlc.crm.core.mcp.tools.schema.contact;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.mcp.CrmMcpIdentity;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.formatter.ContactRestReqFormatter;
import com.tlc.crm.core.mcp.tools.CrmSecureMcpTool;
import com.tlc.crm.core.mcp.tools.schema.builder.ContactFieldSchemaBuilder;

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
 * MCP tool for updating a contact in the CRM.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@McpTool(
        name = "update_contact",
        title = "Update Contact", description = "Update a contact in the CRM. Pass a single `input` object containing default fields and org-specific custom fields."
)
public class UpdateContactTool extends CrmSecureMcpTool<Contact> {

    private final ContactManager contactManager;
    private final ContactConverter contactConverter;
    private final ContactRestReqFormatter contactRestReqFormatter;
    private final ContactFieldSchemaBuilder fieldSchemaBuilder;

    public UpdateContactTool() {
        this.contactManager = ContactManager.getInstance();
        this.contactConverter = ContactConverter.getInstance();
        this.contactRestReqFormatter = ContactRestReqFormatter.getInstance();
        this.fieldSchemaBuilder = ContactFieldSchemaBuilder.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConfigManager<Contact> getConfigManager() {
        return contactManager;
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
            final Contact contact = convertPartialData(crmUser, requestJson, i18nResolver);
            final Collection<ErrorDescriptor> errorDescriptors = validate(List.of(contact), Group.Update.class);

            if (!errorDescriptors.isEmpty()) {
                final List<String> errors = errorDescriptors.stream().map(ErrorDescriptor::message).toList();

                return errorResult("Validation failed:\n• " + String.join("\n• ", errors));
            }
            final EventData eventData = new SecureEventData(orgId, userId);

            eventData.setLocale(locale);
            eventData.setPreviousValues(List.of(contactManager.get(orgId, contact.id())));
            final JsonObject jsonObject = update(orgId, userId, eventData, List.of(contact));

            if (Objects.equals("error", jsonObject.optString("status", ""))) {
                return McpToolResult.error(jsonObject.optString("message", "Unknown error"));
            } else {
                return McpToolResult.json(jsonObject);
            }
        } catch (Exception e) {
            LOGGER.error("MCP: Update contact failed. Reason: {}", e.getMessage(), e);
            return errorResult("Failed to update contact: " + e.getMessage());
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
    protected Contact convertPartialData(final CrmUser crmUser, final JsonObject jsonObject, final I18nResolver i18nResolver) {
        final JsonObject contactJson = contactRestReqFormatter.format(crmUser.orgId(), jsonObject, i18nResolver);
        final ConverterConfig converterConfig = new ConverterConfig(crmUser.orgId(), crmUser.id(), Locale.getDefault(), true, contactJson);

        return contactConverter.convert(converterConfig, contactJson);
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
            LOGGER.error("MCP: Dynamic schema generation failed for update_contact: {}", e.getMessage());
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
        return isAuthorized(ctx) && hasPermission(ctx, SubCategory.Type.CONTACTS, List.of(Type.EDIT, Type.VIEW));
    }

}
