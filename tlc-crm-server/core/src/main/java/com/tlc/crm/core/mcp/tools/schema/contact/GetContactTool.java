package com.tlc.crm.core.mcp.tools.schema.contact;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.mcp.CrmMcpIdentity;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
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
 * MCP Tool for retrieving contact details from CRM.
 * Accepts a contact ID and returns all details of the contact.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@McpTool(
        name = "get_contact",
        title = "Get Contact",
        description = "Retrieve detailed information about a specific contact from the CRM system by its ID",
        version = "1.0",
        readOnly = true,
        idempotent = true
)
public class GetContactTool extends CrmSecureMcpTool<Contact> {

    private final ContactManager contactManager;
    private final ContactConverter contactConverter;

    public GetContactTool() {
        this.contactManager = ContactManager.getInstance();
        this.contactConverter = ContactConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConfigManager<Contact> getConfigManager() {
        return contactManager;
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
                                 @ToolParam(description = "Contact ID to retrieve") final Long id) {
        try {

            if (id == null || id <= 0) {
                return errorResult("Invalid Contact ID");
            }
            final CrmUser crmUser = ctx.identity().crmUser();
            final Long orgId = crmUser.orgId();
            final Contact contact = contactManager.get(orgId, id);

            if (contact == null) {
                return errorResult("Contact not found");
            }

            final ConverterConfig converterConfig = new ConverterConfig(orgId, crmUser.id(), Locale.getDefault(), false, null);
            final JsonObject jsonObject = contactConverter.convertToRestJson(contact, converterConfig);

            return McpToolResult.json(jsonObject);
        } catch (Exception e) {
            LOGGER.error("MCP: Contact get action failed. Reason: {}", e.getMessage(), e);
            return errorResult("Failed to retrieve contact: " + e.getMessage());
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
        return isAuthorized(ctx) && hasPermission(ctx, SubCategory.Type.CONTACTS, List.of(Action.Type.VIEW));
    }
}
