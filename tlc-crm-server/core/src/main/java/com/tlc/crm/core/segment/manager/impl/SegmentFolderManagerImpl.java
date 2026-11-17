package com.tlc.crm.core.segment.manager.impl;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.core.segment.manager.SegmentFolderManager;
import com.tlc.crm.core.segment.models.SegmentFilterGroup;
import com.tlc.crm.core.segment.models.SegmentFilterUserPreference;
import com.tlc.crm.core.segment.models.SegmentFolder;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.core.segment.fields.SegmentFolderFields;
import com.tlc.crm.core.sql.resource.OMKSSEGMENT;
import com.tlc.crm.core.sql.resource.OMKSSEGMENTFOLDER;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.validator.ProcessOutcome;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Provides the {@link SegmentFolder} related DB actions.
 *
 * @author Mohamed Yasar K
 * @version 1.0
 * @see AbstractConfigManager
 */
public class SegmentFolderManagerImpl extends AbstractConfigManager<SegmentFolder> implements SegmentFolderManager {

    private final UserService userService;

    private static class Instance {
        private static final SegmentFolderManagerImpl INSTANCE = new SegmentFolderManagerImpl();
    }

    private SegmentFolderManagerImpl() {
        userService = UserService.getInstance();
    }

    /**
     * Gets the instance of {@link SegmentFolderManagerImpl}.
     *
     * @return {@link SegmentFolderManagerImpl}.
     */
    public static SegmentFolderManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return OMKSSegmentFolder table.
     */
    @Override
    public Table getTable() {
        return Table.get(OMKSSEGMENTFOLDER.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param ids   Which contains the collection of folder ids.
     * @return Collection of {@link SegmentFolder} that hold the retrieved data.
     */
    @Override
    public Collection<SegmentFolder> get(final Long orgId, final Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        final Table folderTable = getTable();
        final Collection<SegmentFolder> folders = orgDataStore(orgId).get(folderTable, ids).getRows(folderTable)
                .map(row -> convertRowToFolder(orgId, row)).toList();

        if (!folders.isEmpty()) {
            final Map<Long, Long> segmentCounts = getSegmentCounts(orgId, folders.stream().map(SegmentFolder::id).toList());

            folders.forEach(folder -> folder.setSegmentCount(segmentCounts.getOrDefault(folder.id(), 0L)));
        }

        return folders;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param ids   Which contains the collection of folder ids.
     * @return Collection of {@link SegmentFolder} that hold the partial data.
     */
    @Override
    public Collection<SegmentFolder> partialGet(final Long orgId, final Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        } else {
            final Table table = getTable();
            final SelectQuery selectQuery = SelectQuery.get(table);

            selectQuery.addSelectClause(table.getColumn(OMKSSEGMENTFOLDER.NAME));
            selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), ids)));

