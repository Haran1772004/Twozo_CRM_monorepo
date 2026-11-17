package com.tlc.crm.core.api.analytics.entity.config.modules;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Represents analytics capability configuration for a modular field.
 * Author: karthick V
 * Version: 1.0
 */
@Getter
public class ModularFieldConfig {
    private static final int ZERO_INDEX = 0;
    private final boolean isPeriodicFilterAllowed;
    private final boolean isFilterAllowed;
    private final boolean isBucketByEnabled;
    private final boolean isDurationAllowed;
    private final boolean isDefaultFilter;
    private final boolean isSortable;
    private final boolean isUnderlyingField;
    private final boolean isMandatory;
    private final boolean isPreCalculated;
    private final boolean isFutureDateRestricted;
    private final Integer axisType;
    private final Collection<Integer> measureIds;
    private final String dropDownName;
    private final Integer relatedModuleId;

    /**
     * Creates modular field configuration from json payload.
     *
     * @param jsonObject Represents serialized modular field configuration.
     */
    public ModularFieldConfig(final JsonObject jsonObject) {
        this.isFilterAllowed = jsonObject.optBoolean(CRMReportKeys.IS_FILTER_ALLOWED);
        this.isBucketByEnabled = jsonObject.optBoolean(CRMReportKeys.IS_BUCKET_ENABLED);
        this.isDurationAllowed = jsonObject.optBoolean(CRMReportKeys.IS_DURATION_ALLOWED);
        this.isPeriodicFilterAllowed = jsonObject.optBoolean(CRMReportKeys.IS_PERIODIC_FILTER_ALLOWED);
        this.isDefaultFilter = jsonObject.optBoolean(CRMReportKeys.IS_DEFAULT_FILTER);
        this.isSortable = jsonObject.optBoolean(CRMReportKeys.IS_SORTABLE);
        this.isUnderlyingField = jsonObject.optBoolean(CRMReportKeys.IS_UNDERLYING_FIELD);
        this.isMandatory = jsonObject.optBoolean(CRMReportKeys.IS_MANDATORY);
        this.isPreCalculated = jsonObject.optBoolean(CRMReportKeys.IS_PRE_CALCULATED);
        this.isFutureDateRestricted = jsonObject.optBoolean(CRMReportKeys.IS_FUTURE_DATE_RESTRICTED);
        this.axisType = jsonObject.getInt(CRMReportKeys.AXIS_TYPE);
        this.measureIds = new ArrayList<>(NumericalConstant.SIX);
        this.dropDownName = jsonObject.optString(CRMReportKeys.DROPDOWN_NAME, StringUtil.EMPTY_STRING);
        this.relatedModuleId = jsonObject.optInt(CRMReportKeys.RELATED_MODULE_ID, 0);

        final JsonArray measureArray = jsonObject.optJsonArray(CRMReportKeys.MEASURE_TYPES);

        if (Objects.nonNull(measureArray)) {
            final int measureSize = measureArray.size();

            if (ZERO_INDEX < measureSize) {
                for (int index = ZERO_INDEX; index < measureSize; index++) {
                    this.measureIds.add(measureArray.getInt(index));
                }
            }
        }
    }

    /**
     * Creates modular field configuration from explicit properties.
     *
     * @param axisType Represents supported axis type.
     * @param isPeriodicFilterAllowed Indicates periodic filter support.
     * @param isFilterAllowed Indicates filter support.
     * @param isBucketByEnabled Indicates bucket support.
     * @param isDurationAllowed Indicates duration support.
     * @param isDefaultFilter Indicates default filter flag.
     * @param isSortable Indicates sorting support.
     * @param isUnderlyingField Indicates underlying-data inclusion support.
     * @param isMandatory Indicates mandatory field flag.
     * @param measureIds Represents supported measure ids.
     */
    public ModularFieldConfig(final Integer axisType, final boolean isPeriodicFilterAllowed, final boolean isFilterAllowed,
                              final boolean isBucketByEnabled, final boolean isDurationAllowed, final boolean isDefaultFilter,
                              final boolean isSortable, final boolean isUnderlyingField, final boolean isMandatory, final boolean isFutureDateRestricted,
                              final Collection<Integer> measureIds, final String dropDownName, final Integer relatedModuleId)
    {
        this.isFilterAllowed = isFilterAllowed;
        this.isBucketByEnabled = isBucketByEnabled;
        this.isDurationAllowed = isDurationAllowed;
        this.isDefaultFilter = isDefaultFilter;
        this.isPeriodicFilterAllowed = isPeriodicFilterAllowed;
        this.isSortable = isSortable;
        this.isUnderlyingField = isUnderlyingField;
        this.isMandatory = isMandatory;
        this.isFutureDateRestricted = isFutureDateRestricted;
        this.isPreCalculated = false;
        this.axisType = axisType;
        this.measureIds = measureIds;
        this.dropDownName = dropDownName;
        this.relatedModuleId = relatedModuleId;
    }

    /**
     * Converts configuration as json representation.
     *
     * @return The serialized {@link JsonObject}.
     */
    public JsonObject configAsJson() {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(CRMReportKeys.AXIS_TYPE, axisType);
        jsonObject.put(CRMReportKeys.IS_FILTER_ALLOWED, isFilterAllowed);
        jsonObject.put(CRMReportKeys.IS_BUCKET_ENABLED, isBucketByEnabled);
        jsonObject.put(CRMReportKeys.IS_DURATION_ALLOWED, isDurationAllowed);
        jsonObject.put(CRMReportKeys.IS_PERIODIC_FILTER_ALLOWED, isPeriodicFilterAllowed);
        jsonObject.put(CRMReportKeys.IS_DEFAULT_FILTER, isDefaultFilter);
        jsonObject.put(CRMReportKeys.IS_SORTABLE, isSortable);
        jsonObject.put(CRMReportKeys.IS_UNDERLYING_FIELD, isUnderlyingField);
        jsonObject.put(CRMReportKeys.IS_MANDATORY, isMandatory);
        jsonObject.put(CRMReportKeys.DROPDOWN_NAME, dropDownName);
        jsonObject.put(CRMReportKeys.RELATED_MODULE_ID, relatedModuleId);
        jsonObject.put(CRMReportKeys.IS_PRE_CALCULATED, isPreCalculated);
        jsonObject.put(CRMReportKeys.IS_FUTURE_DATE_RESTRICTED, isFutureDateRestricted);

        if (Objects.nonNull(measureIds) && !measureIds.isEmpty()) {
            final JsonArray measureArray = Json.array();

            for (final Integer measureId : measureIds) {
                measureArray.put(measureId);
            }
            jsonObject.put(CRMReportKeys.MEASURE_TYPES, measureArray);
        }

        return jsonObject;
    }
}
