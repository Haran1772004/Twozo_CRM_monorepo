package com.tlc.crm.entity.list.filter;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.CriteriaValue;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.date.DateIntervals;
import com.tlc.crm.entity.filter.fields.FilterOptions;
import com.tlc.crm.entity.internal.status.FilterErrorCodes;
import com.tlc.crm.entity.list.filter.appender.CriteriaAppender;
import com.tlc.crm.entity.list.filter.appender.CriteriaAppenderHolder;
import com.tlc.crm.entity.list.filter.appender.impl.CountCriteriaAppender;
import com.tlc.crm.entity.list.filter.appender.impl.DynamicFieldEmptyValueCriteriaAppender;
import com.tlc.crm.entity.list.filter.appender.impl.MaxValueCriteriaAppender;
import com.tlc.crm.entity.list.filter.appender.impl.MinValueCriteriaAppender;
import com.tlc.crm.entity.list.filter.appender.impl.NullableFieldCriteriaAppender;
import com.tlc.crm.entity.list.filter.appender.model.CountCriteriaAppenderRequisites;
import com.tlc.crm.entity.list.filter.appender.model.DynamicFieldEmptyValueCriteriaAppenderRequisites;
import com.tlc.crm.entity.list.filter.appender.model.MinMaxValueCriteriaAppenderRequisites;
import com.tlc.crm.entity.list.filter.appender.model.NullableFieldCriteriaAppenderRequisites;
import com.tlc.crm.entity.list.filter.builder.FieldCriteriaBuilder;
import com.tlc.crm.entity.list.filter.internal.FilterActionContainer;
import com.tlc.crm.entity.list.filter.internal.JoinStatusHolder;
import com.tlc.crm.entity.list.filter.loader.JoinsLoader;
import com.tlc.crm.entity.list.filter.models.FilterCriteriaDetails;
import com.tlc.crm.entity.list.filter.models.JoinPreference;
import com.tlc.crm.entity.list.filter.models.JoinPriorityDetails;
import com.tlc.crm.entity.list.filter.source.FilterSourceType;
import com.tlc.crm.entity.list.filter.source.SourceLinkGroup;
import com.tlc.crm.entity.list.filter.source.SourceTableMappings;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.manager.field.type.AddressManager;
import com.tlc.crm.entity.manager.field.type.DateRangeManager;
import com.tlc.crm.entity.manager.field.type.DateTimeRangeManager;
import com.tlc.crm.entity.manager.field.type.EmailManager;
import com.tlc.crm.entity.manager.field.type.MonetaryManager;
import com.tlc.crm.entity.manager.field.type.PhoneManager;
import com.tlc.crm.entity.manager.field.type.TimeRangeManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.filter.DateIntervalService;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.fields.service.internal.FieldTypeServiceImpl;
import com.tlc.crm.fields.sql.resource.OMETDATERANGE;
import com.tlc.crm.fields.sql.resource.OMETDATETIMERANGE;
import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.crm.fields.sql.resource.OMETMONETARY;
import com.tlc.crm.fields.sql.resource.OMETPHONE;
import com.tlc.crm.fields.sql.resource.OMETTIMERANGE;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.sql.resource.OMCRMUSER;
import com.tlc.l10n.temporal.interval.Interval;

import com.tlc.sql.common.api.dml.Argument;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.function.SqlFunction;
import com.tlc.sql.common.api.meta.CommonDataType;
import com.tlc.sql.common.util.TableUtils;

