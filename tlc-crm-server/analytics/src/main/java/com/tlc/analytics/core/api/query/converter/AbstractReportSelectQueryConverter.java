package com.tlc.analytics.core.api.query.converter;

import com.tlc.analytics.config.internal.ConfigErrorCodes;
import com.tlc.analytics.config.keys.CommonBaseModelKeys;
import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.table.AnalyticsDataType;
import com.tlc.analytics.config.model.table.ColumnRelation;
import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.config.model.table.TableRelation;
import com.tlc.analytics.config.model.view.BaseField;
import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.model.view.filter.FilterFunctionMapper;
import com.tlc.analytics.config.model.view.filter.OperatorType;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.config.model.view.report.AnalyticsReport;
import com.tlc.analytics.config.model.view.report.ReportFieldCustomRange;
import com.tlc.analytics.config.model.view.report.CaseWhenConfig;
import com.tlc.analytics.config.model.view.report.KPIView;
import com.tlc.analytics.config.model.view.report.AnalyticsCustomFieldConfig;
import com.tlc.analytics.config.model.view.report.chart.Axis;
import com.tlc.analytics.config.model.view.report.path.PathInfo;
import com.tlc.analytics.config.model.view.report.path.RelationsInPath;
import com.tlc.analytics.config.model.view.report.path.ReportPathMapping;
import com.tlc.analytics.core.api.internal.DataViewRequest;
import com.tlc.analytics.core.api.query.SelectQueryConverter;
import com.tlc.analytics.core.api.query.filter.FilterApplier;
import com.tlc.analytics.core.api.query.operation.OperationApplier;
import com.tlc.analytics.service.column.ColumnDetailsService;
import com.tlc.analytics.sql.resource.OACDIMCONTACT;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.pair.Pair;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.security.iam.LoggedInUserInfo;

import com.tlc.l10n.temporal.model.TimezoneInfo;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.JsonColumn;
import com.tlc.sql.common.api.dml.ConditionalColumn;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.JoinClause;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.With;
import com.tlc.sql.common.api.dml.function.FunctionArgument;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.api.meta.ArrayColumnDefinition;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.CommonDataType;
import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.api.meta.DataType;

import com.tlc.sql.olap.api.dml.OlapConditionalColumn;
import com.tlc.sql.olap.api.dml.OlapCriteria;
import com.tlc.sql.olap.api.dml.OlapJsonColumn;
import com.tlc.sql.olap.api.dml.OlapSelectQuery;
import com.tlc.sql.olap.api.dml.function.OlapSqlFunctionBuilderFactory;
import com.tlc.sql.olap.api.meta.OlapDataType;

