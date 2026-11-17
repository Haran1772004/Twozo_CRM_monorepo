package com.tlc.analytics.config.model.view.report;

import com.tlc.analytics.config.internal.ConfigErrorCodes;
import com.tlc.analytics.config.model.view.filter.OperatorType;

import com.tlc.commons.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * Represents the report types
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@RequiredArgsConstructor
@Getter
public enum ReportType
{
    GRAPH( 1 ),
    TABULAR( 2 ),
    PIVOT( 3 ),
    KPI( 4 );

    private final int typeCode;

    /**
     * Retrieves the {@link ReportType} enum corresponding to a given type code.
     *
     * @param typeCode The type code to look up.
     * @return The {@link OperatorType} enum associated with the provided code.
     * @throws IllegalArgumentException if the provided code does not match any known operation type.
     */
    public static ReportType getReportType( final int typeCode )
    {
        return switch (typeCode) {
            case 1 -> GRAPH;
            case 2 -> TABULAR;
            case 3 -> PIVOT;
            case 4 -> KPI;
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_REPORT_TYPE, "i18n.analytics.invalid.report.type");
        };
    }
}
