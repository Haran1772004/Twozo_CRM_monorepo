package com.tlc.crm.core.api.deal.manager;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.field.deal.PipelineFields;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;
import com.tlc.crm.core.internal.event.index.publisher.IndexDataPublisher;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MDOPIPELINE;
import com.tlc.crm.core.sql.resource.MDOPIPELINESTAGE;
import com.tlc.crm.core.sql.resource.MDOSTAGEPROGRESS;
import com.tlc.crm.core.sql.resource.OMDOUSERFORECASTPREFERENCE;
import com.tlc.crm.core.sql.resource.OMDOUSERPIPELINE;
import com.tlc.crm.core.sql.resource.OMDOUSERPREFERENCE;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.handler.OrderManager;
import com.tlc.crm.entity.resolver.OrderResolver;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DmlUtil;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the {@link Pipeline} related action
 *
 * @author Sundar
 * @version 1.0
 */
public class PipelineManager extends AbstractConfigManager<Pipeline> {

    private static final int DEFAULT_PIPELINE_ORDER = 1;

    private final PipelineStageManager pipelineStageManager;
    private final OrderResolver orderResolver;
    private final OrderManager orderManager;
    private final IndexDataPublisher indexDataPublisher;

    private PipelineManager() {
        pipelineStageManager = PipelineStageManager.getInstance();
        orderResolver = OrderResolver.getInstance();
        orderManager = OrderManager.getInstance();
        indexDataPublisher = IndexDataPublisher.getInstance();
    }

    private static class Instance {
        private static final PipelineManager INSTANCE = new PipelineManager();
    }

