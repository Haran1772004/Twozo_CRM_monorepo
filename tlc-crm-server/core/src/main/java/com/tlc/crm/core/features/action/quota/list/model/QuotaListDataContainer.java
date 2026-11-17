package com.tlc.crm.core.features.action.quota.list.model;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.list.ListInterpreter;
import com.tlc.crm.common.list.ListStructure;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.model.ListDataContainer;
import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.features.action.quota.list.fields.QuotaListFields;
import com.tlc.crm.core.features.api.quota.model.ForecastType;
import com.tlc.crm.core.features.api.quota.model.QuotaMetric;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.i18n.I18nResolutionAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.math.BigDecimal;

/**
 * <p>
 * A model which represents the {@link QuotaListDataContainer}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class QuotaListDataContainer extends ListDataContainer {

    private static final Collection<String> FIELDS_TO_INCLUDE = Set.of(QuotaListFields.USER_ID, QuotaListFields.ID, QuotaListFields.CAN_VIEW,
            QuotaListFields.CAN_EDIT, QuotaListFields.CAN_CLONE, QuotaListFields.CAN_RECALCULATE_ACHIEVEMENT, QuotaListFields.CAN_DELETE);
    private static final String CURRENT = "Current";
    private static final String FORECAST = "Forecast";
    private static final Long DEFAULT_VALUE = 0L;

    private JsonObject requestJson;

    public QuotaListDataContainer(final long orgId, final I18nResolutionAdapter i18NResolutionAdapter, final Collection<Map<String, Object>> responseData) {
        super(orgId, i18NResolutionAdapter, responseData);
    }

    public QuotaListDataContainer(final long orgId, final I18nResolutionAdapter i18NResolutionAdapter,
                                  final Collection<Map<String, Object>> responseData, final JsonObject requestJson, final Collection<Long> userIds) {
        super(orgId, i18NResolutionAdapter, responseData, userIds);
        this.requestJson = requestJson;
    }

    public JsonObject getRequestJson() {
        return requestJson;
    }

    /**
     * <p>
     * Converts the rows into data.
     * </p>
     *
     * @param listInterpreter Represents the {@link ListInterpreter} details
     * @return A collection of maps representing the interpreted and updated data
     */
    public Collection<Map<String, Object>> getData(final ListInterpreter listInterpreter) {
        final Collection<Map<String, Object>> data = getData();
        final Collection<Map<String, Object>> updatedData = new ArrayList<>();
        final ListStructure listStructure = listInterpreter.structure();
        final JsonObject criteriaJson = requestJson.getJsonObject(ListActionFields.CRITERIA);
        final ForecastType forecastType = ForecastType.getById(criteriaJson.optShort(QuotaListFields.FORECAST_TYPE, ForecastType.WON.getId()));
        final I18nResolutionAdapter i18NResolutionAdapter = getI18NResolutionAdapter();
        final long orgId = getOrgId();
        final CrmUser crmUser = LoggedInUserInfo.getUser();
        final RolePermission overrideSubcategoryPermission = RolePermissionService.getInstance().getPermission(orgId, crmUser.getRoleId(),
                Category.Type.QUOTAS_AND_FORECASTING, SubCategory.Type.OVERRIDE, Action.Type.ACCESS);

        for (final Map<String, Object> row : data) {
            final Map<String, Object> rowMap = new HashMap<>();

            row.forEach((key, value) -> {
                final FieldConfig fieldConfig = listStructure.getField(key);

                if (Objects.nonNull(fieldConfig)) {
                    final FieldTransformer transformer = fieldConfig.getTransformer();

                    if (Objects.nonNull(transformer)) {
                        final Object transformedValue = transformer.transform(orgId, key, row);

                        if (fieldConfig.isPersistent() && transformedValue instanceof Map) {
                            resolveCategory(i18NResolutionAdapter, row, fieldConfig, (Map<String, Object>) transformedValue, forecastType, overrideSubcategoryPermission, listStructure);
                        }
                        rowMap.put(key, transformedValue);
                    }
                } else if (FIELDS_TO_INCLUDE.contains(key)) {
                    rowMap.put(key, value);
                }
            });
            updatedData.add(rowMap);
        }
        return updatedData;
    }

    /**
     * <p>
     * Resolves a quota category value
     * </p>
     *
     * @param i18nResolutionAdapter         Represents the {@link I18nResolutionAdapter}
     * @param rowMap                        Represents the quota rows
     * @param fieldConfig                   Represents the {@link FieldConfig}
     * @param data                          The source data map containing quota details
     * @param forecastType                  Represents the {@link ForecastType}
     * @param overrideSubcategoryPermission Represents the override subcategory permission
     */
    private void resolveCategory(final I18nResolutionAdapter i18nResolutionAdapter, final Map<String, Object> rowMap,
                                 final FieldConfig fieldConfig, final Map<String, Object> data, final ForecastType forecastType,
                                 final RolePermission overrideSubcategoryPermission, final ListStructure listStructure) {
        final List<Map<String, Object>> summary = (List<Map<String, Object>>) data.get(QuotaListFields.SUMMARY);
        final Map<String, Object> categorySummaryMap = summary.get(NumericalConstant.ZERO);
        final boolean isWon = forecastType.isWon();
        final boolean isEditable = (isWon && Boolean.TRUE.equals(rowMap.get(QuotaListFields.CAN_EDIT))
                && Objects.isNull(rowMap.get(QuotaListFields.TEAM_QUOTA_BASE_QUOTA_ID)));
        final Short metric = (Short) rowMap.get(QuotaListFields.METRIC);
        final QuotaMetric quotaMetric = QuotaMetric.getById(metric);
        final Number value = NumberUtils.parseNumber(data.getOrDefault(QuotaListFields.VALUE, DEFAULT_VALUE));
        double categoryValue = Objects.isNull(value) ? DEFAULT_VALUE : value.doubleValue();

        if (isWon) {
            categoryValue += getCommittedCategoryValue(fieldConfig, rowMap, listStructure);
        }
        final Number quotaWonValue = isWon ? NumberUtils.parseNumber(rowMap.getOrDefault(QuotaListFields.QUOTA_WON_VALUE, DEFAULT_VALUE)) : null;
        final double wonValue = Objects.isNull(quotaWonValue) ? DEFAULT_VALUE : quotaWonValue.doubleValue();
        final double totalValue = categoryValue + wonValue;
        final String formattedValue = BigDecimal.valueOf(totalValue).stripTrailingZeros().toPlainString();
        final String displayValue = quotaMetric.isDealValue() ? StringUtil.join(StringUtil.Delimiter.EMPTY_SPACE,
                BaseCurrencyService.getInstance().getBaseCurrency(getOrgId()).getDefaultCurrency().getSymbol(), formattedValue) : formattedValue;
        final boolean hasOverridePermission = Objects.nonNull(overrideSubcategoryPermission) && overrideSubcategoryPermission.isGranted();
        final boolean isOverrideAllowed = isWon && hasOverridePermission;

        data.put(QuotaListFields.VALUE, displayValue);
        data.put(QuotaListFields.IS_EDITABLE, isEditable && hasOverridePermission);
        data.put(QuotaListFields.IS_OVERRIDDEN, isOverrideAllowed && Boolean.TRUE.equals(data.get(QuotaListFields.IS_OVERRIDDEN)));

        if (!isOverrideAllowed) {
            data.remove(QuotaListFields.OVERRIDE_VALUE);

            if (summary.size() > NumericalConstant.ONE) {
                summary.remove(NumericalConstant.ONE);
            }
        }

        if (Objects.nonNull(categorySummaryMap)) {
            categorySummaryMap.put(QuotaListFields.DISPLAY_NAME, StringUtil.join(StringUtil.Delimiter.EMPTY_SPACE, CURRENT,
                    i18nResolutionAdapter.get(fieldConfig.getDisplayName()), FORECAST));
            categorySummaryMap.put(QuotaListFields.VALUE, displayValue);
            categorySummaryMap.put(QuotaListFields.IS_CURRENT_VALUE, true);
        }
    }

    /**
     * <p>
     * Gets the Committed category's WITHOUT_WON value if the given field is a Best Case category.
     * This is used when forecast type is Won, so that Best Case totals include Committed values.
     * </p>
     *
     * @param fieldConfig   The current field configuration (forecast category)
     * @param rowMap        The quota row data containing all category values
     * @param listStructure Represents the {@link ListStructure}
     * @return The Committed category value if the field is Best Case, otherwise 0
     */
    private double getCommittedCategoryValue(final FieldConfig fieldConfig, final Map<String, Object> rowMap, final ListStructure listStructure) {
        final FieldConfig.Config config = fieldConfig.getConfig();

        if (Objects.nonNull(config) && QuotaListFields.BEST_CASE.equals(config.getType())) {
            final Optional<FieldConfig> committedFieldConfig = listStructure.getFields().stream().filter(field -> {
                final FieldConfig.Config committedConfig = field.getConfig();

                if (Objects.isNull(committedConfig)) {
                    return false;
                }
                return QuotaListFields.COMMITTED.equals(committedConfig.getType());
            }).findFirst();

            if (committedFieldConfig.isPresent()) {
                final Number committedValue = NumberUtils.parseNumber(rowMap.getOrDefault(String.valueOf(committedFieldConfig.get().getIdAsString()), DEFAULT_VALUE));

                return Objects.isNull(committedValue) ? DEFAULT_VALUE : committedValue.doubleValue();
            }
        }
        return DEFAULT_VALUE;
    }
}