import com.tlc.sql.oltp.resolver.OltpTableResolver;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the action which can be perform filter related actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class FilterAction {

    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("dd-MM-yyyy");
    private static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("h:m a");
    private static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat("d-M-y h:m:s a");
    private static final long ONE_DAY_IN_MILLISECONDS = 24 * 60 * 60 * 1000;
    private static final byte ONE = 1;
    private static final long ONE_MINUTE_MS = 60_000L;
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final DateTimeFormatter TIME_PARSER = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    protected static final String ENTITY_ID = FieldKeys.ENTITY_ID.name();
    protected final AddressManager addressManager;
    protected final EmailManager emailManager;
    protected final PhoneManager phoneManager;
    protected final MonetaryManager monetaryManager;
    protected final DateIntervalService dateIntervalService;
    protected final FieldCriteriaBuilder fieldCriteriaBuilder;

    private final FilterActionContainer filterActionContainer;
    private final FieldTypeService fieldTypeService;
    private final DateRangeManager dateRangeManager;
    private final TimeRangeManager timeRangeManager;
    private final DateTimeRangeManager dateTimeRangeManager;
    private final FieldService fieldService;

    public FilterAction() {
        filterActionContainer = FilterActionContainer.getInstance();
        fieldTypeService = FieldTypeService.getInstance();
        addressManager = AddressManager.getInstance();
        monetaryManager = MonetaryManager.getInstance();
        emailManager = EmailManager.getInstance();
        phoneManager = PhoneManager.getInstance();
        dateRangeManager = DateRangeManager.getInstance();
        timeRangeManager = TimeRangeManager.getInstance();
        dateTimeRangeManager = DateTimeRangeManager.getInstance();
        dateIntervalService = DateIntervalService.getInstance();
        fieldCriteriaBuilder = FieldCriteriaBuilder.getInstance();
        fieldService = FieldService.getSecureInstance();
    }

    /**
     * <p>
     * Builds the {@link WhereClause} for the system fields using {@link FilterCriteria}.
     * </p>
     *
     * @param fieldInfo              {@link FieldInfo} that holds the field information.
     * @param filterCriteria         holds the details of criteria.
     * @param criteriaAppenderHolder Holds the post-criteria appender.
     * @return {@link WhereClause} holds the details about retrieval criteria.
     */
    public WhereClause buildWhereClause(final FieldInfo fieldInfo, final FilterCriteria filterCriteria,
                                        final CriteriaAppenderHolder criteriaAppenderHolder) {
        if (Objects.isNull(fieldInfo)) {
            return null;
        }
        final CriteriaField criteriaField = filterCriteria.getCriteriaField();
        final Comparator comparator = filterCriteria.getComparator();
        final Operator operator = Operator.getOperator(comparator.getAliasName());
        final Collection<CriteriaValue> criteriaValues = filterCriteria.getCriteriaValues();
        final CriteriaValue criteriaValue = Objects.isNull(criteriaValues) || criteriaValues.isEmpty() ? null
                : criteriaValues.stream().findAny().get();
        final Object value = Objects.isNull(criteriaValue) ? null : criteriaValue.getValue();
        final FilterOptions filterOption = FilterOptions.get(criteriaField.getAliasName());
        final String fieldAlias = Objects.isNull(filterOption) ? fieldInfo.idAsString() : filterOption.name();
        final boolean isPersistent = fieldInfo.isPersistent();
        final Table fieldTable = isPersistent ? fieldInfo.table() : getAliasTable(fieldInfo.table(), fieldAlias);
        final Column fieldColumn = getFieldColumn(fieldTable, fieldInfo, filterOption);
        Column column = null;

        if (isPersistent) {
            column = fieldColumn;
        }

        final WhereClause filter = switch (criteriaField.getFieldType().storeType()) {
            case SINGLE_STRING, AUTO_NUMBER -> {
                column = fieldColumn;

                yield new WhereClause(new Criteria(column, value, operator));
            }
            case SINGLE_NUMBER -> buildCriteriaForNumberField(fieldColumn, value, operator);
            case DATE, DATE_TIME -> {
                column = fieldColumn;

                yield buildCriteriaForDateField(operator, criteriaValue, column);
            }
            case TIME -> buildCriteriaForTimeField(fieldColumn, value, operator);
            case DROPDOWN, OWNER, CONTACT, COMPANY, CONTACT_ONBOARD, COMPANY_ONBOARD, CONTACTS_ONBOARD -> {
                column = fieldColumn;

                yield isPersistent ? buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator) : buildFieldCriteria(criteriaField.getId(), null, fieldTable, column, comparator, criteriaValues);
            }
            case MULTI_SELECT -> {
                final Collection<Long> valueEntityIds = Objects.isNull(criteriaValues) || criteriaValues.isEmpty() ? null
                        : criteriaValues.stream().map(CriteriaValue::getValueEntityId).collect(Collectors.toSet());

                if (isPersistent) {
                    yield buildPersistentMultiSelectCriteria(column, operator, valueEntityIds);
                } else {
                    final Map<Table, Column> fieldTableFkColumns = TableUtils.groupFkColumnsByTable(fieldTable, OltpTableResolver.get());
                    final Table table = getTable();
                    column = fieldTableFkColumns.get(table);

                    yield fieldCriteriaBuilder.buildMultiSelectFieldCriteria(criteriaField.getId(), fieldTable, fieldColumn, operator, valueEntityIds);
                }
            }
            case MONETARY -> {
                if (isPersistent) {
                    yield buildPersistentFieldCriteria(monetaryManager.getTable(), OMETMONETARY.VALUE, column, comparator, criteriaValues);
                } else {
                    final Table monetaryTable = getAliasTable(monetaryManager.getTable(), fieldAlias);
                    column = monetaryTable.getColumn(OMETMONETARY.VALUE);

                    yield buildFieldCriteria(criteriaField.getId(), monetaryTable, fieldTable, column, comparator, criteriaValues);
                }
            }
            case ADDRESS -> {
                final Table addressTable = getAliasTable(addressManager.getTable(), fieldAlias);
                final Long additionalOptionId = criteriaField.getAdditionalOption().getOptionEntityId();
                final Column subTypeColumn = fetchFieldSubTypeColumn(addressTable, additionalOptionId);

                if (isPersistent) {
                    yield buildPersistentFieldCriteria(addressTable, Objects.isNull(subTypeColumn) ? null : subTypeColumn.getColumnName(), column, comparator, criteriaValues);
                } else {
                    column = subTypeColumn;

                    yield buildFieldCriteria(criteriaField.getId(), addressTable, fieldTable, column, comparator, criteriaValues);
                }
            }
            case EMAIL, EMAILS -> {
                if (isPersistent) {
                    yield buildPersistentFieldCriteria(emailManager.getTable(), OMETEMAIL.VALUE, column, comparator, criteriaValues);
                } else {
                    final Table emailTable = getAliasTable(emailManager.getTable(), fieldAlias);
                    column = emailTable.getColumn(OMETEMAIL.VALUE);

                    yield buildFieldCriteria(criteriaField.getId(), emailTable, fieldTable, column, comparator, criteriaValues);
                }
            }
            case PHONE, PHONES -> {
                if (isPersistent) {
                    yield buildPersistentFieldCriteria(phoneManager.getTable(), OMETPHONE.NUMBER, column, comparator, criteriaValues);
                } else {
                    final Table phoneTable = getAliasTable(phoneManager.getTable(), fieldAlias);
                    column = phoneTable.getColumn(OMETPHONE.NUMBER);

                    yield buildFieldCriteria(criteriaField.getId(), phoneTable, fieldTable, column, comparator, criteriaValues);
                }
            }
            case DATE_RANGE -> {
                final String columnName = comparator.getKeyName();
                final String subTypeColumnName = Objects.isNull(columnName) ? OMETDATERANGE.FROM : columnName;

                if (isPersistent) {
                    yield buildPersistentDateFieldCriteria(dateRangeManager.getTable(), subTypeColumnName, column, comparator, criteriaValues, DATE_FORMATTER);
                } else {
                    final Table dateRangeTable = getAliasTable(dateRangeManager.getTable(), fieldAlias);
                    column = dateRangeTable.getColumn(subTypeColumnName);

                    try {
                        yield isPersistent ? buildPersistentDateFieldCriteria(dateRangeManager.getTable(), subTypeColumnName, fieldColumn, comparator, criteriaValues, DATE_FORMATTER) : buildDateFieldCriteria(criteriaField.getId(), dateRangeTable, fieldTable, column, comparator, criteriaValues, DATE_FORMATTER);
                    } catch (ParseException e) {
                        throw ErrorCode.get(FilterErrorCodes.UNABLE_TO_PARSE_DATE, e.getMessage());
                    }
                }
            }
            case TIME_RANGE -> {
                final String columnName = comparator.getKeyName();
                final String subTypeColumnName = Objects.isNull(columnName) ? OMETTIMERANGE.FROM : columnName;

                if (isPersistent) {
                    yield buildPersistentTimeRangeCriteria(timeRangeManager.getTable(), subTypeColumnName, column, operator, criteriaValues);
                } else {
                    final Table timeRangeTable = getAliasTable(timeRangeManager.getTable(), fieldAlias);
                    column = timeRangeTable.getColumn(subTypeColumnName);

                    yield buildNonPersistentTimeRangeCriteria(criteriaField.getId(), timeRangeTable, fieldTable, column, comparator, criteriaValues);
                }
            }
            case DATE_TIME_RANGE -> {
                final String columnName = comparator.getKeyName();
                final String rangeColumnName = Objects.isNull(columnName) ? OMETDATETIMERANGE.FROM : columnName;

                if (isPersistent) {
                    yield buildPersistentDateFieldCriteria(dateTimeRangeManager.getTable(), rangeColumnName, column, comparator, criteriaValues, DATE_TIME_FORMATTER);
                } else {
                    final Table dateTimeRangeTable = getAliasTable(dateTimeRangeManager.getTable(), fieldAlias);
                    column = dateTimeRangeTable.getColumn(rangeColumnName);

                    try {
                        yield buildDateFieldCriteria(criteriaField.getId(), dateTimeRangeTable, fieldTable, column, comparator, criteriaValues, DATE_TIME_FORMATTER);
                    } catch (ParseException e) {
                        throw ErrorCode.get(FilterErrorCodes.UNABLE_TO_PARSE_DATE_TIME, e.getMessage());
                    }
                }
            }
            default -> null;
        };
        final NullableFieldCriteriaAppenderRequisites criteriaRequisites = new NullableFieldCriteriaAppenderRequisites.Builder().build(column);

        setRequisitesIntoNullableFieldCriteriaAppender(criteriaRequisites, criteriaAppenderHolder);

        return filter;
    }

    /**
     * <p>
     * Appends the fieldId criteria for the case where the selected source differs from the provider source.
     * </p>
     *
     * @param provider    Represents the name of the {@link Provider}.
     * @param source      Represents the selected source.
     * @param hasNoValue  {@link CriteriaAppender} to appends the criteria for inverted operators.
     * @param operator    It is used to compare the given value with the given respective column.
     * @param whereClause {@link WhereClause} holds the details about retrieval criteria.
     * @return {@link WhereClause}.
     */
    public final WhereClause appendFieldIdCriteria(final boolean isCountableField, final Provider provider, final FilterSourceType source, final boolean hasNoValue,
                                                   final Operator operator, final WhereClause whereClause) {
        final String sourceName = provider.getSource().getName();

        if (!source.getName().equalsIgnoreCase(sourceName)) {
            final SourceTableMappings sourceTableMappings = SourceTableMappings.get(FilterSourceType.get(sourceName), source);

            if (Objects.nonNull(sourceTableMappings) && sourceTableMappings.isPersistentMapperTable()) {
                final String fieldName = sourceTableMappings.getFieldName();
                final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(sourceTableMappings.getPrimarySource().getName(), fieldName);

                if (fields.containsKey(fieldName)) {
                    final Long fieldId = fields.get(fieldName).id();
                    final Table mapperTable = sourceTableMappings.getMapperTable();

                    return fieldCriteriaBuilder.buildFieldIdCriteria(isCountableField, fieldId, hasNoValue, mapperTable, operator, whereClause);
                }
            }
        }
        return whereClause;
    }

    /**
     * <p>
     * Builds the joins for the required sources and append it into {@link SelectQuery} using {@link JoinsLoader}.
     * </p>
     *
     * @param filterCriteriaDetails {@link FilterCriteriaDetails} that holds the joins data information.
     */
    public final void appendJoins(final FilterCriteriaDetails filterCriteriaDetails) {
        final SourceHolder sourceHolder = filterCriteriaDetails.getSourceHolder();
        final JoinsLoader joinsLoader = filterCriteriaDetails.getJoinsLoader();
        final FilterOptions filterOption = filterCriteriaDetails.getFilterOptions();
        final FieldInfo fieldInfo = filterCriteriaDetails.getFieldInfo();
        final FilterSourceType relatedSource = sourceHolder.relatedSource();
        final FilterSourceType source = sourceHolder.source();
        final FilterAction sourceAction = filterActionContainer.fetchSourceAction(Objects.isNull(relatedSource) ? source : relatedSource);

        loadJoins(filterActionContainer.getSource(this), filterCriteriaDetails);

        if (Objects.isNull(fieldInfo) || !fieldInfo.isPersistent()) {
            fetchSourceAction(source).appendJoinsBasedOnField(filterCriteriaDetails);
        }

        if (Objects.nonNull(fieldInfo)) {
            sourceAction.appendJoinsBasedOnFieldType(Objects.isNull(filterOption) ? fieldInfo.idAsString() :
                    filterOption.name(), fieldInfo, joinsLoader, filterCriteriaDetails);
        }
    }

    /**
     * <p>
     * Loads the required joins to manage the applied criteria.
     * <p>
     * NOTE:
     * Please define this method when no direct relations for the base and local tables,
     * when relations of that tables are not single mapping table.
     * </p>
     *
     * @param baseTable             Refers to the base table of the previous {@link FilterAction}, which is used to
     *                              append the joins after the already joined tables.
     * @param filterCriteriaDetails Helps to manage the joins data information.
     * @param mapperTables          Refers to the mapper tables of the previous {@link FilterAction}. It is used to make
     *                              joins for the current and previous sources if those source tables have no direct relations.
     */
    public void loadJoins(final Table baseTable, final FilterCriteriaDetails filterCriteriaDetails, final Table... mapperTables) {
        loadJoins(baseTable, getTable(), filterCriteriaDetails, mapperTables);
    }

    /**
     * <p>
     * Loads the required joins to manage the applied criteria.
     * <p>
     * NOTE:
     * Please define this method when no direct relations for the base and local tables,
     * when relations of that tables are not single mapping table.
     * </p>
     *
     * @param baseTable             Refers to the base table of the previous {@link FilterAction}, which is used to
     *                              append the joins after the already joined tables.
     * @param filterCriteriaDetails Helps to manage the joins data information.
     * @param mapperTables          Refers to the mapper tables of the previous {@link FilterAction}. It is used to make
     *                              joins for the current and previous sources if those source tables have no direct relations.
     */
    public void loadJoins(final Table baseTable, final Table localTable, final FilterCriteriaDetails filterCriteriaDetails,
                          final Table... mapperTables) {
        loadJoins(baseTable, localTable, Objects.isNull(mapperTables) ? Collections.emptyList()
                : List.of(mapperTables), Collections.emptyList(), new JoinStatusHolder(), filterCriteriaDetails, filterCriteriaDetails.getPriorityDetailsHolder());
    }

    /**
     * <p>
     * Gets the main table of the current {@link FilterAction}.
     * </p>
     *
     * @return Respective {@link Table}.
     */
    public Table getTable() {
        return null;
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
    public WhereClause buildCriteriaForDateField(final Operator operator, final CriteriaValue criteriaValue,
                                                 final Column column) {
        final Interval<Long, Long> dateInterval = Objects.isNull(criteriaValue) ? null : fetchDateInterval(criteriaValue);

        if (Objects.nonNull(dateInterval)) {
            return buildDateFieldCriteria(dateInterval, operator, column);
        }

        return new WhereClause(new Criteria(column, null, operator));
    }

    /**
     * Resolves the parent {@link FilterAction} for the given field. Returns the current instance
     * if no field info is provided; otherwise, delegates to the filter action associated with the field's source.
     *
     * @param fieldInfo the field information used to determine the parent source, or {@code null} to use the current action.
     * @return the resolved {@link FilterAction}.
     */
    protected final FilterAction fetchParentFilterAction(final FieldInfo fieldInfo) {
        return Objects.isNull(fieldInfo) ? this : fetchSourceAction(FilterSourceType.get(fieldInfo.fieldSource().getName()));
    }

    /**
     * <p>
     * Fetches the {@link FilterAction} for the given {@link FilterSourceType}.
     * </p>
     *
     * @param sourceType Refers the source value of the filter.
     * @return {@link FilterAction} to perform filter related actions.
     */
    protected FilterAction fetchSourceAction(final FilterSourceType sourceType) {
        return filterActionContainer.fetchSourceAction(sourceType);
    }

    /**
     * <p>
     * Builds the {@link WhereClause} for the owner and dropdown-related fields.
     * </p>
     *
     * @param column         Represents the column to be filtered.
     * @param criteriaValues Represents the comparing values.
     * @param comparator     Represents the comparator to be compared with values.
     * @return {@link WhereClause}.
     */
    protected final WhereClause buildOwnerAndDropdownFieldCriteria(final Column column, final Collection<CriteriaValue> criteriaValues,
                                                                   final Comparator comparator) {
        final Operator operator = Operator.getOperator(comparator.getAliasName());

        if (Objects.isNull(criteriaValues) || criteriaValues.isEmpty()) {
            return new WhereClause(new Criteria(column, null, operator));
        } else if (comparator.isMultiValueAllowed() || criteriaValues.size() > 1) {
            final Collection<Long> valueEntityIds = criteriaValues.stream().map(CriteriaValue::getValueEntityId).filter(Objects::nonNull).collect(Collectors.toSet());

            return new WhereClause(new Criteria(column, valueEntityIds, operator.convertToMultiValueOperator()));
        } else {
            return new WhereClause(new Criteria(column, criteriaValues.stream().findAny().get().getValueEntityId(), operator));
        }
    }

    /**
     * <p>
     * Fetches the {@link Column} of the given field.
     * </p>
     *
     * @param fieldTable   Represents the table of the column.
     * @param fieldInfo    Represents the details of the field.
     * @param filterOption Represents the option of the filter.
     * @return Field {@link Column}.
     */
    protected Column getFieldColumn(final Table fieldTable, final FieldInfo fieldInfo, final FilterOptions filterOption) {
        return fieldTable.getColumn(fieldInfo.columnName());
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
     * @param dateInterval Holds the details of the date interval value.
     * @param operator     It is used to compare the given value with the given respective column.
     * @param column       It is used to filter the entities as per the given value.
     * @return {@link WhereClause} holds the details of retrieval criteria.
     */
    protected WhereClause buildDateFieldCriteria(final Interval<Long, Long> dateInterval, final Operator operator, final Column column) {
        final Long start = dateInterval.start();
        final Long end = dateInterval.end();

        if (Objects.nonNull(start) || Objects.nonNull(end)) {
            return switch (operator) {
                case EQUAL -> buildEqualOperatorCriteria(column, start, end);
                case NOT_EQUAL -> new WhereClause(Objects.isNull(start) ? Criteria.gt(column, end) : Objects.isNull(end)
                        ? Criteria.lt(column, start) : Criteria.notBetween(column, start, end));
                case LESS_THAN -> buildLessThanOperatorCriteria(column, start).orElseGet(() -> buildNegligibleCriteria(column, start, end));
                case GREATER_THAN -> buildGreaterThanOperatorCriteria(column, end).orElseGet(() -> buildNegligibleCriteria(column, start, end));
                case LESS_EQUAL -> {
                    final WhereClause equalOperatorCriteria = buildEqualOperatorCriteria(column, start, end);

                    yield buildLessThanOperatorCriteria(column, start).map(equalOperatorCriteria::or).orElse(equalOperatorCriteria);
                }
                case GREATER_EQUAL -> {
                    final WhereClause equalOperatorCriteria = buildEqualOperatorCriteria(column, start, end);

                    yield buildGreaterThanOperatorCriteria(column, end).map(equalOperatorCriteria::or).orElse(equalOperatorCriteria);
                }

                default -> null;
            };
        }
        return new WhereClause(new Criteria(column, null, operator));
    }

    /**
     * Builds a {@link WhereClause} that identifies records considered "negligible"
     * within a given range, based on the specified column and primary key exclusion logic.
     * <p>
     * This method constructs an equality-based filter for the provided column
     * within the given {@code start} and {@code end} values, and then excludes
     * matching primary keys from the results using a subquery.
     *
     * <p><b>Detailed Logic:</b></p>
     * <ol>
     *   <li>Retrieve the table and its primary key column from the provided {@link Column}.</li>
     *   <li>Create a base {@link WhereClause} using {@link #buildEqualOperatorCriteria(Column, Long, Long)}
     *       to match rows within the given range.</li>
     *   <li>Construct a subquery selecting the primary key column from the same table,
     *       applying the same base filter conditions.</li>
     *   <li>Return a composite {@link WhereClause} that adds a <i>NOT IN</i> condition
     *       to exclude records already identified by the subquery.</li>
     * </ol>
     *
     * <p>This method is typically used to detect or filter out negligible records —
     * i.e., records that fall within a minimal or insignificant value range —
     * while ensuring duplicates or previously processed entries are ignored.</p>
     *
     * @param column the column to apply the negligible range filter on.
     * @param start  the lower bound of the range (inclusive).
     * @param end    the upper bound of the range (inclusive).
     * @return a {@link WhereClause} combining range and exclusion conditions.
     */
    private WhereClause buildNegligibleCriteria(final Column column, final Long start, final Long end) {
        final WhereClause whereClause = buildEqualOperatorCriteria(column, start, end);

        return whereClause.and(whereClause.not());
    }

    /**
     * <p>
     * Constructs a {@link Criteria} representing an equality comparison between the specified column and a range of values.
     * </p>
     *
     * @param column The column to apply the filter on.
     * @param start  The starting value of the range.
     * @param end    The ending value of the range.
     * @return {@link WhereClause} containing the retrieval criteria details.
     */
    public WhereClause buildEqualOperatorCriteria(final Column column, final Long start, final Long end) {
        return new WhereClause(Objects.isNull(start) ? Criteria.le(column, end) : Objects.isNull(end)
                ? Criteria.ge(column, start) : Criteria.between(column, start, end));
    }

    /**
     * <p>
     * Constructs a {@link Criteria} representing a greater-than comparison for the specified column and a range of values.
     * </p>
     *
     * @param column The column to apply the filter on.
     * @param end    The ending value of the range.
     * @return {@link WhereClause} containing the retrieval criteria details.
     */
    public Optional<WhereClause> buildGreaterThanOperatorCriteria(final Column column, final Long end) {
        return Optional.ofNullable(end).map(endValue -> new WhereClause(Criteria.gt(column, endValue)));
    }

    /**
     * <p>
     * Constructs a {@link Criteria} representing a less-than comparison for the specified column and a range of values.
     * </p>
     *
     * @param column The column to apply the filter on.
     * @param start  The starting value of the range.
     * @return {@link WhereClause} containing the retrieval criteria details.
     */
    public Optional<WhereClause> buildLessThanOperatorCriteria(final Column column, final Long start) {
        return Optional.ofNullable(start).map(startValue -> new WhereClause(Criteria.lt(column, startValue)));
    }

    /**
     * <p>
     * Builds the joins for the required field and append it into {@link SelectQuery} using {@link JoinsLoader}.
     * <p>
     * NOTE: Please implement this method if you want to apply criteria for which field is not present in
     * main table of the current source.
     * (eg: Source - Contact, Other table fields - Email value, Email Type, Phone value, Phone Type).
     * </p>
     *
     * @param filterCriteriaDetails Helps to manage the joins data information.
     */
    protected void appendJoinsBasedOnField(final FilterCriteriaDetails filterCriteriaDetails) {
    }

    /**
     * <p>
     * Fetches the alias table of the given {@link Table}.
     * </p>
     *
     * @param table It provides the name of the table.
     * @param alias Helps to create unique table instance for fields.
     * @return Alias table of the given {@link Table}.
     */
    protected final Table getAliasTable(final Table table, final String alias) {
        return TableUtils.getAliasTable(table, alias, OltpTableResolver.get());
    }

    /**
     * <p>
     * Converts the time string(11:30 AM) into UTC milliseconds.
     * </p>
     *
     * @param value Time value as string (eg: 11:30 AM).
     * @return time value as {@link Long}(in milliseconds).
     */
    protected final long parseTime(final String value) {
        try {
            return TIME_FORMATTER.parse(value).getTime();
        } catch (ParseException e) {
            throw ErrorCode.get(FilterErrorCodes.UNABLE_TO_PARSE_TIME, e.getMessage());
        }
    }

    /**
     * <p>
     * Builds the {@link WhereClause} for territory filter
     * </p>
     *
     * @param isValueNeeded A flag that indicates whether the value is needed for comparator or not.
     * @param column        Represents the {@link Column}
     * @param valueEntityId Represents the value entity id
     * @param value         Represents the value
     * @param operator      Represents the {@link Operator}
     * @return the {@link WhereClause}
     */
    protected final WhereClause buildTerritoryWhereClause(final boolean isValueNeeded, final boolean isMultiValueAllowed,
                                                          final Column column, final Long valueEntityId,
                                                          final Object value, final Operator operator,
                                                          final Collection<CriteriaValue> criteriaValues) {

        if (Objects.isNull(valueEntityId) && isValueNeeded) {
            if (Objects.isNull(value)) {
                return new WhereClause(Criteria.eq(getTable().getPKColumn(), null));
            }
            return new WhereClause(new Criteria(column, value, operator));
        }

        if (isMultiValueAllowed) {
            return fieldCriteriaBuilder.buildCriteria(column, true, operator, criteriaValues);
        }
        return new WhereClause(new Criteria(column, valueEntityId, operator));
    }

    /**
     * <p>
     * Loads the joins based on the provider and the selected sources are irrelevant or not.
     * <p>
     * NOTE:
     * irrelevantSources - It contains the irrelevant sources of the current provider source.
     * mapperSource - Holds the mapper source of the current provider source. It is used to join the required
     * irrelevant source into the current provider source.
     * (Eg: current provider source - Product; irrelevant sources - Contact, Company, Activity; mapper source - Deal)
     * </p>
     *
     * @param providerSource        Refers to the source of the current provider. It is used to fetch the corresponding
     *                              irrelevant sources and mapper source from the “FilterActionContainer”.
     *                              (NOTE: SourceLinkGroup - The combination of current, irrelevant and mapper sources)
     * @param filterCriteriaDetails Helps to manage the joins data information.
     */
    public final void loadJoins(final FilterSourceType providerSource, final FilterCriteriaDetails filterCriteriaDetails) {
        final Table table = getTable();
        final SourceHolder sourceHolder = filterCriteriaDetails.getSourceHolder();
        final FilterSourceType selectedSource = sourceHolder.source();
        final Collection<SourceLinkGroup> sourceLinkGroups = filterActionContainer.fetchSourceLinkGroups(providerSource);
        final Table baseTable;

        if (Objects.isNull(sourceLinkGroups)) {
            baseTable = table;
        } else {
            final Map<FilterSourceType, FilterSourceType> irrelevantSources = new HashMap<>();

            for (final SourceLinkGroup sourceLinkGroup : sourceLinkGroups) {
                irrelevantSources.put(sourceLinkGroup.getIrrelevantSource(), sourceLinkGroup.getMapperSource());
            }

            if (irrelevantSources.containsKey(selectedSource)) {
                final FilterAction filterAction = filterActionContainer.fetchSourceAction(irrelevantSources.get(selectedSource));
                baseTable = filterAction.getTable();

                filterAction.loadJoins(table, filterCriteriaDetails, getLocalMapperTables());
            } else {
                baseTable = table;
            }
        }
        loadJoins(providerSource, baseTable, filterCriteriaDetails);
    }

    /**
     * <p>
     * Fetches the mapper tables of the current source. It is used when the current source table and the given
     * source table are relatively independent.
     * <p>
     * (Eg: The Deal and Activity tables has no direct relations between them so the mapper tables are introduced here,
     * which is used to make relationships between them. If Deal and Activity tables need to join, then Deal table has
     * to be joined to the mapper table and then to Activity table.)
     * </p>
     *
     * @return array of {@link Table} holds the mapper tables of current source.
     */
    protected Table[] getLocalMapperTables() {
        return null;
    }

    /**
     * <p>
     * Loads the joins for the related source type if the related source type is available.
     * <p>
     * (eg: the selected source and field are “Deal” and “Activities to do”. In this case, “Activities to do” is a field
     * of “Activity” source, so the “Activity” source also needs to be joined).
     * </p>
     *
     * @param localTable            Refers to the base table of the current source, which is used to append the joins
     *                              after the already joined tables.
     * @param localMapperTables     Refers to the mapper tables of the current source. It is used to make joins for
     *                              the current and previous sources if those source tables have no direct relations.
     * @param filterCriteriaDetails Helps to manage the joins data information.
     */
    protected final void loadJoinsBasedOnRelatedSourceType(final Table localTable, final Table[] localMapperTables,
                                                           final FilterCriteriaDetails filterCriteriaDetails) {
        final SourceHolder sourceHolder = filterCriteriaDetails.getSourceHolder();
        final FilterSourceType relatedSourceType = Objects.isNull(sourceHolder) ? null : sourceHolder.relatedSource();

        if (Objects.nonNull(relatedSourceType)) {
            final FilterAction filterAction = filterActionContainer.fetchSourceAction(relatedSourceType);

            if (!Objects.equals(this, filterAction)) {
                filterAction.loadJoins(localTable, filterCriteriaDetails, localMapperTables);
            }
        }
    }

    /**
     * <p>
     * Builds the {@link WhereClause} based on the additional option.
     * </p>
     *
     * @param table              Refers the field type table of the corresponding subtype. (eg: OMETEmail table)
     * @param additionalOptionId Refers the subtype id. It is used to fetch the data for the respective column.
     *                           (eg: id refers to the subtype of the email, that subtype name is one of the column name(EMAIL_TYPE) of OMETEmail table)
     * @return {@link WhereClause}  holds the details about retrieval criteria.
     */
    protected final Column fetchFieldSubTypeColumn(final Table table, final Long additionalOptionId) {
        final FieldSubType fieldSubType = fieldTypeService.getSubType(additionalOptionId);

        if (Objects.isNull(fieldSubType)) {
            return null;
        }
        final String subTypeColumn = ((FieldTypeServiceImpl) fieldTypeService).getSubTypeColumn(fieldSubType);

        return table.getColumn(subTypeColumn);
    }

    /**
     * <p>
     * Builds the {@link WhereClause} for the multi select fields.
     * </p>
     *
     * @param fieldId        Represents the field id.
     * @param remoteTable    Represents the remote table of the field.
     * @param mapperTable    Represents the mapper table of the field table.
     * @param column         Represents the main {@link Column} of the criteria.
     * @param comparator     Represents the {@link Comparator} to compare the value of given column and given value.
     * @param criteriaValues Represents the values to build criteria.
     * @return Multi select field {@link WhereClause}.
     */
    protected final WhereClause buildFieldCriteria(final Long fieldId, final Table remoteTable, final Table mapperTable,
                                                   final Column column, final Comparator comparator, final Collection<CriteriaValue> criteriaValues) {
        return fieldCriteriaBuilder.build(fieldId, remoteTable, mapperTable, column, comparator, criteriaValues);
    }

    /**
     * <p>
     * Sets the {@link NullableFieldCriteriaAppenderRequisites} into {@link NullableFieldCriteriaAppender}.
     * </p>
     *
     * @param criteriaRequisites     Represents the {@link NullableFieldCriteriaAppenderRequisites}.
     * @param criteriaAppenderHolder Holds the post-criteria appender.
     */
    protected final void setRequisitesIntoNullableFieldCriteriaAppender(final NullableFieldCriteriaAppenderRequisites criteriaRequisites,
                                                                        final CriteriaAppenderHolder criteriaAppenderHolder) {
        criteriaAppenderHolder.get(NullableFieldCriteriaAppender.TYPE).setCriteriaRequisites(criteriaRequisites);
    }

    /**
     * <p>
     * Sets the {@link CountCriteriaAppenderRequisites} into {@link CountCriteriaAppender}.
     * </p>
     *
     * @param criteriaRequisites     Represents the {@link CountCriteriaAppenderRequisites}.
     * @param criteriaAppenderHolder Holds the post-criteria appender.
     */
    protected final void setRequisitesIntoCountCriteriaAppender(final CountCriteriaAppenderRequisites criteriaRequisites,
                                                                final CriteriaAppenderHolder criteriaAppenderHolder) {
        criteriaAppenderHolder.get(CountCriteriaAppender.TYPE).setCriteriaRequisites(criteriaRequisites);
    }

    /**
     * <p>
     * Sets the {@link MinMaxValueCriteriaAppenderRequisites} into {@link MaxValueCriteriaAppender}.
     * </p>
     *
     * @param criteriaRequisites     Represents the {@link MinMaxValueCriteriaAppenderRequisites}.
     * @param criteriaAppenderHolder Holds the post-criteria appender.
     */
    protected final void setRequisitesIntoMaxValueCriteriaAppender(final MinMaxValueCriteriaAppenderRequisites criteriaRequisites,
                                                                   final CriteriaAppenderHolder criteriaAppenderHolder) {
        criteriaAppenderHolder.get(MaxValueCriteriaAppender.TYPE).setCriteriaRequisites(criteriaRequisites);
    }

    /**
     * <p>
     * Sets the {@link MinMaxValueCriteriaAppenderRequisites} into {@link MinValueCriteriaAppender}.
     * </p>
     *
     * @param criteriaRequisites     Represents the {@link MinMaxValueCriteriaAppenderRequisites}.
     * @param criteriaAppenderHolder Holds the post-criteria appender.
     */
    protected final void setRequisitesIntoMinValueCriteriaAppender(final MinMaxValueCriteriaAppenderRequisites criteriaRequisites,
                                                                   final CriteriaAppenderHolder criteriaAppenderHolder) {
        criteriaAppenderHolder.get(MinValueCriteriaAppender.TYPE).setCriteriaRequisites(criteriaRequisites);
    }

    /**
     * <p>
     * Sets the {@link DynamicFieldEmptyValueCriteriaAppenderRequisites} into {@link DynamicFieldEmptyValueCriteriaAppender}.
     * </p>
     *
     * @param criteriaRequisites     Represents the {@link DynamicFieldEmptyValueCriteriaAppenderRequisites}.
     * @param criteriaAppenderHolder Holds the post-criteria appender.
     */
    protected final void setRequisitesIntoEmptyValueCriteriaAppender(final DynamicFieldEmptyValueCriteriaAppenderRequisites criteriaRequisites,
                                                                     final CriteriaAppenderHolder criteriaAppenderHolder) {
        criteriaAppenderHolder.get(DynamicFieldEmptyValueCriteriaAppender.TYPE).setCriteriaRequisites(criteriaRequisites);
    }

    /**
     * <p>
     * Builds the {@link WhereClause} for the tag field.
     * </p>
     *
     * @param operator       Represents the {@link Operator} to compare the value of given column and given value.
     * @param valueEntityIds Represents the values to build criteria
     * @param tagColumn      Represents the tag column
     * @param entityIdColumn Represents the entity id column
     * @return Multi select field {@link WhereClause}.
     */
    protected final WhereClause buildTagFieldCriteria(final Table tagMappingTable, final Column entityIdColumn, final Column tagColumn,
                                                      final Collection<Long> valueEntityIds, final Operator operator) {
        if (valueEntityIds.isEmpty()) {
            return Operator.EQUAL == operator ? fieldCriteriaBuilder.buildInvertedOperatorCriteria(null, tagMappingTable, null, entityIdColumn)
                    .or(new WhereClause(Criteria.eq(entityIdColumn, null))) : new WhereClause(Criteria.notEq(tagMappingTable.getPKColumn(), null));
        }
        return fieldCriteriaBuilder.buildMultiSelectFieldCriteria(tagMappingTable, entityIdColumn, tagColumn, valueEntityIds, operator);
    }

    /**
     * <p>
     * Load the joins depends on related source type when the provider and selected sources are same, Otherwise
     * load the joins between of them.
     * <p>
     * (eg: Source - Contact, Selected field - First name, in this case there is no need for joins because it is already
     * present in the existing joined table).
     * <p>
     * NOTE:
     * relatedSourceType - A source which is related to the filter option.
     * (eg. source is Deal and the filter option is Activities to do, in this case the filter option is depending on Activity source)
     * </p>
     *
     * @param providerSource        Refers to the source of the current provider. It is used to fetch the corresponding
     *                              irrelevant sources and mapper source from the “FilterActionContainer”.
     * @param baseTable             Refers to the table of the current provider source, which is used to append the joins
     *                              after the already joined tables.
     * @param filterCriteriaDetails Helps to manage the joins data information.
     */
    private void loadJoins(final FilterSourceType providerSource, final Table baseTable, final FilterCriteriaDetails filterCriteriaDetails) {
        final Table[] localMapperTables = getLocalMapperTables();
        final SourceHolder sourceHolder = filterCriteriaDetails.getSourceHolder();
        final FilterSourceType selectedSource = sourceHolder.source();

        if (Objects.equals(providerSource, selectedSource)) {
            final FilterSourceType relatedSourceType = sourceHolder.relatedSource();

            if (Objects.nonNull(relatedSourceType) && !Objects.equals(relatedSourceType, selectedSource)) {
                final FilterAction filterAction = filterActionContainer.fetchSourceAction(relatedSourceType);

                filterAction.loadJoins(baseTable, filterCriteriaDetails, localMapperTables);
            }
        } else {
            final FilterAction filterAction = filterActionContainer.fetchSourceAction(selectedSource);

            filterAction.loadJoins(baseTable, filterCriteriaDetails, localMapperTables);
        }
    }

    /**
     * <p>
     * Builds the {@link WhereClause} for the multi select fields.
     * </p>
     *
     * @param fieldId        Represents the field id.
     * @param remoteTable    Represents the remote table of the field.
     * @param mapperTable    Represents the mapper table of the field table.
     * @param column         Represents the main {@link Column} of the criteria.
     * @param comparator     Represents the {@link Comparator} to compare the value of given column and given value.
     * @param criteriaValues Represents the values to build criteria.
     * @param dateFormatter  Represents the {@link SimpleDateFormat} to format the given date values.
     * @return Multi select field {@link WhereClause}.
     */
    private WhereClause buildDateFieldCriteria(final Long fieldId, final Table remoteTable, final Table mapperTable,
                                               final Column column, final Comparator comparator, final Collection<CriteriaValue> criteriaValues,
                                               final SimpleDateFormat dateFormatter) throws ParseException {
        if (Objects.nonNull(criteriaValues)) {
            for (final CriteriaValue criteriaValue : criteriaValues) {
                final Object value = criteriaValue.getValue();

                criteriaValue.setValue(Objects.isNull(value) ? null : dateFormatter.parse((String) value).getTime());
            }
        }
        return buildFieldCriteria(fieldId, remoteTable, mapperTable, column, comparator, criteriaValues);
    }

    /**
     * <p>
     * Builds the {@link WhereClause} for persistent complex field types (MONETARY, EMAIL, PHONE, ADDRESS).
     * For persistent fields, the FK ID to the value table is stored directly in the wide table column
     * This method joins the value table from the wide table column and builds criteria on the value table's data column.
     * </p>
     *
     * @param valueTable     The value table (e.g., OMETMonetary, OMETEmail).
     * @param valueColumnName The column name in the value table to filter on (e.g., VALUE, NUMBER).
     * @param fkColumn       The FK column in the wide table (e.g., MONETARY_ID1).
     * @param comparator     The comparator for the filter.
     * @param criteriaValues The filter values.
     * @return {@link WhereClause}.
     */
    private WhereClause buildPersistentFieldCriteria(final Table valueTable, final String valueColumnName, final Column fkColumn,
            final Comparator comparator, final Collection<CriteriaValue> criteriaValues) {
        final Operator operator = Operator.getOperator(comparator.getAliasName());
        final SelectQuery selectQuery = SelectQuery.get(valueTable);

        selectQuery.setWhereClause(fieldCriteriaBuilder.buildCriteria(valueTable.getColumn(valueColumnName), comparator.isMultiValueAllowed(), operator, criteriaValues));
        selectQuery.addSelectClause(valueTable.getPKColumn());

        if (Objects.isNull(criteriaValues) || criteriaValues.isEmpty()) {
            return new WhereClause(new Criteria(fkColumn, null, operator));
        }
        return new WhereClause(Criteria.in(fkColumn, selectQuery));
    }

    /**
     * <p>
     * Builds the {@link WhereClause} for persistent date and time related field types (DATE_RANGE, TIME_RANGE, DATE_TIME_RANGE).
     * </p>
     *
     * @param valueTable      The value table (e.g., OMETDateRange).
     * @param valueColumnName The column name in the value table to filter on.
     * @param fkColumn        The FK column in the wide table.
     * @param comparator      The comparator for the filter.
     * @param criteriaValues  The filter values.
     * @param dateFormatter   The date formatter.
     * @return {@link WhereClause}.
     */
    private WhereClause buildPersistentDateFieldCriteria(final Table valueTable, final String valueColumnName, final Column fkColumn, final Comparator comparator,
            final Collection<CriteriaValue> criteriaValues, final SimpleDateFormat dateFormatter) {
        if (Objects.nonNull(criteriaValues)) {

            for (final CriteriaValue criteriaValue : criteriaValues) {
                final Object value = criteriaValue.getValue();

                try {
                    criteriaValue.setValue(Objects.isNull(value) ? null : dateFormatter.parse((String) value).getTime());
                } catch (ParseException e) {
                    throw ErrorCode.get(FilterErrorCodes.UNABLE_TO_PARSE_DATE_TIME, e.getMessage());
                }
            }
        }
        return buildPersistentFieldCriteria(valueTable, valueColumnName, fkColumn, comparator, criteriaValues);
    }

    /**
     * <p>
     * Builds the {@link WhereClause} for persistent MULTI_SELECT fields. values are stored as BIGINT[] array columns
     * </p>
     *
     * @param arrayColumn    The BIGINT[] array column in the wide table.
     * @param operator       The operator for the filter.
     * @param valueEntityIds The values to filter on.
     * @return {@link WhereClause}.
     */
    private WhereClause buildPersistentMultiSelectCriteria(final Column arrayColumn, final Operator operator,
                                                           final Collection<Long> valueEntityIds) {
        if (Objects.isNull(valueEntityIds) || valueEntityIds.isEmpty()) {
            return Operator.EQUAL == operator ? new WhereClause(new Criteria(arrayColumn, null, Operator.EQUAL))
                    : new WhereClause(new Criteria(arrayColumn, null, Operator.NOT_EQUAL));
        }
        return new WhereClause(new Criteria(arrayColumn, valueEntityIds, operator == Operator.EQUAL ? Operator.IN : Operator.NOT_IN));
    }

    /**
     * <p>
     * Loads the joins between the base and local tables. Need to use mapper table for join the base and local tables
     * when no direct relations between them.
     * </p>
     *
     * @param baseTable             Refers to the base table of the current provider source, which is used to append the
     *                              joins after the already joined tables.
     * @param localTable            Refers to the base table of the current source, which is used to append the joins
     *                              after the already joined tables.
     * @param filterCriteriaDetails Helps to manage the joins data information.
     * @param joinPriorityDetails   Holds the priority data of the join.
     * @param mapperTables          Refers to the mapper tables of the previous {@link FilterAction}. It is used to
     *                              make joins for the current and previous sources if those source tables have no direct relations.
     * @param allMapperTables       Holds the mapper tables of the current provider source and current source.
     *                              It is used to neglect the already joined table.
     * @param joinStatusHolder      Holds the status of the join. It is used to identify the joins are loaded or not.
     */
    private void loadJoins(final Table baseTable, final Table localTable, final Collection<Table> mapperTables,
                           final Collection<Table> allMapperTables, final JoinStatusHolder joinStatusHolder,
                           final FilterCriteriaDetails filterCriteriaDetails, final JoinPriorityDetails joinPriorityDetails) {
        final String localTableName = localTable.getName();
        final Map<String, Column> localTableFkRemoteColumns = TableUtils.groupFkColumnsByTableName(localTable);
        final Map<String, Column> baseTableFkRemoteColumns = TableUtils.groupFkColumnsByTableName(baseTable);
        final Table[] localMapperTables = getLocalMapperTables();
        final JoinPreference joinPreference = joinPriorityDetails.getJoinPriorityType();
        final boolean hasMainTablePriority = joinPreference.hasMainTablePriority();
        final boolean hasMapperTablePriority = joinPreference.hasMapperTablePriority();
        final JoinsLoader joinsLoader = filterCriteriaDetails.getJoinsLoader();

        if (localTableFkRemoteColumns.containsKey(baseTable.getName()) && hasMainTablePriority) {
            final FieldInfo fieldInfo = filterCriteriaDetails.getFieldInfo();
            final boolean needAliasNameUsage = filterCriteriaDetails.useAliasTable();
            final String aliasName = needAliasNameUsage && Objects.nonNull(fieldInfo) ? fieldInfo.aliasName() : null;

            loadJoinsBasedOnLocalTable(aliasName, baseTable, localTable, joinsLoader, allMapperTables, localTableFkRemoteColumns, joinStatusHolder);
        } else if (baseTableFkRemoteColumns.containsKey(localTableName) && hasMainTablePriority) {
            joinsLoader.load(baseTableFkRemoteColumns.get(localTableName), localTable.getPKColumn());

            joinStatusHolder.setJoinsLoaded(true);
        } else if (!joinStatusHolder.isMapperTableJoinsAreLoaded() && (!mapperTables.isEmpty() ||
                (Objects.nonNull(localMapperTables) && localMapperTables.length > 0)) && hasMapperTablePriority) {
            loadJoinsFromMapperTables(baseTable, mapperTables, localMapperTables, joinStatusHolder, filterCriteriaDetails, joinPriorityDetails);
        }

        if (joinStatusHolder.isJoinsLoaded() && !allMapperTables.contains(Table.get(localTable.getName()))) {
            loadJoinsBasedOnRelatedSourceType(localTable, localMapperTables, filterCriteriaDetails);
        }
    }

    /**
     * <p>
     * Loads the joins based on local table, joins the base and local tables and joins the local and main tables
     * if the local table is mapper table.
     * <p>
     * (Eg: The Deal and Activity tables have no direct relations between them so the mapper tables are introduced here,
     * which is used to make relationships between them. If Deal and Activity tables need to join, then Deal table has
     * to be joined to the mapper table and then to Activity table.)
     * </p>
     *
     * @param aliasName                 Represents the alias name of the main table.
     * @param baseTable                 Refers to the table of the current provider source, which is used to append
     *                                  the joins after the already joined tables.
     * @param localTable                Refers to the base table of the current source, which is used to append
     *                                  the joins after the already joined tables.
     * @param joinsLoader               Helps to add the {@link JoinClause} into {@link SelectQuery}.
     * @param allMapperTables           Holds the mapper tables of the current provider source and current source.
     *                                  It is used to neglect the already joined table.
     * @param localTableFkRemoteColumns Foreign remote columns of the local table.
     */
    private void loadJoinsBasedOnLocalTable(final String aliasName, final Table baseTable, final Table localTable, final JoinsLoader joinsLoader,
                                            final Collection<Table> allMapperTables, final Map<String, Column> localTableFkRemoteColumns,
                                            final JoinStatusHolder joinStatusHolder) {
        final Column baseTablePkColumn = baseTable.getPKColumn();

        if (allMapperTables.contains(Table.get(localTable.getName()))) {
            final Table mainTable = getTable();
            final String mainTableName = mainTable.getName();

            if (localTableFkRemoteColumns.containsKey(mainTableName)) {
                final Table mainAliasTable = Objects.isNull(aliasName) ? mainTable : getAliasTable(mainTable, aliasName);

                joinsLoader.load(baseTablePkColumn, localTableFkRemoteColumns.get(baseTable.getName()));
                joinsLoader.load(localTableFkRemoteColumns.get(mainTableName), mainAliasTable.getPKColumn());

                joinStatusHolder.setJoinsLoaded(true);
            }
        } else {
            joinsLoader.load(baseTablePkColumn, localTableFkRemoteColumns.get(baseTable.getName()));

            joinStatusHolder.setJoinsLoaded(true);
        }
    }

    /**
     * <p>
     * Builds the joins based on the field type and append it into {@link SelectQuery} using {@link JoinsLoader}.
     * </p>
     *
     * @param fieldAlias            Represents the alias name of the field.
     * @param fieldInfo             {@link FieldInfo} that holds the field information.
     * @param joinsLoader           Helps to add the {@link JoinClause} into {@link SelectQuery}.
     * @param filterCriteriaDetails {@link FilterCriteriaDetails} that holds the field table for persistent fields.
     */
    private void appendJoinsBasedOnFieldType(final String fieldAlias, final FieldInfo fieldInfo, final JoinsLoader joinsLoader,
                                             final FilterCriteriaDetails filterCriteriaDetails) {
        final Table table = getTable();

        if (fieldInfo.isPersistent()) {
            final Table persistentFieldTable = Objects.nonNull(filterCriteriaDetails) && Objects.nonNull(filterCriteriaDetails.getFieldTable())
                    ? filterCriteriaDetails.getFieldTable() : fieldInfo.table();
            joinsLoader.load(table.getPKColumn(), persistentFieldTable.getColumn(ENTITY_ID));
            return;
        }

        final Table fieldTable = getAliasTable(fieldInfo.table(), fieldAlias);
        final Map<Table, Column> fieldFkColumns = TableUtils.groupFkColumnsByTable(fieldTable, OltpTableResolver.get());
        final StoreType storeType = fieldInfo.storeType();
        final Column remoteColumn = getRemoteColumn(fieldTable, storeType, fieldFkColumns);

        if (Objects.nonNull(remoteColumn) && (fieldInfo.isPersistent() || (fieldInfo.presetConfig().hasMapperTable()
                && !StoreType.isSingleValueStore(storeType)))) {
            joinsLoader.load(table.getPKColumn(), remoteColumn);

            switch (storeType) {
                case PHONE, PHONES -> appendJoins(phoneManager.getTable(), fieldAlias, joinsLoader, fieldFkColumns);
                case EMAIL, EMAILS -> appendJoins(emailManager.getTable(), fieldAlias, joinsLoader, fieldFkColumns);
                case MONETARY, DEAL_VALUE ->
                        appendJoins(monetaryManager.getTable(), fieldAlias, joinsLoader, fieldFkColumns);
                case TIME_RANGE -> appendJoins(timeRangeManager.getTable(), fieldAlias, joinsLoader, fieldFkColumns);
                case DATE_RANGE -> appendJoins(dateRangeManager.getTable(), fieldAlias, joinsLoader, fieldFkColumns);
                case DATE_TIME_RANGE ->
                        appendJoins(dateTimeRangeManager.getTable(), fieldAlias, joinsLoader, fieldFkColumns);
                case ADDRESS -> appendJoins(addressManager.getTable(), fieldAlias, joinsLoader, fieldFkColumns);
                case OWNER -> appendJoins(Table.get(OMCRMUSER.TABLE), fieldAlias, joinsLoader, fieldFkColumns);
                //TODO If we add an extra field type, implement joins if any joins are needed.
            }
        }
    }

    /**
     * <p>
     * Gets the remote column from the table When the field has same store type and source
     * </p>
     *
     * @param fieldTable     Represents the field table
     * @param storeType      Represents the {@link StoreType}
     * @param fieldFkColumns Map containing key as Table and value fk columns
     * @return The Remote column
     */
    protected Column getRemoteColumn(final Table fieldTable, final StoreType storeType, final Map<Table, Column> fieldFkColumns) {
        return fieldFkColumns.get(getTable());
    }

    /**
     * <p>
     * Appends the joins for the given table using {@link JoinsLoader}.
     * </p>
     *
     * @param table          It is used to be joined with mapper table.
     * @param joinsLoader    Helps to add the {@link JoinClause} into {@link SelectQuery}.
     * @param fieldFkColumns Refers the mapper table fk columns.
     */
    private void appendJoins(final Table table, final String fieldAlias, final JoinsLoader joinsLoader,
                             final Map<Table, Column> fieldFkColumns) {
        joinsLoader.load(fieldFkColumns.get(table), getAliasTable(table, fieldAlias).getPKColumn());
    }

    /**
     * <p>
     * Loads the required joins from mapper tables.
     * <p>
     * (eg: if the base table is contact and the local table is activity, In this case, relation between of them is
     * not directly, which uses mapper table to join them(mapperTable - ContactActivity table)).
     * </p>
     *
     * @param baseTable             Refers to the table of the current provider source, which is used to append
     *                              the joins after the already joined tables.
     * @param mapperTables          Refers to the mapper tables of the previous {@link FilterAction}. It is used to make
     *                              joins for the current and previous sources if those source tables have no direct relations.
     * @param localMapperTables     Refers to the mapper tables of the current source. It is used to make joins for
     *                              the current and previous sources if those source tables have no direct relations.
     * @param filterCriteriaDetails Helps to manage the joins data information.
     * @param joinPriorityDetails   Holds the priority data of the join.
     * @param joinStatusHolder      Holds the status of the join. It is used to identify the joins are loaded or not.
     */
    private void loadJoinsFromMapperTables(final Table baseTable, final Collection<Table> mapperTables, final Table[] localMapperTables,
                                           final JoinStatusHolder joinStatusHolder, final FilterCriteriaDetails filterCriteriaDetails,
                                           final JoinPriorityDetails joinPriorityDetails) {
        final List<Table> allMapperTables = new ArrayList<>(mapperTables);
        final JoinPriorityDetails priorityDetailHolder = new JoinPriorityDetails(JoinPreference.ANY);
        final Collection<Table> priorityMapperTables = joinPriorityDetails.getMapperTables();

        joinStatusHolder.setMapperTableJoinsAreLoaded(true);

        if (Objects.nonNull(localMapperTables) && localMapperTables.length > 0) {
            allMapperTables.addAll(List.of(localMapperTables));
        }

        if (Objects.isNull(priorityMapperTables)) {

            for (final Table mapperTable : allMapperTables) {

                if (joinStatusHolder.isJoinsLoaded()) {
                    break;
                }
                loadJoins(baseTable, mapperTable, Collections.emptyList(), allMapperTables, joinStatusHolder, filterCriteriaDetails, priorityDetailHolder);
            }
        } else {

            for (final Table mapperTable : allMapperTables) {

                if (priorityMapperTables.contains(mapperTable)) {
                    loadJoins(baseTable, mapperTable, Collections.emptyList(), allMapperTables, joinStatusHolder, filterCriteriaDetails, priorityDetailHolder);
                }
            }
        }
    }

    /**
     * <p>
     * Fetches the date interval value for {@link CriteriaValue}.
     * </p>
     *
     * @param criteriaValue holds the details of criteria value.
     * @return {@link Interval} holds the intervals of date.
     */
    private Interval<Long, Long> fetchDateInterval(final CriteriaValue criteriaValue) {
        final Long valueEntityId = criteriaValue.getValueEntityId();

        if (Objects.isNull(valueEntityId)) {
            final long time;

            try {
                time = DATE_FORMATTER.parse((String) criteriaValue.getValue()).getTime();
            } catch (ParseException e) {
                throw ErrorCode.get(FilterErrorCodes.UNABLE_TO_PARSE_DATE, e.getMessage());
            }

            return new Interval<>(time, time - ONE + ONE_DAY_IN_MILLISECONDS);
        } else {
            return DateIntervals.valueOf(dateIntervalService.get(valueEntityId).getName()).getInterval();
        }
    }

    /**
     * <p>
     * Builds the criteria for the provided number value
     * </p>
     *
     * @param column   Represents the column of the table
     * @param value    Represents the number value
     * @param operator Represents the operator to compare
     * @return the where clause
     */
    protected final WhereClause buildCriteriaForNumberField(final Column column, final Object value, final Operator operator) {
        return new WhereClause(new Criteria(column, Objects.isNull(value) ? null : NumberUtils.parseNumber(value).longValue(), operator));
    }

    /**
     * <p>
     * Builds the {@link WhereClause} for a time field adjusted to the current user's regional timezone.
     * </p>
     * <p>
     * Converts the target column into milliseconds from the start of the day and filters across
     * a 1-minute window for granular comparisons.
     * </p>
     *
     * @param column   The {@link Column} representing the timestamp to be filtered.
     * @param value    The time string value (e.g., "11:30 AM") to compare against.
     * @param operator The {@link Operator} defining the comparison logic.
     * @return The constructed {@link WhereClause}.
     */
    protected final WhereClause buildCriteriaForTimeField(final Column column, final Object value, final Operator operator) {
        if (Objects.isNull(value)) {
            return new WhereClause(new Criteria(column, null, operator));
        }

        final long offsetMs = getUserOffsetMs();
        final long timeOfDay = parseTimeOfDay((String) value);
        final Column timeOfDayColumn = toTimeOfDayColumn(column, offsetMs);
        final long endOfMinute = timeOfDay + ONE_MINUTE_MS - NumericalConstant.ONE;

        return switch (operator) {
            case EQUAL -> new WhereClause(Criteria.between(timeOfDayColumn, timeOfDay, endOfMinute));
            case NOT_EQUAL -> new WhereClause(Criteria.notBetween(timeOfDayColumn, timeOfDay, endOfMinute));
            case LESS_EQUAL -> new WhereClause(Criteria.le(timeOfDayColumn, endOfMinute));
            case LESS_THAN -> new WhereClause(Criteria.lt(timeOfDayColumn, timeOfDay));
            case GREATER_EQUAL -> new WhereClause(Criteria.ge(timeOfDayColumn, timeOfDay));
            case GREATER_THAN -> new WhereClause(Criteria.gt(timeOfDayColumn, endOfMinute));
            default -> new WhereClause(new Criteria(timeOfDayColumn, timeOfDay, operator));
        };
    }

    /**
     * <p>
     * Transforms a timestamp column into an expression representing the time of day in milliseconds,
     * adjusted by the provided timezone offset using a SQL {@code MOD} function over 24 hours.
     * </p>
     *
     * @param column   The timestamp {@link Column} to transform.
     * @param offsetMs The timezone offset in milliseconds to adjust the timestamp.
     * @return A functional {@link Column} yielding the millisecond offset within the day.
     */
    private Column toTimeOfDayColumn(final Column column, final long offsetMs) {
        final SqlFunction.Builder modFunction = SqlFunction.Builder.create()
                .setName("MOD")
                .setReturnType(CommonDataType.BIGINT)
                .addArgument(resolver -> "")
                .addArgument(resolver -> String.valueOf(ONE_DAY_IN_MILLISECONDS));
        final Argument shiftedArgument = resolver -> "(" + resolver.apply(column) + " + " + offsetMs + ")";

        return column.function(modFunction, shiftedArgument, "TIME_OF_DAY", 0);
    }

    /**
     * <p>
     * Builds the {@link WhereClause} for persistent TIME_RANGE field types by querying the value table
     * with time-of-day comparison logic and matching against the foreign key column.
     * </p>
     *
     * @param valueTable      The value {@link Table} (e.g., OMETTimeRange).
     * @param valueColumnName The column name in the value table to filter on (e.g., FROM, TO).
     * @param fkColumn        The foreign key {@link Column} in the wide table.
     * @param operator        The {@link Operator} representing the filter condition.
     * @param criteriaValues  The collection of {@link CriteriaValue} to match.
     * @return The constructed {@link WhereClause}.
     */
    private WhereClause buildPersistentTimeRangeCriteria(final Table valueTable, final String valueColumnName, final Column fkColumn,
                                                         final Operator operator, final Collection<CriteriaValue> criteriaValues) {
        if (Objects.isNull(criteriaValues) || criteriaValues.isEmpty()) {
            return new WhereClause(new Criteria(fkColumn, null, operator));
        }

        final Object value = criteriaValues.stream().findAny().get().getValue();
        final SelectQuery selectQuery = SelectQuery.get(valueTable);

        selectQuery.setWhereClause(buildCriteriaForTimeField(valueTable.getColumn(valueColumnName), value, operator));
        selectQuery.addSelectClause(valueTable.getPKColumn());

        return new WhereClause(Criteria.in(fkColumn, selectQuery));
    }

    /**
     * <p>
     * Builds the {@link WhereClause} for non-persistent TIME_RANGE dynamic fields, converting time string
     * values into day-millisecond offsets and applying user timezone shifts.
     * </p>
     *
     * @param fieldId        The field ID of the dynamic field.
     * @param remoteTable    The remote {@link Table} storing the field value.
     * @param mapperTable    The mapper {@link Table} connecting the entity to the remote table.
     * @param column         The target {@link Column} to evaluate.
     * @param comparator     The {@link Comparator} for the condition.
     * @param criteriaValues The collection of {@link CriteriaValue} containing time strings.
     * @return The constructed {@link WhereClause}.
     */
    private WhereClause buildNonPersistentTimeRangeCriteria(final Long fieldId, final Table remoteTable, final Table mapperTable,
                                                            final Column column, final Comparator comparator, final Collection<CriteriaValue> criteriaValues) {
        if (Objects.nonNull(criteriaValues)) {
            for (final CriteriaValue criteriaValue : criteriaValues) {
                final Object value = criteriaValue.getValue();

                criteriaValue.setValue(Objects.isNull(value) ? null : parseTimeOfDay((String) value));
            }
        }

        final Column timeOfDayColumn = toTimeOfDayColumn(column, getUserOffsetMs());

        return buildFieldCriteria(fieldId, remoteTable, mapperTable, timeOfDayColumn, comparator, criteriaValues);
    }

    /**
     * <p>
     * Parses a time string (e.g., "11:30 AM") into elapsed milliseconds from the start of the day.
     * </p>
     *
     * @param value The formatted time string to parse.
     * @return The time of day in milliseconds.
     */
    private long parseTimeOfDay(final String value) {
        return LocalTime.parse(value.trim().toUpperCase(), TIME_PARSER).toSecondOfDay() * MILLIS_PER_SECOND;
    }

    /**
     * <p>
     * Retrieves the current logged-in user's regional timezone offset in milliseconds relative to UTC.
     * </p>
     *
     * @return The timezone offset in milliseconds.
     */
    private long getUserOffsetMs() {
        final CrmUser crmUser = LoggedInUserInfo.getUser();
        final UserRegionalPreference userPreference = crmUser.getPreferenceHolder().get(UserPreferenceType.USER_REGIONAL_PREFERENCE);

        return ZoneId.of(userPreference.getTimezoneInfo().getTimeZone().getID()).getRules().getOffset(Instant.now()).getTotalSeconds() * MILLIS_PER_SECOND;
    }
}