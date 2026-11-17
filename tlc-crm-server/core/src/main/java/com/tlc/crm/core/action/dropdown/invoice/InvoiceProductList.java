package com.tlc.crm.core.action.dropdown.invoice;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.action.dropdown.AbstractProductDropdownAction;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.features.invoice.fields.InvoiceFields;
import com.tlc.crm.core.features.invoice.manager.InvoiceManager;
import com.tlc.crm.entity.dropdown.annotation.Dropdown;
import com.tlc.l10n.model.currency.Currency;

/**
 * <p>
 * Provides functionality related to managing {@link Product} related dropdown actions for invoices.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 * @see AbstractProductDropdownAction
 */
@Dropdown(name = "invoiceProductList", fieldAlias = "invoiceProduct")
public class InvoiceProductList extends AbstractProductDropdownAction {

    private final InvoiceManager invoiceManager;

    public InvoiceProductList() {
        this.invoiceManager = InvoiceManager.getInstance();
    }

    /**
     * <p>
     * Resolves the currency associated with the invoice for the invoice product dropdown.
     * </p>
     *
     * @param orgId Organization identifier.
     * @param data  Holds the details needed to resolve the invoice.
     * @return {@link Currency} associated with the invoice.
     */
    @Override
    protected Currency resolveCurrency(final long orgId, final JsonObject data) {
        return invoiceManager.get(orgId, data.getLong(InvoiceFields.INVOICE_ID)).getCurrency();
    }
}