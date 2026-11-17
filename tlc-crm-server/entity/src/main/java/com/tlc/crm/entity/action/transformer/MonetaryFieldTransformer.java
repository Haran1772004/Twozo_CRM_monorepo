package com.tlc.crm.entity.action.transformer;

import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.list.transformer.formats.Formats;

import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.fields.transformer.TransformerFields;
import com.tlc.crm.entity.models.type.Monetary;

import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.l10n.model.currency.Currency;

import java.math.BigDecimal;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Manages {@link Monetary} related transformer actions.
 *
 * @author eswari
 * @version 1.0
 */
@Transformer(name = "monetary", type = Transformer.Type.FIELD)
public class MonetaryFieldTransformer implements FieldTransformer {

    private final FieldTypeService fieldTypeService;
    private final BaseCurrencyService baseCurrencyService;

    public MonetaryFieldTransformer() {
        fieldTypeService = FieldTypeService.getInstance();
        baseCurrencyService = BaseCurrencyService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organisation id.
     * @param fieldName  the name of the field to use for the transformation
     * @param entity the map of data to transform
     * @return the {@link Map} contains transformed value
     */
    @Override
    public Map<String, Object> transform(final long orgId, final String fieldName, final Map<String, Object> entity) {
        final Optional<Object> optValue = Optional.ofNullable(entity.get(fieldName));
        final String isManuallyAdded = TransformerFields.IS_MANUALLY_ADDED.getName();
        final String isProductAvailable = TransformerFields.IS_PRODUCT_AVAILABLE.getName();

        return optValue.map(fieldId -> {
            final Map<String, Object> transformedValue = new HashMap<>();
            final Map<StoreSubType, FieldSubType> subTypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.MONETARY);
            final String columnFormat = Formats.COLUMN_FORMAT.get();
            final String currencyId = TransformerFields.CURRENCY_ID.getName();

            for (final Map.Entry<StoreSubType, FieldSubType> entry : subTypes.entrySet()) {
                final String name = entry.getKey().getAliasName();
                final StoreSubType storeSubType = StoreSubType.get(name);
                final Object value = entity.get(String.format(columnFormat, fieldName, name));

                if (Objects.nonNull(storeSubType)) {
                    switch (storeSubType) {
                        case CURRENCY -> {
                            final Object currency = entity.get(String.format(columnFormat, fieldName, currencyId));

                            transformedValue.put(TransformerFields.SYMBOL.getName(), currency instanceof Short id ? Currency.getCurrencyById(id).getSymbol() : currency);
                        }
                        case VALUE -> {
                            if (value instanceof Number) {
                                transformedValue.put(name, baseCurrencyService.applyPrecision(orgId, (value instanceof BigDecimal)
                                        ? (BigDecimal) value : BigDecimal.valueOf(((Number) value).doubleValue())));
                            }
                        }
                        default -> transformedValue.put(name, value);
                    }
                } else {
                    transformedValue.put(name, value);
                }
            }

            if (entity.containsKey(isProductAvailable)) {
                transformedValue.put(isManuallyAdded, !(boolean) entity.get(isProductAvailable));
            }

            transformedValue.put(TransformerFields.ID.getName(), fieldId);

            return transformedValue;
        }).orElseGet(() -> null);
    }
}