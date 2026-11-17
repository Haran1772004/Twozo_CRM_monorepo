package com.tlc.crm.notifications.internal.event.handler;

import com.tlc.crm.common.iam.api.model.CrmUser;

import java.util.Collection;
import java.util.Set;

/**
 * Strategy interface for handling user update events for a specific telephony provider.
 * Each telephony provider (e.g., TeleCMI, MCube) implements this interface to encapsulate
 * its own user synchronization logic.
 *
 * <p>New providers can be added by implementing this interface and registering the handler,
 * without modifying existing event listener code.</p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface TelephoneUserUpdateHandler {

    /**
     * Handles user deactivation by removing the specified users from the telephony provider
     * and clearing their configuration.
     *
     * @param orgId              the organization identifier
     * @param deactivatedUserIds the set of deactivated user identifiers
     */
    void handleDeactivatedUsers(final long orgId, final Set<Long> deactivatedUserIds);

    /**
     * Synchronizes updated CRM user details with the telephony provider, removing users
     * with invalid data and updating the rest.
     *
     * @param orgId         the organization identifier
     * @param crmUsers      the collection of updated CRM users
     * @param previousUsers the collection of previous CRM user states before update
     */
    void handleUserUpdate(final long orgId, final Collection<CrmUser> crmUsers, final Collection<CrmUser> previousUsers);
}