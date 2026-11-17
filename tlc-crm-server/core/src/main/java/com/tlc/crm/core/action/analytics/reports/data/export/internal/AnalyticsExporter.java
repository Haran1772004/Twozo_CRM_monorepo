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
 * Defines a contract for exporting analytics data.
 *
 * <p>Implementations of this interface handle the process of exporting analytics
 * data in a specific format, such as CSV other structured data types.</p>
 *
 * @author Saravanan
 * @version 1.0
 */
public interface AnalyticsExporter
{
    /**
     * Exports analytics data based on the given request and parameters.
     *
     * @param orgId                  the identifier of the organization for which the data is being exported
     * @param reqData                a {@link JsonObject} containing the request data needed to generate the response
     * @param req                    the analytics request containing export details
     * @param actionType             the {@link ActionType} that specifies the type of action to be performed
     * @param reportModularFieldMap  a map of field ID to {@link ReportModularField} objects containing modular field metadata
     * @param i18nResolver           the {@link I18nResolver} used to localize field values
     * @return a {@link File} object pointing to the generated CSV export file
     * @throws ExportException if an error occurs during the export process
     */
    @NonNull File doExport( final @NonNull Long orgId,
                            final @NonNull JsonObject reqData,
                            final @NonNull AnalyticsRequest req,
                            final @NonNull ActionType actionType,
                            final @NonNull Map<Long, ReportModularField> reportModularFieldMap,
                            final @NonNull I18nResolver i18nResolver) throws ExportException;
}