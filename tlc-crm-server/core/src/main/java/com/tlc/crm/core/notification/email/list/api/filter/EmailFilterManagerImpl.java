package com.tlc.crm.core.notification.email.list.api.filter;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.notification.email.api.manager.addon.team.TeamInboxManager;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.core.notification.email.inbox.api.model.TeamInbox;
import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.FilterAttribute;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.Folder;
import com.tlc.crm.core.notification.email.list.api.filter.model.SyncEmailFilterOption;
import com.tlc.crm.core.notification.email.list.api.filter.model.TeamInboxFilterOption;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.FilterInterval;
import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.LinkedCategory;
import com.tlc.crm.core.notification.email.list.service.filter.EmailFilterManager;
import com.tlc.crm.core.sql.resource.OMEMFILTERATTRIBUTECONFIG;
import com.tlc.crm.core.sql.resource.OMEMFILTERSUBATTRIBUTECONFIG;

import com.tlc.crm.core.sql.resource.OMEMFILTERSUBATTRIBUTESYNCEMAILCONFIG;
import com.tlc.crm.core.sql.resource.OMEMFILTERSUBATTRIBUTETEAMINBOXCONFIG;
import com.tlc.crm.email.notification.email.sync.api.EmailSyncManager;
import com.tlc.crm.email.notification.sql.resource.OMEMADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMTEAMINBOX;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.MultiQuery;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Manages the services of {@link EmailListFilter} related actions.
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see EmailFilterManager
 */
public class EmailFilterManagerImpl implements EmailFilterManager {

    private final EmailFilterQueryBuilder emailFilterQueryBuilder;
    private final TeamInboxManager teamInboxManager;

    private EmailFilterManagerImpl() {
        this.emailFilterQueryBuilder = EmailFilterQueryBuilder.getInstance();
        this.teamInboxManager = TeamInboxManager.getInstance();
    }

    private static class Instance {
        private static final EmailFilterManagerImpl INSTANCE = new EmailFilterManagerImpl();
    }

