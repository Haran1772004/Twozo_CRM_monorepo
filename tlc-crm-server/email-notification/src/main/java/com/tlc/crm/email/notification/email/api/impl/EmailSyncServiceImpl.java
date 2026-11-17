package com.tlc.crm.email.notification.email.api.impl;

import com.tlc.commons.pair.Pair;

import com.tlc.crm.email.notification.email.EmailSyncService;
import com.tlc.crm.email.notification.email.sync.api.EmailSyncManager;
import com.tlc.crm.email.notification.email.sync.api.model.EmailSyncSettings;
import com.tlc.crm.email.notification.email.api.model.config.EmailSyncConfig;
import com.tlc.crm.email.notification.model.UserSyncAddressInfo;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import static com.tlc.crm.email.notification.email.sync.api.model.EmailSyncSettings.SyncInterval.LAST_ONE_MONTH;

/**
 * <p>
 * Provides the service for email sync
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class EmailSyncServiceImpl implements EmailSyncService {

    private static final short ZERO = 0;

    private final EmailSyncManager emailSyncManager;

    public EmailSyncServiceImpl() {
        this.emailSyncManager = EmailSyncManager.getInstance();
    }

    private static class Instance {
        private static final EmailSyncServiceImpl INSTANCE = new EmailSyncServiceImpl();
    }

    /**
     * <p>
     * Gets the {@link EmailSyncServiceImpl} Instance
     * </p>
     *
     * @return {@link EmailSyncServiceImpl}
     */
    public static EmailSyncServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a user id
     * @return Pair of sender id and email address
     */
    @Override
    public Pair<Long, UserSyncAddressInfo> getSenderInfo(final Long orgId, final Long userId) {
        return emailSyncManager.getSenderInfo(orgId, userId);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents a user id
     * @param conversationId Represents the conversation id
     * @return Pair of sender id and email address
     * @pa
     */
    @Override
    public Pair<Long, UserSyncAddressInfo> getSenderInfo(final Long orgId, final Long userId, final Long conversationId) {
        return emailSyncManager.getSenderInfo(orgId, userId, conversationId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param emailIds which holds the {@link Collection} of email ids
     * @return The {@link Collection} of user ids
     */
    @Override
    public Collection<Long> getUserIds(final long orgId, final Collection<Long> emailIds) {
        return emailSyncManager.getSyncedUserIds(orgId, emailIds);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @return the {@link UserSyncAddressInfo}
     */
    public Optional<UserSyncAddressInfo> getSyncEmailAddress(final Long orgId, final Long userId) {
        return emailSyncManager.getSyncEmailAddress(orgId, userId);
    }
}
