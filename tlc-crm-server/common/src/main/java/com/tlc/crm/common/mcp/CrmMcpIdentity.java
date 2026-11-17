package com.tlc.crm.common.mcp;

import com.tlc.crm.common.iam.api.model.CrmUser;

/**
 * Represents the authenticated CRM identity for an MCP request.
 * <p>
 * Encapsulates the authenticated {@link CrmUser} associated with the current
 * MCP session and is used to provide user context during tool execution.
 *
 * @param crmUser The authenticated CRM user.
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public record CrmMcpIdentity(CrmUser crmUser) {
}
