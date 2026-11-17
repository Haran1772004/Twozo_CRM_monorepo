package com.tlc.crm.core.action.dropdown;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.entity.dropdown.annotation.Dropdown;
import com.tlc.l10n.model.currency.Currency;

/**
 * <p>
 * Provides functionality related to managing {@link Product} related dropdown actions for quotes.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 * @see AbstractProductDropdownAction
 */
@Dropdown(name = "quoteProductList", fieldAlias = "quoteProduct")
public class QuoteProductList extends AbstractProductDropdownAction {

    private final DealManager dealManager;

    public QuoteProductList() {
        this.dealManager = DealManager.getInstance();
    }

    /**
     * <p>
     * Resolves the currency associated with the deal for the quote product dropdown.
     * </p>
     *
     * @param orgId Organization identifier.
     * @param data  Holds the details needed to resolve the deal.
     * @return {@link Currency} associated with the deal, or {@code null} if no currency is available.
     */
    @Override
    protected Currency resolveCurrency(final long orgId, final JsonObject data) {
        return dealManager.getDealCurrency(orgId, data.getLong(DealFields.DEAL_ID)).orElseGet(() -> null);
    }
}