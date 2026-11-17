package com.tlc.crm.core.api.analytics.integ.general.analytics;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.api.analytics.entity.config.fields.AnalyticsField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMSystemModularField;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMViewType;
import com.tlc.crm.core.api.analytics.service.field.AnalyticsFieldService;
import com.tlc.crm.core.api.analytics.service.util.ModularFieldUtil;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service responsible for determining valid CRM chart view types based on analytics configuration.
 *
 * @author Saravanan
 * @version 1.0
 */
public class CRMChartViewTypeService {

    private final FieldTypeService fieldTypeService;
    private final AnalyticsFieldService analyticsFieldService;
    private final FieldService fieldService;

    private CRMChartViewTypeService() {
        this.fieldTypeService = FieldTypeService.getInstance();
        this.analyticsFieldService = AnalyticsFieldService.getInstance();
        this.fieldService = FieldService.getInstance();
    }

    private static class Instance
    {
        private static final CRMChartViewTypeService INSTANCE = new CRMChartViewTypeService();
    }

    public static CRMChartViewTypeService INSTANCE()
    {
        return CRMChartViewTypeService.Instance.INSTANCE;
    }

    /**
     * Determines whether the given field (from viewByJson) is of a date type.
     *
     * @param orgId       the organization ID
     * @param fieldId     the field ID used to determine if the field is a date type
     * @return {@code true} if the field is of date type, {@code false} otherwise
     */
    public boolean isDateType(final Long orgId, final Long fieldId, final Map<Long, ReportModularField> reportFieldMap) {
        if (Objects.isNull(fieldId)) {
            return true;
        }

        final ReportModularField reportModularField = reportFieldMap.get(fieldId);

        if (Objects.nonNull(reportModularField)) {
            final ModularField modularField = reportModularField.getModularField();

            if (Objects.nonNull(modularField)) {
                StoreType storeType = null;

                if (modularField instanceof CRMSystemModularField systemModularField) {
                    final Long anaFieldId = systemModularField.getAnaFieldId();

                    if (Objects.nonNull(anaFieldId)) {
                        final AnalyticsField anaField = analyticsFieldService.getAnalyticsFieldById(anaFieldId);
                        storeType = fieldTypeService.get(anaField.getFieldTypeId()).storeType();
                    }
                }

                if (Objects.isNull(storeType) && Objects.nonNull(modularField.getCrmFieldId())) {
                    final FieldInfo fieldInfo = fieldService.getDefaultFieldById(modularField.getCrmFieldId());
                    storeType = fieldInfo.storeType();
                }

                return Objects.nonNull(storeType) && storeType.isDateType();
            }
        }

        return false;
    }

    /**
     * Determines whether any of the given field IDs corresponds to a date-type field.
     *
     * @param orgId    the organization ID used to resolve field definitions
     * @param fieldIds the collection of field IDs to evaluate; may be {@code null} or empty
     * @return {@code true} if at least one of the given field IDs is a date type,
     *         or if {@code fieldIds} is {@code null}, empty, contains a {@code null} entry,
     *         or if {@code size} is 0; otherwise {@code false}
     */
    public boolean isAnyDateType(final Long orgId, final List<Long> fieldIds, final Map<Long, ReportModularField> reportFieldMap) {
        if (Objects.isNull(fieldIds) || fieldIds.isEmpty()) {
            return true;
        }

        for (final Long fieldId : fieldIds) {
            if (Objects.isNull(fieldId)) {
                return true;
            }
        }

        try {
            final Map<Long, ReportModularField> reportModularFieldMap = ModularFieldUtil.filterReportFields(reportFieldMap, fieldIds);
            final Set<Long> anaFieldIds = reportModularFieldMap.values().stream()
                    .filter(reportModularField -> !reportModularField.isCustom() && Objects.nonNull(reportModularField.getSystemModularField()))
                    .map(reportModularField -> reportModularField.getSystemModularField().getAnaFieldId())
                    .filter(Objects::nonNull).collect(Collectors.toSet());
            final Map<Long, AnalyticsField> anaFieldMap = !anaFieldIds.isEmpty() ? analyticsFieldService.getAnalyticsFieldsByIds(anaFieldIds) : null;
            final Set<Long> allCrmFieldIds = reportModularFieldMap.values().stream()
                    .map(ReportModularField::getModularField).filter(Objects::nonNull)
                    .map(ModularField::getCrmFieldId).filter(Objects::nonNull).collect(Collectors.toSet());
            final Map<Long, FieldInfo> fieldInfoMap = allCrmFieldIds.isEmpty() ? Map.of() : fieldService.getFieldsById(orgId, allCrmFieldIds);

            for (final Long fieldId : fieldIds) {

                if (Objects.isNull(fieldId)) {
                    return true;
                }
                final ReportModularField reportModularField = reportModularFieldMap.get(fieldId);

                if (Objects.isNull(reportModularField)) {
                    continue;
                }

                final ModularField modularField = reportModularField.getModularField();

                if (Objects.isNull(modularField)) {
                    continue;
                }

                StoreType storeType = null;

                if (modularField instanceof CRMSystemModularField systemModularField) {

                    if (Objects.nonNull(anaFieldMap)) {
                        final AnalyticsField anaField = anaFieldMap.get(systemModularField.getAnaFieldId());

                        if (Objects.nonNull(anaField)) {
                            storeType = fieldTypeService.get(anaField.getFieldTypeId()).storeType();
                        }
                    }
                }

                if (Objects.isNull(storeType) && Objects.nonNull(modularField.getCrmFieldId())) {
                    final FieldInfo fieldInfo = fieldInfoMap.get(modularField.getCrmFieldId());

                    if (Objects.nonNull(fieldInfo)) {
                        storeType = fieldInfo.storeType();
                    }
                }

                if (Objects.nonNull(storeType) && storeType.isDateType()) {
                    return true;
                }
            }
        } catch ( final Exception exception ) {
            throw ErrorCode.get(AnalyticsErrorCodes.INVALID_FIELD,
                                "i18n.analytics.config.invalid.field");
        }

        return false;
    }


