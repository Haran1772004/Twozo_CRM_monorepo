package com.tlc.crm.core.validator.analytics.layout.filter;

import com.tlc.analytics.config.model.view.filter.OperatorType;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.analytics.entity.config.fields.AnalyticsField;
import com.tlc.crm.core.api.analytics.entity.config.function.CRMFunction;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMSystemModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.manager.CRMReportManagerAPI;
import com.tlc.crm.core.api.analytics.service.field.AnalyticsFieldService;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.sql.resource.MCANACOMPARATORFUNCTION;
import com.tlc.crm.core.sql.resource.MCANAFIELDTYPECOMPARATOR;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.sql.common.api.dml.Criteria;

import lombok.NonNull;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides an abstract way to validate the filter configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public class FilterValidator {
    private final CRMReportManagerAPI crmManager;
    private final AnalyticsFieldService analyticsFieldService;

    private FilterValidator() {
        this.crmManager = CRMReportManagerAPI.INSTANCE();
        this.analyticsFieldService = AnalyticsFieldService.getInstance();
    }

    private static class Instance {
        private static final FilterValidator INSTANCE = new FilterValidator();
    }

    public static FilterValidator INSTANCE() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param jsonObject Represents the {@link JsonObject} containing the configuration
     */
    public void validateRequest(final @NonNull Long orgId,
                                final @NonNull JsonObject jsonObject,
                                final Map<Long, ReportModularField> reportModularFieldMap) throws Exception {
        final Long fieldId = jsonObject.getLong(CRMReportKeys.FIELD_ID);
        final int comparatorId = jsonObject.getInt(CRMReportKeys.COMPARATOR_ID);
        final ReportModularField reportModularField = reportModularFieldMap.get(fieldId);

        if (Objects.isNull(reportModularField)) {
            throw ErrorCode.get(AnalyticsErrorCodes.MEASURE_FIELD_MISMATCHED,
                    "i18n.analytics.config.invalid.measure.field");
        }

        final ModularField modularField = reportModularField.getModularField();

        final Long crmFieldId = modularField.getCrmFieldId();
        Long anaFieldId = null;

        if (!reportModularField.isCustom()) {
            anaFieldId = modularField instanceof CRMSystemModularField crmSystemModularField ? crmSystemModularField.getAnaFieldId() : null;
        }

        if (Objects.isNull(crmFieldId)) {
            final AnalyticsField analyticsField = analyticsFieldService.getAnalyticsFieldById(anaFieldId);

            if (Objects.isNull(analyticsField)) {
                throw ErrorCode.get(AnalyticsErrorCodes.MEASURE_FIELD_MISMATCHED,
                        "i18n.analytics.config.invalid.measure.field");
            }
        }

        if (Objects.isNull(anaFieldId)) {
            final FieldInfo fieldInfo = FieldService.getInstance().getDefaultFieldById(crmFieldId);

            if (Objects.isNull(fieldInfo)) {
                throw ErrorCode.get(AnalyticsErrorCodes.MEASURE_FIELD_MISMATCHED,
                        "i18n.analytics.config.invalid.measure.field");
            }
            final StoreType storeType = fieldInfo.storeType();

            if (Objects.isNull(storeType)) {
                throw ErrorCode.get(AnalyticsErrorCodes.MEASURE_FIELD_MISMATCHED,
                        "i18n.analytics.config.invalid.measure.field");
            }
            final Collection<OperatorType> supportedOperators = OperatorType.getSupportedOperators(storeType);

            if (Objects.isNull(supportedOperators) || supportedOperators.isEmpty()) {
                throw ErrorCode.get(AnalyticsErrorCodes.MEASURE_FIELD_MISMATCHED,
                        "i18n.analytics.config.invalid.measure.field");
            }
            final Collection<Integer> comparatorIds = supportedOperators.stream().map(OperatorType::getTypeCode)
                    .collect(Collectors.toSet());

            if (!comparatorIds.contains(comparatorId)) {
                throw ErrorCode.get(AnalyticsErrorCodes.INVALID_COMPARATOR,
                        "i18n.analytics.config.invalid.comparator");
            }
        }
        final OperatorType comparator = OperatorType.getOperatorType(comparatorId);

        if (Objects.isNull(comparator)) {
            throw ErrorCode.get(AnalyticsErrorCodes.INVALID_COMPARATOR,
                    "i18n.analytics.config.invalid.comparator");
        }

        if (jsonObject.containsKey(CRMViewPropertyKeys.FILTER_VALUES) && comparator.isHasValue()) {
            final JsonArray jsonArray = jsonObject.getJsonArray(CRMViewPropertyKeys.VALUES);
            final int totalSize = jsonArray.size();

            if (0 == totalSize) {
                throw ErrorCode.get(AnalyticsErrorCodes.FILTER_VALUES_SHOULD_NOT_BE_EMPTY,
                        "i18n.analytics.config.filter.invalid.values");
            }

            for (int index = 0; index < totalSize; index++) {
                final Object value = jsonArray.getJsonObject(index)
                        .get(CRMViewPropertyKeys.VALUE);

                if (Objects.isNull(value) || value instanceof final String string && string.isBlank()
                        || value instanceof final Map<?, ?> valueMap && valueMap.isEmpty()) {
                    throw ErrorCode.get(AnalyticsErrorCodes.FILTER_VALUE_SHOULD_NOT_BE_EMPTY,
                            "i18n.analytics.config.filter.invalid.value");
                }
            }
        }

        if (jsonObject.containsKey(CRMReportKeys.FUNCTION_ID)) {
            final boolean functionIdExists = CRMFunction.getSupportedFunctions(comparatorId).stream().filter(Objects::nonNull)
                    .map(CRMFunction::getTypeCode).anyMatch(functionId -> functionId.equals(jsonObject.getInt( CRMReportKeys.FUNCTION_ID )));

            if (!functionIdExists) {
                throw ErrorCode.get(AnalyticsErrorCodes.INVALID_FUNCTION,
                        "i18n.analytics.config.invalid.function");
            }
        }
    }
}
