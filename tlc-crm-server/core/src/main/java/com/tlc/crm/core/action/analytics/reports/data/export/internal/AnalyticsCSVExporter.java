package com.tlc.crm.core.action.analytics.reports.data.export.internal;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.config.keys.view.BaseFieldKeys;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.analytics.core.api.UnderlyingDataRequest;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.action.analytics.common.service.impl.ExportDataRetrievalServiceImpl;
import com.tlc.crm.core.action.analytics.reports.data.ReportEntityResolver;
import com.tlc.crm.core.action.analytics.reports.data.UserIdFetchHandler;
import com.tlc.crm.core.api.analytics.general.keys.common.CommonKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.i18n.I18nResolver;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.pk.export.csvexporter.CSVExportOptions;
import com.tlc.pk.export.csvexporter.CSVExporter;
import com.tlc.pk.export.csvexporter.data.Data;
import com.tlc.pk.export.csvexporter.strategy.CSVStrategyFactory;
import com.tlc.pk.export.csvexporter.writer.CSVWriteOptions;

import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import lombok.NonNull;
import lombok.extern.java.Log;

import org.apache.commons.collections4.map.HashedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A CSV exporter specifically for analytics data.
 *
 * @author Saravanan
 * @version 1.0
 */
@Log
class AnalyticsCSVExporter extends CSVExporter implements AnalyticsExporter
{
    private static final Logger LOGGER = LoggerFactory.getLogger( AnalyticsCSVExporter.class );
    private static final String ANALYTICS_EXPORT = "analyticsExport";
    private static final String CSV_EXTENSION = "csv";

    private static final int UNDERLYING_EXPORT_CHUNK_SIZE = NumericalConstant.ONE_HUNDRED;
    private static final int UNDERLYING_EXPORT_START_INDEX = NumericalConstant.ONE;

    private final GraphCSVDataConverter graphCSVDataConverter;
    private final UnderlyingCSVDataConverter underlyingCSVDataConverter;
    private final UserIdFetchHandler userIdFetchHandler;
    private final ExportFileManager exportFileManager;
    private final ExportDataRetrievalServiceImpl retrievalService;
    private final UserService userService;
    private final ReportEntityResolver reportEntityResolver;

    private AnalyticsCSVExporter() {
        this.graphCSVDataConverter = GraphCSVDataConverter.getInstance();
        this.underlyingCSVDataConverter = UnderlyingCSVDataConverter.getInstance();
        this.userIdFetchHandler = UserIdFetchHandler.getINSTANCE();
        this.exportFileManager = ExportFileManager.getInstance();
        this.retrievalService = ExportDataRetrievalServiceImpl.getInstance();
        this.userService = UserService.getInstance();
        this.reportEntityResolver = ReportEntityResolver.getInstance();
    }

    private static class Instance
    {
        private static final AnalyticsExporter INSTANCE = new AnalyticsCSVExporter();
    }

    public static AnalyticsExporter getInstance()
    {
        return Instance.INSTANCE;
    }

    @Override
    protected void validate()
    {
        // TODO Check permissions here.
        log.info( "VALIDATION COMPLETED IN ANALYTICS CSV EXPORTER" );
    }

