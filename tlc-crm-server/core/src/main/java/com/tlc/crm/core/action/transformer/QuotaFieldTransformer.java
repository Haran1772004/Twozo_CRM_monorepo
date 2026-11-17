package com.tlc.crm.core.action.transformer;

import com.tlc.commons.util.StringUtil;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.features.action.quota.list.fields.QuotaListFields;
import com.tlc.crm.core.features.api.quota.model.QuotaMetric;
import com.tlc.crm.core.features.api.quota.model.QuotaTimePeriod;
import com.tlc.crm.entity.currency.BaseCurrencyService;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Manages deal won fields related transformer actions.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@Transformer(name = "quota", type = Transformer.Type.FIELD)
public class QuotaFieldTransformer extends AbstractQuotaFieldTransformer {

    private final BaseCurrencyService baseCurrencyService;

    public QuotaFieldTransformer() {
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
        final Double target = NumberUtils.parseDouble(entity.get(fieldName));
        final String value = (target % 1 == 0) ? String.valueOf(target.longValue()) : String.valueOf(target);
        final QuotaMetric quotaMetric = QuotaMetric.getById((Short) entity.get(QuotaListFields.METRIC));
        final Map<String, Object> transformedValue = new HashMap<>();

        transformedValue.put(QuotaListFields.VALUE, quotaMetric.isDealValue() ? StringUtil.join(StringUtil.Delimiter.EMPTY_SPACE,
                baseCurrencyService.getBaseCurrency(orgId).getDefaultCurrency().getSymbol(), value) : value);

        if (entity.containsKey(QuotaListFields.TIME_PERIOD)) {
            transformedValue.put(QuotaListFields.TIME_PERIOD, resolveI18nValue(QuotaTimePeriod.getById((Short)
                    entity.get(QuotaListFields.TIME_PERIOD)).getDisplayName()));
        }
        return transformedValue;
    }
}
