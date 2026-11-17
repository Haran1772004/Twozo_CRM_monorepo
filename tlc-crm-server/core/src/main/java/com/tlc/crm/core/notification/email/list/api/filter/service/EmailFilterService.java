package com.tlc.crm.core.notification.email.list.api.filter.service;

import com.tlc.crm.core.notification.email.list.api.filter.FilterOption;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.Folder;

import java.util.Map;

/**
 * <p>
 * Defines the contract for managing email list filter configurations and preferences.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface EmailFilterService {

    /**
     * <p>
     * Returns the singleton instance of the {@link EmailFilterService}.
     * </p>
     *
     * @return The {@link EmailFilterService} instance.
     */
    static EmailFilterService get() {
        return EmailFilterServiceImpl.getInstance();
    }

    /**
     * <p>
     * Gets the existing email list filter
     * </p>
     *
     * @param orgId  Represents an Organization ID
     * @param userId Represents a user ID
     * @param folder Represents the {@link Folder}
     */
    EmailListFilter getExistingEmailListFilter(final long orgId, final long userId, final Folder folder);

    /**
     * <p>
     * Updates the filter attribute configuration of the specified {@link EmailListFilter}.
     * </p>
     *
     * @param emailFilter The email filter whose filter attribute configuration is to be updated.
     */
    void updateFilterAttributeConfig(final EmailListFilter emailFilter);

    /**
     * <p>
     * Retrieves the default filter configurations for a user within a given organization and folder.
     * </p>
     *
     * @param orgId  The organization identifier.
     * @param userId The user identifier.
     * @param folder The {@link Folder} representing the email folder.
     * @return A map of {@link EmailListFilter.FilterAttribute} to {@link FilterOption} representing the default filter configurations.
     */
    Map<EmailListFilter.FilterAttribute, FilterOption> fetchDefaultFilterConfig(final long orgId, final long userId,
                                                                                final Folder folder);
}