package com.tlc.crm.common.list;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.list.model.ConfiguredField;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.model.FieldConfigSource;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.i18n.I18nResolver;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DmlUtil;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.oltp.api.dml.KCharCriteria;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * <p>
 * Implements the {@link ListInterpreter} provides functionality
 * </p>
 *
 * @author Abishek
 * @version 1.0
 * @see ListInterpreter
 */
public class SQLListInterpreter implements ListInterpreter {
    private static final int SORT_DISABLED = 0;
    private static final int SORT_DEFAULT_ASCENDING = 2;
    private static final int SORT_DEFAULT_DESCENDING = 3;
    private static final int SELECT_DISTINCT = 1;
    private static final int SELECT_COUNT = 2;
    private static final int SELECT_MIN = 3;
    private static final int SELECT_MAX = 4;
    private static final int SELECT_AVG = 5;
    private final ListStructure listStructure;
    private final Long userId;
    private final Long orgId;
    private final SelectQuery selectQuery;
    private final I18nResolver i18nResolver;

    public SQLListInterpreter(final Long userId, final Long orgId, final String providerName, final I18nResolver i18nResolver,
                              final JoinLoader joinLoader) {
        this.userId = Objects.requireNonNull(userId);
        this.orgId = Objects.requireNonNull(orgId);
        this.i18nResolver = i18nResolver;
        this.listStructure = DefaultStructureHandler.get().getStructure(userId, orgId, providerName);
        final Collection<FieldConfig> fields = listStructure.getFields();
        final SortedSet<Table> tables = new TreeSet<>();

        for (final FieldConfig field : fields) {
            final FieldConfigSource fieldSource = field.getSource();
            final Table table = Table.get(fieldSource.table());

            tables.add(table);
        }

        if (joinLoader.isJoinExist()) {
            final List<JoinClause> preJoins = joinLoader.getJoins();
            this.selectQuery = SelectQuery.get(preJoins.get(0).getLocalTable());

            selectQuery.addJoinClause(preJoins);
        } else {
            this.selectQuery = SelectQuery.get(tables.first());

            selectQuery.addJoinClause(DmlUtil.getJoins(tables));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyDefaultOrderBy() {
        final Collection<FieldConfig> fields = listStructure.getFields();

        for (final FieldConfig field : fields) {
            final int sortFlag = field.getSort();

            if (sortFlag == SORT_DEFAULT_ASCENDING || sortFlag == SORT_DEFAULT_DESCENDING) {
                final OrderByClause orderByClause = getOrderByClause(field);

                selectQuery.addOrderByClause(orderByClause);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyDefaultSelect() {
        final Collection<ConfiguredField> fields = getDefaultFieldConfigs();

        for (ConfiguredField field : fields) {
            selectClause(field);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectClause(final ConfiguredField configuredField) {

        if (configuredField.isEnabled()) {
            final FieldConfig fieldConfig = listStructure.getField(configuredField.fieldName());

            if (Objects.isNull(fieldConfig)) {
                throw ErrorCode.get(CommonErrorCodes.FIELDS_NOT_FOUND, "i18n.entity.list.field.not.found");
            }
            selectQuery.addSelectClause(getSelectColumn(fieldConfig));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyDefaultLimit() {
        selectQuery.setLimitClause(new LimitClause(1, 10));
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldName The field to be included in the select clause.
     */
    @Override
    public void selectClause(String fieldName) {
        final FieldConfig field = listStructure.getField(fieldName);
        final Column column = getSelectColumn(field);

        selectQuery.addSelectClause(column);
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldName The field to be used for ordering.
     * @param asc       A boolean indicating whether the ordering is ascending (true) or descending (false).
     */
    @Override
    public void orderByClause(String fieldName, boolean asc) {
        final FieldConfig field = listStructure.getField(fieldName);
        if (field.getSort() != SORT_DISABLED) {
            final OrderByClause orderByClause = getOrderByClause(field, asc);
            selectQuery.addOrderByClause(orderByClause);
        } else {
            throw ErrorCode.get(ErrorCodes.ACCESS_DENIED, "Sort not allowed for this field!!");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldName The key or field name for the criteria.
     * @param value     The value to match against the specified key.
     * @param comparator The condition specifying how to apply the criteria (e.g., "equals", "greater than")
     */
    @Override
    public void criteria(final String fieldName, final Object value, final String comparator) {
        final FieldConfig field = listStructure.getField(fieldName);

        if (field.isSearch()) {
            final FieldConfigSource sqlSource = field.getSource();
            final Table table = Table.get(sqlSource.table());
            final Column column = table.getColumn(sqlSource.column(), field.getName());
            final DataType dataType = column.getColumnDefinition().getDataType();
            final boolean isKChar = dataType.isKChar();
            switch (comparator) {
                case "contains" -> {
                    if (isKChar) {
                        appendCondition(KCharCriteria.contains(column, value.toString(), i18nResolver));
                    } else {
                        appendCondition(new WhereClause(Criteria.contains(column, value.toString())));
                    }
                }
                case "startsWith" -> {
                    if (isKChar) {
                        appendCondition(KCharCriteria.startsWith(column, value.toString(), i18nResolver));
                    } else {
                        appendCondition(new WhereClause(Criteria.startsWith(column, value.toString())));
                    }
                }
                case "in" -> {
                    if (isKChar) {
                        appendCondition(KCharCriteria.in(column, (Collection<String>) value, i18nResolver));
                    } else {
                        appendCondition(new WhereClause(Criteria.in(column, (Collection<?>) value)));
                    }
                }
                case "notIn" -> {
                    if (isKChar) {
                        appendCondition(KCharCriteria.notIn(column, (Collection<String>) value, i18nResolver));
                    } else {
                        appendCondition(new WhereClause(Criteria.notIn(column, (Collection<?>) value)));
                    }
                }
                case "notEqual" -> {
                    if (isKChar) {
                        appendCondition(KCharCriteria.notEq(column, value.toString(), i18nResolver));
                    } else {
                        appendCondition(new WhereClause(Criteria.notEq(column, value)));
                    }
                }
                default -> {
                    if (isKChar) {
                        appendCondition(KCharCriteria.eq(column, value.toString(), i18nResolver));
                    } else {
                        appendCondition(new WhereClause(Criteria.eq(column, value)));
                    }
                }
            }
        } else {
            throw ErrorCode.get(ErrorCodes.ACCESS_DENIED, "Search not allowed for this field!!");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param start The index of the first result to retrieve.
     * @param limit The maximum number of results to retrieve.
     */
    @Override
    public void setLimits(int start, int limit) {
        selectQuery.setLimitClause(new LimitClause(start, Math.min(100, limit)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long fetchTotalHits() {
        final CountQuery countQuery = CountQuery.get(selectQuery.getBaseTable());

        countQuery.addJoinClause(selectQuery.getJoinClause());
        countQuery.setWhereClause(selectQuery.getWhereClause());
        return DataStoreProvider.getSecureReadableOrgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of maps containing the mapped data.
     */
    @Override
    public Collection<Map<String, Object>> getResult() {
        return DataStoreProvider.getSecureReadableOrgDataStore(orgId).getData(selectQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @return The name of the primary field.
     */
    @Override
    public String primaryField() {
        return listStructure.getPrimaryField().getName();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link ListStructure} details
     */
    @Override
    public ListStructure structure() {
        return listStructure;
    }

    /**
     * <p>
     * Retrieves an OrderByClause instance based on the provided field configuration, with default ascending sorting.
     * </p>
     *
     * @param field Represents the {@link FieldConfig} details
     * @return The {@link OrderByClause}
     */
    private OrderByClause getOrderByClause(final FieldConfig field) {
        return getOrderByClause(field, field.getSort() == SORT_DEFAULT_ASCENDING);
    }

    /**
     * <p>
     * Retrieves an OrderByClause instance based on the provided field configuration and sorting order.
     * </p>
     *
     * @param field Represents the {@link FieldConfig} details
     * @param asc   A boolean indicating whether the sorting order is ascending (true) or descending (false).
     * @return The {@link OrderByClause}
     */
    private OrderByClause getOrderByClause(final FieldConfig field, final boolean asc) {
        final FieldConfigSource sqlSource = field.getSource();
        final Table table = Table.get(sqlSource.table());
        final Column column = table.getColumn(sqlSource.column());

        return new OrderByClause(column, asc ? OrderByClause.OrderType.ASCENDING : OrderByClause.OrderType.DESCENDING);
    }

    /**
     * <p>
     * Retrieves the corresponding column for the given field configuration.
     * </p>
     *
     * @param field Represents the {@link FieldConfig} details
     * @return The {@link Column}
     */
    private Column getSelectColumn(final FieldConfig field) {
        final FieldConfigSource sqlSource = field.getSource();
        final Table table = Table.get(sqlSource.table());
        final Column column = table.getColumn(sqlSource.column(), field.getName());

        return switch (sqlSource.type()) {
            case SELECT_DISTINCT -> column.distinct();
            case SELECT_COUNT -> column.count();
            case SELECT_MIN -> column.min();
            case SELECT_MAX -> column.max();
            case SELECT_AVG -> column.average();
            default -> column;
        };
    }

    /**
     * <p>
     * Appends the provided whereClause to the existing where clause in the selectQuery.
     * </p>
     *
     * @param whereClause The where clause to be appended.
     */
    private void appendCondition(WhereClause whereClause) {
        final WhereClause existing = selectQuery.getWhereClause();
        if (existing == null) {
            selectQuery.setWhereClause(whereClause);
        } else {
            selectQuery.setWhereClause(existing.and(whereClause));
        }
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return {@link Collection} of {@link ConfiguredField}
     */
    @Override
    public Collection<ConfiguredField> getDefaultConfiguredFields() {
        return getDefaultFieldConfigs();
    }

    /**
     * <p>
     * Gets the {@link Collection} of default {@link ConfiguredField}.
     * </p>
     *
     * @return {@link Collection} of {@link ConfiguredField}
     */
    private Collection<ConfiguredField> getDefaultFieldConfigs() {
        final Collection<ConfiguredField> configuredFields = new HashSet<>();
        final Collection<FieldConfig> fields = listStructure.getFields();

        for (final FieldConfig fieldConfig : fields) {
            final FieldConfig.Config config = fieldConfig.getConfig();

            if (Objects.nonNull(config) && config.isDefaultField()) {
                final ConfiguredField configuredField = new ConfiguredField(fieldConfig.getName());

                configuredField.setOrder(fieldConfig.getOrder());
                configuredField.setEnabled(fieldConfig.getConfig().isEnabledForWeb());

                configuredFields.add(configuredField);
            }
        }

        return configuredFields;
    }

    /**
     * {@inheritDoc}
     *
     * @param whereClause The {@link WhereClause} to be set
     */
    @Override
    public void setWhereClause(final WhereClause whereClause) {
        final WhereClause existingWhereClause = selectQuery.getWhereClause();

        selectQuery.setWhereClause(Objects.isNull(existingWhereClause) ? whereClause : existingWhereClause.and(whereClause));
    }

    /**
     * <p>
     * Gets a collection of dynamic fields name
     * </p>
     *
     * @return The collection of dynamic fields name
     */
    @Override
    public Collection<String> getDynamicFields() {
        return Collections.emptyList();
    }
}