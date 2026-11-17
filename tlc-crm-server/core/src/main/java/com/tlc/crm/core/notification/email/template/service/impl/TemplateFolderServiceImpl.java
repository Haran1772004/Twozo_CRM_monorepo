package com.tlc.crm.core.notification.email.template.service.impl;

import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.core.notification.email.template.api.TemplateFolderManager;
import com.tlc.crm.core.notification.email.template.api.model.EntityMailTemplate;
import com.tlc.crm.core.notification.email.template.api.model.TemplateFolder;
import com.tlc.crm.core.notification.email.template.api.model.TemplateFolder.FilterAction;
import com.tlc.crm.core.notification.email.template.service.TemplateFolderService;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the service related to Template folder
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public class TemplateFolderServiceImpl implements TemplateFolderService {

    private final TemplateFolderManager templateFolderManager;
    private final TeamManager teamManager;
    private final TerritoryManager territoryManager;
    private final UserService crmUserService;

    public TemplateFolderServiceImpl() {
        this.templateFolderManager = TemplateFolderManager.getInstance();
        this.teamManager = TeamManager.getInstance();
        this.territoryManager = TerritoryManager.getInstance();
        this.crmUserService = UserService.getInstance();
    }

    private static class Instance {
        private static final TemplateFolderServiceImpl INSTANCE = new TemplateFolderServiceImpl();
    }

    /**
     * <p>
     * Gets instance of {@link TemplateFolderServiceImpl}.
     * </p>
     *
     * @return {@link TemplateFolderServiceImpl} instance
     */
    public static TemplateFolderServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param folderNames Represents the {@link Collection} of folder names
     * @return True if {@link TemplateFolder} names are exists, otherwise returns false
     */
    @Override
    public boolean checkTemplateFoldersAreExists(final long orgId, final Collection<String> folderNames) {
        return templateFolderManager.checkTemplateFoldersAreExists(orgId, folderNames);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param folderId Represents the template folder id
     * @return True if {@link TemplateFolder} id are exists, otherwise returns false
     */
    @Override
    public boolean exists(final long orgId, final Long folderId) {
        return templateFolderManager.exists(orgId, folderId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents an organization id
     * @param folderIds Represents the {@link Collection} of {@link TemplateFolder} ids
     * @return True if {@link TemplateFolder} names are exists, otherwise returns false
     */
    @Override
    public boolean exists(final long orgId, final Collection<Long> folderIds) {
        return templateFolderManager.exists(orgId, folderIds);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param folderId Represents the template folder id
     * @return The {@link TemplateFolder}
     */
    @Override
    public TemplateFolder get(final long orgId, final Long folderId) {
        return templateFolderManager.get(orgId, folderId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents an organization id
     * @param folderIds Represents the {@link Collection} of {@link TemplateFolder} ids
     * @return The {@link Collection} of {@link TemplateFolder}
     */
    @Override
    public Collection<TemplateFolder> get(final long orgId, final Collection<Long> folderIds) {
        return templateFolderManager.get(orgId, folderIds);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents an organization id
     * @param templateFolder Represents the {@link TemplateFolder} that holds the template folder attributes
     */
    @Override
    public void populateFolderUsers(final long orgId, final TemplateFolder templateFolder) {
        final Long folderId = templateFolder.id();

        if (Objects.nonNull(folderId)) {
            final Collection<CrmUser> users = templateFolder.getCrmUsers();
            final Collection<Team> teams = templateFolder.getTeams();
            final Collection<Territory> territories = templateFolder.getTerritories();

            templateFolder.setCrmUsers(Objects.isNull(users) || users.isEmpty() ? resolveUsers(orgId, folderId) : users);
            templateFolder.setTeams(Objects.isNull(teams) || teams.isEmpty() ?
                    resolveTeams(orgId, folderId) : teamManager.getTeams(orgId,
                    teams.stream().map(Team::id).collect(Collectors.toSet()), true));
            templateFolder.setTerritories(Objects.isNull(territories) || territories.isEmpty() ?
                    resolveTerritories(orgId, folderId) : territoryManager.getTerritories(orgId,
                    territories.stream().map(Territory::id).collect(Collectors.toSet()), true));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param templateFolder    Represents the {@link TemplateFolder}
     * @param associatedUserIds Represents the {@link Collection} of associated user ids
     * @return the {@link Collection} of user ids associated with template
     */
    @Override
    public Collection<Long> retrieveAssociatedUserIdsFromSharedFolder(final TemplateFolder templateFolder,
                                                                      final Collection<Long> associatedUserIds) {
        final Collection<CrmUser> crmUsers = templateFolder.getCrmUsers();
        final Collection<Team> teams = templateFolder.getTeams();
        final Collection<Territory> territories = templateFolder.getTerritories();
        final Long orgId = templateFolder.orgId();

        if (Objects.nonNull(crmUsers) && !crmUsers.isEmpty()) {
            associatedUserIds.addAll(crmUsers.stream().map(CrmUser::id).collect(Collectors.toUnmodifiableSet()));
        }

        if (Objects.nonNull(teams) && !teams.isEmpty()) {
            final Collection<Long> teamIds = teams.stream().map(Team::id).collect(Collectors.toUnmodifiableSet());

            associatedUserIds.addAll(teamManager.getTeamUserIds(orgId, teamIds));
        }

        if (Objects.nonNull(territories) && !territories.isEmpty()) {
            final Collection<Long> territoryIds = territories.stream().map(Territory::id)
                    .collect(Collectors.toUnmodifiableSet());

            associatedUserIds.addAll(territoryManager.getTerritoryUserIds(orgId, territoryIds));
        }

        return associatedUserIds;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents an organization id
     * @param userId       Represents the user id
     * @param filterAction Represents the {@link FilterAction}
     * @param searchText   Represents the text to search for (folder name / template name / tag name prefix)
     * @return The total count of matching {@link TemplateFolder}
     */
    @Override
    public long getUserAccessibleFolderCount(final long orgId, final long userId, final FilterAction filterAction,
                                             final String searchText) {
        return templateFolderManager.countUserAccessibleFolders(orgId, userId, filterAction, searchText);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents an organization id
     * @param userId       Represents the user id
     * @param filterAction Represents the {@link FilterAction}
     * @param searchText   Represents the text to search for within folder names
     * @param start        Represents the starting index of {@link TemplateFolder} to be fetched
     * @param limit        Represents the maximum number of {@link TemplateFolder} to be fetched
     * @return The {@link Collection} of {@link TemplateFolder}
     */
    @Override
    public Collection<TemplateFolder> fetchUserAccessibleFolders(final long orgId, final long userId,
                                                                 final FilterAction filterAction, final String searchText,
                                                                 final int start, final int limit) {
        return templateFolderManager.fetchUserAccessibleFolders(orgId, userId, filterAction, searchText, start, limit);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents an organization id
     * @param templateFolder Represents the {@link TemplateFolder} that holds the template folder attributes
     * @return A {@link Map} which keys are status message and values are update operation details
     */
    @Override
    public Map<String, Collection<Long>> updateFolderVisibility(final long orgId, final TemplateFolder templateFolder) {
        return templateFolderManager.updateFolderVisibility(orgId, templateFolder);
    }

    /**
     * <p>
     * Resolves and retrieves the {@link CrmUser}
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param folderId Represents the template folder id
     * @return The {@link Collection} of {@link CrmUser}
     */
    private Collection<CrmUser> resolveUsers(final long orgId, final long folderId) {
        final Collection<Long> userIds = templateFolderManager.fetchAssociatedUserIds(orgId, folderId);

        return userIds.isEmpty() ? Collections.emptyList() : crmUserService.getUsers(orgId, userIds);
    }

    /**
     * <p>
     * Resolves and retrieves the {@link Team}
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param folderId Represents the template folder id
     * @return The {@link Collection} of {@link Team}
     */
    private Collection<Team> resolveTeams(final long orgId, final long folderId) {
        final Collection<Long> teamIds = templateFolderManager.fetchAssociatedTeamIds(orgId, folderId);

        return teamIds.isEmpty() ? Collections.emptyList() : teamManager.getTeams(orgId, teamIds, true);
    }

    /**
     * <p>
     * Resolves and retrieves the {@link Territory}
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param folderId Represents the template folder id
     * @return The {@link Collection} of {@link Territory}
     */
    private Collection<Territory> resolveTerritories(final long orgId, final long folderId) {
        final Collection<Long> territoryIds = templateFolderManager.fetchAssociatedTerritoryIds(orgId, folderId);

        return territoryIds.isEmpty() ? Collections.emptyList() : territoryManager.getTerritories(orgId, territoryIds, true);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents an organization id
     * @param userId       Represents the user id
     * @param start        Represents the starting index of {@link EntityMailTemplate} to be fetched
     * @param limit        Represents the maximum number of {@link EntityMailTemplate} to be fetched
     * @param filterAction Represents the {@link FilterAction}
     * @param searchText   Represents the text to search for within folder names
     * @return The {@link Collection} of {@link EntityMailTemplate}
     */
    @Override
    public Collection<EntityMailTemplate> getTemplates(final long orgId, final long userId, final int start, final int limit, final FilterAction filterAction, final String searchText) {
        return templateFolderManager.getTemplates(orgId, userId, start, limit, filterAction, searchText);
    }
}