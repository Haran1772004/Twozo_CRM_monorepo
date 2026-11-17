package com.tlc.crm.core.notification.email.settings.service.impl;

import com.tlc.crm.core.notification.email.settings.service.EmailSettingsService;
import com.tlc.crm.core.notification.email.settings.resolver.EmailSettingsResolver;

import com.tlc.crm.email.notification.email.api.model.settings.EmailLinkerSettings;
import com.tlc.crm.email.notification.email.api.model.settings.EmailSettings;
import com.tlc.crm.email.notification.email.api.model.settings.EmailSignature;
import com.tlc.crm.email.notification.email.api.model.settings.EmailTrackerSettings;
import com.tlc.crm.email.notification.email.api.model.address.UserBlockedEmailAddress;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * <p>
 * Implements the {@link EmailSettingsService} provides the functionality for accessing the {@link EmailSettings}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 * @see EmailSettingsService
 */
public class EmailSettingsServiceImpl implements EmailSettingsService {

    private final EmailSettingsResolver emailSettingsResolver;

    private EmailSettingsServiceImpl() {
        emailSettingsResolver = EmailSettingsResolver.getInstance();
    }

    private static class Instance {
        private static final EmailSettingsServiceImpl INSTANCE = new EmailSettingsServiceImpl();
    }

    /**
     * <p>
     * Gets the {@link EmailSettingsServiceImpl} instance
     * </p>
     *
     * @return The {@link EmailSettingsServiceImpl}
     */
    public static EmailSettingsServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @return The {@link EmailSettings}
     */
    @Override
    public EmailSettings getEmailSettings(final long orgId, final long userId) {
        return emailSettingsResolver.get(orgId, userId).orElseGet(() -> null);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @return The email signature
     */
    @Override
    public String fetchEmailSignature(final long orgId, final long userId) {
        final EmailSignature emailSignature = getEmailSettings(orgId, userId).getEmailSignature();

        return Objects.isNull(emailSignature) ? null : emailSignature.getEmailSignature();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @return The {@link EmailTrackerSettings}
     */
    @Override
    public EmailTrackerSettings getEmailTrackerSettings(final long orgId, final long userId) {
        return getEmailSettings(orgId, userId).getEmailTrackerSettings();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @return The {@link EmailLinkerSettings}
     */
    @Override
    public EmailLinkerSettings getEmailLinkerSettings(final long orgId, final long userId) {
        return getEmailSettings(orgId, userId).getEmailLinkerSettings();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @return The collection of blocked email address
     */
    @Override
    public Collection<UserBlockedEmailAddress> getBlockedAddresses(final long orgId, final long userId) {
        final Collection<UserBlockedEmailAddress> userBlockedEmailAddresses = getEmailSettings(orgId, userId).getUserBlockedEmailAddresses();

        return Objects.isNull(userBlockedEmailAddresses) ? Collections.emptySet() : userBlockedEmailAddresses;
    }
}
