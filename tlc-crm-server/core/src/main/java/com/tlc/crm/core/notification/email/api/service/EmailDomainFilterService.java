package com.tlc.crm.core.notification.email.api.service;

import com.tlc.crm.core.notification.email.api.model.email.EmailDomainFilterPreference;
import com.tlc.crm.core.notification.email.api.service.impl.EmailDomainFilterServiceImpl;

import java.util.Optional;

/**
 * <p>
 * Service for managing Email Domain filter preferences.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface EmailDomainFilterService {

    /**
     * <p>
     * Returns the singleton instance of {@link EmailDomainFilterService}.
     * </p>
     *
     * @return The {@link EmailDomainFilterService} instance.
     */
    static EmailDomainFilterService getInstance() {
        return EmailDomainFilterServiceImpl.getInstance();
    }

    /**
     * <p>
     * Saves or updates the filter preference for a user in the specified organization.
     * </p>
     *
     * @param orgId                       The organization ID.
     * @param emailDomainFilterPreference The {@link EmailDomainFilterPreference} to save or update.
     */
    void save(final Long orgId, final EmailDomainFilterPreference emailDomainFilterPreference);

    /**
     * <p>
     * Retrieves the filter preference for a user in the specified organization.
     * </p>
     *
     * @param orgId  The organization ID.
     * @param userId The user ID whose filter preference is being retrieved.
     * @return An {@link Optional} containing the {@link EmailDomainFilterPreference} if found, otherwise empty.
     */
    Optional<EmailDomainFilterPreference> get(final Long orgId, final Long userId);

    /**
     * <p>
     * Deletes the filter preference for a user in the specified organization.
     * </p>
     *
     * @param orgId  The organization ID.
     * @param userId The user ID whose filter preference is to be deleted.
     */
    void delete(final Long orgId, final Long userId);
}