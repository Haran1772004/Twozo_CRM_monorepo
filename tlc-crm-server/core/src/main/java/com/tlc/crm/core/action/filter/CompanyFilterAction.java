package com.tlc.crm.core.action.filter;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.features.api.dataimport.manager.ImportManager;
import com.tlc.crm.core.features.api.dataimport.model.ImportInfo;
import com.tlc.crm.core.sql.resource.MACCOMPANYACTIVITY;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOMCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MDOCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MTCOMPANYTAG;
import com.tlc.crm.core.sql.resource.OMCOMFOLLOWERS;
import com.tlc.crm.core.sql.resource.OMITCOMPANYHISTORY;
import com.tlc.crm.core.sql.resource.OMITHISTORY;
import com.tlc.crm.core.sql.resource.OMITIMPORT;

import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.CriteriaValue;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.fields.FilterOptions;
import com.tlc.crm.entity.list.filter.Filter;
import com.tlc.crm.entity.list.filter.FilterAction;
import com.tlc.crm.entity.list.filter.appender.CriteriaAppenderHolder;
import com.tlc.crm.entity.list.filter.appender.model.NullableFieldCriteriaAppenderRequisites;
import com.tlc.crm.entity.list.filter.loader.JoinsLoader;
import com.tlc.crm.entity.list.filter.models.FilterCriteriaDetails;
import com.tlc.crm.entity.list.filter.models.JoinPreference;
import com.tlc.crm.entity.list.filter.models.JoinPriorityDetails;
import com.tlc.crm.entity.list.filter.source.FilterSourceType;
import com.tlc.crm.entity.list.filter.source.SourceLinkGroup;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.models.FieldInfo;

import com.tlc.crm.fields.models.StoreType;

import com.tlc.l10n.temporal.interval.Interval;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the action which can be perform company related filter actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Filter(source = FilterSourceType.COMPANY, provider = Provider.COMPANY_LIST, sourceLinkGroups = SourceLinkGroup.COMPANY_PRODUCT)
public class CompanyFilterAction extends FilterAction {

    private final CompanyManager companyManager;
    private final ImportManager importManager;

