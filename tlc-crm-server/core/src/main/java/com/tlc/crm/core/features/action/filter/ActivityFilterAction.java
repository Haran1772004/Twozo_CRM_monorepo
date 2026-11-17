package com.tlc.crm.core.features.action.filter;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;

import com.tlc.crm.core.sql.resource.MACCOMPANYACTIVITY;
import com.tlc.crm.core.sql.resource.MACCONTACTACTIVITY;
import com.tlc.crm.core.sql.resource.MACDEALACTIVITY;

import com.tlc.crm.entity.features.api.activity.manager.ActivityManager;
import com.tlc.crm.entity.features.api.activity.model.Activity;
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
import com.tlc.crm.entity.list.filter.appender.model.DynamicFieldEmptyValueCriteriaAppenderRequisites;
import com.tlc.crm.entity.list.filter.appender.model.MinMaxValueCriteriaAppenderRequisites;
import com.tlc.crm.entity.list.filter.appender.model.NullableFieldCriteriaAppenderRequisites;
import com.tlc.crm.entity.list.filter.models.FilterCriteriaDetails;
import com.tlc.crm.entity.list.filter.models.JoinPreference;
import com.tlc.crm.entity.list.filter.models.JoinPriorityDetails;
import com.tlc.crm.entity.list.filter.source.FilterSourceType;
import com.tlc.crm.entity.list.filter.source.SourceLinkGroup;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.sql.resource.OMACACTIVITY;
import com.tlc.crm.entity.sql.resource.OMACACTIVITYCOLLABORATOR;

import com.tlc.crm.security.iam.LoggedInUserInfo;

import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.Interval;
import com.tlc.l10n.temporal.model.TimezoneInfo;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.time.Instant;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the action which can be perform activity related filter actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Filter(source = FilterSourceType.ACTIVITY, provider = Provider.ACTIVITY_LIST, sourceLinkGroups = SourceLinkGroup.ACTIVITY_PRODUCT)
public class ActivityFilterAction extends FilterAction {

    private static final String START = "start";
    private static final String END = "end";

    private final ActivityManager activityManager;

