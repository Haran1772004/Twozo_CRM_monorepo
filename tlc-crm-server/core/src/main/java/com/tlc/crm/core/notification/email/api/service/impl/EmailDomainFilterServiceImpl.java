package com.tlc.crm.core.notification.email.api.service.impl;

import com.tlc.crm.core.notification.email.api.manager.EmailDomainFilterManager;
import com.tlc.crm.core.notification.email.api.manager.impl.EmailDomainFilterManagerImpl;
import com.tlc.crm.core.notification.email.api.model.email.EmailDomainFilterPreference;
import com.tlc.crm.core.notification.email.api.service.EmailDomainFilterService;

import java.util.Optional;

/**
 * <p>
 * Implementation of {@link EmailDomainFilterService}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailDomainFilterServiceImpl implements EmailDomainFilterService {

    private final EmailDomainFilterManager emailDomainFilterManager;

    private static class Instance {
        private static final EmailDomainFilterService INSTANCE = new EmailDomainFilterServiceImpl();
    }

    /**
     * <p>
     * Returns the singleton instance of {@link EmailDomainFilterService}.
     * </p>
     *
     * @return The {@link EmailDomainFilterService} instance.
     */
    public static EmailDomainFilterService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Private constructor to initialize the underlying {@link EmailDomainFilterManager}.
     * </p>
     */
    private EmailDomainFilterServiceImpl() {
        this.emailDomainFilterManager = EmailDomainFilterManagerImpl.getInstance();
    }

    /**
     * <p>
     * Saves or updates the filter preference for a user in the specified organization.
     * </p>
     *
     * @param orgId      The organization ID.
     * @param preference The {@link EmailDomainFilterPreference} to save or update.
     */
    @Override
    public void save(final Long orgId, final EmailDomainFilterPreference preference) {
        emailDomainFilterManager.save(orgId, preference);
    }

    /**
     * <p>
     * Retrieves the filter preference for a user in the specified organization.
     * </p>
     *
     * @param orgId  The organization ID.
     * @param userId The user ID whose filter preference is being retrieved.
     * @return An {@link Optional} containing the {@link EmailDomainFilterPreference} if found, otherwise empty.
     */
    @Override
    public Optional<EmailDomainFilterPreference> get(final Long orgId, final Long userId) {
        return emailDomainFilterManager.get(orgId, userId);
    }

    /**
     * <p>
     * Deletes the filter preference for a user in the specified organization.
     * </p>
     *
     * @param orgId  The organization ID.
     * @param userId The user ID whose filter preference is to be deleted.
     */
    @Override
    public void delete(final Long orgId, final Long userId) {
        emailDomainFilterManager.delete(orgId, userId);
    }
}