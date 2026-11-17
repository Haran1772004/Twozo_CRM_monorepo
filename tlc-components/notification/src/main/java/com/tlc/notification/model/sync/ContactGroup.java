package com.tlc.notification.model.sync;

import com.tlc.sync.service.oauth.model.provider.ContactProvider;

/**
 * <p>
 * Represents the contact group
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public interface ContactGroup {

    int DEFAULT_GROUP = 1;

    /**
     * <p>
     * Gets the group id.
     * </p>
     *
     * @return the group id.
     */
    int getId();

    /**
     * <p>
     * Gets the {@link ContactGroup}.
     * </p>
     *
     * @return {@link ContactGroup}.
     */
    static ContactGroup get(final ContactProvider contactProvider, final int id) {
        return switch (contactProvider) {
            case GOOGLE -> GmailContactGroup.valueOf(id);
            case OUTLOOK -> OutlookContactGroup.valueOf(id);
            default -> null;
        };
    }

    /**
     * <p>
     * Gets the default {@link ContactGroup}.
     * </p>
     *
     * @return {@link ContactGroup}.
     */
    static ContactGroup getDefaultGroup(final ContactProvider contactProvider) {
        return switch (contactProvider) {
            case GOOGLE -> GmailContactGroup.valueOf(DEFAULT_GROUP);
            case OUTLOOK -> OutlookContactGroup.valueOf(DEFAULT_GROUP);
        };
    }
}