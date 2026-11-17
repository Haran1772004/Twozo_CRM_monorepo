package com.tlc.crm.core.action.filter;

import com.tlc.crm.core.sql.resource.MACCONTACTACTIVITY;
import com.tlc.crm.core.sql.resource.MTCONTACTTAG;
import com.tlc.crm.core.sql.resource.OMCONFOLLOWERS;
import com.tlc.crm.core.sql.resource.OMKLMARKETINGLISTCONTACT;
import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.list.filter.appender.CriteriaAppenderHolder;
import com.tlc.crm.entity.list.filter.appender.model.NullableFieldCriteriaAppenderRequisites;
import com.tlc.crm.entity.list.filter.models.JoinPreference;
import com.tlc.crm.entity.list.filter.models.FilterCriteriaDetails;
import com.tlc.crm.entity.list.filter.models.JoinPriorityDetails;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.l10n.temporal.interval.Interval;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.features.api.dataimport.manager.ImportManager;
import com.tlc.crm.core.features.api.dataimport.model.ImportInfo;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MCONCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.OMITCONTACTHISTORY;
import com.tlc.crm.core.sql.resource.OMITHISTORY;
import com.tlc.crm.core.sql.resource.OMITIMPORT;
import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.CriteriaValue;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.fields.FilterOptions;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.list.filter.Filter;
import com.tlc.crm.entity.list.filter.FilterAction;
import com.tlc.crm.entity.list.filter.loader.JoinsLoader;
import com.tlc.crm.entity.list.filter.source.FilterSourceType;
import com.tlc.crm.entity.list.filter.source.SourceLinkGroup;

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
 * Provides the action which can be perform contact related filter actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Filter(source = FilterSourceType.CONTACT, provider = Provider.CONTACT_LIST, sourceLinkGroups = SourceLinkGroup.CONTACT_PRODUCT)
public class ContactFilterAction extends FilterAction {

    private final ContactManager contactManager;
    private final ImportManager importManager;