    /**
     * Exports analytics data to a CSV file.
     *
     * @param orgId                  organization identifier
     * @param reqData                export request payload containing view and selected field details
     * @param request                analytics request context
     * @param actionType             action that triggered export
     * @param reportModularFieldMap  a map of field ID to {@link ReportModularField} objects containing modular field metadata
     * @param i18nResolver           resolver used for localized value conversion
     * @return generated CSV file
     */
    @Override
    public @NonNull File doExport(final @NonNull Long orgId,
                                  final @NonNull JsonObject reqData,
                                  final @NonNull AnalyticsRequest request,
                                  final @NonNull ActionType actionType,
                                  final @NonNull Map<Long, ReportModularField> reportModularFieldMap,
                                  final @NonNull I18nResolver i18nResolver) {
        final Map<Long, CrmUser> userMap = new HashedMap<>();
        final boolean isUnderlyingExport = reqData.containsKey(CRMViewPropertyKeys.CHILD_VIEW_ID);
        final ExportFileManager.ExportStreamContext streamContext = openExportStream(reqData, request);
        boolean isHeaderWritten = false;

        final JsonObject requestLimitJson = reqData.optJsonObject(CommonKeys.LIMIT);
        int start = isUnderlyingExport
                ? ExportPaginationUtil.getStartIndex(requestLimitJson, UNDERLYING_EXPORT_START_INDEX)
                : UNDERLYING_EXPORT_START_INDEX;
        final int totalAllowed = isUnderlyingExport
                ? ExportPaginationUtil.getEndIndex(requestLimitJson, start, UNDERLYING_EXPORT_CHUNK_SIZE)
                : Integer.MAX_VALUE;
        boolean hasData = false;

        final UnderlyingDataRequest builtUnderlyingRequest = isUnderlyingExport
                ? retrievalService.buildUnderlyingRequest(request, actionType, reqData, i18nResolver, reportModularFieldMap) : null;
        boolean success = false;

        try {
            while (!isUnderlyingExport || start <= totalAllowed) {
                final int currentChunkSize = isUnderlyingExport
                        ? Math.min(UNDERLYING_EXPORT_CHUNK_SIZE, totalAllowed - start + NumericalConstant.ONE)
                        : UNDERLYING_EXPORT_CHUNK_SIZE;

                final List<GraphAxis<Object>> graphAxes = isUnderlyingExport
                        ? retrievalService.fetchData(builtUnderlyingRequest, start, currentChunkSize)
                        : retrievalService.fetchData(request, actionType, reqData, i18nResolver, reportModularFieldMap);

                reportEntityResolver.replaceEntityIdsWithDisplayNames(orgId, graphAxes, reportModularFieldMap);

                if (graphAxes.isEmpty()) {
                    break;
                }

                hasData = true;
                final Collection<Long> userIds = new ArrayList<>(userIdFetchHandler.getUserIds(orgId, graphAxes, reportModularFieldMap));

                if (!userIds.isEmpty()) {
                    loadUsersIntoMap(orgId, userIds, userMap);
                }

                writeCSVChunkToStream(streamContext, orgId, graphAxes, userMap, reqData, reportModularFieldMap, i18nResolver, !isHeaderWritten);
                isHeaderWritten = true;
                graphAxes.clear();

                if (!isUnderlyingExport) {
                    break;
                }

                start += currentChunkSize;
            }

            if (!hasData) {
                throw ErrorCode.get(AnalyticsErrorCodes.EXPORT_ACTION_FAILED,
                        "i18n.analytics.config.export.via.email.empty.data");
            }
            final File result = closeExportStream(streamContext);
            success = true;

            return result;
        } finally {
            if (!success) {
                abortExportStream(streamContext);
            }
        }
    }

    /**
     * Opens the export stream for the selected analytics view.
     *
     * @param reqData request payload containing the view identifier
     * @param request analytics request context
     * @return opened export stream context
     */
    private @NonNull ExportFileManager.ExportStreamContext openExportStream(final @NonNull JsonObject reqData,
                                                                            final @NonNull AnalyticsRequest request) {
        final long viewId = reqData.getLong(BaseFieldKeys.VIEW_ID);
        final Long orgId = request.orgId();

        try {
            final String viewName = CRMViewManagerAPI.INSTANCE()
                    .getCRMChildViewManager()
                    .get(orgId, viewId)
                    .getName();
            return exportFileManager.openStream(orgId, ANALYTICS_EXPORT, viewName, CSV_EXTENSION);
        } catch (IOException exception) {
            LOGGER.error("CRM Analytics: Failed to open export stream for orgId: {}, viewId: {}", orgId, viewId, exception);
            throw ErrorCode.get(AnalyticsErrorCodes.EXPORT_ACTION_FAILED,
                    "i18n.analytics.config.export.action.failed");
        }
    }