import lombok.Getter;
import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.List;
import java.util.HashSet;
import java.util.Objects;
import java.util.IdentityHashMap;
import java.util.SortedSet;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Abstract base for building CTE-based SELECT queries for analytics reports.
 *
 * <p>Converts a {@link DataViewRequest} into a SQL {@link With} (CTE) query by:
 * <ol>
 *   <li>Building a filtered CTE with filter conditions ({@link FilteredCteBuilder})</li>
 *   <li>Resolving outer joins for uncovered report fields ({@link OuterJoinResolver})</li>
 *   <li>Applying aggregation/group-by clauses ({@link #applyReportAggregation})</li>
 * </ol>
 *
 * <h3>Performance Optimizations</h3>
 * <ul>
 *   <li>Lazy-cached JSON parsing in {@link BaseField} for case-when and custom field configs</li>
 *   <li>Pre-compiled regex pattern for alias sanitization</li>
 *   <li>Identity-based projection cache to avoid hash collisions</li>
 *   <li>Loop-based iteration on hot paths instead of streams</li>
 *   <li>Extracted shared helpers to eliminate code duplication</li>
 * </ul>
 *
 * @param <V>       the analytics report view type
 * @param <Request> the data view request type
 * @author karthick-v
 * @version 1.0
 */
abstract class AbstractReportSelectQueryConverter<V extends AnalyticsReport, Request extends DataViewRequest<V>>
        implements SelectQueryConverter<V, Request> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractReportSelectQueryConverter.class);
    private static final String DEFAULT_FILTER_COLUMN = "IS_DELETED";
    private static final Pattern ALIAS_SANITIZE_PATTERN = Pattern.compile("[^a-zA-Z0-9_]");
    protected static final String CTE_ALIAS = "filtered_data";
    private static final String HOUR_OF_THE_DAY = "hour_of_the_day";

    private final ColumnDetailsService columnDetailsService;

    /**
     * Initializes the column details service singleton instance.
     */
    protected AbstractReportSelectQueryConverter() {
        this.columnDetailsService = ColumnDetailsService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param viewRequest The view request containing details to generate the select query.
     * @return The generated select query based on the view request.
     * @throws Exception If any error occurs during the conversion process.
     */
    @Override
    public @NonNull With convert(final @NonNull Request viewRequest) throws Exception {
        final long startTime = System.currentTimeMillis();
        final Long orgId = viewRequest.getOrgId();
        final Long userId = viewRequest.getUserId();
        final Long viewId = viewRequest.getView().getViewId();

        LOGGER.info("Analytics: Query Converter - START - Constructing the select query (CTE-based flow), orgId [{}], userId [{}], viewId [{}]", orgId, userId, viewId);

        // 1. Build filtered CTE with filter conditions
        final SelectQuery filteredCteQuery = new FilteredCteBuilder(viewRequest).build();
        final List<DerivedTable> derivedTables = new ArrayList<>();
        final DerivedTable filteredCteTable = DerivedTable.get(DatabaseType.OLAP, filteredCteQuery.getBaseTable().getName(), CTE_ALIAS, filteredCteQuery);

        filteredCteTable.setWithClauseCTE(true);
        derivedTables.add(filteredCteTable);

        // 2. Build outer query with joins for uncovered report fields and collect additional derived tables
        final SelectQuery outerQuery = getOuterQuery(viewRequest, filteredCteTable, filteredCteQuery);
        LOGGER.info("Analytics: Query Converter - Select query constructed (CTE-based flow), orgId [{}], userId [{}], viewId [{}]", orgId, userId, viewId);

        // 5. Build With clause with all derived tables and the outer query
        final With.Builder withBuilder = With.Builder.create();

        derivedTables.forEach(withBuilder::addDerivedTable);
        withBuilder.selectQuery(outerQuery);

        LOGGER.info("Analytics: Query Converter - END - Convert operation completed in [{}] ms, orgId [{}], userId [{}], viewId [{}]",
                (System.currentTimeMillis() - startTime), orgId, userId, viewId);

        return withBuilder.build();
    }

    /**
     * Builds the outer {@link SelectQuery} by resolving joins, applying aggregation,
     * and setting pagination limits from the view request.
     *
     * @param viewRequest      the data view request containing report configuration
     * @param filteredCteTable the filtered CTE derived table
     * @param filteredCteQuery the filtered CTE select query
     * @return the constructed outer select query with aggregation and pagination applied
     */
    private SelectQuery getOuterQuery(final Request viewRequest, final DerivedTable filteredCteTable, final SelectQuery filteredCteQuery) {
        final SelectQuery outerQuery = new OuterJoinResolver(viewRequest, filteredCteTable,
                Collections.singleton(filteredCteQuery.getBaseTable().getName())).build();

        applyReportAggregation(viewRequest, outerQuery);

        final Integer startingIndex = viewRequest.getStart();
        final Integer endingIndex = viewRequest.getLimit();

        if (startingIndex != null && endingIndex != null) {
            outerQuery.setLimitClause(new LimitClause(startingIndex, endingIndex));
        }

        return outerQuery;
    }

    /**
     * Computes a unique column alias for the given field, handling the distinction
     * between fields with and without explicit IDs, and underlying vs. chart requests.
     *
     * @param chartUniqueId       the unique chart identifier (may be null)
     * @param columnName          the base column name
     * @param field               the base field providing field ID and operation type
     * @param isAppliedConfig     whether configuration is applied
     * @param selectedColumnNames set of already-selected column names for dedup
     * @param isUnderlyingRequest whether this is an underlying data request
     * @return the computed column alias string
     */
    private String computeColumnAlias(final Long chartUniqueId, final String columnName,
                                      final BaseField field, final boolean isAppliedConfig,
                                      final Set<String> selectedColumnNames,
                                      final boolean isUnderlyingRequest) {
        final Long fieldId = field.id();

        if (fieldId == null) {
            return isAppliedConfig || selectedColumnNames.contains(columnName)
                    ? getColumnAlias(chartUniqueId, columnName, field, isUnderlyingRequest) : String.format("%s%s", columnName, field.getFieldId());
        }
        return String.format("%s_%s", getColumnAlias(chartUniqueId, columnName, field, isUnderlyingRequest), fieldId);
    }

    /**
     * Collects column names from the given SELECT clause into a new {@link HashSet}.
     *
     * @param selectClause the SELECT clause columns
     * @return a set of column names
     */
    private static Set<String> collectSelectedColumnNames(final Collection<Column> selectClause) {
        final Set<String> names = new HashSet<>(selectClause.size());

        for (final Column column : selectClause) {
            names.add(column.getColumnName());
        }

        return names;
    }

    /**
     * Resolves the effective column name from a column, using the alias for
     * JSON-extracted custom field columns (where the alias represents the extracted value).
     *
     * @param column the column to resolve the name for
     * @return the alias if the column is a JsonColumn with an alias, otherwise the column name
     */
    private static String resolveEffectiveColumnName(final Column column) {
        return (column instanceof JsonColumn && column.hasAlias()) ? column.getColumnAlias() : column.getColumnName();
    }

    /**
     * Builds a filtered Common Table Expression (CTE) by applying filter conditions
     * and projecting covered report field columns from the root table and joined tables.
     */
    protected class FilteredCteBuilder {
        private final Request viewRequest;
        private final Long orgId;
        private final Set<String> coveredTables = new HashSet<>();
        private final Set<String> coveredColumns = new HashSet<>();
        private final SelectQuery cteQuery;

        /**
         * Constructs the {@link FilteredCteBuilder} with the given view request
         * and initializes the CTE query using the root table.
         *
         * @param viewRequest the data view request containing report configuration
         */
        public FilteredCteBuilder(final Request viewRequest) {
            this.viewRequest = viewRequest;
            this.orgId = viewRequest.getOrgId();
            this.cteQuery = OlapSelectQuery.get(Table.get(DatabaseType.OLAP, viewRequest.getRootTableDetail().getTableName()));
        }

        /**
         * Builds the filtered CTE {@link SelectQuery} by applying filter fields
         * and adding covered report field columns to the SELECT clause.
         *
         * @return the constructed filtered CTE select query
         */
        SelectQuery build() {
            LOGGER.debug("Analytics: Query Converter - Building filtered CTE, orgId [{}], userId [{}], viewId [{}]", orgId, viewRequest.getUserId(), viewRequest.getView().getViewId());
            final V view = viewRequest.getView();
            applyFilterFieldsToCte(view);
            addCoveredReportFieldColumns(view, cteQuery);
            LOGGER.debug("Analytics: Query Converter - Filtered CTE built: coveredTables={}, coveredColumns={}, orgId [{}], userId [{}], viewId [{}]", coveredTables.size(), coveredColumns.size(), orgId, viewRequest.getUserId(), viewRequest.getView().getViewId());
            return cteQuery;
        }

        /**
         * Iterates over the view's filter fields, applies each as a WHERE condition
         * on the CTE query, and appends the default {@code IS_DELETED = false} filter.
         *
         * @param view the analytics report view containing filter field definitions
         */
        private void applyFilterFieldsToCte(final V view) {
            final Collection<FilterField> filterFields = view.getFilterFields();

            if (filterFields == null) {
                return;
            }

            for (final FilterField filterField : filterFields) {
                final ColumnDetail columnDetail = viewRequest.getColumnDetails(filterField.getColumnId());

                if (columnDetail == null) {
                    throw ErrorCode.get(ConfigErrorCodes.INVALID_COLUMN_CONFIGURATION,
                            "i18n.analytics.column.details.not.found");
                }

                loadCTEQueryJoin(viewRequest, columnDetail.getTableDetail());
                handleFilter(filterField, cteQuery, viewRequest);

                coveredColumns.add(columnDetail.getColumnName());
                coveredTables.add(columnDetail.getTableDetail().getTableName());
            }

            final Criteria criteria = Criteria.eq(cteQuery.getBaseTable().getColumn(DEFAULT_FILTER_COLUMN), false);
            final WhereClause existingWhere = cteQuery.getWhereClause();

            if (existingWhere != null) {
                cteQuery.setWhereClause(existingWhere.and(criteria));
            } else {
                cteQuery.setWhereClause(new WhereClause(criteria));
            }
        }

        /**
         * Adds a JOIN clause to the CTE query for the given table if it is related
         * to the root table and has not already been covered.
         *
         * @param viewRequest  the data view request containing relation mappings
         * @param tableDetail the target table to potentially join into the CTE
         */
        private void loadCTEQueryJoin(final Request viewRequest, final TableDetail tableDetail) {
            final String targetTableName = tableDetail.getTableName();
            final TableDetail rootTableDetail = viewRequest.getRootTableDetail();

            if (rootTableDetail.getTableName().equals(targetTableName) || coveredTables.contains(targetTableName)) {
                return;
            }

            final Collection<RelationsInPath> relationsInPaths = viewRequest.getView().getReportPathMapping().getPathInfo().getRelationsInPaths();

            if (relationsInPaths == null) {
                return;
            }

            final Long rootTableId = rootTableDetail.id();
            final Long targetTableId = tableDetail.id();

            for (final RelationsInPath relationsInPath : relationsInPaths) {
                final TableRelation tableRelation = viewRequest.getTableRelation(relationsInPath.getRelationId());
                final Long parentTableId = tableRelation.getParentTableId();
                final Long childTableId = tableRelation.getChildTableId();

                if (Objects.equals(parentTableId, rootTableId) && Objects.equals(childTableId, targetTableId)) {
                    final ColumnRelation columnRelation = tableRelation.getColumnRelation();

                    cteQuery.addJoinClause(new JoinClause(Table.get(DatabaseType.OLAP, viewRequest.getTableDetails(parentTableId).getTableName()),
                            viewRequest.getColumnDetails(columnRelation.getParentColId()).getColumnName(),
                            Table.get(DatabaseType.OLAP, viewRequest.getTableDetails(childTableId).getTableName()),
                            viewRequest.getColumnDetails(columnRelation.getChildColId()).getColumnName()));
                    coveredTables.add(targetTableName);
                    break;
                }
            }
        }

        /**
         * Projects columns of report fields whose tables are already covered by the CTE.
         * Also applies custom field adaptation, KPI function columns, and custom range filters.
         *
         * @param view     the analytics report view containing report field definitions
         * @param cteQuery the CTE select query to add columns to
         */
        private void addCoveredReportFieldColumns(final V view, final SelectQuery cteQuery) {
            final Set<String> selectedColumnNames = collectSelectedColumnNames(cteQuery.getSelectClause());
            final Collection<ReportField> reportFields = view.getReportFields();

            for (final ReportField reportField : reportFields) {
                final ColumnDetail columnDetail = viewRequest.getColumnDetails(reportField.getColumnId());
                final TableDetail tableDetail = columnDetail.getTableDetail();

                if (!coveredTables.contains(tableDetail.getTableName())) {
                    continue;
                }

                final String columnName = columnDetail.getColumnName();
                final String columnAliasName = computeColumnAlias(viewRequest.getChartUniqueId(), columnName, reportField,
                        viewRequest.isApplyConfiguration(), selectedColumnNames, viewRequest.isUnderlyingRequest());
                final Column column = adaptColumnForCustomFieldIfNeeded(columnAliasName, Table.get(DatabaseType.OLAP,
                        tableDetail.getTableName()).getColumn(columnDetail.getColumnName()), reportField);

                cteQuery.addSelectClause(column);
                coveredColumns.add(columnDetail.getColumnName());
                selectedColumnNames.add(column.getColumnName());

                if (view instanceof KPIView && reportField.getAxis() == Axis.X_AXIS) {
                    applyFunctionInterval(adaptColumnForCustomFieldIfNeeded(columnAliasName, column, reportField), columnDetail,
                            viewRequest, cteQuery);
                }
            }
        }
    }

    /**
     * Builds hierarchical SELECT queries with cardinality-aware join ordering.
     *
     * <p>Design Principle: The filtered CTE (lowest relationIndex) remains at the innermost
     * position while fact tables wrap around it as nested derived tables. The table with
     * the highest relationIndex becomes the outer FROM clause driver.
     *
     * <p>Query Structure Target:
     * <pre>
     * SELECT ... FROM highest_index_table
     * LEFT JOIN (
     *   SELECT ... FROM middle_index_table
     *   INNER JOIN (
     *     SELECT ... FROM lower_index_table
     *     INNER JOIN filtered_cte ON ...  -- innermost
     *   ) AS derived_1 ON ...
     * ) AS derived_2 ON ...
     * </pre>
     */
    protected class OuterJoinResolver {

        private final Request viewRequest;
        private final Long orgId;
        private final DerivedTable filteredTable;
        private final Set<String> cteTables;

        private final Map<String, DerivedTable> derivedTableRegistry = new HashMap<>();
        private final Map<SelectQuery, Set<String>> projectedColumns = new IdentityHashMap<>();
        private final Map<SelectQuery, Set<String>> projectedAliases = new IdentityHashMap<>();
        private final Map<SelectQuery, Map<String, String>> jsonAliasCache = new IdentityHashMap<>();
        private final Set<SelectQuery> caseWhenProcessedQueries = Collections.newSetFromMap(new IdentityHashMap<>());

        /**
         * Returns the cached set of projected column names for the given query.
         * Initializes from the query's current SELECT clause on first access.
         *
         * @param query the select query
         * @return mutable set of projected column names (lowercase)
         */
        private Set<String> getProjectedColumnNames(final SelectQuery query) {
            return projectedColumns.computeIfAbsent(query,
                    selectQuery -> new HashSet<>(collectSelectedColumnNames(selectQuery.getSelectClause())));
        }

        /**
         * Returns the cached set of projected column aliases for the given query.
         * Initializes from the query's current SELECT clause on first access.
         *
         * @param query the select query
         * @return mutable set of projected aliases
         */
        private Set<String> getProjectedAliasList(final SelectQuery query) {
            return projectedAliases.computeIfAbsent(query, selectQuery -> {
                final Set<String> aliases = new HashSet<>();
                final SortedSet<Column> selectClause = selectQuery.getSelectClause();

                for (final Column column : selectClause) {
                    if (column.hasAlias()) {
                        aliases.add(column.getColumnAlias().toLowerCase());
                    }
                }
                return aliases;
            });
        }

        /**
         * Returns a cached map of raw column name (lowercase) → JSON alias for the given query.
         * Only includes {@link JsonColumn} entries that have aliases.
         *
         * @param query the select query
         * @return map of raw column name → alias
         */
        private Map<String, String> getJsonAliasMappings(final SelectQuery query) {
            return jsonAliasCache.computeIfAbsent(query, selectQuery -> {
                final Map<String, String> map = new HashMap<>();
                final SortedSet<Column> selectClause = selectQuery.getSelectClause();

                for (final Column column : selectClause) {

                    if (column instanceof JsonColumn && column.hasAlias()) {
                        map.put(column.getColumnName().toLowerCase(), column.getColumnAlias());
                    }
                }
                return map;
            });
        }

        /**
         * Constructs the {@link OuterJoinResolver} with required dependencies.
         *
         * @param viewRequest   the data view request
         * @param filteredTable the filtered CTE derived table
         * @param cteTables     set of table names already covered by the CTE
         */
        public OuterJoinResolver(final Request viewRequest, final DerivedTable filteredTable, final Set<String> cteTables) {
            this.viewRequest = viewRequest;
            this.orgId = viewRequest.getOrgId();
            this.filteredTable = filteredTable;
            this.cteTables = cteTables;
        }

        /**
         * Builds the outer {@link SelectQuery} by resolving path relations and
         * constructing a hierarchical join structure around the filtered CTE.
         *
         * @return the outer select query with all necessary joins
         */
        public SelectQuery build() {
            LOGGER.debug("Analytics: Query Converter - Building outer query with hierarchical join structure, orgId [{}], userId [{}], viewId [{}]", orgId, viewRequest.getUserId(), viewRequest.getView().getViewId());

            final V view = viewRequest.getView();

            if (!hasValidPathMapping(view)) {
                LOGGER.debug("Analytics: Query Converter - No valid path mapping found, returning simple filtered table query, orgId [{}], userId [{}], viewId [{}]", orgId, viewRequest.getUserId(), viewRequest.getView().getViewId());
                return OlapSelectQuery.get(filteredTable);
            }

            final List<RelationsInPath> sortedRelations = getSortedRelations(view.getReportPathMapping().getPathInfo());

            if (sortedRelations.isEmpty()) {
                return OlapSelectQuery.get(filteredTable);
            }

            return buildHierarchicalQuery(sortedRelations);
        }

        // ==================== QUERY CONSTRUCTION ====================

        /**
         * Iterates through sorted relations and wraps each as a nested derived table,
         * building the hierarchical query from innermost CTE to outermost fact table.
         *
         * @param sortedRelations the relations sorted by relation index (ascending)
         * @return the fully constructed hierarchical select query
         */
        private SelectQuery buildHierarchicalQuery(final List<RelationsInPath> sortedRelations) {
            LOGGER.debug("Analytics: Query Converter - Building hierarchical query with {} relations, orgId [{}], userId [{}], viewId [{}]",
                    sortedRelations.size(), orgId, viewRequest.getUserId(), viewRequest.getView().getViewId());

            DerivedTable currentQuery = filteredTable;

            for (final RelationsInPath relation : sortedRelations) {
                final TableRelation tableRelation = viewRequest.getTableRelation(relation.getRelationId());

                if (tableRelation == null) {
                    continue;
                }

                currentQuery = wrapWithNextRelation(currentQuery, tableRelation);
            }

            LOGGER.debug("Analytics: Query Converter - Hierarchical query construction completed, orgId [{}], userId [{}], viewId [{}]",
                    orgId, viewRequest.getUserId(), viewRequest.getView().getViewId());
            return currentQuery.getSelectQuery();
        }

        /**
         * Wraps the current inner query with the next table relation by delegating
         * to the appropriate case handler based on CTE membership of parent/child tables.
         *
         * @param innerQuery    the current innermost derived table
         * @param tableRelation the table relation to apply
         * @return the new derived table wrapping the inner query
         */
        private DerivedTable wrapWithNextRelation(final DerivedTable innerQuery, final TableRelation tableRelation) {
            final RelationContext context = new RelationContext(tableRelation, innerQuery);

            if (context.isBothTablesInCte()) {
                return handleBothInCte(context);
            }

            if (context.isParentInCte()) {
                return handleParentInCte(context);
            }

            if (context.isChildInCte()) {
                return handleChildInCte(context);
            }
            return handleNeitherInCte(context);
        }

        // ==================== CASE HANDLERS ====================

        /**
         * Handles the case where both parent and child tables are already in the CTE.
         * Returns the inner query unchanged since no additional join is needed.
         *
         * @param context the relation context containing table and column details
         * @return the existing inner derived table
         */
        private DerivedTable handleBothInCte(final RelationContext context) {
            return context.getInnerQuery();
        }

        /**
         * Handles the case where the parent table is in the CTE but the child is not.
         * Creates an outer query with the child table as the base, joins the CTE as an inner
         * derived table, and projects CTE columns into the outer query.
         *
         * @param context the relation context containing table and column details
         * @return a new derived table wrapping the child table with the CTE join
         */
        private DerivedTable handleParentInCte(final RelationContext context) {
            final Table childTable = context.getChildTable();
            final SelectQuery outerQuery = OlapSelectQuery.get(childTable);
            final SelectQuery innerSelect = context.getInnerQuery().getSelectQuery();

            ensureColumnProjected(innerSelect, context.getParentColumnName());

            final DerivedTable innerDerived = context.getInnerQuery();

            ensureReportFieldsProjected(innerDerived);
            ensureCaseWhenColumnsProjected(innerSelect);
            forwardColumnsToOuter(innerSelect, innerDerived, outerQuery, childTable);

            final String joinKeyColumn = context.getParentColumnName();
            final String childTableName = context.getChildTableName();

            if (!isAlreadyProjectedByNameOrAlias(outerQuery, joinKeyColumn)) {
                outerQuery.addSelectClause(innerDerived.getColumn(joinKeyColumn, joinKeyColumn));
            }

            outerQuery.addJoinClause(new JoinClause(childTable, context.getChildColumnName(), innerDerived, joinKeyColumn));
            applyIsDeletedFilter(outerQuery, childTable);

            return createDerivedTable(outerQuery, childTableName, String.format("%s_outer_derived", childTableName));
        }

        /**
         * Handles the case where the child table is in the CTE but the parent is not.
         * Creates a derived table for the parent and joins it into the filtered CTE query.
         *
         * @param context the relation context containing table and column details
         * @return the filtered table with the parent join added
         */
        private DerivedTable handleChildInCte(final RelationContext context) {
            final SelectQuery outerQuery = filteredTable.getSelectQuery();
            final String childColumnName = context.getChildColumnName();

            ensureColumnProjected(outerQuery, childColumnName);

            outerQuery.addJoinClause(new JoinClause(filteredTable, childColumnName, createParentDerivedTable(context),
                    context.getParentColumnName()));

            return filteredTable;
        }

        /**
         * Handles the case where neither parent nor child table is in the CTE.
         * Creates a derived chain: inner derived → parent FK filter → child outer query.
         *
         * @param context the relation context containing table and column details
         * @return a new derived table wrapping the child with the inner derived join
         */
        private DerivedTable handleNeitherInCte(final RelationContext context) {
            final Table childTable = context.getChildTable();
            final SelectQuery outerQuery = OlapSelectQuery.get(childTable);
            final DerivedTable innerDerived = context.getInnerQuery();
            final SelectQuery innerSelect = innerDerived.getSelectQuery();
            final String parentColumnName = context.getParentColumnName();
            final String childColumnName  = context.getChildColumnName();
            final String childTableName = context.getChildTableName();

            ensureColumnProjected(innerSelect, parentColumnName);
            ensureReportFieldsProjected(innerDerived);
            ensureCaseWhenColumnsProjected(innerSelect);

            if (childTable.containsColumn(childColumnName)) {
                outerQuery.addSelectClause(childTable.getColumn(childColumnName));
            }

            forwardColumnsToOuter(innerSelect, innerDerived, outerQuery, null);

            outerQuery.addJoinClause(new JoinClause(childTable, childColumnName, innerDerived, parentColumnName));
            applyIsDeletedFilter(outerQuery, childTable);

            return createDerivedTable(outerQuery, childTableName, String.format("%s_derived", childTableName));
        }

        /**
         * Forwards columns from an inner SELECT clause to an outer query via a derived table reference.
         * Handles JSON-extracted custom field columns by using their alias name.
         * Optionally excludes columns that already belong to a specific table (e.g., child table in a join).
         *
         * @param innerSelect  the inner SELECT query containing source columns
         * @param innerDerived the derived table reference for the inner query
         * @param outerQuery   the outer query to forward columns into
         * @param excludeTable table whose columns should be excluded (nullable)
         */
        private void forwardColumnsToOuter(final SelectQuery innerSelect, final DerivedTable innerDerived,
                                           final SelectQuery outerQuery, final Table excludeTable) {
            final SortedSet<Column> selectClause = innerSelect.getSelectClause();
            final Set<String> outerProjectedColumnNames = getProjectedColumnNames(outerQuery);

            for (final Column column : selectClause) {
                final String columnName = resolveEffectiveColumnName(column);

                if ((excludeTable == null || !excludeTable.containsColumn(columnName)) && outerProjectedColumnNames.add(columnName)) {
                    outerQuery.addSelectClause(innerDerived.getColumn(columnName, columnName));
                }
            }
        }

        /**
         * Ensures all report field columns and their case-when dependency columns
         * are projected by recursively walking derived tables.
         *
         * @param innerDerived the inner derived table to project columns into
         */
        private void ensureReportFieldsProjected(final DerivedTable innerDerived) {
            final Collection<ReportField> reportFields = viewRequest.getView().getReportFields();
            final Map<Long, ColumnDetail> columnDetailsCache = new HashMap<>();

            for (final ReportField reportField : reportFields) {
                projectColumnRecursively(innerDerived, reportField.getColumnId(), columnDetailsCache);

                final Collection<Long> caseWhenColumnIds = getCaseWhenColumnIds(reportField);

                for (final Long caseWhenColumnId : caseWhenColumnIds) {
                    projectColumnRecursively(innerDerived, caseWhenColumnId, columnDetailsCache);
                }
            }
        }

        /**
         * Ensures columns referenced by CASE-WHEN configurations are projected in the inner query.
         * Tries direct table lookup first, then recursively bubbles up from nested derived tables.
         * Uses memoization to avoid reprocessing the same query multiple times.
         *
         * @param innerQuery the inner select query to add projections to
         */
        private void ensureCaseWhenColumnsProjected(final SelectQuery innerQuery) {
            if (!caseWhenProcessedQueries.add(innerQuery)) {
                return;
            }

            final Collection<ReportField> reportFields = viewRequest.getView().getReportFields();
            final Set<String> projectedColumnNames = collectSelectedColumnNames(innerQuery.getSelectClause());
            final Map<Long, ColumnDetail> columnDetailsCache = new HashMap<>();
            final Map<String, Table> normalTableMap = new HashMap<>();
            final List<DerivedTable> derivedTables = new ArrayList<>();
            final Collection<Table> tables = innerQuery.getTables();

            for (final Table table : tables) {

                if (table instanceof DerivedTable derivedTable) {
                    derivedTables.add(derivedTable);
                } else {
                    normalTableMap.put(table.getName().toLowerCase(), table);
                }
            }

            for (final ReportField reportField : reportFields) {
                final Collection<Long> columnIds = getCaseWhenColumnIds(reportField);

                if (columnIds.isEmpty()) {
                    continue;
                }

                for (final Long columnId : columnIds) {
                    final ColumnDetail columnDetail = columnDetailsCache.computeIfAbsent(columnId, this::resolveColumnDetails);

                    if (columnDetail == null) {
                        continue;
                    }

                    final String columnName = columnDetail.getColumnName();

                    if (projectedColumnNames.contains(columnName)) {
                        continue;
                    }

                    final String sourceTableKey = columnDetail.getTableDetail().getTableName().toLowerCase();
                    final Table matchedTable = normalTableMap.get(sourceTableKey);

                    if (matchedTable != null) {
                        innerQuery.addSelectClause(matchedTable.getColumn(columnName));
                        projectedColumnNames.add(columnName);
                        continue;
                    }

                    for (final DerivedTable derivedTable : derivedTables) {
                        ensureCaseWhenColumnsProjected(derivedTable.getSelectQuery());

                        if (isAlreadyProjected(derivedTable.getSelectQuery(), columnName)) {
                            innerQuery.addSelectClause(derivedTable.getColumn(columnName, columnName));
                            projectedColumnNames.add(columnName);
                            break;
                        }
                    }
                }
            }
        }

        /**
         * Recursively projects a column identified by {@code columnId} through the derived table hierarchy.
         * Uses the shared {@code projectedColumns} cache to avoid rebuilding column name sets.
         *
         * @param derivedTable the derived table to start projection from
         * @param columnId     the column ID to project
         * @return {@code true} if the column was found and projected
         */
        private boolean projectColumnRecursively(final DerivedTable derivedTable, final Long columnId,
                                                 final Map<Long, ColumnDetail> columnDetailsCache) {
            final ColumnDetail columnDetail = columnDetailsCache.computeIfAbsent(columnId, viewRequest::getColumnDetails);

            if (columnDetail == null) {
                return false;
            }

            final String columnName = columnDetail.getColumnName();
            final String targetTableName = columnDetail.getTableDetail().getTableName();
            final SelectQuery query = derivedTable.getSelectQuery();
            final Collection<Table> tables = query.getTables();
            final Set<String> currentProjected = getProjectedColumnNames(query);

            for (final Table table : tables) {

                if (!(table instanceof DerivedTable) && table.getName().equalsIgnoreCase(targetTableName)) {

                    if (currentProjected.add(columnName)) {
                        query.addSelectClause(table.getColumn(columnName));
                    }
                    return true;
                }
            }

            for (final Table table : tables) {

                if (table instanceof final DerivedTable nestedDerived && projectColumnRecursively(nestedDerived, columnId,
                        columnDetailsCache)) {

                    if (currentProjected.add(columnName) && !isAlreadyProjectedByAlias(query, nestedDerived, columnName)) {
                        query.addSelectClause(nestedDerived.getColumn(columnName, columnName));
                    }
                    return true;
                }
            }
            return false;
        }

        /**
         * Checks whether a column with the given name or alias is already present in the query's projection cache.
         * O(1) lookup via cached sets.
         *
         * @param query      the select query to check
         * @param columnName the column name to look for (case-insensitive)
         * @return {@code true} if the column is already projected by name or alias
         */
        private boolean isAlreadyProjectedByNameOrAlias(final SelectQuery query, final String columnName) {
            return getProjectedColumnNames(query).contains(columnName)
                    || getProjectedAliasList(query).contains(columnName.toLowerCase());
        }

        /**
         * Checks whether a column with the given name is already present in the query's projection cache.
         *
         * @param query      the select query to check
         * @param columnName the column name to look for (case-insensitive)
         * @return {@code true} if the column is already projected
         */
        private boolean isAlreadyProjected(final SelectQuery query, final String columnName) {
            return getProjectedColumnNames(query).contains(columnName);
        }

        /**
         * Checks whether a column is already projected in the outer query under a JSON alias.
         * Uses cached JSON alias mappings for O(1) lookup instead of iterating SELECT clauses.
         *
         * @param outerQuery    the outer select query to check
         * @param nested        the nested derived table mapping raw names to aliases
         * @param rawColumnName the raw column name to check for
         * @return {@code true} if the column's alias is already projected
         */
        private boolean isAlreadyProjectedByAlias(final SelectQuery outerQuery, final DerivedTable nested, final String rawColumnName) {
            final String alias = resolveJsonColumnAlias(nested, rawColumnName);

            return alias != null && isAlreadyProjected(outerQuery, alias);
        }

        /**
         * Finds the alias of a JSON column for the given raw column name.
         * Uses cached mappings per query to avoid repeated SELECT clause iteration.
         * Falls back to nested derived tables recursively.
         *
         * @param derivedTable  the derived table to search
         * @param rawColumnName the raw column name to find
         * @return the alias if found, otherwise {@code null}
         */
        private String resolveJsonColumnAlias(final DerivedTable derivedTable, final String rawColumnName) {
            if (derivedTable == null || rawColumnName == null) {
                return null;
            }

            final SelectQuery selectQuery = derivedTable.getSelectQuery();
            final String alias = getJsonAliasMappings(selectQuery).get(rawColumnName.toLowerCase());

            if (alias != null) {
                return alias;
            }
            final Collection<Table> tables = selectQuery.getTables();

            for (final Table table : tables) {

                if (table instanceof DerivedTable nestedDerivedTable) {
                    final String nestedAlias = resolveJsonColumnAlias(nestedDerivedTable, rawColumnName);

                    if (nestedAlias != null) {
                        return nestedAlias;
                    }
                }
            }
            return null;
        }

        /**
         * Extracts the column IDs referenced by the CASE-WHEN configuration of a report field.
         *
         * @param reportField the report field to extract CASE-WHEN column IDs from
         * @return a list of column IDs, or an empty list if no CASE-WHEN config exists
         */
        private List<Long> getCaseWhenColumnIds(final ReportField reportField) {
            final Collection<CaseWhenConfig> caseWhenConfigs = reportField.getCaseWhenConfig();

            if (caseWhenConfigs == null) {
                return Collections.emptyList();
            }

            final List<Long> result = new ArrayList<>();
            final Map<String, Long> columnDetailsPatternMap = viewRequest.getColumnDetailsPatternMap();

            for (final CaseWhenConfig caseWhenConfig : caseWhenConfigs) {
                final Long columnId = columnDetailsPatternMap.get(caseWhenConfig.getColumn());

                if (columnId != null) {
                    result.add(columnId);
                }
            }
            return result;
        }

        /**
         * Resolves {@link ColumnDetail} for the given column ID, first from request, then from cache.
         *
         * @param columnId the column ID to resolve
         * @return the resolved column details, or {@code null} if not found
         */
        private ColumnDetail resolveColumnDetails(final Long columnId) {
            final ColumnDetail columnDetail = viewRequest.getColumnDetails(columnId);

            return columnDetail == null ? columnDetailsService.get(columnId) : columnDetail;
        }

        /**
         * Creates or retrieves a cached {@link DerivedTable} for the given query and table name.
         *
         * @param query     the select query for the derived table
         * @param tableName the logical table name (registry key)
         * @param alias     the alias for the derived table (will be sanitized)
         * @return the created or cached derived table
         */
        private DerivedTable createDerivedTable(final SelectQuery query, final String tableName, final String alias) {
            final DerivedTable existing = derivedTableRegistry.get(tableName);
            final DerivedTable derived = existing != null ? existing : DerivedTable.get(DatabaseType.OLAP, tableName, sanitizeAlias(alias), query);

            derivedTableRegistry.put(tableName, derived);

            return derived;
        }

        /**
         * Creates a derived table for the parent table in a relation with an FK IN-filter.
         *
         * @param context the relation context providing parent table and column details
         * @return a new derived table representing the parent table with FK filter
         */
        private DerivedTable createParentDerivedTable(final RelationContext context) {
            final Table parentTable = context.getParentTable();
            final SelectQuery parentQuery = SelectQuery.get(parentTable);
            final String parentColumnName = context.getParentColumnName();
            final String parentTableName = context.getParentTableName();

            parentQuery.addSelectClause(parentTable.getColumn(parentColumnName));
            applyIsDeletedFilter(parentQuery, parentTable);
            addFkFilter(parentQuery, parentTable, parentColumnName);

            return createDerivedTable(parentQuery, parentTableName, String.format("%s_outer_derived", parentTableName));
        }

        /**
         * Adds an IN-filter to the given query against values from the filtered CTE table.
         *
         * @param query      the select query to add the filter to
         * @param table      the table containing the column to filter
         * @param columnName the column name to apply the IN-filter on
         */
        private void addFkFilter(final SelectQuery query, final Table table, final String columnName) {
            final SelectQuery subQuery = SelectQuery.get(filteredTable);

            subQuery.addSelectClause(findOrAddColumn(filteredTable.getSelectQuery(), columnName));

            final Criteria inCriteria = Criteria.in(table.getColumn(columnName), subQuery);
            final WhereClause existing = query.getWhereClause();

            if (existing == null) {
                query.setWhereClause(new WhereClause(inCriteria));
            } else {
                query.setWhereClause(existing.and(inCriteria));
            }
        }

        /**
         * Ensures the specified column is projected in the query's SELECT clause.
         * Uses an identity-based projection cache to avoid redundant checks and hash collisions.
         *
         * @param query      the select query to ensure projection in
         * @param columnName the column name to project
         */
        private void ensureColumnProjected(final SelectQuery query, final String columnName) {
            if (!getProjectedColumnNames(query).add(columnName)) {
                return;
            }

            final Table table = findTableForColumn(query, columnName);

            if (table == null) {
                return;
            }

            if (table instanceof DerivedTable derivedTable) {
                ensureColumnProjected(derivedTable.getSelectQuery(), columnName);
                query.addSelectClause(derivedTable.getColumn(columnName, columnName));
                return;
            }

            query.addSelectClause(table.getColumn(columnName));
        }

        /**
         * Finds an existing column in the SELECT clause or adds it from the appropriate table.
         *
         * @param query      the select query to search and modify
         * @param columnName the column name to find or add
         * @return the found or newly added column
         * @throws IllegalStateException if no table can be found for the column
         */
        private Column findOrAddColumn(final SelectQuery query, final String columnName) {
            final SortedSet<Column> selectClause = query.getSelectClause();

            for (final Column column : selectClause) {

                if (column.getColumnName().equals(columnName)) {
                    return column;
                }
            }

            final Table table = findTableForColumn(query, columnName);

            if (table != null) {
                final Column newColumn = table.getColumn(columnName);

                query.addSelectClause(newColumn);
                return newColumn;
            }

            throw ErrorCode.get(ConfigErrorCodes.INVALID_COLUMN_CONFIGURATION);
        }

        /**
         * Searches the query's tables for one that contains the specified column.
         * First checks column cache, then falls back to schema-level definition lookup.
         *
         * @param query      the select query whose tables to search
         * @param columnName the column name to look for
         * @return the table containing the column, or {@code null} if not found
         */
        private Table findTableForColumn(final SelectQuery query, final String columnName) {
            final Collection<Table> tables = query.getTables();
            Table fallback = null;

            for (final Table table : tables) {

                if (tableHasColumn(table, columnName)) {
                    return table;
                }

                if (fallback == null && tableHasColumnInDefinition(table, columnName)) {
                    fallback = table;
                }

                if (table instanceof DerivedTable derivedTable) {
                    final Table nestedTable = findTableForColumn(derivedTable.getSelectQuery(), columnName);

                    if (nestedTable != null) {
                        return table;
                    }
                }
            }
            return fallback;
        }

        /**
         * Checks whether the given table contains a column in its column cache.
         *
         * @param table      the table to check
         * @param columnName the column name to look for
         * @return {@code true} if the column is cached in the table
         */
        private boolean tableHasColumn(final Table table, final String columnName) {
            try {
                return table.containsColumn(columnName);
            } catch (final Exception exception) {
                return false;
            }
        }

        /**
         * Checks whether the given table can resolve the column from its table definition.
         *
         * @param table      the table to check
         * @param columnName the column name to look for
         * @return {@code true} if the table owns the column at the schema level
         */
        private boolean tableHasColumnInDefinition(final Table table, final String columnName) {
            try {
                table.getColumn(columnName);
                return true;
            } catch (final Exception exception) {
                return false;
            }
        }

        // ==================== HELPER METHODS ====================

        /**
         * Validates whether the view has a non-null report path mapping with relations.
         *
         * @param view the analytics report view to validate
         * @return {@code true} if the view has a valid path mapping with relations
         */
        private boolean hasValidPathMapping(final V view) {
            final ReportPathMapping reportPathMapping = view.getReportPathMapping();
            final PathInfo pathInfo = reportPathMapping.getPathInfo();

            if (pathInfo == null) {
                return false;
            }

            return Objects.nonNull(pathInfo.getRelationsInPaths());
        }

        /**
         * Returns the path relations sorted by relation index in ascending order.
         * Uses in-place sort instead of stream for better performance on hot path.
         *
         * @param pathInfo the path info containing relations
         * @return a sorted list of relations
         */
        private List<RelationsInPath> getSortedRelations(final PathInfo pathInfo) {
            final List<RelationsInPath> sorted = new ArrayList<>(pathInfo.getRelationsInPaths());

            sorted.sort(Comparator.comparingInt(RelationsInPath::getRelationIndex));

            return sorted;
        }

        /**
         * Sanitizes a derived table alias by replacing non-alphanumeric characters
         * (except underscores) with underscores and converting to lowercase.
         *
         * @param alias the raw alias string to sanitize
         * @return the sanitized alias string
         */
        private String sanitizeAlias(final String alias) {
            return ALIAS_SANITIZE_PATTERN.matcher(alias).replaceAll("_").toLowerCase();
        }

        // ==================== RELATION CONTEXT ====================

        /**
         * Encapsulates the resolved parent/child table and column details for a single
         * {@link TableRelation}, providing convenience methods for CTE membership checks.
         */
        private class RelationContext {
            @Getter
            private final DerivedTable innerQuery;
            private final ColumnDetail parentColumnDetail;
            private final ColumnDetail childColumnDetail;
            private final String parentTableName;
            private final String childTableName;

            /**
             * Constructs the {@link RelationContext} by resolving parent and child column details
             * from the given {@link TableRelation}.
             *
             * @param tableRelation the table relation providing parent/child column IDs
             * @param innerQuery    the current inner derived table
             */
            public RelationContext(final TableRelation tableRelation, final DerivedTable innerQuery) {
                final ColumnRelation columnRelation = tableRelation.getColumnRelation();

                this.innerQuery = innerQuery;
                this.parentColumnDetail = viewRequest.getColumnDetails(columnRelation.getParentColId());
                this.childColumnDetail = viewRequest.getColumnDetails(columnRelation.getChildColId());
                this.parentTableName = parentColumnDetail != null ? parentColumnDetail.getTableDetail().getTableName() : "";
                this.childTableName = childColumnDetail != null ? childColumnDetail.getTableDetail().getTableName() : "";
            }

            /**
             * Checks whether both parent and child tables are already covered by the CTE.
             *
             * @return {@code true} if both tables are in the CTE
             */
            boolean isBothTablesInCte() {
                return isParentInCte() && isChildInCte();
            }

            /**
             * Checks whether the parent table is already covered by the CTE.
             *
             * @return {@code true} if the parent table is in the CTE
             */
            boolean isParentInCte() {
                return parentColumnDetail != null && cteTables.contains(parentTableName);
            }

            /**
             * Checks whether the child table is already covered by the CTE.
             *
             * @return {@code true} if the child table is in the CTE
             */
            boolean isChildInCte() {
                return childColumnDetail != null && cteTables.contains(childTableName);
            }

            /**
             * Returns the parent table name.
             *
             * @return the parent table name
             */
            String getParentTableName() {
                return parentTableName;
            }

            /**
             * Returns the parent column name, or empty string if parent column details are null.
             *
             * @return the parent column name
             */
            String getParentColumnName() {
                return parentColumnDetail != null ? parentColumnDetail.getColumnName() : StringUtil.EMPTY_STRING;
            }

            /**
             * Creates and returns a new OLAP {@link Table} instance for the parent table.
             *
             * @return the parent {@link Table}
             */
            Table getParentTable() {
                return Table.get(DatabaseType.OLAP, parentTableName);
            }

            /**
             * Returns the child table name.
             *
             * @return the child table name
             */
            String getChildTableName() {
                return childTableName;
            }

            /**
             * Returns the child column name, or empty string if child column details are null.
             *
             * @return the child column name
             */
            String getChildColumnName() {
                return childColumnDetail != null ? childColumnDetail.getColumnName() : StringUtil.EMPTY_STRING;
            }

            /**
             * Creates and returns a new OLAP {@link Table} instance for the child table.
             *
             * @return the child {@link Table}
             */
            Table getChildTable() {
                return Table.get(DatabaseType.OLAP, childTableName);
            }
        }
    }

    /**
     * Applies an IS_DELETED = false filter to the given query if the table contains
     * the IS_DELETED column. This ensures soft-deleted fact table records are excluded.
     *
     * @param query the select query to apply the filter to
     * @param table the table to check for the IS_DELETED column
     */
    protected void applyIsDeletedFilter(final SelectQuery query, final Table table) {
        try {
            final Criteria criteria = Criteria.eq(table.getColumn(DEFAULT_FILTER_COLUMN), false);
            final WhereClause existing = query.getWhereClause();

            if (existing != null) {
                query.setWhereClause(existing.and(criteria));
            } else {
                query.setWhereClause(new WhereClause(criteria));
            }
        } catch (final Exception ignored) {
            LOGGER.debug("Analytics: Query Converter - Skipping IS_DELETED filter for table [{}] — column not found at schema level", table.getName());
        }
    }

    // ==================== REPORT AGGREGATION BUILDER ====================

    /**
     * Applies aggregation, operation, function, ordering, and group-by clauses
     * to the outer select query for each report field.
     *
     * @param viewRequest the data view request
     * @param selectQuery the outer select query to apply aggregation to
     */
    protected void applyReportAggregation(final Request viewRequest, final SelectQuery selectQuery) {
        final V view = viewRequest.getView();
        final boolean isUnderlyingRequest = viewRequest.isUnderlyingRequest();
        final Collection<ReportField> reportFields = view.getReportFields();
        final Set<Long> columnIds = reportFields.stream().map(ReportField::getColumnId).collect(Collectors.toSet());

        final Map<Long, ColumnDetail> columnDetailsMap = columnDetailsService.getAll(columnIds).stream()
                .collect(Collectors.toMap(ColumnDetail::id, columnDetails -> columnDetails));

        List.copyOf(selectQuery.getSelectClause()).forEach(selectQuery::removeSelectClause);

        for (final ReportField reportField : reportFields) {
            final Long columnId = reportField.getColumnId();
            final ColumnDetail columnDetail = columnDetailsMap.get(columnId);

            if (columnDetail == null) {
                continue;
            }

            final Pair<Column, Column> pair = applyOperationAndGet(viewRequest.getChartUniqueId(), reportField, viewRequest, selectQuery);
            final Column columnKey = pair.getKey();

            if (columnKey == null) {
                continue;
            }

            final Column columnValue = pair.getValue();
            final Axis axis = reportField.getAxis();
            final boolean isKpi = view instanceof KPIView && Axis.X_AXIS == axis;

            if (Axis.X_AXIS == axis && !isUnderlyingRequest) {
                final DataType dataType = columnKey.getColumnDefinition().getDataType();
                final Criteria nullCheckCriteria;

                if (CommonDataType.TEXT == dataType) {
                    nullCheckCriteria = Criteria.notEq(columnKey, StringUtil.EMPTY_STRING);
                } else {
                    nullCheckCriteria = dataType.isArray() ? OlapCriteria.notHasAny(columnKey, null)
                            : Criteria.notEq(columnKey, null);
                }

                selectQuery.setWhereClause(WhereClauseUtil.connect(selectQuery.getWhereClause(),
                        new WhereClause(nullCheckCriteria), Connector.AND));

                if (!isKpi) {
                    selectQuery.addOrderByClause(new OrderByClause(columnKey));
                }
            }

            final ReportFieldCustomRange reportFieldCustomRange = reportField.getReportFieldCustomRange();

            if (reportFieldCustomRange != null) {
                selectQuery.setWhereClause(WhereClauseUtil.connect(selectQuery.getWhereClause(),
                        new WhereClause(Criteria.between(columnKey, reportFieldCustomRange.getFrom(),
                                reportFieldCustomRange.getTo())), Connector.AND));
            }

            buildAggregationGroupByClause(columnValue, reportField, selectQuery, viewRequest);

            final Column finalColumn = resolveAggregationFunctionColumn(columnValue, reportField, columnDetail, isUnderlyingRequest);

            if (!isKpi) {
                selectQuery.addSelectClause(finalColumn != null ? finalColumn : columnValue);
            }
        }
    }

    /**
     * Resolves and builds a function column based on report field configuration.
     *
     * @param columnValue         Column on which function may be applied
     * @param reportField         Report field configuration
     * @param columnDetail       Metadata of the column
     * @param isUnderlyingRequest Whether this is an underlying data request
     * @return Function-applied column OR null if no transformation needed
     */
    private Column resolveAggregationFunctionColumn(final Column columnValue, final ReportField reportField,
                                                    final ColumnDetail columnDetail, final boolean isUnderlyingRequest) {
        final String functionName = reportField.getFunctionName();
        final DataType columnDataType = columnValue.getColumnDefinition().getDataType();

        if (functionName != null) {
            // Skip arrayJoin for underlying data — underlying data shows raw array values.
            // arrayJoin explodes arrays into separate rows and removes rows with empty/null
            // arrays, causing contacts without tags/multiselect fields to disappear.
            if (isUnderlyingRequest && "array".equalsIgnoreCase(functionName)) {
                return null;
            }
            return buildAggregationFunctionColumn(columnValue, functionName);
        }

        if (isUnderlyingRequest) {
            if (reportField.getCaseWhenAsString() == null) {
                return null;
            }

            return OperationApplier.getForNumericalField()
                    .applyAndGet(columnValue, OperationType.getOperationType(
                            reportField.getCaseWhenConfig().iterator().next().getOperationId()));
        }

        final Integer axisType = reportField.getAxisType();

        if (axisType == null || axisType != NumericalConstant.ONE) {
            return null;
        }

        if (columnDetail.getDataType() == AnalyticsDataType.DATE) {
            return buildAggregationFunctionColumn(columnValue, "date");
        }

        if (columnDataType.isArray()) {
            final JsonColumn arrayJsonColumn = columnValue instanceof JsonColumn jsonColumn ? jsonColumn : null;

            return buildAggregationArrayFunctionColumn(columnValue, arrayJsonColumn, (arrayJsonColumn != null)
                    ? isAggregationJsonDateType(arrayJsonColumn, columnDataType) : columnDataType.isDate());
        }

        if (columnDataType.isJson()) {
            final OlapJsonColumn olapJsonColumn = (OlapJsonColumn) columnValue;

            if (olapJsonColumn.isArray()) {
                final boolean isJsonArrayDateType = isAggregationJsonDateType(olapJsonColumn, columnDataType);

                return buildAggregationArrayFunctionColumn(columnValue, olapJsonColumn, isJsonArrayDateType);
            }
        }

        return null;
    }

    /**
     * Builds a function-applied column based on the given function name and column metadata.
     *
     * @param column       The input column on which function will be applied
     * @param functionName The function name to apply (e.g., "date", "array", etc.)
     * @return A new Column with the function applied, or transformed column based on logic
     */
    private Column buildAggregationFunctionColumn(final Column column, final String functionName) {
        final String functionNameLowerCase = functionName.toLowerCase();
        final DataType columnDataType = column.getColumnDefinition().getDataType();
        final JsonColumn aggregatedJsonColumn = column instanceof JsonColumn jsonColumn ? jsonColumn : null;
        final boolean isDateType = (aggregatedJsonColumn != null) ? isAggregationJsonDateType(aggregatedJsonColumn, columnDataType) : columnDataType.isDate();

        if (isDateType) {
            return buildAggregationDateTimeFunctionColumn(column, functionNameLowerCase, null);
        }

        if (HOUR_OF_THE_DAY.equals(functionNameLowerCase)) {
            return buildAggregationDateTimeFunctionColumn(column, functionNameLowerCase, buildAggregationTimezoneArgument(column));
        }

        return buildAggregationDateTimeFunctionColumn(column, functionNameLowerCase,
                FunctionArgument.create(OlapSqlFunctionBuilderFactory.toTimeZone(column, TimezoneInfo.UTC.getTimeZone().getID()).build()));
    }

    /**
     * Determines whether a JSON column represents a DATE type.
     *
     * @param jsonColumn     The JSON column instance
     * @param columnDataType The base column datatype
     * @return true if it represents a DATE type, false otherwise
     */
    private boolean isAggregationJsonDateType(final JsonColumn jsonColumn, final DataType columnDataType) {
        final DataType returnType = jsonColumn.getReturnType();

        return returnType.isDate() || columnDataType.isDate();
    }

    /**
     * Builds a timezone function argument for the given column.
     *
     * <p><b>Note:</b> This is invoked lazily only when required to avoid unnecessary overhead.</p>
     *
     * @param column The column to which timezone conversion will be applied
     * @return FunctionArgument representing timezone conversion
     */
    private FunctionArgument buildAggregationTimezoneArgument(final Column column) {

        final UserRegionalPreference userRegionalPreference = ((CrmUser) LoggedInUserInfo.getUser()).getPreferenceHolder()
                .get(UserRegionalPreference.TYPE);

        return FunctionArgument.create(OlapSqlFunctionBuilderFactory.toTimeZone(column,
                        userRegionalPreference.getTimezoneInfo().getTimeZone().getID()).build()
        );
    }

    /**
     * Builds the arrayJoin function column with appropriate return type.
     *
     * @param column     the source column
     * @param jsonColumn the JSON column cast (nullable)
     * @param isDateType whether the column is a date type
     * @return the arrayJoin function column
     */
    private Column buildAggregationArrayFunctionColumn(final Column column, final JsonColumn jsonColumn, final boolean isDateType) {
        return column.function(OlapSqlFunctionBuilderFactory.arrayJoin().addArguments(column).setReturnType(getDataType(jsonColumn,
                        isDateType, column.getColumnDefinition())), column.getColumnAlias(), NumericalConstant.ZERO);
    }

    /**
     * Builds a date/time function column. When {@code timezoneArgument} is null (pure date types),
     * uses the column directly; when non-null, wraps with timezone conversion.
     *
     * @param column           the source column
     * @param functionName     the normalized (lowercase) function name
     * @param timezoneArgument timezone function argument (null for pure date types)
     * @return the function-wrapped column, or {@code null} if the function name is unknown
     */
    private Column buildAggregationDateTimeFunctionColumn(final Column column, final String functionName,
                                                          final FunctionArgument timezoneArgument) {
        final boolean isDateOnly = (timezoneArgument == null);
        final String alias = column.getColumnAlias();

        return switch (functionName) {
            case "date" -> isDateOnly ? column.function(OlapSqlFunctionBuilderFactory.date().addArguments(column), alias, NumericalConstant.ZERO)
                    : column.function(OlapSqlFunctionBuilderFactory.date().addArgument(timezoneArgument), timezoneArgument, alias, NumericalConstant.ZERO);
            case "hour_of_the_day" -> {
                if (isDateOnly) {
                    final FunctionArgument hourArg = FunctionArgument.create(OlapSqlFunctionBuilderFactory.toDateTime(column).build());

                    yield column.function(OlapSqlFunctionBuilderFactory.hourOfTheDay().addArgument(hourArg), hourArg, alias, NumericalConstant.ZERO);
                }
                yield column.function(OlapSqlFunctionBuilderFactory.hourOfTheDay().addArgument(timezoneArgument), timezoneArgument, alias, NumericalConstant.ZERO);
            }
            case "day_of_the_week" -> applyAggregationDateFunction(column, OlapSqlFunctionBuilderFactory.dayOfTheWeek(), alias, isDateOnly, timezoneArgument);
            case "day_of_the_month" -> applyAggregationDateFunction(column, OlapSqlFunctionBuilderFactory.dayOfTheMonth(), alias, isDateOnly, timezoneArgument);
            case "quarter_of_the_year" -> applyAggregationDateFunction(column, OlapSqlFunctionBuilderFactory.quarterOfTheYear(), alias, isDateOnly, timezoneArgument);
            case "month_of_the_year" -> applyAggregationDateFunction(column, OlapSqlFunctionBuilderFactory.monthOfTheYear(), alias, isDateOnly, timezoneArgument);
            case "by_month" -> applyAggregationDateFunction(column, OlapSqlFunctionBuilderFactory.byMonth(), alias, isDateOnly, timezoneArgument);
            case "by_week" -> applyAggregationDateFunction(column, OlapSqlFunctionBuilderFactory.byWeek(), alias, isDateOnly, timezoneArgument);
            case "by_year" -> applyAggregationDateFunction(column, OlapSqlFunctionBuilderFactory.byYear(), alias, isDateOnly, timezoneArgument);
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_COLUMN_CONFIGURATION, "i18n.analytics.unsupported.function.name");
        };
    }

    /**
     * Applies a date function to the column, with or without timezone conversion.
     *
     * @param column           the source column
     * @param functionBuilder  the SQL function builder (e.g., dayOfTheWeek, byMonth)
     * @param alias            the column alias
     * @param isDateOnly       whether to skip timezone conversion
     * @param timezoneArgument the timezone function argument (used when not date-only)
     * @return the function-wrapped column
     */
    private Column applyAggregationDateFunction(final Column column, final com.tlc.sql.common.api.dml.function.SqlFunction.Builder functionBuilder,
                                                final String alias, final boolean isDateOnly,
                                                final FunctionArgument timezoneArgument) {
        return isDateOnly ? column.function(functionBuilder.addArguments(column), alias, NumericalConstant.ZERO)
                : column.function(functionBuilder.addArguments(column), timezoneArgument, alias, NumericalConstant.ZERO);
    }

    /**
     * Adds a GROUP BY clause to the select query based on the report field's configuration.
     *
     * @param columnKey   the column to group by when operation is ACTUAL
     * @param reportField the report field containing group-by configuration
     * @param selectQuery the select query to add GROUP BY clauses to
     * @param viewRequest the data view request for column detail lookups
     */
    private void buildAggregationGroupByClause(final Column columnKey,
                                               final ReportField reportField,
                                               final SelectQuery selectQuery,
                                               final Request viewRequest) {
        final Long groupByColumnId = reportField.getGroupByColumnId();

        if (groupByColumnId != null) {
            final ColumnDetail columnDetail = viewRequest.getColumnDetails(groupByColumnId);
            final TableDetail tableDetail = columnDetail.getTableDetail();
            final String tableName = tableDetail.getTableName();
            final Table table = tableDetail.getAliasName() == null ? Table.get(DatabaseType.OLAP, tableName)
                    : Table.get(DatabaseType.OLAP, tableName, tableDetail.getAliasName());

            selectQuery.addGroupByClause(new GroupByClause(table.getColumn(columnDetail.getColumnName())));
        }

        if (OperationType.ACTUAL == reportField.getOperationType()
                && (!viewRequest.isUnderlyingRequest()
                || Objects.isNull(reportField.getCaseWhenConfig()))) {
            selectQuery.addGroupByClause(new GroupByClause(columnKey));
        }
    }

    /**
     * Resolves the effective data type for a column, considering JSON structure,
     * array types, and date-specific transformations.
     *
     * @param jsonColumn  JSON column instance (nullable if column is not JSON)
     * @param isDateType  Flag indicating whether the column should be treated as DATE type
     * @param columnDef   Column definition containing base metadata
     * @return Resolved {@link DataType} based on input conditions
     */
    private static DataType getDataType(final JsonColumn jsonColumn, final boolean isDateType, final ColumnDefinition columnDef) {
        final DataType dataType = columnDef.getDataType();

        return isDateType ? OlapDataType.DATE : (jsonColumn != null ? jsonColumn.getReturnType()
                 : (dataType.isArray() ? ((ArrayColumnDefinition) columnDef).getElementType() : dataType));
    }

    // ==================== FILTER HANDLING ====================

    /**
     * Applies a filter condition to the select query based on the filter field's data type.
     *
     * @param filterField the filter field definition containing operator and value
     * @param selectQuery the select query to add the filter condition to
     * @param viewRequest the data view request for column detail lookups
     */
    void handleFilter(final FilterField filterField, final SelectQuery selectQuery, final Request viewRequest) {
        final UserRegionalPreference userRegionalPreference = ((CrmUser) LoggedInUserInfo.getUser()).getPreferenceHolder()
                .get(UserPreferenceType.USER_REGIONAL_PREFERENCE);
        final TimezoneInfo timezoneInfo = userRegionalPreference.getTimezoneInfo();
        final Pair<Column, Column> column = applyOperationAndGet(null, filterField, viewRequest, selectQuery);

        if (column.getKey() == null) {
            return;
        }
        final Column columnValue = column.getValue();
        final Integer havingOperationType = filterField.getHavingOperationType();
        final Column operationColumn = havingOperationType == null ? columnValue
                : OperationApplier.getForNumericalField().applyAndGet(columnValue, OperationType.getOperationType(havingOperationType));
        final ColumnDetail columnDetail = viewRequest.getColumnDetails(filterField.getColumnId());
        final AnalyticsDataType analyticsDataType = Objects.nonNull(filterField.getCustomFieldConfigAsString())
                ? columnDetail.getDataType(filterField.getStoreType()) : columnDetail.getDataType();

        switch (analyticsDataType) {
            case NUMERICAL ->
                    FilterApplier.getNumericFilter().apply(selectQuery, operationColumn, filterField, viewRequest::getColumnDetails, timezoneInfo);
            case STRING ->
                    FilterApplier.getTxtFilter().apply(selectQuery, operationColumn, filterField, viewRequest::getColumnDetails, timezoneInfo);
            case DATE ->
                    FilterApplier.getDateFilter().apply(selectQuery, operationColumn, filterField, viewRequest::getColumnDetails, timezoneInfo);
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_DATA_TYPE, "i18n.analytics.invalid.data.type");
        }
    }

    // ==================== COLUMN OPERATION ====================

    /**
     * Resolves the column for the given field, applies custom field adaptation and
     * CASE-WHEN conditional logic, then applies the appropriate operation.
     *
     * @param chartUniqueId the unique chart identifier for column alias generation
     * @param field         the base field to process
     * @param viewRequest   the data view request for column detail lookups
     * @param selectQuery   the select query providing table context
     * @return a {@link Pair} of (adapted column, operation-applied column)
     */
    Pair<Column, Column> applyOperationAndGet(final Long chartUniqueId, final @NonNull BaseField field,
                                              final Request viewRequest,
                                              final SelectQuery selectQuery) {
        final OperationType operationType = field.getOperationType();
        final ColumnDetail columnDetail = viewRequest.getColumnDetails(field.getColumnId());
        final Table table = fetchTable(columnDetail.getTableDetail(), selectQuery);
        final String columnName = columnDetail.getColumnName();
        final String columnAliasName = computeColumnAlias(chartUniqueId, columnName, field, viewRequest.isApplyConfiguration(),
                collectSelectedColumnNames(selectQuery.getSelectClause()), viewRequest.isUnderlyingRequest());
        final Column adaptedColumn;

        if (table instanceof final DerivedTable derivedTable && field.getCustomFieldConfig().isPresent()
                && isColumnAliasProjectedInDerived(derivedTable, columnAliasName)) {
            adaptedColumn = derivedTable.getColumn(columnAliasName, columnAliasName);
        } else {
            adaptedColumn = adaptColumnForCustomFieldIfNeeded(columnAliasName, table.getColumn(columnName, columnAliasName), field);
        }

        final Column newColumn = field.getCaseWhenAsString() == null ? adaptedColumn
                : buildConditionalColumn(columnAliasName, adaptedColumn, viewRequest.getColumnDetailsPatternMap(), field);

        if (newColumn == null) {
            return Pair.of(null, null);
        }

        if (OperationType.ACTUAL == operationType) {
            return Pair.of(adaptedColumn, newColumn);
        }

        final AnalyticsDataType analyticsDataType = columnDetail.getDataType(field.getStoreType());

        final OperationApplier operationApplier = switch (analyticsDataType) {
            case NUMERICAL -> OperationApplier.getForNumericalField();
            case DATE -> OperationApplier.getForDateField();
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_DATA_TYPE, "i18n.analytics.invalid.data.type");
        };

        if (field.isPreCalculated()) {
            return Pair.of(adaptedColumn, operationApplier.applyAndGet(newColumn, OperationType.SUM));
        }

        return Pair.of(adaptedColumn, operationApplier.applyAndGet(newColumn, operationType));
    }

    /**
     * Checks whether the given column alias is already projected in a derived table's
     * inner SELECT clause.
     *
     * @param derivedTable the derived table to check
     * @param aliasName    the column alias name to look for
     * @return {@code true} if the alias is found
     */
    private boolean isColumnAliasProjectedInDerived(final DerivedTable derivedTable, final String aliasName) {
        final SortedSet<Column> selectClause = derivedTable.getSelectQuery().getSelectClause();

        for (final Column column : selectClause) {

            if (aliasName.equalsIgnoreCase(column.getColumnName()) || aliasName.equalsIgnoreCase(column.getColumnAlias())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves the appropriate {@link Table} from the select query for the given table details.
     *
     * @param tableDetail the table details to resolve
     * @param selectQuery  the select query containing available tables
     * @return the resolved table reference
     */
    private Table fetchTable(final TableDetail tableDetail, final SelectQuery selectQuery) {
        final Table baseTable = selectQuery.getBaseTable();
        final Collection<Table> tables = selectQuery.getTables();

        if (tables.size() == NumericalConstant.ONE) {
            return baseTable;
        }

        final String tableName = tableDetail.getTableName();
        final String baseTableName = baseTable.getName();

        if (baseTableName.equals(tableName)) {
            return baseTable;
        }

        Table fallback = null;

        for (final Table table : tables) {
            final String name = table.getName();

            if (name.equals(tableName)) {
                return table;
            }

            if (fallback == null && !name.equals(baseTableName)) {
                fallback = table;
            }
        }

        return fallback != null ? fallback : baseTable;
    }

    /**
     * Builds an OLAP {@link ConditionalColumn} (CASE-WHEN) from the field's CASE-WHEN configuration.
     *
     * @param columnAlias       the alias for the resulting conditional column
     * @param actualColumn      the actual column to use in WHEN clause results
     * @param caseWhenColumnMap mapping of case-when column pattern keys to column IDs
     * @param field             the base field containing CASE-WHEN configuration
     * @return the constructed conditional column
     */
    private ConditionalColumn<?, ?> buildConditionalColumn(final String columnAlias, final Column actualColumn,
                                                           final Map<String, Long> caseWhenColumnMap, final BaseField field) {
        final List<ConditionalColumn.WhenClause> whenClauses = new ArrayList<>();
        final Collection<CaseWhenConfig> caseWhenConfigs = field.getCaseWhenConfig();

        for (final CaseWhenConfig caseWhenConfig : caseWhenConfigs) {
            final Object valueForCriteria = caseWhenConfig.getValue().size() == NumericalConstant.ONE
                    ? caseWhenConfig.getValue().get(NumericalConstant.ZERO) : caseWhenConfig.getValue();
            final Column columnForCriteria = actualColumn.getTable().getColumn(columnDetailsService.get(caseWhenColumnMap
                    .get(caseWhenConfig.getColumn())).getColumnName());
            final Criteria criteria = buildCriteria(valueForCriteria, columnForCriteria, OperatorType.getOperatorType(caseWhenConfig.getOperator()));

            whenClauses.add(ConditionalColumn.WhenClause.create(new WhereClause(criteria), actualColumn));
        }
        return OlapConditionalColumn.when(whenClauses).column(actualColumn).alias(columnAlias).build();
    }

    /**
     * Builds a {@link Criteria} based on the given operator type.
     *
     * @param value        the value to compare against
     * @param column       the column to apply the criteria on
     * @param operatorType the comparison operator type
     * @return the constructed criteria
     */
    private Criteria buildCriteria(final Object value, final Column column, final OperatorType operatorType) {
        return switch (operatorType) {
            case IS -> Criteria.eq(column, value);
            case IS_NOT -> Criteria.notEq(column, value);
            case GREATER_THAN -> Criteria.gt(column, CommonBaseModelKeys.CURRENT_TIME.equals(value.toString())
                    ? System.currentTimeMillis() : (Number) value);
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_CRITERIA_OPERATOR_TYPE);
        };
    }

    /**
     * Generates a unique column alias based on the chart ID, column name, field metadata,
     * and optional filter function name.
     *
     * @param chartUniqueId       the unique chart identifier
     * @param columnName          the base column name
     * @param field               the base field providing field ID and operation type
     * @param isUnderlyingRequest whether this is an underlying data request
     * @return the generated column alias string
     */
    private String getColumnAlias(final Long chartUniqueId, final String columnName, final BaseField field,
                                  final boolean isUnderlyingRequest) {
        String functionName = null;

        if (field instanceof final FilterField filterField) {
            final FilterFunctionMapper filterFunctionMapper = filterField.getFilterFunctionMapper();

            functionName = filterFunctionMapper == null ? null : filterFunctionMapper.getFunctionName();
        }

        if (isUnderlyingRequest) {
            return String.format("%s%s", columnName, field.getFieldId());
        }
        return String.format("%s_%s_%s_%s_%s", columnName, field.getOperationType().getTypeCode(), field.getFieldId(), functionName, chartUniqueId);
    }

    /**
     * Applies a configurable SQL function to the given column if one exists.
     * Uses loop-based iteration instead of streams for performance.
     * <p>
     * This method is designed to be overridden by subclasses (e.g., KPI) to add
     * query-level side effects such as WHERE clause modifications.
     *
     * @param column        the column to apply the function to
     * @param columnDetail the column details for function lookup
     * @param viewRequest   the data view request for parent column resolution
     * @param selectQuery   the select query
     */
    void applyFunctionInterval(final @NonNull Column column, final @NonNull ColumnDetail columnDetail, final @NonNull Request viewRequest,
                               final SelectQuery selectQuery) {
    }

    /**
     * Adapts the column for custom fields by extracting JSON path values if applicable.
     *
     * @param columnAliasName the alias for the resulting column
     * @param originalColumn  the original column reference
     * @param field           the base field containing custom field config
     * @return the adapted JSON column, or the original column if not a custom field
     */
    private Column adaptColumnForCustomFieldIfNeeded(final String columnAliasName, final Column originalColumn, final BaseField field) {
        if (originalColumn instanceof OlapJsonColumn) {
            return originalColumn;
        }

        final Optional<AnalyticsCustomFieldConfig> customFieldConfig = field.getCustomFieldConfig();
        final StoreType storeType = field.getStoreType();
        final DataType dataType = getReturnType(storeType);

        if (customFieldConfig.isEmpty()) {
            return originalColumn;
        }

        final String index = String.valueOf(customFieldConfig.get().getIndex());

        if (OACDIMCONTACT.MONETARY_FIELDS.equals(originalColumn.getColumnName())) {
            return originalColumn.jsonColumn(columnAliasName, dataType, dataType.isArray()).path(index, "value", "value");
        } else {
            return originalColumn.jsonColumn(columnAliasName, dataType, dataType.isArray()).path(index, "value");
        }
    }

    /**
     * Maps a {@link StoreType} to the corresponding SQL data type for JSON column extraction.
     *
     * @param storeType the store type to map
     * @return the corresponding SQL data type, or {@code null} if unsupported
     */
    private DataType getReturnType(final StoreType storeType) {
        return switch (storeType) {
            case SINGLE_NUMBER, OWNER, COMPANY, CONTACT, MONETARY -> CommonDataType.NULLABLE_BIGINT;
            case SINGLE_STRING, EMAIL, PHONE, DROPDOWN, TEXT -> CommonDataType.TEXT;
            case MULTI_SELECT -> CommonDataType.ARRAY;
            case DATE -> OlapDataType.NULLABLE_DATE;
            default -> null;
        };
    }
}
