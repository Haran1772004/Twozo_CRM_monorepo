package com.tlc.analytics.config.model.view;

import com.tlc.analytics.config.internal.ConfigErrorCodes;

import com.tlc.commons.code.ErrorCode;

import lombok.Getter;

/**
 * <p>
 * Represents the view types
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@Getter
public enum ViewType {
    CHART(1, "i18n.analytics.view.type.chart"),
    TABULAR(2, "i18n.analytics.view.type.tabular"),
    WIDGET(3, "i18n.analytics.view.type.widget"),
    IMAGE(4, "i18n.analytics.view.type.image"),
    TITLE(5, "i18n.analytics.view.type.title"),
    DASHBOARD(6, "i18n.analytics.view.type.dashboard"),
    TABLE(7, "i18n.analytics.view.type.table"),
    PIVOT(8, "i18n.analytics.view.type.pivot"),
    TABBED_DASHBOARD(9, "i18n.analytics.view.type.tabbed.dashboard"),
    QUERY_TABLE(10, "i18n.analytics.view.type.query.table"),
    ;

    private final int typeCode;
    private final String i18nKeyOfDisplayName;

    ViewType(int typeCode, String i18nKeyOfDisplayName) {
        this.typeCode = typeCode;
        this.i18nKeyOfDisplayName = i18nKeyOfDisplayName;
    }

    /**
     * Retrieves the {@link ViewType} enum corresponding to a given type code.
     *
     * @param typeCode The type code to look up.
     * @return The {@link ViewType} enum associated with the provided code.
     * @throws IllegalArgumentException if the provided code does not match any known operation type.
     */
    public static ViewType getType(final int typeCode) {
        return switch (typeCode) {
            case 1 -> CHART;
            case 2 -> TABULAR;
            case 3 -> WIDGET;
            case 4 -> IMAGE;
            case 5 -> TITLE;
            case 6 -> DASHBOARD;
            case 7 -> TABLE;
            case 8 -> PIVOT;
            case 9 -> TABBED_DASHBOARD;
            case 10 -> QUERY_TABLE;
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_VIEW_TYPE, "i18n.analytics.invalid.view.type");
        };
    }

    /**
     * Retrieves the type code of the {@link ViewType} corresponding to the given name.
     *
     * @param name The name of the view type (case-insensitive).
     * @return The type code associated with the provided name.
     */
    public static int getViewTypeId(final String name) {
        return switch (name.toLowerCase()) {
            case "chart" -> CHART.getTypeCode();
            case "tabular" -> TABULAR.getTypeCode();
            case "widget" -> WIDGET.getTypeCode();
            case "image" -> IMAGE.getTypeCode();
            case "title" -> TITLE.getTypeCode();
            case "dashboard" -> DASHBOARD.getTypeCode();
            case "table" -> TABLE.getTypeCode();
            case "pivot" -> PIVOT.getTypeCode();
            case "tabbed_dashboard" -> TABBED_DASHBOARD.getTypeCode();
            case "query_table" -> QUERY_TABLE.getTypeCode();
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_VIEW_TYPE, "i18n.analytics.invalid.view.type");
        };
    }
}
