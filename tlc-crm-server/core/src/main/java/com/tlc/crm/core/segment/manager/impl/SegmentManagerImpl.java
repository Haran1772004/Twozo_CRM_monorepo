package com.tlc.crm.core.segment.manager.impl;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.filter.api.manager.FilterManager;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.core.segment.manager.SegmentManager;
import com.tlc.crm.core.segment.converter.SegmentGroupConverter;
import com.tlc.crm.core.segment.models.Segment;
import com.tlc.crm.core.segment.models.SegmentCondition;
import com.tlc.crm.core.segment.models.SegmentConditionCriteria;
import com.tlc.crm.core.segment.models.SegmentFilterUserPreference;
import com.tlc.crm.core.segment.models.SegmentFolder;
import com.tlc.crm.core.segment.models.SegmentConditionGroup;
import com.tlc.crm.core.sql.resource.OMKSSEGMENT;
import com.tlc.crm.core.sql.resource.OMKSSEGMENTFOLDER;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.validator.ProcessOutcome;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the {@link Segment} related DB actions.
 * The current implementation fetches segment definitions directly from DB and does not use async recompute.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 * @see AbstractConfigManager
 */
public final class SegmentManagerImpl extends AbstractConfigManager<Segment> implements SegmentManager {

    private final UserService userService;
    private final SegmentFolderManagerImpl folderManager;
    private final FilterManager filterManager;
    private final SegmentGroupConverter segmentGroupConverter;

    private static class Instance {
        private static final SegmentManagerImpl INSTANCE = new SegmentManagerImpl();
    }

    private SegmentManagerImpl() {
        userService = UserService.getInstance();
        folderManager = SegmentFolderManagerImpl.getInstance();
        filterManager = FilterManager.getInstance();
        this.segmentGroupConverter = SegmentGroupConverter.getInstance();
    }

