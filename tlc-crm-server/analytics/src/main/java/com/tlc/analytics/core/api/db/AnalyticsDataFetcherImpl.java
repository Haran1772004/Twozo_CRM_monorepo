package com.tlc.analytics.core.api.db;

import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.With;
import com.tlc.sql.common.api.dml.RowReceiver;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.meta.DatabaseType;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link AnalyticsDataFetcher} interface for fetching data from an organization data store.
 *
 * @author Aswath
 * @version 1.0
 */
@NoArgsConstructor( staticName = "create", access = AccessLevel.PRIVATE )
@Accessors( fluent = true )
class AnalyticsDataFetcherImpl implements AnalyticsDataFetcher
{
    private static final Logger LOGGER = LoggerFactory.getLogger( AnalyticsDataFetcherImpl.class );

    private static class Instance
    {
        private static final AnalyticsDataFetcherImpl INSTANCE = new AnalyticsDataFetcherImpl();
    }

    /**
     * Returns singleton instance of fetcher implementation.
     *
     * @return The {@link AnalyticsDataFetcherImpl} instance.
     */
    public static AnalyticsDataFetcherImpl INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    The ID of the organization from which to fetch data. Must not be null.
     * @param with     The WITH clause context containing the SELECT query and CTE definitions. Must not be null.
     * @param consumer The consumer function to process each fetched row. Must not be null.
     */
    @Override
    public void fetchData(final @NonNull Long orgId, final With with, final @NonNull Consumer<ANARow> consumer )
    {
        LOGGER.info( "Analytics: Fetching data for orgId: {}", orgId );
        final RowReceiver rowReceiver = row -> consumer.accept( ANARow.create( row ) );
        final Map<Table, RowReceiver> tableRowReceiverMap = new HashMap<>();
        final SelectQuery selectQuery = with.getSelectQuery();
        final Set<Table> tables = selectQuery.getJoinClause()
                                          .stream()
                                          .map( JoinClause::getRemoteTable )
                                          .collect( Collectors.toSet() );
        selectQuery.getSelectClause().forEach(selectClause -> tables.add( selectClause.getTable() ) );

        tables.add( selectQuery.getBaseTable() );
        tables.forEach( table -> tableRowReceiverMap.put( table, rowReceiver ) );

        getOrgDataStore( orgId ).fetchDataWithRowReceivers( with,  tableRowReceiverMap );
        LOGGER.info( "Analytics: Data fetch completed successfully, orgId [{}]", orgId );
    }

    /**
     * Retrieves the organization data store for the given organization ID.
     *
     * @param orgId The ID of the organization. Must not be null.
     * @return The organization data store corresponding to the given ID.
     */
    private OrgDataStore getOrgDataStore( final Long orgId )
    {
        return DataStoreProvider.getOrgDataStore( orgId, DatabaseType.OLAP );
    }
}
