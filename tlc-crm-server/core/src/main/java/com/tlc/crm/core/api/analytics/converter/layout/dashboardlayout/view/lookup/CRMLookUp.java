package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.view.lookup;

import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.config.model.view.report.path.PathInfo;
import com.tlc.analytics.config.model.view.report.path.RelationsInPath;
import com.tlc.analytics.config.model.view.report.path.ReportPathMapping;
import com.tlc.analytics.core.api.path.AnalyticsPathFinderService;
import com.tlc.analytics.service.column.ColumnDetailsService;
import com.tlc.analytics.service.table.TableDetailService;
import com.tlc.analytics.sql.resource.OACDIMACTIVITY;
import com.tlc.analytics.sql.resource.OACDIMCOMPANY;
import com.tlc.analytics.sql.resource.OACDIMCONTACT;
import com.tlc.analytics.sql.resource.OACDIMDEAL;
import com.tlc.analytics.sql.resource.OACDIMDEALLINEITEMS;
import com.tlc.analytics.sql.resource.OACDIMPRODUCT;
import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.api.analytics.entity.config.modules.ModuleType;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Responsible for retrieving and constructing the shortest path for lookup-related detail in CRM analytics
 * </p>
 *
 * @author jamesh
 * @version 1.0
 */
public class CRMLookUp {
    private static final Logger LOGGER = LoggerFactory.getLogger(CRMLookUp.class);

    private final ColumnDetailsService columnDetailsService;
    private final TableDetailService tableDetailService;
    private final Function<Set<Long>, Collection<TableDetail>> tableDetailsRetriever;
    private final AnalyticsPathFinderService analyticsPathFinderService;

    private CRMLookUp() {
        columnDetailsService = ColumnDetailsService.getInstance();
        tableDetailService = TableDetailService.getInstance();
        tableDetailsRetriever = (ids) -> tableDetailService.getAll(ids);
        analyticsPathFinderService = AnalyticsPathFinderService.getInstance();
    }

    private static class Instance {
        private static final CRMLookUp INSTANCE = new CRMLookUp();
    }

    public static CRMLookUp INSTANCE() {
        return CRMLookUp.Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link ReportPathMapping} details based on the root table id
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param rootTableId Represents the root table id
     * @param columnIds   Contains the collection of column ids
     * @return The {@link ReportPathMapping}
     */
    public final @NonNull ReportPathMapping getReportPathMapping(final @NonNull Long orgId,
                                                                 final @NonNull Long rootTableId,
                                                                 final @NonNull Collection<Long> columnIds,
                                                                 final Collection<TableDetail> involvedTableDetails) {
        final TableDetail rootTableDetail = tableDetailService.get(rootTableId);

        if (Objects.nonNull(rootTableDetail)) {
            final List<RelationsInPath> relationsInPaths = analyticsPathFinderService.findAndGetPath(rootTableDetail,
                    tableDetailsRetriever, new ArrayList<>(involvedTableDetails));
            LOGGER.info("CRM Analytics: Successfully fetched shortest path for view with orgId: {}", orgId);

            return ReportPathMapping.builder().orgId(orgId).pathInfo(PathInfo.builder().orgId(orgId)
                    .relationsInPaths(relationsInPaths).build()).build();
        } else {
            LOGGER.error("CRM Analytics: Failed to fetched shortest path for view with orgId: {}", orgId);
            throw ErrorCode.get(AnalyticsErrorCodes.SHORTEST_PATH_FETCH_ACTION_FAILED,
                    "i18n.analytics.view.shortest.path.fetch.action.failed");
        }
    }

    /**
     * Computes and returns the report path mapping between a root table and
     * the set of tables involved in the given column IDs.
     *
     * @param orgId        Organization ID
     * @param rootTableId  Root table ID from which paths are calculated
     * @param columnIds    Collection of column IDs used in the report
     * @return {@link ReportPathMapping} containing path information between tables
     */
    public final @NonNull ReportPathMapping getReportPathMapping(final @NonNull Long orgId,
                                                                 final @NonNull Long rootTableId,
                                                                 final @NonNull Collection<Long> columnIds) {
        final Collection<ColumnDetail> columnDetails = columnDetailsService.getAll(new HashSet<>(columnIds));
        final Set<Long> involvedTables = columnDetails.stream().map(columnDetail -> columnDetail.getTableDetail()
                .id()).collect(Collectors.toSet());
        final List<TableDetail> involvedTableDetails = new ArrayList<>(tableDetailService.getAll(involvedTables));
        final TableDetail rootTableDetail = tableDetailService.get(rootTableId);

        if (Objects.nonNull(rootTableDetail)) {
            final List<RelationsInPath> relationsInPaths = analyticsPathFinderService.findAndGetPath(rootTableDetail,
                    tableDetailsRetriever, new ArrayList<>(involvedTableDetails));
            LOGGER.info("CRM Analytics: Successfully fetched shortest path for view with orgId: {}", orgId);

            return ReportPathMapping.builder().orgId(orgId)
                    .pathInfo(PathInfo.builder().orgId(orgId).relationsInPaths(relationsInPaths).build())
                    .build();
        } else {
            LOGGER.error("CRM Analytics: Failed to fetched shortest path for view with orgId: {}", orgId);
            throw ErrorCode.get(AnalyticsErrorCodes.SHORTEST_PATH_FETCH_ACTION_FAILED,
                    "i18n.analytics.view.shortest.path.fetch.action.failed");
        }
    }

    /**
     * <p>
     * Retrieves the root table name based on the given module id
     * </p>
     *
     * @param moduleId Represents the module id
     * @return The root table name
     */
    @NonNull
    public String getRootTable(final @NonNull Integer moduleId) {
        final ModuleType moduleType = ModuleType.getModuleType(moduleId);

        return switch (moduleType) {
            case DEAL -> OACDIMDEAL.TABLE;
            case COMPANY -> OACDIMCOMPANY.TABLE;
            case CONTACT -> OACDIMCONTACT.TABLE;
            case PRODUCT -> OACDIMPRODUCT.TABLE;
            case DEAL_LINE_ITEMS -> OACDIMDEALLINEITEMS.TABLE;
            case ACTIVITY -> OACDIMACTIVITY.TABLE;
            default -> throw ErrorCode.get(AnalyticsErrorCodes.INVALID_MODULE,
                    "i18n.analytics.config.invalid.module");
        };
    }
}
