package com.tlc.analytics.core.api.path;

import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.config.model.view.report.path.RelationsInPath;

import lombok.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * <p>
 * It responsible for generating {@link RelationsInPath} entity.
 * </P>
 *
 * @author KarthickV
 * @version 1.0
 */
public interface AnalyticsPathFinderService
{
    /**
     * Finds and returns the relationship path between the root table and the specified tables.
     *
     * @param startingTableDetail     the details of the root table to start from.
     * @param tableDetailsRetriever    function to get table details by their IDs.
     * @param involvedTables           list of tables that should be included in the relationship path.
     * @return a list of relationships between the starting table and the target tables.
     */
    List<RelationsInPath> findAndGetPath( final @NonNull TableDetail startingTableDetail,
                                          final @NonNull Function<Set<Long>, Collection<TableDetail>> tableDetailsRetriever,
                                          final @NonNull List<TableDetail> involvedTables );

    /**
     * Gets the instance of {@link AnalyticsPathFinderService}.
     *
     * @return {@link AnalyticsPathFinderService}.
     */
    static AnalyticsPathFinderService getInstance()
    {
        return AnalyticsPathFinderServiceImpl.INSTANCE();
    }
}