    public CompanyFilterAction() {
        companyManager = CompanyManager.getInstance();
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
        final Collection<CriteriaValue> criteriaValues = filterCriteria.getCriteriaValues();
        final CriteriaValue criteriaValue = Objects.nonNull(criteriaValues) && !criteriaValues.isEmpty()
                ? criteriaValues.stream().findAny().get() : null;
        final boolean criteriaValueIsNull = Objects.isNull(criteriaValue);
        final Object value = criteriaValueIsNull ? null : criteriaValue.getValue();
        final Long valueEntityId = criteriaValueIsNull ? null : criteriaValue.getValueEntityId();
        final Comparator comparator = filterCriteria.getComparator();
        final Operator operator = Operator.getOperator(comparator.getAliasName());
        final Table table = getTable();
        Column column = null;

        final WhereClause filter = switch (filterOption) {
            case COMPANY, COMPANY_ID -> {
                column = table.getPKColumn();

                yield new WhereClause(new Criteria(column, valueEntityId, operator));
            }
            case CREATED_AT -> {
                column = table.getColumn(MCOCOMPANY.CREATED_TIME);

                yield buildCriteriaForDateField(operator, criteriaValue, column);
            }
            case UPDATED_AT -> {
                column = table.getColumn(MCOCOMPANY.MODIFIED_TIME);

                yield buildCriteriaForDateField(operator, criteriaValue, column);
            }
            case EMPLOYEE_COUNT -> {
                column = table.getColumn(MCOCOMPANY.NUMBER_OF_EMPLOYEE);
                final WhereClause whereClause;

                if (Objects.isNull(value)) {
                    whereClause = new WhereClause(new Criteria(column, comparator.isValueNeeded() ? 0 : null, operator));
                } else {
                    whereClause = new WhereClause(new Criteria(column, Long.parseLong(value.toString()), operator));
                }

                yield whereClause;
            }
            case TAGS -> {
                final Table companyTagTable = getCompanyTagTable();
                final Collection<Long> valueEntityIds = Objects.isNull(criteriaValues) ? Collections.emptySet() : criteriaValues.stream()
                        .map(CriteriaValue::getValueEntityId).filter(Objects::nonNull).collect(Collectors.toSet());
                column = companyTagTable.getColumn(MTCOMPANYTAG.COMPANY_ID);

                yield buildTagFieldCriteria(companyTagTable, column, companyTagTable.getColumn(MTCOMPANYTAG.TAG_ID), valueEntityIds, operator);
            }
            case IMPORTED_TIME -> {
                final Table importTable = importManager.getTable();
                column = importTable.getColumn(OMITIMPORT.MODIFIED_TIME);
                final WhereClause dateFieldCriteria = buildDateFieldCriteria((Interval<Long, Long>) value, operator, column);

                yield Objects.isNull(dateFieldCriteria) ? null : new WhereClause(Criteria.eq(importTable.getColumn(OMITIMPORT.STATUS),
                        ImportInfo.Status.FINISHED.getType())).and(dateFieldCriteria);
            }
            case TERRITORY -> {
                column = table.getColumn(MCOCOMPANY.TERRITORY_ID);

                yield buildTerritoryWhereClause(comparator.isValueNeeded(), comparator.isMultiValueAllowed(), column,
                        Objects.isNull(criteriaValue) ? null : criteriaValue.getValueEntityId(), value, operator, criteriaValues);
            }
            case SALES_OWNER -> {
                column = table.getColumn(MCOCOMPANY.OWNER_ID);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case CREATED_BY -> {
                column = table.getColumn(MCOCOMPANY.CREATED_BY);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case UPDATED_BY -> {
                column = table.getColumn(MCOCOMPANY.UPDATED_BY);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case RECENT_NOTE ->
                    fetchSourceAction(FilterSourceType.NOTE).buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
            case OPEN_DEALS, WON_DEALS, LOST_DEALS, CLOSED_DEALS ->
                    fetchSourceAction(FilterSourceType.DEAL).buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
            case LAST_EMAIL_RECEIVED, LAST_EMAIL_SENT, EMAIL_MESSAGE_COUNT ->
                    fetchSourceAction(FilterSourceType.EMAIL).buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
            case LAST_ACTIVITY_DATE, NEXT_ACTIVITY_DATE, TOTAL_ACTIVITIES, DONE_ACTIVITIES, UPCOMING_ACTIVITIES,
                 LAST_ACTIVITY_TYPE ->
                    fetchSourceAction(FilterSourceType.ACTIVITY).buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
            case FOLLOWER -> {
                column = getCompanyFollowersTable().getColumn(OMCOMFOLLOWERS.FOLLOWER_ID);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case LAST_ASSIGNED_AT -> buildCriteriaForDateField(operator, criteriaValue, table.getColumn(MCOCOMPANY.LAST_ASSIGNED_AT));

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
        final Table companyTable = getTable();
        final Column companyPkColumn = companyTable.getPKColumn();
        final FilterOptions filterOption = filterCriteriaDetails.getFilterOptions();
        final JoinsLoader joinsLoader = filterCriteriaDetails.getJoinsLoader();

        if (Objects.nonNull(filterOption)) {
            switch (filterOption) {
                case RECENT_NOTE ->
                        fetchSourceAction(FilterSourceType.NOTE).loadJoins(companyTable, filterCriteriaDetails);
                case OPEN_DEALS, WON_DEALS, LOST_DEALS, CLOSED_DEALS ->
                        fetchSourceAction(FilterSourceType.DEAL).loadJoins(companyTable, filterCriteriaDetails);
                case LAST_EMAIL_RECEIVED, LAST_EMAIL_SENT, EMAIL_MESSAGE_COUNT ->
                        fetchSourceAction(FilterSourceType.EMAIL).loadJoins(companyTable, filterCriteriaDetails);
                case LAST_ACTIVITY_DATE, NEXT_ACTIVITY_DATE, TOTAL_ACTIVITIES, DONE_ACTIVITIES, UPCOMING_ACTIVITIES,
                     LAST_ACTIVITY_TYPE -> {
                    final FilterAction activityFilterAction = fetchSourceAction(FilterSourceType.ACTIVITY);
                    final JoinPriorityDetails joinPriorityDetails = new JoinPriorityDetails(JoinPreference.MAPPER_TABLE);
                    final FieldInfo fieldInfo = filterCriteriaDetails.getFieldInfo();
                    final Table activityTable = activityFilterAction.getTable();
                    final Table activityAliasTable = Objects.isNull(fieldInfo) ? activityTable : getAliasTable(activityTable, fieldInfo.aliasName());

                    joinPriorityDetails.addMapperTable(Table.get(MACCOMPANYACTIVITY.TABLE));
                    activityFilterAction.loadJoins(companyTable, activityAliasTable,
                            new FilterCriteriaDetails(true, filterCriteriaDetails, joinPriorityDetails));
                }
                case TAGS -> joinsLoader.load(companyPkColumn, getCompanyTagTable().getColumn(MTCOMPANYTAG.COMPANY_ID));
                case DEAL ->
                        joinsLoader.load(companyPkColumn, getDealCompanyTable().getColumn(MDOCOMPANYFIELDS.COMPANY_ID));
                case IMPORTED_TIME -> {
                    final Table companyHistoryTable = Table.get(OMITCOMPANYHISTORY.TABLE);
                    final Table importHistoryTable = importManager.getHistoryTable();

                    joinsLoader.load(companyPkColumn, companyHistoryTable.getColumn(OMITCOMPANYHISTORY.ENTITY_ID));
                    joinsLoader.load(companyHistoryTable.getColumn(OMITCOMPANYHISTORY.HISTORY_ID), importHistoryTable.getPKColumn());
                    joinsLoader.load(importHistoryTable.getColumn(OMITHISTORY.IMPORT_ID), importManager.getTable().getPKColumn());
                }
                case FOLLOWER -> {
                    joinsLoader.load(companyPkColumn, getCompanyFollowersTable().getColumn(OMCOMFOLLOWERS.COMPANY_ID));
                }
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
            case NAME -> table.getColumn(MCOCOMPANY.NAME);
            case ID -> table.getPKColumn();
            case CREATED_AT -> table.getColumn(MCOCOMPANY.CREATED_TIME);
            case WEBSITE -> table.getColumn(MCOCOMPANY.WEBSITE);
            case FACEBOOK -> table.getColumn(MCOCOMPANY.FACEBOOK);
            case TWITTER -> table.getColumn(MCOCOMPANY.TWITTER);
            case LINKEDIN -> table.getColumn(MCOCOMPANY.LINKEDIN);
            case SIC_CODE -> table.getColumn(MCOCOMPANY.SIC_CODE);
            case REMARKS -> table.getColumn(MCOCOMPANY.REMARKS);
            case UPDATED_AT -> table.getColumn(MCOCOMPANY.MODIFIED_TIME);
            case LAST_ASSIGNED_AT -> table.getColumn(MCOCOMPANY.LAST_ASSIGNED_AT);
            case DESCRIPTION -> table.getColumn(MCOCOMPANY.DESCRIPTION);
            default -> super.getFieldColumn(fieldTable, fieldInfo, filterOption);
        };
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTable     Represents the field table
     * @param storeType      Represents the {@link StoreType}
     * @param fieldFkColumns Map containing key as Table and value fk columns
     * @return The Remote column
     */
    @Override
    protected Column getRemoteColumn(final Table fieldTable, final StoreType storeType, final Map<Table, Column> fieldFkColumns) {
        return switch (storeType) {
            case COMPANY, COMPANY_ONBOARD -> fieldTable.getColumn(MCOMCOMPANYFIELDS.ENTITY_ID);
            default -> super.getRemoteColumn(fieldTable, storeType, fieldFkColumns);
        };
    }

    /**
     * {@inheritDoc}
     *
     * @return Company {@link Table}.
     */
    @Override
    public Table getTable() {
        return companyManager.getTable();
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
     * Fetches the table of the company tag.
     * </p>
     *
     * @return Company tag table.
     */
    private Table getCompanyTagTable() {
        return Table.get(MTCOMPANYTAG.TABLE);
    }

    /**
     * <p>
     * Fetches the company followers table
     * </p>
     *
     * @return The company followers table
     */
    private Table getCompanyFollowersTable() {
        return Table.get(OMCOMFOLLOWERS.TABLE);
    }
}