    /**
     * <p>
     * Gets the instance of {@link SegmentManagerImpl}.
     * </p>
     *
     * @return {@link SegmentManagerImpl}.
     */
    public static SegmentManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return OMKSSegment table.
     */
    @Override
    public Table getTable() {
        return Table.get(OMKSSEGMENT.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param ids   Which contains the collection of segment ids.
     * @return Collection of {@link Segment} that hold the retrieved data.
     */
    @Override
    public Collection<Segment> get(final Long orgId, final Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        final Table segmentTable = getTable();
        final Table folderTable = folderManager.getTable();

        final SelectQuery selectQuery = SelectQuery.get(segmentTable);

        selectQuery.addSelectClause(segmentTable, folderTable);
        selectQuery.addJoinClause(new JoinClause(segmentTable.getColumn(OMKSSEGMENT.FOLDER_ID),
                folderTable.getPKColumn(), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(segmentTable.getPKColumn(), ids)));
        selectQuery.addOrderByClause(new OrderByClause(segmentTable.getColumn(OMKSSEGMENT.NAME)));

        final DataContainer dataContainer = DataStoreProvider.getOrgDataStore(orgId).get(selectQuery);
        final Map<Long, Segment> segments = buildSegments(orgId, dataContainer, segmentTable);

        loadUsersAndFoldersFromContainer(orgId, dataContainer, segments);
        loadGroups(orgId, segments);

        return segments.values();
    }


    /**
     * Converts the raw segment rows from the {@link DataContainer} into a map of
     * {@link Segment} keyed by id, preserving insertion order.
     *
     * @param orgId         Represents the organization id.
     * @param dataContainer Container holding the joined query results.
     * @param segmentTable  Table representing segments.
     * @return Map of segment id to {@link Segment}.
     */
    private Map<Long, Segment> buildSegments(final Long orgId, final DataContainer dataContainer, final Table segmentTable) {
        return dataContainer.getRows(segmentTable)
                .map(row -> convertRowToSegment(orgId, row))
                .collect(Collectors.toMap(Segment::id, Function.identity(), (oldValue, newValue) -> oldValue, HashMap::new));
    }

    /**
     * <p>
     * Loads groups and conditions from the GROUPS_JSON column using the converter.
     * The converter handles JSON deserialization and batch loading of FilterCriteria.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param segments Map of segments to populate with groups and conditions.
     */
    private void loadGroups(final Long orgId, final Map<Long, Segment> segments) {
        if (segments.isEmpty()) {
            return;
        }

        segments.values().forEach(segment -> {
            final String groupsJson = segment.getGroupsJson();

            if (Objects.nonNull(groupsJson) && !groupsJson.isBlank()) {
                this.segmentGroupConverter.loadGroupsFromJson(orgId, segment, groupsJson);
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param ids   Which contains the collection of segment ids.
     * @return Collection of {@link Segment} that hold the partial data.
     */
    @Override
    public Collection<Segment> partialGet(final Long orgId, final Collection<Long> ids) {
        return get(orgId, ids);
    }

    /**
     * {@inheritDoc}
     *
     * @param segment Holds the {@link Segment} attributes.
     * @return {@link Segment} that holds the created segment data.
     */
    @Override
    public Segment create(final Segment segment) {
        final DataContainer targetContainer = DataContainer.create();

        loadSegmentIntoContainer(true, segment, null, targetContainer);
        orgDataStore(segment.orgId()).commitChanges(targetContainer);

        return segment;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organization id.
     * @param segments Holds the collection of {@link Segment} attributes.
     * @return The response as {@link ProcessOutcome}.
     */
    @Override
    protected ProcessOutcome update(final Long orgId, final Collection<Segment> segments) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final Collection<Long> ids = segments.stream().map(Segment::id).filter(Objects::nonNull).toList();
        final DataContainer sourceContainer = buildSourceContainer(orgId, ids);
        final DataContainer targetContainer = DataContainer.create();

        segments.forEach(segment -> {
            loadSegmentIntoContainer(false, segment, sourceContainer, targetContainer);
            processOutcome.addProcessedRecord(segment.id());
        });
        orgDataStore(orgId).commitChanges(targetContainer);

        return processOutcome;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                   Represents the organization id.
     * @param segmentFilterPreference Represents the filter preference used to narrow, sort and page the segment list.
     * @return Collection of {@link Segment} that match the given filter preference.
     */
    @Override
    public Collection<Segment> get(final Long orgId, final SegmentFilterUserPreference segmentFilterPreference) {
        final Table segmentTable = getTable();
        final Table segmentFolderTable = folderManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(segmentTable);

        selectQuery.addSelectClause(segmentTable, segmentFolderTable);
        selectQuery.addJoinClause(new JoinClause(segmentTable.getColumn(OMKSSEGMENT.FOLDER_ID), segmentFolderTable.getPKColumn(),
                JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(buildListWhereClause(segmentTable, segmentFilterPreference));
        selectQuery.addOrderByClause(getOrderByClause(segmentFilterPreference));
        selectQuery.setLimitClause(new LimitClause(segmentFilterPreference.getStart(), segmentFilterPreference.getCount()));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final List<Segment> segments = new ArrayList<>();
        final Map<Long, Segment> segmentMap = new LinkedHashMap<>();

        dataContainer.getRows(segmentTable).forEach(row -> {
            final Segment segment = convertRowToSegment(orgId, row);

            segments.add(segment);
            segmentMap.put(segment.id(), segment);
        });
        loadUsersAndFoldersFromContainer(orgId, dataContainer, segmentMap);

        return segments;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                   Represents the organization id.
     * @param segmentFilterPreference Represents the filter preference used to narrow the segment count.
     * @return The total count of segments matching the given filter preference.
     */
    @Override
    public long getTotalCount(final Long orgId, final SegmentFilterUserPreference segmentFilterPreference) {
        final Table segmentTable = getTable();
        final CountQuery countQuery = CountQuery.get(segmentTable);
        final WhereClause whereClause = buildListWhereClause(segmentTable, segmentFilterPreference);

        if (Objects.nonNull(whereClause)) {
            countQuery.setWhereClause(whereClause);
        }

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId            Represents the organization id.
     * @param dropdownLimit    Represents the dropdown limit.
     * @param dropdownCriteria Represents the dropdown criteria.
     * @return Collection of {@link Segment} for the dropdown list.
     */
    @Override
    public Collection<Segment> getDropdownList(final Long orgId, final DropdownLimit dropdownLimit,
                                               final DropdownCriteria dropdownCriteria) {
        final SelectQuery selectQuery = getDropdownSelectQuery(dropdownLimit, dropdownCriteria);
        final Table segmentTable = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final List<Segment> results = new ArrayList<>();
        final Map<Long, Segment> map = new LinkedHashMap<>();

        dataContainer.getRows(segmentTable).forEach(row -> {
            final Segment segment = convertRowToSegment(orgId, row);

            results.add(segment);
            map.put(segment.id(), segment);
        });
        loadUsersAndFoldersFromContainer(orgId, dataContainer, map);

        return results;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId            Represents the organization id.
     * @param dropdownCriteria Represents the dropdown criteria.
     * @return The total count of segments matching the given dropdown criteria.
     */
    @Override
    public long getDropdownCount(final Long orgId, final DropdownCriteria dropdownCriteria) {
        final CountQuery countQuery = CountQuery.get(getTable());

        countQuery.setWhereClause(buildDropdownWhereClause(dropdownCriteria));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateComputationState(final long orgId, final Collection<Long> segmentIds,
                                       final Segment.ComputationState state) {
        if (Objects.isNull(segmentIds) || segmentIds.isEmpty() || Objects.isNull(state)) {
            return;
        }
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMKSSEGMENT.COMPUTATION_STATE), state.getId());

        if (state.isComputed()) {
            updateQuery.addUpdateColumn(table.getColumn(OMKSSEGMENT.LAST_CALCULATED_AT), Instant.now().toEpochMilli());
        }
        updateQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), segmentIds)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Segment.ComputationState getComputationState(final long orgId, final long segmentId) {
        final Table segmentTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(segmentTable);

        selectQuery.addSelectClause(segmentTable.getColumn(OMKSSEGMENT.COMPUTATION_STATE));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(segmentTable.getPKColumn(), segmentId)));

        return orgDataStore(orgId).get(selectQuery).getRows(segmentTable).findFirst().map(row ->
                Segment.ComputationState.get(row.get(OMKSSEGMENT.COMPUTATION_STATE))).orElse(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateStatus(final Long orgId, final Collection<Long> segmentIds, final short statusId) {
        if (Objects.isNull(segmentIds) || segmentIds.isEmpty()) {
            return;
        }
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMKSSEGMENT.STATUS), statusId);
        updateQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), segmentIds)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addToFolder(final Long orgId, final Collection<Long> segmentIds, final Long folderId) {
        if (Objects.isNull(segmentIds) || segmentIds.isEmpty()) {
            return;
        }
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMKSSEGMENT.FOLDER_ID), folderId);
        updateQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), segmentIds)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param names Represents the names.
     * @return {@code true} if any name exists.
     */
    @Override
    public boolean existsByName(final Long orgId, final Collection<String> names) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.in(table.getColumn(OMKSSEGMENT.NAME), names)));

        return orgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param ids   Represents the segment ids.
     * @param names Represents the names.
     * @return {@code true} if any name exists.
     */
    public boolean existsByName(final Long orgId, final Collection<Long> ids,
                                final Collection<String> names) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(
                Criteria.in(table.getColumn(OMKSSEGMENT.NAME), names)).and(Criteria.notIn(table.getPKColumn(), ids))
        );

        return orgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * <p>
     * Gets the select query for segment dropdown.
     * </p>
     *
     * @param dropdownLimit    Represents the dropdown limit.
     * @param dropdownCriteria Represents the dropdown criteria.
     * @return {@link SelectQuery}.
     */
    private SelectQuery getDropdownSelectQuery(final DropdownLimit dropdownLimit, final DropdownCriteria dropdownCriteria) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.setWhereClause(buildDropdownWhereClause(dropdownCriteria));
        selectQuery.setLimitClause(new LimitClause(dropdownLimit.start(), dropdownLimit.limit()));

        return selectQuery;
    }

