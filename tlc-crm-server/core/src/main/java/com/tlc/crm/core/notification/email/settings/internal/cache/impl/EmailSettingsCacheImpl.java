package com.tlc.crm.core.notification.email.settings.internal.cache.impl;

import com.tlc.cache.Cache;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.cache.remote.CacheConfig;

import com.tlc.crm.core.notification.email.settings.api.EmailSettingsManager;

import com.tlc.crm.email.notification.email.api.model.address.UserBlockedEmailAddress;
import com.tlc.crm.core.notification.email.settings.internal.cache.EmailSettingsCache;
import com.tlc.crm.email.notification.email.api.model.settings.EmailSettings;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Stores the {@link EmailSettings} details into cache
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 * @see EmailSettingsCache
 */
public class EmailSettingsCacheImpl implements EmailSettingsCache {

    private final Cache<String, EmailSettings> emailSettingsCache;
    private final EmailSettingsManager emailSettingsManager;

    private EmailSettingsCacheImpl() {
        final CacheConfig<String, EmailSettings> userEmailSettingsConfig = new CacheConfig<>("emailSettingsConfig");

        userEmailSettingsConfig.setTtl((int) TimeUnit.MINUTES.toMillis(5));
        emailSettingsCache = CacheServiceProvider.get().createOrOpenCache(userEmailSettingsConfig);
        emailSettingsManager = EmailSettingsManager.getInstance();
    }

    private static class Instance {
        private static final EmailSettingsCacheImpl INSTANCE = new EmailSettingsCacheImpl();
    }

    /**
     * <p>
     * Gets the {@link EmailSettingsCacheImpl} instance
     * </p>
     *
     * @return The {@link EmailSettingsCacheImpl}
     */
    public static EmailSettingsCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param emailSettings Represents the {@link EmailSettings}
     */
    @Override
    public void set(final EmailSettings emailSettings) {
        emailSettingsCache.put(generateKey(emailSettings.orgId(), emailSettings.getUserId()), emailSettings);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @return An Optional containing the {@link EmailSettings}
     */
    @Override
    public Optional<EmailSettings> get(final long orgId, final long userId) {
        return Optional.ofNullable(emailSettingsCache.get(generateKey(orgId, userId)));
    }

    /**
     * <p>
     * Adds the {@link EmailSettings} into cache for the given {@link EmailSettings}
     * </p>
     *
     * @param emailSettings Represents {@link EmailSettings}
     */
    public void addEmailSettings(final EmailSettings emailSettings) {
        loadEmailSettings(emailSettings);
    }

    /**
     * <p>
     * Adds the {@link UserBlockedEmailAddress} into cache for the given {@link UserBlockedEmailAddress}
     * </p>
     *
     * @param userBlockedEmailAddress Represents the {@link UserBlockedEmailAddress}
     */
    public void addBlockedEmailAddress(final UserBlockedEmailAddress userBlockedEmailAddress) {
        loadBlockedEmailAddress(userBlockedEmailAddress, false);
    }

    /**
     * <p>
     * Removes the {@link UserBlockedEmailAddress} from the cache for the given {@link UserBlockedEmailAddress}
     * </p>
     *
     * @param userBlockedEmailAddress Represents the {@link UserBlockedEmailAddress}
     */
    public void removeBlockedAddress(final UserBlockedEmailAddress userBlockedEmailAddress) {
        loadBlockedEmailAddress(userBlockedEmailAddress, true);
    }

    /**
     * <p>
     * Loads blocked {@link UserBlockedEmailAddress} into cache, If the {@link UserBlockedEmailAddress} is marked as deleted,
     * it is removed from the cache
     * </p>
     *
     * @param userBlockedEmailAddress Represents the {@link UserBlockedEmailAddress}
     * @param isDeleted               A flag that indicated whether the given email address is deleted or not
     */
    private void loadBlockedEmailAddress(final UserBlockedEmailAddress userBlockedEmailAddress, final boolean isDeleted) {
        final long orgId = userBlockedEmailAddress.orgId();
        final long userId = userBlockedEmailAddress.getUserId();
        final String key = generateKey(userBlockedEmailAddress.orgId(), userBlockedEmailAddress.getUserId());
        final EmailSettings emailSettings = emailSettingsCache.get(key);

        if (Objects.nonNull(emailSettings)) {

            if (isDeleted) {
                emailSettingsCache.remove(key);
            } else {
                final Collection<UserBlockedEmailAddress> emailAddressesBlockedByUser = emailSettings.getUserBlockedEmailAddresses();

                if (Objects.isNull(emailAddressesBlockedByUser) || emailAddressesBlockedByUser.isEmpty()) {
                    final Collection<UserBlockedEmailAddress> userBlockedEmailAddresses = emailSettingsManager.getBlockedAddresses(orgId, userId);

                    if (Objects.nonNull(userBlockedEmailAddresses) && !userBlockedEmailAddresses.isEmpty()) {
                        emailSettings.setUserBlockedEmailAddresses(userBlockedEmailAddresses);
                    }
                } else {
                    emailAddressesBlockedByUser.add(userBlockedEmailAddress);
                }
                set(emailSettings);
            }
        }
    }

    /**
     * <p>
     * Loads the {@link EmailSettings} into cache
     * </p>
     *
     * @param emailSettings Represents the {@link EmailSettings}
     */
    private void loadEmailSettings(final EmailSettings emailSettings) {
        final Optional<EmailSettings> existingEmailSettings = get(emailSettings.orgId(), emailSettings.getUserId());

        if (existingEmailSettings.isPresent()) {
            final Collection<UserBlockedEmailAddress> userBlockedEmailAddresses = existingEmailSettings.get().getUserBlockedEmailAddresses();

            if (Objects.nonNull(userBlockedEmailAddresses) && !userBlockedEmailAddresses.isEmpty()) {
                emailSettings.setUserBlockedEmailAddresses(userBlockedEmailAddresses);
            }
        }
        set(emailSettings);
    }

    /**
     * <p>
     * Generates a unique key based on the provided organization id and user id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @return A unique key string combining the organization id and user id
     */
    private String generateKey(final long orgId, final long userId) {
        return String.format("%d%s%d", orgId, "_", userId);
    }
}

