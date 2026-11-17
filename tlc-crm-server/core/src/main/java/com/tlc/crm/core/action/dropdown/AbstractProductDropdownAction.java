package com.tlc.crm.core.action.dropdown;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.models.SortConfig;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.model.PriceTag;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.api.product.model.Variation;
import com.tlc.crm.core.field.product.ProductFields;
import com.tlc.crm.core.internal.product.status.ProductErrorCodes;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.fields.option.OptionKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.fields.service.settings.SourceSettingsService;
import com.tlc.l10n.model.currency.Currency;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Provides common functionality related to managing product dropdown actions.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 * @see AbstractDropdownAction
 */
public abstract class AbstractProductDropdownAction extends AbstractDropdownAction {

    protected final ProductManager productManager;

    protected AbstractProductDropdownAction() {
        this.productManager = ProductManager.getInstance();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final Long orgId = dropdownRequest.getOrgId();

        if (!SourceSettingsService.getInstance().isSourceEnabled(orgId, Source.PRODUCT.getName())) {
            throw ErrorCode.get(ProductErrorCodes.PRODUCT_IS_DISABLED, "i18n.product.disabled");
        }
        final JsonObject data = dropdownRequest.data();
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final long totalRecords = productManager.getTotalCount(orgId, dropdownCriteria);
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final JsonObject sortJson = data.optJsonObject(ProductFields.SORT);
            final boolean includeUnitPrice = data.getBoolean(ProductFields.INCLUDE_UNIT_PRICE);
            final SortConfig sortConfig = Objects.isNull(sortJson) ? new SortConfig(ProductFields.NAME, SortType.ASCENDING)
                    : new SortConfig(sortJson.getString(ProductFields.FIELD), SortType.getSortType(sortJson.getString(ProductFields.TYPE)));
            final Currency currency = resolveCurrency(orgId, data);
            final Collection<Product> products = productManager.getProductDropdownList(
                    orgId, dropdownLimit, dropdownCriteria, includeUnitPrice, currency, sortConfig);

            for (final Product product : products) {
                final JsonObject jsonObject = Json.object();

                jsonObject.put(OptionKeys.VALUE, product.id());
                jsonObject.put(OptionKeys.NAME, product.getName());

                addUnitPriceDetails(product, currency, jsonObject);
                optionContainer.addOption(jsonObject);
            }
        }

        return optionContainer;
    }

    /**
     * <p>
     * Resolves the currency to be used for the product dropdown.
     * </p>
     *
     * @param orgId Organization identifier.
     * @param data  Holds the details needed to resolve the currency.
     * @return {@link Currency} associated with the dropdown context.
     */
    protected abstract Currency resolveCurrency(final long orgId, final JsonObject data);

    /**
     * <p>
     * Adds unit price details to the product dropdown option.
     * </p>
     *
     * @param product    Holds the product details.
     * @param currency   Currency used for the product price.
     * @param jsonObject Holds the product dropdown option.
     */
    protected void addUnitPriceDetails(final Product product, final Currency currency, final JsonObject jsonObject) {
        final JsonObject unitPriceJson = Json.object();
        final Variation variation = product.getDefaultVariation();
        double price = 0.00;
        double tax = 0.00;
        double discount = 0.00;

        if (Objects.nonNull(variation)) {
            final Collection<PriceTag> priceTags = variation.getPriceTags();

            if (Objects.nonNull(priceTags) && !priceTags.isEmpty()) {
                final PriceTag priceTag = priceTags.iterator().next();
                final Double priceTagPrice = priceTag.getPrice();
                final Double priceTagTax = priceTag.getTax();
                final Double priceTagDiscount = priceTag.getDiscount();

                price = Objects.isNull(priceTagPrice) ? price : priceTagPrice;
                tax = Objects.isNull(priceTagTax) ? tax : priceTagTax;
                discount = Objects.isNull(priceTagDiscount) ? discount : priceTagDiscount;
            }
        }
        unitPriceJson.put(ProductFields.CURRENCY, Objects.isNull(currency) ? null : currency.getSymbol());
        unitPriceJson.put(ProductFields.PRICE, price);
        unitPriceJson.put(ProductFields.TAX, tax);
        unitPriceJson.put(ProductFields.DISCOUNT, discount);

        jsonObject.put(ProductFields.UNIT_PRICE, unitPriceJson);
    }
}