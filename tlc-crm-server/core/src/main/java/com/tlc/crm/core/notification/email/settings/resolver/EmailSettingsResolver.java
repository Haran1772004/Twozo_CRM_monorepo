package com.tlc.crm.core.notification.email.settings.resolver;

import com.tlc.crm.core.notification.email.settings.resolver.impl.EmailSettingsResolverImpl;

import com.tlc.crm.email.notification.email.api.model.settings.EmailSettings;

import java.util.Optional;

/**
 * <p>
 * A resolver that resolves the {@link EmailSettings} related details
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface EmailSettingsResolver {

    /**
     * <p>
     * Gets the {@link EmailSettingsResolver} instance
     * </p>
     *
     * @return The {@link EmailSettingsResolver}
     */
    static EmailSettingsResolver getInstance() {
        return EmailSettingsResolverImpl.getInstance();
    }

    /**
     * <p>
     * Gets the {@link EmailSettings} for the given org id, user id.
     * If the email settings are not cached, it retrieves the data from the database, caches it, and returns organization
     * email settings from the cache
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @return An Optional containing the {@link EmailSettings}
     */
    Optional<EmailSettings> get(final long orgId, final long userId);
}
