package com.tlc.analytics.config.model.view;

import com.tlc.analytics.config.internal.ConfigErrorCodes;

import com.tlc.commons.code.ErrorCode;

import lombok.Getter;

import java.util.Collection;
import java.util.List;

/**
 * {@link OperationType} is an enum representing various types of operations that can be performed on view column.
 * Each enum value corresponds to a specific operation type and includes a type code and an
 * internationalization (i18n) key for the display name.
 *
 * @author Aswath
 * @version 1.0
 */
@Getter
public enum OperationType
{
    COUNT( 1, "i18n.analytics.measure.config.count" ),
    SUM( 2, "i18n.analytics.measure.config.sum" ),
    MIN( 3, "i18n.analytics.measure.config.min" ),
    MAX( 4, "i18n.analytics.measure.config.max" ),
    AVG( 5, "i18n.analytics.measure.config.average" ),
    ACTUAL( 6, "i18n.analytics.measure.config.actual" ),
    DAILY( 7, "i18n.analytics.measure.config.daily" ),
    WEEKLY( 8, "i18n.analytics.measure.config.weekly" ),
    MONTHLY( 9, "i18n.analytics.measure.config.monthly" ),
    YEARLY( 10, "i18n.analytics.measure.config.yearly" ),
    PREVIOUS( 11, "i18n.analytics.measure.config.previous" ),
    LATEST( 12, "i18n.analytics.measure.config.latest" ),
    DIFF_PCT_LATEST( 13, "i18n.analytics.measure.config.diff.pct.latest" ),
    DIFF_PCT_PREVIOUS( 14, "i18n.analytics.measure.config.diff.pct.previous" ),
    DIFF_LATEST( 15, "i18n.analytics.measure.config.diff.latest" ),
    DIFF_PREVIOUS( 16, "i18n.analytics.measure.config.diff.previous" ),
    TOTAL( 17, "i18n.analytics.measure.config.total" ),
    PERCENTAGE( 18, "i18n.analytics.measure.config.percentage" ),
    TODAY( 19, "i18n.analytics.kpi.period.today" ),
    THIS_WEEK( 20, "i18n.analytics.kpi.period.thisWeek" ),
    THIS_MONTH( 21, "i18n.analytics.kpi.period.thisMonth" ),
    THIS_QUARTER( 22, "i18n.analytics.kpi.period.thisQuarter" ),
    THIS_YEAR( 23, "i18n.analytics.kpi.period.thisYear" ),
    CUSTOM( 24, "i18n.analytics.kpi.period.custom" ),
    YESTERDAY(25, "i18n.analytics.kpi.period.yesterday" ),
    LAST_WEEK(26, "i18n.analytics.kpi.period.last.week" ),
    LAST_MONTH(27, "i18n.analytics.kpi.period.last.month" ),
    LAST_QUARTER(28, "i18n.analytics.kpi.period.last.quarter" ),
    LAST_YEAR(29, "i18n.analytics.kpi.period.last.year" ),
    ;

    private final int typeCode;
    private final String i18nKeyOfDisplayName;

    /**
     * Constructor for {@link OperationType} enum values.
     *
     * @param typeCode               The numerical code representing the operation type.
     * @param i18nKeyOfDisplayName   The internationalization key for the display name of the operation type.
     */
    OperationType( final int typeCode, final String i18nKeyOfDisplayName )
    {
        this.typeCode = typeCode;
        this.i18nKeyOfDisplayName = i18nKeyOfDisplayName;
    }

    /**
     * Retrieves the {@link OperationType} enum corresponding to a given type code.
     *
     * @param typeCode The type code to look up.
     * @return The {@link OperationType} enum associated with the provided code.
     */
    public static OperationType getOperationType(final int typeCode) {
        return switch (typeCode) {
            case 1 -> COUNT;
            case 2 -> SUM;
            case 3 -> MIN;
            case 4 -> MAX;
            case 5 -> AVG;
            case 6 -> ACTUAL;
            case 7 -> DAILY;
            case 8 -> WEEKLY;
            case 9 -> MONTHLY;
            case 10 -> YEARLY;
            case 11 -> PREVIOUS;
            case 12 -> LATEST;
            case 13 -> DIFF_PCT_LATEST;
            case 14 -> DIFF_PCT_PREVIOUS;
            case 15 -> DIFF_LATEST;
            case 16 -> DIFF_PREVIOUS;
            case 17 -> TOTAL;
            case 18 -> PERCENTAGE;
            case 19 -> TODAY;
            case 20 -> THIS_WEEK;
            case 21 -> THIS_MONTH;
            case 22 -> THIS_YEAR;
            case 23 -> THIS_QUARTER;
            case 24 -> CUSTOM;
            case 25 -> YESTERDAY;
            case 26 -> LAST_WEEK;
            case 27 -> LAST_MONTH;
            case 28 -> LAST_QUARTER;
            case 29 -> LAST_YEAR;
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_OPERATION_TYPE,
                    "i18n.analytics.invalid.operation.type");
        };
    }

    /**
     * Retrieves the list of {@link OperationType} values that represent KPI time-based units.
     *
     * @return A collection of {@link OperationType} representing KPI time units.
     */
    public static Collection<OperationType> getKPITimeUnits() {
        return List.of(OperationType.TODAY, OperationType.THIS_WEEK, OperationType.THIS_MONTH, OperationType.THIS_QUARTER,
                OperationType.THIS_YEAR, OperationType.CUSTOM);
    }

    public static int getOperationTypeCode( final String operationName )
    {
        return switch ( operationName) {
            case "COUNT" -> 1;
            case "SUM" -> 2;
            case "MIN" -> 3;
            case "MAX" -> 4;
            case "AVG" -> 5;
            case "ACTUAL" -> 6;
            case "DAILY" -> 7;
            case "WEEKLY" -> 8;
            case "MONTHLY" -> 9;
            case "YEARLY" -> 10;
            case "PREVIOUS" -> 11;
            case "LATEST" -> 12;
            case "DIFF_PCT_LATEST" -> 13;
            case "DIFF_PCT_PREVIOUS" -> 14;
            case "DIFF_LATEST" -> 15;
            case "DIFF_PREVIOUS" -> 16;
            case "TOTAL" -> 17;
            case "PERCENTAGE" -> 18;
            case "TODAY" -> 19;
            case "THIS_WEEK" -> 20;
            case "THIS_MONTH" -> 21;
            case "THIS_YEAR" -> 22;
            case "THIS_QUARTER" -> 23;
            case "CUSTOM" -> 24;
            case "YESTERDAY" -> 25;
            case "LAST_WEEK" -> 26;
            case "LAST_MONTH" -> 27;
            case "LAST_QUARTER" -> 28;
            case "LAST_YEAR" -> 29;
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_OPERATION_TYPE,
                    "i18n.analytics.invalid.operation.type");
        };
    }
}
