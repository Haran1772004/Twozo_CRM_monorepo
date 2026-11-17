package com.tlc.crm.core.action.analytics.reports.data.export.internal;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import com.tlc.pk.export.ExportType;

import lombok.NonNull;

/**
 * Factory class for creating instances of analytics exporters.
 *
 * <p>This factory provides methods to obtain the appropriate {@code AnalyticsExporter}
 * implementation based on the specified export type.</p>
 *
 * @author Saravanan
 * @version 1.0
 */
public class AnalyticsExportFactory
{

    /**
     * Returns an instance of {@code AnalyticsExporter} based on the specified export type.
     *
     * <p>This method determines the appropriate exporter implementation for the given
     * {@code exportType} and returns an instance of {@code AnalyticsExporter}. If the
     * export type is not recognized, an {@code IllegalArgumentException} is thrown.</p>
     *
     * @param exportType the integer value representing the export type (must correspond to a valid export type)
     * @return an instance of {@code AnalyticsExporter} suitable for the given export type
     * @throws IllegalArgumentException if the provided export type is invalid or unsupported
     */
    public static @NonNull AnalyticsExporter getInstance( final int exportType ) throws IllegalArgumentException
    {
        if( ExportType.CSV.getExportType() == exportType )
        {
            return AnalyticsCSVExporter.getInstance();
        }
        else
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_EXPORT_TYPE,
                                 "i18n.analytics.config.invalid.export.type" );
        }
    }

}
