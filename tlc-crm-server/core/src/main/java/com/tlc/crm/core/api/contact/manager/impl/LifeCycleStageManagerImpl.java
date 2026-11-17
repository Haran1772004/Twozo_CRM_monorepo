package com.tlc.crm.core.api.contact.manager.impl;

import com.google.common.base.Supplier;
import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.LifeCycleRuleDetails;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.internal.contact.handler.LifeCycleMigrationHandler;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.crm.core.sql.resource.OMCOLIFECYCLERULE;
import com.tlc.crm.core.sql.resource.OMCOLIFECYCLESTAGE;
import com.tlc.crm.core.sql.resource.OMCOLIFECYCLESTATUS;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
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
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
 * Implements the {@link LifeCycleStage} related DB actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class LifeCycleStageManagerImpl extends AbstractConfigManager<LifeCycleStage> implements LifeCycleStageManager {

    private final LifeCycleStatusManager lifeCycleStatusManager;

    private static class Instance {
        private static final LifeCycleStageManagerImpl INSTANCE = new LifeCycleStageManagerImpl();
    }

    private LifeCycleStageManagerImpl() {
        lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
    }

    public static LifeCycleStageManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return OMCOLifecycleStage table.
     */
    @Override
    public Table getTable() {
        return Table.get(OMCOLIFECYCLESTAGE.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the lifecycle rule.
     * </p>
     *
     * @return OMCOLifecycleRule table.
     */
    private Table getRuleTable() {
        return Table.get(OMCOLIFECYCLERULE.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param ids   Represents the stage ids.
     * @return Collection of stages.
     */
    @Override
    public Collection<LifeCycleStage> get(final Long orgId, final Collection<Long> ids) {
        final Table lifeCycleStageTable = getTable();
        final DataContainer dataContainer = get(orgId, new WhereClause(Criteria.in(lifeCycleStageTable.getPKColumn(), ids)));
        final Stream<Row> lifeCycleStageRows = dataContainer.getRows(lifeCycleStageTable);

        return lifeCycleStageRows.map(lifeCycleStageRow -> convertRowToModel(lifeCycleStageRow, dataContainer)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param lifeCycleStage An entity that holds the contact stage attributes which have to be saved into database.
     */
    @Override
    public LifeCycleStage create(final LifeCycleStage lifeCycleStage) {
        final DataContainer targetContainer = DataContainer.create();

        loadLifecycleStageIntoContainer(true, lifeCycleStage, null, targetContainer);
        orgDataStore(lifeCycleStage.orgId()).commitChanges(targetContainer);

        return lifeCycleStage;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Organization id.
     * @param stages Collection of sources which have to be updated into database.
     * @return Updated status.
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<LifeCycleStage> stages) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<Long> ids = stages.stream().map(LifeCycleStage::id).filter(Objects::nonNull).toList();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final DataContainer entityContainer = get(orgId, new WhereClause(Criteria.in(getTable().getPKColumn(), ids)));

        stages.forEach(lifecycleStage -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadLifecycleStageIntoContainer(false, lifecycleStage, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(lifecycleStage.id());
            } catch (ErrorCode errorCode) {
                LoggerUtil.logError(LOGGER, errorCode, "Life cycle stage update failed. Org ID: {}, Entity ID: {}", orgId, lifecycleStage.id());
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, lifecycleStage.id().toString()));
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
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event.
     * @param lifeCycleStage  An entity that holds the stage attributes.
     * @param sourceContainer Holds the user defined and system field values of existing stage.
     * @param targetContainer Holds the updated information of user defined and system field values.
     */
    @Override
    public void loadLifecycleStageIntoContainer(final boolean isCreateEvent, final LifeCycleStage lifeCycleStage,
                                                final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table stageTable = getTable();
        final Long id = lifeCycleStage.id();
        final Long orgId = lifeCycleStage.orgId();
        final Collection<LifeCycleStatus> statuses = lifeCycleStage.getStatuses();
        final Collection<Long> statusIds = new HashSet<>();
        final Row stageRow = isCreateEvent ? new Row(stageTable)
                : Optional.ofNullable(sourceContainer.getRow(stageTable, id)).map(Row::new).orElse(null);

        if (!isCreateEvent && Objects.isNull(stageRow)) {
            throw ErrorCode.get(ContactErrorCodes.LIFE_CYCLE_STAGE_NOT_FOUND, String.format("Org ID : %d, Entity ID : %d", orgId, id));
        }

        if (stageRow.isNewRow()) {
            final Short order = lifeCycleStage.getOrder();

            stageRow.setOrgId(orgId);
            stageRow.set(OMCOLIFECYCLESTAGE.IS_SYSTEM_STAGE, lifeCycleStage.isSystemStage());
            stageRow.set(OMCOLIFECYCLESTAGE.IS_LAST_STAGE, lifeCycleStage.isLastStage());
            stageRow.set(OMCOLIFECYCLESTAGE.ORDER, Objects.isNull(order) ? getNextStageOrder(orgId, targetContainer) : order);

            lifeCycleStage.setId(stageRow.getPKValue());
        }
        final Long stageId = lifeCycleStage.id();

        if (Objects.nonNull(statuses)) {
            for (final LifeCycleStatus status : statuses) {
                lifeCycleStatusManager.loadStatusIntoContainer(isCreateEvent, stageId, status, sourceContainer, targetContainer);
                statusIds.add(status.id());
            }
        }
        stageRow.set(OMCOLIFECYCLESTAGE.NAME, lifeCycleStage.getName());
        targetContainer.updateRow(stageRow);

        if (!isCreateEvent) {
            final Table statusTable = lifeCycleStatusManager.getTable();
            final Column statusPkColumn = statusTable.getPKColumn();
            final WhereClause whereClause = new WhereClause(Criteria.eq(statusTable.getColumn(OMCOLIFECYCLESTATUS.STAGE_ID), stageId));
            final Collection<Long> currentStatusIds = orgDataStore(orgId).get(statusTable, whereClause).getRows(statusTable).map(Row::getPKValue).collect(Collectors.toSet());
            final DataContainer dataContainer = orgDataStore(orgId).get(statusTable, statusIds.isEmpty() ? whereClause
                    : whereClause.and(Criteria.notIn(statusPkColumn, statusIds)));
            final Collection<Long> statusIdsToBeRemoved = new HashSet<>();

            dataContainer.getRows(statusTable).forEach(statusRow -> {
                statusIdsToBeRemoved.add(statusRow.getPKValue());
                targetContainer.deleteRow(statusRow);
            });

            if (statusIdsToBeRemoved.containsAll(currentStatusIds)) {
                lifeCycleStage.getStatuses().stream().findFirst().ifPresent(defaultStatus ->
                        LifeCycleMigrationHandler.getInstance().migrateContactAndRules(orgId, statusIdsToBeRemoved, stageId, defaultStatus));
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organisation id.
     * @return Count of the stages.
     */
    @Override
    public long getStagesCount(final long orgId) {
        return orgDataStore(orgId).get(CountQuery.get(getTable()));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organisation id.
     * @param criteria Represents the retrieval criteria.
     * @return Count of the stages.
     */
    @Override
    public long getStagesCount(final long orgId, final DropdownCriteria criteria) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(criteria.build(table.getColumn(OMCOLIFECYCLESTAGE.NAME)))
                .and(Criteria.eq(table.getColumn(OMCOLIFECYCLESTAGE.IS_ACTIVE), true)));
        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param row Represents the attributes of {@link LifeCycleStage}.
     * @return {@link LifeCycleStage}.
     */
    @Override
    public LifeCycleStage convertRowToModel(final Row row) {
        final LifeCycleStage lifeCycleStage = new LifeCycleStage();
        final Short order = row.get(OMCOLIFECYCLESTAGE.ORDER);
        final Boolean isActive = row.get(OMCOLIFECYCLESTAGE.IS_ACTIVE);
        final Boolean isLastStage = row.get(OMCOLIFECYCLESTAGE.IS_LAST_STAGE);
        final Boolean isSystemStage = row.get(OMCOLIFECYCLESTAGE.IS_SYSTEM_STAGE);

        lifeCycleStage.setId(row.getPKValue());
        lifeCycleStage.setOrgId(row.getOrgId());
        lifeCycleStage.setName(row.get(OMCOLIFECYCLESTAGE.NAME));

        if (Objects.nonNull(order)) {
            lifeCycleStage.setOrder(order);
        }

        if (Objects.nonNull(isActive)) {
            lifeCycleStage.setActive(isActive);
        }

        if (Objects.nonNull(isLastStage)) {
            lifeCycleStage.setLastStage(isLastStage);
        }

        if (Objects.nonNull(isSystemStage)) {
            lifeCycleStage.setSystemStage(isSystemStage);
        }
        return lifeCycleStage;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents the organisation id.
     * @param stageOrders Represents the orders of the lifecycle stages.
     */
    @Override
    public void updateOrder(final long orgId, final Map<Short, Long> stageOrders) {
        final Table lifecycleStageTable = getTable();
        final List<UpdateQuery> updateQueries = new ArrayList<>();

        for (final Map.Entry<Short, Long> stageOrder : stageOrders.entrySet()) {
            final UpdateQuery updateQuery = UpdateQuery.get(lifecycleStageTable);

            updateQuery.addUpdateColumn(lifecycleStageTable.getColumn(OMCOLIFECYCLESTAGE.ORDER), stageOrder.getKey());
            updateQuery.setWhereClause(new WhereClause(Criteria.eq(lifecycleStageTable.getPKColumn(), stageOrder.getValue())));
            updateQueries.add(updateQuery);
        }
        orgDataStore(orgId).update(updateQueries);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organisation id.
     * @param id       Represents the lifecycle status id.
     * @param isActive A flag that indicates whether the lifecycle stage is active or not.
     */
    @Override
    public void updateStatus(final long orgId, final long id, final boolean isActive) {
        final Table lifecycleStageTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(lifecycleStageTable);

        updateQuery.addUpdateColumn(lifecycleStageTable.getColumn(OMCOLIFECYCLESTAGE.IS_ACTIVE), isActive);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(lifecycleStageTable.getPKColumn(), id)));
        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                           Organization id.
     * @param applyCriteriaForLifeCycleStatus Flag to identify whether to apply the criteria for lifecycle stage or lifecycle status
     * @param limit                           Which holds the limit configuration.
     * @param criteria                        Which configure the criteria.
     * @return Collection of lifeCycleStages.
     */
    @Override
    public Collection<LifeCycleStage> getLifeCycleStageDropdownList(final Long orgId, final boolean applyCriteriaForLifeCycleStatus,
                                                                    final DropdownLimit limit, final DropdownCriteria criteria) {
        final Table lifeCycleStageTable = getTable();
        final Column lifeCycleStageNameColumn = lifeCycleStageTable.getColumn(OMCOLIFECYCLESTAGE.NAME);
        final Column lifeCycleStageOrderColumn = lifeCycleStageTable.getColumn(OMCOLIFECYCLESTAGE.ORDER);
        final WhereClause whereClause = new WhereClause(Criteria.eq(lifeCycleStageTable.getColumn(OMCOLIFECYCLESTAGE.IS_ACTIVE), true));
        final SelectQuery selectQuery;

        if (applyCriteriaForLifeCycleStatus) {
            final Table lifeCycleStatusTable = lifeCycleStatusManager.getTable();
            final Column lifeCycleStageIdColumn = lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.STAGE_ID);
            final Column lifeCycleStatusNameColumn = lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.NAME);
            selectQuery = SelectQuery.get(lifeCycleStatusTable);

            selectQuery.addSelectClause(List.of(lifeCycleStatusNameColumn, lifeCycleStageIdColumn));
            selectQuery.addJoinClause(new JoinClause(lifeCycleStageIdColumn, lifeCycleStageTable.getPKColumn()));
            selectQuery.addOrderByClause(new OrderByClause(lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.ORDER)));
            selectQuery.setWhereClause(whereClause.and(criteria.build(lifeCycleStatusNameColumn)));
        } else {
            selectQuery = SelectQuery.get(lifeCycleStageTable);

            selectQuery.setWhereClause(whereClause.and(criteria.build(lifeCycleStageNameColumn)));
        }
        selectQuery.addSelectClause(List.of(lifeCycleStageNameColumn, lifeCycleStageOrderColumn));
        selectQuery.addOrderByClause(new OrderByClause(lifeCycleStageOrderColumn));
        selectQuery.setLimitClause(limit.getLimitClause());

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> rows = dataContainer.getRows(lifeCycleStageTable);

        return rows.map(row -> convertRowToModel(row, dataContainer)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                Represents the organization ID.
     * @param lifeCycleStageValues Which contains the collection of lifecycle stage names.
     * @return {@link Collection} of lifecycle stages.
     */
    @Override
    public Collection<LifeCycleStage> getLifeCycleStages(final Long orgId, final Collection<String> lifeCycleStageValues) {
        final Table lifeCycleStageTable = getTable();
        final Column lifeCycleStageNameColumn = lifeCycleStageTable.getColumn(OMCOLIFECYCLESTAGE.NAME);
        final Column lifeCycleStagePkColumn = lifeCycleStageTable.getPKColumn();
        final Column orgIdColumn = lifeCycleStageTable.getColumn(OMCOLIFECYCLESTAGE.ORG_ID);
        final SelectQuery selectQuery = SelectQuery.get(lifeCycleStageTable);
        final SelectQuery firstLifeCycleStageSelectQuery = SelectQuery.get(lifeCycleStageTable);

        firstLifeCycleStageSelectQuery.addSelectClause(lifeCycleStagePkColumn);
        firstLifeCycleStageSelectQuery.setWhereClause(new WhereClause(Criteria.eq(orgIdColumn, orgId)));
        firstLifeCycleStageSelectQuery.addOrderByClause(new OrderByClause(lifeCycleStagePkColumn));
        firstLifeCycleStageSelectQuery.setLimitClause(new LimitClause(1));

        selectQuery.addSelectClause(List.of(lifeCycleStageNameColumn, lifeCycleStagePkColumn, orgIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(lifeCycleStageNameColumn, lifeCycleStageValues))
                .or(Criteria.eq(lifeCycleStagePkColumn, firstLifeCycleStageSelectQuery)));

        final Stream<Row> lifeCycleStageRows = orgDataStore(orgId).get(selectQuery).getRows(lifeCycleStageTable);

        return lifeCycleStageRows.map(this::convertRowToModel).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId             Represents the organisation id.
     * @param lifeCycleStageIds Contains the collection of life cycle stage ids.
     * @return true if all the life cycle stages are exists.
     */
    @Override
    public boolean exists(final Long orgId, final Set<Long> lifeCycleStageIds) {
        final Table lifeCycleStageTable = getTable();
        final CountQuery countQuery = CountQuery.get(lifeCycleStageTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(lifeCycleStageTable.getPKColumn(), lifeCycleStageIds)));

        return lifeCycleStageIds.size() == orgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organisation id.
     * @param stages Contains the collection of life cycle stage names.
     * @return true if all the life cycle stages are exists.
     */
    @Override
    public boolean exists(final Long orgId, final Collection<LifeCycleStage> stages) {
        final Table lifeCycleStageTable = getTable();
        final Collection<Long> stageIds = new HashSet<>();
        final Collection<String> stageNames = new HashSet<>();
        final CountQuery countQuery = CountQuery.get(lifeCycleStageTable);
        WhereClause whereClause = null;

        for (final LifeCycleStage stage : stages) {
            final Long stageId = stage.id();
            final String stageName = stage.getName();

            if (Objects.nonNull(stageId)) {
                stageIds.add(stageId);
            }

            if (Objects.nonNull(stageName)) {
                stageNames.add(stageName);
            }
        }

        if (!stageIds.isEmpty()) {
            whereClause = new WhereClause(Criteria.notIn(lifeCycleStageTable.getPKColumn(), stageIds));
        }

        if (!stageNames.isEmpty()) {
            final Criteria criteria = Criteria.in(lifeCycleStageTable.getColumn(OMCOLIFECYCLESTAGE.NAME), stageNames);

            whereClause = Objects.isNull(whereClause) ? new WhereClause(criteria) : whereClause.and(criteria);
        }
        countQuery.setWhereClause(whereClause);

        return 0 < orgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represent the organization id.
     * @return {@link Long} of lifecycle stage id.
     */
    @Override
    public Long getDefaultStageId(final Long orgId) {
        return getDefaultStageRow(orgId, 1, List.of(getTable().getPKColumn())).getPKValue();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId               Represent the organization id.
     * @param levelOfDefaultStage Represent the level of the default stage.
     *                            NOTE: The first stage is the default. If the first stage has to be deactivated,
     *                            the next stage becomes the default, and this pattern continues through to the last stage.
     * @return {@link Long} of lifecycle stage id.
     */
    @Override
    public Long getDefaultStageId(final Long orgId, final int levelOfDefaultStage) {
        return getDefaultStageRow(orgId, levelOfDefaultStage, List.of(getTable().getPKColumn())).getPKValue();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represent the organization id.
     * @return the default {@link LifeCycleStage}.
     */
    @Override
    public LifeCycleStage getDefaultStage(final long orgId) {
        return convertRowToModel(getDefaultStageRow(orgId, 1, Collections.emptyList()));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organisation id.
     * @param labels Represents the labels of the lifecycle status.
     * @return Map of stage ids and their statuses.
     */
    @Override
    public Map<Long, Collection<Long>> getStagesByLabels(final long orgId, final Collection<LifeCycleStatus.Label> labels) {
        final Table lifecycleStageTable = getTable();
        final Table lifeCycleStatusTable = lifeCycleStatusManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(lifecycleStageTable);
        final Collection<Short> labelIds = labels.stream().map(LifeCycleStatus.Label::getId).collect(Collectors.toSet());
        final Column stagePkColumn = lifecycleStageTable.getPKColumn();
        final Column statusIdColumn = lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.STAGE_ID);

        selectQuery.addSelectClause(stagePkColumn);
        selectQuery.addSelectClause(lifeCycleStatusTable.getPKColumn());
        selectQuery.addSelectClause(statusIdColumn);
        selectQuery.addJoinClause(new JoinClause(stagePkColumn, statusIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.LABEL), labelIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(lifecycleStageTable).collect(Collectors.toMap(Row::getPKValue, lifecycleStageRow -> {
            final Long stageId = lifecycleStageRow.getPKValue();
            final Stream<Row> lifecycleStatusRows = dataContainer.getRows(lifeCycleStatusTable, new WhereClause(Criteria.eq(statusIdColumn, stageId)));

            return lifecycleStatusRows.map(Row::getPKValue).collect(Collectors.toSet());
        }));
    }

    /**
     * {@inheritDoc}
     *
     * @param lifeCycleRuleDetails Holds the details of the lifecycle stage rules.
     */
    @Override
    public void updateRules(final long orgId, final Collection<LifeCycleRuleDetails> lifeCycleRuleDetails) {
        final Table ruleTable = getRuleTable();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final DataContainer targetContainer = DataContainer.create();
        final DataContainer dataContainer = orgDataStore.get(ruleTable, (WhereClause) null);
        final Column ruleColumn = ruleTable.getColumn(OMCOLIFECYCLERULE.RULE);
        final Collection<Short> ruleIds = new HashSet<>();
        final boolean isRuleNotExists = 0 == dataContainer.size(ruleTable);
        final Supplier<Row> rowCreator = () -> new Row(ruleTable);

        for (final LifeCycleRuleDetails lifeCycleRuleDetail : lifeCycleRuleDetails) {
            final short ruleId = lifeCycleRuleDetail.getType().getId();
            final Row ruleRow = isRuleNotExists ? rowCreator.get() : Optional.ofNullable(dataContainer.getRow(ruleTable,
                    new WhereClause(Criteria.eq(ruleColumn, ruleId)))).orElseGet(rowCreator);

            if (ruleRow.isNewRow()) {
                ruleRow.setOrgId(orgId);
                ruleRow.set(OMCOLIFECYCLERULE.RULE, ruleId);
            }
            ruleRow.set(OMCOLIFECYCLERULE.STAGE_ID, lifeCycleRuleDetail.getStage().id());
            ruleRow.set(OMCOLIFECYCLERULE.STATUS_ID, lifeCycleRuleDetail.getStatus().id());
            ruleIds.add(ruleId);

            targetContainer.updateRow(ruleRow);
        }
        dataContainer.getRows(ruleTable, new WhereClause(Criteria.notIn(ruleColumn, ruleIds))).forEach(targetContainer::deleteRow);
        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organisation id.
     * @return Collection of {@link LifeCycleRuleDetails}.
     */
    @Override
    public Collection<LifeCycleRuleDetails> getRules(final long orgId) {
        return getRules(orgId, null);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organisation id.
     * @param ruleType Represents the type of the lifecycle stage rule.
     * @return {@link LifeCycleRuleDetails}.
     */
    @Override
    public Optional<LifeCycleRuleDetails> getRule(final long orgId, final LifeCycleRuleDetails.LifeCycleRuleType ruleType) {
        final Column ruleColumn = getRuleTable().getColumn(OMCOLIFECYCLERULE.RULE);

        return getRules(orgId, new WhereClause(Criteria.eq(ruleColumn, ruleType.getId()))).stream().findFirst();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<LifeCycleStage> partialGet(final Long orgId, final Collection<Long> ids) {
        final Table lifeCycleStageTable = getTable();
        final WhereClause whereClause = new WhereClause(Criteria.in(lifeCycleStageTable.getPKColumn(), ids));
        final Column nameColumn = lifeCycleStageTable.getColumn(OMCOLIFECYCLESTAGE.NAME);
        final DataContainer dataContainer = orgDataStore(orgId).get(lifeCycleStageTable, whereClause, List.of(nameColumn));
        final Stream<Row> lifeCycleStageRows = dataContainer.getRows(lifeCycleStageTable);

        return lifeCycleStageRows.map(this::convertRowToModel).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organisation id.
     * @return Collection of {@link LifeCycleStage}.
     */
    @Override
    public Collection<LifeCycleStage> getAll(final Long orgId) {
        final DataContainer dataContainer = get(orgId, (WhereClause) null);
        final Stream<Row> lifeCycleStageRows = dataContainer.getRows(getTable());

        return lifeCycleStageRows.map(lifeCycleStageRow -> convertRowToModel(lifeCycleStageRow, dataContainer)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organisation id.
     * @param stageId Represents the stage id.
     * @return A flag that indicates whether the given stage is last stage or not.
     */
    @Override
    public boolean isLastStage(final Long orgId, final Long stageId) {
        final Table stageTable = getTable();
        final Column lastStageColumn = stageTable.getColumn(OMCOLIFECYCLESTAGE.IS_LAST_STAGE);

        return orgDataStore(orgId).get(stageTable, stageId, List.of(lastStageColumn)).get(lastStageColumn);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organisation id.
     * @param stageId Represents the stage id.
     * @return A flag that indicates whether the given stage is de-activatable or not.
     */
    @Override
    public boolean isDeactivatableStage(final Long orgId, final Long stageId) {
        final Table lifecycleStageTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(lifecycleStageTable);

        selectQuery.addSelectClause(lifecycleStageTable.getPKColumn());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(lifecycleStageTable.getColumn(OMCOLIFECYCLESTAGE.IS_SYSTEM_STAGE), true))
                .and(Criteria.eq(lifecycleStageTable.getColumn(OMCOLIFECYCLESTAGE.IS_LAST_STAGE), false)));
        final Set<Long> stageIds = orgDataStore(orgId).get(selectQuery).getRows(lifecycleStageTable).map(Row::getPKValue).collect(Collectors.toSet());

        return stageIds.contains(stageId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organisation id.
     * @param stageIds Represents the stage ids.
     * @return True, if the given stages are system default stages.
     */
    @Override
    public boolean isSystemStages(final Long orgId, final Set<Long> stageIds) {
        final Table lifecycleStageTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(lifecycleStageTable);

        selectQuery.addSelectClause(lifecycleStageTable.getPKColumn());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(lifecycleStageTable.getColumn(OMCOLIFECYCLESTAGE.IS_SYSTEM_STAGE), true))
                .and(Criteria.in(lifecycleStageTable.getPKColumn(), stageIds)));
        final Set<Long> lifecycleStageIds = orgDataStore(orgId).get(selectQuery).getRows(lifecycleStageTable).map(Row::getPKValue).collect(Collectors.toSet());

        return lifecycleStageIds.size() == stageIds.size();
    }

    /**
     * <p>
     * Updates the order of the lifecycle stage that follows the removed stage.
     * </p>
     *
     * @param orgId             Represents the organisation id.
     * @param removedStageOrder Represents the removed stage order.
     */
    @Override
    public void reArrangeStageOrders(final long orgId, final short removedStageOrder) {
        final Table stageTable = getTable();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final SelectQuery selectQuery = SelectQuery.get(stageTable);
        final Column pkColumn = stageTable.getPKColumn();
        final Column orderColumn = stageTable.getColumn(OMCOLIFECYCLESTAGE.ORDER);

        selectQuery.addSelectClause(pkColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.gt(orderColumn, removedStageOrder))
                .and(Criteria.eq(stageTable.getColumn(OMCOLIFECYCLESTAGE.IS_LAST_STAGE), false)));
        selectQuery.addOrderByClause(new OrderByClause(orderColumn));
        final Stream<Row> stageRows = orgDataStore.get(selectQuery).getRows(stageTable);
        final List<UpdateQuery> updateQueries = new ArrayList<>();
        final Counter orderCounter = new Counter(removedStageOrder);

        stageRows.forEach(stageRow -> {
            final UpdateQuery updateQuery = UpdateQuery.get(stageTable);

            updateQuery.addUpdateColumn(orderColumn, orderCounter.getAndIncrement());
            updateQuery.setWhereClause(new WhereClause(Criteria.eq(pkColumn, stageRow.getPKValue())));
            updateQueries.add(updateQuery);
        });
        orgDataStore.update(updateQueries);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organisation id.
     * @param stageId Represents the stage id.
     * @param label   Represents the label of the status.
     * @return True, if the given stage has labeled statuses.
     */
    @Override
    public boolean isLabeledStatusExists(final Long orgId, final Long stageId, final LifeCycleStatus.Label label) {
        final Table statusTable = lifeCycleStatusManager.getTable();
        final CountQuery countQuery = CountQuery.get(statusTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(statusTable.getColumn(OMCOLIFECYCLESTATUS.STAGE_ID), stageId))
                .and(new WhereClause(Criteria.eq(statusTable.getColumn(OMCOLIFECYCLESTATUS.LABEL), label.getId()))));

        return 0 != orgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organisation.
     * @param criteria Holds the data retrieval criteria.
     * @return Total status count.
     */
    @Override
    public long getTotalRecordsCount(final Long orgId, final DropdownCriteria criteria) {
        final Table lifeCycleStageTable = getTable();
        final CountQuery countQuery = CountQuery.get(lifeCycleStageTable);

        countQuery.setWhereClause(new WhereClause(criteria.build(lifeCycleStageTable.getColumn(OMCOLIFECYCLESTAGE.NAME))));
        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches the order of the next lifecycle stage in the given stage.
     * </p>
     *
     * @param orgId           Represents the organisation id.
     * @param targetContainer Holds the updated information of user defined and system field values.
     * @return Next stage order.
     */
    private short getNextStageOrder(final long orgId, final DataContainer targetContainer) {
        final Table lifecycleStageTable = getTable();
        final Counter counter = new Counter();

        if (0 < targetContainer.size(lifecycleStageTable)) {
            final Stream<Row> lifecycleStageRows = targetContainer.getRows(lifecycleStageTable);

            lifecycleStageRows.map(row -> (short) row.get(OMCOLIFECYCLESTAGE.ORDER)).max(Short::compareTo)
                    .ifPresent(maxOrder -> counter.setCount(Math.max(maxOrder, counter.getCount())));
        } else {
            final Column orderColumn = lifecycleStageTable.getColumn(OMCOLIFECYCLESTAGE.ORDER);
            final WhereClause whereClause = new WhereClause(Criteria.eq(lifecycleStageTable.getColumn(OMCOLIFECYCLESTAGE.IS_LAST_STAGE), false));
            final DataContainer dataContainer = orgDataStore(orgId).get(lifecycleStageTable, whereClause, List.of(orderColumn));
            final short maxOrder = dataContainer.getRows(lifecycleStageTable).max(Comparator.comparing(stageRow ->
                    stageRow.get(orderColumn))).map(statusRow -> (short) statusRow.get(orderColumn)).orElseGet(() -> (short) 0);

            counter.setCount(maxOrder);
        }
        return (short) counter.incrementAndGet();
    }

    /**
     * <p>
     * Fetches the {@link DataContainer} based on given criteria.
     * </p>
     *
     * @param orgId       Represents the organisation id.
     * @param whereClause Represents the retrieval criteria.
     * @return {@link DataContainer}.
     */
    private DataContainer get(final Long orgId, final WhereClause whereClause) {
        final Table lifeCycleStageTable = getTable();
        final Table statusTable = lifeCycleStatusManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(lifeCycleStageTable);

        selectQuery.addJoinClause(new JoinClause(lifeCycleStageTable.getPKColumn(), statusTable.getColumn(OMCOLIFECYCLESTATUS.STAGE_ID)));
        selectQuery.setWhereClause(whereClause);
        selectQuery.addOrderByClause(new OrderByClause(lifeCycleStageTable.getColumn(OMCOLIFECYCLESTAGE.ORDER)));
        selectQuery.addOrderByClause(new OrderByClause(statusTable.getColumn(OMCOLIFECYCLESTATUS.ORDER)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Fetches the collection of {@link LifeCycleRuleDetails} of the organisation.
     * </p>
     *
     * @param orgId       Represents the organisation id.
     * @param whereClause Holds the retrieval criteria.
     * @return Collection of {@link LifeCycleRuleDetails}.
     */
    private Collection<LifeCycleRuleDetails> getRules(final long orgId, final WhereClause whereClause) {
        final Table lifecycleStageTable = getTable();
        final Table lifeCycleStatusTable = lifeCycleStatusManager.getTable();
        final Table ruleTable = getRuleTable();
        final SelectQuery selectQuery = SelectQuery.get(ruleTable);

        selectQuery.addJoinClause(new JoinClause(ruleTable.getColumn(OMCOLIFECYCLERULE.STAGE_ID), lifecycleStageTable.getPKColumn()));
        selectQuery.addJoinClause(new JoinClause(ruleTable.getColumn(OMCOLIFECYCLERULE.STATUS_ID), lifeCycleStatusTable.getPKColumn()));
        selectQuery.setWhereClause(whereClause);
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return 0 == dataContainer.size(ruleTable) ? Collections.emptyList() : dataContainer.getRows(ruleTable)
                .map(ruleRow -> convertRowToRule(ruleRow, dataContainer)).sorted(Comparator.comparing(LifeCycleRuleDetails::getType)).toList();
    }

    /**
     * <p>
     * Converts the {@link Row} into {@link LifeCycleRuleDetails}.
     * </p>
     *
     * @param ruleRow       Represents the lifecycle stage rule.
     * @param dataContainer Holds the retrieved data.
     * @return {@link LifeCycleRuleDetails}.
     */
    private LifeCycleRuleDetails convertRowToRule(final Row ruleRow, final DataContainer dataContainer) {
        final LifeCycleRuleDetails lifeCycleRuleDetails = new LifeCycleRuleDetails(LifeCycleRuleDetails.LifeCycleRuleType.get(ruleRow.get(OMCOLIFECYCLERULE.RULE)));
        final Row lifeCycleStageRow = dataContainer.getRow(getTable(), (Long) ruleRow.get(OMCOLIFECYCLERULE.STAGE_ID));
        final Row lifeCycleStatusRow = dataContainer.getRow(lifeCycleStatusManager.getTable(), (Long) ruleRow.get(OMCOLIFECYCLERULE.STATUS_ID));

        lifeCycleRuleDetails.setOrgId(ruleRow.getOrgId());
        lifeCycleRuleDetails.setStage(convertRowToModel(lifeCycleStageRow));
        lifeCycleRuleDetails.setStatus(lifeCycleStatusManager.convertRowToModel(lifeCycleStatusRow));

        return lifeCycleRuleDetails;
    }

    /**
     * <p>
     * Converts the row into {@link LifeCycleStage}.
     * </p>
     *
     * @param lifeCycleStageRow Represents the attributes of {@link LifeCycleStage}.
     * @param dataContainer     Holds the user defined and system field values of existing stage.
     * @return {@link LifeCycleStage}.
     */
    private LifeCycleStage convertRowToModel(final Row lifeCycleStageRow, final DataContainer dataContainer) {
        final Table lifeCycleStatusTable = lifeCycleStatusManager.getTable();
        final LifeCycleStage lifeCycleStage = convertRowToModel(lifeCycleStageRow);
        final Stream<Row> lifeCycleStatusRows = dataContainer.getRows(lifeCycleStatusTable,
                new WhereClause(Criteria.eq(lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.STAGE_ID), lifeCycleStageRow.getPKValue())));

        lifeCycleStatusRows.forEach(statusRow -> lifeCycleStage.addStatus(lifeCycleStatusManager.convertRowToModel(statusRow)));
        return lifeCycleStage;
    }

    /**
     * <p>
     * Gets the default lifecycle stage row for the given organization.
     * </p>
     *
     * @param orgId               Represent the organization id.
     * @param levelOfDefaultStage Represent the level of the default stage.
     *                            NOTE: The first stage is the default. If the first stage has to be deactivated,
     *                            the next stage becomes the default, and this pattern continues through to the last stage.
     * @return the default life cycle stage {@link Row}.
     */
    private Row getDefaultStageRow(final long orgId, final int levelOfDefaultStage, final Collection<Column> selectedColumns) {
        final Table lifeCycleStageTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(lifeCycleStageTable);

        selectQuery.addSelectClause(selectedColumns);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(lifeCycleStageTable.getColumn(OMCOLIFECYCLESTAGE.IS_ACTIVE), true)));
        selectQuery.addOrderByClause(new OrderByClause(lifeCycleStageTable.getColumn(OMCOLIFECYCLESTAGE.ORDER)));
        selectQuery.setLimitClause(new LimitClause(levelOfDefaultStage, 1));

        return orgDataStore(orgId).get(selectQuery).getRow(getTable());
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId            The ID of the organization to which the life cycle status belongs.
     * @param lifeCycleStageId The ID of the life cycle status to retrieve.
     * @return The name of the lifecycle stage.
     */
    @Override
    public String getLifeCycleStageName(final long orgId, final Long lifeCycleStageId) {
        final Table lifeCycleStageTable = getTable();
        final Row row = orgDataStore(orgId).get(lifeCycleStageTable, lifeCycleStageId,
                List.of(lifeCycleStageTable.getColumn(OMCOLIFECYCLESTAGE.NAME)));

        return Objects.isNull(row) ? null : row.get(OMCOLIFECYCLESTAGE.NAME);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param start Represents the starting index
     * @param limit Represents the maximum number of records to fetch
     * @return The collection of {@link LifeCycleStage}
     */
    @Override
    public Collection<LifeCycleStage> getLifeCycleStages(final long orgId, final int start, final int limit) {
        final Table lifeCycleStageTable = getTable();
        final Table lifeCycleStatusTable = LifeCycleStatusManager.getInstance().getTable();
        final SelectQuery selectQuery = SelectQuery.get(lifeCycleStatusTable);

        selectQuery.addSelectClause(lifeCycleStageTable.getColumn(OMCOLIFECYCLESTAGE.NAME));
        selectQuery.addSelectClause(lifeCycleStatusTable);
        selectQuery.addJoinClause(new JoinClause(lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.STAGE_ID), lifeCycleStageTable.getPKColumn()));
        selectQuery.addOrderByClause(new OrderByClause(lifeCycleStageTable.getColumn(OMCOLIFECYCLESTAGE.ORDER)));
        selectQuery.addOrderByClause(new OrderByClause(lifeCycleStatusTable.getColumn(OMCOLIFECYCLESTATUS.ORDER)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(lifeCycleStageTable.getColumn(OMCOLIFECYCLESTAGE.IS_ACTIVE), true)));
        selectQuery.setLimitClause(new LimitClause(start, limit));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> lifeCycleStageRows = dataContainer.getRows(lifeCycleStageTable);

        return lifeCycleStageRows.map(row -> convertRowToModel(row, dataContainer)).toList();
    }
}