    /**
     * Returns a set of supported {@link CRMViewType} options based on chart configuration context.
     *
     * @param isSameMeasure  whether all metrics share the same measure type
     * @param metricSize     the number of selected metrics
     * @param orgId          the organization ID
     * @param fieldIds       the field IDs used to determine if the field is a date type
     * @param viewBySize     the number of view-by fields
     * @param reportFieldMap pre-fetched map of field ID to ReportModularField
     * @return a filtered {@link EnumSet} of supported {@link CRMViewType} options
     */
    public EnumSet<CRMViewType> getSupportedViewTypes(final boolean isSameMeasure, final int metricSize, final Long orgId,
                                                      final List<Long> fieldIds, final int viewBySize,
                                                      final Map<Long, ReportModularField> reportFieldMap) {
        final boolean isDateType = isAnyDateType(orgId, fieldIds, reportFieldMap);
        EnumSet<CRMViewType> supported;

        if (Objects.equals(metricSize, NumericalConstant.ONE)) {
            if (Objects.equals(viewBySize, NumericalConstant.ONE)) {
                supported = isDateType ? EnumSet.of(CRMViewType.AREA, CRMViewType.BAR, CRMViewType.DONUT,
                                                    CRMViewType.HEAT_MAP, CRMViewType.HORIZONTAL_BAR, CRMViewType.LINE,
                                                    CRMViewType.PIE)
                                       : EnumSet.of(CRMViewType.BAR, CRMViewType.DONUT, CRMViewType.HEAT_MAP,
                                                    CRMViewType.HORIZONTAL_BAR, CRMViewType.PIE);
            } else {
                supported = EnumSet.of(CRMViewType.BUBBLE, CRMViewType.GROUPED_BAR, CRMViewType.GROUPED_HORIZONTAL_BAR,
                                       CRMViewType.HEAT_MAP,
                                       CRMViewType.STACKED_BAR, CRMViewType.STACKED_HORIZONTAL_BAR);
            }
        } else if (Objects.equals(metricSize, NumericalConstant.TWO)) {
            if (Objects.equals(viewBySize, NumericalConstant.ONE)) {
                if (isSameMeasure) {
                    supported = isDateType ? EnumSet.of(CRMViewType.AREA, CRMViewType.BUBBLE, CRMViewType.GROUPED_BAR,
                                                        CRMViewType.GROUPED_HORIZONTAL_BAR, CRMViewType.HEAT_MAP, CRMViewType.LINE,
                                                        CRMViewType.STACKED_BAR, CRMViewType.STACKED_HORIZONTAL_BAR)
                                           : EnumSet.of(CRMViewType.BUBBLE, CRMViewType.GROUPED_BAR,
                                                        CRMViewType.GROUPED_HORIZONTAL_BAR, CRMViewType.HEAT_MAP,
                                                        CRMViewType.STACKED_BAR, CRMViewType.STACKED_HORIZONTAL_BAR);
                } else {
                    supported = EnumSet.of(CRMViewType.BUBBLE);
                }
            } else {
                supported = EnumSet.of(CRMViewType.BUBBLE);
            }

        } else {
            if (Objects.equals(viewBySize, NumericalConstant.ONE)) {
                if (isSameMeasure) {
                    supported = isDateType ? EnumSet.of(CRMViewType.AREA, CRMViewType.LINE, CRMViewType.GROUPED_BAR, CRMViewType.GROUPED_HORIZONTAL_BAR, CRMViewType.HEAT_MAP,
                                                        CRMViewType.STACKED_BAR, CRMViewType.STACKED_HORIZONTAL_BAR)
                                           : EnumSet.of(CRMViewType.GROUPED_BAR, CRMViewType.HEAT_MAP,
                                                        CRMViewType.GROUPED_HORIZONTAL_BAR, CRMViewType.STACKED_BAR, CRMViewType.STACKED_HORIZONTAL_BAR);
                } else {
                    supported = EnumSet.noneOf(CRMViewType.class);
                }
            } else {
                supported = EnumSet.noneOf(CRMViewType.class);
            }
        }
        return supported;
    }
}
