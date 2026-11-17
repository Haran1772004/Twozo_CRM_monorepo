package com.tlc.crm.core.api.deal.manager;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Deal.State;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;
import com.tlc.crm.core.internal.event.index.publisher.IndexDataPublisher;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MDOPIPELINE;
import com.tlc.crm.core.sql.resource.MDOPIPELINESTAGE;
import com.tlc.crm.core.sql.resource.MDOSTAGEPROGRESS;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.handler.OrderManager;
import com.tlc.crm.common.list.sort.api.manager.SortManagerImpl;
import com.tlc.crm.common.list.sort.models.SortCriteria;
import com.tlc.crm.common.list.sort.models.SortViewType;
import com.tlc.crm.entity.resolver.OrderResolver;
import com.tlc.crm.common.sql.resource.OMKANBANSORTCONFIG;
import com.tlc.crm.common.sql.resource.OMSORTCONFIG;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DmlUtil;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the {@link PipelineStage} related actions
 *
 * @author Sundar
 * @version 1.0
 */
public class PipelineStageManager extends AbstractConfigManager<PipelineStage> {

    private final OrderResolver orderResolver;
    private final OrderManager orderManager;
    private final IndexDataPublisher indexDataPublisher;

    private PipelineStageManager() {
        orderResolver = OrderResolver.getInstance();
        orderManager = OrderManager.getInstance();
        indexDataPublisher = IndexDataPublisher.getInstance();
    }

    private static class Instance {
        private static final PipelineStageManager INSTANCE = new PipelineStageManager();
    }