    /**
     * <p>
     * Builds the where clause for the segment dropdown.
     * </p>
     *
     * @param dropdownCriteria Represents the dropdown criteria.
     * @return {@link WhereClause}.
     */
    private WhereClause buildDropdownWhereClause(final DropdownCriteria dropdownCriteria) {
        final Table table = getTable();
        WhereClause whereClause = null;

        if (Objects.nonNull(dropdownCriteria)) {
            whereClause = new WhereClause(dropdownCriteria.build(table.getColumn(OMKSSEGMENT.NAME)));
        }

        return whereClause;
    }

    /**
     * <p>
     * Builds segment list filter criteria.
     * </p>
     *
     * @param segmentTable Represents the segment table.
     * @param preference   Represents the filter preference.
     * @return {@link WhereClause} for list/count queries.
     */
    private WhereClause buildListWhereClause(final Table segmentTable, final SegmentFilterUserPreference preference) {
        WhereClause whereClause = null;

        if (Objects.isNull(preference)) {
            return whereClause;
        }
        final Segment.Status status = preference.getStatus();
        final SegmentFolder segmentFolder = preference.getSegmentFolder();
        final Long folderId = Objects.isNull(segmentFolder) ? null : segmentFolder.id();

        if (Objects.nonNull(status)) {
            whereClause = new WhereClause(Criteria.eq(segmentTable.getColumn(OMKSSEGMENT.STATUS), status.getId()));
        }

        if (Objects.nonNull(folderId)) {
            final WhereClause folderClause = new WhereClause(Criteria.eq(segmentTable.getColumn(OMKSSEGMENT.FOLDER_ID), folderId));

            whereClause = Objects.isNull(whereClause) ? folderClause : whereClause.and(folderClause);
        }
        final String searchText = preference.getSearchText();

        if (Objects.nonNull(searchText) && !searchText.isBlank()) {
            final WhereClause nameClause = new WhereClause(Criteria.contains(segmentTable.getColumn(OMKSSEGMENT.NAME), searchText.trim()));

            whereClause = Objects.isNull(whereClause) ? nameClause : whereClause.and(nameClause);
        }

        return whereClause;
    }