    /**
     * Appends one chunk of analytics rows to the already opened CSV stream.
     *
     * <p>Headers are written only once during the first successful chunk write.</p>
     *
     * @param streamContext         active export stream context
     * @param orgId                 organization identifier
     * @param graphAxes             analytics graph axes to be converted into rows
     * @param userMap               user cache used while transforming axis payloads
     * @param reqData               request payload with selected/exported fields
     * @param reportModularFieldMap a map of field ID to {@link ReportModularField} objects containing modular field metadata
     * @param i18nResolver          i18n resolver used by converters
     * @param includeHeaders         whether column headers should be written for this chunk
     */
    private void writeCSVChunkToStream(final @NonNull ExportFileManager.ExportStreamContext streamContext,
                                       final @NonNull Long orgId,
                                       final @NonNull List<GraphAxis<Object>> graphAxes,
                                       final Map<Long, CrmUser> userMap,
                                       final @NonNull JsonObject reqData,
                                       final @NonNull Map<Long, ReportModularField> reportModularFieldMap,
                                       final @NonNull I18nResolver i18nResolver,
                                       final boolean includeHeaders) {
        if (graphAxes.isEmpty()) {
            return;
        }

        final Data csvData = reqData.containsKey(CRMViewPropertyKeys.CHILD_VIEW_ID)
                ? underlyingCSVDataConverter.convertToCSVRows(orgId, graphAxes, userMap, reportModularFieldMap, extractSelectFieldIds(reqData), i18nResolver)
                : graphCSVDataConverter.convertToCSVRows(orgId, graphAxes, userMap, reportModularFieldMap, i18nResolver);

        final CSVWriteOptions writeOptions = CSVWriteOptions.builder()
                .data(csvData)
                .includeHeaders(includeHeaders)
                .oStream(new NonClosingOutputStream(streamContext.getStream()))
                .csvStrategy(CSVStrategyFactory.getDefaultStrategy())
                .build();

        final CSVExportOptions exportOptions = CSVExportOptions.builder()
                .writeOptions(writeOptions)
                .build();

        try {
            this.doExport(exportOptions);
            streamContext.getStream().flush();
        } catch (final Exception exception) {
            throw ErrorCode.get(AnalyticsErrorCodes.EXPORT_ACTION_FAILED,
                    "i18n.analytics.config.export.action.failed");
        }
    }

    /**
     * Flushes and closes the export stream, returning the finalized CSV file.
     *
     * @param streamContext active export stream context
     * @return finalized exported file
     */
    private @NonNull File closeExportStream(final @NonNull ExportFileManager.ExportStreamContext streamContext) {
        try {
            return exportFileManager.closeStream(streamContext);
        } catch (IOException exception) {
            throw ErrorCode.get(AnalyticsErrorCodes.EXPORT_ACTION_FAILED,
                    "i18n.analytics.config.export.action.failed");
        }
    }

    /**
     * Aborts the export process by closing the stream and deleting the partially written file.
     *
     * @param streamContext active export stream context
     */
    private void abortExportStream(final @NonNull ExportFileManager.ExportStreamContext streamContext) {
        exportFileManager.abortStream(streamContext);
    }

    /**
     * Extracts the ordered list of fieldIds from the {@code select} array in the request payload.
     *
     * @param reqData the request JSON object
     * @return ordered list of fieldIds from {@code select}, or an empty list if not present
     */
    private static @NonNull List<Long> extractSelectFieldIds(final @NonNull JsonObject reqData) {
        final JsonArray selectArray = reqData.optJsonArray(CRMViewPropertyKeys.SELECT);

        if (Objects.isNull(selectArray) || selectArray.size() == NumericalConstant.ZERO) {
            return Collections.emptyList();
        }

        final List<Long> selectFieldIds = new ArrayList<>(selectArray.size());

        for (int i = NumericalConstant.ZERO; i < selectArray.size(); i++) {
            selectFieldIds.add(selectArray.getLong(i));
        }

        return selectFieldIds;
    }

    /**
     * Fetches users for the given user IDs that are not already present in the local lookup map
     * and stores them for use during the current export operation.
     *
     * @param orgId   organization identifier
     * @param userIds collection of user IDs required for the current export operation
     * @param userMap local lookup map of already resolved user IDs to {@link CrmUser} objects,
     *                updated in-place with any newly fetched users
     */
    private void loadUsersIntoMap(final @NonNull Long orgId,
                                  final @NonNull Collection<Long> userIds,
                                  final @NonNull Map<Long, CrmUser> userMap) {
        final List<Long> missingUserIds = userIds.stream()
                .filter(userId -> !userMap.containsKey(userId))
                .toList();

        if (missingUserIds.isEmpty()) {
            return;
        }

        final Map<Long, CrmUser> fetchedUsers = userService.getUsers(orgId, missingUserIds)
                .stream()
                .collect(Collectors.toMap(CrmUser::id, Function.identity()));

        userMap.putAll(fetchedUsers);
    }

    /**
     * Output stream wrapper that prevents delegated CSV writers from closing the shared export stream.
     *
     * <p>This allows chunked CSV writes to reuse the same underlying stream across multiple export calls.</p>
     */
    private static final class NonClosingOutputStream extends FilterOutputStream {
        /**
         * Creates a wrapper that prevents downstream writers from closing the underlying stream.
         *
         * @param outputStream the underlying output stream that remains open for subsequent writes
         */
        private NonClosingOutputStream(final @NonNull OutputStream outputStream) {
            super(outputStream);
        }

        /**
         * Flushes buffered bytes without closing the wrapped stream.
         *
         * @throws IOException if flushing the wrapped stream fails
         */
        @Override
        public void close() throws IOException {
            flush();
        }
    }

}