    public ActivityFilterAction() {
        activityManager = ActivityManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return Array of {@link Table} holds the mapping tables of current source.
     */
    @Override
    protected Table[] getLocalMapperTables() {
        return new Table[]{
                Table.get(MACCOMPANYACTIVITY.TABLE), Table.get(MACCONTACTACTIVITY.TABLE), Table.get(MACDEALACTIVITY.TABLE)
        };
    }

    /**
     * {@inheritDoc}
     *
     * @return activity {@link Table}.
     */
    @Override
    public Table getTable() {
        return activityManager.getTable();
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
        final Comparator comparator = filterCriteria.getComparator();
        final Collection<CriteriaValue> criteriaValues = filterCriteria.getCriteriaValues();
        final CriteriaValue criteriaValue = Objects.isNull(criteriaValues) || criteriaValues.isEmpty() ? null :
                criteriaValues.stream().findAny().get();
        final Object value = Objects.isNull(criteriaValue) ? null : criteriaValue.getValue();
        final Long valueEntityId = Objects.isNull(criteriaValue) ? null : criteriaValue.getValueEntityId();
        final Operator operator = Operator.getOperator(comparator.getAliasName());
        final FilterOptions filterOptions = FilterOptions.get(criteriaField.getAliasName());
        final Table table = getTable();
        Column column = null;

        if (Objects.isNull(filterOptions)) {
            return super.buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
        }
        final WhereClause filter = switch (filterOptions) {
            case ACTIVITY -> {
                column = Table.get(OMACACTIVITY.TABLE).getPKColumn();

                yield new WhereClause(new Criteria(column, valueEntityId, operator));
            }
            case LAST_ACTIVITY_TYPE -> {
                final Table activityTable = getAliasTable(table, fieldInfo.aliasName());
                column = activityTable.getColumn(OMACACTIVITY.START_TIME);
                final long currentTime = Instant.now().toEpochMilli();
                final WhereClause typeCriteria = buildOwnerAndDropdownFieldCriteria(activityTable.getColumn(OMACACTIVITY.TYPE_ID), criteriaValues, comparator);
                final WhereClause lastActivityCriteria = new WhereClause(Criteria.lt(column, currentTime))
                        .and(new WhereClause(Criteria.gt(activityTable.getColumn(OMACACTIVITY.END_TIME), currentTime))
                                .or(Criteria.eq(activityTable.getColumn(OMACACTIVITY.IS_DONE), true)));

                if (comparator.isValueNeeded() || Operator.NOT_EQUAL == operator) {
                    final MinMaxValueCriteriaAppenderRequisites maxValueCriteriaRequisites = new MinMaxValueCriteriaAppenderRequisites.Builder()
                            .setFieldInfo(fieldInfo)
                            .setUseAliasTable(true)
                            .setCriteria(lastActivityCriteria)
                            .setRelatedSourceType(FilterSourceType.ACTIVITY)
                            .setJoinPriorityDetails(new JoinPriorityDetails(JoinPreference.MAPPER_TABLE))
                            .build(column);
                    final DynamicFieldEmptyValueCriteriaAppenderRequisites dynamicFieldEmptyValueCriteriaRequisites = new DynamicFieldEmptyValueCriteriaAppenderRequisites.Builder()
                            .setFieldInfo(fieldInfo)
                            .setPositiveCriteria(lastActivityCriteria)
                            .setRelatedSourceType(FilterSourceType.ACTIVITY)
                            .build(column);

                    setRequisitesIntoEmptyValueCriteriaAppender(dynamicFieldEmptyValueCriteriaRequisites, criteriaAppenderHolder);
                    setRequisitesIntoMaxValueCriteriaAppender(maxValueCriteriaRequisites, criteriaAppenderHolder);
                    yield typeCriteria;
                } else {
                    final WhereClause nextActivityCriteria = new WhereClause(Criteria.gt(column, currentTime));
                    final DynamicFieldEmptyValueCriteriaAppenderRequisites criteriaRequisites = new DynamicFieldEmptyValueCriteriaAppenderRequisites.Builder()
                            .setFieldInfo(fieldInfo)
                            .setPositiveCriteria(nextActivityCriteria)
                            .setNegativeCriteria(lastActivityCriteria)
                            .setRelatedSourceType(FilterSourceType.ACTIVITY)
                            .build(column);

                    setRequisitesIntoEmptyValueCriteriaAppender(criteriaRequisites, criteriaAppenderHolder);
                    yield typeCriteria;
                }
            }
            case LAST_ACTIVITY_DATE -> {
                final Table activityTable = Objects.isNull(fieldInfo) ? table : getAliasTable(table, fieldInfo.aliasName());
                column = activityTable.getColumn(OMACACTIVITY.START_TIME);
                final long currentTime = Instant.now().toEpochMilli();
                final FilterAction parentFilterAction = fetchParentFilterAction(fieldInfo);
                final WhereClause dateCriteria = parentFilterAction.buildCriteriaForDateField(operator, criteriaValue, column);
                final WhereClause lastActivityCriteria = new WhereClause(Criteria.lt(column, currentTime))
                        .and(new WhereClause(Criteria.gt(activityTable.getColumn(OMACACTIVITY.END_TIME), currentTime))
                                .or(Criteria.eq(activityTable.getColumn(OMACACTIVITY.IS_DONE), true)));

                if (comparator.isValueNeeded() || Operator.NOT_EQUAL == operator) {
                    final MinMaxValueCriteriaAppenderRequisites maxValueCriteriaRequisites = new MinMaxValueCriteriaAppenderRequisites.Builder()
                            .setFieldInfo(fieldInfo)
                            .setUseAliasTable(true)
                            .setCriteria(lastActivityCriteria)
                            .setRelatedSourceType(FilterSourceType.ACTIVITY)
                            .setJoinPriorityDetails(new JoinPriorityDetails(JoinPreference.MAPPER_TABLE))
                            .build(column);
                    final DynamicFieldEmptyValueCriteriaAppenderRequisites dynamicFieldEmptyValueCriteriaRequisites = new DynamicFieldEmptyValueCriteriaAppenderRequisites.Builder()
                            .setFieldInfo(fieldInfo)
                            .setPositiveCriteria(lastActivityCriteria)
                            .setRelatedSourceType(FilterSourceType.ACTIVITY)
                            .build(column);

                    setRequisitesIntoEmptyValueCriteriaAppender(dynamicFieldEmptyValueCriteriaRequisites, criteriaAppenderHolder);
                    setRequisitesIntoMaxValueCriteriaAppender(maxValueCriteriaRequisites, criteriaAppenderHolder);
                    yield dateCriteria;
                } else {
                    final WhereClause nextActivityCriteria = new WhereClause(Criteria.gt(column, currentTime));
                    final DynamicFieldEmptyValueCriteriaAppenderRequisites criteriaRequisites = new DynamicFieldEmptyValueCriteriaAppenderRequisites.Builder()
                            .setFieldInfo(fieldInfo)
                            .setPositiveCriteria(nextActivityCriteria)
                            .setNegativeCriteria(lastActivityCriteria)
                            .setRelatedSourceType(FilterSourceType.ACTIVITY)
                            .build(column);

                    setRequisitesIntoEmptyValueCriteriaAppender(criteriaRequisites, criteriaAppenderHolder);
                    yield dateCriteria;
                }
            }
            case NEXT_ACTIVITY_DATE -> {
                final Table activityTable = Objects.isNull(fieldInfo) ? table : getAliasTable(table, fieldInfo.aliasName());
                column = activityTable.getColumn(OMACACTIVITY.START_TIME);
                final long currentTime = Instant.now().toEpochMilli();
                final FilterAction parentFilterAction = fetchParentFilterAction(fieldInfo);
                final WhereClause dateCriteria = parentFilterAction.buildCriteriaForDateField(operator, criteriaValue, column);
                final WhereClause nextActivityCriteria = new WhereClause(Criteria.gt(column, currentTime))
                        .and(Criteria.eq(activityTable.getColumn(OMACACTIVITY.IS_DONE), false));

                if (comparator.isValueNeeded() || Operator.NOT_EQUAL == operator) {
                    final MinMaxValueCriteriaAppenderRequisites minValueCriteriaRequisites = new MinMaxValueCriteriaAppenderRequisites.Builder()
                            .setFieldInfo(fieldInfo)
                            .setUseAliasTable(true)
                            .setCriteria(nextActivityCriteria)
                            .setRelatedSourceType(FilterSourceType.ACTIVITY)
                            .setJoinPriorityDetails(new JoinPriorityDetails(JoinPreference.MAPPER_TABLE))
                            .build(column);
                    final DynamicFieldEmptyValueCriteriaAppenderRequisites dynamicFieldEmptyValueCriteriaRequisites = new DynamicFieldEmptyValueCriteriaAppenderRequisites.Builder()
                            .setFieldInfo(fieldInfo)
                            .setPositiveCriteria(nextActivityCriteria)
                            .setRelatedSourceType(FilterSourceType.ACTIVITY)
                            .build(column);

                    setRequisitesIntoEmptyValueCriteriaAppender(dynamicFieldEmptyValueCriteriaRequisites, criteriaAppenderHolder);
                    setRequisitesIntoMinValueCriteriaAppender(minValueCriteriaRequisites, criteriaAppenderHolder);
                    yield dateCriteria;
                } else {
                    final WhereClause lastActivityCriteria = new WhereClause(Criteria.lt(column, currentTime));
                    final DynamicFieldEmptyValueCriteriaAppenderRequisites criteriaRequisites = new DynamicFieldEmptyValueCriteriaAppenderRequisites.Builder()
                            .setFieldInfo(fieldInfo)
                            .setPositiveCriteria(lastActivityCriteria)
                            .setNegativeCriteria(nextActivityCriteria)
                            .setRelatedSourceType(FilterSourceType.ACTIVITY)
                            .build(column);

                    setRequisitesIntoEmptyValueCriteriaAppender(criteriaRequisites, criteriaAppenderHolder);
                    yield dateCriteria;
                }
            }
            case MARK_AS_DONE_TIME -> {
                column = table.getColumn(OMACACTIVITY.COMPLETED_TIME);

                yield buildCriteriaForDateField(operator, criteriaValue, column);
            }
            case AVAILABILITY -> {
                column = table.getColumn(OMACACTIVITY.AVAILABILITY);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case STATUS -> {
                column = table.getColumn(OMACACTIVITY.STATUS);
                final Collection<Long> valueEntityIds = Objects.isNull(criteriaValues) || criteriaValues.isEmpty() ? null
                        : criteriaValues.stream().map(CriteriaValue::getValueEntityId).filter(Objects::nonNull).collect(Collectors.toSet());

                yield Objects.isNull(valueEntityIds) || (valueEntityIds.size() == 1 || comparator.isMultiValueAllowed())
                        ? buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator)
                        : new WhereClause(Criteria.in(column, valueEntityIds));
            }
            case TYPE_ID -> {
                column = table.getColumn(OMACACTIVITY.TYPE_ID);
                final Collection<Long> valueEntityIds = Objects.isNull(criteriaValues) || criteriaValues.isEmpty() ? null
                        : criteriaValues.stream().map(CriteriaValue::getValueEntityId).filter(Objects::nonNull).collect(Collectors.toSet());

                yield Objects.isNull(valueEntityIds) || (valueEntityIds.size() == 1 || comparator.isMultiValueAllowed())
                        ? buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator)
                        : new WhereClause(Criteria.in(column, valueEntityIds));
            }
            case DUE_DATE -> {
                column = table.getColumn(OMACACTIVITY.END_TIME);

                yield Objects.equals(DateIntervals.BETWEEN.name(), dateIntervalService.get(valueEntityId).getName())
                        ? buildCriteriaForBetween(value) : buildCriteriaForDateField(operator, criteriaValue, column);
            }
            case TOTAL_ACTIVITIES -> {
                final CountCriteriaAppenderRequisites criteriaRequisites = new CountCriteriaAppenderRequisites.Builder()
                        .setCount(value)
                        .setPriorityDetailHolders(List.of(new JoinPriorityDetails(JoinPreference.MAPPER_TABLE)))
                        .setRelatedSourceType(FilterSourceType.ACTIVITY)
                        .setFieldInfo(fieldInfo)
                        .build(table.getPKColumn());

                setRequisitesIntoCountCriteriaAppender(criteriaRequisites, criteriaAppenderHolder);
                yield null;
            }
            case DONE_ACTIVITIES -> {
                final Column isDoneColumn = table.getColumn(OMACACTIVITY.IS_DONE);
                final CountCriteriaAppenderRequisites criteriaRequisites = new CountCriteriaAppenderRequisites.Builder()
                        .setCount(value)
                        .setPriorityDetailHolders(List.of(new JoinPriorityDetails(JoinPreference.MAPPER_TABLE)))
                        .setRelatedSourceType(FilterSourceType.ACTIVITY)
                        .setFieldInfo(fieldInfo)
                        .build(isDoneColumn);

                setRequisitesIntoCountCriteriaAppender(criteriaRequisites, criteriaAppenderHolder);
                yield new WhereClause(Criteria.eq(isDoneColumn, true));
            }
            case UPCOMING_ACTIVITIES -> {
                final Column isDoneColumn = table.getColumn(OMACACTIVITY.IS_DONE);
                final CountCriteriaAppenderRequisites criteriaRequisites = new CountCriteriaAppenderRequisites.Builder()
                        .setCount(value)
                        .setPriorityDetailHolders(List.of(new JoinPriorityDetails(JoinPreference.MAPPER_TABLE)))
                        .setRelatedSourceType(FilterSourceType.ACTIVITY)
                        .setFieldInfo(fieldInfo)
                        .build(isDoneColumn);

                setRequisitesIntoCountCriteriaAppender(criteriaRequisites, criteriaAppenderHolder);
                yield new WhereClause(Criteria.eq(isDoneColumn, false)).and(Criteria.gt(table.getColumn(OMACACTIVITY.START_TIME), Instant.now().toEpochMilli()));
            }
            case IS_DONE -> {
                column = table.getColumn(OMACACTIVITY.IS_DONE);

                yield new WhereClause(Criteria.eq(column, Activity.CompletionStatus.getStatus(valueEntityId)));
            }
            case CREATOR -> {
                column = table.getColumn(OMACACTIVITY.CREATOR_ID);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case ASSIGNEE_ID -> {
                column = table.getColumn(OMACACTIVITY.ASSIGNEE_ID);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case COLLABORATORS -> {
                final Table collaboratorTable = getCollaboratorTable();
                final Column activityIdColumn = collaboratorTable.getColumn(OMACACTIVITYCOLLABORATOR.ACTIVITY_ID);
                final Collection<Long> valueEntityIds = Objects.isNull(criteriaValues) ? Collections.emptySet() : criteriaValues.stream()
                        .map(CriteriaValue::getValueEntityId).filter(Objects::nonNull).collect(Collectors.toSet());
                column = collaboratorTable.getColumn(OMACACTIVITYCOLLABORATOR.COLLABORATOR_ID);

                yield operator.isInvertedOperator() && !valueEntityIds.isEmpty()
                        ? fieldCriteriaBuilder.buildMultiSelectFieldCriteria(collaboratorTable, activityIdColumn, column, valueEntityIds, operator)
                        : buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case REMINDER -> {
                column = table.getColumn(OMACACTIVITY.REMINDER);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case DURATION -> {
                column = table.getColumn(OMACACTIVITY.DURATION);

                yield new WhereClause(new Criteria(column, value, operator));
            }
            case PRIORITY -> {
                column = table.getColumn(OMACACTIVITY.PRIORITY);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case TODAY_ACTIVITIES -> {
                final Table activityTable = Objects.isNull(fieldInfo) ? table : getAliasTable(table, fieldInfo.aliasName());
                column = activityTable.getColumn(OMACACTIVITY.START_TIME);
                final Column endTimeColumn = activityTable.getColumn(OMACACTIVITY.END_TIME);
                final Interval<Long, Long> interval = DateIntervals.TODAY.getInterval();
                final Long startTime = interval.start();
                final Long endTime = interval.end();

                yield new WhereClause(Criteria.between(column, startTime, endTime))
                        .or(Criteria.between(endTimeColumn, startTime, endTime)).or(new WhereClause(Criteria.lt(column, startTime))
                                .and(Criteria.gt(endTimeColumn, endTime)));
            }
            case CONTACT_IDS -> {
                final SelectQuery selectQuery = SelectQuery.get(table);
                final Table contactActivityTable = getContactActivityTable();
                final Column activityPkColumn = table.getPKColumn();

                selectQuery.addSelectClause(activityPkColumn);
                selectQuery.addJoinClause(new JoinClause(activityPkColumn, contactActivityTable.getColumn(MACCONTACTACTIVITY.ACTIVITY_ID), JoinClause.JoinType.LEFT));
                selectQuery.setWhereClause(new WhereClause(Criteria.eq(contactActivityTable.getColumn(MACCONTACTACTIVITY.CONTACT_ID), valueEntityId)));

                yield new WhereClause(switch (operator) {
                    case NOT_EQUAL -> Criteria.notIn(activityPkColumn, selectQuery);
                    default -> Criteria.in(activityPkColumn, selectQuery);
                });
            }
            case COMPANY_ID ->
                    new WhereClause(new Criteria(getCompanyActivityTable().getColumn(MACCOMPANYACTIVITY.COMPANY_ID), valueEntityId, operator));
            case DEAL_ID ->
                    new WhereClause(new Criteria(getDealActivityTable().getColumn(MACDEALACTIVITY.DEAL_ID), valueEntityId, operator));
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
    protected void appendJoinsBasedOnField(final FilterCriteriaDetails filterCriteriaDetails) {
        final Table activityTable = getTable();
        final FilterOptions filterOption = filterCriteriaDetails.getFilterOptions();

        if (Objects.nonNull(filterOption)) {
            switch (filterOption) {
                case CONTACT_IDS ->
                        filterCriteriaDetails.getJoinsLoader().load(activityTable.getPKColumn(), getContactActivityTable().getColumn(MACCONTACTACTIVITY.ACTIVITY_ID));
                case COMPANY_ID ->
                        filterCriteriaDetails.getJoinsLoader().load(activityTable.getPKColumn(), getCompanyActivityTable().getColumn(MACCOMPANYACTIVITY.ACTIVITY_ID));
                case DEAL_ID ->
                        filterCriteriaDetails.getJoinsLoader().load(activityTable.getPKColumn(), getDealActivityTable().getColumn(MACDEALACTIVITY.ACTIVITY_ID));
                case COLLABORATORS ->
                        filterCriteriaDetails.getJoinsLoader().load(activityTable.getPKColumn(), getCollaboratorTable().getColumn(OMACACTIVITYCOLLABORATOR.ACTIVITY_ID));
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
            case CREATED_AT -> table.getColumn(OMACACTIVITY.CREATED_TIME);
            case START_TIME -> table.getColumn(OMACACTIVITY.START_TIME);
            case END_TIME -> table.getColumn(OMACACTIVITY.END_TIME);
            case UPDATED_AT -> table.getColumn(OMACACTIVITY.MODIFIED_TIME);
            case DESCRIPTION -> table.getColumn(OMACACTIVITY.DESCRIPTION);
            case LOCATION -> table.getColumn(OMACACTIVITY.LOCATION);
            case TITLE -> table.getColumn(OMACACTIVITY.TITLE);
            case LAST_NOTIFICATION_TIME -> table.getColumn(OMACACTIVITY.LAST_NOTIFICATION_TIME);
            default -> super.getFieldColumn(fieldTable, fieldInfo, filterOption);
        };
    }

    /**
     * <p>
     * Builds the criteria for the between condition of the due date field.
     * </p>
     *
     * @param value Holds the input value of criteria.
     * @return {@link WhereClause}.
     */
    private WhereClause buildCriteriaForBetween(final Object value) {
        if (value instanceof final Map<?, ?> valueMap) {
            final Long endTimeOfDay = TemporalServiceProvider.get((Long) valueMap.get(END)).getEndTimeOfDay(getUserTimeZoneInfo());

            return new WhereClause(Criteria.between(getTable().getColumn(OMACACTIVITY.END_TIME), (Long) valueMap.get(START), endTimeOfDay));
        }
        return null;
    }

    /**
     * <p>
     * Gets the user {@link TimezoneInfo}
     * </p>
     *
     * @return The current user {@link TimezoneInfo}
     */
    private TimezoneInfo getUserTimeZoneInfo() {
        final CrmUser crmUser = LoggedInUserInfo.getUser();
        final UserPreferenceHolder preferenceHolder = crmUser.getPreferenceHolder();
        final UserRegionalPreference userPreference = preferenceHolder.get(UserPreferenceType.USER_REGIONAL_PREFERENCE);

        return userPreference.getTimezoneInfo();
    }

    /**
     * <p>
     * Fetches the table of the activity collaborators.
     * </p>
     *
     * @return Activity collaborators table.
     */
    private Table getCollaboratorTable() {
        return Table.get(OMACACTIVITYCOLLABORATOR.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the activity contact.
     * </p>
     *
     * @return Activity contact table.
     */
    private Table getContactActivityTable() {
        return Table.get(MACCONTACTACTIVITY.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the activity company.
     * </p>
     *
     * @return Activity company table.
     */
    private Table getCompanyActivityTable() {
        return Table.get(MACCOMPANYACTIVITY.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the activity deal.
     * </p>
     *
     * @return Activity deal table.
     */
    private Table getDealActivityTable() {
        return Table.get(MACDEALACTIVITY.TABLE);
    }
}
