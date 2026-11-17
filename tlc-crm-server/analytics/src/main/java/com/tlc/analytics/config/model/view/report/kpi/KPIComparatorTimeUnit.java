package com.tlc.analytics.config.model.view.report.kpi;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * Represents the KPI comparator time unit for the KPI widget
 * </p>
 *
 * @author karthick-v
 * @version 1.0
 */
@Getter
@RequiredArgsConstructor
public enum KPIComparatorTimeUnit {
    YESTERDAY(1, "i18n.analytics.widget.comp.time.unit.yesterday"),
    PREVIOUS_DAY_LAST_YEAR(2, "i18n.analytics.widget.comp.time.unit.previous.day.last.year"),
    LAST_WEEK(3, "i18n.analytics.widget.comp.time.unit.last.week"),
    PREVIOUS_WEEK_LAST_YEAR(4, "i18n.analytics.widget.comp.time.unit.previous.week.last.year"),
    LAST_MONTH(5, "i18n.analytics.widget.comp.time.unit.last.month"),
    PREVIOUS_MONTH_LAST_YEAR(7, "i18n.analytics.widget.comp.time.unit.previous.month.last.year"),
    LAST_QUARTER(8, "i18n.analytics.widget.comp.time.unit.last.quarter"),
    PREVIOUS_QUARTER_LAST_YEAR(9, "i18n.analytics.widget.comp.time.unit.previous.quarter.last.year"),
    LAST_YEAR(10, "i18n.analytics.widget.comp.time.unit.last.year"),
    SAME_PERIOD_LAST_YEAR(11, "i18n.analytics.widget.comp.time.unit.same.period.last.year"),
    CUSTOM(12, "i18n.analytics.widget.comp.time.unit.custom");

    private final int typeCode;
    private final String i18nKeyOfDisplayName;

}