    /**
     * <p>
     * Returns the {@link OrderByClause} based on the given sorting configuration.
     * </p>
     *
     * @param preference  Represents the filter preference which contains the sorting configuration.
     * @return The {@link OrderByClause} to be applied for the query.
     */
    private OrderByClause getOrderByClause(final SegmentFilterUserPreference preference) {
        final Table segmentTable = getTable();
        final OrderByClause defaultOrderByClause = new OrderByClause(segmentTable.getColumn(OMKSSEGMENT.CREATED_TIME), OrderByClause.OrderType.DESCENDING);

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
            case "name" -> new OrderByClause(segmentTable.getColumn(OMKSSEGMENT.NAME), orderType);
            case "createdTime" -> new OrderByClause(segmentTable.getColumn(OMKSSEGMENT.CREATED_TIME), orderType);
            case "modifiedTime" -> new OrderByClause(segmentTable.getColumn(OMKSSEGMENT.MODIFIED_TIME), orderType);
            case "folderName" -> new OrderByClause(folderManager.getTable().getColumn(OMKSSEGMENTFOLDER.NAME), orderType);
            default -> defaultOrderByClause;
        };
    }

    /**
     * <p>
     * Loads the segment details into the target container.
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create event or update event.
     * @param segment         An entity that holds the {@link Segment} attributes.
     * @param sourceContainer Holds the old information of segment.
     * @param targetContainer Holds the updated information of segment.
     */
    private void loadSegmentIntoContainer(final boolean isCreateEvent, final Segment segment,
                                          final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table segmentTable = getTable();
        final Long currentTime = Instant.now().toEpochMilli();
        final Row row = isCreateEvent ? new Row(segmentTable) : sourceContainer.getRow(segmentTable, segment.id());
        final SegmentFolder folder = segment.getFolder();
        final Long folderId = Objects.isNull(folder) ? null : folder.id();

        if (row.isNewRow()) {
            segment.setId(row.getPKValue());
            row.setOrgId(segment.orgId());
            row.set(OMKSSEGMENT.CREATED_BY, segment.getCreatedBy().id());
            row.set(OMKSSEGMENT.CREATED_TIME, currentTime);
            row.set(OMKSSEGMENT.COMPUTATION_STATE, Segment.ComputationState.UNCOMPUTED.getId());
        }
        row.set(OMKSSEGMENT.NAME, segment.getName());
        row.set(OMKSSEGMENT.STATUS, segment.getStatus().getId());
        row.set(OMKSSEGMENT.FOLDER_ID, folderId);
        row.set(OMKSSEGMENT.MODIFIED_BY, segment.getModifiedBy().id());
        row.set(OMKSSEGMENT.MODIFIED_TIME, currentTime);
        row.set(OMKSSEGMENT.LAST_CALCULATED_AT, segment.getLastCalculatedAt());
        final Collection<SegmentConditionGroup> segmentConditionGroups = segment.getSegmentConditionGroups();

        if (Objects.nonNull(segmentConditionGroups) && !segmentConditionGroups.isEmpty()) {
            loadCriteriaIntoContainer(isCreateEvent, segment, sourceContainer, targetContainer);
            row.set(OMKSSEGMENT.GROUPS, this.segmentGroupConverter.convertGroupsToJsonString(segmentConditionGroups));
        }

        targetContainer.updateRow(row);
    }

    /**
     * <p>
     * Persists FilterCriteria objects referenced by segment conditions.
     * </p>
     *
     * @param isCreateEvent   Indicates if this is a create or update operation.
     * @param segment         The segment being persisted.
     * @param sourceContainer Container with existing data.
     * @param targetContainer Container for updates.
     */
    private void loadCriteriaIntoContainer(final boolean isCreateEvent, final Segment segment,
                                           final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Collection<SegmentConditionGroup> segmentConditionGroups = segment.getSegmentConditionGroups();

        if (Objects.isNull(segmentConditionGroups)) {
            return;
        }

        for (final SegmentConditionGroup segmentConditionGroup : segmentConditionGroups) {
            final Collection<SegmentCondition> conditions = segmentConditionGroup.getConditions();

            if (Objects.isNull(conditions)) {
                continue;
            }

            for (final SegmentCondition segmentCondition : conditions) {

                if (segmentCondition.getType().isContactProperty() || segmentCondition.getType().isCompanyProperty()) {
                    final SegmentConditionCriteria conditionCriteria = segmentCondition.getConditionCriteria();

                    if (Objects.nonNull(conditionCriteria)) {
                        final FilterCriteria filterCriteria = conditionCriteria.getFilterCriteria();

                        if (hasCriteriaDetails(filterCriteria)) {
                            filterManager.loadCriteriaIntoContainer(
                                    isCreateEvent || Objects.isNull(filterCriteria.getId()),
                                    filterCriteria, sourceContainer, targetContainer
                            );
                        }
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Checks whether criteria payload contains full criteria details that should be persisted.
     * </p>
     *
     * @param criteria Holds criteria attributes.
     * @return {@code true} when criteria details are available.
     */
    private boolean hasCriteriaDetails(final FilterCriteria criteria) {
        return Objects.nonNull(criteria) && (Objects.nonNull(criteria.getFieldSource()) || Objects.nonNull(criteria.getComparator())
                || Objects.nonNull(criteria.getCriteriaField()) || Objects.nonNull(criteria.getCriteriaValues()));
    }

    /**
     * <p>
     * Builds source data required for segment update.
     * Loads segment rows and FilterCriteria for update comparison.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param ids   Represents the segment ids.
     * @return {@link DataContainer} holding existing segment rows and criteria.
     */
    private DataContainer buildSourceContainer(final Long orgId, final Collection<Long> ids) {
        final DataContainer sourceContainer = DataContainer.create();

        if (ids.isEmpty()) {
            return sourceContainer;
        }
        final Table segmentTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(segmentTable);

        selectQuery.addSelectClause(segmentTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(segmentTable.getPKColumn(), ids)));
        sourceContainer.append(orgDataStore(orgId).get(selectQuery));
        appendSegmentCriteriaRows(orgId, sourceContainer);

        return sourceContainer;
    }

    /**
     * <p>
     * Appends criteria rows used by segment conditions.
     * Uses converter to extract criteria IDs from JSON.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param sourceContainer Holds the accumulated rows.
     */
    private void appendSegmentCriteriaRows(final Long orgId, final DataContainer sourceContainer) {
        final Table segmentTable = getTable();
        final Collection<Long> criteriaIds = new HashSet<>();

        sourceContainer.getRows(segmentTable).forEach(row -> {
            criteriaIds.addAll(this.segmentGroupConverter.extractCriteriaIds(row.get(OMKSSEGMENT.GROUPS).toString()));
        });

        if (!criteriaIds.isEmpty()) {
            sourceContainer.append(filterManager.getCriteriaContainer(orgId, criteriaIds));
        }
    }

    /**
     * <p>
     * Converts the segment {@link Row} into {@link Segment} instance.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param row   Which contains the values of segment.
     * @return {@link Segment} that holds the values of segment.
     */
    private Segment convertRowToSegment(final Long orgId, final Row row) {
        final Segment segment = new Segment();
        final Long folderId = row.get(OMKSSEGMENT.FOLDER_ID);

        segment.setId(row.getPKValue());
        segment.setOrgId(orgId);
        segment.setName(row.get(OMKSSEGMENT.NAME));
        segment.setStatus(Segment.Status.get(row.get(OMKSSEGMENT.STATUS)));
        segment.setCreatedTime(row.get(OMKSSEGMENT.CREATED_TIME));
        segment.setModifiedTime(row.get(OMKSSEGMENT.MODIFIED_TIME));
        segment.setLastCalculatedAt(row.get(OMKSSEGMENT.LAST_CALCULATED_AT));
        segment.setComputationState(Segment.ComputationState.get(row.get(OMKSSEGMENT.COMPUTATION_STATE)));
        segment.setGroupsJson(row.get(OMKSSEGMENT.GROUPS));

        if (Objects.nonNull(folderId)) {
            final SegmentFolder folder = new SegmentFolder();

            folder.setId(folderId);
            folder.setOrgId(orgId);
            segment.setFolder(folder);
        }

        return segment;
    }

    /**
     * <p>
     * Batch attaches user and folder details for segments using rows already available in the given
     * {@link DataContainer}. This avoids per-row user/folder lookups and reduces the work to a single
     * pass over users combined with a map-based match against folders, instead of comparing every
     * folder row against every segment.
     * </p>
     *
     * @param orgId     Represents the organization id.
     * @param container Holds the segment, folder, group, condition and criteria rows fetched for this call.
     * @param segments  Holds the segments, keyed by id, that should receive the resolved user and folder details.
     */
    private void loadUsersAndFoldersFromContainer(final Long orgId, final DataContainer container, final Map<Long, Segment> segments) {
        if (segments.isEmpty()) {
            return;
        }
        final Table segmentTable = getTable();
        final Table folderTable = folderManager.getTable();
        final Collection<Long> userIds = new HashSet<>();

        container.getRows(segmentTable).forEach(row -> {
            addUserId(row.get(OMKSSEGMENT.CREATED_BY), userIds);
            addUserId(row.get(OMKSSEGMENT.MODIFIED_BY), userIds);
        });

        container.getRows(folderTable).forEach(row -> {
            addUserId(row.get(OMKSSEGMENTFOLDER.CREATED_BY), userIds);
            addUserId(row.get(OMKSSEGMENTFOLDER.MODIFIED_BY), userIds);
        });

        final Map<Long, CrmUser> usersById = userIds.isEmpty() ? Map.of()
                : userService.getUsers(orgId, userIds).stream()
                .collect(Collectors.toMap(CrmUser::id, Function.identity()));

        if (!usersById.isEmpty()) {
            container.getRows(segmentTable).forEach(row -> {
                final Segment segment = segments.get(row.getPKValue());

                if (Objects.isNull(segment)) {
                    return;
                }
                final Long createdBy = row.get(OMKSSEGMENT.CREATED_BY);
                final Long modifiedBy = row.get(OMKSSEGMENT.MODIFIED_BY);

                if (Objects.nonNull(createdBy)) {
                    segment.setCreatedBy(usersById.get(createdBy));
                }
                if (Objects.nonNull(modifiedBy)) {
                    segment.setModifiedBy(usersById.get(modifiedBy));
                }
            });
        }
        loadFolders(orgId, container, folderTable, segments, usersById);
    }

    /**
     * <p>
     * Records a user id for batched lookup, skipping {@code null} values.
     * </p>
     *
     * @param userId  Represents the candidate user id, may be {@code null}.
     * @param userIds Holds the accumulated, deduplicated user ids.
     */
    private void addUserId(final Long userId, final Collection<Long> userIds) {
        if (Objects.nonNull(userId)) {
            userIds.add(userId);
        }
    }

    /**
     * <p>
     * Attaches fully resolved {@link SegmentFolder} details to every segment that references it.
     * Segments are first grouped by folder id so each folder row is matched in constant time,
     * instead of scanning the full segment collection for every folder row.
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param container   Holds the folder rows fetched for this call.
     * @param folderTable Represents the segment folder table.
     * @param segments    Holds the segments, keyed by id, that may reference a folder.
     * @param usersById   Holds the resolved users, keyed by id, used to populate folder audit fields.
     */
    private void loadFolders(final Long orgId, final DataContainer container, final Table folderTable,
                             final Map<Long, Segment> segments, final Map<Long, CrmUser> usersById) {
        final Map<Long, List<Segment>> segmentsByFolderId = new HashMap<>();

        segments.values().forEach(segment -> {
            final SegmentFolder folder = segment.getFolder();

            if (Objects.nonNull(folder)) {
                segmentsByFolderId.computeIfAbsent(folder.id(), key -> new ArrayList<>()).add(segment);
            }
        });

        if (segmentsByFolderId.isEmpty()) {
            return;
        }

        container.getRows(folderTable).forEach(row -> {
            final Long id = row.getPKValue();
            final List<Segment> matchingSegments = segmentsByFolderId.get(id);

            if (Objects.isNull(matchingSegments)) {
                return;
            }
            final SegmentFolder folder = new SegmentFolder();

            folder.setId(id);
            folder.setOrgId(orgId);
            folder.setName(row.get(OMKSSEGMENTFOLDER.NAME));
            folder.setStarred(row.get(OMKSSEGMENTFOLDER.IS_STARRED));
            final Long folderCreatedBy = row.get(OMKSSEGMENTFOLDER.CREATED_BY);
            final Long folderModifiedBy = row.get(OMKSSEGMENTFOLDER.MODIFIED_BY);

            if (Objects.nonNull(folderCreatedBy)) {
                folder.setCreatedBy(usersById.get(folderCreatedBy));
            }
            if (Objects.nonNull(folderModifiedBy)) {
                folder.setModifiedBy(usersById.get(folderModifiedBy));
            }

            matchingSegments.forEach(segment -> segment.setFolder(folder));
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(final Long orgId, final Set<Long> segmentIds) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);
        final WhereClause whereClause = new WhereClause(Criteria.in(table.getPKColumn(), segmentIds));

        countQuery.setWhereClause(whereClause);

        return orgDataStore(orgId).get(countQuery) == segmentIds.size();
    }
}