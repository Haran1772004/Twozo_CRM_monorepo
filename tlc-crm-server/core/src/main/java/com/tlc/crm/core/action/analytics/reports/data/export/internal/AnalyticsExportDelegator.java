package com.tlc.crm.core.action.analytics.reports.data.export.internal;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.i18n.I18nResolver;
import com.tlc.pk.export.ExportException;
import lombok.NonNull;

import java.io.File;
import java.util.Map;

/**
 * Delegates the export process for analytics data to the appropriate exporter.
 *
 * <p>This class follows the Singleton pattern and acts as a delegator, determining
 * the export type from the request data and forwarding the export operation to
 * the corresponding {@link AnalyticsExporter} instance retrieved from
 * {@link AnalyticsExportFactory}.
 *
 * @author Saravanan
 * @version 1.0
 */
public class AnalyticsExportDelegator
{
    private static final String EXPORT_TYPE = "exportType";

    private AnalyticsExportDelegator()
    {
    }

    private static class Instance
    {
        private static final AnalyticsExportDelegator INSTANCE = new AnalyticsExportDelegator();
    }

    /**
     * Returns the singleton instance of {@code AnalyticsExportManager}.
     *
     * @return the singleton instance of {@code AnalyticsExportManager}.
     */
    public static AnalyticsExportDelegator getInstance()
    {
        return Instance.INSTANCE;
    }

    /**
     * Performs the export operation based on the provided request data.
     *
     * @param orgId                  the identifier of the organization for which the data is being exported
     * @param req                    the analytics request containing export details
     * @param actionType             the type of action triggering the export
     * @param requestData            a JSON object containing additional export parameters, including the export type
     * @param reportModularFieldMap  a map of field ID to {@link ReportModularField} objects containing modular field metadata
     * @param i18nResolver           an instance of {@link I18nResolver} for localization support
     * @return a {@link File} representing the exported data
     * @throws ExportException if an error occurs during the export process
     */
    public @NonNull File performExport( final @NonNull Long orgId,
                                        final @NonNull AnalyticsRequest req,
                                        final @NonNull ActionType actionType,
                                        final @NonNull JsonObject requestData,
                                        final @NonNull Map<Long, ReportModularField> reportModularFieldMap,
                                        final @NonNull I18nResolver i18nResolver) throws ExportException
    {
        final int exportType = requestData.getInt( EXPORT_TYPE );

        return AnalyticsExportFactory.getInstance( exportType )
                .doExport( orgId, requestData, req, actionType, reportModularFieldMap, i18nResolver );
    }
}