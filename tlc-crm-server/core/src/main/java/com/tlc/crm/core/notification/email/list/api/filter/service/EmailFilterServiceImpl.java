package com.tlc.crm.core.notification.email.list.api.filter.service;

import com.tlc.crm.core.notification.email.list.api.filter.FilterOption;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.Folder;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.FilterAttribute;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.LinkedCategory;
import com.tlc.crm.core.notification.email.list.service.filter.EmailFilterManager;
import com.tlc.crm.core.notification.email.api.service.TeamInboxService;

import java.util.Map;

/**
 * <p>
 * Provides an implementation of {@link EmailFilterService} that manages
 * email filter attributes, validates filter configurations, and interacts
 * with the {@link EmailFilterManager} and {@link TeamInboxService}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailFilterServiceImpl implements EmailFilterService {

    private final EmailFilterManager emailFilterManager;

    private EmailFilterServiceImpl() {
        this.emailFilterManager = EmailFilterManager.getInstance();
    }

    private static class Instance {
        private static final EmailFilterService INSTANCE = new EmailFilterServiceImpl();
    }

    /**
     * <p>
     * Gets the singleton instance of {@link EmailFilterServiceImpl}.
     * </p>
     *
     * @return {@link EmailFilterService} instance
     */
    public static EmailFilterService getInstance() {
        return EmailFilterServiceImpl.Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the existing email list filter based on folder
     * </p>
     *
     * @param orgId  Represents an Organization ID
     * @param userId Represents a user ID
     * @param folder Represents the {@link Folder}
     */
    @Override
    public EmailListFilter getExistingEmailListFilter(final long orgId, final long userId, final Folder folder) {
        final Map<FilterAttribute, FilterOption> filterAttributes = fetchDefaultFilterConfig(orgId, userId, folder);

        if (!filterAttributes.isEmpty()) {
            final FilterAttribute linkedCategory = FilterAttribute.LINKED_CATEGORY;
            final EmailListFilter emailFilter = new EmailListFilter();

            emailFilter.setUserId(userId);
            emailFilter.setOrgId(orgId);
            emailFilter.setFilterAttribute(filterAttributes);

            if (filterAttributes.containsKey(linkedCategory)) {
                emailFilter.setLinkedCategory((LinkedCategory) filterAttributes.get(linkedCategory));
            }
            emailFilter.setConfigured(true);
            emailFilter.setFolder(folder);

            return emailFilter;
        }

        return null;
    }

    /**
     * <p>
     * Updates the filter attribute configuration of the specified email filter.
     * </p>
     *
     * @param emailFilter Represents the {@link EmailListFilter} to be updated
     */
    @Override
    public void updateFilterAttributeConfig(final EmailListFilter emailFilter) {
        emailFilterManager.updateFilterAttributeConfig(emailFilter);
    }

    /**
     * <p>
     * Retrieves the default filter configurations based on the specified
     * organization, user, and folder.
     * </p>
     *
     * @param orgId  Represents the organization identifier
     * @param userId Represents the user identifier
     * @param folder Represents the {@link Folder} of email
     * @return A map of filter attributes with their corresponding filter options
     */
    @Override
    public Map<FilterAttribute, FilterOption> fetchDefaultFilterConfig(final long orgId, final long userId,
                                                                       final Folder folder) {
        return emailFilterManager.fetchDefaultFilterConfig(orgId, userId, folder);
    }
}