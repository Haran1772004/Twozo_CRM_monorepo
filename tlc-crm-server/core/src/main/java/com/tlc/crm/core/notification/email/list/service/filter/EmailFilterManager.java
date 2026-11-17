package com.tlc.crm.core.notification.email.list.service.filter;

import com.tlc.crm.core.notification.email.list.api.filter.EmailFilterManagerImpl;
import com.tlc.crm.core.notification.email.list.api.filter.FilterOption;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.FilterAttribute;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.Folder;

import java.util.Map;

/**
 * <p>
 * Provides the services for managing email filters.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface EmailFilterManager {

    /**
     * <p>
     * Gets an instance of the {@link EmailFilterManager}.
     * </p>
     *
     * @return an instance of the EmailFilterManager
     */
    static EmailFilterManager getInstance() {
        return EmailFilterManagerImpl.getInstance();
    }

    /**
     * <p>
     * Updates the filter attribute configuration of the specified email filter.
     * </p>
     *
     * @param emailFilter Represents the {@link EmailListFilter}
     */
    void updateFilterAttributeConfig(final EmailListFilter emailFilter);

    /**
     * <p>
     * Retrieves the default filter configurations based on the specified user and organization.
     * </p>
     *
     * @param orgId  Represents an organization
     * @param userId Represents the user
     * @param folder Represents the {@link Folder} of email
     * @return the user filter preferences with filter attributes
     */
    Map<FilterAttribute, FilterOption> fetchDefaultFilterConfig(final long orgId, final long userId, final Folder folder);
}
