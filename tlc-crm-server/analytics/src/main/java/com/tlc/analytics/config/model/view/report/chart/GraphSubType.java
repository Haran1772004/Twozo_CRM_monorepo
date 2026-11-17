package com.tlc.analytics.config.model.view.report.chart;

import com.tlc.analytics.config.internal.ConfigErrorCodes;
import com.tlc.commons.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * Represents the graph sub types
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@RequiredArgsConstructor
@Getter
public enum GraphSubType
{
    DEFAULT_PIE( 1, "i18n.analytics.graph.subtype.default.pie" ),
    RING( 2, "i18n.analytics.graph.subtype.ring" ),
    SEMI_PIE( 3, "i18n.analytics.graph.subtype.semi.pie" ),
    SEMI_RING( 4, "i18n.analytics.graph.subtype.semi.ring" ),
    DEFAULT_BAR( 5, "i18n.analytics.graph.subtype.default.bar" ),
    HORIZONTAL_BAR( 6, "i18n.analytics.graph.subtype.horizontal.bar" ),
    BUTTERFLY_BAR( 7, "i18n.analytics.graph.subtype.butterfly.bar" ),
    HISTOGRAM( 8, "i18n.analytics.graph.subtype.histogram" ),
    DEFAULT_LINE( 9, "i18n.analytics.graph.subtype.default.line" ),
    LINE_WITH_MARKER( 10, "i18n.analytics.graph.subtype.line.with.marker" ),
    SMOOTH_LINE( 11, "i18n.analytics.graph.subtype.smooth.line" ),
    SMOOTH_LINE_WITH_MARKER( 12, "i18n.analytics.graph.subtype.smooth.line.with.marker" ),
    STEP( 13, "i18n.analytics.graph.subtype.step" );

    private final int typeCode;
    private final String i18nKeyOfDisplayName;

    public static GraphSubType getGraphSubType( final int subTypeCode )
    {
        switch( subTypeCode )
        {
            case 1 -> {
                return DEFAULT_PIE;
            }
            case 2 -> {
                return RING;
            }
            case 3 -> {
                return SEMI_PIE;
            }
            case 4 -> {
                return SEMI_RING;
            }
            case 5 -> {
                return DEFAULT_BAR;
            }
            case 6 -> {
                return HORIZONTAL_BAR;
            }
            case 7 -> {
                return BUTTERFLY_BAR;
            }
            case 8 -> {
                return HISTOGRAM;
            }
            case 9 -> {
                return DEFAULT_LINE;
            }
            case 10 -> {
                return LINE_WITH_MARKER;
            }
            case 11 -> {
                return SMOOTH_LINE;
            }
            case 12 -> {
                return SMOOTH_LINE_WITH_MARKER;
            }
            case 13 -> {
                return STEP;
            }
            default -> throw ErrorCode.get( ConfigErrorCodes.INVALID_GRAPH_SUB_TYPE,
                                            "i18n.analytics.invalid.graph.subtype" );
        }
    }
}
