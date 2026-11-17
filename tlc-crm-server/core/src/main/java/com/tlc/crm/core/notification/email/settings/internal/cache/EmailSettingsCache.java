package com.tlc.crm.core.notification.email.settings.internal.cache;

import com.tlc.crm.core.notification.email.settings.internal.cache.impl.EmailSettingsCacheImpl;

import com.tlc.crm.email.notification.email.api.model.settings.EmailSettings;

import java.util.Optional;

/**
 * <p>
 * Provides a service to fetch {@link EmailSettings} from the cache
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface EmailSettingsCache {

    /**
     * <p>
     * Gets the {@link EmailSettingsCache} instance
     * </p>
     *
     * @return The {@link EmailSettingsCacheImpl}
     */
    static EmailSettingsCache getInstance() {
        return EmailSettingsCacheImpl.getInstance();
    }

    /**
     * <p>
     * Sets the email settings into cache for the given {@link EmailSettings}
     * </p>
     *
     * @param emailSettings Represents the {@link EmailSettings}
     */
    void set(final EmailSettings emailSettings);

    /**
     * <p>
     * Gets the {@link EmailSettings} for the given org id, user id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @return An Optional containing the {@link EmailSettings}
     */
    Optional<EmailSettings> get(final long orgId, final long userId);
}
