package com.tlc.crm.core.mcp.tools.schema.contact;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.mcp.CrmMcpIdentity;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
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
 * MCP Tool for deleting contact from CRM.
 * Accepts a contact ID and removes the contact from the system.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@McpTool(
        name = "delete_contact",
        title = "Delete Contact",
        description = "Delete a contact from the CRM system. This action is permanent and cannot be undone.",
        version = "1.0",
        destructive = true
)
public class DeleteContactTool extends CrmSecureMcpTool<Contact> {

    private final ContactManager contactManager;

    public DeleteContactTool() {
        this.contactManager = ContactManager.getInstance();
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
    public McpToolResult execute(final McpToolContext<CrmMcpIdentity> ctx,
                                 @ToolParam(description = "Contact ID to delete", required = true) final Long id,
                                 @ToolParam(description = "Must be true to confirm deletion (safety check)", required = true,
                                         options = {"true", "false"}) final boolean confirmDelete) {
        try {
            if (id == null || id <= 0) {
                return errorResult("Invalid contact ID");
            }

            if (!confirmDelete) {
                return errorResult("Deletion must be explicitly confirmed. Set confirmDelete to true.");
            }

            final CrmUser crmUser = ctx.identity().crmUser();
            final long orgId = crmUser.orgId();
            final long userId = crmUser.id();

            if (!contactManager.exists(orgId, id)) {
                return errorResult("Contact not found");
            }

            final EventData eventData = new SecureEventData(orgId, userId);
            final Contact contact = contactManager.get(orgId, id);

            eventData.setLocale(Locale.getDefault());
            eventData.setPreviousValues(List.of(contact));
            final JsonObject jsonObject = delete(orgId, userId, eventData, List.of(contact));

            if (Objects.equals("error", jsonObject.optString("status", ""))) {
                return McpToolResult.error(jsonObject.optString("message", "Unknown error"));
            } else {
                return McpToolResult.json(jsonObject);
            }
        } catch (Exception e) {
            LOGGER.error("MCP: Contact delete action failed. Reason: {}", e.getMessage(), e);
            return errorResult("Failed to delete contact");
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
        return isAuthorized(ctx) && hasPermission(ctx, SubCategory.Type.CONTACTS, List.of(Type.DELETE, Type.VIEW));
    }

}
