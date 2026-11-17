package com.tlc.crm.core.validator.analytics.layout.view.axis;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.api.analytics.entity.config.axis.AxisTypes;
import com.tlc.crm.core.api.analytics.entity.config.fields.AnalyticsField;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMSubModuleMapper;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMSystemModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModuleType;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.entity.config.timePeriod.CRMTimePeriodField;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.manager.CRMReportManagerAPI;

import com.tlc.crm.core.api.analytics.service.field.AnalyticsFieldService;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.sql.resource.CRMSUBMODULEMAPPER;

import com.tlc.crm.core.validator.analytics.RequestValidator;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;

import com.tlc.sql.common.api.dml.Criteria;
import lombok.NonNull;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Provides an abstract way to validate the view by configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see RequestValidator
 */
public class ViewByValidator{

    private final CRMReportManagerAPI crmManager;
    private final AnalyticsFieldService analyticsFieldService;
    private final CRMViewConfigManager<CRMSubModuleMapper> subModuleMapperManager;
    private final FieldTypeService fieldTypeService;

    /**
     * Creates view-by validator dependencies.
     */
    private ViewByValidator() {
        this.crmManager = CRMReportManagerAPI.INSTANCE();
        this.analyticsFieldService = AnalyticsFieldService.getInstance();
        this.subModuleMapperManager = crmManager.getSubModuleMapperManager();
        this.fieldTypeService = FieldTypeService.getInstance();
    }

    private static class Instance {
        private static final ViewByValidator INSTANCE = new ViewByValidator();
    }

    /**
     * Returns singleton instance.
     *
     * @return {@link ViewByValidator} singleton
     */
    public static ViewByValidator INSTANCE() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param jsonObject Represents the {@link JsonArray} containing the configuration
     */
    public void validateRequest( final @NonNull Long orgId,
                                 final @NonNull JsonObject jsonObject,
                                 final Map<Long, ReportModularField> reportModularFieldMap) throws Exception {
        final ReportModularField reportModularField = jsonObject.containsKey(CRMReportKeys.FIELD_ID)
                ? reportModularFieldMap.get(jsonObject.getLong(CRMReportKeys.FIELD_ID)) : null;
        final ModularField activeField = (Objects.nonNull(reportModularField))
                ? reportModularField.getModularField()
                : null;

        if (Objects.nonNull(reportModularField) && jsonObject.containsKey(CRMReportKeys.MODULE_ID)) {
            final Long reportFieldId = reportModularField.id();
            final int moduleId = jsonObject.getInt(CRMReportKeys.MODULE_ID);

            try {
                ModuleType.getModuleType(moduleId);
            } catch (final Exception exception) {
                throw ErrorCode.get(AnalyticsErrorCodes.MODULE_ID_NOT_FOUND);
            }

            if (jsonObject.containsKey(CRMReportKeys.SUB_MODULE_ID)) {
                final int subModuleId = jsonObject.getInt(CRMReportKeys.SUB_MODULE_ID);
                final Criteria subModuleMapperCriteria = Criteria.eq(subModuleMapperManager.getTable()
                                                                                           .getColumn(CRMSUBMODULEMAPPER.MODULE_ID), moduleId);
                final Collection<CRMSubModuleMapper> subModuleMappers = subModuleMapperManager.getAll(orgId, subModuleMapperCriteria);

                if (Objects.isNull(subModuleMappers) || subModuleMappers.isEmpty()) {
                    throw ErrorCode.get(AnalyticsErrorCodes.SUB_MODULES_NOT_FOUND_FOR_MODULE);
                }

                final Collection<Integer> subModuleIds = subModuleMappers.stream()
                                                                      .map(CRMSubModuleMapper::getSubModuleId)
                                                                      .toList();
                if (!subModuleIds.contains(subModuleId)) {
                    throw ErrorCode.get(AnalyticsErrorCodes.SUB_MODULE_NOT_LINKED_TO_MODULE);
                }

                validateAxisFieldForModule(orgId, subModuleId, reportFieldId, reportModularFieldMap);
            } else {
                validateAxisFieldForModule(orgId, moduleId, reportFieldId, reportModularFieldMap);
            }

            final boolean isBucketEnabled = activeField.getModularFieldConfig().isBucketByEnabled();

            if (!isBucketEnabled && jsonObject.containsKey(CRMViewPropertyKeys.BUCKET)) {
                throw ErrorCode.get(AnalyticsErrorCodes.BUCKET_NOT_SUPPORTED_FOR_FIELD);
            }
        }

        if (jsonObject.containsKey(CRMReportKeys.TIME_PERIOD_FIELD_ID)) {
            final Long timePeriodFieldId = jsonObject.getLong(CRMReportKeys.TIME_PERIOD_FIELD_ID);
            final CRMViewConfigManager<CRMTimePeriodField> timePeriodFieldManager = crmManager.getTimePeriodFieldManager();

            try {
                final CRMTimePeriodField timePeriodField = timePeriodFieldManager.get(orgId, timePeriodFieldId);

                if (Objects.isNull(timePeriodField)) {
                    throw ErrorCode.get(AnalyticsErrorCodes.TIME_PERIOD_FIELD_ID_NOT_FOUND);
                }
            } catch (final Exception exception) {
                throw ErrorCode.get(AnalyticsErrorCodes.TIME_PERIOD_FIELD_ID_NOT_FOUND);
            }

            if (Objects.nonNull(reportModularField)) {
                final Long crmFieldId = activeField.getCrmFieldId();
                Long anaFieldId = null;

                if (activeField instanceof CRMSystemModularField CRMSystemModularField) {
                    anaFieldId = CRMSystemModularField.getAnaFieldId();
                }

                StoreType storeType = null;

                if (Objects.nonNull(anaFieldId)) {
                    final AnalyticsField anaField = analyticsFieldService.getAnalyticsFieldById(anaFieldId);
                    storeType = fieldTypeService.get(anaField.getFieldTypeId()).storeType();
                }
                if (Objects.nonNull(crmFieldId)) {
                    final FieldInfo fieldInfo = FieldService.getInstance().getField(orgId, crmFieldId);
                    storeType = fieldInfo.storeType();
                }

                assert storeType != null;
                if (!storeType.isDateType()) {
                    throw ErrorCode.get(AnalyticsErrorCodes.FIELD_DOES_NOT_SUPPORT_TIME_PERIOD);
                }
            }
        }

        final String label = jsonObject.getString(CRMViewPropertyKeys.LABEL);

        if (Objects.isNull(label) || label.isBlank()) {
            throw ErrorCode.get(AnalyticsErrorCodes.LABEL_SHOULD_NOT_BE_EMPTY,
                                "i18n.analytics.config.view.invalid.label");
        }
        if (NumericalConstant.TWO_HUNDRED_FIFTY_FIVE < label.length()) {
            throw ErrorCode.get(AnalyticsErrorCodes.LABEL_NAME_LIMIT_EXCEEDED,
                                "i18n.analytics.config.view.label.limit.exceeded");
        }
    }

