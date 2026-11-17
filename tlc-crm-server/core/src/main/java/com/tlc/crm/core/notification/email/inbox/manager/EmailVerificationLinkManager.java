package com.tlc.crm.core.notification.email.inbox.manager;

import com.tlc.crm.core.notification.email.inbox.api.model.EmailVerificationLink;
import com.tlc.crm.core.notification.email.inbox.manager.impl.EmailVerificationLinkManagerImpl;

import java.util.Optional;

/**
 * <p>
 * Provides methods to create, update, and retrieve {@link EmailVerificationLink} instances.
 * This interface defines the contract for managing email links.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 * @see EmailVerificationLink
 * @see EmailVerificationLinkManagerImpl
 */
public interface EmailVerificationLinkManager {

    /**
     * <p>
     * Retrieves the singleton instance of {@link EmailVerificationLinkManager}.
     * </p>
     *
     * @return the singleton manager instance
     */
    static EmailVerificationLinkManager get() {
        return EmailVerificationLinkManagerImpl.getInstance();
    }

    /**
     * <p>
     * Persists the given {@link EmailVerificationLink}. This may include creating
     * a new entry or updating an existing one depending on the implementation.
     * </p>
     *
     * @param emailVerificationLink the verification link to save; must not be {@code null}
     */
    void save(final EmailVerificationLink emailVerificationLink);

    /**
     * <p>
     * Retrieves an {@link EmailVerificationLink} associated with the given tracking ID.
     * </p>
     *
     * @param trackingId the tracking identifier used to locate the verification link
     * @return an {@link Optional} containing the matching {@link EmailVerificationLink},
     * or an empty {@link Optional} if none is found
     */
    Optional<EmailVerificationLink> get(final String trackingId);
}