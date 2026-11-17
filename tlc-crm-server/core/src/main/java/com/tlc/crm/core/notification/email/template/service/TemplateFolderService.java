package com.tlc.crm.core.notification.email.template.service;

import com.tlc.crm.core.notification.email.template.api.model.EntityMailTemplate;
import com.tlc.crm.core.notification.email.template.api.model.TemplateFolder;
import com.tlc.crm.core.notification.email.template.api.model.TemplateFolder.FilterAction;
import com.tlc.crm.core.notification.email.template.service.impl.TemplateFolderServiceImpl;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Provides the services for template folder
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public interface TemplateFolderService {

    /**
     * Gets instance of {@link TemplateFolderService}
     *
     * @return {@link TemplateFolderService} instance
     */
    static TemplateFolderService getInstance() {
        return TemplateFolderServiceImpl.getInstance();
    }

    /**
     * <p>
     * Checks whether any template folder is exists with one of the given names
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param folderNames Represents the {@link Collection} of folder names
     * @return True if {@link TemplateFolder} names are exists, otherwise returns false
     */
    boolean checkTemplateFoldersAreExists(final long orgId, final Collection<String> folderNames);

    /**
     * <p>
     * Checks whether the template folder id is exists or not
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param folderId Represents the template folder id
     * @return True if {@link TemplateFolder} id are exists, otherwise returns false
     */
    boolean exists(final long orgId, final Long folderId);

    /**
     * <p>
     * Checks whether the template folder ids are exists or not
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param folderIds Represents the {@link Collection} of {@link TemplateFolder} ids
     * @return True if {@link TemplateFolder} ids are exists, otherwise returns false
     */
    boolean exists(final long orgId, final Collection<Long> folderIds);

    /**
     * <p>
     * Fetches the {@link TemplateFolder} for a specific organization based on folder id
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param folderId Represents the template folder id
     * @return The {@link TemplateFolder}
     */
    TemplateFolder get(final long orgId, final Long folderId);

    /**
     * <p>
     * Fetches the collection of {@link TemplateFolder} for a specific organization based on folder ids
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param folderIds Represents the {@link Collection} of {@link TemplateFolder} ids
     * @return The collection of {@link TemplateFolder}
     */
    Collection<TemplateFolder> get(final long orgId, final Collection<Long> folderIds);

    /**
     * <p>
     * Populates the users, teams, and territories associated with a specific template folder
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param templateFolder Represents the {@link TemplateFolder} that holds the template folder attributes
     */
    void populateFolderUsers(final long orgId, final TemplateFolder templateFolder);

    /**
     * <p>
     * Updates the visibility of a template folder within an organization
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param templateFolder Represents the {@link TemplateFolder} that holds the template folder attributes
     * @return A {@link Map} which keys are status message and values are update operation details
     */
    Map<String, Collection<Long>> updateFolderVisibility(final long orgId, final TemplateFolder templateFolder);

    /**
     * <p>
     * Retrieves the associated user ids from a shared folder, considering users, teams, and territories
     * associated with the given TemplateFolder
     * </p>
     *
     * @param templateFolder    Represents the {@link TemplateFolder}
     * @param associatedUserIds Represents the {@link Collection} of associated user ids
     * @return the {@link Collection} of user ids associated with template
     */
    Collection<Long> retrieveAssociatedUserIdsFromSharedFolder(final TemplateFolder templateFolder,
                                                               final Collection<Long> associatedUserIds);

    /**
     * <p>
     * Returns the total count of user accessible template folders matching the given filter action and search text.
     * Used to compute accurate pagination metadata (hasMore, totalRecords).
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param userId       Represents the user id
     * @param filterAction Represents the {@link FilterAction}
     * @param searchText   Represents the text to search for (folder name / template name / tag name prefix)
     * @return The total count of matching {@link TemplateFolder}
     */
    long getUserAccessibleFolderCount(final long orgId, final long userId, final FilterAction filterAction,
                                      final String searchText);

    /**
     * <p>
     * Retrieves a collection of user accessible template folders based on the filter action within the given organization
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param userId       Represents the user id
     * @param filterAction Represents the {@link FilterAction}
     * @param searchText   Represents the text to search for within folder names
     * @param start        Represents the starting index of {@link TemplateFolder} to be fetched
     * @param limit        Represents the maximum number of {@link TemplateFolder} to be fetched
     * @return The {@link Collection} of {@link TemplateFolder}
     */
    Collection<TemplateFolder> fetchUserAccessibleFolders(final long orgId, final long userId, final FilterAction filterAction,
                                                          final String searchText, final int start, final int limit);

    /**
     * <p>
     * Retrieves a collection of {@link EntityMailTemplate} based on the filter action within the given organization
     *
     * @param orgId        Represents an organization id
     * @param userId       Represents the user id
     * @param start        Represents the starting index of {@link EntityMailTemplate} to be fetched
     * @param limit        Represents the maximum number of {@link EntityMailTemplate} to be fetched
     * @param filterAction Represents the {@link FilterAction}
     * @param searchText   Represents the text to search for within folder names
     * @return The {@link Collection} of {@link EntityMailTemplate}
     */
    Collection<EntityMailTemplate> getTemplates(final long orgId, final long userId, final int start, final int limit, final FilterAction filterAction,
                                                final String searchText);
}