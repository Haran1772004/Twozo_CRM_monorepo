package com.tlc.crm.email.notification.email;

import com.tlc.commons.pair.Pair;

import com.tlc.crm.email.notification.email.api.impl.EmailSyncServiceImpl;

import com.tlc.crm.email.notification.model.UserSyncAddressInfo;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Provides the service for email sync
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface EmailSyncService {

    /**
     * <p>
     * Fetches the {@link EmailSyncService} instance
     * </p>
     *
     * @return the instance of {@link EmailSyncService}
     */
    static EmailSyncService getInstance() {
        return EmailSyncServiceImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the sender information for the provided org id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @return {@link Pair} of sender id and email address
     */
    Pair<Long, UserSyncAddressInfo> getSenderInfo(final Long orgId, final Long userId);

    /**
     * <p>
     * Fetches the sender information for the provided org id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @param conversationId Represents the conversationId
     * @return {@link Pair} of sender id and email address
     */
    Pair<Long, UserSyncAddressInfo> getSenderInfo(final Long orgId, final Long userId, final Long conversationId);

    /**
     * <p>
     * Retrieves the synced email user ids for a given organization
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param emailIds which holds the {@link Collection} of email ids
     * @return The {@link Collection} of user ids
     */
    Collection<Long> getUserIds(final long orgId, final Collection<Long> emailIds);

    /**
     * <p>
     * Gets the optional of sync email address
     * </p>
     *
     * @param orgId  Represents the org id
     * @param userId Represents the user id
     * @return the optional of {@link UserSyncAddressInfo}
     */
    Optional<UserSyncAddressInfo> getSyncEmailAddress(final Long orgId, final Long userId);
}
