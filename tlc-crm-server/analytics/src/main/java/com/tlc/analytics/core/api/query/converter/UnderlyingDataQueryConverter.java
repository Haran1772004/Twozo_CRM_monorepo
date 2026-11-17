package com.tlc.analytics.core.api.query.converter;

import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.table.ColumnRelation;
import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.config.model.table.TableRelation;
import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.analytics.config.model.view.report.AnalyticsReport;
import com.tlc.analytics.config.model.view.report.ChartView;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.config.model.view.report.dynamic.underlying.AnalyticsSort;
import com.tlc.analytics.config.model.view.report.path.PathInfo;
import com.tlc.analytics.config.model.view.report.path.RelationsInPath;
import com.tlc.analytics.config.model.view.report.path.ReportPathMapping;
import com.tlc.analytics.core.api.ChartRequest;
import com.tlc.analytics.core.api.UnderlyingDataRequest;
import com.tlc.analytics.core.api.internal.DataViewRequest;

import com.tlc.commons.pair.Pair;

import com.tlc.crm.common.models.SortType;
import com.tlc.crm.entity.filter.date.DateIntervals;
import com.tlc.l10n.temporal.interval.Interval;

import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.With;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.function.SqlFunction;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.olap.api.dml.OlapSelectQuery;
import com.tlc.sql.olap.api.dml.function.OlapSqlFunctionBuilderFactory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.SortedSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Query converter for underlying data requests.
 *
 * <p>
 * Builds a performance-optimized flat query structure:
 * <pre>
 * WITH filtered_data AS (SELECT ... FROM root_table WHERE ...)
 * SELECT ... FROM filtered_data
 *   LEFT JOIN fact_table1 ON ...
 *   LEFT JOIN fact_table2 ON ...
 *   LEFT JOIN dim_table1  ON ...
 * GROUP BY ... LIMIT ...
 * </pre>
 * <p>
 * Key differences from chart query ({@link AbstractReportSelectQueryConverter}):
 * <ul>
 *   <li>Flat LEFT JOINs instead of nested derived tables with INNER JOINs</li>
 *   <li>filtered_data (small dataset) is always the FROM/base table ? drives all scans</li>
 *   <li>All base entity rows preserved even when related tables have no match</li>
 *   <li>No intermediate derived table materialization ? better memory usage</li>
 *   <li>ClickHouse optimizer can plan all joins together</li>
 * </ul>
 * <p>
 * Common infrastructure (CTE building, filter handling, aggregation, column operations)
 * is inherited from {@link AbstractReportSelectQueryConverter}.
 *
 * @param <V>       The type of report view associated with the request, e.g., {@link ChartView}.
 * @param <Request> The type of data view request, e.g., {@link ChartRequest}.
 * @author Aswath
 * @version 3.0 - Flat LEFT JOIN architecture for underlying data
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class UnderlyingDataQueryConverter<V extends AnalyticsReport, Request extends DataViewRequest<V>>
        extends AbstractReportSelectQueryConverter<V, Request> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnderlyingDataQueryConverter.class);

    private static class Instance {
        private static final UnderlyingDataQueryConverter INSTANCE = new UnderlyingDataQueryConverter();
    }

    /**
     * Returns singleton instance.
     *
     * @return {@link UnderlyingDataQueryConverter} singleton
     */
    public static UnderlyingDataQueryConverter INSTANCE() {
        return Instance.INSTANCE;
    }

    /**
     * Converts the underlying data view request into a flat LEFT JOIN query.
     * <p>
     * Flow:
     * <ol>
     *   <li>Build filtered CTE using parent's {@link FilteredCteBuilder} (common)</li>
     *   <li>Build flat outer query with LEFT JOINs for all path relations</li>
     *   <li>Apply aggregation using parent's {@link AbstractReportSelectQueryConverter#applyReportAggregation} (common)</li>
     *   <li>Apply pagination (LIMIT)</li>
     *   <li>Apply sorting (ORDER BY)</li>
     *   <li>Wrap in WITH clause</li>
     * </ol>
     *
     * @param viewRequest The view request containing details to generate the select query.
     * @return The generated WITH query with flat LEFT JOINs.
     * @throws Exception If any error occurs during the conversion process.
     */
    @Override
    public @NonNull With convert(final @NonNull Request viewRequest) throws Exception {
        final Long orgId = viewRequest.getOrgId();

        LOGGER.info("Analytics: Underlying Data Converter - Constructing query (flat LEFT JOIN architecture), orgId [{}]", orgId);
        final SelectQuery filteredCteQuery = new FilteredCteBuilder(viewRequest).build();

        applyKpiDurationFilter(viewRequest, filteredCteQuery);
        ensureJoinKeyColumnsInCte(viewRequest, filteredCteQuery);

        final List<DerivedTable> derivedTables = new ArrayList<>();
        final DerivedTable filteredCteTable = DerivedTable.get(DatabaseType.OLAP, filteredCteQuery.getBaseTable().getName(), CTE_ALIAS, filteredCteQuery);

        filteredCteTable.setWithClauseCTE(true);
        derivedTables.add(filteredCteTable);

        final SelectQuery outerQuery = buildFlatLeftJoinQuery(viewRequest, filteredCteTable);

        applyReportAggregation(viewRequest, outerQuery);

        if (!requiresGroupBy(viewRequest)) {
            final List<GroupByClause> groupByClauses = new ArrayList<>(outerQuery.getGroupByClause());

            for (final GroupByClause groupByClause : groupByClauses) {
                outerQuery.removeGroupByClause(groupByClause);
            }
            LOGGER.debug("Analytics: Underlying Data Converter - Removed all GROUP BY (no aggregation required), orgId [{}]", orgId);
        }
        final Integer startingIndex = viewRequest.getStart();
        final Integer endingIndex = viewRequest.getLimit();

        if (startingIndex != null && endingIndex != null) {
            outerQuery.setLimitClause(new LimitClause(startingIndex, endingIndex));
        }

        applySorting(viewRequest, outerQuery);

        LOGGER.info("Analytics: Underlying Data Converter - Query constructed (flat LEFT JOIN architecture), orgId [{}]", orgId);

        final With.Builder withBuilder = With.Builder.create();

        derivedTables.forEach(withBuilder::addDerivedTable);
        withBuilder.selectQuery(outerQuery);

        return withBuilder.build();
    }


    /**
     * Ensures that all join key columns required by the outer query's LEFT JOINs
     * are projected in the CTE's SELECT clause.
     * <p>
     * The {@link FilteredCteBuilder} only projects columns that correspond to report fields.
     * However, the flat LEFT JOIN outer query needs join key columns (e.g., {@code ID},
     * {@code CONTACT_ID}) to be available from {@code filtered_data}. Without these columns
     * in the CTE's SELECT, the outer query's JOIN conditions would reference non-existent columns.
     * <p>
     * This method iterates through all path relations, identifies which join key columns
     * belong to the root table (or tables already joined in the CTE), and adds them to
     * the CTE's SELECT clause if they are not already projected.
     *
     * @param viewRequest      the data view request containing path relation info
     * @param filteredCteQuery the CTE select query to add join key columns to
     */
    private void ensureJoinKeyColumnsInCte(final Request viewRequest, final SelectQuery filteredCteQuery) {
        final V view = viewRequest.getView();
        final ReportPathMapping reportPathMapping = view.getReportPathMapping();

        if (reportPathMapping != null) {
            final PathInfo pathInfo = reportPathMapping.getPathInfo();

            if (pathInfo == null || pathInfo.getRelationsInPaths() == null) {
                return;
            }
            final String rootTableName = viewRequest.getRootTableDetail().getTableName();
            final Set<String> projectedColumns = new HashSet<>();
            final SortedSet<Column> selectClause = filteredCteQuery.getSelectClause();

            for (final Column column : selectClause) {
                projectedColumns.add(column.getColumnName());
            }

            for (final RelationsInPath relation : pathInfo.getRelationsInPaths()) {
                final TableRelation tableRelation = viewRequest.getTableRelation(relation.getRelationId());

                if (tableRelation == null) {
                    continue;
                }
                final ColumnRelation columnRelation = tableRelation.getColumnRelation();
                final ColumnDetail parentColDetails = viewRequest.getColumnDetails(columnRelation.getParentColId());
                final ColumnDetail childColDetails = viewRequest.getColumnDetails(columnRelation.getChildColId());

                if (parentColDetails == null || childColDetails == null) {
                    continue;
                }
                final String parentTableName = parentColDetails.getTableDetail().getTableName();
                final String parentColumnName = parentColDetails.getColumnName();
                final Table baseTable = filteredCteQuery.getBaseTable();

                if (rootTableName.equals(parentTableName) && projectedColumns.add(parentColumnName)) {
                    filteredCteQuery.addSelectClause(baseTable.getColumn(parentColumnName));
                    LOGGER.debug("Analytics: Underlying Data Converter - Projected join key column: {}.{} into CTE", parentTableName, parentColumnName);
                }
                final String childTableName = childColDetails.getTableDetail().getTableName();
                final String childColumnName = childColDetails.getColumnName();

                if (rootTableName.equals(childTableName) && projectedColumns.add(childColumnName)) {
                    filteredCteQuery.addSelectClause(baseTable.getColumn(childColumnName));
                    LOGGER.debug("Analytics: Underlying Data Converter - Projected join key column: {}.{} into CTE", childTableName, childColumnName);
                }
            }
        }
    }

    /**
     * Builds a flat SELECT query with filtered_data as the FROM/base table
     * and all path relations as flat LEFT JOINs at the same level.
     * <p>
     * Generated structure:
     * <pre>
     * FROM filtered_data
     *   LEFT JOIN fact_contact_company ON filtered_data.ID = fact_contact_company.CONTACT_ID
     *   LEFT JOIN fact_contact_deal    ON filtered_data.ID = fact_contact_deal.CONTACT_ID
     *   LEFT JOIN dim_company          ON fact_contact_company.COMPANY_ID = dim_company.ID
     *   LEFT JOIN dim_territory        ON dim_company.TERRITORY = dim_territory.ID
     * </pre>
     *
     * @param viewRequest   the data view request with path relation info
     * @param filteredTable the filtered CTE derived table (base table for FROM)
     * @return the flat select query with all LEFT JOINs
     */
    private SelectQuery buildFlatLeftJoinQuery(final Request viewRequest, final DerivedTable filteredTable) {
        final Long orgId = viewRequest.getOrgId();
        final SelectQuery flatQuery = OlapSelectQuery.get(filteredTable);
        final V view = viewRequest.getView();

        if (!pathExits(view)) {
            LOGGER.debug("Analytics: Underlying Data Converter - No valid path mapping, returning simple filtered query, orgId [{}]", orgId);
            return flatQuery;
        }
        final List<RelationsInPath> sortedRelations = view.getReportPathMapping().getPathInfo().getRelationsInPaths().stream()
                .sorted(Comparator.comparingInt(RelationsInPath::getRelationIndex)).toList();

        if (sortedRelations.isEmpty()) {
            return flatQuery;
        }

        LOGGER.debug("Analytics: Underlying Data Converter - Building flat query with {} LEFT JOINs, orgId [{}]", sortedRelations.size(), orgId);
        final Map<String, Table> joinedTableMap = new HashMap<>();

        joinedTableMap.put(viewRequest.getRootTableDetail().getTableName(), filteredTable);

        for (final RelationsInPath relation : sortedRelations) {
            final TableRelation tableRelation = viewRequest.getTableRelation(relation.getRelationId());

            if (tableRelation == null) {
                LOGGER.warn("Analytics: Underlying Data Converter - TableRelation not found for relationId: {}, orgId [{}]", relation.getRelationId(), orgId);
                continue;
            }
            final ColumnRelation columnRelation = tableRelation.getColumnRelation();
            final ColumnDetail parentColDetails = viewRequest.getColumnDetails(columnRelation.getParentColId());
            final ColumnDetail childColDetails = viewRequest.getColumnDetails(columnRelation.getChildColId());

            if (parentColDetails == null || childColDetails == null) {
                LOGGER.warn("Analytics: Underlying Data Converter - Column details not found for relation: {}, orgId [{}]", relation.getRelationId(), orgId);
                continue;
            }
            final String parentTableName = parentColDetails.getTableDetail().getTableName();
            final String childTableName = childColDetails.getTableDetail().getTableName();
            final String parentColumnName = parentColDetails.getColumnName();
            final String childColumnName = childColDetails.getColumnName();
            final boolean parentJoined = joinedTableMap.containsKey(parentTableName);
            final boolean childJoined = joinedTableMap.containsKey(childTableName);

            if (parentJoined && childJoined) {
                LOGGER.debug("Analytics: Underlying Data Converter - Both {} and {} already joined, skipping, orgId [{}]", parentTableName, childTableName, orgId);
                continue;
            }

            if (parentJoined) {
                final Table localTable = joinedTableMap.get(parentTableName);
                final Table childTable = Table.get(DatabaseType.OLAP, childTableName);

                flatQuery.addJoinClause(new JoinClause(localTable, parentColumnName, childTable, childColumnName, JoinClause.JoinType.LEFT));
                joinedTableMap.put(childTableName, childTable);
                applyIsDeletedFilter(flatQuery, childTable);

                LOGGER.debug("Analytics: Underlying Data Converter - LEFT JOIN {} ON {}.{} = {}.{}, orgId [{}]", childTableName, localTable.getAlias(), parentColumnName, childTableName, childColumnName, orgId);
            } else if (childJoined) {
                final Table localTable = joinedTableMap.get(childTableName);
                final Table parentTable = Table.get(DatabaseType.OLAP, parentTableName);

                flatQuery.addJoinClause(new JoinClause(localTable, childColumnName, parentTable, parentColumnName, JoinClause.JoinType.LEFT));
                joinedTableMap.put(parentTableName, parentTable);
                LOGGER.debug("Analytics: Underlying Data Converter - LEFT JOIN {} ON {}.{} = {}.{}, orgId [{}]", parentTableName, localTable.getAlias(), childColumnName,
                        parentTableName, parentColumnName, orgId);
            } else {
                final Table childTable = Table.get(DatabaseType.OLAP, childTableName);

                flatQuery.addJoinClause(new JoinClause(filteredTable, parentColumnName, childTable, childColumnName, JoinClause.JoinType.LEFT));
                joinedTableMap.put(childTableName, childTable);
                LOGGER.debug("Analytics: Underlying Data Converter - LEFT JOIN {} (neither in query, using CTE), orgId [{}]", childTableName, orgId);
            }
        }
        LOGGER.debug("Analytics: Underlying Data Converter - Flat query completed with {} LEFT JOINs, orgId [{}]", joinedTableMap.size() - 1, orgId);
        return flatQuery;
    }

    private boolean pathExits(final V view) {
        final ReportPathMapping reportPathMapping = view.getReportPathMapping();

        if (reportPathMapping == null) {
            return false;
        }
        final PathInfo pathInfo = reportPathMapping.getPathInfo();

        return pathInfo != null && pathInfo.getRelationsInPaths() != null;
    }

    /**
     * Checks whether GROUP BY is required for the underlying data query.
     * <p>
     * GROUP BY is needed when any report field requires aggregation, which is indicated by:
     * <ul>
     *   <li>A non-ACTUAL {@link OperationType} (e.g., SUM, COUNT, AVG) ? the field itself
     *       is wrapped in an aggregate function</li>
     *   <li>A non-null {@code groupByColumnId} ? the field has a configured GROUP BY dependency,
     *       meaning it participates in aggregation (e.g., DEAL_VALUE with CASE-WHEN that should
     *       produce {@code SUM(multiIf(STATE='won', DEAL_VALUE, NULL))} grouped by contact columns)</li>
     * </ul>
     * When no field requires aggregation (all are raw ACTUAL without groupByColumnId),
     * GROUP BY just acts like DISTINCT and causes duplicate rows.
     *
     * @param viewRequest the data view request containing report fields
     * @return {@code true} if GROUP BY should be kept in the query
     */
    private boolean requiresGroupBy(final Request viewRequest) {
        final V view = viewRequest.getView();
        final Collection<ReportField> reportFields = view.getReportFields();

        if (reportFields == null) {
            return false;
        }

        for (final ReportField reportField : reportFields) {
            final OperationType operationType = reportField.getOperationType();

            if (operationType != null && operationType != OperationType.ACTUAL) {
                return true;
            }

            if (reportField.getGroupByColumnId() != null) {
                return true;
            }

            if (Objects.nonNull(reportField.getCaseWhenAsString())) {
                return true;
            }

        }
        return false;
    }

    /**
     * Applies sorting (ORDER BY) to the outer query based on the view request's sort configuration.
     * <p>
     * For underlying data queries, the sort must reference a column that is already in the
     * outer SELECT clause. This is important because:
     * <ul>
     *   <li>Root table columns come from the {@code filtered_data} CTE, not the original OLAP table</li>
     *   <li>Aggregated columns (e.g., {@code SUM(CASE WHEN ... THEN DEAL_VALUE END) AS DEAL_VALUE41})
     *       must be referenced by their SELECT alias, not the raw table column</li>
     * </ul>
     *
     * @param viewRequest the data view request containing sort info
     * @param outerQuery  the outer select query to apply sorting to
     */
    private void applySorting(final Request viewRequest, final SelectQuery outerQuery) {
        final Pair<ReportField, AnalyticsSort> analyticsSort = viewRequest.getReportFieldSort();

        if (analyticsSort != null && analyticsSort.getKey() != null) {
            final Long orgId = viewRequest.getOrgId();
            final ReportField sortField = analyticsSort.getKey();
            LOGGER.debug("Analytics: Underlying Data Converter - Applying sort on field: {}, orgId [{}]", sortField.getFieldName(), orgId);

            final ColumnDetail columnDetail = viewRequest.getColumnDetails(sortField.getColumnId());
            final String columnName = columnDetail.getColumnName();
            final SortedSet<Column> selectClause = outerQuery.getSelectClause();
            Column sortColumn = null;

            for (final Column selectCol : selectClause) {
                final String alias = selectCol.getColumnAlias();

                if (alias != null && alias.startsWith(columnName)) {
                    sortColumn = selectCol;
                    break;
                }

                if (columnName.equals(selectCol.getColumnName())) {
                    sortColumn = selectCol;
                    break;
                }
            }

            if (sortColumn == null) {
                final TableDetail tableDetail = columnDetail.getTableDetail();
                final String tableName = tableDetail.getTableName();
                final Table table;

                if (viewRequest.getRootTableDetail().getTableName().equals(tableName)) {
                    table = outerQuery.getBaseTable();
                } else {
                    final String aliasName = tableDetail.getAliasName();

                    table = aliasName == null ? Table.get(DatabaseType.OLAP, tableName) : Table.get(DatabaseType.OLAP, tableName, aliasName);
                }
                sortColumn = table.getColumn(columnName);
            }
            final OrderByClause.OrderType orderType = SortType.ASCENDING == analyticsSort.getValue().type()
                    ? OrderByClause.OrderType.ASCENDING : OrderByClause.OrderType.DESCENDING;

            outerQuery.addOrderByClause(new OrderByClause(sortColumn, orderType));
        }
    }

    /**
     * Applies the KPI duration time period filter to the CTE query when the underlying data
     * request originates from a KPI view.
     * <p>
     * The KPI's duration (e.g., TODAY, THIS_WEEK, THIS_MONTH, THIS_QUARTER, THIS_YEAR, CUSTOM)
     * defines a time range that filters the KPI chart data. When a user drills down into
     * underlying data from a KPI, the same time period must be applied to ensure the
     * underlying records match the KPI's scope.
     * <p>
     * This method checks if the view request carries KPI duration context
     * ({@code operationType} and {@code durationColumnId} from {@link UnderlyingDataRequest}).
     * If present, it resolves the date interval using {@link DateIntervals}, converts the
     * start/end times to ClickHouse {@code fromUnixTimestamp64Milli} expressions, and adds
     * a {@code BETWEEN} WHERE clause to the CTE query on the duration column.
     *
     * @param viewRequest      the data view request (may be an {@link UnderlyingDataRequest} with duration context)
     * @param filteredCteQuery the CTE select query to apply the duration filter to
     */
    private void applyKpiDurationFilter(final Request viewRequest, final SelectQuery filteredCteQuery) {
        if (!(viewRequest instanceof final UnderlyingDataRequest underlyingRequest)) {
            return;
        }
        final Long orgId = viewRequest.getOrgId();
        final OperationType operationType = underlyingRequest.getOperationType();
        final Long durationColumnId = underlyingRequest.getDurationColumnId();

        if (operationType == null || durationColumnId == null) {
            return;
        }

        if (!OperationType.getKPITimeUnits().contains(operationType)) {
            return;
        }

        final ColumnDetail durationColDetails = viewRequest.getColumnDetails(durationColumnId);

        if (durationColDetails == null) {
            LOGGER.warn("Analytics: Underlying Data Converter - Duration column details not found for columnId: {}, orgId [{}]", durationColumnId, orgId);
            return;
        }
        final SqlFunction.Builder startBuilder;
        final SqlFunction.Builder endBuilder;

        if (operationType == OperationType.CUSTOM) {
            final Long from = underlyingRequest.getDurationFrom();
            final Long to = underlyingRequest.getDurationTo();

            if (from == null || to == null) {
                LOGGER.warn("Analytics: Underlying Data Converter - CUSTOM duration missing from/to values, orgId [{}]", orgId);
                return;
            }
            startBuilder = OlapSqlFunctionBuilderFactory.fromUnixTimestamp64Milli(from);
            endBuilder = OlapSqlFunctionBuilderFactory.fromUnixTimestamp64Milli(to);
        } else {
            final DateIntervals dateInterval = switch (operationType) {
                case TODAY -> DateIntervals.TODAY;
                case THIS_WEEK -> DateIntervals.THIS_WEEK;
                case THIS_MONTH -> DateIntervals.THIS_MONTH;
                case THIS_QUARTER -> DateIntervals.THIS_QUARTER;
                case THIS_YEAR -> DateIntervals.THIS_YEAR;
                default -> null;
            };

            if (dateInterval == null) {
                LOGGER.warn("Analytics: Underlying Data Converter - Unsupported duration operationType: {}, orgId [{}]", operationType, orgId);
                return;
            }
            final Interval<Long, Long> interval = dateInterval.getInterval();

            if (interval == null || interval.start() == null || interval.end() == null) {
                LOGGER.warn("Analytics: Underlying Data Converter - Date interval returned null for operationType: {}, orgId [{}]", operationType, orgId);
                return;
            }
            startBuilder = OlapSqlFunctionBuilderFactory.fromUnixTimestamp64Milli(interval.start());
            endBuilder = OlapSqlFunctionBuilderFactory.fromUnixTimestamp64Milli(interval.end());
        }
        final String durationColumnName = durationColDetails.getColumnName();
        final String durationTableName = durationColDetails.getTableDetail().getTableName();
        final Column durationColumn;

        if (viewRequest.getRootTableDetail().getTableName().equals(durationTableName)) {
            durationColumn = filteredCteQuery.getBaseTable().getColumn(durationColumnName);
        } else {
            durationColumn = Table.get(DatabaseType.OLAP, durationTableName).getColumn(durationColumnName);
        }
        filteredCteQuery.setWhereClause(WhereClauseUtil.connect(filteredCteQuery.getWhereClause(),
                new WhereClause(Criteria.between(durationColumn, startBuilder, endBuilder)), Connector.AND));

        LOGGER.info("Analytics: Underlying Data Converter - Applied KPI duration filter: {} on column {}.{}",
                operationType, durationTableName, durationColumnName);
    }
}