    /**
     * Validates whether the given modular field ID is valid for the specified module and is allowed
     * to be used on the X-axis of a chart or report.
     *
     * @param orgId          the organization ID to which the data belongs
     * @param moduleId       the module ID to validate the modular field against
     * @param modularFieldId the modular field ID to be validated
     */
    private void validateAxisFieldForModule(final @NonNull Long orgId,
                                            final @NonNull Integer moduleId,
                                            final @NonNull Long modularFieldId,
                                            final Map<Long, ReportModularField> reportFieldMap) {
        final ReportModularField reportField = reportFieldMap.get(modularFieldId);

        if (Objects.isNull(reportField)) {
            throw ErrorCode.get(AnalyticsErrorCodes.AXIS_FIELD_NOT_VALID_FOR_MODULE,
                    "i18n.analytics.config.axis.field.not.valid.for.module");
        }

        final ModularField effectiveField = reportField.getModularField();

        if (effectiveField == null) {
            throw ErrorCode.get(AnalyticsErrorCodes.AXIS_FIELD_NOT_VALID_FOR_MODULE,
                    "i18n.analytics.config.axis.field.not.valid.for.module");
        }

        if (!Objects.equals(effectiveField.getModuleId(), moduleId)) {
            throw ErrorCode.get(AnalyticsErrorCodes.AXIS_FIELD_NOT_VALID_FOR_MODULE,
                    "i18n.analytics.config.axis.field.not.valid.for.module");
        }

        final AxisTypes axisType = AxisTypes.getAxisTypes(effectiveField.getModularFieldConfig().getAxisType());
        if (axisType != AxisTypes.X_ONLY && axisType != AxisTypes.X_Y) {
            throw ErrorCode.get(AnalyticsErrorCodes.AXIS_FIELD_NOT_VALID_FOR_MODULE,
                    "i18n.analytics.config.axis.field.not.valid.for.module");
        }
    }
}
