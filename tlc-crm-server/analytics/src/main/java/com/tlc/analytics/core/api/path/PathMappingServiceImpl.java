package com.tlc.analytics.core.api.path;

import com.tlc.analytics.config.internal.ConfigErrorCodes;
import com.tlc.analytics.config.model.table.ColumnRelation;
import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.config.model.table.TableRelation;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.model.view.report.ReportField;

import com.tlc.analytics.config.model.view.report.path.RelationsInPath;
import com.tlc.analytics.config.model.view.report.path.ReportPathMapping;
import com.tlc.analytics.service.table.TableRelationService;
import com.tlc.commons.code.ErrorCode;

import lombok.NonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * The {@link  PathMappingServiceImpl} class provides an implementation of the {@link PathMappingService} interface.
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see PathMappingService
 */
class PathMappingServiceImpl implements PathMappingService {
    private final TableRelationService tableRelationService;

    private PathMappingServiceImpl() {
        this.tableRelationService = TableRelationService.getInstance();
    }

    private static class Instance {
        private static final PathMappingServiceImpl INSTANCE = new PathMappingServiceImpl();
    }

    public static PathMappingServiceImpl INSTANCE() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param reportFields Represents the collection of {@link ReportField}
     * @param filterFields Represents the collection of {@link FilterField}
     * @return A column IDs
     */
    @NonNull
    @Override
    public Set<Long> getColumnIds(final @NonNull Collection<ReportField> reportFields,
                                  final Collection<FilterField> filterFields) {
        final Set<Long> columnIds = reportFields.stream().map(ReportField::getColumnId).collect(Collectors.toCollection(LinkedHashSet::new));

        if (Objects.nonNull(filterFields)) {
            filterFields.forEach(filterField -> columnIds.add(filterField.getColumnId()));
        }

        return columnIds;
    }

    /**
     * {@inheritDoc}
     *
     * @param rootTableId          Represents the root table ID
     * @param involvedTableDetails Represents the collection of {@link TableDetail}
     * @return A {@link TableDetail} entity
     */
    @NonNull
    @Override
    public TableDetail getRootTableDetails(final @NonNull Long rootTableId,
                                           final @NonNull Collection<TableDetail> involvedTableDetails) {
        return involvedTableDetails.stream().filter(tableDetails -> tableDetails.id().equals(rootTableId))
                .findFirst().orElseThrow(() -> ErrorCode.get(ConfigErrorCodes.TABLE_DETAILS_NOT_FOUND));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId             Represents the organization ID
     * @param columnIds         Represents the set of column ids.
     * @param reportPathMapping An entity that holds the {@link ReportPathMapping} details
     */
    @Override
    public void loadColumnRelationDetails(final @NonNull Long orgId,
                                          final @NonNull Set<Long> columnIds,
                                          final @NonNull ReportPathMapping reportPathMapping) {
        final Collection<RelationsInPath> relationsInPaths = reportPathMapping.getPathInfo().getRelationsInPaths();

        if (Objects.nonNull(relationsInPaths)) {
            final Collection<TableRelation> tableRelations = tableRelationService.getAll(relationsInPaths.stream()
                    .map(RelationsInPath::getRelationId).collect(Collectors.toSet()));

            for (final TableRelation tableRelation : tableRelations) {
                final ColumnRelation columnRelation = tableRelation.getColumnRelation();

                columnIds.add(columnRelation.getParentColId());
                columnIds.add(columnRelation.getChildColId());
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId            Represents the organisation id
     * @param relationsInPaths a collection of {@link RelationsInPath} which containing relation ids
     * @return a map where the keys are relation ids and the values are the corresponding {@link TableRelation}
     */
    @Override
    public Map<Long, TableRelation> getTableRelations(final @NonNull Long orgId, final @NonNull Collection<RelationsInPath> relationsInPaths) {
        final Collection<TableRelation> tableRelations = tableRelationService.getAll(relationsInPaths.stream()
                .map(RelationsInPath::getRelationId).collect(Collectors.toSet()));

        return tableRelations.stream().collect(Collectors.toMap(TableRelation::id, Function.identity(),
                (a, b) -> a, HashMap::new));
    }
}
