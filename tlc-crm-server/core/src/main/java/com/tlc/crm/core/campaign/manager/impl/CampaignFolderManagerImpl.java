package com.tlc.crm.core.campaign.manager.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.CommonFields;
import com.tlc.crm.common.models.SortType;

import com.tlc.crm.core.campaign.manager.CampaignFolderManager;
import com.tlc.crm.core.campaign.model.CampaignFilterGroup;
import com.tlc.crm.core.campaign.model.CampaignFilterUserPreference;
import com.tlc.crm.core.campaign.model.CampaignFolder;
import com.tlc.crm.core.campaign.model.CampaignCategory;
import com.tlc.crm.core.sql.resource.OMKCPFOLDER;

import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the {@link CampaignFolder} related DB actions.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class CampaignFolderManagerImpl extends AbstractConfigManager<CampaignFolder> implements CampaignFolderManager {

    private CampaignFolderManagerImpl() {
    }

    private static final class InstanceHolder {
        private static final CampaignFolderManagerImpl INSTANCE = new CampaignFolderManagerImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link CampaignFolderManagerImpl}.
     * </p>
     *
     * @return {@link CampaignFolderManagerImpl}.
     */
    public static CampaignFolderManagerImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@link Table}.
     */
    @Override
    public Table getTable() {
        return Table.get(OMKCPFOLDER.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param ids   Represents the ids.
     * @return {@link Collection} of {@link CampaignFolder}.
     */
    @Override
    public Collection<CampaignFolder> get(final Long orgId, final Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        final Table folderTable = getTable();

        return orgDataStore(orgId).get(folderTable, ids).getRows(folderTable)
                .map(row -> convertRowToFolder(orgId, row)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param ids   Represents the ids.
     * @return {@link Collection} of {@link CampaignFolder}.
     */
    @Override
    public Collection<CampaignFolder> partialGet(final Long orgId, final Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(List.of(table.getColumn(OMKCPFOLDER.NAME), table.getColumn(OMKCPFOLDER.CATEGORY),
                table.getColumn(OMKCPFOLDER.IS_STARRED)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), ids)));

        return orgDataStore(orgId).get(selectQuery).getRows(table).map(row -> convertRowToFolder(orgId, row)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param folder Represents the campaign folder.
     * @return {@link CampaignFolder}.
     */
    @Override
    public CampaignFolder create(final CampaignFolder folder) {
        final DataContainer targetContainer = DataContainer.create();

        loadFolderIntoContainer(true, folder, null, targetContainer);
        orgDataStore(folder.orgId()).commitChanges(targetContainer);

        return folder;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organization id.
     * @param folders Represents the folders.
     * @return {@link ProcessOutcome}.
     */
    @Override
    protected ProcessOutcome update(final Long orgId, final Collection<CampaignFolder> folders) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final Table table = getTable();
        final Collection<Long> ids = folders.stream().map(CampaignFolder::id).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
        final DataContainer sourceContainer = orgDataStore(orgId).get(table, ids);
        final Collection<ErrorDescriptor> failedEntities = new ArrayList<>();

        folders.forEach(folder -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadFolderIntoContainer(false, folder, sourceContainer, targetContainer);
                orgDataStore(orgId).commitChanges(targetContainer);
                processOutcome.addProcessedRecord(folder.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Campaign folder update failed Org ID : {} ID : {} reason : {}", orgId, folder.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, folder.id().toString()));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }

        return processOutcome;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organization id.
     * @param category Represents the campaign category.
     * @param folderId Represents the folder id.
     * @param name     Represents the name.
     * @return {@link Boolean}.
     */
    @Override
    public boolean existsByName(final Long orgId, final CampaignCategory category, final Long folderId,
                                final String name) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(OMKCPFOLDER.NAME), name))
                .and(Criteria.eq(table.getColumn(OMKCPFOLDER.CATEGORY), category.getId()));

        countQuery.setWhereClause(Objects.nonNull(folderId) ? whereClause.and(Criteria.notEq(table.getPKColumn(), folderId)) : whereClause);

        return orgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the organization id.
     * @param preference Represents the campaign filter preference.
     * @return {@link Long}.
     */
    @Override
    public long getTotalCount(final Long orgId, final CampaignFilterUserPreference preference) {
        final Table folderTable = getTable();
        final CountQuery countQuery = CountQuery.get(folderTable);

        countQuery.setWhereClause(buildListWhereClause(folderTable, preference));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the organization id.
     * @param preference Represents the campaign filter preference.
     * @return {@link Collection} of {@link CampaignFolder}.
     */
    @Override
    public Collection<CampaignFolder> get(final Long orgId, final CampaignFilterUserPreference preference) {
        final Table folderTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(folderTable);

        selectQuery.setWhereClause(buildListWhereClause(folderTable, preference));
        selectQuery.addOrderByClause(getOrderByClause(preference));
        selectQuery.setLimitClause(new LimitClause(preference.getStart(), preference.getLimit()));

        return orgDataStore(orgId).get(selectQuery).getRows(folderTable).map(row -> convertRowToFolder(orgId, row)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId               Represents the organization id.
     * @param category            Represents the campaign category.
     * @param campaignFilterGroup Represents the campaign filter group.
     * @param dropdownLimit       Represents the dropdown limit.
     * @param dropdownCriteria    Represents the dropdown criteria.
     * @return {@link Collection} of {@link CampaignFolder}.
     */
    @Override
    public Collection<CampaignFolder> getDropdownList(final Long orgId, final CampaignCategory category, final CampaignFilterGroup campaignFilterGroup,
                                                      final DropdownLimit dropdownLimit, final DropdownCriteria dropdownCriteria) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table.getColumn(OMKCPFOLDER.NAME));
        selectQuery.setWhereClause(buildDropdownWhereClause(category, dropdownCriteria, campaignFilterGroup));
        selectQuery.setLimitClause(new LimitClause(dropdownLimit.start(), dropdownLimit.limit()));

        return orgDataStore(orgId).get(selectQuery).getRows(getTable()).map(row -> convertRowToFolder(orgId, row)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId               Represents the organization id.
     * @param category            Represents the campaign category.
     * @param campaignFilterGroup Represents the campaign filter group.
     * @param dropdownCriteria    Represents the dropdown criteria.
     * @return {@link Long}.
     */
    @Override
    public long getDropdownCount(final Long orgId, final CampaignCategory category, final CampaignFilterGroup campaignFilterGroup,
                                 final DropdownCriteria dropdownCriteria) {
        final CountQuery countQuery = CountQuery.get(getTable());

        countQuery.setWhereClause(buildDropdownWhereClause(category, dropdownCriteria, campaignFilterGroup));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the organization id.
     * @param userId     Represents the user id.
     * @param folderId   Represents the folder id.
     * @param folderName Represents the updated folder name.
     */
    @Override
    public void rename(final Long orgId, final Long userId, final Long folderId, final String folderName) {
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMKCPFOLDER.NAME), folderName);
        updateQuery.addUpdateColumn(table.getColumn(OMKCPFOLDER.UPDATED_BY), userId);
        updateQuery.addUpdateColumn(table.getColumn(OMKCPFOLDER.UPDATED_AT), Instant.now().toEpochMilli());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), folderId)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Builds the where clause for the campaign folder dropdown.
     * </p>
     *
     * @param category            Represents the campaign category.
     * @param dropdownCriteria    Represents the dropdown criteria.
     * @param campaignFilterGroup Represents the campaign filter group.
     * @return {@link WhereClause}.
     */
    private WhereClause buildDropdownWhereClause(final CampaignCategory category, final DropdownCriteria dropdownCriteria,
                                                 final CampaignFilterGroup campaignFilterGroup) {
        final Table table = getTable();
        WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(OMKCPFOLDER.CATEGORY), category.getId()));

        if (Objects.nonNull(dropdownCriteria)) {
            whereClause = WhereClauseUtil.connect(whereClause,
                    new WhereClause(dropdownCriteria.build(table.getColumn(OMKCPFOLDER.NAME))), Connector.AND);
        }

        if (Objects.nonNull(campaignFilterGroup)) {
            whereClause = WhereClauseUtil.connect(whereClause, new WhereClause(Criteria.eq(table.getColumn(OMKCPFOLDER.IS_STARRED),
                    campaignFilterGroup.isStarredFolderGroup())), Connector.AND);
        }

        return whereClause;
    }

    /**
     * <p>
     * Returns the {@link OrderByClause} based on the given sorting configuration.
     * </p>
     *
     * @param preference Represents the campaign filter preference.
     * @return The {@link OrderByClause} to be applied for the query.
     */
    private OrderByClause getOrderByClause(final CampaignFilterUserPreference preference) {
        final Table folderTable = getTable();
        final OrderByClause defaultOrderByClause = new OrderByClause(folderTable.getColumn(OMKCPFOLDER.CREATED_AT), OrderByClause.OrderType.DESCENDING);

        if (Objects.isNull(preference)) {
            return defaultOrderByClause;
        }
        final JsonObject sortJson = preference.getSortJson();

        if (Objects.nonNull(sortJson)) {
            final String sortFieldName = sortJson.getString(CommonFields.FIELD_NAME);
            final String sortType = sortJson.getString(CommonFields.TYPE);

            if (Objects.isNull(sortFieldName) || Objects.isNull(sortType)) {
                return defaultOrderByClause;
            }

            return switch (sortFieldName) {
                case "name" -> new OrderByClause(folderTable.getColumn(OMKCPFOLDER.NAME), SortType.getSortType(sortType).getOrderType());
                default -> defaultOrderByClause;
            };
        }

        return defaultOrderByClause;
    }

    /**
     * <p>
     * Builds the filter criteria for campaign folder list and count queries
     * based on the provided search parameters.
     * </p>
     *
     * @param folderTable Represents the campaign folder table.
     * @param preference  Represents the campaign filter preference.
     * @return The {@link WhereClause} containing the filter criteria, or {@code null}
     * if no filters are applied.
     */
    private WhereClause buildListWhereClause(final Table folderTable, final CampaignFilterUserPreference preference) {
        if (Objects.isNull(preference)) {
            return null;
        }
        WhereClause whereClause = new WhereClause(Criteria.eq(folderTable.getColumn(OMKCPFOLDER.CATEGORY), preference.getCategory().getId()));

        if (preference.IsStarredFolder()) {
            whereClause = WhereClauseUtil.connect(whereClause, new WhereClause(Criteria.eq(folderTable.getColumn(OMKCPFOLDER.IS_STARRED), true)), Connector.AND);
        }
        final String searchText = preference.getSearchText();

        if (Objects.nonNull(searchText) && !searchText.isBlank()) {
            whereClause = WhereClauseUtil.connect(whereClause, new WhereClause(Criteria.contains(folderTable.getColumn(OMKCPFOLDER.NAME), searchText)), Connector.AND);
        }

        return whereClause;
    }

    /**
     * <p>
     * Loads the campaign folder details into the target container.
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create event or update event.
     * @param folder          An entity that holds the {@link CampaignFolder} attributes.
     * @param sourceContainer Holds the old information of folder.
     * @param targetContainer Holds the updated information of folder.
     */
    private void loadFolderIntoContainer(final boolean isCreateEvent, final CampaignFolder folder,
                                         final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table table = getTable();
        final Long currentTime = Instant.now().toEpochMilli();
        final Row row = isCreateEvent ? new Row(table) : sourceContainer.getRow(table, folder.id());

        if (row.isNewRow()) {
            folder.setId(row.getPKValue());
            row.setOrgId(folder.orgId());
            row.set(OMKCPFOLDER.CREATED_BY, folder.getCreatedById());
            row.set(OMKCPFOLDER.CREATED_AT, currentTime);
            row.set(OMKCPFOLDER.CATEGORY, folder.getCategory().getId());
        }
        row.set(OMKCPFOLDER.NAME, folder.getName());
        row.set(OMKCPFOLDER.IS_STARRED, folder.isStarred());
        row.set(OMKCPFOLDER.UPDATED_BY, folder.getUpdatedById());
        row.set(OMKCPFOLDER.UPDATED_AT, currentTime);

        targetContainer.updateRow(row);
    }

    /**
     * <p>
     * Converts the folder {@link Row} into {@link CampaignFolder} instance.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param row   Which contains the values of campaign folder.
     * @return {@link CampaignFolder} that holds the values of campaign folder.
     */
    private CampaignFolder convertRowToFolder(final Long orgId, final Row row) {
        final CampaignFolder folder = new CampaignFolder();
        final Short categoryId = row.get(OMKCPFOLDER.CATEGORY);
        final Boolean isStarred = row.get(OMKCPFOLDER.IS_STARRED);

        folder.setId(row.getPKValue());
        folder.setOrgId(orgId);
        folder.setName(row.get(OMKCPFOLDER.NAME));
        folder.setCreatedTime(row.get(OMKCPFOLDER.CREATED_AT));
        folder.setUpdatedTime(row.get(OMKCPFOLDER.UPDATED_AT));

        if (Objects.nonNull(categoryId)) {
            folder.setCategory(CampaignCategory.get(categoryId));
        }
        if (Objects.nonNull(isStarred)) {
            folder.setStarred(isStarred);
        }
        folder.setCreatedById(row.get(OMKCPFOLDER.CREATED_BY));
        folder.setUpdatedById(row.get(OMKCPFOLDER.UPDATED_BY));

        return folder;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param category Represents the campaign category.
     * @param folderId Represents folder id to exclude.
     * @return true if exists, false otherwise
     */
    @Override
    public boolean exists(final Long orgId, final CampaignCategory category, final Long folderId) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getPKColumn(), folderId))
                .and(Criteria.eq(table.getColumn(OMKCPFOLDER.CATEGORY), category.getId()));

        countQuery.setWhereClause(whereClause);

        return orgDataStore(orgId).get(countQuery) > 0;
    }
}