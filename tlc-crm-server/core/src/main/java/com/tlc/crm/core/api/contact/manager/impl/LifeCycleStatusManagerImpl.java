package com.tlc.crm.core.api.contact.manager.impl;

import com.google.common.base.Supplier;
import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.crm.core.sql.resource.OMCOLIFECYCLESTAGE;
import com.tlc.crm.core.sql.resource.OMCOLIFECYCLESTATUS;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.HavingClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.model.ErrorDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the {@link LifeCycleStatus} related DB actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class LifeCycleStatusManagerImpl extends AbstractConfigManager<LifeCycleStatus> implements LifeCycleStatusManager {

    private static class Instance {
        private static final LifeCycleStatusManagerImpl INSTANCE = new LifeCycleStatusManagerImpl();
    }

    private LifeCycleStatusManagerImpl() {
    }

    public static LifeCycleStatusManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(OMCOLIFECYCLESTATUS.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param ids   Represents the status ids.
     * @return Collection of statuses.
     */
    @Override
    public Collection<LifeCycleStatus> get(final Long orgId, final Collection<Long> ids) {
        final Table lifeCycleStatusTable = getTable();
        final Stream<Row> rows = orgDataStore(orgId).get(lifeCycleStatusTable, ids).getRows(lifeCycleStatusTable);

        return rows.map(this::convertRowToModel).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param lifeCycleStatus An entity that holds the contact status attributes which have to be saved into database.
     */
    @Override
    public LifeCycleStatus create(final LifeCycleStatus lifeCycleStatus) {
        final DataContainer targetContainer = DataContainer.create();

        loadStatusIntoContainer(true, lifeCycleStatus.getStageId(), lifeCycleStatus, null, targetContainer);
        orgDataStore(lifeCycleStatus.orgId()).commitChanges(targetContainer);

        return lifeCycleStatus;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Organization id.
     * @param statuses Collection of sources which have to be updated into database.
     * @return Updated status.
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<LifeCycleStatus> statuses) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<Long> ids = statuses.stream().map(LifeCycleStatus::id).filter(Objects::nonNull).toList();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final DataContainer entityContainer = orgDataStore(orgId).get(getTable(), ids);

        statuses.forEach(lifecycleStatus -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadStatusIntoContainer(false, lifecycleStatus.getStageId(), lifecycleStatus, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(lifecycleStatus.id());
            } catch (ErrorCode errorCode) {
                LoggerUtil.logError(LOGGER, errorCode, "Life cycle status update failed. Org ID: {}, Entity ID: {}", orgId, lifecycleStatus.id());
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(ContactErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, lifecycleStatus.id().toString()));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(ContactErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }
        return processOutcome;
    }

    /**
     * {@inheritDoc}
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event.
     * @param stageId         Represents the lifecycle stage id.
     * @param lifeCycleStatus An entity that holds the status attributes.
     * @param sourceContainer Holds the user defined and system field values of existing status.
     * @param targetContainer Holds the updated information of user defined and system field values.
     */
    @Override
    public void loadStatusIntoContainer(final boolean isCreateEvent, final Long stageId, final LifeCycleStatus lifeCycleStatus,
                                        final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table statusTable = getTable();
        final Long id = lifeCycleStatus.id();
        final Long orgId = lifeCycleStatus.orgId();
        final Short order = lifeCycleStatus.getOrder();
        final LifeCycleStatus.Label label = lifeCycleStatus.getLabel();
        final Supplier<Row> rowCreator = () -> new Row(statusTable);
        final Row statusRow = isCreateEvent ? rowCreator.get()
                : Optional.ofNullable(sourceContainer.getRow(statusTable, id)).map(Row::new).orElseGet(rowCreator);

        if (!isCreateEvent && Objects.isNull(statusRow)) {
            throw ErrorCode.get(ContactErrorCodes.LIFE_CYCLE_STATUS_NOT_FOUND, String.format("Org ID : %d, Entity ID : %d", orgId, id));
        }

        if (statusRow.isNewRow()) {
            statusRow.setOrgId(orgId);
            statusRow.set(OMCOLIFECYCLESTATUS.STAGE_ID, stageId);
            statusRow.set(OMCOLIFECYCLESTATUS.IS_SYSTEM_STATUS, lifeCycleStatus.isSystemStatus());
            lifeCycleStatus.setId(statusRow.getPKValue());
        }

        if (Objects.nonNull(label)) {
            statusRow.set(OMCOLIFECYCLESTATUS.LABEL, label.getId());
        }

        if (Objects.isNull(order)) {
            if (isCreateEvent) {
                statusRow.set(OMCOLIFECYCLESTATUS.ORDER, getNextStatusOrder(orgId, stageId, targetContainer));
            }
        } else {
            statusRow.set(OMCOLIFECYCLESTATUS.ORDER, order);
        }
        statusRow.set(OMCOLIFECYCLESTATUS.NAME, lifeCycleStatus.getName());
        targetContainer.updateRow(statusRow);
    }

    /**
     * {@inheritDoc}
     *
     * @param row Represents the attributes of {@link LifeCycleStatus}.
     * @return {@link LifeCycleStatus}.
     */
    @Override
    public LifeCycleStatus convertRowToModel(final Row row) {
        final LifeCycleStatus lifeCycleStatus = new LifeCycleStatus();
        final Short labelId = row.get(OMCOLIFECYCLESTATUS.LABEL);
        final Boolean isSystemStatus = row.get(OMCOLIFECYCLESTATUS.IS_SYSTEM_STATUS);

        lifeCycleStatus.setId(row.getPKValue());
        lifeCycleStatus.setOrgId(row.getOrgId());
        lifeCycleStatus.setName(row.get(OMCOLIFECYCLESTATUS.NAME));
        lifeCycleStatus.setOrder(row.get(OMCOLIFECYCLESTATUS.ORDER));
        lifeCycleStatus.setStageId(row.get(OMCOLIFECYCLESTATUS.STAGE_ID));

        if (Objects.nonNull(isSystemStatus)) {
            lifeCycleStatus.setSystemStatus(isSystemStatus);
        }

        if (Objects.nonNull(labelId)) {
            lifeCycleStatus.setLabel(LifeCycleStatus.Label.get(labelId));
        }

        return lifeCycleStatus;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId            Represents the organization id.
     * @param lifecycleStageId Represents the lifecycle stage id.
     * @param limit            Which holds the limit configuration.
     * @param criteria         Which configure the criteria.
     * @return Collection of lifeCycleStatuses.
     */
    @Override
    public Collection<LifeCycleStatus> getLifeCycleStatusDropdownList(final Long orgId, Long lifecycleStageId, final DropdownLimit limit,
                                                                      final DropdownCriteria criteria) {
        final Table lifeCycleStatusTable = getTable();
        final Table lifeCycleStageTable = LifeCycleStageManager.getInstance().getTable();
        final Column lifeCycleStatusNameColumn = lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.NAME);
        final SelectQuery selectQuery = SelectQuery.get(lifeCycleStatusTable);
        final WhereClause whereClause = new WhereClause(criteria.build(lifeCycleStatusNameColumn));

        selectQuery.addSelectClause(lifeCycleStatusNameColumn);
        selectQuery.addSelectClause(lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.LABEL));
        selectQuery.setLimitClause(limit.getLimitClause());
        selectQuery.addOrderByClause(new OrderByClause(lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.ORDER)));

        if (Objects.isNull(lifecycleStageId)) {
            selectQuery.addJoinClause(new JoinClause(lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.STAGE_ID), lifeCycleStageTable.getPKColumn()));
            selectQuery.setWhereClause(whereClause.and(Criteria.eq(lifeCycleStageTable.getColumn(OMCOLIFECYCLESTAGE.IS_ACTIVE), true)));
        } else {
            selectQuery.setWhereClause(whereClause.and(Criteria.eq(lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.STAGE_ID), lifecycleStageId)));
        }
        final Stream<Row> rows = orgDataStore(orgId).get(selectQuery).getRows(lifeCycleStatusTable);

        return rows.map(this::convertRowToModel).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents the organisation id.
     * @param statusOrders Represents the orders of the lifecycle statuses.
     */
    @Override
    public void updateOrder(final long orgId, final Map<Short, Long> statusOrders) {
        final Table lifecycleStatusTable = getTable();
        final List<UpdateQuery> updateQueries = new ArrayList<>();

        for (final Map.Entry<Short, Long> statusOrder : statusOrders.entrySet()) {
            final UpdateQuery updateQuery = UpdateQuery.get(lifecycleStatusTable);

            updateQuery.addUpdateColumn(lifecycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.ORDER), statusOrder.getKey());
            updateQuery.setWhereClause(new WhereClause(Criteria.eq(lifecycleStatusTable.getPKColumn(), statusOrder.getValue())));
            updateQueries.add(updateQuery);
        }
        orgDataStore(orgId).update(updateQueries);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                 Represents the organization ID.
     * @param lifeCycleStatusValues Which contains the collection of lifecycle status names.
     * @return {@link Collection} of lifecycle statuses.
     */
    @Override
    public Collection<LifeCycleStatus> getLifeCycleStatuses(final Long orgId, final Collection<String> lifeCycleStatusValues) {
        final Table lifeCycleStatusTable = getTable();
        final Column lifeCycleStatusNameColumn = lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.NAME);
        final Column lifeCycleStatusPkColumn = lifeCycleStatusTable.getPKColumn();
        final Column orgIdColumn = lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.ORG_ID);
        final SelectQuery selectQuery = SelectQuery.get(lifeCycleStatusTable);
        final SelectQuery firstLifeCycleStatusSelectQuery = SelectQuery.get(lifeCycleStatusTable);

        firstLifeCycleStatusSelectQuery.addSelectClause(lifeCycleStatusPkColumn);
        firstLifeCycleStatusSelectQuery.addOrderByClause(new OrderByClause(lifeCycleStatusPkColumn));
        firstLifeCycleStatusSelectQuery.setLimitClause(new LimitClause(1));

        selectQuery.addSelectClause(List.of(lifeCycleStatusNameColumn, lifeCycleStatusPkColumn, orgIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(lifeCycleStatusNameColumn, lifeCycleStatusValues))
                .or(Criteria.eq(lifeCycleStatusPkColumn, firstLifeCycleStatusSelectQuery)));

        final Stream<Row> lifeCycleStatusRows = orgDataStore(orgId).get(selectQuery).getRows(lifeCycleStatusTable);

        return lifeCycleStatusRows.map(this::convertRowToModel).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId              Represents the organisation id.
     * @param lifeCycleStatusIds Contains the collection of life cycle status ids.
     * @return true if all the life cycle statuses are exists.
     */
    @Override
    public boolean exists(final Long orgId, final Set<Long> lifeCycleStatusIds) {
        return lifeCycleStatusIds.size() == count(orgId, new WhereClause(Criteria.in(getTable().getPKColumn(), lifeCycleStatusIds)));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId             Represents the organisation id.
     * @param lifeCycleStageId  Represents the life cycle stage id.
     * @param lifeCycleStatusId Represents the life cycle status id.
     * @return true if the life cycle status is present.
     */
    @Override
    public boolean exists(final Long orgId, final Long lifeCycleStageId, final Long lifeCycleStatusId) {
        final Table lifecycleStageTable = getTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(lifecycleStageTable.getColumn(OMCOLIFECYCLESTATUS.STAGE_ID), lifeCycleStageId))
                .and(Criteria.eq(lifecycleStageTable.getPKColumn(), lifeCycleStatusId));

        return 0 < count(orgId, whereClause);
    }

    /**
     * <p>
     * Checks whether all the given life cycle statuses are exists or not.
     * </p>
     *
     * @param orgId    Represents the organisation id.
     * @param statuses Contains the collection of life cycle status names.
     * @return true if all the life cycle statuses exist.
     */
    @Override
    public boolean exists(final Long orgId, final Collection<LifeCycleStatus> statuses) {
        final Table lifecycleStatusTable = getTable();
        final Collection<Long> statusIds = new HashSet<>();
        final Collection<String> statusNames = new HashSet<>();
        WhereClause whereClause = null;

        for (final LifeCycleStatus status : statuses) {
            final Long statusId = status.id();
            final String statusName = status.getName();

            if (Objects.nonNull(statusId)) {
                statusIds.add(statusId);
            }

            if (Objects.nonNull(statusName)) {
                statusNames.add(statusName);
            }
        }

        if (!statusIds.isEmpty()) {
            whereClause = new WhereClause(Criteria.notIn(lifecycleStatusTable.getPKColumn(), statusIds));
        }

        if (!statusNames.isEmpty()) {
            final Criteria criteria = Criteria.in(lifecycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.NAME), statusNames);

            whereClause = Objects.isNull(whereClause) ? new WhereClause(criteria) : whereClause.and(criteria);
        }
        return 0 < count(orgId, whereClause);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId            Represent the organization id.
     * @param lifecycleStageId Represent the lifecycle stage id.
     * @return {@link Long} of lifecycle status id.
     */
    @Override
    public Long getDefaultStatusId(final Long orgId, final Long lifecycleStageId) {
        return getDefaultLifeCycleStatusRow(orgId, 1, lifecycleStageId, List.of(getTable().getPKColumn())).getPKValue();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId             Represent the organization id.
     * @param lifecycleStageIds Represent the lifecycle stage id.
     * @return {@link Long} of lifecycle status id.
     */
    @Override
    public Map<Long, Long> getDefaultStatusIdsByStage(final Long orgId, final Collection<Long> lifecycleStageIds) {
        final Table lifeCycleStatusTable = getTable();
        final Column stageIdColumn = lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.STAGE_ID);
        final SelectQuery selectQuery = SelectQuery.get(lifeCycleStatusTable);

        selectQuery.addSelectClause(lifeCycleStatusTable.getPKColumn());
        selectQuery.addSelectClause(stageIdColumn);
        selectQuery.addSelectClause(lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.ORDER));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(stageIdColumn, lifecycleStageIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Map<Long, Long> defaultStatusIds = new HashMap<>();

        for (final Long lifecycleStageId : lifecycleStageIds) {
            final Stream<Row> statusRows = dataContainer.getRows(lifeCycleStatusTable, new WhereClause(Criteria.eq(stageIdColumn, lifecycleStageId)));
            final Optional<Long> optionalStatusId = statusRows.min(Comparator.comparing(statusRow -> statusRow.get(OMCOLIFECYCLESTATUS.ORDER))).map(Row::getPKValue);

            optionalStatusId.ifPresent(statusId -> defaultStatusIds.put(lifecycleStageId, statusId));
        }
        return defaultStatusIds;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                Represent the organization id.
     * @param lifecycleStageId     Represent the lifecycle stage id.
     * @param levelOfDefaultStatus Represent the level of the default status.
     *                             NOTE: The first status is the default. If the first status has to be deactivated,
     *                             the next status becomes the default, and this pattern continues through to the last status.
     * @return {@link Long} of lifecycle status id.
     */
    @Override
    public LifeCycleStatus getDefaultStatus(final Long orgId, final Long lifecycleStageId, final int levelOfDefaultStatus) {
        final Optional<Row> defaultLifeCycleStatusRow = Optional.ofNullable(getDefaultLifeCycleStatusRow(orgId, levelOfDefaultStatus, lifecycleStageId, Collections.emptyList()));

        return defaultLifeCycleStatusRow.map(this::convertRowToModel).orElse(null);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId            Represent the organization id.
     * @param lifecycleStageId Represent the lifecycle stage id.
     * @return the default {@link LifeCycleStatus}.
     */
    @Override
    public LifeCycleStatus getDefaultStatus(final long orgId, final Long lifecycleStageId) {
        return convertRowToModel(getDefaultLifeCycleStatusRow(orgId, 1, lifecycleStageId, Collections.emptyList()));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organisation id.
     * @param statusIds Represents the lifecycle status ids.
     * @return Labeled statuses.
     */
    @Override
    public Map<Long, LifeCycleStatus.Label> getLabelsByStatus(final Long orgId, final Set<Long> statusIds) {
        final Table lifecycleStatusTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(lifecycleStatusTable);
        final Collection<Short> wonAndLostLabelIds = List.of(LifeCycleStatus.Label.CLOSED_WON.getId(), LifeCycleStatus.Label.CLOSED_LOST.getId());
        final Column labelColumn = lifecycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.LABEL);

        selectQuery.addSelectClause(labelColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(lifecycleStatusTable.getPKColumn(), statusIds))
                .and(Criteria.in(labelColumn, wonAndLostLabelIds)));
        final Stream<Row> lifecycleStatusRows = orgDataStore(orgId).get(selectQuery).getRows(lifecycleStatusTable);

        return lifecycleStatusRows.collect(Collectors.toMap(Row::getPKValue, row -> LifeCycleStatus.Label.get(row.get(OMCOLIFECYCLESTATUS.LABEL))));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organisation id.
     * @param statusIds Represents the status ids.
     * @return True, if the given statuses are deletable or not.
     */
    @Override
    public boolean isDeletable(final Long orgId, final Set<Long> statusIds) {
        final Table statusTable = getTable();
        final Table stageTable = LifeCycleStageManager.getInstance().getTable();
        final CountQuery countQuery = CountQuery.get(statusTable);
        final SelectQuery singleStatusQuery = SelectQuery.get(statusTable);
        final Collection<Short> wonAndLostLabelIds = List.of(LifeCycleStatus.Label.CLOSED_WON.getId(), LifeCycleStatus.Label.CLOSED_LOST.getId());
        final Column stageIdColumn = statusTable.getColumn(OMCOLIFECYCLESTATUS.STAGE_ID);
        final Column pkColumn = statusTable.getPKColumn();

        singleStatusQuery.addSelectClause(stageIdColumn);
        singleStatusQuery.addGroupByClause(new GroupByClause(stageIdColumn));
        singleStatusQuery.addHavingClause(new HavingClause(Criteria.eq(pkColumn.count(), 1)));

        countQuery.addJoinClause(new JoinClause(stageIdColumn, stageTable.getPKColumn()));
        countQuery.setWhereClause(new WhereClause(Criteria.in(pkColumn, statusIds))
                .and(new WhereClause(Criteria.eq(stageTable.getColumn(OMCOLIFECYCLESTAGE.IS_LAST_STAGE), true))
                .and(Criteria.in(statusTable.getColumn(OMCOLIFECYCLESTATUS.LABEL), wonAndLostLabelIds))
                .or(Criteria.in(stageIdColumn, singleStatusQuery))));

        return 0 == orgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represent the organization id.
     * @param stageId Represent the lifecycle stage id.
     * @return Count of the statuses.
     */
    @Override
    public long getStatusesCount(final Long orgId, final Long stageId) {
        final Table lifecycleStatusTable = getTable();
        final CountQuery countQuery = CountQuery.get(lifecycleStatusTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(lifecycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.STAGE_ID), stageId)));
        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Updates the order of the lifecycle status that follows the removed status.
     * </p>
     *
     * @param orgId              Represents the organisation id.
     * @param stageId            Represents the stage id.
     * @param removedStatusOrder Represents the removed status order.
     */
    @Override
    public void reArrangeStatusOrder(final long orgId, final long stageId, final short removedStatusOrder) {
        final Table statusTable = getTable();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final SelectQuery selectQuery = SelectQuery.get(statusTable);
        final Column pkColumn = statusTable.getPKColumn();
        final Column orderColumn = statusTable.getColumn(OMCOLIFECYCLESTATUS.ORDER);

        selectQuery.addSelectClause(pkColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(statusTable.getColumn(OMCOLIFECYCLESTATUS.STAGE_ID), stageId))
                .and(Criteria.gt(orderColumn, removedStatusOrder)).and(Criteria.eq(statusTable.getColumn(OMCOLIFECYCLESTATUS.LABEL), null)));
        selectQuery.addOrderByClause(new OrderByClause(orderColumn));
        final Stream<Row> statusRows = orgDataStore.get(selectQuery).getRows(statusTable);
        final List<UpdateQuery> updateQueries = new ArrayList<>();
        final Counter orderCounter = new Counter(removedStatusOrder);

        statusRows.forEach(statusRow -> {
            final UpdateQuery updateQuery = UpdateQuery.get(statusTable);

            updateQuery.addUpdateColumn(orderColumn, orderCounter.getAndIncrement());
            updateQuery.setWhereClause(new WhereClause(Criteria.eq(pkColumn, statusRow.getPKValue())));
            updateQueries.add(updateQuery);
        });
        orgDataStore.update(updateQueries);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organisation id.
     * @param stageIds  Represents the stage ids.
     * @param statusIds Represents the status ids.
     * @return Remaining status ids.
     */
    @Override
    public Collection<Long> getRemainingStatuses(final Long orgId, final Set<Long> stageIds, final Set<Long> statusIds) {
        final Table lifecycleStatusTable = getTable();
        final Column lifecycleStatusPkColumn = lifecycleStatusTable.getPKColumn();
        WhereClause whereClause = new WhereClause(Criteria.in(lifecycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.STAGE_ID), stageIds));

        if (!statusIds.isEmpty()) {
            whereClause = whereClause.and(Criteria.notIn(lifecycleStatusPkColumn, statusIds));
        }
        final DataContainer dataContainer = orgDataStore(orgId).get(lifecycleStatusTable, whereClause, List.of(lifecycleStatusPkColumn));

        return dataContainer.getRows(lifecycleStatusTable).map(Row::getPKValue).collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId            Represents an organisation.
     * @param lifeCycleStageId Represents the lifecycle stage id.
     * @param criteria         Holds the data retrieval criteria.
     * @return Total status count.
     */
    @Override
    public long getTotalRecordsCount(final Long orgId, final Long lifeCycleStageId, final DropdownCriteria criteria) {
        final Table lifeCycleStatusTable = getTable();
        final CountQuery countQuery = CountQuery.get(lifeCycleStatusTable);
        final WhereClause whereClause = new WhereClause(criteria.build(lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTAGE.NAME)));

        countQuery.setWhereClause(Objects.isNull(lifeCycleStageId) ? whereClause : whereClause.and(Criteria.eq(lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.STAGE_ID), lifeCycleStageId)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param ids   Represents the status ids.
     * @return Collection of statuses.
     */
    @Override
    public Collection<LifeCycleStatus> partialGet(final Long orgId, final Collection<Long> ids) {
        final Table lifeCycleStatusTable = getTable();
        final WhereClause whereClause = new WhereClause(Criteria.in(lifeCycleStatusTable.getPKColumn(), ids));
        final Column nameColumn = lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.NAME);
        final DataContainer dataContainer = orgDataStore(orgId).get(lifeCycleStatusTable, whereClause,
                List.of(nameColumn, lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.LABEL)));
        final Stream<Row> lifeCycleStatusRows = dataContainer.getRows(lifeCycleStatusTable);

        return lifeCycleStatusRows.map(this::convertRowToModel).toList();
    }

    /**
     * <p>
     * Fetches the order of the next lifecycle status in the given stage.
     * </p>
     *
     * @param orgId           Represents the organisation id.
     * @param stageId         Represents the stage id.
     * @param targetContainer Holds the updated information of user defined and system field values.
     * @return Next status order.
     */
    private short getNextStatusOrder(final long orgId, final long stageId, final DataContainer targetContainer) {
        final Table lifecycleStatusTable = getTable();
        final Counter counter = new Counter();

        if (0 < targetContainer.size(lifecycleStatusTable)) {
            final Stream<Row> lifecycleStatusRows = targetContainer.getRows(lifecycleStatusTable);

            lifecycleStatusRows.forEach(lifecycleStatusRow -> {
                final Short label = lifecycleStatusRow.get(OMCOLIFECYCLESTATUS.LABEL);
                final short order = lifecycleStatusRow.get(OMCOLIFECYCLESTATUS.ORDER);

                if (Objects.isNull(label) && order > counter.getCount()) {
                    counter.setCount(order);
                }
            });
        } else {
            final Column orderColumn = lifecycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.ORDER);
            final WhereClause whereClause = new WhereClause(Criteria.eq(lifecycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.STAGE_ID), stageId))
                    .and(Criteria.eq(lifecycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.LABEL), null));
            final DataContainer dataContainer = orgDataStore(orgId).get(lifecycleStatusTable, whereClause, List.of(orderColumn));
            final short maxOrder = dataContainer.getRows(lifecycleStatusTable).max(Comparator.comparing(stageRow ->
                    stageRow.get(orderColumn))).map(statusRow -> (short) statusRow.get(orderColumn)).orElseGet(() -> (short) 0);

            counter.setCount(maxOrder);
        }
        return (short) counter.incrementAndGet();
    }

    /**
     * <p>
     * Counts the {@link LifeCycleStatus} which matches the given criteria.
     * </p>
     *
     * @param orgId       Represents the organisation id.
     * @param whereClause Holds the retrieval criteria.
     * @return Count of the {@link LifeCycleStatus}.
     */
    private long count(final Long orgId, final WhereClause whereClause) {
        final CountQuery countQuery = CountQuery.get(getTable());

        countQuery.setWhereClause(whereClause);
        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets the default lifecycle stage row for the given organization
     * </p>
     *
     * @param orgId                Represent the organization id.
     * @param levelOfDefaultStatus Represent the level of the default status.
     *                             NOTE: The first status is the default. If the first status has to be deactivated,
     *                             the next status becomes the default, and this pattern continues through to the last status.
     * @param lifecycleStageId     Represent the lifecycle stage id.
     * @param selectedColumns      Represents the column to be selected.
     * @return the default life cycle status {@link Row}.
     */
    private Row getDefaultLifeCycleStatusRow(final long orgId, final int levelOfDefaultStatus, final long lifecycleStageId,
                                             final Collection<Column> selectedColumns) {
        final Table lifeCycleStatusTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(lifeCycleStatusTable);

        selectQuery.addSelectClause(selectedColumns);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.STAGE_ID), lifecycleStageId)));
        selectQuery.addOrderByClause(new OrderByClause(lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.ORDER)));
        selectQuery.setLimitClause(new LimitClause(levelOfDefaultStatus, 1));

        return orgDataStore(orgId).get(selectQuery).getRow(lifeCycleStatusTable);
    }

    /**
     * <p>
     *  {@inheritDoc}
     * </p>
     *
     * @param orgId             the ID of the organization to which the life cycle status belongs.
     * @param lifeCycleStatusId the ID of the life cycle status to retrieve.
     * @return a {@link LifeCycleStatus} object containing partial details of the life cycle status.
     */
    @Override
    public String getLifeCycleStatusName(final long orgId, final Long lifeCycleStatusId) {
        final Table lifeCycleStatusTable = getTable();
        final Row row = orgDataStore(orgId).get(lifeCycleStatusTable, lifeCycleStatusId, List.of(lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.NAME)));

        return Objects.isNull(row) ? null : row.get(OMCOLIFECYCLESTATUS.NAME);
    }
}