            return orgDataStore(orgId).get(selectQuery).getRows(table).map(row -> {
                final SegmentFolder folder = new SegmentFolder();

                folder.setId(row.getPKValue());
                folder.setOrgId(orgId);
                folder.setName(row.get(OMKSSEGMENTFOLDER.NAME));

                return folder;
            }).toList();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param folder Holds the {@link SegmentFolder} attributes.
     * @return {@link SegmentFolder} that holds the created folder data.
     */
    @Override
    public SegmentFolder create(final SegmentFolder folder) {
        final DataContainer targetContainer = DataContainer.create();

        loadFolderIntoContainer(true, folder, null, targetContainer);
        orgDataStore(folder.orgId()).commitChanges(targetContainer);

        return folder;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organization id.
     * @param folders Holds the collection of {@link SegmentFolder} attributes.
     * @return The response as {@link ProcessOutcome}.
     */
    @Override
    protected ProcessOutcome update(final Long orgId, final Collection<SegmentFolder> folders) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final Table table = getTable();
        final Collection<Long> ids = folders.stream().map(SegmentFolder::id).filter(Objects::nonNull).toList();
        final DataContainer sourceContainer = orgDataStore(orgId).get(table, ids);
        final DataContainer targetContainer = DataContainer.create();

        folders.forEach(folder -> {
            loadFolderIntoContainer(false, folder, sourceContainer, targetContainer);
            processOutcome.addProcessedRecord(folder.id());
        });
        orgDataStore(orgId).commitChanges(targetContainer);

        return processOutcome;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param names Represents the folder names.
     * @return {@code true} if any name exists.
     */
    @Override
    public boolean existsByName(final Long orgId, final Collection<String> names) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.in(table.getColumn(OMKSSEGMENTFOLDER.NAME), names)));

        return orgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param ids   Represents folder ids to exclude.
     * @param names Represents folder names.
     * @return {@code true} if any name exists outside the ids.
     */
    @Override
    public boolean existsByName(final Long orgId, final Collection<Long> ids, final Collection<String> names) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.in(table.getColumn(OMKSSEGMENTFOLDER.NAME), names))
                .and(Criteria.notIn(table.getPKColumn(), ids)));

        return orgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTotalCount(final Long orgId, final SegmentFilterUserPreference segmentFilterPreference) {
        final Table folderTable = getTable();
        final CountQuery countQuery = CountQuery.get(folderTable);

        final WhereClause whereClause = buildListWhereClause(folderTable, segmentFilterPreference);

        if (Objects.nonNull(whereClause)) {
            countQuery.setWhereClause(whereClause);
        }

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<SegmentFolder> get(final Long orgId, final SegmentFilterUserPreference segmentFilterPreference) {
        final Table folderTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(folderTable);

        selectQuery.setWhereClause(buildListWhereClause(folderTable, segmentFilterPreference));
        selectQuery.addOrderByClause(getOrderByClause(segmentFilterPreference));
        selectQuery.setLimitClause(new LimitClause(segmentFilterPreference.getStart(), segmentFilterPreference.getCount()));

        final List<SegmentFolder> folders = orgDataStore(orgId).get(selectQuery).getRows(folderTable)
                .map(row -> convertRowToFolder(orgId, row)).toList();

        if (!folders.isEmpty()) {
            final Map<Long, Long> segmentCounts = getSegmentCounts(orgId, folders.stream().map(SegmentFolder::id).toList());

            folders.forEach(folder -> folder.setSegmentCount(segmentCounts.getOrDefault(folder.id(), 0L)));
        }

        return folders;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<SegmentFolder> getDropdownList(final Long orgId, final SegmentFilterGroup segmentFilterGroup,
                                                     final DropdownLimit dropdownLimit, final DropdownCriteria dropdownCriteria) {
        final SelectQuery selectQuery = getDropdownSelectQuery(segmentFilterGroup, dropdownLimit, dropdownCriteria);

        return orgDataStore(orgId).get(selectQuery).getRows(getTable()).map(row -> convertRowToFolder(orgId, row)).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDropdownCount(final Long orgId, final SegmentFilterGroup segmentFilterGroup, final DropdownCriteria dropdownCriteria) {
        final CountQuery countQuery = CountQuery.get(getTable());

        countQuery.setWhereClause(buildDropdownWhereClause(dropdownCriteria, segmentFilterGroup));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets the select query for segment folder dropdown.
     * </p>
     *
     * @param dropdownLimit    Represents the dropdown limit.
     * @param dropdownCriteria Represents the dropdown criteria.
     * @return {@link SelectQuery}.
     */
    private SelectQuery getDropdownSelectQuery(final SegmentFilterGroup segmentFilterGroup,
                                               final DropdownLimit dropdownLimit, final DropdownCriteria dropdownCriteria) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.setWhereClause(buildDropdownWhereClause(dropdownCriteria, segmentFilterGroup));
        selectQuery.addOrderByClause(new OrderByClause(table.getColumn(OMKSSEGMENTFOLDER.CREATED_TIME)));
        selectQuery.setLimitClause(new LimitClause(dropdownLimit.start(), dropdownLimit.limit()));

        return selectQuery;
    }

    /**
     * <p>
     * Builds the where clause for the segment folder dropdown.
     * </p>
     *
     * @param dropdownCriteria Represents the dropdown criteria.
     * @return {@link WhereClause}.
     */
    private WhereClause buildDropdownWhereClause(final DropdownCriteria dropdownCriteria, final SegmentFilterGroup segmentFilterGroup) {
        final Table table = getTable();
        WhereClause whereClause = null;

        if (Objects.nonNull(dropdownCriteria)) {
            whereClause = new WhereClause(dropdownCriteria.build(table.getColumn(OMKSSEGMENTFOLDER.NAME)));
        }

        if (Objects.nonNull(segmentFilterGroup)) {
            whereClause = WhereClauseUtil.connect(whereClause, new WhereClause(Criteria.eq(table.getColumn(OMKSSEGMENTFOLDER.IS_STARRED),
                    segmentFilterGroup.isStarredFolderGroup())), Connector.AND);
        }

        return whereClause;
    }

    /**
     * <p>
     * Returns the {@link OrderByClause} based on the given sorting configuration.
     * </p>
     *
     * @param preference Represents the segment filter preference.
     * @return The {@link OrderByClause} to be applied for the query.
     */
    private OrderByClause getOrderByClause(final SegmentFilterUserPreference preference) {
        final Table segmentFolderTable = getTable();
        final OrderByClause defaultOrderByClause = new OrderByClause(
                segmentFolderTable.getColumn(OMKSSEGMENTFOLDER.CREATED_TIME), OrderByClause.OrderType.DESCENDING
        );

        if (Objects.isNull(preference.getSortJson())) {
            return defaultOrderByClause;
        }
        final String sortFieldName = preference.getSortFieldName();
        final String sortType = preference.getSortType();

        if (Objects.isNull(sortFieldName) || Objects.isNull(sortType)) {
            return defaultOrderByClause;
        }
        final OrderByClause.OrderType orderType =
                switch (SortType.getSortType(sortType)) {
                    case ASCENDING -> OrderByClause.OrderType.ASCENDING;
                    case DESCENDING -> OrderByClause.OrderType.DESCENDING;
                };

        return switch (sortFieldName) {
            case "name" -> new OrderByClause(segmentFolderTable.getColumn(OMKSSEGMENTFOLDER.NAME), orderType);
            default -> defaultOrderByClause;
        };
    }

    /**
     * <p>
     * Builds the filter criteria for segment folder list and count queries
     * based on the provided search parameters.
     * </p>
     *
     * @param segmentFolderTable Represents the segment folder table.
     * @param preference         Represents the segment filter preference.
     * @return The {@link WhereClause} containing the filter criteria, or {@code null}
     * if no filters are applied.
     */
    private WhereClause buildListWhereClause(final Table segmentFolderTable, final SegmentFilterUserPreference preference) {
        WhereClause whereClause = null;

        if (Objects.isNull(preference)) {
            return whereClause;
        }
        final Boolean isStarred = preference.getIsStarred();

        if (Objects.nonNull(isStarred)) {
            whereClause = WhereClauseUtil.connect(whereClause,
                    new WhereClause(Criteria.eq(segmentFolderTable.getColumn(OMKSSEGMENTFOLDER.IS_STARRED), isStarred)), Connector.AND
            );
        }
        final String searchText = preference.getSearchText();

        if (Objects.nonNull(searchText) && !searchText.isBlank()) {
            whereClause = WhereClauseUtil.connect(whereClause, new WhereClause(Criteria.contains(segmentFolderTable.getColumn(OMKSSEGMENTFOLDER.NAME),
                    searchText.trim())), Connector.AND);
        }

        return whereClause;
    }

    /**
     * <p>
     * Loads the segment folder details into the target container.
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create event or update event.
     * @param folder          An entity that holds the {@link SegmentFolder} attributes.
     * @param sourceContainer Holds the old information of folder.
     * @param targetContainer Holds the updated information of folder.
     */
    private void loadFolderIntoContainer(final boolean isCreateEvent, final SegmentFolder folder,
                                         final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table table = getTable();
        final Long currentTime = Instant.now().toEpochMilli();
        final Row row = isCreateEvent ? new Row(table) : sourceContainer.getRow(table, folder.id());

        if (row.isNewRow()) {
            folder.setId(row.getPKValue());
            row.setOrgId(folder.orgId());
            row.set(OMKSSEGMENTFOLDER.CREATED_BY, folder.getCreatedBy().id());
            row.set(OMKSSEGMENTFOLDER.CREATED_TIME, currentTime);
        }
        row.set(OMKSSEGMENTFOLDER.NAME, folder.getName());
        row.set(OMKSSEGMENTFOLDER.IS_STARRED, folder.isStarred());
        row.set(OMKSSEGMENTFOLDER.MODIFIED_BY, folder.getModifiedBy().id());
        row.set(OMKSSEGMENTFOLDER.MODIFIED_TIME, currentTime);

        targetContainer.updateRow(row);
    }

    /**
     * <p>
     * Gets segment counts grouped by folder id.
     * </p>
     *
     * @param orgId     Represents the organization id.
     * @param folderIds Represents the folder ids.
     * @return The folder id to segment count mapping.
     */
    private Map<Long, Long> getSegmentCounts(final Long orgId, final Collection<Long> folderIds) {
        final Map<Long, Long> segmentCounts = new HashMap<>();
        final Table segmentTable = Table.get(OMKSSEGMENT.TABLE);
        final Column folderIdColumn = segmentTable.getColumn(OMKSSEGMENT.FOLDER_ID);
        final SelectQuery selectQuery = SelectQuery.get(segmentTable);

        selectQuery.addSelectClause(List.of(folderIdColumn,
                segmentTable.getColumn(OMKSSEGMENT.ID, SegmentFolderFields.SEGMENT_COUNT.getName()).count()));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(folderIdColumn, folderIds)));
        selectQuery.addGroupByClause(new GroupByClause(folderIdColumn));

        orgDataStore(orgId).getData(selectQuery).forEach(row -> {
            final Long folderId = (Long) row.get(OMKSSEGMENT.FOLDER_ID);
            final Object segmentCount = row.get(SegmentFolderFields.SEGMENT_COUNT.getName());

            if (Objects.nonNull(folderId)) {
                segmentCounts.put(folderId, Objects.isNull(segmentCount) ? 0L : Long.parseLong(String.valueOf(segmentCount)));
            }
        });

        return segmentCounts;
    }

    /**
     * <p>
     * Converts the folder {@link Row} into {@link SegmentFolder} instance.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param row   Which contains the values of segment folder.
     * @return {@link SegmentFolder} that holds the values of segment folder.
     */
    private SegmentFolder convertRowToFolder(final Long orgId, final Row row) {
        final SegmentFolder folder = new SegmentFolder();
        final Long createdBy = row.get(OMKSSEGMENTFOLDER.CREATED_BY);
        final Long modifiedBy = row.get(OMKSSEGMENTFOLDER.MODIFIED_BY);

        folder.setId(row.getPKValue());
        folder.setOrgId(orgId);
        folder.setName(row.get(OMKSSEGMENTFOLDER.NAME));
        folder.setStarred(Boolean.TRUE.equals(row.get(OMKSSEGMENTFOLDER.IS_STARRED)));
        folder.setCreatedTime(row.get(OMKSSEGMENTFOLDER.CREATED_TIME));
        folder.setModifiedTime(row.get(OMKSSEGMENTFOLDER.MODIFIED_TIME));

        if (Objects.nonNull(createdBy)) {
            folder.setCreatedBy(userService.getUser(orgId, createdBy));
        }
        if (Objects.nonNull(modifiedBy)) {
            folder.setModifiedBy(userService.getUser(orgId, modifiedBy));
        }

        return folder;
    }
}