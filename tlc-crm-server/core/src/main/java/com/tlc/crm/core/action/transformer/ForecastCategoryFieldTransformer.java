package com.tlc.crm.core.action.transformer;

import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.core.features.action.quota.list.fields.QuotaListFields;
import com.tlc.crm.core.features.api.quota.model.QuotaMetric;
import com.tlc.crm.entity.currency.BaseCurrencyService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Manages deal won field related transformer actions.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@Transformer(name = "forecastCategory", type = Transformer.Type.FIELD)
public class ForecastCategoryFieldTransformer extends AbstractQuotaFieldTransformer implements FieldTransformer {

    private static final long DEFAULT_VALUE = 0L;
    private static final String OVERRIDE_VALUE = "i18n.quota.override.value";

    private final BaseCurrencyService baseCurrencyService;

    public ForecastCategoryFieldTransformer() {
        baseCurrencyService = BaseCurrencyService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organization id.
     * @param fieldName The name of the field to use for the transformation.
     * @param entity    The map of data to transform.
     * @return An Object representing the transformed.
     */
    @Override
    public Object transform(final long orgId, final String fieldName, final Map<String, Object> entity) {
        final Object value = Objects.requireNonNullElse(entity.get(fieldName), DEFAULT_VALUE);
        final Map<String, Object> transformedValue = new HashMap<>();
        final Short metric = (Short) entity.get(QuotaListFields.METRIC);
        final QuotaMetric quotaMetric = QuotaMetric.getById(metric);
        final List<Map<String, Object>> summary = new ArrayList<>();
        final long overrideValue = (Long) Objects.requireNonNullElse(entity.get(StringUtil.join(StringUtil.Delimiter.EMPTY_STRING, QuotaListFields.OVERRIDE_VALUE, fieldName)), DEFAULT_VALUE);
        final String displayValue = quotaMetric.isDealValue() ? StringUtil.join(StringUtil.Delimiter.EMPTY_SPACE, baseCurrencyService.getBaseCurrency(orgId).getDefaultCurrency().getSymbol(), String.valueOf(value))
                : String.valueOf(value);
        final Map<String, Object> currentValueMap = new HashMap<>();
        final Object summaryId = entity.get(StringUtil.join(StringUtil.Delimiter.EMPTY_STRING, QuotaListFields.SUMMARY_ID, fieldName));

        currentValueMap.put(QuotaListFields.VALUE, displayValue);
        summary.add(currentValueMap);
        transformedValue.put(QuotaListFields.VALUE, value);

        final String displayOverrideValue = quotaMetric.isDealValue() ? StringUtil.join(StringUtil.Delimiter.EMPTY_SPACE,
                baseCurrencyService.getBaseCurrency(orgId).getDefaultCurrency().getSymbol(), String.valueOf(overrideValue)) : String.valueOf(overrideValue);

        transformedValue.put(QuotaListFields.IS_OVERRIDDEN, overrideValue > NumericalConstant.ZERO);
        transformedValue.put(QuotaListFields.OVERRIDE_VALUE, displayOverrideValue);
        summary.add(Map.of(QuotaListFields.DISPLAY_NAME, resolveI18nValue(OVERRIDE_VALUE), QuotaListFields.VALUE, displayOverrideValue));

        if (Objects.nonNull(summaryId)) {
            transformedValue.put(QuotaListFields.SUMMARY_ID, summaryId);
        }
        transformedValue.put(QuotaListFields.CATEGORY_ID, Long.parseLong(fieldName));
        transformedValue.put(QuotaListFields.SUMMARY, summary);

        return transformedValue;
    }
}
