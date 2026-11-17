package com.tlc.crm.core.notification.email.settings.service;

import com.tlc.crm.core.notification.email.settings.service.impl.EmailSettingsServiceImpl;

import com.tlc.crm.email.notification.email.api.model.settings.EmailLinkerSettings;
import com.tlc.crm.email.notification.email.api.model.settings.EmailSettings;
import com.tlc.crm.email.notification.email.api.model.settings.EmailTrackerSettings;
import com.tlc.crm.email.notification.email.api.model.address.UserBlockedEmailAddress;

import java.util.Collection;

/**
 * <p>
 * Provides an abstract way to deal with {@link EmailSettings}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface EmailSettingsService {

    /**
     * <p>
     * Gets the {@link EmailSettingsService} instance
     * </p>
     *
     * @return The {@link EmailSettingsService}
     */
    static EmailSettingsService getInstance() {
        return EmailSettingsServiceImpl.getInstance();
    }

    /**
     * <p>
     * Gets the {@link EmailSettings} for the given org id, user id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @return The {@link EmailSettings}
     */
    EmailSettings getEmailSettings(final long orgId, final long userId);

    /**
     * <p>
     * Fetches a email signature for the given org id, user id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @return The email signature
     */
    String fetchEmailSignature(final long orgId, final long userId);

    /**
     * <p>
     * Gets the {@link EmailTrackerSettings} for the given org id, user id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @return The {@link EmailTrackerSettings}
     */
    EmailTrackerSettings getEmailTrackerSettings(final long orgId, final long userId);

    /**
     * <p>
     * Gets the {@link EmailLinkerSettings} for the given org id, user id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @return The {@link EmailLinkerSettings}
     */
    EmailLinkerSettings getEmailLinkerSettings(final long orgId, final long userId);

    /**
     * <p>
     * Fetches a collection of user blocked email address for the given org id, user id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @return The collection of blocked email address
     */
    Collection<UserBlockedEmailAddress> getBlockedAddresses(final long orgId, final long userId);
}
