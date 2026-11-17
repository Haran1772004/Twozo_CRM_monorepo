package com.tlc.crm.core.notification.email.template.api;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.core.notification.email.template.api.model.EntityMailTemplate;
import com.tlc.crm.core.notification.email.template.api.model.TemplateFolder;
import com.tlc.crm.core.notification.email.template.api.model.TemplateFolder.AccessType;
import com.tlc.crm.core.notification.email.template.api.model.TemplateFolder.Visibility;
import com.tlc.crm.core.notification.email.template.api.model.TemplateFolder.FilterAction;
import com.tlc.crm.core.notification.email.template.api.builder.EmailTemplateQueryBuilder;
import com.tlc.crm.core.notification.email.template.status.TemplateFolderErrorCodes;
import com.tlc.crm.core.sql.resource.OMEMTAG;
import com.tlc.crm.core.sql.resource.OMEMTEMPLATE;
import com.tlc.crm.core.sql.resource.OMEMTEMPLATEFOLDER;
import com.tlc.crm.core.sql.resource.OMEMTEMPLATEFOLDERTEAM;
import com.tlc.crm.core.sql.resource.OMEMTEMPLATEFOLDERTERRITORY;
import com.tlc.crm.core.sql.resource.OMEMTEMPLATEFOLDERUSER;
import com.tlc.crm.core.sql.resource.OMEMTEMPLATESTATISTICS;
import com.tlc.crm.core.sql.resource.OMEMTEMPLATETAG;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the {@link TemplateFolder} related DB actions
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class TemplateFolderManager extends AbstractConfigManager<TemplateFolder> {

    private final TemplateManager templateManager;
    private final EmailTemplateQueryBuilder emailTemplateQueryBuilder;

    public TemplateFolderManager() {
        this.templateManager = TemplateManager.getInstance();
        this.emailTemplateQueryBuilder = EmailTemplateQueryBuilder.getInstance();
    }

    public static class Instance {
        private static final TemplateFolderManager INSTANCE = new TemplateFolderManager();
    }

    /**
     * <p>
     * Gets the {@link TemplateFolderManager} Instance
     * </p>
     *
     * @return {@link TemplateFolderManager}
     */
    public static TemplateFolderManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The template folder table
     */
    @Override
    public Table getTable() {
        return Table.get(OMEMTEMPLATEFOLDER.TABLE);
    }

    /**
     * <p>
     * Gets the {@link OMEMTEMPLATEFOLDERUSER} table
     * </p>
     *
     * @return the template folder user table
     */
    private Table getTemplateFolderUser() {
        return Table.get(OMEMTEMPLATEFOLDERUSER.TABLE);
    }

    /**
     * <p>
     * Gets the {@link OMEMTEMPLATEFOLDERTEAM} table
     * </p>
     *
     * @return the template folder team table
     */
    private Table getTemplateFolderTeam() {
        return Table.get(OMEMTEMPLATEFOLDERTEAM.TABLE);
    }

    /**
     * <p>
     * Gets the {@link OMEMTEMPLATEFOLDERTERRITORY} table
     * </p>
     *
     * @return the template folder territory table
     */
    private Table getTemplateFolderTerritory() {
        return Table.get(OMEMTEMPLATEFOLDERTERRITORY.TABLE);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Represents the collection of template folder ids
     * @return The collection of {@link TemplateFolder}
     */
    @Override
    public Collection<TemplateFolder> get(final Long orgId, final Collection<Long> ids) {
        final DataContainer dataContainer = getTemplateFolderContainer(orgId, ids);

        return dataContainer.getRows(getTable()).map(row -> convertRowToModel(row, dataContainer)).toList();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Represents the {@link Collection} of {@link TemplateFolder} ids
     * @return the {@link Collection} of {@link TemplateFolder}
     */
    @Override
    public Collection<TemplateFolder> partialGet(final Long orgId, final Collection<Long> ids) {
        return null;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param templateFolder Represents the {@link TemplateFolder} that holds the attributes
     * @return The {@link TemplateFolder}
     */
    @Override
    public TemplateFolder create(final TemplateFolder templateFolder) {
        final DataContainer targetContainer = DataContainer.create();

        loadTemplateFolderIntoContainer(true, templateFolder, targetContainer);
        orgDataStore(templateFolder.orgId()).commitChanges(targetContainer);

        return templateFolder;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param templateFolders Represents the {@link Collection} of {@link TemplateFolder} that attributes have to be updated
     * @return A {@link Map} which keys are status message and values are update operation details
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<TemplateFolder> templateFolders) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<Long> ids = templateFolders.stream().map(TemplateFolder::id).filter(Objects::nonNull).toList();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final DataContainer dataContainer = getDataContainer(orgId, ids);

        templateFolders.forEach(templateFolder -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadTemplateFolderIntoContainer(false, templateFolder, dataContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(templateFolder.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Template folder update failed Org ID : {} ID : {} reason : {}", orgId, templateFolder.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, templateFolder.id().toString()));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }

        return processOutcome;
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
    public boolean checkTemplateFoldersAreExists(final long orgId, final Collection<String> folderNames) {
        final Table folderTable = getTable();
        final CountQuery countQuery = CountQuery.get(folderTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(folderTable.getColumn(OMEMTEMPLATEFOLDER.NAME), folderNames)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Checks whether the template folder ids are exists or not
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Represents the collection of folder ids
     * @return True if {@link TemplateFolder} names are exists, otherwise returns false
     */
    public boolean exists(final Long orgId, final Collection<Long> ids) {
        final Table folderTable = getTable();
        final CountQuery countQuery = CountQuery.get(folderTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(folderTable.getPKColumn(), ids)));

        return orgDataStore(orgId).get(countQuery) == ids.size();
    }

    /**
     * <p>
     * Updates the visibility of a template folder within an organization
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param templateFolder Represents the {@link TemplateFolder} that holds the template folder attributes
     * @return A {@link Map} which keys are status message and values are update operation details
     */
    public Map<String, Collection<Long>> updateFolderVisibility(final Long orgId, final TemplateFolder templateFolder) {
        final Long templateFolderId = templateFolder.id();
        final Map<String, Collection<Long>> response = new HashMap<>();
        final Collection<Long> updatedRecords = new ArrayList<>();
        final Collection<CrmUser> crmUsers = templateFolder.getCrmUsers();
        final Collection<Team> teams = templateFolder.getTeams();
        final Collection<Territory> territories = templateFolder.getTerritories();
        final DataContainer entityContainer = getFolderContainer(orgId, List.of(templateFolderId));
        final DataContainer targetContainer = DataContainer.create();

        if (Objects.nonNull(crmUsers)) {
            final Table templateFolderUserTable = getTemplateFolderUser();
            final DataContainer dataContainer = orgDataStore(orgId).get(templateFolderUserTable,
                    new WhereClause(Criteria.eq(templateFolderUserTable
                            .getColumn(OMEMTEMPLATEFOLDERUSER.FOLDER_ID), templateFolderId)));

            entityContainer.append(dataContainer);
        }

        if (Objects.nonNull(teams)) {
            final Table templateFolderTeamTable = getTemplateFolderTeam();
            final DataContainer dataContainer = orgDataStore(orgId).get(templateFolderTeamTable,
                    new WhereClause(Criteria.eq(templateFolderTeamTable
                            .getColumn(OMEMTEMPLATEFOLDERTEAM.FOLDER_ID), templateFolderId)));

            entityContainer.append(dataContainer);
        }

        if (Objects.nonNull(territories)) {
            final Table templateFolderTerritoryTable = getTemplateFolderTerritory();
            final DataContainer dataContainer = orgDataStore(orgId).get(templateFolderTerritoryTable,
                    new WhereClause(Criteria.eq(templateFolderTerritoryTable
                            .getColumn(OMEMTEMPLATEFOLDERTERRITORY.FOLDER_ID), templateFolderId)));

            entityContainer.append(dataContainer);
        }

        loadTemplateFolderVisibilityIntoContainer(false, templateFolder, entityContainer, targetContainer);
        orgDataStore(orgId).commitChanges(targetContainer);
        updatedRecords.add(templateFolder.id());
        response.put(CoreEmailFields.UPDATED_RECORDS.getName(), updatedRecords);

        return response;
    }

    /**
     * <p>
     * Fetches a collection of associated user ids for a given collection of folder ids within an organization
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param folderId Represents the template folder id
     * @return the {@link Collection} of {@link CrmUser} ids
     */
    public Collection<Long> fetchAssociatedUserIds(final long orgId, final long folderId) {
        final Table templateFolderUserTable = getTemplateFolderUser();
        final Criteria criteria = Criteria.eq(templateFolderUserTable.getColumn(OMEMTEMPLATEFOLDERUSER.FOLDER_ID), folderId);

        return fetchFolderIds(orgId, templateFolderUserTable, templateFolderUserTable.getColumn(
                OMEMTEMPLATEFOLDERUSER.USER_ID), criteria);
    }

    /**
     * <p>
     * Fetches a collection of associated team ids for a given collection of folder ids within an organization
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param folderId Represents the template folder id
     * @return the {@link Collection} of {@link Team} ids
     */
    public Collection<Long> fetchAssociatedTeamIds(final long orgId, final long folderId) {
        final Table templateFolderTeamTable = getTemplateFolderTeam();
        final Criteria criteria = Criteria.eq(templateFolderTeamTable.getColumn(OMEMTEMPLATEFOLDERTEAM.FOLDER_ID), folderId);

        return fetchFolderIds(orgId, templateFolderTeamTable, templateFolderTeamTable.getColumn(
                OMEMTEMPLATEFOLDERTEAM.TEAM_ID), criteria);
    }

    /**
     * <p>
     * Fetches a collection of associated territory ids for a given collection of folder ids within an organization
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param folderId Represents the template folder id
     * @return the {@link Collection} of {@link Territory} ids
     */
    public Collection<Long> fetchAssociatedTerritoryIds(final long orgId, final long folderId) {
        final Table templateFolderTerritoryTable = getTemplateFolderTerritory();
        final Criteria criteria = Criteria.eq(templateFolderTerritoryTable.getColumn(OMEMTEMPLATEFOLDERTERRITORY.FOLDER_ID),
                folderId);

        return fetchFolderIds(orgId, templateFolderTerritoryTable, templateFolderTerritoryTable.getColumn(
                OMEMTEMPLATEFOLDERTERRITORY.TERRITORY_ID), criteria);
    }

    /**
     * <p>
     * Retrieves a collection of user accessible template folders based on the filter action within the given organization.
     * When searchText is provided, it is matched (prefix, case-insensitive) against folder name, template names,
     * and tag names at the DB query level so that pagination operates on already-filtered rows.
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param userId       Represents the user id
     * @param filterAction Represents the {@link FilterAction}
     * @param searchText   Represents the text to search for (folder name / template name / tag name prefix)
     * @param start        Represents the starting index of {@link TemplateFolder} to be fetched
     * @param limit        Represents the maximum number of {@link TemplateFolder} to be fetched
     * @return The {@link Collection} of {@link TemplateFolder}
     */
    public Collection<TemplateFolder> fetchUserAccessibleFolders(final long orgId, final long userId,
                                                                 final FilterAction filterAction, final String searchText,
                                                                 final int start, final int limit) {
        final Table folderTable = getTable();
        final Column folderPkColumn = folderTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(folderTable);

        selectQuery.addSelectClause(folderPkColumn);
        selectQuery.setWhereClause(emailTemplateQueryBuilder.buildFilterActionCriteria(userId, folderTable, filterAction, searchText));
        selectQuery.addOrderByClause(List.of(new OrderByClause(folderTable.getColumn(OMEMTEMPLATEFOLDER.CREATED_TIME)), new OrderByClause(folderPkColumn)));
        selectQuery.setLimitClause(new LimitClause(start, limit));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Collection<Long> templateFolderIds = dataContainer.getRows(folderTable).map(Row::getPKValue).toList();
        final DataContainer folderContainer = DataContainer.create();

        if (!templateFolderIds.isEmpty()) {
            folderContainer.append(getTemplateFolderContainer(orgId, templateFolderIds));
        }

        if (!folderContainer.isEmpty()) {
            return folderContainer.getRows(folderTable).map(row -> (convertRowToModel(row, folderContainer))).toList();
        }

        return Collections.emptyList();
    }

    /**
     * <p>
     * Returns the total count of user accessible template folders matching the given filter action and search text.
     * This uses a {@link CountQuery} with the same {@link WhereClause} as {@link #fetchUserAccessibleFolders},
     * so pagination metadata (hasMore, totalRecords) is always accurate.
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param userId       Represents the user id
     * @param filterAction Represents the {@link FilterAction}
     * @param searchText   Represents the text to search for (folder name / template name / tag name prefix)
     * @return The total count of matching {@link TemplateFolder}
     */
    public long countUserAccessibleFolders(final long orgId, final long userId, final FilterAction filterAction,
                                           final String searchText) {
        final Table folderTable = getTable();
        final CountQuery countQuery = CountQuery.get(folderTable);

        countQuery.setWhereClause(emailTemplateQueryBuilder.buildFilterActionCriteria(userId, folderTable, filterAction, searchText));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets the designated folder {@link WhereClause}.
     * </p>
     *
     * @param userId     Represents the user id
     * @param innerQuery Represents the {@link SelectQuery}
     * @return {@link WhereClause}
     */
    private WhereClause getDesignatedFolderWhereClause(final Long userId, final SelectQuery innerQuery) {
        final Table folderTable = getTable();
        final WhereClause whereClause = new WhereClause(Criteria.notEq(folderTable.getColumn(OMEMTEMPLATEFOLDER.OWNER_ID), userId))
                .and(Criteria.eq(folderTable.getColumn(OMEMTEMPLATEFOLDER.VISIBILITY_TYPE), Visibility.DESIGNATED.getId()));

        return new WhereClause(Criteria.in(folderTable.getPKColumn(), innerQuery)).or(whereClause);
    }

    /**
     * <p>
     * Retrieves a collection of folder ids based on specified criteria.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param table  Represents the {@link Table} to which criteria are applied
     * @param column Represents the {@link Column} to which criteria are applied
     * @return the {@link Collection} of ids
     */
    private Collection<Long> fetchFolderIds(final Long orgId, final Table table, final Column column,
                                            final Criteria criteria) {
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.setWhereClause(new WhereClause(criteria));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? Collections.emptySet() : dataContainer.getRows(table)
                .map(row -> (Long) row.get(column)).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * <p>
     * Loads the template folder details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param templateFolder  Represents the {@link TemplateFolder} attributes
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void loadTemplateFolderIntoContainer(final boolean isCreateEvent, final TemplateFolder templateFolder, final DataContainer targetContainer) {
        final DataContainer dataContainer = isCreateEvent ? null : orgDataStore(templateFolder.orgId()).get(getTable(), List.of(templateFolder.id()));

        loadTemplateFolderIntoContainer(isCreateEvent, templateFolder, dataContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the template folder details into container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param templateFolder  Represents the {@link TemplateFolder} attributes
     * @param entityContainer Holds the user defined and system field values of existing {@link TemplateFolder}
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void loadTemplateFolderIntoContainer(final boolean isCreateEvent, final TemplateFolder templateFolder,
                                                 final DataContainer entityContainer, final DataContainer targetContainer) {
        final Table templateFolderTable = getTable();
        final Long id = templateFolder.id();
        final Row templateFolderRow = isCreateEvent ? new Row(templateFolderTable) : entityContainer.getRow(templateFolderTable, id);

        if (!isCreateEvent && Objects.isNull(templateFolderRow)) {
            throw ErrorCode.get(TemplateFolderErrorCodes.TEMPLATE_FOLDER_NOT_FOUND, "i18n.template.folder.not.found");
        }
        final Long currentTime = Instant.now().toEpochMilli();

        if (templateFolderRow.isNewRow()) {
            templateFolderRow.setOrgId(templateFolder.orgId());
            templateFolderRow.set(OMEMTEMPLATEFOLDER.CREATED_TIME, currentTime);
            templateFolderRow.set(OMEMTEMPLATEFOLDER.OWNER_ID, templateFolder.getOwnerId());
            templateFolder.setId(templateFolderRow.getPKValue());
        }
        templateFolderRow.set(OMEMTEMPLATEFOLDER.NAME, templateFolder.getName());
        templateFolderRow.set(OMEMTEMPLATEFOLDER.UPDATED_TIME, currentTime);

        targetContainer.updateRow(templateFolderRow);
    }

    /**
     * <p>
     * Loads the template folder visibility into container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param templateFolder  Represents the {@link TemplateFolder} attributes
     * @param entityContainer Holds the user defined and system field values of existing {@link TemplateFolder}
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void loadTemplateFolderVisibilityIntoContainer(final boolean isCreateEvent, final TemplateFolder templateFolder,
                                                           final DataContainer entityContainer, final DataContainer targetContainer) {
        final Table folderTable = getTable();
        final Table folderUsersTable = getTemplateFolderUser();
        final Table folderTeamTable = getTemplateFolderTeam();
        final Table folderTerritoryTable = getTemplateFolderTerritory();
        final Long orgId = templateFolder.orgId();
        final Long folderId = templateFolder.id();
        final Collection<Team> teams = templateFolder.getTeams();
        final Collection<Territory> territories = templateFolder.getTerritories();
        final Collection<CrmUser> crmUsers = templateFolder.getCrmUsers();
        final Row existingFolderRow = entityContainer.getRow(folderTable, new WhereClause(Criteria.eq(folderTable.getPKColumn(), folderId)));
        final Row templateFolderRow = Objects.isNull(existingFolderRow) ? new Row(folderTable) : existingFolderRow;
        final Long currentTime = Instant.now().toEpochMilli();

        if (templateFolderRow.isNewRow()) {
            templateFolderRow.setOrgId(templateFolder.orgId());
            templateFolderRow.set(OMEMTEMPLATEFOLDER.CREATED_TIME, currentTime);
            templateFolder.setId(templateFolderRow.getPKValue());
            templateFolderRow.set(OMEMTEMPLATEFOLDER.NAME, templateFolder.getName());
            templateFolderRow.set(OMEMTEMPLATEFOLDER.OWNER_ID, templateFolder.getOwnerId());
        }
        templateFolderRow.set(OMEMTEMPLATEFOLDER.UPDATED_TIME, currentTime);
        templateFolderRow.set(OMEMTEMPLATEFOLDER.ACCESS_TYPE, templateFolder.getAccessType().getId());
        templateFolderRow.set(OMEMTEMPLATEFOLDER.VISIBILITY_TYPE, templateFolder.getVisibility().getId());
        templateFolderRow.set(OMEMTEMPLATEFOLDER.PREVENT_SHARE, templateFolder.preventShare());
        targetContainer.updateRow(templateFolderRow);

        if (Objects.nonNull(crmUsers) && !crmUsers.isEmpty()) {

            for (final CrmUser crmUser : crmUsers) {
                final Long userId = crmUser.id();
                final Row existingRow = entityContainer.getRow(folderUsersTable, new WhereClause(Criteria.eq(
                        folderUsersTable.getColumn(OMEMTEMPLATEFOLDERUSER.USER_ID), userId)).and(Criteria.eq(
                        folderUsersTable.getColumn(OMEMTEMPLATEFOLDERUSER.FOLDER_ID), folderId)));
                final Row row = Objects.isNull(existingRow) ? new Row(folderUsersTable) : existingRow;

                if (row.isNewRow()) {
                    row.setOrgId(orgId);
                    row.set(OMEMTEMPLATEFOLDERUSER.FOLDER_ID, folderId);
                }
                row.set(OMEMTEMPLATEFOLDERUSER.USER_ID, userId);
                targetContainer.updateRow(row);
            }
        }

        if (Objects.nonNull(teams) && !teams.isEmpty()) {

            for (final Team team : teams) {
                final Long teamId = team.id();
                final Row existingRow = entityContainer.getRow(folderTeamTable, new WhereClause(Criteria.eq(
                        folderTeamTable.getColumn(OMEMTEMPLATEFOLDERTEAM.TEAM_ID), teamId)).and(Criteria.eq(
                        folderTeamTable.getColumn(OMEMTEMPLATEFOLDERTEAM.FOLDER_ID), folderId)));
                final Row row = Objects.isNull(existingRow) ? new Row(folderTeamTable) : existingRow;

                if (row.isNewRow()) {
                    row.setOrgId(orgId);
                    row.set(OMEMTEMPLATEFOLDERTEAM.FOLDER_ID, folderId);
                }
                row.set(OMEMTEMPLATEFOLDERTEAM.TEAM_ID, teamId);
                targetContainer.updateRow(row);
            }
        }

        if (Objects.nonNull(territories) && !territories.isEmpty()) {

            for (final Territory territory : territories) {
                final Long territoryId = territory.id();
                final Row existingRow = entityContainer.getRow(folderTerritoryTable, new WhereClause(Criteria.eq(
                        folderTerritoryTable.getColumn(OMEMTEMPLATEFOLDERTERRITORY.TERRITORY_ID), territoryId)).and(Criteria.eq(
                        folderTerritoryTable.getColumn(OMEMTEMPLATEFOLDERTERRITORY.FOLDER_ID), folderId)));
                final Row row = Objects.isNull(existingRow) ? new Row(folderTerritoryTable) : existingRow;

                if (row.isNewRow()) {
                    row.setOrgId(orgId);
                    row.set(OMEMTEMPLATEFOLDERTERRITORY.FOLDER_ID, folderId);
                }
                row.set(OMEMTEMPLATEFOLDERTERRITORY.TERRITORY_ID, territoryId);
                targetContainer.updateRow(row);
            }
        }

        if (!isCreateEvent) {
            deleteUnmappedUsers(folderId, crmUsers, entityContainer, targetContainer);
            deleteUnmappedTeams(folderId, teams, entityContainer, targetContainer);
            deleteUnmappedTerritories(folderId, territories, entityContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Deletes the users that are not associated with the template folder
     * </p>
     *
     * @param folderId        Represents the template folder id
     * @param users           Represents the {@link Collection} of {@link CrmUser}
     * @param entityContainer Holds the template folder defined and existing {@link TemplateFolder} values
     * @param targetContainer Holds the updated information of template folder defined {@link TemplateFolder} values
     */
    private void deleteUnmappedUsers(final Long folderId, final Collection<CrmUser> users,
                                     final DataContainer entityContainer, final DataContainer targetContainer) {
        final Table templateFolderUserTable = Table.get(OMEMTEMPLATEFOLDERUSER.TABLE);

        if (entityContainer.size(templateFolderUserTable) > 0) {
            final Collection<Long> userIds = Objects.isNull(users) || users.isEmpty() ? Collections.emptySet() :
                    users.stream().map(CrmUser::id).collect(Collectors.toUnmodifiableSet());
            final Stream<Row> rows = entityContainer.getRows(templateFolderUserTable, new WhereClause(Criteria.eq(
                    templateFolderUserTable.getColumn(OMEMTEMPLATEFOLDERUSER.FOLDER_ID), folderId))
                    .and(Criteria.notIn(templateFolderUserTable.getColumn(OMEMTEMPLATEFOLDERUSER.USER_ID), userIds)));

            rows.forEach(targetContainer::deleteRow);
        }
    }

    /**
     * <p>
     * Deletes the teams that are not associated with the template folder
     * </p>
     *
     * @param folderId        Represents the template folder id
     * @param teams           Represents the {@link Collection} of {@link Team}
     * @param entityContainer Holds the template folder defined and existing {@link TemplateFolder} values
     * @param targetContainer Holds the updated information of template folder defined {@link TemplateFolder} values
     */
    private void deleteUnmappedTeams(final Long folderId, final Collection<Team> teams,
                                     final DataContainer entityContainer, final DataContainer targetContainer) {
        final Table templateFolderTeamTable = Table.get(OMEMTEMPLATEFOLDERTEAM.TABLE);

        if (entityContainer.size(templateFolderTeamTable) > 0) {
            final Collection<Long> teamIds = Objects.isNull(teams) || teams.isEmpty() ? Collections.emptySet() :
                    teams.stream().map(Team::id).collect(Collectors.toUnmodifiableSet());
            final Stream<Row> rows = entityContainer.getRows(templateFolderTeamTable, new WhereClause(Criteria.eq(
                    templateFolderTeamTable.getColumn(OMEMTEMPLATEFOLDERTEAM.FOLDER_ID), folderId))
                    .and(Criteria.notIn(templateFolderTeamTable.getColumn(OMEMTEMPLATEFOLDERTEAM.TEAM_ID), teamIds)));

            rows.forEach(targetContainer::deleteRow);
        }
    }

    /**
     * <p>
     * Deletes the territories that are not associated with the template folder
     * </p>
     *
     * @param folderId        Represents the template folder id
     * @param territories     Represents the {@link Collection} of {@link Territory}
     * @param entityContainer Holds the template folder defined and existing {@link TemplateFolder} values
     * @param targetContainer Holds the updated information of template folder defined {@link TemplateFolder} values
     */
    private void deleteUnmappedTerritories(final Long folderId, final Collection<Territory> territories,
                                           final DataContainer entityContainer, final DataContainer targetContainer) {
        final Table templateFolderTerritoryTable = Table.get(OMEMTEMPLATEFOLDERTERRITORY.TABLE);

        if (entityContainer.size(templateFolderTerritoryTable) > 0) {
            final Collection<Long> territoryIds = Objects.isNull(territories) || territories.isEmpty() ? Collections.emptySet() :
                    territories.stream().map(Territory::id).collect(Collectors.toUnmodifiableSet());
            final Stream<Row> rows = entityContainer.getRows(templateFolderTerritoryTable, new WhereClause(Criteria.eq(
                    templateFolderTerritoryTable.getColumn(OMEMTEMPLATEFOLDERTERRITORY.FOLDER_ID), folderId))
                    .and(Criteria.notIn(templateFolderTerritoryTable.getColumn(OMEMTEMPLATEFOLDERTERRITORY.TERRITORY_ID),
                            territoryIds)));

            rows.forEach(targetContainer::deleteRow);
        }
    }

    /**
     * <p>
     * Gets the data container for the provided template folder ids
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Represents the {@link Collection} of folder ids
     * @return The {@link DataContainer}
     */
    private DataContainer getDataContainer(final Long orgId, final Collection<Long> ids) {
        final Table folderTable = getTable();
        final Table templateTable = Table.get(OMEMTEMPLATE.TABLE);
        final Column folderTablePKColumn = folderTable.getPKColumn();
        final Column templateFolderIdColumn = templateTable.getColumn(OMEMTEMPLATE.FOLDER_ID);
        final SelectQuery selectQuery = SelectQuery.get(folderTable);

        selectQuery.addJoinClause(new JoinClause(folderTablePKColumn, templateFolderIdColumn, JoinClause.JoinType.LEFT));
        selectQuery.addSelectClause(List.of(templateTable.getColumn(OMEMTEMPLATEFOLDER.ORG_ID), templateTable.getColumn(OMEMTEMPLATE.NAME),
                templateFolderIdColumn, templateTable.getColumn(OMEMTEMPLATE.SUBJECT)));
        selectQuery.addSelectClause(folderTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(folderTablePKColumn, ids)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Gets the template folder data container for the provided folder ids
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Represents the {@link Collection} of folder ids
     * @return The {@link DataContainer}
     */
    private DataContainer getTemplateFolderContainer(final Long orgId, final Collection<Long> ids) {
        final Table folderTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(folderTable);

        buildSelectQueryToFetchTemplateFolder(selectQuery);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(folderTable.getPKColumn(), ids)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Builds the select query for fetch template folders
     * </p>
     *
     * @param selectQuery {@link SelectQuery}
     */
    private void buildSelectQueryToFetchTemplateFolder(final SelectQuery selectQuery) {
        final Table folderTable = getTable();
        final Table templateTable = Table.get(OMEMTEMPLATE.TABLE);
        final Table statisticsTable = Table.get(OMEMTEMPLATESTATISTICS.TABLE);
        final Table templateTagTable = Table.get(OMEMTEMPLATETAG.TABLE);
        final Table tagTable = Table.get(OMEMTAG.TABLE);
        final Table folderUserTable = getTemplateFolderUser();
        final Table folderTeamTable = getTemplateFolderTeam();
        final Table folderTerritoryTable = getTemplateFolderTerritory();
        final Column templatePkColumn = templateTable.getPKColumn();
        final Column folderPkColumn = folderTable.getPKColumn();
        final Column templateFolderIdColumn = templateTable.getColumn(OMEMTEMPLATE.FOLDER_ID);
        final Column tagTemplateIdColumn = templateTagTable.getColumn(OMEMTEMPLATETAG.TEMPLATE_ID);
        final Column templateTagIdColumn = templateTagTable.getColumn(OMEMTEMPLATETAG.TAG_ID);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addJoinClause(List.of(new JoinClause(folderPkColumn, templateFolderIdColumn, leftJoin),
                new JoinClause(templatePkColumn, statisticsTable.getColumn(OMEMTEMPLATESTATISTICS.TEMPLATE_ID), leftJoin),
                new JoinClause(templatePkColumn, tagTemplateIdColumn, leftJoin),
                new JoinClause(templateTagIdColumn, tagTable.getPKColumn(), leftJoin),
                new JoinClause(folderPkColumn, folderUserTable.getColumn(OMEMTEMPLATEFOLDERUSER.FOLDER_ID), leftJoin),
                new JoinClause(folderPkColumn, folderTeamTable.getColumn(OMEMTEMPLATEFOLDERTEAM.FOLDER_ID), leftJoin),
                new JoinClause(folderPkColumn, folderTerritoryTable.getColumn(OMEMTEMPLATEFOLDERTERRITORY.FOLDER_ID), leftJoin)));

        selectQuery.addSelectClause(List.of(templateTagIdColumn, tagTemplateIdColumn, tagTable.getColumn(OMEMTAG.NAME)));
        selectQuery.addSelectClause(folderTable, templateTable, statisticsTable, folderUserTable, folderTeamTable,
                folderTerritoryTable);
    }

    /**
     * <p>
     * Converts row into {@link TemplateFolder}
     * </p>
     *
     * @param row Represents the {@link TemplateFolder} row
     * @return The {@link TemplateFolder}
     */
    private TemplateFolder convertRowToModel(final Row row, final DataContainer dataContainer) {
        final Long folderId = row.getPKValue();
        final Table folderUserTable = getTemplateFolderUser();
        final Table folderTeamTable = getTemplateFolderTeam();
        final Table folderTerritoryTable = getTemplateFolderTerritory();
        final Table templateTable = Table.get(OMEMTEMPLATE.TABLE);
        final TemplateFolder templateFolder = new TemplateFolder();
        final Collection<EntityMailTemplate> templates = new LinkedHashSet<>();
        final Stream<Row> rows = dataContainer.getRows(templateTable,
                new WhereClause(Criteria.eq(templateTable.getColumn(OMEMTEMPLATE.FOLDER_ID), folderId)));
        final Long orgId = row.getOrgId();

        templateFolder.setId(folderId);
        templateFolder.setOrgId(orgId);
        templateFolder.setOwnerId(row.get(OMEMTEMPLATEFOLDER.OWNER_ID));
        templateFolder.setName(row.get(OMEMTEMPLATEFOLDER.NAME));
        templateFolder.setCreatedTime(row.get(OMEMTEMPLATEFOLDER.CREATED_TIME));

        rows.forEach(templateRow -> {
            final EntityMailTemplate template = templateManager.convertRowToModel(templateRow, orgId, dataContainer);

            templates.add(template);
        });
        templateFolder.setVisibility(Visibility.get(row.get(OMEMTEMPLATEFOLDER.VISIBILITY_TYPE)));
        templateFolder.setPreventShare(row.get(OMEMTEMPLATEFOLDER.PREVENT_SHARE));
        templateFolder.setAccessType(AccessType.getAccessTypeById(row.get(OMEMTEMPLATEFOLDER.ACCESS_TYPE)));
        templateFolder.setTemplates(templates);

        if (dataContainer.size(folderUserTable) > 0) {
            final Collection<Row> userRows = dataContainer.getRows(folderUserTable,
                    new WhereClause(Criteria.eq(folderUserTable.getColumn(OMEMTEMPLATEFOLDERUSER.FOLDER_ID), folderId))).collect(Collectors.toUnmodifiableSet());
            final UserService userService = UserService.getInstance();
            final Collection<Long> userIds = new HashSet<>();

            for (final Row userRow : userRows) {
                userIds.add(userRow.get(OMEMTEMPLATEFOLDERUSER.USER_ID));
            }

            templateFolder.setCrmUsers(userService.getUsers(templateFolder.orgId(), userIds));
        }

        if (dataContainer.size(folderTeamTable) > 0) {
            final Collection<Row> teamRows = dataContainer.getRows(folderTeamTable,
                            new WhereClause(Criteria.eq(folderTeamTable.getColumn(OMEMTEMPLATEFOLDERTEAM.FOLDER_ID), folderId)))
                    .collect(Collectors.toUnmodifiableSet());
            final Collection<Team> teams = new ArrayList<>();

            for (final Row teamRow : teamRows) {
                final Team team = new Team();

                team.setId(teamRow.get(OMEMTEMPLATEFOLDERTEAM.TEAM_ID));
                teams.add(team);
            }
            templateFolder.setTeams(teams);
        }

        if (dataContainer.size(folderTerritoryTable) > 0) {
            final Collection<Row> territoryRows = dataContainer.getRows(folderTerritoryTable,
                            new WhereClause(Criteria.eq(folderTerritoryTable.getColumn(OMEMTEMPLATEFOLDERTERRITORY.FOLDER_ID), folderId)))
                    .collect(Collectors.toUnmodifiableSet());
            final Collection<Territory> territories = new ArrayList<>();

            for (final Row territoryRow : territoryRows) {
                final Territory territory = new Territory();

                territory.setId(territoryRow.get(OMEMTEMPLATEFOLDERTERRITORY.TERRITORY_ID));
                territories.add(territory);
            }
            templateFolder.setTerritories(territories);
        }

        return templateFolder;
    }

    /**
     * <p>
     * Fetches the folder container for the provided folder ids
     * </p>
     *
     * @param orgId     Represents the organization id
     * @param folderIds Refers to the {@link Collection} of folder ids
     * @return {@link DataContainer}
     */
    private DataContainer getFolderContainer(final Long orgId, final Collection<Long> folderIds) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Table folderTable = getTable();
        final Table folderUserTable = getTemplateFolderUser();
        final Table folderTeamTable = getTemplateFolderTeam();
        final Table folderTerritoryTable = getTemplateFolderTerritory();
        final SelectQuery folderSelectQuery = SelectQuery.get(folderTable);
        final SelectQuery usersSelectQuery = SelectQuery.get(folderUserTable);
        final SelectQuery teamsSelectQuery = SelectQuery.get(folderTeamTable);
        final SelectQuery territoriesSelectQuery = SelectQuery.get(folderTerritoryTable);

        folderSelectQuery.setWhereClause(new WhereClause(Criteria.in(folderTable.getPKColumn(), folderIds)));
        usersSelectQuery.setWhereClause(new WhereClause(Criteria.in(folderUserTable.getColumn(OMEMTEMPLATEFOLDERUSER.FOLDER_ID), folderIds)));
        teamsSelectQuery.setWhereClause(new WhereClause(Criteria.in(folderTeamTable.getColumn(OMEMTEMPLATEFOLDERUSER.FOLDER_ID), folderIds)));
        territoriesSelectQuery.setWhereClause(new WhereClause(Criteria.in(folderTerritoryTable.getColumn(OMEMTEMPLATEFOLDERUSER.FOLDER_ID), folderIds)));

        final DataContainer folderDatacontainer = orgDataStore.get(folderSelectQuery);
        final DataContainer usersDataContainer = orgDataStore.get(usersSelectQuery);
        final DataContainer teamsDataContainer = orgDataStore.get(teamsSelectQuery);
        final DataContainer territoriesDatacontainer = orgDataStore.get(territoriesSelectQuery);

        folderDatacontainer.append(usersDataContainer);
        folderDatacontainer.append(teamsDataContainer);
        folderDatacontainer.append(territoriesDatacontainer);

        return folderDatacontainer;
    }

    /**
     * <p>
     * Gets the template counts based on visibility for a specific user within their organization
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return {@link Map} email template visibility with their count
     */
    public Map<FilterAction, Integer> getTemplatesCount(final long orgId, final long userId) {
        final Table templateTable = templateManager.getTable();
        final Table templateFolderTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(templateTable);
        final Column folderOwnerIdColumn = templateFolderTable.getColumn(OMEMTEMPLATEFOLDER.OWNER_ID);
        final Column templateFolderIdColumn = templateTable.getColumn(OMEMTEMPLATE.FOLDER_ID);

        selectQuery.addJoinClause(new JoinClause(templateFolderIdColumn, templateFolderTable.getPKColumn()));
        selectQuery.addSelectClause(List.of(templateTable.getColumn(OMEMTEMPLATE.NAME), folderOwnerIdColumn, templateFolderIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(folderOwnerIdColumn, userId)).or(Criteria.in(templateFolderTable.getColumn(OMEMTEMPLATEFOLDER.VISIBILITY_TYPE),
                List.of(Visibility.EVERYONE.getId(), Visibility.DESIGNATED.getId()))));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyMap();
        } else {
            final Map<FilterAction, Integer> templatesCount = new HashMap<>();

            dataContainer.getRows(templateFolderTable).forEach(row -> {
                final FilterAction action = Objects.equals(userId, row.get(OMEMTEMPLATEFOLDER.OWNER_ID)) ? FilterAction.CREATED_BY_ME : FilterAction.SHARED_WITH_ME;
                final Stream<Row> rows = dataContainer.getRows(templateTable, new WhereClause(Criteria.eq(templateFolderIdColumn, row.getPKValue())));

                templatesCount.merge(action, (int) rows.count(), Integer::sum);
            });

            templatesCount.put(FilterAction.ALL, dataContainer.size(templateTable));

            return templatesCount;
        }
    }

    /**
     * <p>
     * Gets the template count for the given organization and user based {@link FilterAction}
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param userId       Represents the user id
     * @param searchText   Represents the search text
     * @param filterAction Represents the {@link FilterAction}
     * @return the {@link Long}
     */
    public Long getTemplatesCount(final long orgId, final long userId, final String searchText, final FilterAction filterAction) {
        final Table templateTable = templateManager.getTable();
        final Table templateFolderTable = getTable();
        final CountQuery countQuery = CountQuery.get(templateTable);
        final Column folderOwnerIdColumn = templateFolderTable.getColumn(OMEMTEMPLATEFOLDER.OWNER_ID);
        final SelectQuery innerQuery = SelectQuery.get(templateFolderTable);
        final Criteria searchCriteria = Criteria.contains(templateTable.getColumn(OMEMTEMPLATE.NAME), searchText);

        countQuery.addJoinClause(new JoinClause(templateTable.getColumn(OMEMTEMPLATE.FOLDER_ID), templateFolderTable.getPKColumn(), JoinClause.JoinType.LEFT));
        innerQuery.addSelectClause(templateFolderTable.getPKColumn());
        innerQuery.setWhereClause(new WhereClause(Criteria.notEq(folderOwnerIdColumn, userId))
                .and(Criteria.eq(templateFolderTable.getColumn(OMEMTEMPLATEFOLDER.VISIBILITY_TYPE), Visibility.EVERYONE.getId())));

        final WhereClause filterWhereClause = switch (filterAction) {
            case CREATED_BY_ME -> new WhereClause(Criteria.eq(folderOwnerIdColumn, userId));
            case SHARED_WITH_ME -> getDesignatedFolderWhereClause(userId, innerQuery);
            case ALL -> new WhereClause(Criteria.eq(folderOwnerIdColumn, userId)).or(getDesignatedFolderWhereClause(userId, innerQuery));
        };

        countQuery.setWhereClause(Objects.isNull(searchText) ? filterWhereClause : filterWhereClause.and(searchCriteria));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets the {@link Collection} of template for the given user and organization
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param userId       Represents the user id
     * @param start        Represents the starting index of {@link EntityMailTemplate} to be fetched
     * @param limit        Represents the maximum number of {@link EntityMailTemplate} to be fetched
     * @param filterAction Represents the {@link FilterAction}
     * @param searchText   Represents the text to search for within folder names
     * @return The {@link Collection} of {@link EntityMailTemplate}
     */
    public Collection<EntityMailTemplate> getTemplates(final long orgId, final long userId, final int start, final int limit, final FilterAction filterAction, final String searchText) {
        final Table templateTable = templateManager.getTable();
        final Table templateFolderTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(templateTable);
        final Column folderOwnerIdColumn = templateFolderTable.getColumn(OMEMTEMPLATEFOLDER.OWNER_ID);
        final SelectQuery innerQuery = SelectQuery.get(templateFolderTable);
        final Column templateFolderPkColumn = templateFolderTable.getPKColumn();

        selectQuery.addJoinClause(new JoinClause(templateTable.getColumn(OMEMTEMPLATE.FOLDER_ID), templateFolderPkColumn, JoinClause.JoinType.LEFT));
        selectQuery.setLimitClause(new LimitClause(start, limit));
        selectQuery.addSelectClause(templateTable.getColumn(OMEMTEMPLATE.NAME));
        innerQuery.addSelectClause(templateFolderPkColumn);
        innerQuery.setWhereClause(new WhereClause(Criteria.notEq(folderOwnerIdColumn, userId))
                .and(Criteria.eq(templateFolderTable.getColumn(OMEMTEMPLATEFOLDER.VISIBILITY_TYPE), TemplateFolder.Visibility.EVERYONE.getId())));

        final WhereClause filterWhereClause = switch (filterAction) {
            case CREATED_BY_ME -> new WhereClause(Criteria.eq(folderOwnerIdColumn, userId));
            case SHARED_WITH_ME -> getDesignatedFolderWhereClause(userId, innerQuery);
            case ALL -> new WhereClause(Criteria.eq(folderOwnerIdColumn, userId)).or(getDesignatedFolderWhereClause(userId, innerQuery));
        };

        selectQuery.setWhereClause(Objects.isNull(searchText) ? filterWhereClause : filterWhereClause.and(Criteria.contains(templateTable.getColumn(OMEMTEMPLATE.NAME), searchText)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyList();
        } else {
            return dataContainer.getRows(templateTable).map(row -> {
                final EntityMailTemplate entityMailTemplate = new EntityMailTemplate();

                entityMailTemplate.setName(row.get(OMEMTEMPLATE.NAME));
                entityMailTemplate.setId(row.getPKValue());

                return entityMailTemplate;
            }).toList();
        }
    }
}
