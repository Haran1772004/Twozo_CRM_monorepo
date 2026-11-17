package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view;


import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * A enum that presents the {@link CRMViewType} details
 * </p>
 *
 * @author jamesh
 * @version 1.0
 */
@Getter
public enum CRMViewType
{
    KPI( 1, "kpiCard", "i18n.crm.analytics.view.type.kpi" ),
    LINE( 2, "lineChart", "i18n.crm.analytics.view.type.line" ),
    BAR( 3, "barChart", "i18n.crm.analytics.view.type.bar" ),
    PIE( 4, "pieChart", "i18n.crm.analytics.view.type.pie" ),
    AREA( 5, "areaChart", "i18n.crm.analytics.view.type.area.chart" ),
    DONUT( 6, "donutChart", "i18n.crm.analytics.view.type.donut.chart" ),
    HORIZONTAL_BAR( 7, "horizontalBarChart", "i18n.crm.analytics.view.type.horizontal.bar.chart" ),
    HEAT_MAP( 8, "heatMap", "i18n.crm.analytics.view.type.heat.map" ),
    GROUPED_BAR( 9, "groupedBarChart", "i18n.crm.analytics.view.type.grouped.bar.chart" ),
    GROUPED_HORIZONTAL_BAR( 10, "groupedHorizontalBarChart", "i18n.crm.analytics.view.type.grouped.horizontal.bar.chart" ),
    STACKED_BAR( 11, "stackedBarChart", "i18n.crm.analytics.view.type.stacked.bar.chart" ),
    STACKED_HORIZONTAL_BAR( 12, "stackedHorizontalBarChart", "i18n.crm.analytics.view.type.stacked.horizontal.bar.chart" ),
    SUMMARY( 13, "summaryTable", "i18n.crm.analytics.view.type.summary.table" ),
    BUBBLE( 14,"bubbleChart", "i18n.crm.analytics.view.type.bubble.chart");

    private final int typeCode;
    private final String aliasName;
    private final String i18nKeyOfDisplayName;

    private static final Map<Integer, CRMViewType> TYPE_CODE_MAP = Arrays.stream( values() )
            .collect( Collectors.toUnmodifiableMap( CRMViewType::getTypeCode, Function.identity() ) );
    private static final Map<String, CRMViewType> ALIAS_NAME_MAP = Arrays.stream( values() )
            .collect( Collectors.toUnmodifiableMap( CRMViewType::getAliasName, Function.identity() ) );

    CRMViewType(int typeCode, String aliasName, String i18nKeyOfDisplayName)
    {
        this.typeCode = typeCode;
        this.aliasName = aliasName;
        this.i18nKeyOfDisplayName = i18nKeyOfDisplayName;
    }

    /**
     * <p>
     * Gets the {@link CRMViewType} details based on the type code
     * </p>
     *
     * @param typeCode Represents the view type id
     * @return The {@link CRMViewType} details
     */
    public static CRMViewType getCrmViewType( final int typeCode )
    {
        final CRMViewType crmViewType = TYPE_CODE_MAP.get( typeCode );

        if( Objects.isNull( crmViewType ) )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_VIEW_TYPE,
                    "i18n.crm.analytics.invalid.view.type" );
        }

        return crmViewType;
    }

    public static CRMViewType fromId( final int typeCode )
    {
        return getCrmViewType( typeCode );
    }

    public static CRMViewType getByAlias( final String aliasName )
    {
        final CRMViewType crmViewType = ALIAS_NAME_MAP.get( aliasName );

        if( Objects.isNull( crmViewType ) )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_VIEW_TYPE,
                    "i18n.crm.analytics.invalid.view.type" );
        }

        return crmViewType;
    }

    public static boolean isValidTypeId( final int typeCode )
    {
        return TYPE_CODE_MAP.containsKey( typeCode );
    }

    /**
     * Checks whether the provided view type is a chart type.
     *
     * @param crmViewType view type to verify
     * @return {@code true} when the view type is chart-based
     */
    public static boolean isChartType(final CRMViewType crmViewType) {
        return !Objects.equals(CRMViewType.KPI, crmViewType);
    }

    /**
     *
     * <p>
     * Gets the Id based on the view type
     * </p>
     *
     * @param crmViewType the {@link CRMViewType} to resolve
     * @param orgId       the ID of the organization whose view types are being queried
     * @return the persisted identifier of the matching view type
     */
    public static Integer getTypeId( final CRMViewType crmViewType, long orgId )
    {
        if (Objects.isNull(crmViewType)) {
            throw ErrorCode.get( AnalyticsErrorCodes.VIEW_TYPE_NOT_FOUND,
                    "ii8n.crm.analytics.invalid.view.type.fetch" );
        }

        return crmViewType.getTypeCode();
    }
}