    /**
     * Gets the {@link PipelineStageManager} instance
     *
     * @return the {@link PipelineStageManager} instance
     */
    public static PipelineStageManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Table} of pipeline stage
     */
    @Override
    public Table getTable() {
        return Table.get(MDOPIPELINESTAGE.TABLE);
    }

    /**
     * <p>
     * Fetches the {@link PipelineStage} that has one of the id value mentioned in the parameter
     * </p>
     *
     * @param orgId Organization id
     * @param ids   Pipeline stages id
     * @return Collection of pipeline stages
     */
    @Override
    public Collection<PipelineStage> get(Long orgId, Collection<Long> ids) {
        final Table table = getTable();
        final Stream<Row> rows = orgDataStore(orgId).get(table, ids).getRows(table);

        if (Objects.isNull(rows)) {
            throw ErrorCode.get(DealErrorCodes.PIPELINE_STAGE_NOT_FOUND, "i18n.pipelineStage.not.found");
        } else {
            return rows.map(this::convertRowToModel).toList();
        }
    }

    /**
     * <p>
     * Saves the attributes of {@link PipelineStage} into the database
     * </p>
     *
     * @param pipelineStage An entity that holds the pipeline stage attributes which have to be saved into database
     */
    @Override
    public PipelineStage create(PipelineStage pipelineStage) {
        final DataContainer targetContainer = DataContainer.create();
        final Long orgId = pipelineStage.orgId();

        loadPipelineStageIntoContainer(orgId, List.of(pipelineStage), targetContainer);
        orgDataStore(orgId).commitChanges(targetContainer);

        return pipelineStage;
    }

    /**
     * <p>
     * Updates the given pipeline stages to database
     * </p>
     *
     * @param orgId          Organization id
     * @param pipelineStages Collection of pipeline stages that have to be updated
     * @return Updated status
     */
    @Override
    public ProcessOutcome update(Long orgId, Collection<PipelineStage> pipelineStages) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<Long> ids = pipelineStages.stream().map(PipelineStage::id).filter(Objects::nonNull).toList();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final DataContainer entityContainer = orgDataStore.get(getTable(), ids);

        pipelineStages.forEach(pipelineStage -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadPipelineStageIntoContainer(false, pipelineStage, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(pipelineStage.id());
                publishPipelineStageToIndex(orgId, pipelineStage);
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Pipeline stage update failed Org ID : {} ID : {} reason : {}", orgId, pipelineStage.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(DealErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, pipelineStage.id().toString()));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(DealErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }
        return processOutcome;
    }

    /**
     * <p>
     * Loads the pipeline stages into the target container
     * </p>
     *
     * @param orgId           Organization id
     * @param pipelineStages  An entity that holds the pipeline stages attributes
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadPipelineStageIntoContainer(final Long orgId, final Collection<PipelineStage> pipelineStages,
                                               final DataContainer targetContainer) {
        final Table table = getTable();
        final List<Long> ids = pipelineStages.stream().map(PipelineStage::id).filter(Objects::nonNull).toList();
        final Long pipelineId = pipelineStages.stream().findFirst().get().getPipelineId();
        final boolean isCreateEvent = ids.isEmpty();
        final DataContainer entityContainer = isCreateEvent ? null : orgDataStore(orgId).get(table, new WhereClause(Criteria.eq(table.getColumn(MDOPIPELINESTAGE.PIPELINE_ID), pipelineId)));

        pipelineStages.forEach(pipelineStage -> loadPipelineStageIntoContainer(isCreateEvent, pipelineStage, entityContainer, targetContainer));

        if (!isCreateEvent) {
            final Stream<Row> unMappedPipelineStageRows = entityContainer.getRows(table, new WhereClause(Criteria.notIn(table.getPKColumn(), ids)));

            unMappedPipelineStageRows.forEach(row -> migratePipelineStageDeals(orgId, row.getPKValue(), pipelineId, targetContainer));
        }
    }

    /**
     * <p>
     * Loads the pipeline stages into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param pipelineStage   An entity that holds the pipeline stage attributes
     * @param sourceContainer Holds the user defined and system field values of existing pipeline stage
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadPipelineStageIntoContainer(final boolean isCreateEvent, final PipelineStage pipelineStage,
                                               final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table pipelineStageTable = getTable();
        final Long id = pipelineStage.id();
        final Long pipelineId = pipelineStage.getPipelineId();
        final Long orgId = pipelineStage.orgId();
        final Short pipelineStageOrder = pipelineStage.getOrder();
        final Row pipelineStageRow = Objects.isNull(id) ? new Row(pipelineStageTable)
                : Optional.ofNullable(sourceContainer.getRow(pipelineStageTable, id)).map(Row::new).orElse(null);

        if (!isCreateEvent && Objects.isNull(pipelineStageRow)) {
            throw ErrorCode.get(DealErrorCodes.PIPELINE_STAGE_NOT_FOUND, "i18n.pipelineStage.not.found");
        }

        if (pipelineStageRow.isNewRow()) {
            pipelineStageRow.setOrgId(orgId);
            pipelineStage.setId(pipelineStageRow.getPKValue());
        }

        pipelineStageRow.set(MDOPIPELINESTAGE.PROBABILITY, pipelineStage.getProbability());
        pipelineStageRow.set(MDOPIPELINESTAGE.PIPELINE_ID, pipelineId);
        pipelineStageRow.set(MDOPIPELINESTAGE.NAME, pipelineStage.getName());
        pipelineStageRow.set(MDOPIPELINESTAGE.IS_DEFAULT, pipelineStage.isDefault());

        if (isCreateEvent && PipelineManager.getInstance().exists(orgId, pipelineId)) {
            final Stream<Row> pipelineStageRows = getWonAndLostStages(orgId, pipelineId);
            final Counter counter = new Counter(orderResolver.getNextOrder(orgId, new WhereClause(Criteria.eq(pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID),
                    pipelineId)), pipelineStageTable) - 2);

            pipelineStageRow.set(MDOPIPELINESTAGE.ORDER, counter.getAndIncrement());

            pipelineStageRows.forEach(stageRow -> {
                stageRow.set(MDOPIPELINESTAGE.ORDER, counter.getAndIncrement());
                targetContainer.updateRow(stageRow);
            });
        }

        if (Objects.nonNull(pipelineStageOrder)) {
            pipelineStageRow.set(MDOPIPELINESTAGE.ORDER, pipelineStageOrder);
        }
        targetContainer.updateRow(pipelineStageRow);
    }

    /**
     * <p>
     * Gets the pipeline stages rows by pipeline id
     * </p>
     *
     * @param orgId      Represents the organization id
     * @param pipelineId Represents the pipeline id
     * @return stream of pipeline stage rows.
     */
    private Stream<Row> getWonAndLostStages(final Long orgId, final Long pipelineId) {
        final Table pipelineStageTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(pipelineStageTable);

        selectQuery.addSelectClause(pipelineStageTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID), pipelineId))
                .and(Criteria.in(pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME), List.of(State.WON, State.LOST))));
        selectQuery.addOrderByClause(new OrderByClause(pipelineStageTable.getColumn(MDOPIPELINESTAGE.ORDER)));

        return orgDataStore(orgId).get(selectQuery).getRows(pipelineStageTable);
    }

    /**
     * Checks whether the pipeline stage is already exists or not by given name and id
     *
     * @param orgId      organisation ID
     * @param pipelineId pipeline ID
     * @param stageId    pipeline stage ID
     * @param name       pipeline name
     * @return true if the given named pipeline stage is already exists
     */
    public boolean isStageAlreadyExists(final Long orgId, final Long pipelineId, final Long stageId, final String name) {
        final Table stageTable = getTable();
        final CountQuery countQuery = CountQuery.get(stageTable);

        countQuery.setWhereClause(getStageFilter(pipelineId, stageId, name, stageTable));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * Constructs the where clause which can be used to filter the pipeline stage by its name, pipeline and ID
     *
     * @param pipelineId pipeline ID
     * @param stageId    pipeline stage ID
     * @param name       pipeline stage name
     * @param table      represents the pipeline stage table
     * @return {@link WhereClause} which contains the criteria to filter pipeline stage
     */
    private WhereClause getStageFilter(final Long pipelineId, final Long stageId, final String name, final Table table) {
        final WhereClause condition = new WhereClause(Criteria.eq(table
                .getColumn(MDOPIPELINESTAGE.PIPELINE_ID), pipelineId)).and(Criteria.eq(table
                .getColumn(MDOPIPELINESTAGE.NAME), name));

        return Objects.nonNull(stageId) ? condition.and(Criteria.notEq(table.getPKColumn(), stageId)) : condition;
    }

    /**
     * Gets the pipeline stages by pipeline
     *
     * @param orgId      organisation ID
     * @param pipelineId pipeline ID
     * @return the {@link Stream} of pipeline stage rows
     */
    public Collection<PipelineStage> getPipelineStagesByPipelineId(final Long orgId, final Long pipelineId) {
        final Table pipelineStageTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(pipelineStageTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(
                pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID), pipelineId)));
        selectQuery.addOrderByClause(new OrderByClause(pipelineStageTable.getColumn(MDOPIPELINESTAGE.ORDER)));
        final Stream<Row> pipelineStageRows = orgDataStore(orgId).get(selectQuery).getRows(pipelineStageTable);

        return Objects.isNull(pipelineStageRows) ? null : pipelineStageRows.map(this::convertRowToModel).toList();
    }

    /**
     * <p>
     * Gets the sort criteria that applied for each pipeline stage
     * </p>
     *
     * @param orgId                     Represents the organisation id
     * @param userId                    Represents the user Id
     * @param pipelineStageIdCollection Represents the collection of pipeline stage id
     * @return The map containing key as pipeline stage id and value as applied {@link SortCriteria}
     */
    public Map<Long, Optional<SortCriteria>> getPipelineStageCriteriaList(final Long orgId, final Long userId, final Collection<Long> pipelineStageIdCollection) {
        final Table kanbanSortTable = Table.get(OMKANBANSORTCONFIG.TABLE);
        final Table sortTable = Table.get(OMSORTCONFIG.TABLE);
        final Column sortConfigPkColumn = sortTable.getPKColumn();
        final Column kanbanStageIdColumn = kanbanSortTable.getColumn(OMKANBANSORTCONFIG.STAGE_ID);
        final Column kanbanSortIdColumn = kanbanSortTable.getColumn(OMKANBANSORTCONFIG.SORT_CONFIG_ID);
        final SelectQuery selectQuery = SelectQuery.get(kanbanSortTable);
        final Map<Long, Optional<SortCriteria>> stageSortCriteriaMap = new HashMap<>();

        selectQuery.addSelectClause(List.of(sortTable.getColumn(OMSORTCONFIG.CONFIG), sortTable.getColumn(OMSORTCONFIG.ORG_ID),
                kanbanStageIdColumn, kanbanSortIdColumn));
        selectQuery.addJoinClause(new JoinClause(kanbanSortIdColumn, sortConfigPkColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(kanbanStageIdColumn, pipelineStageIdCollection))
                .and(new WhereClause(Criteria.eq(sortTable.getColumn(OMSORTCONFIG.USER_ID), userId)))
                .and(new WhereClause(Criteria.eq(sortTable.getColumn(OMSORTCONFIG.VIEW_ID), SortViewType.KANBAN_SORT.getId()))));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        dataContainer.indexRows(kanbanStageIdColumn);
        final SortManagerImpl sortManager = SortManagerImpl.getInstance();

        for (final Long stageId : pipelineStageIdCollection) {
            final Row kanbanSortRow = dataContainer.getIndexedRow(kanbanStageIdColumn, stageId);

            if (Objects.nonNull(kanbanSortRow)) {
                final Long sortId = kanbanSortRow.get(OMKANBANSORTCONFIG.SORT_CONFIG_ID);
                final Row sortConfigRow = dataContainer.getRow(sortTable, sortId);

                if (Objects.nonNull(sortConfigRow)) {
                    stageSortCriteriaMap.putIfAbsent(stageId, sortManager.convertRowToCriteriaSet(sortConfigRow).stream().findFirst());
                }
            }
        }
        return stageSortCriteriaMap;
    }

    /**
     * {@inheritDoc}
     *
     * @param row Which contains the pipeline stage data
     * @return the {@link PipelineStage} filled with pipeline stage data
     */
    PipelineStage convertRowToModel(final Row row) {
        final PipelineStage pipelineStage = new PipelineStage();
        final Boolean isDefault = row.get(MDOPIPELINESTAGE.IS_DEFAULT);

        if (Objects.nonNull(isDefault)) {
            pipelineStage.setDefault(isDefault);
        }
        pipelineStage.setId(row.getPKValue());
        pipelineStage.setPipelineId(row.get(MDOPIPELINESTAGE.PIPELINE_ID));
        pipelineStage.setName(row.get(MDOPIPELINESTAGE.NAME));
        pipelineStage.setProbability(row.get(MDOPIPELINESTAGE.PROBABILITY));
        pipelineStage.setOrder(row.get(MDOPIPELINESTAGE.ORDER));
        pipelineStage.setOrgId(row.getOrgId());

        return pipelineStage;
    }

    /**
     * Checks whether the given pipeline and appropriate pipeline stage IDS are exist or not
     *
     * @param orgId               Represents the organisation ID
     * @param pipelineAndStageIds Which contains the pipeline and appropriate stage IDS
     * @return true if the given pipeline and stage IDS are exist and matched
     */
    public boolean exists(final Long orgId, final Map<Long, Collection<Long>> pipelineAndStageIds) {
        final Table pipelineStageTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(pipelineStageTable);
        final Column pipelineIdColumn = pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID);
        boolean validPipelineAndStages = false;

        selectQuery.addSelectClause(List.of(pipelineIdColumn, pipelineStageTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(pipelineIdColumn, pipelineAndStageIds.keySet())));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (!dataContainer.isEmpty()) {
            dataContainer.indexRows(pipelineIdColumn);
            int count = 0;

            for (final Map.Entry<Long, Collection<Long>> pipelineAndStageIdEntry : pipelineAndStageIds.entrySet()) {
                if (count > 0 && !validPipelineAndStages) {
                    break;
                }
                validPipelineAndStages = dataContainer.getIndexedRows(pipelineIdColumn, pipelineAndStageIdEntry.getKey())
                        .map(Row::getPKValue).collect(Collectors.toSet()).containsAll(pipelineAndStageIdEntry.getValue());
                count++;
            }
        }
        return validPipelineAndStages;
    }

    /**
     * Fetches the {@link PipelineStage} for the dropdown actions.
     *
     * @param orgId      Organization id.
     * @param pipelineId Refers to the pipeline id
     * @param limit      Which holds the limit configuration.
     * @param criteria   Which configure the criteria.
     * @return Collection of pipelineStages.
     */
    public Collection<PipelineStage> getDropdownList(final Long orgId, final Long pipelineId, final DropdownLimit limit,
                                                     final DropdownCriteria criteria) {
        final Table pipelineStageTable = getTable();
        final Column pipelineStageNameColumn = pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME);
        final Column pipelineStageOrderColumn = pipelineStageTable.getColumn(MDOPIPELINESTAGE.ORDER);
        final SelectQuery selectQuery = SelectQuery.get(pipelineStageTable);

        selectQuery.addSelectClause(List.of(pipelineStageTable.getPKColumn(), pipelineStageNameColumn, pipelineStageOrderColumn,
                pipelineStageTable.getColumn(MDOPIPELINESTAGE.IS_DEFAULT)));
        selectQuery.setLimitClause(limit.getLimitClause());
        final WhereClause whereClause = new WhereClause(criteria.build(pipelineStageNameColumn));

        selectQuery.setWhereClause(Objects.isNull(pipelineId) ? whereClause
                : whereClause.and(Criteria.eq(pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID), pipelineId)));
        selectQuery.addOrderByClause(new OrderByClause(pipelineStageOrderColumn));

        final Stream<Row> rows = orgDataStore(orgId).get(selectQuery).getRows(pipelineStageTable);

        return rows.map(row -> {
            final PipelineStage pipelineStage = new PipelineStage();

            pipelineStage.setId(row.getPKValue());
            pipelineStage.setDefault(row.get(MDOPIPELINESTAGE.IS_DEFAULT));
            pipelineStage.setName(row.get(MDOPIPELINESTAGE.NAME));
            pipelineStage.setOrder(row.get(MDOPIPELINESTAGE.ORDER));

            return pipelineStage;
        }).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId organisation ID
     * @param ids   which contains the collection of pipeline stage IDs
     * @return the {@link PipelineStage} with partial data
     */
    @Override
    public Collection<PipelineStage> partialGet(final Long orgId, final Collection<Long> ids) {
        final Table pipeLineStageTable = getTable();
        final SelectQuery query = SelectQuery.get(pipeLineStageTable);
        final Column pkColumn = pipeLineStageTable.getPKColumn();

        query.addSelectClause(List.of(pkColumn, pipeLineStageTable.getColumn(MDOPIPELINESTAGE.NAME)));
        query.setWhereClause(new WhereClause(Criteria.in(pkColumn, ids)));

        final Stream<Row> pipelineStageRows = orgDataStore(orgId).get(query).getRows(pipeLineStageTable);

        return Objects.nonNull(pipelineStageRows) ?
                pipelineStageRows.map(this::convertPartialRowToModel).toList() : Collections.emptyList();
    }

    /**
     * Migrates the pipelineStage deals to respective default pipelineStage for mapped pipeline id
     * and also deletes the selected pipeline stage
     *
     * @param orgId           Represents the organization id
     * @param pipelineStageId Represents the pipeline stage id to be deleted
     * @param pipelineId      Represents the pipeline id
     */
    public final void migratePipelineStageDeals(final Long orgId, final Long pipelineStageId, final Long pipelineId) {
        final DataContainer dataContainer = DataContainer.create();

        migratePipelineStageDeals(orgId, pipelineStageId, pipelineId, dataContainer);
        orgDataStore(orgId).commitChanges(dataContainer);
    }

    /**
     * Migrates the pipelineStage deals to respective default pipelineStage for mapped pipeline id
     * and also deletes the selected pipeline stage
     *
     * @param orgId           represents the organization id
     * @param pipelineStageId represents the pipeline stage id to be deleted
     * @param pipelineId      represents the pipeline id
     * @param targetContainer Represents the {@link DataContainer}
     */
    public final void migratePipelineStageDeals(final Long orgId, final Long pipelineStageId, final Long pipelineId,
                                                final DataContainer targetContainer) {
        final Table pipelineStageTable = getTable();
        final Table pipelineTable = Table.get(MDOPIPELINE.TABLE);
        final Table dealTable = Table.get(MDODEAL.TABLE);
        final Column stagePkColumn = pipelineStageTable.getPKColumn();
        final Column defaultStageColumn = pipelineStageTable.getColumn(MDOPIPELINESTAGE.IS_DEFAULT);
        final Column pipelineIdColumn = pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID);
        final Column dealStageIdColumn = dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID);
        final Criteria stagePipelineCriteria = Criteria.eq(pipelineIdColumn, pipelineId);
        final WhereClause stageWhereClause = new WhereClause(Criteria.eq(stagePkColumn, pipelineStageId));
        final WhereClause defaultWhereClause = new WhereClause((Criteria.eq(defaultStageColumn, true)));
        final WhereClause wonWhereClause = new WhereClause(Criteria.eq(pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME), State.WON));
        final WhereClause lostWhereClause = new WhereClause(Criteria.eq(pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME), State.LOST));
        final SelectQuery stageSelectQuery = SelectQuery.get(pipelineStageTable);
        final OrgDataStore orgDataStore = orgDataStore(orgId);

        stageSelectQuery.addJoinClause(new JoinClause(pipelineIdColumn, pipelineTable.getPKColumn()));
        stageSelectQuery.addSelectClause(List.of(pipelineIdColumn, stagePkColumn, pipelineTable.getColumn(MDOPIPELINE.IS_DEFAULT),
                defaultStageColumn, pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME)));
        stageSelectQuery.setWhereClause((defaultWhereClause.or(lostWhereClause).or(wonWhereClause)).and(stagePipelineCriteria).or(stageWhereClause));

        final DataContainer stageDataContainer = orgDataStore.get(stageSelectQuery);
        final Row stagePipelineRow = stageDataContainer.getRow(pipelineStageTable, stageWhereClause.and(stagePipelineCriteria));
        final Row defaultPipelineStageRow = stageDataContainer.getRow(pipelineStageTable, defaultWhereClause
                .and(Criteria.notEq(dealStageIdColumn, pipelineStageId)));
        final Row wonStageRow = stageDataContainer.getRow(pipelineStageTable, wonWhereClause);
        final Row lostStageRow = stageDataContainer.getRow(pipelineStageTable, lostWhereClause);

        if (Objects.isNull(stagePipelineRow)) {
            throw ErrorCode.get(DealErrorCodes.PIPELINE_NOT_FOUND, "i18n.pipeline.not.found");
        }

        if (Objects.nonNull(defaultPipelineStageRow)) {
            if (Objects.equals(defaultPipelineStageRow.get(stagePkColumn), pipelineStageId)
                    || Objects.equals(wonStageRow.get(stagePkColumn), pipelineStageId)
                    || Objects.equals(lostStageRow.get(stagePkColumn), pipelineStageId)) {
                throw ErrorCode.get(DealErrorCodes.PIPELINE_STAGE_DELETION_RESTRICTED);
            } else {
                final Long defaultStageId = defaultPipelineStageRow.getPKValue();
                final SelectQuery selectQuery = SelectQuery.get(dealTable);
                final Table dealStageProgressTable = Table.get(MDOSTAGEPROGRESS.TABLE);

                selectQuery.addJoinClause(new JoinClause(dealTable.getPKColumn(), dealStageProgressTable.getColumn(MDOSTAGEPROGRESS.DEAL_ID)));
                selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealStageIdColumn, pipelineStageId)));

                final DataContainer dataContainer = orgDataStore.get(selectQuery);
                final Stream<Row> dealRows = dataContainer.getRows(dealTable);
                final long currentTime = Instant.now().toEpochMilli();

                dealRows.forEach(dealRow -> {
                    final Long dealId = dealRow.getPKValue();

                    dealRow.set(dealStageIdColumn, defaultStageId);
                    targetContainer.updateRow(dealRow);
                    dataContainer.getRows(dealStageProgressTable, new WhereClause(Criteria.eq(dealStageProgressTable
                            .getColumn(MDOSTAGEPROGRESS.DEAL_ID), dealId))).forEach(targetContainer::deleteRow);

                    final Row stageProgressRow = new Row(dealStageProgressTable);

                    stageProgressRow.set(MDOSTAGEPROGRESS.DEAL_ID, dealId);
                    stageProgressRow.set(MDOSTAGEPROGRESS.PIPELINE_ID, dealRow.get(MDODEAL.PIPELINE_ID));
                    stageProgressRow.set(MDOSTAGEPROGRESS.PIPELINE_STAGE_ID, defaultStageId);
                    stageProgressRow.set(MDOSTAGEPROGRESS.START_TIME, currentTime);
                    targetContainer.addNewRow(stageProgressRow);
                });
                final Row pipelineStageRow = stageDataContainer.getRow(pipelineStageTable, new WhereClause((Criteria.eq(stagePkColumn, pipelineStageId))));

                if (Objects.nonNull(pipelineStageRow)) {
                    targetContainer.deleteRow(pipelineStageRow);
                }
            }
        }
    }

    /**
     * Converts partial stage row to pipelineStage
     *
     * @param pipelineStageRow Represents the row of pipelineStage
     * @return {@link PipelineStage}
     */
    public PipelineStage convertPartialRowToModel(final Row pipelineStageRow) {
        final PipelineStage pipelineStage = new PipelineStage();

        pipelineStage.setId(pipelineStageRow.getPKValue());
        pipelineStage.setName(pipelineStageRow.get(MDOPIPELINESTAGE.NAME));

        return pipelineStage;
    }

    /**
     * <p>
     * Gets the default pipeline stage for the corresponding pipeline.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param pipeLineId Represents the pipeline id
     * @return Entity of {@link PipelineStage}
     */
    public PipelineStage getDefaultPipeLineStage(final Long orgId, final Long pipeLineId) {
        final Table pipelineStageTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(pipelineStageTable);

        selectQuery.addSelectClause(pipelineStageTable.getPKColumn());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(pipelineStageTable.getColumn(MDOPIPELINESTAGE.IS_DEFAULT), true))
                .and(Criteria.eq(pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID), pipeLineId)));

        final Row defaultPipelineStageRow = orgDataStore(orgId).get(selectQuery).getRow(pipelineStageTable);

        if (Objects.nonNull(defaultPipelineStageRow)) {
            final PipelineStage pipelineStage = new PipelineStage();

            pipelineStage.setOrgId(orgId);
            pipelineStage.setId(defaultPipelineStageRow.getPKValue());

            return pipelineStage;
        }

        return null;
    }

    /**
     * <p>
     * Gets the pipeline stage name by given pipeline stage id
     * </p>
     *
     * @param orgId           represents the organization id
     * @param pipelineStageId represents the pipeline stage id
     * @return the pipeline stage name
     */
    public String getPipelineStageName(final Long orgId, final Long pipelineStageId) {
        final Table pipelineStageTable = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(pipelineStageTable, new WhereClause(Criteria.eq(pipelineStageTable.getPKColumn(), pipelineStageId)),
                List.of(pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME)));

        return dataContainer.isEmpty() ? null : dataContainer.getRow(pipelineStageTable).get(MDOPIPELINESTAGE.NAME);
    }

    /**
     * <p>
     * Gets the total count of pipelines stages matches for given pipeline id and org id
     * </p>
     *
     * @param orgId Represents the organization id
     * @return Count of pipelines stages
     */
    public Long getPipelineStageCount(final Long orgId, final Long pipelineId) {
        final Table pipelineStageTable = getTable();
        final CountQuery countQuery = CountQuery.get(pipelineStageTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID), pipelineId)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets the pipelines stage ids matches for given {@link Collection} of pipeline id
     * </p>
     *
     * @param orgId Represents the organization id
     * @return pipeline stage ids matches for {@link Collection} of pipeline id
     */
    public Map<Long, Collection<Long>> getPipelineStagesIds(final Long orgId, final Collection<Long> pipelineIds) {
        final Table pipelineStageTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(pipelineStageTable);
        final Map<Long, Collection<Long>> stageIds = new HashMap<>();
        final Column pipelineIdColumn = pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID);

        selectQuery.addSelectClause(pipelineIdColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(pipelineIdColumn, pipelineIds)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        for (final Long pipelineId : pipelineIds) {
            final Stream<Row> rows = dataContainer.getRows(pipelineStageTable, new WhereClause(Criteria.eq(pipelineIdColumn, pipelineId)));

            stageIds.put(pipelineId, rows.map(Row::getPKValue).collect(Collectors.toList()));
        }

        return stageIds;
    }

    /**
     * <p>
     * Gets the stage id based on state
     * </p>
     *
     * @param orgId Represents the organization id
     * @param state Represents the state
     * @return the pipeline stage id
     */
    public Long getPipelineStageId(final Long orgId, final State state, final Long dealId) {
        final Table pipelineStageTable = getTable();
        final Table dealTable = Table.get(MDODEAL.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(pipelineStageTable);
        final SelectQuery dealSelectQuery = SelectQuery.get(dealTable);

        dealSelectQuery.addSelectClause(dealTable.getColumn(MDODEAL.PIPELINE_ID));
        dealSelectQuery.setWhereClause(new WhereClause(Criteria.eq(dealTable.getPKColumn(), dealId)));
        final WhereClause whereClause = new WhereClause(Criteria.eq(pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID), dealSelectQuery));

        selectQuery.addSelectClause(pipelineStageTable.getPKColumn());
        selectQuery.setWhereClause(whereClause.and(Objects.equals(State.OPEN, state) ?
                new WhereClause(Criteria.eq(pipelineStageTable.getColumn(MDOPIPELINESTAGE.IS_DEFAULT), true)) :
                new WhereClause(Criteria.eq(pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME), state))
        ));
        final Row row = orgDataStore(orgId).get(selectQuery).getRow(pipelineStageTable);

        return Objects.nonNull(row) ? row.getPKValue() : null;
    }

    /**
     * <p>
     * Gets the pipeline stages mapped for {@link Collection} of pipeline id
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param pipelineIds Represents the {@link Collection} of pipeline id
     * @return the won, lost, default stages matches for {@link Collection} of pipeline id
     */
    public Map<Long, Map<State, PipelineStage>> getPipelineStages(final Long orgId, final Collection<Long> pipelineIds) {
        final Table pipelineStageTable = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(buildSelectQuery(null, pipelineIds));
        final Map<Long, Map<State, PipelineStage>> stages = new HashMap<>();

        for (final Long pipelineId : pipelineIds) {
            final Stream<Row> rows = dataContainer.getRows(pipelineStageTable,
                    new WhereClause(Criteria.eq(pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID), pipelineId)));

            stages.put(pipelineId, getPipelineStages(rows));
        }

        return stages;
    }

    /**
     * <p>
     * Gets pipeline stage mapped for pipeline id
     * </p>
     *
     * @param orgId      Represents the organization id
     * @param pipelineId Represents the pipeline id
     * @return the {@link Map} of state and pipeline stage matches for given pipeline id
     */
    public Map<State, PipelineStage> getPipelineStages(final Long orgId, final Long pipelineId) {
        return getPipelineStages(orgDataStore(orgId).get(buildSelectQuery(pipelineId, null)).getRows(getTable()));
    }

    /**
     * Builds the select query
     *
     * @param pipelineId  Represents the pipeline id
     * @param pipelineIds Represents the {@link Collection} of pipeline id
     * @return {@link SelectQuery}
     */
    private SelectQuery buildSelectQuery(final Long pipelineId, final Collection<Long> pipelineIds) {
        final Table pipelineStageTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(pipelineStageTable);
        final Column nameColumn = pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME);
        final Column pipelineIdColumn = pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID);
        final Column isDefaultColumn = pipelineStageTable.getColumn(MDOPIPELINESTAGE.IS_DEFAULT);
        final WhereClause whereClause = new WhereClause(Criteria.eq(isDefaultColumn, true)).
                or(new WhereClause(Criteria.in(nameColumn, List.of(State.LOST, State.WON))));

        selectQuery.addSelectClause(List.of(nameColumn, isDefaultColumn, pipelineStageTable.getColumn(MDOPIPELINESTAGE.ORDER),
                pipelineStageTable.getColumn(MDOPIPELINESTAGE.PROBABILITY), pipelineIdColumn));

        selectQuery.setWhereClause(whereClause.and(Objects.isNull(pipelineId) ? new WhereClause(Criteria.in(pipelineIdColumn, pipelineIds)) :
                new WhereClause(Criteria.eq(pipelineIdColumn, pipelineId))));

        return selectQuery;
    }

    /**
     * <p>
     * Gets the {@link PipelineStage} and corresponding to their state
     * </p>
     *
     * @param rows Holds the rows of the pipeline table
     * @return the {@link Map} of state and pipeline stage
     */
    private Map<State, PipelineStage> getPipelineStages(final Stream<Row> rows) {
        final Map<State, PipelineStage> stageMap = new HashMap<>();

        rows.filter(Objects::nonNull).forEach(row -> stageMap.put(State.getStateByName(row.get(MDOPIPELINESTAGE.NAME)),
                convertRowToModel(row)));

        return stageMap;
    }

    /**
     * <p>
     * Gets the default pipeline stage id mapped for given pipeline ids
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param pipelineIds Represents the {@link Collection} of pipeline id
     * @return {@link Map} of pipeline id and default pipeline stage id
     */
    public Map<Long, Long> getDefaultStageIds(final Long orgId, final Collection<Long> pipelineIds) {
        final Table pipelineStageTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(pipelineStageTable);
        final Map<Long, Long> defaultStageIds = new HashMap<>();
        final Column pipelineIdColumn = pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID);

        selectQuery.addSelectClause(pipelineIdColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(pipelineIdColumn, pipelineIds)).and
                (new WhereClause(Criteria.eq(pipelineStageTable.getColumn(MDOPIPELINESTAGE.IS_DEFAULT), true))));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        for (final Long pipelineId : pipelineIds) {
            final Row row = dataContainer.getRow(pipelineStageTable, new WhereClause(Criteria.eq(pipelineIdColumn, pipelineId)));

            if (Objects.nonNull(row)) {
                defaultStageIds.put(pipelineId, row.getPKValue());
            }
        }

        return defaultStageIds;
    }

    /**
     * <p>
     * Checks if all the specified pipeline stage IDs exist for a given pipeline
     * </p>
     *
     * @param orgId            Represents the organization id
     * @param pipelineId       Represents the pipeline id
     * @param pipelineStageIds The set of pipeline stage IDs to check for existence
     * @return true if all the pipeline stage IDs exist for the given pipeline, false otherwise
     */
    public boolean exists(final Long orgId, final Long pipelineId, final Set<Long> pipelineStageIds) {
        final Table pipelineStageTable = getTable();
        final CountQuery countQuery = CountQuery.get(pipelineStageTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(pipelineStageTable.getPKColumn(), pipelineStageIds))
                .and(Criteria.eq(pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID), pipelineId)));

        return pipelineStageIds.size() == orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Checks if the specified pipeline stages are draggable
     * </p>
     *
     * @param orgId            Represents the organization id
     * @param pipelineStageIds A set of pipeline stage IDs
     * @return True if all pipeline stages are draggable, false otherwise
     */
    public boolean isDraggableStages(final Long orgId, final Set<Long> pipelineStageIds) {
        final Table pipelineStageTable = getTable();
        final CountQuery countQuery = CountQuery.get(pipelineStageTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(pipelineStageTable.getPKColumn(), pipelineStageIds))
                .and(Criteria.notEq(pipelineStageTable.getColumn(MDOPIPELINESTAGE.IS_DEFAULT), true))
                .and(Criteria.notIn(pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME), List.of(Deal.State.WON, State.LOST))));

        return pipelineStageIds.size() == orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Updates the order of pipeline stages.
     * </p>
     *
     * @param orgId            Represents the organization id
     * @param pipelineStageIds Represents the collection of pipeline stage ids
     */
    public void updatePipelineStagesOrder(final Long orgId, final Collection<Long> pipelineStageIds) {
        orderManager.update(orgId, new Counter(1), pipelineStageIds, getTable());
    }

    /**
     * <p>
     * Gets the pipeline stage by using the pipeline stage id, it contains only partial data.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param pipelineStageId Represents the id the {@link PipelineStage}
     * @return Entity of the {@link PipelineStage}
     */
    public PipelineStage getPartialPipelineStageById(final Long orgId, final Long pipelineStageId) {
        final Table pipelineStageTable = getTable();
        final Column pipelineStagePKColumn = pipelineStageTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(pipelineStageTable);
        final String pipelineStageName = MDOPIPELINESTAGE.NAME;
        final String pipelineId = MDOPIPELINESTAGE.PIPELINE_ID;

        selectQuery.addSelectClause(List.of(pipelineStagePKColumn, pipelineStageTable.getColumn(pipelineStageName),
                pipelineStageTable.getColumn(pipelineId)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(pipelineStagePKColumn, pipelineStageId)));

        final Row pipelineStageRow = orgDataStore(orgId).get(selectQuery).getRow(pipelineStageTable);

        if (Objects.isNull(pipelineStageRow)) {
            return null;
        } else {
            final PipelineStage pipelineStage = new PipelineStage();

            pipelineStage.setId(pipelineStageRow.getPKValue());
            pipelineStage.setName(pipelineStageRow.get(pipelineStageName));
            pipelineStage.setPipelineId(pipelineStageRow.get(pipelineId));

            return pipelineStage;
        }
    }

    /**
     * <p>
     * Gets the default pipeline stage id by given pipeline id
     * </p>
     *
     * @param orgId      Represents the organization id
     * @param pipelineId Represents the pipeline id
     * @return The default pipeline stage id
     */
    public PipelineStage getDefaultPipelineStage(final Long orgId, final Long pipelineId) {
        final Table pipelineStageTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(pipelineStageTable);

        selectQuery.addSelectClause(List.of(pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID), pipelineId))
                .and(Criteria.eq(pipelineStageTable.getColumn(MDOPIPELINESTAGE.IS_DEFAULT), true)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return convertRowToModel(dataContainer.getRow(pipelineStageTable));
    }

    /**
     * <p>
     * Gets the default {@link PipelineStage} by given org id
     * </p>
     *
     * @param orgId Represents the organization id
     * @return The default {@link PipelineStage}
     */
    public PipelineStage getDefaultPipelineStage(final long orgId) {
        final Table pipelineTable = PipelineManager.getInstance().getTable();
        final Table pipelineStageTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(pipelineTable);

        selectQuery.addJoinClause(DmlUtil.getJoins(pipelineTable, pipelineStageTable));
        selectQuery.addSelectClause(pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(pipelineTable.getColumn(MDOPIPELINE.IS_DEFAULT), true))
                .and(Criteria.eq(pipelineStageTable.getColumn(MDOPIPELINESTAGE.IS_DEFAULT), true)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? null : convertRowToModel(dataContainer.getRow(pipelineStageTable));
    }

    /**
     * <p>
     * Publishes pipeline stage details to Deal Index.
     * </p>
     *
     * @param orgId         Represents an organization's id.
     * @param pipelineStage Represents {@link PipelineStage}.
     */
    private void publishPipelineStageToIndex(final Long orgId, final PipelineStage pipelineStage) {
        final DealManager dealManager = DealManager.getInstance();

        dealManager.getDealIds(orgId, pipelineStage.id(), pipelineStage.getPipelineId()).forEach(dealId -> {
            final Deal deal = new Deal();

            deal.setOrgId(orgId);
            deal.setId(dealId);
            deal.setPipelineStage(pipelineStage);
            indexDataPublisher.publishIndexData(orgId, List.of(deal), Action.PARTIAL_UPDATE);
        });
    }

    /**
     * Gets the {@link Map} of pipeline stage names by the given {@link Collection} of pipeline stage IDs.
     *
     * @param orgId            represents an organization ID
     * @param pipelineStageIds represents the pipeline stage IDs
     * @return a {@link Map} where the key is the pipeline stage ID and the value is the pipeline stage name
     */
    public Map<Long, String> getPipelineStageNames(final long orgId, final Collection<Long> pipelineStageIds) {
        final Table pipelineStageTable = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(pipelineStageTable, new WhereClause(Criteria.in(pipelineStageTable.getPKColumn(), pipelineStageIds)));

        if (dataContainer.isEmpty()) {
            return Collections.emptyMap();
        }

        return dataContainer.getRows(pipelineStageTable).collect(Collectors.toMap(
                Row::getPKValue, row -> row.get(pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME))));
    }

    /**
     * <p>
     * Retrieves the total pipeline stage count
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param pipelineId       Represents the pipelineId
     * @param dropdownCriteria Represents the dropdown criteria
     * @return The count of entities
     */
    public long getTotalCount(final Long orgId, final Long pipelineId, final DropdownCriteria dropdownCriteria) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);
        final WhereClause whereClause = Objects.isNull(pipelineId) ? null :
                new WhereClause(Criteria.eq(table.getColumn(MDOPIPELINESTAGE.PIPELINE_ID), pipelineId));

        countQuery.setWhereClause(WhereClauseUtil.connect(whereClause, new WhereClause(dropdownCriteria.build(table.getColumn(MDOPIPELINESTAGE.NAME))),
                Connector.AND));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets the total count of pipelines stages matches for given pipeline id and org id
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param pipelineIds Represents the {@link Collection} of pipeline id
     * @return {@link Map} of pipeline id and their stages count
     */
    public Map<Long, Long> getPipelineStagesCount(final long orgId, final Collection<Long> pipelineIds) {
        final Table pipelineStageTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(pipelineStageTable);
        final Column pipelineIdColumn = pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID);

        selectQuery.addGroupByClause(new GroupByClause(pipelineIdColumn));
        selectQuery.addSelectClause(List.of(pipelineStageTable.getPKColumn().count(), pipelineIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(pipelineIdColumn, pipelineIds)));

        return Collections.unmodifiableMap(orgDataStore(orgId).getData(selectQuery).stream().collect(Collectors.toMap(
                row -> (Long) row.get(MDOPIPELINESTAGE.PIPELINE_ID),
                row -> (Long) row.get(MDOPIPELINESTAGE.ID))));
    }

    /**
     * <p>
     * Checks whether any pipeline stage name exist or not
     * </p>
     *
     * @param orgId              Represents the org id
     * @param pipelineId         Represents the pipeline id
     * @param pipelineStageNames Represents the pipeline stage names
     * @return true if any pipeline stage exists, false otherwise
     */
    public boolean anyPipelineStageExists(final Long orgId, final Long pipelineId, final Collection<String> pipelineStageNames) {
        final Table pipelineStageTable = getTable();
        final CountQuery countQuery = CountQuery.get(pipelineStageTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID), pipelineId))
                .and(Criteria.in(pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME), pipelineStageNames)));

        return orgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }
}