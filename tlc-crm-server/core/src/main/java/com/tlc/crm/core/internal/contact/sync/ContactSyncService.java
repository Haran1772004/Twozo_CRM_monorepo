package com.tlc.crm.core.internal.contact.sync;

import com.tlc.commons.json.JsonArray;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.ContactSyncSettings;
import com.tlc.crm.core.internal.contact.sync.impl.ContactSyncServiceImpl;
import com.tlc.crm.entity.async.SyncInfo;
import com.tlc.notification.model.contact.Person;
import com.tlc.notification.model.email.EmailAddress;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Provides the services for contact sync
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface ContactSyncService {

    /**
     * Gets instance of {@link ContactSyncService}.
     *
     * @return {@link ContactSyncService} instance
     */
    static ContactSyncService getInstance() {
        return ContactSyncServiceImpl.getInstance();
    }

    /**
     * <p>
     * Gets the synced contact email address
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return the {@link EmailAddress}
     */
    EmailAddress getSyncedAddress(final long orgId, final long userId);

    /**
     * <p>
     * Gets the external person id from the provider
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param id      Represents the contact id
     * @return the contact label
     */
    String getExternalPersonId(final long orgId, final Long id);

    /**
     * <p>
     * Pull and stores the contacts from the provider
     * </p>
     */
    void pullContactsFromProvider();

    /**
     * <p>
     * Pushes the contacts to the provider
     * </p>
     *
     * @param contactSyncSettings Represents {@link ContactSyncSettings}
     */
    void pushContactsToProvider(final ContactSyncSettings contactSyncSettings);

    /**
     * <p>
     * Converts the contact to person model
     * </p>
     *
     * @param contact Represents the {@link Contact}
     * @param orgId   Represents an organization id
     * @return {@link Person}
     */
    Person convertContactToPerson(final Contact contact, final Long orgId);

    /**
     * <p>
     * Checks whether the email is synced for the contact synchronization
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an owner id
     * @return true, if contact is synced
     */
    boolean isSyncEnabled(final long orgId, final long userId);

    /**
     * <p>
     * Builds the contacts based on the given request params
     * </p>
     *
     * @param contactsArray Holds contacts in {@link JsonArray}.
     * @return contacts in a {@link Collection}.
     */
    Collection<Contact> convertJsonToModels(final JsonArray contactsArray);

    /**
     * <p>
     * Fetches the external person id for the respective contact id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param ids    Represents the contact ids
     * @return the external person id with contact id
     */
    Map<Long, String> fetchExternalPersonIds(final long orgId, final long userId, final Collection<Long> ids);

    /**
     * <p>
     * Disables and terminates contact synchronization for a specific user and organization.
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents a user id
     */
    void stopSync(final long orgId, final long userId);

    /**
     * <p>
     * Updates the contact sync status fo the given sync id
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents the user id
     * @param syncInfo Represents the {@link SyncInfo}
     */
    void updateContactSyncStatus(final long orgId, final long userId, final SyncInfo syncInfo);

    /**
     * <p>
     * Gets the contact {@link SyncInfo} for an respective organization and sync id.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the contact sync id
     * @return {@link SyncInfo}
     */
    SyncInfo getContactSyncInfo(final long orgId, final Long syncId);

    /**
     * <p>
     * Loads the contacts based on the given settings
     * </p>
     *
     * @param contactSyncSettings Represents the {@link ContactSyncSettings}
     */
    void loadContacts(final ContactSyncSettings contactSyncSettings);
}