    public ContactFilterAction() {
        contactManager = ContactManager.getInstance();
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
        final CriteriaValue criteriaValue = Objects.nonNull(criteriaValues) && !criteriaValues.isEmpty() ?
                criteriaValues.stream().findAny().get() : null;
        final Object value = Objects.isNull(criteriaValue) ? null : criteriaValue.getValue();
        final Long valueEntityId = Objects.isNull(criteriaValue) ? null : criteriaValue.getValueEntityId();
        final Comparator comparator = filterCriteria.getComparator();
        final Operator operator = Operator.getOperator(comparator.getAliasName());
        final Table table = getTable();
        Column column = null;

        final WhereClause filter = switch (filterOption) {
            case CONTACT, CONTACT_IDS -> {
                column = table.getPKColumn();

                yield new WhereClause(new Criteria(column, valueEntityId, operator));
            }
            case CREATED_AT -> {
                column = table.getColumn(MCOCONTACT.CREATED_TIME);

                yield buildCriteriaForDateField(operator, criteriaValue, column);
            }
            case UPDATED_AT -> {
                column = table.getColumn(MCOCONTACT.MODIFIED_TIME);

                yield buildCriteriaForDateField(operator, criteriaValue, column);
            }
            case CREATED_TIME -> {
                column = table.getColumn(MCOCONTACT.CREATED_TIME);

                yield buildDateFieldCriteria((Interval<Long, Long>) value, operator, column);
            }
            case UPDATED_TIME -> {
                column = table.getColumn(MCOCONTACT.MODIFIED_TIME);

                yield buildDateFieldCriteria((Interval<Long, Long>) value, operator, column);
            }
            case IMPORTED_TIME -> {
                final Table importTable = importManager.getTable();
                column = importTable.getColumn(OMITIMPORT.MODIFIED_TIME);
                final WhereClause dateFieldCriteria = buildDateFieldCriteria((Interval<Long, Long>) value, operator, column);

                yield Objects.isNull(dateFieldCriteria) ? null : new WhereClause(Criteria.eq(importTable.getColumn(OMITIMPORT.STATUS),
                        ImportInfo.Status.FINISHED.getType())).and(dateFieldCriteria);
            }
            case VOICE_CALL -> fetchSourceAction(FilterSourceType.CALL).buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
//            case IS_ACTIVE -> {
//                column = table.getColumn(MCOCONTACT.IS_ACTIVE);
//
//                yield new WhereClause(new Criteria(column, value, operator));
//            }TODO soft delete needed.
            case TEAM -> {
                column = table.getColumn(MCOCONTACT.OWNER_ID);

                yield new WhereClause(new Criteria(column, value, Objects.isNull(value) ? Operator.EQUAL : operator));
            }
            case LIFE_CYCLE_STAGE -> {
                column = table.getColumn(MCOCONTACT.LIFE_CYCLE_STAGE_ID);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case LIFE_CYCLE_STATUS -> {
                column = table.getColumn(MCOCONTACT.LIFE_CYCLE_STATUS_ID);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case TAGS -> {
                final Table contactTagTable = getContactTagTable();
                final Collection<Long> valueEntityIds = Objects.isNull(criteriaValues) ? Collections.emptySet() : criteriaValues.stream()
                        .map(CriteriaValue::getValueEntityId).filter(Objects::nonNull).collect(Collectors.toSet());
                column = contactTagTable.getColumn(MTCONTACTTAG.CONTACT_ID);

                yield buildTagFieldCriteria(contactTagTable, column, contactTagTable.getColumn(MTCONTACTTAG.TAG_ID), valueEntityIds, operator);
            }
            case MARKETING_LIST -> {
                final Table marketingListContactTable = getMarketingListContactTable();
                final Collection<Long> valueEntityIds = Objects.isNull(criteriaValues) ? Collections.emptySet()
                        : criteriaValues.stream().map(CriteriaValue::getValueEntityId).filter(Objects::nonNull).collect(Collectors.toSet());
                final Column marketingListIdColumn = marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID);
                column = marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.CONTACT_ID);

                if (valueEntityIds.isEmpty()) {
                    yield Operator.EQUAL == operator
                            ? new WhereClause(Criteria.eq(column, null))
                            : new WhereClause(Criteria.notEq(marketingListContactTable.getPKColumn(), null));
                }
                yield fieldCriteriaBuilder.buildMultiSelectFieldCriteria(marketingListContactTable, column, marketingListIdColumn, valueEntityIds, operator);
            }
            case SALES_OWNER -> {
                column = table.getColumn(MCOCONTACT.OWNER_ID);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case OPEN_DEALS, WON_DEALS, LOST_DEALS, CLOSED_DEALS ->
                    fetchSourceAction(FilterSourceType.DEAL).buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
            case RECENT_NOTE ->
                    fetchSourceAction(FilterSourceType.NOTE).buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
            case LAST_ACTIVITY_DATE, NEXT_ACTIVITY_DATE, TOTAL_ACTIVITIES, DONE_ACTIVITIES, UPCOMING_ACTIVITIES, LAST_ACTIVITY_TYPE ->
                    fetchSourceAction(FilterSourceType.ACTIVITY).buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
            case LAST_EMAIL_RECEIVED, LAST_EMAIL_SENT, EMAIL_MESSAGE_COUNT ->
                    fetchSourceAction(FilterSourceType.EMAIL).buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
            case TERRITORY -> {
                column = table.getColumn(MCOCONTACT.TERRITORY_ID);

                yield buildTerritoryWhereClause(comparator.isValueNeeded(), comparator.isMultiValueAllowed(), column,
                        Objects.isNull(criteriaValue) ? null : criteriaValue.getValueEntityId(), value, operator, criteriaValues);
            }
            case CAMPAIGN -> null; //TODO Campaign need to implement.
            case CREATED_BY -> {
                column = table.getColumn(MCOCONTACT.CREATED_BY);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case UPDATED_BY -> {
                column = table.getColumn(MCOCONTACT.UPDATED_BY);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case FOLLOWER -> {
                column = getContactFollowersTable().getColumn(OMCONFOLLOWERS.FOLLOWER_ID);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case LAST_ASSIGNED_AT -> buildCriteriaForDateField(operator, criteriaValue, table.getColumn(MCOCONTACT.LAST_ASSIGNED_AT));

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
        final Table contactTable = getTable();
        final Column contactPkColumn = contactTable.getPKColumn();
        final FilterOptions filterOption = filterCriteriaDetails.getFilterOptions();
        final JoinsLoader joinsLoader = filterCriteriaDetails.getJoinsLoader();

        if (Objects.nonNull(filterOption)) {
            switch (filterOption) {
                case RECENT_NOTE ->
                        fetchSourceAction(FilterSourceType.NOTE).loadJoins(contactTable, filterCriteriaDetails);
                case EMAIL_CONVERSATION, LAST_EMAIL_RECEIVED, LAST_EMAIL_SENT, EMAIL_MESSAGE_COUNT ->
                        fetchSourceAction(FilterSourceType.EMAIL).loadJoins(contactTable, filterCriteriaDetails);
                case VOICE_CALL -> fetchSourceAction(FilterSourceType.CALL).loadJoins(contactTable, filterCriteriaDetails);
                case OPEN_DEALS, WON_DEALS, LOST_DEALS, CLOSED_DEALS -> {
                    final JoinPriorityDetails joinPriorityDetails = new JoinPriorityDetails(JoinPreference.MAPPER_TABLE);

                    fetchSourceAction(FilterSourceType.DEAL).loadJoins(contactTable,
                            new FilterCriteriaDetails(false, filterCriteriaDetails, joinPriorityDetails));
                }
                case LAST_ACTIVITY_DATE, NEXT_ACTIVITY_DATE, TOTAL_ACTIVITIES, DONE_ACTIVITIES, UPCOMING_ACTIVITIES, LAST_ACTIVITY_TYPE -> {
                    final FilterAction activityFilterAction = fetchSourceAction(FilterSourceType.ACTIVITY);
                    final JoinPriorityDetails joinPriorityDetails = new JoinPriorityDetails(JoinPreference.MAPPER_TABLE);
                    final FieldInfo fieldInfo = filterCriteriaDetails.getFieldInfo();
                    final Table activityTable = activityFilterAction.getTable();
                    final Table activityAliasTable = Objects.isNull(fieldInfo) ? activityTable : getAliasTable(activityTable, fieldInfo.aliasName());

                    joinPriorityDetails.addMapperTable(Table.get(MACCONTACTACTIVITY.TABLE));
                    activityFilterAction.loadJoins(contactTable, activityAliasTable,
                            new FilterCriteriaDetails(true, filterCriteriaDetails, joinPriorityDetails));
                }
                case TAGS -> joinsLoader.load(contactPkColumn, getContactTagTable().getColumn(MTCONTACTTAG.CONTACT_ID));
                case MARKETING_LIST -> joinsLoader.load(contactPkColumn, getMarketingListContactTable().getColumn(OMKLMARKETINGLISTCONTACT.CONTACT_ID));
                case IMPORTED_TIME -> {
                    final Table contactHistoryTable = Table.get(OMITCONTACTHISTORY.TABLE);
                    final Table importHistoryTable = importManager.getHistoryTable();

                    joinsLoader.load(contactPkColumn, contactHistoryTable.getColumn(OMITCONTACTHISTORY.ENTITY_ID));
                    joinsLoader.load(contactHistoryTable.getColumn(OMITCONTACTHISTORY.HISTORY_ID), importHistoryTable.getPKColumn());
                    joinsLoader.load(importHistoryTable.getColumn(OMITHISTORY.IMPORT_ID), importManager.getTable().getPKColumn());
                }
                case FOLLOWER -> {
                    joinsLoader.load(contactPkColumn, getContactFollowersTable().getColumn(OMCONFOLLOWERS.CONTACT_ID));
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
            case ID -> table.getPKColumn();
            case CREATED_AT -> table.getColumn(MCOCONTACT.CREATED_TIME);
            case FIRST_NAME -> table.getColumn(MCOCONTACT.FIRST_NAME);
            case LAST_NAME -> table.getColumn(MCOCONTACT.LAST_NAME);
            case OTHER_UNSUBSCRIBE_REASON -> table.getColumn(MCOCONTACT.OTHER_UNSUBSCRIBE_REASON);
            case FACEBOOK -> table.getColumn(MCOCONTACT.FACEBOOK);
            case LINKEDIN -> table.getColumn(MCOCONTACT.LINKEDIN);
            case TWITTER -> table.getColumn(MCOCONTACT.TWITTER);
            case DEPARTMENT -> table.getColumn(MCOCONTACT.DEPARTMENT);
            case DESIGNATION -> table.getColumn(MCOCONTACT.DESIGNATION);
            case DATE_OF_BIRTH -> table.getColumn(MCOCONTACT.DATE_OF_BIRTH);
            case LAST_ASSIGNED_AT -> table.getColumn(MCOCONTACT.LAST_ASSIGNED_AT);
            case UPDATED_AT -> table.getColumn(MCOCONTACT.MODIFIED_TIME);
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
                getContactCompanyTable()
        };
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTable      Represents the field table
     * @param storeType       Represents the {@link StoreType}
     * @param fieldFkColumns  Map containing key as Table and value fk columns
     * @return The Remote column
     */
    @Override
    protected Column getRemoteColumn(final Table fieldTable, final StoreType storeType, final Map<Table, Column> fieldFkColumns) {
        return switch (storeType) {
            case CONTACT -> fieldTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID);
            default -> super.getRemoteColumn(fieldTable, storeType, fieldFkColumns);
        };
    }

    /**
     * {@inheritDoc}
     *
     * @return contact {@link Table}.
     */
    @Override
    public Table getTable() {
        return contactManager.getTable();
    }

    /**
     * <p>
     * Fetches the table of the contact company.
     * </p>
     *
     * @return Contact company table.
     */
    private Table getContactCompanyTable() {
        return Table.get(MCONCOMPANYFIELDS.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the contact tag.
     * </p>
     *
     * @return Contact tag table.
     */
    private Table getContactTagTable() {
        return Table.get(MTCONTACTTAG.TABLE);
    }

    /**
     * <p>
     * Gets the contact followers table
     * </p>
     *
     * @return The contact followers table
     */
    private Table getContactFollowersTable() {
        return Table.get(OMCONFOLLOWERS.TABLE);
    }

    /**
     * <p>
     * Gets the marketing list contact table
     * </p>
     *
     * @return The marketing list contact table
     */
    private Table getMarketingListContactTable() {
        return Table.get(OMKLMARKETINGLISTCONTACT.TABLE);
    }

}
