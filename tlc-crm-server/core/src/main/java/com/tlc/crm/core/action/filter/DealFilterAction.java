package com.tlc.crm.core.action.filter;

import com.tlc.commons.util.TypeChecker;

import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.features.api.dataimport.manager.ImportManager;
import com.tlc.crm.core.features.api.dataimport.model.ImportInfo;
import com.tlc.crm.core.sql.resource.MACDEALACTIVITY;
import com.tlc.crm.core.sql.resource.MDOCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MDOPARTICIPANT;
import com.tlc.crm.core.sql.resource.MDOPIPELINE;
import com.tlc.crm.core.sql.resource.MDOPIPELINESTAGE;
import com.tlc.crm.core.sql.resource.MDOPRODUCTDETAIL;
import com.tlc.crm.core.sql.resource.MDOSTAGEPROGRESS;
import com.tlc.crm.core.sql.resource.MTDEALTAG;
import com.tlc.crm.core.sql.resource.OMDOFOLLOWERS;
import com.tlc.crm.core.sql.resource.OMDOPRODUCTDETAILSHOLDER;
import com.tlc.crm.core.sql.resource.OMITDEALHISTORY;
import com.tlc.crm.core.sql.resource.OMITHISTORY;
import com.tlc.crm.core.sql.resource.OMITIMPORT;

import com.tlc.crm.entity.filter.api.models.AdditionalOption;
import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.CriteriaValue;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.date.DateIntervals;
import com.tlc.crm.entity.filter.fields.FilterOptions;
import com.tlc.crm.entity.list.filter.Filter;
import com.tlc.crm.entity.list.filter.FilterAction;
import com.tlc.crm.entity.list.filter.appender.CriteriaAppenderHolder;
import com.tlc.crm.entity.list.filter.appender.model.CountCriteriaAppenderRequisites;
import com.tlc.crm.entity.list.filter.appender.model.NullableFieldCriteriaAppenderRequisites;
import com.tlc.crm.entity.list.filter.loader.JoinsLoader;
import com.tlc.crm.entity.list.filter.models.FilterCriteriaDetails;
import com.tlc.crm.entity.list.filter.models.JoinPreference;
import com.tlc.crm.entity.list.filter.models.JoinPriorityDetails;
import com.tlc.crm.entity.list.filter.source.FilterSourceType;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.models.FieldInfo;

import com.tlc.crm.fields.sql.resource.OMETMONETARY;

import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.Interval;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.HavingClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the action which can be perform deal related filter actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Filter(source = FilterSourceType.DEAL, provider = Provider.DEAL_LIST)
public class DealFilterAction extends FilterAction {

    private static final long MILLI_SECONDS_FOR_ONE_DAY = 86400000;

    private final DealManager dealManager;
    private final PipelineManager pipelineManager;
    private final PipelineStageManager pipelineStageManager;
    private final ProductManager productManager;
    private final ImportManager importManager;

