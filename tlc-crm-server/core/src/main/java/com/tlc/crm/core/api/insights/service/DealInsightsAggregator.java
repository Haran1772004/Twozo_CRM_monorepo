package com.tlc.crm.core.api.insights.service;

import com.tlc.crm.core.api.insights.manager.EntityInsightsManager;
import com.tlc.crm.core.api.insights.manager.impl.EntityInsightsManagerImpl;
import com.tlc.crm.core.api.insights.models.DealInsights;
import com.tlc.crm.core.sql.resource.MDOCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.ConditionalColumn;
import com.tlc.sql.common.api.dml.function.NumberValue;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.oltp.api.dml.OltpConditionalColumn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
/**
 * <p>
 * Recomputes the {@code DEAL} group (open / won / lost counts) for contact
 * and company owner entities directly from {@code MDODeal}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class DealInsightsAggregator implements EntityInsightsGroupAggregator {

    private static final int STATE_OPEN = 1;
    private static final int STATE_WON = 2;
    private static final int STATE_LOST = 3;

    private static final String ALIAS_OPEN = "open_deals_count";
    private static final String ALIAS_WON = "won_deals_count";
    private static final String ALIAS_LOST = "lost_deals_count";

    private final EntityInsightsManager entityInsightsManager;

    private DealInsightsAggregator() {
        this.entityInsightsManager = EntityInsightsManagerImpl.getInstance();
    }

    private static final class InstanceHolder {
        private static final DealInsightsAggregator INSTANCE = new DealInsightsAggregator();
    }

    /**
     * <p>
     * Gets the {@link DealInsightsAggregator} singleton.
     * </p>
     *
     * @return The {@link DealInsightsAggregator} instance.
     */
    public static DealInsightsAggregator getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private OrgDataStore getOrgDataStore(final Long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public void aggregate(final long orgId, final Source source, final Collection<Long> affectedEntityIds) {
        if (Objects.isNull(affectedEntityIds) || affectedEntityIds.isEmpty()) {
            return;
        }

        if (source != Source.CONTACT && source != Source.COMPANY) {
            return;
        }
        aggregate(orgId, Map.of(source, affectedEntityIds));
    }

    /**
     * <p>
     * Recomputes the DEAL group for every applicable source in
     * {@code entityIdsBySource} in the fewest possible DB round-trips: one
     * aggregation SELECT per source, then a single combined
     * {@code createIfNotExists + UPDATE} for all sources.
     * </p>
     *
     * @param orgId             Represents the organization id.
     * @param entityIdsBySource Mapping of source to the entity ids that need recompute.
     */
    @Override
    public void aggregate(final long orgId, final Map<Source, Collection<Long>> entityIdsBySource) {
        if (Objects.isNull(entityIdsBySource) || entityIdsBySource.isEmpty()) {
            return;
        }
        final OrgDataStore orgDataStore = getOrgDataStore(orgId);
        final Map<Source, Map<Long, DealInsights>> allInsights = new HashMap<>();

        for (final Map.Entry<Source, Collection<Long>> entry : entityIdsBySource.entrySet()) {
            final Source source = entry.getKey();
            final Collection<Long> affectedEntityIds = entry.getValue();

            if (Objects.isNull(affectedEntityIds) || affectedEntityIds.isEmpty()) {
                continue;
            }

            if (source != Source.CONTACT && source != Source.COMPANY) {
                continue;
            }
            final Map<Long, DealInsights> insightsMap = (source == Source.CONTACT)
                    ? insightsForContacts(orgDataStore, affectedEntityIds)
                    : insightsForCompanies(orgDataStore, affectedEntityIds);

            for (final Long entityId : affectedEntityIds) {
                insightsMap.computeIfAbsent(entityId, key -> new DealInsights());
            }
            allInsights.put(source, insightsMap);
        }

        if (!allInsights.isEmpty()) {
            entityInsightsManager.updateDealInsights(orgId, allInsights);
        }
    }

    /**
     * <p>
     * Builds a conditional SUM column for a given deal state.
     * </p>
     *
     * @param stateColumn      Represents the state column.
     * @param stateValue       Represents the deal state value to match.
     * @param alias            Represents the column alias.
     * @param definitionColumn Represents the base table column for metadata.
     * @return The conditional sum {@link Column}.
     */
    private Column buildStateCountColumn(final Column stateColumn, final int stateValue,
                                         final String alias, final Column definitionColumn) {
        final List<ConditionalColumn.WhenClause> whenClauses = new ArrayList<>();

        whenClauses.add(ConditionalColumn.WhenClause.create(
                new WhereClause(Criteria.eq(stateColumn, stateValue)), new NumberValue(1)));

        return OltpConditionalColumn.when(whenClauses)
                .otherwise(new NumberValue(0))
                .column(definitionColumn)
                .alias(alias)
                .build()
                .sum();
    }

    /**
     * <p>
     * Builds the three state count columns (open, won, lost) for the given
     * state and definition columns.
     * </p>
     *
     * @param stateColumn      Represents the state column.
     * @param definitionColumn Represents the base table column for metadata.
     * @return Array of [openCountColumn, wonCountColumn, lostCountColumn].
     */
    private Column[] buildStateCountColumns(final Column stateColumn, final Column definitionColumn) {
        return new Column[]{
                buildStateCountColumn(stateColumn, STATE_OPEN, ALIAS_OPEN, definitionColumn),
                buildStateCountColumn(stateColumn, STATE_WON, ALIAS_WON, definitionColumn),
                buildStateCountColumn(stateColumn, STATE_LOST, ALIAS_LOST, definitionColumn)
        };
    }

    /**
     * <p>
     * Adds the three state count select clauses and a group-by clause to the
     * given query.
     * </p>
     *
     * @param selectQuery    Represents the select query to populate.
     * @param groupByColumn  Represents the column to group by.
     * @param stateCountCols Represents the [open, won, lost] count columns.
     */
    private void addStateCountClauses(final SelectQuery selectQuery, final Column groupByColumn,
                                      final Column[] stateCountCols) {
        selectQuery.addSelectClause(List.of(groupByColumn, stateCountCols[0], stateCountCols[1], stateCountCols[2]));
        selectQuery.addGroupByClause(new GroupByClause(groupByColumn));
    }

    /**
     * <p>
     * Builds a {@link DealInsights} instance from raw count values.
     * </p>
     *
     * @param openValue Represents the raw open count value.
     * @param wonValue  Represents the raw won count value.
     * @param lostValue Represents the raw lost count value.
     * @return The populated {@link DealInsights}.
     */
    private DealInsights buildDealInsights(final Long openValue, final Long wonValue, final Long lostValue) {
        final DealInsights insights = new DealInsights();

        if (Objects.nonNull(openValue)) {
            insights.setOpenDealsCount(openValue);
        }

        if (Objects.nonNull(wonValue)) {
            insights.setWonDealsCount(wonValue);
        }

        if (Objects.nonNull(lostValue)) {
            insights.setLostDealsCount(lostValue);
        }

        return insights;
    }

    /**
     * <p>
     * Reads open/won/lost counts from a result row using the state count columns.
     * </p>
     *
     * @param row            Represents the result row map.
     * @param stateCountCols Represents the [open, won, lost] count columns.
     * @return The populated {@link DealInsights}.
     */
    private DealInsights buildDealInsightsFromRow(final Map<String, Object> row, final Column[] stateCountCols) {
        return buildDealInsights(
                (Long) row.get(stateCountCols[0].getColumnAlias()),
                (Long) row.get(stateCountCols[1].getColumnAlias()),
                (Long) row.get(stateCountCols[2].getColumnAlias())
        );
    }

    /**
     * <p>
     * Aggregates deal counts for the full set of affected contacts in one query.
     * </p>
     *
     * @param dataStore  Represents the organization data store.
     * @param contactIds Represents the contact ids.
     * @return The per-contact deal aggregates.
     */
    private Map<Long, DealInsights> insightsForContacts(final OrgDataStore dataStore,
                                                        final Collection<Long> contactIds) {
        final Map<Long, DealInsights> aggregatesByContactId = new HashMap<>();
        final Table dealTable = Table.get(MDODEAL.TABLE);

        final Column contactIdColumn = dealTable.getColumn(MDODEAL.CONTACT_ID);
        final Column stateColumn = dealTable.getColumn(MDODEAL.STATE);
        final Column[] stateCountCols = buildStateCountColumns(stateColumn, contactIdColumn);

        final SelectQuery selectQuery = SelectQuery.get(dealTable);

        addStateCountClauses(selectQuery, contactIdColumn, stateCountCols);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(contactIdColumn, contactIds)));
        final List<Map<String, Object>> result = dataStore.getData(selectQuery);

        for (final Map<String, Object> row : result) {
            final Long contactId = (Long) row.get(contactIdColumn.getColumnAlias());

            if (Objects.isNull(contactId)) {
                continue;
            }
            aggregatesByContactId.put(contactId, buildDealInsightsFromRow(row, stateCountCols));
        }

        return aggregatesByContactId;
    }

    /**
     * <p>
     * Aggregates deal counts for the full set of affected companies in one
     * query via {@code MDOCompanyFields}.
     * </p>
     *
     * @param orgDataStore Represents the organization data store.
     * @param companyIds   Represents the company ids.
     * @return The per-company deal aggregates.
     */
    private Map<Long, DealInsights> insightsForCompanies(final OrgDataStore orgDataStore, final Collection<Long> companyIds) {
        final Map<Long, DealInsights> aggregatesByCompanyId = new HashMap<>();
        final Table dealTable = Table.get(MDODEAL.TABLE);
        final Table dealCompanyTable = Table.get(MDOCOMPANYFIELDS.TABLE);

        final Column dealStateColumn = dealTable.getColumn(MDODEAL.STATE);
        final Column mappingDealIdColumn = dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID);
        final Column mappingCompanyIdColumn = dealCompanyTable.getColumn(MDOCOMPANYFIELDS.COMPANY_ID);
        final Column[] stateCountColumns = buildStateCountColumns(dealStateColumn, mappingCompanyIdColumn);

        final SelectQuery selectQuery = SelectQuery.get(dealCompanyTable);

        addStateCountClauses(selectQuery, mappingCompanyIdColumn, stateCountColumns);
        selectQuery.addJoinClause(new JoinClause(mappingDealIdColumn, dealTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(mappingCompanyIdColumn, companyIds)));
        final List<Map<String, Object>> companyResults = orgDataStore.getData(selectQuery);

        for (final Map<String, Object> row : companyResults) {
            final Long companyId = (Long) row.get(mappingCompanyIdColumn.getColumnAlias());

            if (Objects.isNull(companyId)) {
                continue;
            }
            aggregatesByCompanyId.put(companyId, buildDealInsightsFromRow(row, stateCountColumns));
        }

        return aggregatesByCompanyId;
    }
}