    /**
     * Gets the {@link EmailFilterManagerImpl} Instance
     *
     * @return {@link EmailFilterManagerImpl}
     */
    public static EmailFilterManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Gets the {@link OrgDataStore} instance
     *
     * @param orgId Represents an organization id
     * @return {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * Gets the filter config table
     *
     * @return the {@link Table} of filter config
     */
    private Table getFilterConfigTable() {
        return Table.get(OMEMFILTERATTRIBUTECONFIG.TABLE);
    }

    /**
     * Gets the filter sub attribute config table
     *
     * @return the {@link Table} of filter config
     */
    private Table getFilterSubConfigTable() {
        return Table.get(OMEMFILTERSUBATTRIBUTECONFIG.TABLE);
    }

    /**
     * Gets the team inbox filter sub attribute config table
     *
     * @return the {@link Table} of filter config
     */
    private Table getTeamInboxFilterConfigTable() {
        return Table.get(OMEMFILTERSUBATTRIBUTETEAMINBOXCONFIG.TABLE);
    }

    /**
     * <p>
     * Gets the sync email filter sub attribute config table
     * </p>
     *
     * @return the {@link Table} of sync email filter config
     */
    private Table getSyncEmailFilterConfigTable() {
        return Table.get(OMEMFILTERSUBATTRIBUTESYNCEMAILCONFIG.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param emailFilter Represents the {@link EmailListFilter} details
     */
    @Override
    public void updateFilterAttributeConfig(final EmailListFilter emailFilter) {
        final Long orgId = emailFilter.orgId();
        final Folder folder = emailFilter.getFolder();

        if (Objects.isNull(folder)) {
            throw ErrorCode.get(EmailErrorCodes.INVALID_EMAIL_FOLDER, "i18n.notifications.email.filter.folder.invalid");
        }
        final DataContainer filterConfigContainer = getExistingFilterContainer(orgId, emailFilter.getUserId(), folder.getId());
        final Table filterAttributeConfigTable = getFilterConfigTable();
        final DataContainer targetContainer = DataContainer.create();

        if (filterConfigContainer.isEmpty()) {
            final Map<FilterAttribute, FilterOption> filterAttributes = emailFilter.getFilterAttribute();

            if (Objects.nonNull(filterAttributes)) {

                for (final FilterAttribute attribute : filterAttributes.keySet()) {
                    loadFilterAttributeIntoContainer(filterAttributeConfigTable, attribute, emailFilter, targetContainer);
                }
            }
        } else {
            updateFilterAttributeRows(filterAttributeConfigTable, emailFilter, filterConfigContainer, targetContainer);
        }
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization
     * @param userId Represents the user
     * @param folder Represents the {@link Folder} of email
     * @return the user filter preferences with filter attributes
     */
    @Override
    public Map<FilterAttribute, FilterOption> fetchDefaultFilterConfig(final long orgId, final long userId, final Folder folder) {
        final Table filterConfigTable = getFilterConfigTable();
        final Table filterSubFilterTable = getFilterSubConfigTable();
        final Table teamInboxFilterConfigTable = getTeamInboxFilterConfigTable();
        final Table syncEmailFilterConfigTable = getSyncEmailFilterConfigTable();
        final Table teamInboxTable = teamInboxManager.getTable();
        final Table emailTable = teamInboxManager.getEmailAddressTable();

        final DataContainer existingFilterContainer = getExistingFilterContainer(orgId, userId, folder.getId());
        final Collection<Row> filterConfigRows = existingFilterContainer.getRows(filterConfigTable).toList();

        if (filterConfigRows.isEmpty()) {
            return Collections.emptyMap();
        } else {
            return filterConfigRows.stream().collect(Collectors.toMap(
                    this::getFilterAttribute,
                    filterConfigRow -> {
                        final FilterAttribute filterAttribute = getFilterAttribute(filterConfigRow);
                        final WhereClause criteria = new WhereClause(Criteria.eq(filterSubFilterTable.getColumn(OMEMFILTERSUBATTRIBUTECONFIG.FILTER_CONFIG_ID),
                                filterConfigRow.getPKValue()));

                        final FilterOption subAttribute;

                        if (filterAttribute.isTeamInboxCategory()) {
                            final Collection<Long> teamInboxIds = existingFilterContainer.getRows(teamInboxFilterConfigTable,
                                    criteria).map(row -> (Long) row.get(OMEMFILTERSUBATTRIBUTETEAMINBOXCONFIG.TEAM_INBOX_ID)).collect(Collectors.toSet());
                            final Collection<TeamInbox> teamInboxes = new ArrayList<>();

                            teamInboxIds.forEach(teamInboxId -> {
                                final Optional<Row> optionalTeamInboxRow = Optional.ofNullable(existingFilterContainer.getRow(teamInboxTable, teamInboxId));

                                optionalTeamInboxRow.ifPresent(teamInboxRow -> {
                                    final Optional<Row> optionalEmailRow = Optional.ofNullable(existingFilterContainer
                                            .getRow(emailTable, (Long) teamInboxRow.get(OMEMTEAMINBOX.EMAIL_ID)));

                                    optionalEmailRow.ifPresent(emailRow -> {
                                        final TeamInbox teamInbox = new TeamInbox();

                                        teamInbox.setId(teamInboxId);
                                        teamInbox.setAddress(new EmailAddress(emailRow.get(OMEMADDRESS.EMAIL_ADDRESS)));

                                        teamInboxes.add(teamInbox);
                                    });
                                });
                            });

                            subAttribute = new TeamInboxFilterOption(teamInboxes);
                        } else if (filterAttribute.isSyncEmailCategory()) {
                            final Collection<Long> syncEmailIds = existingFilterContainer.getRows(syncEmailFilterConfigTable, criteria)
                                    .map(row -> (Long) row.get(OMEMFILTERSUBATTRIBUTESYNCEMAILCONFIG.SYNCED_EMAIL_ID)).collect(Collectors.toSet());

                            return new SyncEmailFilterOption(syncEmailIds);
                        } else if (filterAttribute.hasSubtypes()) {
                            final Integer subFilterConfigId = existingFilterContainer.getRow(filterSubFilterTable,
                                    criteria).get(OMEMFILTERSUBATTRIBUTECONFIG.SUB_ATTRIBUTE_ID);

                            subAttribute = filterAttribute.hasIntervalSubTypes() ? EmailListFilter.FilterInterval.valueOf(subFilterConfigId)
                                    : EmailListFilter.LinkedCategory.valueOf(subFilterConfigId);
                        } else {
                            subAttribute = EmailListFilter.Unread.TRUE;
                        }

                        return subAttribute;
                    }
            ));
        }
    }

    /**
     * Gets the {@link FilterAttribute}
     *
     * @param filterConfigRow Represents the {@link Row} of filter attribute config
     * @return {@link FilterAttribute}
     */
    private FilterAttribute getFilterAttribute(final Row filterConfigRow) {
        return FilterAttribute.getFilterAttribute(filterConfigRow.get(OMEMFILTERATTRIBUTECONFIG.FILTER_CONFIG_ID));
    }

    /**
     * Retrieves the existing {@link DataContainer} of filter for a given user and organization.
     *
     * @param orgId    Represents the organization ID
     * @param userId   Represents the user ID
     * @param folderId Represents the folder ID
     * @return {@link DataContainer} which contains the filter configuration rows for the respective user
     */
    private DataContainer getExistingFilterContainer(final long orgId, final long userId, final int folderId) {
        if (Objects.equals(Folder.TEAM_INBOX.getId(), folderId)) {
            return getTeamInboxFilterConfigContainer(orgId, userId, folderId);
        }

        return getFilterSubAttributeConfigContainer(orgId, userId, folderId);
    }

    /**
     * <p>
     * Retrieves the {@link DataContainer} containing team inbox-related filter configuration data
     * for the specified user within an organization.
     * </p>
     *
     * @param orgId    Represents the organization ID
     * @param userId   Represents the user ID
     * @param folderId Represents the folder ID
     * @return {@link DataContainer} which contains the team inbox filter configuration rows
     */
    private DataContainer getTeamInboxFilterConfigContainer(final long orgId, final long userId, final int folderId) {
        final Table filterAttributeConfigTable = getFilterConfigTable();
        final Table teamInboxFilterConfigTable = getTeamInboxFilterConfigTable();
        final Table teamInboxTable = teamInboxManager.getTable();
        final Table emailTable = teamInboxManager.getEmailAddressTable();

        final SelectQuery selectQuery = SelectQuery.get(filterAttributeConfigTable);

        selectQuery.addJoinClause(
                List.of(
                        new JoinClause(filterAttributeConfigTable.getPKColumn(), teamInboxFilterConfigTable.getColumn(OMEMFILTERSUBATTRIBUTETEAMINBOXCONFIG.FILTER_CONFIG_ID)),
                        new JoinClause(teamInboxFilterConfigTable.getColumn(OMEMFILTERSUBATTRIBUTETEAMINBOXCONFIG.TEAM_INBOX_ID), teamInboxTable.getPKColumn()),
                        new JoinClause(teamInboxTable.getColumn(OMEMTEAMINBOX.EMAIL_ID), emailTable.getPKColumn())
                )
        );
        selectQuery.addSelectClause(filterAttributeConfigTable, teamInboxFilterConfigTable, teamInboxTable, emailTable);
        selectQuery.setWhereClause(getCriteria(userId, folderId));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Retrieves the {@link DataContainer} containing sub-filter attribute configuration data
     * for the specified user within an organization.
     * </p>
     *
     * @param orgId    Represents the organization ID
     * @param userId   Represents the user ID
     * @param folderId Represents the folder ID
     * @return {@link DataContainer} which contains the sub-filter configuration rows for the user
     */
    private DataContainer getFilterSubAttributeConfigContainer(final long orgId, final long userId, final int folderId) {
        final Table filterAttributeConfigTable = getFilterConfigTable();
        final Table subFilterConfigTable = getFilterSubConfigTable();
        final Table syncEmailSubAttributeConfigTable = getSyncEmailFilterConfigTable();

        final Column filterAttributePkColumn = filterAttributeConfigTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        final SelectQuery selectQuery = SelectQuery.get(filterAttributeConfigTable);

        selectQuery.addJoinClause(
                List.of(
                        new JoinClause(filterAttributePkColumn, subFilterConfigTable.getColumn(OMEMFILTERSUBATTRIBUTECONFIG.FILTER_CONFIG_ID), leftJoin),
                        new JoinClause(filterAttributePkColumn, syncEmailSubAttributeConfigTable.getColumn(OMEMFILTERSUBATTRIBUTESYNCEMAILCONFIG.FILTER_CONFIG_ID), leftJoin)
                )
        );
        selectQuery.addSelectClause(filterAttributeConfigTable, subFilterConfigTable, syncEmailSubAttributeConfigTable);
        selectQuery.setWhereClause(getCriteria(userId, folderId));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Builds and returns a {@link WhereClause} used to filter records in the filter configuration table
     * based on the specified user and folder.
     * </p>
     *
     * @param userId   Represents the user ID
     * @param folderId Represents the folder ID
     * @return {@link WhereClause} containing the criteria for filtering configuration records
     */
    private WhereClause getCriteria(final long userId, final int folderId) {
        final Table table = getFilterConfigTable();

        return new WhereClause(Criteria.eq(table.getColumn(OMEMFILTERATTRIBUTECONFIG.USER_ID), userId))
                .and(Criteria.eq(table.getColumn(OMEMFILTERATTRIBUTECONFIG.FOLDER_ID), folderId));
    }

    /**
     * Loads the filter attribute and their sub-attributes into the respective tables based on the provided {@link EmailListFilter}
     *
     * @param filterConfigTable Represents the filter config table
     * @param filterAttribute   Represents the {@link FilterAttribute}
     * @param emailFilter       Represents the {@link EmailListFilter}
     * @param targetContainer   {@link DataContainer} which contains the filter attribute configuration rows
     */
    private void loadFilterAttributeIntoContainer(final Table filterConfigTable, final FilterAttribute filterAttribute,
                                                  final EmailListFilter emailFilter, final DataContainer targetContainer) {
        final long orgId = emailFilter.orgId();
        final Row filterAttributeConfigRow = new Row(filterConfigTable);

        filterAttributeConfigRow.set(OMEMFILTERATTRIBUTECONFIG.USER_ID, emailFilter.getUserId());
        filterAttributeConfigRow.set(OMEMFILTERATTRIBUTECONFIG.FILTER_CONFIG_ID, filterAttribute.getId());
        filterAttributeConfigRow.setOrgId(orgId);
        filterAttributeConfigRow.set(OMEMFILTERATTRIBUTECONFIG.FOLDER_ID, emailFilter.getFolder().getId());

        targetContainer.addNewRow(filterAttributeConfigRow);

        if (filterAttribute.hasSubtypes()) {
            loadFilterSubAttributeConfigIntoContainer(orgId, filterAttributeConfigRow.getPKValue(),
                    emailFilter.getFilterAttribute().get(filterAttribute), DataContainer.create(), targetContainer
            );
        }
    }

    /**
     * <p>
     * Loads the sub-filter attributes into the provided {@link DataContainer} based on the given filter configuration.
     * </p>
     *
     * @param orgId           Organization ID.
     * @param filterPkValue   Primary key of the parent filter configuration.
     * @param subAttribute    {@link FilterOption} to determine sub-filter attributes.
     * @param sourceContainer {@link DataContainer} containing existing filter configuration rows.
     * @param targetContainer {@link DataContainer} to which updated or newly created sub-filter rows will be added.
     */
    private void loadFilterSubAttributeConfigIntoContainer(final Long orgId, final Long filterPkValue,
                                                           final FilterOption subAttribute,
                                                           final DataContainer sourceContainer, final DataContainer targetContainer) {
        if (subAttribute instanceof TeamInboxFilterOption teamInboxFilter) {
            final Table teamInboxFilterConfigTable = getTeamInboxFilterConfigTable();
            final Collection<Long> teamInboxIds = teamInboxFilter.getTeamInboxes()
                    .stream()
                    .map(TeamInbox::id)
                    .collect(Collectors.toSet());

            loadSubAttributeFilterConfigIntoContainer(orgId, filterPkValue, teamInboxFilterConfigTable, teamInboxIds,
                    teamInboxFilterConfigTable.getColumn(OMEMFILTERSUBATTRIBUTETEAMINBOXCONFIG.TEAM_INBOX_ID), sourceContainer, targetContainer);

            return;
        }

        if (subAttribute instanceof SyncEmailFilterOption syncEmailFilterOption) {
            final Table syncEmailFilterConfigTable = getSyncEmailFilterConfigTable();

            loadSubAttributeFilterConfigIntoContainer(orgId, filterPkValue, syncEmailFilterConfigTable, syncEmailFilterOption.getSyncedEmailIds(),
                    syncEmailFilterConfigTable.getColumn(OMEMFILTERSUBATTRIBUTESYNCEMAILCONFIG.SYNCED_EMAIL_ID), sourceContainer, targetContainer);

            return;
        }
        final Table table = getFilterSubConfigTable();
        final int subAttributeId = subAttribute instanceof FilterInterval ? ((FilterInterval) subAttribute).getId()
                : ((LinkedCategory) subAttribute).getId();

        final Row subAttributeRow = sourceContainer.getOrInitRow(
                table,
                new WhereClause(Criteria.eq(table.getColumn(OMEMFILTERSUBATTRIBUTECONFIG.FILTER_CONFIG_ID), filterPkValue))
        );

        if (subAttributeRow.isNewRow()) {
            subAttributeRow.setOrgId(orgId);
        }
        subAttributeRow.set(OMEMFILTERSUBATTRIBUTECONFIG.FILTER_CONFIG_ID, filterPkValue);
        subAttributeRow.set(OMEMFILTERSUBATTRIBUTECONFIG.SUB_ATTRIBUTE_ID, subAttributeId);

        targetContainer.updateRow(subAttributeRow);
    }

    /**
     * <p>
     * Handles loading of sub-attribute filter configuration into the provided {@link DataContainer}.
     * For each mapped ID in the collection, a corresponding row is either created or removed in the target container.
     * </p>
     *
     * @param orgId               Organization ID.
     * @param filterPkValue       Primary key of the parent filter configuration.
     * @param mapperTable         {@link Table} representing the mapper table for the sub-attribute.
     * @param mappedIds           Collection of mapped IDs to be added or removed from the filter configuration.
     * @param mapperColumn        {@link Column} representing the mapper column in the mapper table.
     * @param filterDataContainer {@link DataContainer} containing existing filter configuration rows.
     * @param targetContainer     {@link DataContainer} to which updated or newly created rows will be added.
     */
    private void loadSubAttributeFilterConfigIntoContainer(final Long orgId, final Long filterPkValue,
                                                           final Table mapperTable, final Collection<Long> mappedIds, final Column mapperColumn,
                                                           final DataContainer filterDataContainer, final DataContainer targetContainer) {
        final Map<Long, Row> existingRows = filterDataContainer.getRows(
                mapperTable,
                new WhereClause(Criteria.eq(mapperTable.getColumn(CoreEmailFields.FILTER_CONFIG_ID.name()), filterPkValue))
        ).collect(
                Collectors.toMap(
                        row -> row.get(mapperColumn),
                        row -> row,
                        (existing, duplicate) -> existing
                )
        );

        for (final Long mappedId : mappedIds) {

            if (Objects.isNull(existingRows.remove(mappedId))) {
                final Row row = new Row(mapperTable);

                row.setOrgId(orgId);
                row.set(CoreEmailFields.FILTER_CONFIG_ID.name(), filterPkValue);
                row.set(mapperColumn, mappedId);

                targetContainer.addNewRow(row);
            }
        }

        existingRows.values().forEach(targetContainer::deleteRow);
    }

    /**
     * updates the existing rows of filter attributes based on the provided {@link EmailListFilter}
     * and compares the {@link FilterAttribute} with the existing {@link DataContainer}
     *
     * @param filterConfigTable   Represents the filter attribute config table
     * @param emailFilter         Represents the {@link EmailListFilter}
     * @param filterDataContainer {@link DataContainer} which contains the filter attribute configuration rows
     * @param targetContainer     {@link DataContainer} which contains the updated filter attribute configuration rows
     */
    private void updateFilterAttributeRows(final Table filterConfigTable, final EmailListFilter emailFilter,
                                           final DataContainer filterDataContainer, final DataContainer targetContainer) {
        final Column filterAttributeColumn = filterConfigTable.getColumn(OMEMFILTERATTRIBUTECONFIG.FILTER_CONFIG_ID);
        final Long orgId = emailFilter.orgId();

        filterDataContainer.indexRows(filterAttributeColumn);

        final Collection<Integer> filterAttributeIds = filterDataContainer.getRows(filterConfigTable)
                .map(row -> (Integer) row.get(OMEMFILTERATTRIBUTECONFIG.FILTER_CONFIG_ID)).collect(Collectors.toSet());
        final Map<FilterAttribute, FilterOption> filterAttributes = emailFilter.getFilterAttribute();

        if (Objects.nonNull(filterAttributes)) {

            for (final FilterAttribute filterAttribute : filterAttributes.keySet()) {
                final Integer attributeId = filterAttribute.getId();
                final Row filterAttributeConfigRow = filterDataContainer.getIndexedRow(filterAttributeColumn, attributeId);
                final Long filterPkValue = Objects.isNull(filterAttributeConfigRow) ? null : filterAttributeConfigRow.getPKValue();

                if (filterAttributeIds.remove(attributeId)) {
                    targetContainer.updateRow(filterAttributeConfigRow);

                    if (filterAttribute.hasSubtypes()) {
                        loadFilterSubAttributeConfigIntoContainer(orgId, filterPkValue, filterAttributes.get(filterAttribute), filterDataContainer, targetContainer);
                    }
                } else {
                    loadFilterAttributeIntoContainer(filterConfigTable, filterAttribute, emailFilter, targetContainer);
                }
            }
        }

        for (final Integer attributeId : filterAttributeIds) {
            final Row filterAttributeRow = filterDataContainer.getIndexedRow(filterAttributeColumn, attributeId);

            if (Objects.nonNull(filterAttributeRow)) {
                targetContainer.deleteRow(filterAttributeRow);
            }
        }
    }

    /**
     * Gets the {@link List} of {@link Column} based on the provided email filter.
     *
     * @param emailFilter Represents the {@link EmailListFilter}
     * @return {@link List} of {@link Column}
     */
    public List<Column> getFilteredColumns(final EmailListFilter emailFilter) {
        return emailFilterQueryBuilder.getFilteredColumns(emailFilter);
    }

    /**
     * Gets the {@link List} of {@link JoinClause} for threads, based on the provided {@link EmailListFilter}.
     *
     * @param folder      Represents the {@link Folder}
     * @param emailFilter Represents the {@link EmailListFilter}
     * @param table       Represents the {@link Table} of recent conversation
     * @return {@link List} of {@link JoinClause}
     */
    public List<JoinClause> getThreadJoinClauses(final Folder folder, final EmailListFilter emailFilter, final Table table) {
        return emailFilterQueryBuilder.getThreadJoinClauses(folder, emailFilter, table);
    }

    /**
     * Builds the {@link WhereClause} based on {@link EmailListFilter}
     *
     * @param emailFilter Represents the {@link EmailListFilter
     * @return {@link WhereClause}
     */
    public WhereClause buildThreadWhereClause(final EmailListFilter emailFilter) {
        return emailFilterQueryBuilder.buildThreadWhereClause(emailFilter);
    }

    /**
     * <p>
     * Builds a nested {@code WhereClause} based on the given email filter, table, and query.
     * </p>
     *
     * @param emailFilter the filter containing criteria for email lists, including interval attributes.
     * @param query       the base query containing the current {@link WhereClause} and {@link JoinClause}.
     * @param start       Represent the start
     * @param limit       Represent the limit
     * @return an {@link Optional} containing the nested {@link WhereClause}.
     */
    public Optional<SelectQuery> buildConversationsQuery(final EmailListFilter emailFilter, final SelectQuery query,
                                                         final int start, final int limit) {
        return emailFilterQueryBuilder.buildConversationsSelectQuery(emailFilter, query, start, limit);
    }

    /**
     * <p>
     * Builds a filter query based on the provided email filter, table, and folder.
     * </p>
     *
     * @param emailListFilter the filter containing criteria for email lists, including interval attributes.
     * @param table           the optional table object from which columns may be queried.
     * @param whereClause     the base {@link WhereClause} used to narrow the query conditions.
     * @param folder          the {@link Folder} representing the folder context for the query (e.g., Inbox, Sent).
     * @return a {@link MultiQuery} representing the complete filter query.
     */
    public MultiQuery buildEmailFilterQuery(final EmailListFilter emailListFilter, final Table table, final WhereClause whereClause,
                                            final Folder folder, final boolean isCountQuery) {
        return emailFilterQueryBuilder.buildEmailFilterQuery(emailListFilter, table, whereClause, folder, isCountQuery);
    }
}