    public DealFilterAction() {
        dealManager = DealManager.getInstance();
        pipelineManager = PipelineManager.getInstance();
        pipelineStageManager = PipelineStageManager.getInstance();
        productManager = ProductManager.getInstance();
        importManager = ImportManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldInfo              {@link FieldInfo} that holds the field information.
     * @param filterCriteria         holds the details of criteria.
     * @param criteriaAppenderHolder Holds the post-criteria appender.
     * @return {@link WhereClause} holds the details about retrieval criteria.
     */
    @Override
    public WhereClause buildWhereClause(final FieldInfo fieldInfo, final FilterCriteria filterCriteria,
                                        final CriteriaAppenderHolder criteriaAppenderHolder) {
        final CriteriaField criteriaField = filterCriteria.getCriteriaField();
        final FilterOptions filterOption = FilterOptions.get(criteriaField.getAliasName());

        if (Objects.nonNull(fieldInfo) && fieldInfo.isPersistent() || Objects.isNull(filterOption)) {
            return super.buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
        }
        final String fieldAlias = filterOption.name();
        final Collection<CriteriaValue> criteriaValues = filterCriteria.getCriteriaValues();
        final CriteriaValue criteriaValue = Objects.isNull(criteriaValues) || criteriaValues.isEmpty() ? null
                : criteriaValues.stream().findAny().get();
        final Object value = Objects.isNull(criteriaValue) ? null : criteriaValue.getValue();
        final Long valueEntityId = Objects.isNull(criteriaValue) ? null : criteriaValue.getValueEntityId();
        final Comparator comparator = filterCriteria.getComparator();
        final Operator operator = Operator.getOperator(comparator.getAliasName());
        final Table table = getTable();
        Column column = null;

        final WhereClause filter = switch (filterOption) {
            case DEAL_ID, DEAL -> {
                column = table.getPKColumn();

                yield new WhereClause(new Criteria(column, valueEntityId, operator));
            }
            case CREATED_AT -> {
                column = table.getColumn(MDODEAL.CREATED_TIME);

                yield buildCriteriaForDateField(operator, criteriaValue, column);
            }
            case STAGE_PROBABILITY -> {
                column = pipelineStageManager.getTable().getColumn(MDOPIPELINESTAGE.PROBABILITY);

                yield new WhereClause(new Criteria(column, Objects.isNull(value) ? null : Double.parseDouble(value.toString()), operator));
            }
            case UPDATED_AT -> {
                column = table.getColumn(MDODEAL.MODIFIED_TIME);

                yield isRottenTimeCriteria(valueEntityId) ? getRottenDaysCriteria(operator, column, true)
                        : super.buildCriteriaForDateField(operator, criteriaValue, column);
            }
            case EXPECTED_CLOSE_DATE -> {
                column = table.getColumn(MDODEAL.EXPECTED_CLOSE_DATE);

                yield buildCriteriaForDateField(operator, criteriaValue, column);
            }
            case STATUS -> {
                column = table.getColumn(MDODEAL.STATE);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case PIPELINE -> {
                column = table.getColumn(MDODEAL.PIPELINE_ID);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case PIPELINE_STAGE -> {
                column = table.getColumn(MDODEAL.PIPELINE_STAGE_ID);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case PRIMARY_CONTACT -> {
                column = table.getColumn(MDODEAL.CONTACT_ID);

                yield new WhereClause(new Criteria(column, valueEntityId, operator));
            }
            case RELATED_CONTACTS -> {
                final Table participantTable = getParticipantTable();

                yield new WhereClause(Criteria.eq(participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID), valueEntityId))
                        .and(Criteria.notEq(participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID), table.getColumn(MDODEAL.CONTACT_ID)));
            }
            case PRODUCTS -> {
                final Table productDetailAliasTable = getProductDetailAliasTable(fieldInfo);
                final CountCriteriaAppenderRequisites criteriaRequisites = new CountCriteriaAppenderRequisites.Builder()
                        .setCount(value)
                        .setNullableCriteriaRequired(false)
                        .setPriorityDetailHolders(List.of(new JoinPriorityDetails(JoinPreference.MAPPER_TABLE)))
                        .setRelatedSourceType(FilterSourceType.PRODUCT)
                        .setFieldInfo(fieldInfo)
                        .build(productDetailAliasTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_ID));

                setRequisitesIntoCountCriteriaAppender(criteriaRequisites, criteriaAppenderHolder);
                yield null;
            }
            case DEAL_VALUE -> {
                final Table monetaryTable = getAliasTable(monetaryManager.getTable(), fieldAlias);
                column = monetaryTable.getColumn(OMETMONETARY.VALUE);
                final Object dealValue;

                if (TypeChecker.isDecimalNumber(value)) {
                    dealValue = NumberUtils.parseDouble(value);
                } else if (TypeChecker.isNonDecimalNumber(value)) {
                    dealValue = NumberUtils.parseNumber(value);
                } else {
                    dealValue = value;
                }

                yield new WhereClause(new Criteria(column, dealValue, operator));
            }
            case SALES_OWNER -> {
                column = table.getColumn(MDODEAL.OWNER_ID);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case DEAL_CLOSED_ON -> {
                column = table.getColumn(MDODEAL.CLOSED_TIME);

                yield buildCloseTimeCriteria(valueEntityId, operator, criteriaValue).and(Criteria.in(table.getColumn(MDODEAL.STATE),
                        List.of(Deal.State.LOST.getId(), Deal.State.WON.getId())));
            }
            case DEAL_STAGE_UPDATED_AT -> {
                column = table.getColumn(MDODEAL.STAGE_MODIFIED_TIME);

                yield buildCriteriaForDateField(operator, criteriaValue, column);
            }
            case LOST_TIME -> {
                final WhereClause whereClause = buildCloseTimeCriteria(valueEntityId, operator, criteriaValue);
                column = table.getColumn(MDODEAL.STATE);

                yield Objects.isNull(whereClause) ? null : whereClause.and(Criteria.eq(column, Deal.State.LOST.getId()));
            }
            case WON_TIME -> {
                final WhereClause whereClause = buildCloseTimeCriteria(valueEntityId, operator, criteriaValue);
                column = table.getColumn(MDODEAL.STATE);

                yield Objects.isNull(whereClause) ? null : whereClause.and(Criteria.eq(column, Deal.State.WON.getId()));
            }
            case CLOSED_DEALS -> {
                final Column stateColumn = table.getColumn(MDODEAL.STATE);
                final CountCriteriaAppenderRequisites criteriaRequisites = new CountCriteriaAppenderRequisites.Builder()
                        .setCount(value)
                        .setNullableCriteriaRequired(false)
                        .setPriorityDetailHolders(List.of(new JoinPriorityDetails(JoinPreference.MAPPER_TABLE)))
                        .setRelatedSourceType(FilterSourceType.DEAL)
                        .setFieldInfo(fieldInfo)
                        .build(table.getColumn(MDODEAL.CLOSED_TIME));

                setRequisitesIntoCountCriteriaAppender(criteriaRequisites, criteriaAppenderHolder);
                yield buildDealStateCriteria(stateColumn, filterOption);
            }
            case LOST_DEALS, WON_DEALS, OPEN_DEALS -> {
                final Column stateColumn = table.getColumn(MDODEAL.STATE);
                final CountCriteriaAppenderRequisites criteriaRequisites = new CountCriteriaAppenderRequisites.Builder()
                        .setCount(value)
                        .setPriorityDetailHolders(List.of(new JoinPriorityDetails(JoinPreference.MAPPER_TABLE)))
                        .setRelatedSourceType(FilterSourceType.DEAL)
                        .setFieldInfo(fieldInfo)
                        .build(stateColumn);

                setRequisitesIntoCountCriteriaAppender(criteriaRequisites, criteriaAppenderHolder);
                yield buildDealStateCriteria(stateColumn, filterOption);
            }
            case LAST_EMAIL_RECEIVED, LAST_EMAIL_SENT, EMAIL_MESSAGE_COUNT ->
                    fetchSourceAction(FilterSourceType.EMAIL).buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
            case TAGS -> {
                final Table dealTagTable = getDealTagTable();
                final Collection<Long> valueEntityIds = Objects.isNull(criteriaValues) ? Collections.emptySet() : criteriaValues.stream()
                        .map(CriteriaValue::getValueEntityId).filter(Objects::nonNull).collect(Collectors.toSet());
                column = dealTagTable.getColumn(MTDEALTAG.DEAL_ID);

                yield buildTagFieldCriteria(dealTagTable, column, dealTagTable.getColumn(MTDEALTAG.TAG_ID), valueEntityIds, operator);
            }
            case DATE_OF_ENTERING_STAGE -> {
                final Table stageProgressTable = getStageProgressTable();
                final AdditionalOption additionalOption = criteriaField.getAdditionalOption();
                column = stageProgressTable.getColumn(MDOSTAGEPROGRESS.PIPELINE_STAGE_ID);

                yield new WhereClause(Criteria.eq(stageProgressTable.getColumn(MDOSTAGEPROGRESS.PIPELINE_ID), additionalOption.getOptionEntityId()))
                        .and(Criteria.eq(column, additionalOption.getAdditionalOption().getOptionEntityId()))
                        .and(buildCriteriaForDateField(operator, criteriaValue, stageProgressTable.getColumn(MDOSTAGEPROGRESS.START_TIME)));
            }
            case VALUE -> {
                column = getAliasTable(monetaryManager.getTable(), filterOption.name()).getColumn(OMETMONETARY.VALUE);

                yield new WhereClause(new Criteria(column, Objects.isNull(value) ? 0 : Long.parseLong(value.toString()), operator));
            }
            case UPDATED_TIME -> {
                column = table.getColumn(MDODEAL.MODIFIED_TIME);

                yield buildDateFieldCriteria((Interval<Long, Long>) value, operator, column);
            }
            case CREATED_TIME -> {
                column = table.getColumn(MDODEAL.CREATED_TIME);

                yield buildDateFieldCriteria((Interval<Long, Long>) value, operator, column);
            }
            case IMPORTED_TIME -> {
                final Table importTable = importManager.getTable();
                column = importTable.getColumn(OMITIMPORT.MODIFIED_TIME);
                final WhereClause dateFieldCriteria = buildDateFieldCriteria((Interval<Long, Long>) value, operator, column);

                yield Objects.isNull(dateFieldCriteria) ? null : new WhereClause(Criteria.eq(importTable.getColumn(OMITIMPORT.STATUS),
                        ImportInfo.Status.FINISHED.getType())).and(dateFieldCriteria);
            }
            case TERRITORY -> {
                column = table.getColumn(MDODEAL.TERRITORY_ID);

                yield buildTerritoryWhereClause(comparator.isValueNeeded(), comparator.isMultiValueAllowed(), column,
                        Objects.isNull(criteriaValue) ? null : criteriaValue.getValueEntityId(), value, operator, criteriaValues);
            }
            case CREATED_BY -> {
                column = table.getColumn(MDODEAL.CREATED_BY);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case UPDATED_BY -> {
                column = table.getColumn(MDODEAL.UPDATED_BY);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case RECENT_NOTE ->
                    fetchSourceAction(FilterSourceType.NOTE).buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
            case LAST_ACTIVITY_DATE, NEXT_ACTIVITY_DATE, TOTAL_ACTIVITIES, DONE_ACTIVITIES, UPCOMING_ACTIVITIES,
                 LAST_ACTIVITY_TYPE ->
                    fetchSourceAction(FilterSourceType.ACTIVITY).buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
            case CAMPAIGN -> null; //TODO need to implement Campaign.
            case FOLLOWER -> {
                column = getDealFollowersTable().getColumn(OMDOFOLLOWERS.FOLLOWER_ID);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case LAST_ASSIGNED_AT -> buildCriteriaForDateField(operator, criteriaValue, table.getColumn(MDODEAL.LAST_ASSIGNED_AT));
            case WEIGHTED_VALUE -> {
                final Table dealSubTable = getAliasTable(table, fieldAlias);
                final Table stageSubTable = getAliasTable(pipelineStageManager.getTable(), fieldAlias);
                final Table monetarySubTable = getAliasTable(monetaryManager.getTable(), fieldAlias);

                final SelectQuery weightedSubquery = SelectQuery.get(dealSubTable);
                final Column stageIdColumn = dealSubTable.getColumn(MDODEAL.PIPELINE_STAGE_ID);
                final Column probabilityColumn = stageSubTable.getColumn(MDOPIPELINESTAGE.PROBABILITY);
                final double weightedValue = Objects.isNull(value) ? 0 : Double.parseDouble(value.toString());

                weightedSubquery.addSelectClause(stageIdColumn);

                weightedSubquery.addJoinClause(new JoinClause(stageIdColumn, stageSubTable.getPKColumn()));
                weightedSubquery.addJoinClause(new JoinClause(dealSubTable.getColumn(MDODEAL.MONETARY_ID), monetarySubTable.getPKColumn()));

                weightedSubquery.addGroupByClause(new GroupByClause(stageIdColumn));
                weightedSubquery.addGroupByClause(new GroupByClause(probabilityColumn));
                weightedSubquery.addHavingClause(new HavingClause(new Criteria(monetarySubTable.getColumn(OMETMONETARY.VALUE).sum(), weightedValue, operator)));

                yield new WhereClause(new Criteria(table.getColumn(MDODEAL.PIPELINE_STAGE_ID), weightedSubquery, Operator.IN));
            }
            case FORECAST_CATEGORY -> {
                column = table.getColumn(MDODEAL.FORECAST_CATEGORY_ID);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            default -> super.buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
        };
        final NullableFieldCriteriaAppenderRequisites criteriaRequisites = new NullableFieldCriteriaAppenderRequisites.Builder().build(column);

        setRequisitesIntoNullableFieldCriteriaAppender(criteriaRequisites, criteriaAppenderHolder);
        return filter;
    }

    /**
     * {@inheritDoc}
     *
     * @param filterCriteriaDetails Helps to manage the joins data information.
     */
    @Override
    public void appendJoinsBasedOnField(final FilterCriteriaDetails filterCriteriaDetails) {
        final Table dealTable = getTable();
        final Column dealPkColumn = dealTable.getPKColumn();
        final FilterOptions filterOption = filterCriteriaDetails.getFilterOptions();
        final JoinsLoader joinsLoader = filterCriteriaDetails.getJoinsLoader();

        if (Objects.nonNull(filterOption)) {
            switch (filterOption) {
                case STAGE_PROBABILITY -> joinsLoader.load(dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID),
                        pipelineStageManager.getTable().getPKColumn());
                case RELATED_CONTACTS ->
                        joinsLoader.load(dealPkColumn, getParticipantTable().getColumn(MDOPARTICIPANT.DEAL_ID));
                case DATE_OF_ENTERING_STAGE -> {
                    final Table pipelineStageTable = getPipelineStageTable();

                    joinsLoader.load(dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID),
                            pipelineStageTable.getPKColumn());
                    joinsLoader.load(pipelineStageTable.getPKColumn(),
                            getStageProgressTable().getColumn(MDOSTAGEPROGRESS.PIPELINE_STAGE_ID));
                }
                case PRODUCTS -> {
                    final Table productDetailsHolderTable = getProductDetailsHolderTable();
                    final Table productDetailAliasTable = getProductDetailAliasTable(filterCriteriaDetails.getFieldInfo());

                    joinsLoader.load(dealPkColumn,
                            productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID));
                    joinsLoader.load(productDetailsHolderTable.getPKColumn(),
                            productDetailAliasTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID));
                }
                case TAGS -> joinsLoader.load(dealPkColumn, getDealTagTable().getColumn(MTDEALTAG.DEAL_ID));
                case RECENT_NOTE ->
                        fetchSourceAction(FilterSourceType.NOTE).loadJoins(dealTable, filterCriteriaDetails);
                case LAST_EMAIL_RECEIVED, LAST_EMAIL_SENT, EMAIL_MESSAGE_COUNT ->
                        fetchSourceAction(FilterSourceType.EMAIL).loadJoins(dealTable, filterCriteriaDetails);
                case LAST_ACTIVITY_DATE, NEXT_ACTIVITY_DATE, TOTAL_ACTIVITIES, DONE_ACTIVITIES, UPCOMING_ACTIVITIES,
                     LAST_ACTIVITY_TYPE -> {
                    final FilterAction activityFilterAction = fetchSourceAction(FilterSourceType.ACTIVITY);
                    final JoinPriorityDetails joinPriorityDetails = new JoinPriorityDetails(JoinPreference.MAPPER_TABLE);
                    final FieldInfo fieldInfo = filterCriteriaDetails.getFieldInfo();
                    final Table activityTable = activityFilterAction.getTable();
                    final Table activityAliasTable = Objects.isNull(fieldInfo) ? activityTable : getAliasTable(activityTable, fieldInfo.aliasName());

                    joinPriorityDetails.addMapperTable(Table.get(MACDEALACTIVITY.TABLE));
                    activityFilterAction.loadJoins(dealTable, activityAliasTable,
                            new FilterCriteriaDetails(true, filterCriteriaDetails, joinPriorityDetails));
                }
                case VALUE, DEAL_VALUE ->
                        joinsLoader.load(dealTable.getColumn(MDODEAL.MONETARY_ID), getAliasTable(monetaryManager.getTable(),
                                filterOption.name()).getPKColumn());
                case IMPORTED_TIME -> {
                    final Table dealHistoryTable = Table.get(OMITDEALHISTORY.TABLE);
                    final Table importHistoryTable = importManager.getHistoryTable();

                    joinsLoader.load(dealPkColumn, dealHistoryTable.getColumn(OMITDEALHISTORY.ENTITY_ID));
                    joinsLoader.load(dealHistoryTable.getColumn(OMITDEALHISTORY.HISTORY_ID), importHistoryTable.getPKColumn());
                    joinsLoader.load(importHistoryTable.getColumn(OMITHISTORY.IMPORT_ID), importManager.getTable().getPKColumn());
                }
                case FOLLOWER ->
                        joinsLoader.load(dealPkColumn, getDealFollowersTable().getColumn(OMDOFOLLOWERS.DEAL_ID));
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTable   Represents the table of the column.
     * @param fieldInfo    Represents the details of the field.
     * @param filterOption Represents the option of the filter.
     * @return Field {@link Column}.
     */
    @Override
    protected Column getFieldColumn(final Table fieldTable, final FieldInfo fieldInfo, final FilterOptions filterOption) {
        final Table table = getTable();

        if (Objects.isNull(filterOption)) {
            return super.getFieldColumn(fieldTable, fieldInfo, null);
        }

        return switch (filterOption) {
            case ID -> table.getPKColumn();
            case TITLE -> table.getColumn(MDODEAL.TITLE);
            case DESCRIPTION -> table.getColumn(MDODEAL.DESCRIPTION);
            default -> super.getFieldColumn(fieldTable, fieldInfo, filterOption);
        };
    }

    /**
     * {@inheritDoc}
     *
     * @return Array of {@link Table} holds the mapping tables of current source.
     */
    @Override
    protected Table[] getLocalMapperTables() {
        return new Table[]{
                getParticipantTable(), getDealCompanyTable()
        };
    }

    /**
     * {@inheritDoc}
     *
     * @return deal {@link Table}.
     */
    @Override
    public Table getTable() {
        return dealManager.getTable();
    }

    /**
     * {@inheritDoc}
     *
     * @param baseTable             Refers to the base table of the previous {@link FilterAction}, which is used to
     *                              append the joins after the already joined tables.
     * @param filterCriteriaDetails Helps to manage the joins data information.
     * @param mapperTables          Refers to the mapper tables of those who had redirected this method. It is used to make
     *                              joins for the current and previous sources if those source tables have no direct relations.
     */
    @Override
    public void loadJoins(final Table baseTable, final FilterCriteriaDetails filterCriteriaDetails, final Table... mapperTables) {
        if (baseTable.equals(productManager.getTable())) {
            final Table localTable = getTable();
            final Table productDetailAliasTable = getProductDetailAliasTable(filterCriteriaDetails.getFieldInfo());

            final Table productDetailsHolderTable = getProductDetailsHolderTable();
            final JoinsLoader joinsLoader = filterCriteriaDetails.getJoinsLoader();

            joinsLoader.load(baseTable.getPKColumn(), productDetailAliasTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_ID));
            joinsLoader.load(productDetailAliasTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID),
                    productDetailsHolderTable.getPKColumn());
            joinsLoader.load(productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID), localTable.getPKColumn());

            loadJoinsBasedOnRelatedSourceType(localTable, getLocalMapperTables(), filterCriteriaDetails);
        } else {
            super.loadJoins(baseTable, filterCriteriaDetails, mapperTables);
        }
    }

    /**
     * <p>
     * Fetches the date interval from the given “criteriaValue”, then builds the criteria for that.
     * <p>
     * (eg: equal, not equal, greater than, etc,.).
     * <p>
     * The comparators have been separated by an individual cases.
     * In these cases are create and return their own criteria. Otherwise, criteria were created by containing null values.
     * </p>
     *
     * @param operator      It is used to compare the given value with the given respective column.
     * @param criteriaValue Holds the details of the date interval or date value.
     * @param column        It is used to filter the entities as per the given value.
     * @return {@link WhereClause} holds the details of retrieval criteria.
     */
    @Override
    public WhereClause buildCriteriaForDateField(final Operator operator, final CriteriaValue criteriaValue, final Column column) {
        return Objects.nonNull(criteriaValue) && isRottenTimeCriteria(criteriaValue.getValueEntityId()) ? getRottenDaysCriteria(operator, column, false)
                : super.buildCriteriaForDateField(operator, criteriaValue, column);
    }

    /**
     * <p>
     * Build criteria based on the deal state.
     * </p>
     *
     * @param stateColumn   Refers the selected Column
     * @param filterOptions Refers the filter option
     * @return {@link WhereClause} holds the details about retrieval criteria.
     */
    private WhereClause buildDealStateCriteria(final Column stateColumn, final FilterOptions filterOptions) {

        if (Objects.equals(FilterOptions.CLOSED_DEALS, filterOptions)) {
            final Collection<Integer> dealStateIds = List.of(Deal.State.WON.getId(), Deal.State.LOST.getId());

            return new WhereClause(Criteria.in(stateColumn, dealStateIds));
        }

        final int dealStateId = switch (filterOptions) {
            case OPEN_DEALS -> Deal.State.OPEN.getId();
            case WON_DEALS -> Deal.State.WON.getId();
            default -> Deal.State.LOST.getId();
        };

        return new WhereClause(Criteria.eq(stateColumn, dealStateId));
    }

    /**
     * <p>
     * Fetches the table of the stage progress.
     * </p>
     *
     * @return Stage progress table.
     */
    private Table getStageProgressTable() {
        return Table.get(MDOSTAGEPROGRESS.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the product details holder.
     * </p>
     *
     * @return OMDOProductDetailHolder table.
     */
    private Table getProductDetailsHolderTable() {
        return Table.get(OMDOPRODUCTDETAILSHOLDER.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the product detail.
     * </p>
     *
     * @return OMDOProductDetail table.
     */
    private Table getProductDetailTable() {
        return Table.get(MDOPRODUCTDETAIL.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the deal company.
     * </p>
     *
     * @return Deal company table.
     */
    private Table getDealCompanyTable() {
        return Table.get(MDOCOMPANYFIELDS.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the deal participant.
     * </p>
     *
     * @return Deal participant table.
     */
    private Table getParticipantTable() {
        return Table.get(MDOPARTICIPANT.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the pipeline stage.
     * </p>
     *
     * @return Pipeline stage table.
     */
    private Table getPipelineStageTable() {
        return Table.get(MDOPIPELINESTAGE.TABLE);
    }

    /**
     * <p>
     * Builds the {@link WhereClause} of the close time field.
     * </p>
     *
     * @param valueEntityId Entity id of criteria value.
     * @param operator      Used to compare the given value with the given respective column.
     * @param criteriaValue Holds the details of the {@link CriteriaValue}.
     * @return Holds the retrieval criteria.
     */
    private WhereClause buildCloseTimeCriteria(final Long valueEntityId, final Operator operator,
                                               final CriteriaValue criteriaValue) {
        final Column selectedColumn = getTable().getColumn(MDODEAL.CLOSED_TIME);

        return buildCriteriaForDateField(operator, criteriaValue, selectedColumn);
    }

    /**
     * <p>
     * Checks whether the given value entity id is rotten time id or not.
     * </p>
     *
     * @param valueEntityId Entity id of criteria value.
     * @return A flag that indicates the given value entity id is rotten time id or not.
     */
    private boolean isRottenTimeCriteria(final Long valueEntityId) {
        return Objects.nonNull(valueEntityId) && Objects.equals(DateIntervals.ROTTEN_TIME.name(), dateIntervalService.get(valueEntityId).getName());
    }

    /**
     * <p>
     * Gets the rotten days criteria.
     * </p>
     *
     * @param operator used to compare the value.
     * @param column   for retrieving the data, it presents the column which is to be compared with the given value.
     * @return {@link Criteria} holds the details of retrieval criteria.
     */
    private WhereClause getRottenDaysCriteria(final Operator operator, final Column column, final boolean compareWithCurrentTime) {
        final Table dealTable = getTable();
        final Table pipelineTable = pipelineManager.getTable();
        final Column dealPKColumn = dealTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Column rottenDaysCriteriaColumn = dealTable.getColumn(MDODEAL.MODIFIED_TIME).divide(MILLI_SECONDS_FOR_ONE_DAY)
                .add(pipelineTable.getColumn(MDOPIPELINE.ROTTEN_DAYS));
        final Operator rottenTimeCriteriaOperator = getRottenTimeCriteriaOperator(operator);
        final WhereClause stateCriteria = new WhereClause(Criteria.eq(dealTable.getColumn(MDODEAL.STATE), Deal.State.OPEN.getId()));
        WhereClause whereClause;

        selectQuery.addSelectClause(dealPKColumn);
        selectQuery.addJoinClause(new JoinClause(dealTable.getColumn(MDODEAL.PIPELINE_ID), pipelineTable.getPKColumn()));

        if (compareWithCurrentTime) {
            final long currentTimeInDays = TemporalServiceProvider.get().getUtcDateTime() / MILLI_SECONDS_FOR_ONE_DAY;
            whereClause = new WhereClause(new Criteria(rottenDaysCriteriaColumn, currentTimeInDays, rottenTimeCriteriaOperator.invertOperator()));

            if (Operator.LESS_EQUAL == operator) {
                whereClause = whereClause.or(Criteria.lt(rottenDaysCriteriaColumn, currentTimeInDays));
            }
        } else {
            whereClause = new WhereClause(new Criteria(column.divide(MILLI_SECONDS_FOR_ONE_DAY), rottenDaysCriteriaColumn, rottenTimeCriteriaOperator));

            if (Operator.LESS_EQUAL == operator) {
                whereClause = whereClause.or(new Criteria(column.divide(MILLI_SECONDS_FOR_ONE_DAY), rottenDaysCriteriaColumn, Operator.GREATER_THAN));
            }
        }
        selectQuery.setWhereClause(stateCriteria.and(whereClause));
        return new WhereClause(Criteria.in(dealPKColumn, selectQuery));
    }

    /**
     * Converts the given operator into the appropriate operator for rotten time comparison.
     *
     * @param operator the original filter operator.
     * @return the converted {@link Operator} suitable for rotten time criteria evaluation.
     */
    private Operator getRottenTimeCriteriaOperator(final Operator operator) {
        return switch (operator) {
            case EQUAL -> Operator.GREATER_EQUAL;
            case NOT_EQUAL -> Operator.LESS_THAN;
            default -> operator;
        };
    }

    /**
     * <p>
     * Fetches the table of the deal tag.
     * </p>
     *
     * @return Deal tag table.
     */
    private Table getDealTagTable() {
        return Table.get(MTDEALTAG.TABLE);
    }

    /**
     * <p>
     * Gets the deal followers table
     * </p>
     *
     * @return The deal followers table
     */
    private Table getDealFollowersTable() {
        return Table.get(OMDOFOLLOWERS.TABLE);
    }

    /**
     * <p>
     * Gets the product detail alias table
     * </p>
     *
     * @return The product detail alias table
     */
    private Table getProductDetailAliasTable(final FieldInfo fieldInfo) {
        final Table productDetailTable = getProductDetailTable();

        return Objects.isNull(fieldInfo) ? productDetailTable : getAliasTable(productDetailTable, fieldInfo.aliasName());
    }
}