    /**
     * Gets the {@link PipelineManager} instance
     *
     * @return the {@link PipelineManager} instance
     */
    public static PipelineManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Table} of pipeline
     */
    @Override
    public Table getTable() {
        return Table.get(MDOPIPELINE.TABLE);
    }

    /**
     * <p>
     * Gets the user pipeline table
     * </p>
     *
     * @return the user pipeline {@link Table}
     */
    private Table getUserPipelineTable() {
        return Table.get(OMDOUSERPIPELINE.TABLE);
    }

    /**
     * <p>
     * Gets the pipeline stage table
     * </p>
     *
     * @return the pipeline stage {@link Table}
     */
    private Table getPipelineStageTable() {
        return pipelineStageManager.getTable();
    }

    /**
     * <p>
     * Fetches the {@link Pipeline} that has one of the id value mentioned in the parameter
     * </p>
     *
     * @param orgId Organization id
     * @param ids   Pipelines id
     * @return Collection of pipeline
     */
    @Override
    public Collection<Pipeline> get(final Long orgId, final Collection<Long> ids) {
        final Table pipelineTable = getTable();
        final SelectQuery selectQuery = buildPipelineSelectQuery(pipelineTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(pipelineTable.getPKColumn(), ids)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> rows = dataContainer.getRows(pipelineTable);

        if (Objects.isNull(rows)) {
            throw ErrorCode.get(DealErrorCodes.PIPELINE_NOT_FOUND, "i18n.pipeline.not.found");
        } else {
            return rows.map(row -> convertRowToModel(row, dataContainer, false)).toList();
        }
    }

    /**
     * Gets the {@link Pipeline} by name
     *
     * @param orgId      organisation ID
     * @param pipelineId pipeline ID
     * @param name       pipeline name
     * @return {@link Pipeline} which already exists
     */
    public Pipeline getPipelineByName(final Long orgId, final Long pipelineId, final String name) {
        final Table pipelineTable = getTable();
        final SelectQuery selectQuery = buildPipelineSelectQuery(pipelineTable);

        selectQuery.setWhereClause(getPipelineFilter(pipelineId, name, pipelineTable));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row pipelineRow = dataContainer.getRow(pipelineTable);

        return Objects.nonNull(pipelineRow) ? convertRowToModel(pipelineRow, dataContainer, false) : null;
    }

    /**
     * Builds the {@link SelectQuery} to fetch pipeline and pipeline stage data
     *
     * @param pipelineTable Represents the pipeline table
     * @return {@link SelectQuery}
     */
    private SelectQuery buildPipelineSelectQuery(final Table pipelineTable) {
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(pipelineTable);

        selectQuery.addJoinClause(new JoinClause(pipelineTable.getPKColumn(),
                pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID)));
        selectQuery.addSelectClause(pipelineTable, pipelineStageTable);

        return selectQuery;
    }

    /**
     * Checks whether the pipeline is already exists or not by given name and id
     *
     * @param orgId organisation ID
     * @param id    pipeline ID
     * @param name  pipeline name
     * @return true if the given named pipeline is already exists
     */
    public boolean isPipelineAlreadyExists(final Long orgId, final Long id, final String name) {
        final Table pipelineTable = getTable();
        final CountQuery countQuery = CountQuery.get(pipelineTable);

        countQuery.setWhereClause(getPipelineFilter(id, name, pipelineTable));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * Constructs the where clause which can be used to filter the pipeline by its name and ID
     *
     * @param id    pipeline ID
     * @param name  pipeline name
     * @param table represents the pipeline table
     * @return {@link WhereClause} which contains the criteria to filter pipeline
     */
    private WhereClause getPipelineFilter(final Long id, final String name, final Table table) {
        final WhereClause condition = new WhereClause(Criteria.eq(table.getColumn(MDOPIPELINE.NAME), name));

        return Objects.nonNull(id) ? condition.and(Criteria.notEq(table.getPKColumn(), id)) : condition;
    }

    /**
     * <p>
     * Saves the attributes of {@link Pipeline} into the database
     * </p>
     *
     * @param pipeline An entity that holds the pipeline attributes which have to be saved into database
     */
    @Override
    public Pipeline create(Pipeline pipeline) {
        final DataContainer targetContainer = DataContainer.create();

        loadPipelineIntoContainer(true, pipeline, targetContainer);
        orgDataStore(pipeline.orgId()).commitChanges(targetContainer);

        return pipeline;
    }

    /**
     * <p>
     * Inserts or Updates crm user information based on the provided flag and {@link CrmUser} object
     * </p>
     *
     * @param isCreateEvent A flag indicating whether the operation is triggered by a create event
     * @param crmUser       An entity that holds the {@link CrmUser} details
     */
    public void saveEntity(final boolean isCreateEvent, final CrmUser crmUser) {
        final DataContainer targetContainer = DataContainer.create();

        loadUserPipelineIntoContainer(isCreateEvent, crmUser, targetContainer);

        if (!targetContainer.isEmpty()) {
            orgDataStore(crmUser.orgId()).commitChanges(targetContainer);
        }
    }

    /**
     * <p>
     * Loads the user preferred pipeline details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param crmUser         An entity that holds the {@link CrmUser} details
     * @param targetContainer Holds the updated information of user preferred pipeline details
     */
    public void loadUserPipelineIntoContainer(final boolean isCreateEvent, final CrmUser crmUser,
                                              final DataContainer targetContainer) {
        final Long pipelineId = crmUser.getPipelineId();

        if (Objects.nonNull(pipelineId)) {
            final Long orgId = crmUser.orgId();
            final Long crmUserId = crmUser.id();
            final Table pipelineUserTable = getUserPipelineTable();
            final Row row = isCreateEvent ? new Row(pipelineUserTable) :
                    orgDataStore(orgId).get(pipelineUserTable, new WhereClause(
                            Criteria.eq(pipelineUserTable.getColumn(OMDOUSERPIPELINE.USER_ID), crmUserId))).getRow(pipelineUserTable);
            final Row userRow = Objects.isNull(row) ? new Row(pipelineUserTable) : row;

            if (userRow.isNewRow()) {
                userRow.set(OMDOUSERPIPELINE.USER_ID, crmUserId);
                userRow.setOrgId(orgId);
            }
            userRow.set(OMDOUSERPIPELINE.PIPELINE_ID, pipelineId);

            targetContainer.updateRow(userRow);
        }
    }

    /**
     * <p>
     * Updates the given pipelines to database
     * </p>
     *
     * @param orgId     Organization id
     * @param pipelines Collection of pipelines that have to be updated
     * @return Updated status
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<Pipeline> pipelines) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<Long> ids = pipelines.stream().map(Pipeline::id).filter(Objects::nonNull).toList();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final DataContainer entityContainer = orgDataStore.get(getTable(), ids);

        pipelines.forEach(pipeline -> {
            try {
                final Long pipelineId = pipeline.id();
                final DataContainer targetContainer = DataContainer.create();

                loadPipelineIntoContainer(false, pipeline, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(pipelineId);
                publishPipelineToIndex(orgId, pipeline);
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Pipeline update failed Org ID : {} ID : {} reason : {}", orgId, pipeline.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(DealErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, pipeline.id().toString()));
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
     * Loads the pipeline details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param pipeline        An entity that holds the pipeline attributes
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadPipelineIntoContainer(final boolean isCreateEvent, final Pipeline pipeline, final DataContainer targetContainer) {
        final DataContainer entityContainer = isCreateEvent ? null :
                orgDataStore(pipeline.orgId()).get(getTable(), List.of(pipeline.id()));

        loadPipelineIntoContainer(isCreateEvent, pipeline, entityContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the pipeline details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param pipeline        An entity that holds the pipeline attributes
     * @param sourceContainer Holds the user defined and system field values of existing pipeline
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadPipelineIntoContainer(final boolean isCreateEvent, final Pipeline pipeline, final DataContainer sourceContainer,
                                          final DataContainer targetContainer) {
        final Table pipelineTable = getTable();
        final Long id = pipeline.id();
        final Long orgId = pipeline.orgId();
        final boolean isDefault = pipeline.isDefault();
        final String defaultPipeline = MDOPIPELINE.IS_DEFAULT;
        final Row pipelineRow = isCreateEvent ? new Row(pipelineTable)
                : Optional.ofNullable(sourceContainer.getRow(pipelineTable, id)).map(Row::new).orElse(null);

        if (!isCreateEvent && Objects.isNull(pipelineRow)) {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, "i18n.deal.not.found");
        }

        if (pipelineRow.isNewRow()) {
            pipelineRow.setOrgId(orgId);
            pipeline.setId(pipelineRow.getPKValue());
            pipelineRow.set(MDOPIPELINE.ORDER, orderResolver.getNextOrder(orgId, null, pipelineTable));
        }

        pipelineRow.set(MDOPIPELINE.NAME, pipeline.getName());
        pipelineRow.set(MDOPIPELINE.ROTTEN_DAYS, pipeline.getRottenDays());

        if (isDefault) {
            final Number order = pipelineRow.get(MDOPIPELINE.ORDER);
            final DataContainer dataContainer = orgDataStore(orgId).get(pipelineTable, new WhereClause(Criteria.le(pipelineTable.getColumn(MDOPIPELINE.ORDER),
                    order.shortValue())));

            if (!dataContainer.isEmpty()) {
                final Counter orderIncrementer = new Counter(DEFAULT_PIPELINE_ORDER);

                dataContainer.getRows(pipelineTable).forEach(row -> {

                    if (row.get(MDOPIPELINE.IS_DEFAULT)) {
                        row.set(MDOPIPELINE.IS_DEFAULT, false);
                    }
                    row.set(MDOPIPELINE.ORDER, orderIncrementer.incrementAndGet());
                    targetContainer.updateRow(row);
                });
                pipelineRow.set(MDOPIPELINE.ORDER, DEFAULT_PIPELINE_ORDER);
            }
        }
        pipelineRow.set(defaultPipeline, isDefault);
        targetContainer.updateRow(pipelineRow);

        if (Objects.nonNull(pipeline.getPipelineStages())) {
            final Collection<PipelineStage> stages = pipeline.getPipelineStages();

            stages.forEach(pipelineStage -> pipelineStage.setPipelineId(pipeline.id()));
            pipelineStageManager.loadPipelineStageIntoContainer(pipeline.orgId(), stages, targetContainer);
        }
    }

    /**
     * <p>
     * Converts row into {@link Pipeline}
     * </p>
     *
     * @param row           Pipeline row to be converted
     * @param dataContainer A container that holds the pipeline and pipeline stage data
     * @param isPartial     Refers that the row should be converted to model partially or not
     * @return {@link Pipeline}
     */
    public Pipeline convertRowToModel(final Row row, final DataContainer dataContainer, final boolean isPartial) {
        final Pipeline pipeline = new Pipeline();
        final Boolean isDefault = row.get(MDOPIPELINE.IS_DEFAULT);

        pipeline.setId(row.getPKValue());
        pipeline.setOrgId(row.getOrgId());
        pipeline.setName(row.get(MDOPIPELINE.NAME));
        pipeline.setRottenDays(row.get(MDOPIPELINE.ROTTEN_DAYS));
        pipeline.setOrder(row.get(MDOPIPELINE.ORDER));

        if (Objects.nonNull(isDefault)) {
            pipeline.setDefault(isDefault);
        }

        if (!isPartial) {
            final Table pipelineStageTable = pipelineStageManager.getTable();
            final Stream<Row> pipelineStageRows = dataContainer.getRows(pipelineStageTable,
                    new WhereClause(Criteria.eq(pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID), pipeline.id())));

            if (Objects.nonNull(pipelineStageRows)) {
                pipeline.setPipelineStages(pipelineStageRows.sorted(Comparator.comparing(stage ->
                        stage.get(MDOPIPELINESTAGE.ORDER))).map(pipelineStageManager::convertRowToModel).collect(Collectors.toList()));
            }
        }

        return pipeline;
    }

    /**
     * Fetches the {@link Pipeline} for the dropdown actions.
     *
     * @param orgId    Organization id.
     * @param limit    Which holds the limit configuration.
     * @param criteria Which configure the criteria.
     * @return Collection of pipelines.
     */
    public Collection<Pipeline> getPipelineDropdownList(final Long orgId, final DropdownLimit limit,
                                                        final DropdownCriteria criteria) {
        final Table pipelineTable = getTable();
        final Column pipelineNameColumn = pipelineTable.getColumn(MDOPIPELINE.NAME);
        final Column pipelineOrderColumn = pipelineTable.getColumn(MDOPIPELINE.ORDER);
        final SelectQuery selectQuery = SelectQuery.get(pipelineTable);

        selectQuery.addSelectClause(List.of(pipelineTable.getPKColumn(), pipelineNameColumn, pipelineOrderColumn,
                pipelineTable.getColumn(MDOPIPELINE.IS_DEFAULT)));
        selectQuery.setLimitClause(limit.getLimitClause());
        selectQuery.setWhereClause(new WhereClause(criteria.build(pipelineNameColumn)));
        selectQuery.addOrderByClause(List.of(new OrderByClause(pipelineOrderColumn),
                new OrderByClause(pipelineNameColumn)));

        final Stream<Row> rows = orgDataStore(orgId).get(selectQuery).getRows(pipelineTable);

        if (Objects.isNull(rows)) {
            throw ErrorCode.get(DealErrorCodes.PIPELINE_NOT_FOUND, "i18n.pipeline.not.found");
        } else {
            final List<Pipeline> pipelineList = new ArrayList<>();

            rows.forEach(row -> {
                final Pipeline pipeline = new Pipeline();

                pipeline.setId(row.getPKValue());
                pipeline.setDefault(row.get(MDOPIPELINE.IS_DEFAULT));
                pipeline.setName(row.get(MDOPIPELINE.NAME));
                pipeline.setOrder(row.get(MDOPIPELINE.ORDER));
                pipelineList.add(pipeline);
            });

            return pipelineList;
        }
    }

    /**
     * Checks whether all the given pipelines are exists or not
     *
     * @param orgId       represents the Organisation ID
     * @param pipelineIds which contains the collection of pipeline IDs
     * @return true if all the deals are exists
     */
    public boolean exists(final Long orgId, final Set<Long> pipelineIds) {
        if (Objects.nonNull(pipelineIds) && !pipelineIds.isEmpty()) {
            final Table pipelineTable = getTable();
            final CountQuery pipelinesCountQuery = CountQuery.get(pipelineTable);

            pipelinesCountQuery.setWhereClause(new WhereClause(Criteria.in(pipelineTable.getPKColumn(), pipelineIds)));

            return pipelineIds.size() == orgDataStore(orgId).get(pipelinesCountQuery);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId organisation ID
     * @param ids   which contains the collection of pipeline IDs
     * @return the {@link Pipeline} filled with partial data
     */
    @Override
    public Collection<Pipeline> partialGet(final Long orgId, final Collection<Long> ids) {
        final Table pipelineTable = getTable();
        final SelectQuery query = SelectQuery.get(pipelineTable);

        query.addSelectClause(List.of(pipelineTable.getPKColumn(), pipelineTable.getColumn(MDOPIPELINE.NAME)));
        query.setWhereClause(new WhereClause(Criteria.in(pipelineTable.getPKColumn(), ids)));

        final DataContainer dataContainer = orgDataStore(orgId).get(query);
        final Stream<Row> pipelineRows = dataContainer.getRows(pipelineTable);

        return Objects.nonNull(pipelineRows) ? pipelineRows.map(pipelineRow ->
                convertRowToModel(pipelineRow, null, true)).toList() : Collections.emptyList();
    }

    /**
     * Fetches all pipeline of an organization.
     *
     * @param orgId Refers the organization id.
     * @return {@link Collection} of pipelines of an organization.
     */
    public Collection<Pipeline> getAllPipelineByOrg(final Long orgId) {
        final Table table = getTable();
        final SelectQuery selectQuery = buildPipelineSelectQuery(table);

        selectQuery.setWhereClause(new WhereClause(Criteria.notEq(table.getPKColumn(), null)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> pipelineRows = dataContainer.getRows(table);

        return Objects.isNull(pipelineRows) ? null : pipelineRows.map(pipeline ->
                convertRowToModel(pipeline, dataContainer, false)).toList();
    }

    /**
     * Migrates the pipeline deals to default pipeline for mapped pipeline id
     * and also deletes the selected pipeline
     *
     * @param orgId      represents the organization id
     * @param pipelineId represents the pipeline id to be deleted
     */
    public final List<Deal> migratePipelineDeals(final Long orgId, final Long pipelineId) {
        final Table pipelineTable = getTable();
        final Table dealTable = Table.get(MDODEAL.TABLE);
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final Table userPipelineTable = getUserPipelineTable();
        final Table dealStageProgressTable = Table.get(MDOSTAGEPROGRESS.TABLE);
        final Table userPreferenceTable = Table.get(OMDOUSERPREFERENCE.TABLE);
        final Column pipelinePkColumn = pipelineTable.getPKColumn();
        final Column pipelineDefaultColumn = pipelineTable.getColumn(MDOPIPELINE.IS_DEFAULT);
        final Column stageDefaultColumn = pipelineStageTable.getColumn(MDOPIPELINESTAGE.IS_DEFAULT);
        final Column stagePipelineIdColumn = pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID);
        final Column userPipelineIdColumn = userPipelineTable.getColumn(OMDOUSERPIPELINE.PIPELINE_ID);
        final SelectQuery stageSelectQuery = SelectQuery.get(pipelineStageTable);
        final Criteria defaultStageCriteria = Criteria.eq(stageDefaultColumn, true);
        final Criteria pipelineCriteria = Criteria.eq(pipelinePkColumn, pipelineId);
        final DataContainer targetContainer = DataContainer.create();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        stageSelectQuery.addJoinClause(List.of(new JoinClause(stagePipelineIdColumn, pipelinePkColumn),
                new JoinClause(pipelinePkColumn, userPipelineIdColumn, leftJoin),
                new JoinClause(pipelinePkColumn, dealStageProgressTable.getColumn(MDOSTAGEPROGRESS.PIPELINE_ID), leftJoin),
                new JoinClause(pipelinePkColumn, userPreferenceTable.getColumn(OMDOUSERPREFERENCE.PIPELINE_ID), leftJoin)));
        stageSelectQuery.addSelectClause(List.of(stagePipelineIdColumn, pipelineDefaultColumn, stageDefaultColumn));
        stageSelectQuery.addSelectClause(userPipelineTable, dealStageProgressTable, userPreferenceTable);
        stageSelectQuery.setWhereClause(new WhereClause(Criteria.eq(pipelineDefaultColumn, true))
                .and(defaultStageCriteria).or(pipelineCriteria));
        final DataContainer stageDataContainer = orgDataStore.get(stageSelectQuery);
        final Row defaultPipelineStageRow = stageDataContainer.getRow(pipelineStageTable, new WhereClause(defaultStageCriteria)
                .and(Criteria.notEq(stagePipelineIdColumn, pipelineId)));

        if (Objects.isNull(defaultPipelineStageRow)) {
            throw ErrorCode.get(DealErrorCodes.PIPELINE_DELETION_RESTRICTED);
        } else {
            final Long defaultStagePipelineId = defaultPipelineStageRow.get(MDOPIPELINESTAGE.PIPELINE_ID);
            final Long defaultStageId = defaultPipelineStageRow.getPKValue();
            final Column dealPipelineColumn = dealTable.getColumn(MDODEAL.PIPELINE_ID);
            final int openStateId = Deal.State.OPEN.getId();
            final Stream<Row> dealRows = orgDataStore.get(dealTable,
                    new WhereClause(Criteria.eq(dealPipelineColumn, pipelineId))).getRows(dealTable);
            final long currentTime = Instant.now().toEpochMilli();
            final List<Long> migratedDealIds = new ArrayList<>();

            dealRows.forEach(dealRow -> {
                final Long dealId = dealRow.getPKValue();

                migratedDealIds.add(dealId);
                stageDataContainer.getRows(dealStageProgressTable, new WhereClause(Criteria.eq(dealStageProgressTable
                        .getColumn(MDOSTAGEPROGRESS.DEAL_ID), dealId))).forEach(targetContainer::deleteRow);
                final Row stageProgressRow = new Row(dealStageProgressTable);

                stageProgressRow.set(MDOSTAGEPROGRESS.DEAL_ID, dealId);
                stageProgressRow.set(MDOSTAGEPROGRESS.PIPELINE_ID, defaultStagePipelineId);
                stageProgressRow.set(MDOSTAGEPROGRESS.PIPELINE_STAGE_ID, defaultStageId);
                stageProgressRow.set(MDOSTAGEPROGRESS.START_TIME, currentTime);
                targetContainer.addNewRow(stageProgressRow);

                dealRow.set(MDODEAL.PIPELINE_STAGE_ID, defaultStageId);
                dealRow.set(dealPipelineColumn, defaultStagePipelineId);
                dealRow.set(MDODEAL.STATE, openStateId);
                targetContainer.updateRow(dealRow);
            });
            final Row pipelineRow = stageDataContainer.getRow(pipelineTable, new WhereClause((pipelineCriteria)));
            final Stream<Row> userPipelineRows = stageDataContainer.getRows(userPipelineTable, new WhereClause(Criteria.eq(userPipelineIdColumn, pipelineId)));

            userPipelineRows.forEach(userPipelineRow -> {
                userPipelineRow.set(OMDOUSERPIPELINE.PIPELINE_ID, defaultStagePipelineId);
                targetContainer.updateRow(userPipelineRow);
            });
            final Stream<Row> userPreferenceRows = stageDataContainer.getRows(userPreferenceTable);

            userPreferenceRows.forEach(userPreferenceRow -> {
                userPreferenceRow.set(OMDOUSERPREFERENCE.PIPELINE_ID, defaultStagePipelineId);
                targetContainer.updateRow(userPreferenceRow);
            });

            if (Objects.nonNull(pipelineRow)) {
                targetContainer.deleteRow(pipelineRow);
                orgDataStore.commitChanges(targetContainer);

                if (migratedDealIds.isEmpty()) {

                    return List.of();
                }
                final Pipeline defaultPipeline = new Pipeline();
                final PipelineStage defaultStage = new PipelineStage();

                defaultPipeline.setId(defaultStagePipelineId);
                defaultStage.setId(defaultStageId);

                return migratedDealIds.stream().map(dealId -> {
                    final Deal deal = new Deal();

                    deal.setOrgId(orgId);
                    deal.setId(dealId);
                    deal.setPipeline(defaultPipeline);
                    deal.setPipelineStage(defaultStage);
                    deal.setState(Deal.State.OPEN);

                    return deal;
                }).toList();
            } else {
                throw ErrorCode.get(DealErrorCodes.PIPELINE_NOT_FOUND, "i18n.pipeline.not.found");
            }
        }
    }

    /**
     * <p>
     * Gets the pipeline and respective pipeline stages
     * </p>
     *
     * @param orgId                    Represents an organization id
     * @param userId                   Represents the user id
     * @param dropdownLimit            Refers the limit configuration to be fetched.
     * @param criteria                 Which configure the criteria.
     * @return {@link Collection} of pipelines
     */
    public Collection<Pipeline> getPipelineList(final long orgId, final long userId, final DropdownLimit dropdownLimit,
                                                final DropdownCriteria criteria, final boolean isForecastView) {
        final Table pipelineTable = getTable();
        final Table pipelineStageTable = getPipelineStageTable();
        final Table userPreferenceTable = Table.get(OMDOUSERPREFERENCE.TABLE);
        final Table userForecastPreferenceTable = Table.get(OMDOUSERFORECASTPREFERENCE.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(pipelineStageTable);
        final Column stagePipelineIdColumn = pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID);
        final Column pipelinePkColumn = pipelineTable.getPKColumn();
        final Column userPreferencePipelineColumn = userPreferenceTable.getColumn(OMDOUSERPREFERENCE.PIPELINE_ID);
        final OrderByClause pipelineOrderByClause = new OrderByClause(pipelineTable.getColumn(MDOPIPELINE.ORDER));
        final List<OrderByClause> orderByClauses = List.of(pipelineOrderByClause, new OrderByClause(pipelineStageTable.getColumn(MDOPIPELINESTAGE.ORDER)));
        final LimitClause limitClause = dropdownLimit.getLimitClause();
        final SelectQuery innerQuery = SelectQuery.get(pipelineTable);

        innerQuery.addSelectClause(pipelinePkColumn);
        innerQuery.addOrderByClause(pipelineOrderByClause);
        innerQuery.setLimitClause(limitClause);

        if (null != criteria) {
            innerQuery.setWhereClause(new WhereClause(criteria.build(pipelineTable.getColumn(MDOPIPELINE.NAME))));
        }
        selectQuery.setWhereClause(new WhereClause(Criteria.in(stagePipelineIdColumn, innerQuery)));
        selectQuery.addSelectClause(pipelineStageTable, pipelineTable);
        final List<JoinClause> joins = new ArrayList<>(NumericalConstant.TWO);

        joins.add(new JoinClause(stagePipelineIdColumn, pipelinePkColumn));

        if (isForecastView) {
            selectQuery.addSelectClause(userForecastPreferenceTable);
            joins.add(new JoinClause(pipelinePkColumn, userForecastPreferenceTable.getColumn(OMDOUSERFORECASTPREFERENCE.PIPELINE_ID), JoinClause.JoinType.LEFT));
        } else {
            selectQuery.addSelectClause(userPreferenceTable);
            joins.add(new JoinClause(pipelinePkColumn, userPreferencePipelineColumn, JoinClause.JoinType.LEFT));
        }
        selectQuery.addJoinClause(joins);
        selectQuery.addOrderByClause(orderByClauses);

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> pipelineRows = dataContainer.getRows(pipelineTable);
        final Collection<Pipeline> pipelines = Objects.isNull(pipelineRows) ? Collections.emptySet() :
                pipelineRows.map(pipelineRow -> convertRowToModel(pipelineRow, dataContainer, false))
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        if (isForecastView) {
            final Row userForecastPreferenceRow = dataContainer.getRow(userForecastPreferenceTable,
                    new WhereClause(Criteria.eq(userPreferenceTable.getColumn(OMDOUSERFORECASTPREFERENCE.USER_ID), userId)));

            if (Objects.nonNull(userForecastPreferenceRow)) {
                markPipelineAsSelected(userForecastPreferenceRow.get(OMDOUSERFORECASTPREFERENCE.PIPELINE_ID), pipelines);
            }
        } else if (dataContainer.size(userPreferenceTable) > 0) {
            final Row userPreferenceRow = dataContainer.getRow(userPreferenceTable,
                    new WhereClause(Criteria.eq(userPreferenceTable.getColumn(OMDOUSERPREFERENCE.USER_ID), userId)));

            if (Objects.nonNull(userPreferenceRow)) {
                markPipelineAsSelected(userPreferenceRow.get(OMDOUSERPIPELINE.PIPELINE_ID), pipelines);
            }
        }

        return pipelines;
    }

    /**
     * <p>
     * Retrieves stage list data grouped by pipeline for stage list dropdown.
     * Criteria if present will be applied on stage name so only matched stages are returned.
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param dropdownLimit    Represents dropdown limit configuration
     * @param dropdownCriteria Represents dropdown criteria to be applied on stage name
     * @return Collection of pipelines with matched stages
     */
    public Collection<Pipeline> getPipelineForStageList(final Long orgId, final DropdownLimit dropdownLimit,
                                                        final DropdownCriteria dropdownCriteria) {
        final Table pipelineTable = getTable();
        final Table pipelineStageTable = getPipelineStageTable();
        final SelectQuery selectQuery = SelectQuery.get(pipelineStageTable);

        selectQuery.addJoinClause(new JoinClause(
                pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID), pipelineTable.getPKColumn())
        );
        selectQuery.addSelectClause(List.of(
                pipelineTable.getColumn(MDOPIPELINE.NAME), pipelineTable.getColumn(MDOPIPELINE.ORDER),
                pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME), pipelineStageTable.getColumn(MDOPIPELINESTAGE.ORDER),
                pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID))
        );
        selectQuery.setLimitClause(dropdownLimit.getLimitClause());

        if (Objects.nonNull(dropdownCriteria)) {
            selectQuery.setWhereClause(new WhereClause(
                    dropdownCriteria.build(pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME)))
            );
        }
        selectQuery.addOrderByClause(List.of(
                new OrderByClause(pipelineTable.getColumn(MDOPIPELINE.ORDER)),
                new OrderByClause(pipelineStageTable.getColumn(MDOPIPELINESTAGE.ORDER))
        ));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> stageRows = dataContainer.getRows(pipelineStageTable);

        if (Objects.isNull(stageRows)) {
            return Collections.emptyList();
        }
        final Map<Long, Pipeline> pipelines = new LinkedHashMap<>();

        stageRows.forEach(stageRow -> {
            final Long pipelineId = stageRow.get(MDOPIPELINESTAGE.PIPELINE_ID);
            final Row pipelineRow = dataContainer.getRow(pipelineTable, pipelineId);

            if (Objects.nonNull(pipelineRow)) {
                pipelines.computeIfAbsent(pipelineId,
                        key -> convertRowToModel(pipelineRow, dataContainer, false));
            }
        });

        return pipelines.values();
    }

    /**
     * <p>
     * Marks the pipeline as selected
     * </p>
     *
     * @param pipelineId Represents the pipeline id
     * @param pipelines  Represents the collection of pipelines
     */
    private void markPipelineAsSelected(final Long pipelineId, final Collection<Pipeline> pipelines) {
        if (Objects.nonNull(pipelineId)) {
            for (final Pipeline pipeline : pipelines) {

                if (Objects.equals(pipeline.id(), pipelineId)) {
                    pipeline.setSelected(true);
                    break;
                }
            }
        }
    }

    /**
     * <p>
     * Gets the count of total pipelines present for given org id
     * </p>
     *
     * @param orgId Represents the organization id
     * @return Count of available pipelines
     */
    public Long getPipelineCount(final Long orgId) {
        return orgDataStore(orgId).get(CountQuery.get(getTable()));
    }

    /**
     * Fetches the pipeline and appropriate stages by their names
     *
     * @param orgId              Represents the organization ID
     * @param pipelineNames      Which contains the collection of pipeline names
     * @param pipelineStageNames Which contains the collection of pipeline stage names
     * @return {@link Map} that contains the pipeline mapped with name
     */
    public Collection<Pipeline> fetchPipelinesByNames(final Long orgId, final Collection<String> pipelineNames,
                                                      final Collection<String> pipelineStageNames) {
        final Table pipelineTable = getTable();
        final SelectQuery selectQuery = buildPipelineSelectQuery(pipelineTable);
        final WhereClause defaultPipelineCriteria = new WhereClause(Criteria.eq(pipelineTable.getColumn(MDOPIPELINE.IS_DEFAULT), true));

        if ((Objects.nonNull(pipelineNames) && !pipelineNames.isEmpty())) {
            final WhereClause pipelinesByNamesOrDefaultCriteria = defaultPipelineCriteria
                    .or(Criteria.in(pipelineTable.getColumn(MDOPIPELINE.NAME), pipelineNames));

            if (Objects.nonNull(pipelineStageNames) && !pipelineStageNames.isEmpty()) {
                final Table pipelineStageTable = pipelineStageManager.getTable();
                final WhereClause defaultPipelineStageCriteria = new WhereClause(Criteria.eq(pipelineStageTable.getColumn(MDOPIPELINESTAGE.IS_DEFAULT), true));
                final WhereClause stagesByNamesOrDefaultCriteria = defaultPipelineStageCriteria
                        .or(Criteria.in(pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME), pipelineStageNames));

                selectQuery.setWhereClause(pipelinesByNamesOrDefaultCriteria.and(stagesByNamesOrDefaultCriteria));
            } else {
                selectQuery.setWhereClause(pipelinesByNamesOrDefaultCriteria);
            }
        } else {
            selectQuery.setWhereClause(defaultPipelineCriteria);
        }
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.size(pipelineTable) == 0 ? null : dataContainer.getRows(pipelineTable)
                .map(pipelineRow -> convertRowToModel(pipelineRow, dataContainer, false)).toList();
    }

    /**
     * Gets the default pipeline id and stage id of an organization in a map.
     *
     * @param orgId Refers the organization id.
     * @return {@link Map} which contains default pipeline id and stage id of an organization.
     */
    public Map<String, Long> getDefaultPipelineIdAndStageId(final long orgId) {
        final Table table = getTable();
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addJoinClause(DmlUtil.getJoins(table, pipelineStageTable));
        selectQuery.addSelectClause(List.of(table.getPKColumn(), pipelineStageTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(MDOPIPELINE.IS_DEFAULT), true))
                .and(Criteria.eq(pipelineStageTable.getColumn(MDOPIPELINESTAGE.IS_DEFAULT), true)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row pipelineRow = dataContainer.getRow(table);
        final Row stageRow = dataContainer.getRow(pipelineStageTable);

        return (Objects.nonNull(pipelineRow) && Objects.nonNull(stageRow))
                ? Map.of(PipelineFields.PIPELINE_ID.getName(), pipelineRow.getPKValue(),
                PipelineFields.PIPELINE_STAGE_ID.getName(), stageRow.getPKValue()) : Map.of();
    }

    /**
     * <p>
     * Gets the default pipeline.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @return Entity of {@link Pipeline}
     */
    public Pipeline getDefaultPipeline(final Long orgId) {
        final Table pipeLineTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(pipeLineTable);

        selectQuery.addSelectClause(pipeLineTable.getColumn(MDOPIPELINE.NAME));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(pipeLineTable.getColumn(MDOPIPELINE.IS_DEFAULT), true)));

        final Row defaultPipelineRow = orgDataStore(orgId).get(selectQuery).getRow(pipeLineTable);

        if (Objects.nonNull(defaultPipelineRow)) {
            final Pipeline pipeline = new Pipeline();

            pipeline.setId(defaultPipelineRow.getPKValue());
            pipeline.setName(defaultPipelineRow.get(MDOPIPELINE.NAME));

            return pipeline;
        }

        return null;
    }

    /**
     * Gets the {@link Pipeline}
     *
     * @param orgId      Represents the organization id.
     * @param pipelineId Represents the pipeline id.
     * @return Entity of {@link Pipeline}
     */
    public Pipeline getPipeline(Long orgId, long pipelineId) {
        final Table pipelineTable = getTable();
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(pipelineTable);
        final Column pipelineTablePKColumn = pipelineTable.getPKColumn();

        selectQuery.addJoinClause(new JoinClause(pipelineTablePKColumn, pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(pipelineTablePKColumn, pipelineId)));
        selectQuery.addOrderByClause(new OrderByClause(pipelineStageTable.getColumn(MDOPIPELINESTAGE.ORDER)));
        selectQuery.addSelectClause(List.of(pipelineTable.getColumn(MDOPIPELINE.ORG_ID), pipelineTable.getColumn(MDOPIPELINE.NAME)));
        selectQuery.addSelectClause(pipelineStageTable);

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(pipelineTable);

        if (Objects.isNull(row)) {
            throw ErrorCode.get(DealErrorCodes.PIPELINE_NOT_FOUND, "i18n.pipeline.not.found");
        } else {
            return convertRowToModel(row, dataContainer, false);
        }
    }

    /**
     * <p>
     * Updates the order of pipelines
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param pipelineIds Represents the collection of pipeline ids
     */
    public void updatePipelinesOrder(final Long orgId, final Collection<Long> pipelineIds) {
        orderManager.update(orgId, new Counter(), pipelineIds, getTable());
    }

    /**
     * <p>
     * Gets the pipeline by user id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return The {@link Pipeline} details
     */
    public Pipeline getUserPipeline(final Long orgId, final Long userId) {
        final Table pipelineUserTable = getUserPipelineTable();
        final Table pipelineTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(pipelineUserTable);

        selectQuery.addJoinClause(new JoinClause(pipelineUserTable.getColumn(OMDOUSERPIPELINE.PIPELINE_ID), pipelineTable.getPKColumn()));
        selectQuery.addSelectClause(pipelineTable.getColumn(MDOPIPELINE.NAME));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(pipelineUserTable.getColumn(OMDOUSERPIPELINE.USER_ID), userId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(pipelineTable);

        return Objects.isNull(row) ? null : convertRowToModel(row, dataContainer, true);
    }

    /**
     * <p>
     * Fetches the pipeline id for the given organization id and user id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return The pipeline id
     */
    public Long getPipelineId(final Long orgId, final Long userId) {
        final Table pipelineUserTable = getUserPipelineTable();
        final SelectQuery selectQuery = SelectQuery.get(pipelineUserTable);

        selectQuery.addSelectClause(pipelineUserTable.getColumn(OMDOUSERPIPELINE.PIPELINE_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(pipelineUserTable.getColumn(OMDOUSERPIPELINE.USER_ID), userId)));
        final Row row = orgDataStore(orgId).get(selectQuery).getRow(pipelineUserTable);

        return Objects.isNull(row) ? null : row.get(OMDOUSERPIPELINE.PIPELINE_ID);
    }

    /**
     * <p>
     * Loads the preferred pipeline for a specific user in the given organization
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents the user id
     * @param pipelineId Represents the pipeline id
     */
    public void loadUserPreferredPipeline(final long orgId, final long userId, final long pipelineId) {
        final DataContainer targetContainer = DataContainer.create();

        loadUserPreferredPipelineIntoContainer(orgId, userId, pipelineId, targetContainer);
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads the preferred pipeline for a specific user in the given organization into the provided container
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param pipelineId      Represents the pipeline id
     * @param targetContainer The container to load the preferred pipeline details
     */
    public void loadUserPreferredPipelineIntoContainer(final long orgId, final long userId,
                                                       final long pipelineId, final DataContainer targetContainer) {
        final Table userPreferenceTable = Table.get(OMDOUSERPREFERENCE.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(userPreferenceTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(userPreferenceTable.getColumn(OMDOUSERPREFERENCE.USER_ID), userId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.isEmpty() ? new Row(userPreferenceTable) : dataContainer.getRow(userPreferenceTable);

        if (row.isNewRow()) {
            row.setOrgId(orgId);
        }
        row.set(OMDOUSERPREFERENCE.USER_ID, userId);
        row.set(OMDOUSERPREFERENCE.PIPELINE_ID, pipelineId);

        targetContainer.updateRow(row);
    }

    /**
     * <p>
     * Loads the preferred pipeline for a specific user in the given organization
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents the user id
     * @param pipelineId Represents the pipeline id
     */
    public void loadUserForecastPreference(final long orgId, final long userId, final Long pipelineId) {
        final Table userForecastPreferenceTable = Table.get(OMDOUSERFORECASTPREFERENCE.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(userForecastPreferenceTable);
        final WhereClause whereClause = new WhereClause(Criteria.eq(userForecastPreferenceTable.getColumn(OMDOUSERFORECASTPREFERENCE.USER_ID), userId));

        selectQuery.setWhereClause(whereClause);
        final Row preferenceRow = orgDataStore(orgId).get(selectQuery).getOrInitRow(userForecastPreferenceTable, whereClause);
        final DataContainer targetContainer = DataContainer.create();

        if (preferenceRow.isNewRow()) {
            preferenceRow.setOrgId(orgId);
        }
        preferenceRow.set(OMDOUSERFORECASTPREFERENCE.USER_ID, userId);
        preferenceRow.set(OMDOUSERFORECASTPREFERENCE.PIPELINE_ID, pipelineId);
        targetContainer.updateRow(preferenceRow);

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Gets the pipeline id and their respective {@link Collection} of {@link PipelineStage} mapped for the given stage names and organization id
     * </p>
     *
     * @param orgId          Represents the organization id
     * @param pipelineStages Represents the {@link Collection} of pipeline stage names
     * @return {@link Map} of pipeline id and their {@link Collection} of {@link PipelineStage}
     */
    public Map<Long, Collection<PipelineStage>> getPipelines(final long orgId, final Collection<String> pipelineStages) {
        final Table pipelineTable = getTable();
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(pipelineTable);
        final Column pipelinePkColumn = pipelineTable.getPKColumn();
        final Column pipelineStageNameColumn = pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME);
        final Column pipelineStagePipelineIdColumn = pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID);

        selectQuery.addSelectClause(List.of(pipelinePkColumn, pipelineStageNameColumn, pipelineStagePipelineIdColumn));
        selectQuery.addJoinClause(new JoinClause(pipelinePkColumn, pipelineStagePipelineIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(pipelineStageNameColumn, pipelineStages)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Map<Long, Collection<PipelineStage>> pipelines = new HashMap<>();

        dataContainer.getRows(pipelineTable).forEach(pipelineRow -> {
            final Long pipelineId = pipelineRow.getPKValue();
            final Collection<PipelineStage> stages = dataContainer.getRows(pipelineStageTable,
                    new WhereClause(Criteria.eq(pipelineStagePipelineIdColumn, pipelineId))).map(pipelineStageRow -> {
                final PipelineStage pipelineStage = new PipelineStage();

                pipelineStage.setId(pipelineStageRow.getPKValue());
                pipelineStage.setName(pipelineStageRow.get(MDOPIPELINESTAGE.NAME));

                return pipelineStage;
            }).collect(Collectors.toUnmodifiableSet());

            pipelines.put(pipelineId, stages);
        });

        return pipelines;
    }

    /**
     * <p>
     * Publishes pipeline details to Deal Index.
     * </p>
     *
     * @param orgId    Represents an organization's id.
     * @param pipeline Represents {@link Pipeline}.
     */
    private void publishPipelineToIndex(final Long orgId, final Pipeline pipeline) {
        final Map<Long, Long> dealAndPipelineStageIdsMap = DealManager.getInstance().getDealAndPipelineStageIds(orgId, pipeline.id());

        for (final Map.Entry<Long, Long> entry : dealAndPipelineStageIdsMap.entrySet()) {

            for (final PipelineStage pipelineStage : pipeline.getPipelineStages()) {

                if (Objects.equals(entry.getValue(), pipelineStage.id())) {
                    final Deal deal = new Deal();

                    deal.setOrgId(orgId);
                    deal.setId(entry.getKey());
                    deal.setPipeline(pipeline);
                    deal.setPipelineStage(pipelineStage);
                    indexDataPublisher.publishIndexData(orgId, List.of(deal), Action.PARTIAL_UPDATE);
                }
            }
        }
    }

    /**
     * <p>
     * Retrieves the total pipeline count
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param dropdownCriteria Represent the dropdown criteria
     * @return The count of entities
     */
    public long getTotalCount(final Long orgId, final DropdownCriteria dropdownCriteria) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        if (Objects.nonNull(dropdownCriteria)) {
            countQuery.setWhereClause(new WhereClause(dropdownCriteria.build(table.getColumn(MDOPIPELINE.NAME))));
        }
        return orgDataStore(orgId).get(countQuery);
    }
}


