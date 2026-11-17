package com.tlc.crm.notifications.api.call.manager;

import com.tlc.crm.core.api.contact.model.Contact;

import com.tlc.crm.notifications.api.call.manager.impl.CallManagerImpl;
import com.tlc.crm.notifications.api.call.model.InboundDetails;
import com.tlc.crm.notifications.api.call.model.PhoneNumber;
import com.tlc.crm.notifications.api.call.model.TelephoneConfig;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Manages the call-related DB actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface CallManager {

    static CallManager getInstance() {
        return CallManagerImpl.getInstance();
    }

    /**
     * Retrieves inbound call details for a given phone number and a collection of caller contacts.
     *
     * @param callerContacts A collection of contacts associated with the inbound call.
     * @param phoneNumber The phone number of the caller.
     * @return An {@link InboundDetails} object containing information about the inbound call.
     */
    InboundDetails getInboundDetails(final Collection<Contact> callerContacts, final PhoneNumber phoneNumber);

    /**
     * <p>
     * Fetches the telephony configurations for the given organization and user
     * </p>
     *
     * @param orgId  the organization ID
     * @param userId the user ID
     * @return an {@link Optional} containing the {@link TelephoneConfig} if found, otherwise empty
     */
    Collection<TelephoneConfig> getTelephonyConfigs(final long orgId, final long userId);
}
