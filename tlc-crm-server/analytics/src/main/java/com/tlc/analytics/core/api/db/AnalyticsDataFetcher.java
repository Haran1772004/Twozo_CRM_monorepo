package com.tlc.analytics.core.api.db;

import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.With;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Interface defining the operations for a data fetching API.
 *
 * @author Aswath
 * @version 1.0
 */
public interface AnalyticsDataFetcher
{
    /**
     * Fetches data based on the provided organization ID and select query,
     * and processes each fetched row using the provided consumer.
     *
     * @param orgId The organization id to fetch data for.
     * @param with The with-clause context used by the query.
     * @param consumer The consumer function to process each fetched row.
     */
    void fetchData( final @NonNull Long orgId, final With with, final @NonNull Consumer<ANARow> consumer );

    /**
     * Retrieves an instance of the data fetcher API.
     *
     * @return An instance of the data fetcher API implementation.
     */
    static AnalyticsDataFetcher getInstance()
    {
        return AnalyticsDataFetcherImpl.INSTANCE();
    }

    /**
     * Represents a single row of fetched data.
     */
    @RequiredArgsConstructor( staticName = "create", access = AccessLevel.PACKAGE )
    class ANARow
    {
        private final Map<String, Object> actualRow;

        /**
         * Iterates over each key-value pair in the row and performs the given action.
         *
         * @param consumer The action to be performed on each key-value pair. Must not be null.
         */
        public void forEach( BiConsumer<String, Object> consumer )
        {
            actualRow.forEach( consumer );
        }
    }
}
