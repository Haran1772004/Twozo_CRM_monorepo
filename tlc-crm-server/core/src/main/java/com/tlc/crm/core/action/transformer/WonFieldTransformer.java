package com.tlc.crm.core.action.transformer;

import com.tlc.commons.util.StringUtil;
import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.features.action.quota.list.fields.QuotaListFields;
import com.tlc.crm.core.features.api.quota.model.QuotaMetric;
import com.tlc.crm.entity.currency.BaseCurrencyService;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Manages deal won field related transformer actions.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@Transformer(name = "won", type = Transformer.Type.FIELD)
public class WonFieldTransformer implements FieldTransformer {

    private final BaseCurrencyService baseCurrencyService;

    public WonFieldTransformer() {
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
        final Double value = NumberUtils.parseDouble(entity.get(fieldName));
        final Map<String, Object> transformedValue = new HashMap<>();
        final String wonValue = (value % 1 == 0) ? String.valueOf(value.longValue()) : String.valueOf(value);
        final Short metric = (Short) entity.get(QuotaListFields.METRIC);
        final QuotaMetric quotaMetric = QuotaMetric.getById(metric);
        final String displayWonValue = quotaMetric.isDealValue() ? StringUtil.join(StringUtil.Delimiter.EMPTY_SPACE,
                baseCurrencyService.getBaseCurrency(orgId).getDefaultCurrency().getSymbol(), wonValue)
                : wonValue;
        final long targetValue = NumberUtils.parseNumber(entity.get(QuotaListFields.QUOTA)).longValue();
        long percentage = 0L;

        if (targetValue > 0) {
            percentage = (NumberUtils.parseNumber(wonValue).longValue() * 100) / targetValue;
        }
        transformedValue.put(QuotaListFields.PERCENTAGE, percentage);
        transformedValue.put(QuotaListFields.VALUE, displayWonValue);

        return transformedValue;
    }
}
