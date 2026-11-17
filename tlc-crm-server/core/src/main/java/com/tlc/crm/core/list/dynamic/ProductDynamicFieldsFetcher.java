package com.tlc.crm.core.list.dynamic;

import com.tlc.crm.core.api.product.manager.VariationManager;
import com.tlc.crm.core.api.product.model.PriceTag;
import com.tlc.crm.core.list.dynamic.model.ListDynamicFields;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.fields.transformer.TransformerFields;
import com.tlc.crm.entity.list.dynamic.model.DynamicFieldRequest;
import com.tlc.crm.entity.list.dynamic.model.DynamicFieldsData;
import com.tlc.l10n.model.currency.Currency;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Implements {@link DynamicFieldsFetcher} the for fetching dynamic field data to product list
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class ProductDynamicFieldsFetcher extends DynamicFieldsFetcher {

    private static final Double DEFAULT_PRICE = 0.0D;

    private final VariationManager variationManager;
    private final BaseCurrencyService baseCurrencyService;

    private ProductDynamicFieldsFetcher() {
        variationManager = VariationManager.getInstance();
        baseCurrencyService = BaseCurrencyService.getInstance();
    }

    private static class InstanceHolder {
        private static final ProductDynamicFieldsFetcher INSTANCE = new ProductDynamicFieldsFetcher();
    }

    /**
     * <p>
     * Gets the {@link ProductDynamicFieldsFetcher} instance
     * </p>
     *
     * @return The {@link ProductDynamicFieldsFetcher}
     */
    public static ProductDynamicFieldsFetcher getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param dynamicFieldRequest Represents the {@link DynamicFieldRequest}
     * @return The {@link DynamicFieldsData}
     */
    @Override
    public DynamicFieldsData get(final DynamicFieldRequest dynamicFieldRequest) {
        final long orgId = dynamicFieldRequest.orgId();
        final Collection<Long> entityIds = dynamicFieldRequest.entityIds();
        final Collection<String> dynamicFields = dynamicFieldRequest.dynamicFields();
        final boolean baseCurrencyAmountRequired = dynamicFields.stream().anyMatch(ListDynamicFields.BASE_CURRENCY_AMOUNT::contains);
        final DynamicFieldsData dynamicFieldsData = new DynamicFieldsData();

        if (baseCurrencyAmountRequired) {
            final Map<Long, PriceTag> defaultCurrencyPriceTags = variationManager.fetchDefaultCurrencyPriceTag(orgId, entityIds);
            final Currency currency = baseCurrencyService.getDefaultCurrency(orgId);
            final Map<String, Map<String, Object>> productRows = new HashMap<>();

            if (!defaultCurrencyPriceTags.isEmpty()) {
                defaultCurrencyPriceTags.keySet().forEach(id -> {
                    final Map<String, Object> productRow = new HashMap<>();

                    if (defaultCurrencyPriceTags.containsKey(id)) {
                        final PriceTag priceTag = defaultCurrencyPriceTags.get(id);

                        if (Objects.nonNull(priceTag)) {
                            productRow.put(ListDynamicFields.BASE_CURRENCY_AMOUNT, Map.of(TransformerFields.SYMBOL.getName(), currency.getSymbol(),
                                    TransformerFields.VALUE.getName(), priceTag.getPrice()));
                        }
                    } else {
                        productRow.put(ListDynamicFields.BASE_CURRENCY_AMOUNT, Map.of(TransformerFields.SYMBOL.getName(), currency.getSymbol(),
                                TransformerFields.VALUE.getName(), DEFAULT_PRICE));
                    }
                    productRows.put(id.toString(), productRow);
                });
                dynamicFieldsData.addAll(productRows);
            }
        }
        return dynamicFieldsData;
    }
}