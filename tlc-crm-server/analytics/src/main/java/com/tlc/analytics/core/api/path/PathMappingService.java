package com.tlc.analytics.core.api.path;

import com.tlc.analytics.config.model.table.ColumnRelation;
import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.config.model.table.TableRelation;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.model.view.report.ReportField;

import com.tlc.analytics.config.model.view.report.path.RelationsInPath;
import com.tlc.analytics.config.model.view.report.path.ReportPathMapping;
import lombok.NonNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * The {@link  PathMappingService} interface defines the contract for handling operations
 * related to path mappings in report generation. It provides methods for retrieving
 * column IDs and root table details based on report and filter fields, as well as involved table details.
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public interface PathMappingService
{
    /**
     * <p>
     * Retrieves a set of unique column IDs.
     * </p>
     *
     * @param reportFields Represents the collection of {@link ReportField}
     * @param filterFields Represents the collection of {@link FilterField}
     * @return A set of column ids.
     */
    @NonNull
    Set<Long> getColumnIds( final @NonNull Collection<ReportField> reportFields,
                            final Collection<FilterField> filterFields );

    /**
     * <p>
     *  Retrieves a {@link TableDetail} entity.
     * </p>
     *
     * @param rootTableId          Represents the root table ID
     * @param involvedTableDetails Represents the collection of {@link TableDetail}
     * @return A {@link TableDetail} entity
     */
    @NonNull
    TableDetail getRootTableDetails(final @NonNull Long rootTableId,
                                    final @NonNull Collection<TableDetail> involvedTableDetails );

    /**
     * <p>
     * Loads the {@link ColumnRelation} entity ids.
     * </p>
     *
     * @param orgId             Represents the organization ID
     * @param columnIds         Represents the set of column ids.
     * @param reportPathMapping An entity that holds the {@link ReportPathMapping} details
     */
    void loadColumnRelationDetails(final @NonNull Long orgId,
                                   final @NonNull Set<Long> columnIds,
                                   final @NonNull ReportPathMapping reportPathMapping );

    /**
     * Gets the instance of {@link PathMappingService}.
     *
     * @return {@link PathMappingService}.
     */
    static PathMappingService getInstance()
    {
        return PathMappingServiceImpl.INSTANCE();
    }

    /**
     * <p>
     * Retrieves a map of table relations for the specified organization and relation paths
     * </p>
     *
     * @param orgId            Represents the organisation id
     * @param relationsInPaths a collection of {@link RelationsInPath} which containing relation ids
     * @return a map where the keys are relation ids and the values are the corresponding {@link TableRelation}
     */
    Map<Long, TableRelation> getTableRelations( final @NonNull Long orgId, final @NonNull Collection<RelationsInPath> relationsInPaths );
}
