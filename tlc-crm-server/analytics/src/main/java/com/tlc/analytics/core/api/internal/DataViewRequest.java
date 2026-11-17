package com.tlc.analytics.core.api.internal;

import com.tlc.analytics.config.function.ConfigurableFunctionDetail;
import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.analytics.config.model.view.ViewModel;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.config.model.view.report.dynamic.underlying.AnalyticsSort;
import com.tlc.analytics.config.model.table.TableRelation;

import com.tlc.commons.pair.Pair;

import com.tlc.l10n.temporal.model.TimezoneInfo;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * {@link DataViewRequest} represents a request for Views, extending {@link DefaultViewRequest} and specifying a generic type V that extends {@link ViewModel}.
 * This class includes information about the root table and column details for the requested view.
 *
 * @author Aswath
 * @version 1.0
 */
@SuperBuilder
@Getter
public class DataViewRequest<V extends ViewModel> extends DefaultViewRequest<V>
{
    /**
     * Details of the root table for the view.
     */
    private final TableDetail rootTableDetail;

    /**
     * Column details mapped by column identifier.
     */
    @Getter( AccessLevel.NONE )
    private final @Singular( "columnDetail" ) Map<Long, ColumnDetail> columnDetailsMap;

    @Getter( AccessLevel.NONE )
    private final @Singular( "tableDetail" ) Map<Long, TableDetail> tableDetailMap;

    /**
     * Table relations mapped by join identifier.
     */
    @Getter( AccessLevel.NONE )
    private final @Singular( "tableRelation" ) Map<Long, TableRelation> tableRelationMap;

    @Getter( AccessLevel.NONE )
    private final @Singular( "ANAConfigurableFunctionDetails" ) Map<Long, ConfigurableFunctionDetail> configurableFunctionDetailsMap;

    private final OperationType operationType;

    private Map<String, Long> columnNameDetails;

    private Map<String, Pair<String, Long>> configuredColumnDetails;


    private boolean isApplyConfiguration;

    @Setter
    private Integer start;

    @Setter
    private Integer limit;
    private TimezoneInfo timezoneInfo;
    private Pair<ReportField, AnalyticsSort> reportFieldSort;
    private Long chartUniqueId;

    @Setter
    private Map<String, Map<Long, Table>> derivedTable;
    private boolean isUnderlyingRequest;
    private Map<String, Long> columnDetailsPatternMap;
    private boolean isExport;

    /**
     * Retrieves the details of a column based on the provided column ID.
     *
     * @param columnId The ID of the column for which details are requested. Must not be null.
     * @return The details of the column.
     */
    public ColumnDetail getColumnDetails(final @NonNull Long columnId )
    {
        return columnDetailsMap.get( columnId );
    }

    /**
     * Retrieves the details of a table using the provided table ID.
     *
     * @param tableId the ID of the table must not be null
     * @return the TableDetail object corresponding to the tableId, or null if not found
     */
    public TableDetail getTableDetails(final @NonNull Long tableId )
    {
        return tableDetailMap.get( tableId );
    }

    /**
     * Retrieves the relation of a table using the provided relation ID.
     *
     * @param relationId the ID of the table relation must not be null
     * @return the TableRelation object corresponding to the relationId, or null if not found
     */
    public TableRelation getTableRelation( final @NonNull Long relationId )
    {
        return tableRelationMap.get( relationId );
    }

    /**
     * Retrieves the configurable function details associated with the given column ID.
     *
     * @param columnId   Represents the column id
     * @return The configurable function details associated with the column ID, or null if not found.
     */
    public ConfigurableFunctionDetail getConfigurableFunctionDetails(final @NonNull Long columnId )
    {
        return configurableFunctionDetailsMap.get( columnId );
    }
}
