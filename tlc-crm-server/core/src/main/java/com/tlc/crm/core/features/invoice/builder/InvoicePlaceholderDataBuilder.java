package com.tlc.crm.core.features.invoice.builder;

import com.tlc.commons.json.resolver.model.ArrayPlaceholderEntries;
import com.tlc.commons.json.resolver.model.PlaceholderEntries;
import com.tlc.commons.json.resolver.model.PlaceholderKey;
import com.tlc.commons.json.resolver.model.PlaceholderValue;
import com.tlc.crm.core.features.sales.template.model.AbstractSalesDocumentPlaceHolderDataBuilder;
import com.tlc.crm.core.features.sales.template.model.ArrayTypePlaceholderValue;
import com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey;
import com.tlc.crm.core.features.sales.template.model.LineItemPlaceholderValue;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.CURRENCY;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.HEADER_COMPANY_NAME_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.HEADER_DUE_DATE_LABEL;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.HEADER_DUE_DATE_FORMAT;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.HEADER_DUE_DATE_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.HEADER_INVOICE_DATE_LABEL;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.HEADER_INVOICE_DATE_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.HEADER_LOGO_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.HEADER_PO_NUMBER_LABEL;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.HEADER_PO_NUMBER_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.HEADER_TITLE_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.INVOICE_URL;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.ITEM_DISCOUNT_AMOUNT_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.ITEM_DISCOUNT_CURRENCY_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.ITEM_DISCOUNT_FORMAT;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.ITEM_DISCOUNT_LABEL;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.ITEM_DISCOUNT_PERCENTAGE_AND_AMOUNT_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.ITEM_DISCOUNT_PERCENTAGE_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.ITEM_SUB_TOTAL_CURRENCY_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.ITEM_SUB_TOTAL_LABEL;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.ITEM_SUB_TOTAL_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.ITEM_TAX_CURRENCY_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.ITEM_TAX_LABEL;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.ITEM_TAX_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.ITEM_TOTAL_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.ITEM_TOTAL_VALUE_CURRENCY;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.ITEM_TOTAL_VALUE_LABEL;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.LANGUAGE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.LINE_ITEMS;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.LOCALE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.PARTY_BILLING_ADDRESS_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.PARTY_BILLING_SAME_AS_COMPANY_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.PARTY_BILLING_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.PARTY_BUYER_COMPANY_ADDRESS_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.PARTY_BUYER_COMPANY_LABEL;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.PARTY_BUYER_COMPANY_NAME_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.PARTY_BUYER_COMPANY_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.PARTY_BUYER_CONTACT_EMAIL_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.PARTY_BUYER_CONTACT_LABEL;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.PARTY_BUYER_CONTACT_NAME_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.PARTY_BUYER_CONTACT_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.PARTY_BUYER_TITLE_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.PARTY_SHIPPING_ADDRESS_LABEL;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.PARTY_SHIPPING_ADDRESS_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.PARTY_SHIPPING_SAME_AS_COMPANY_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.PARTY_SHIPPING_VALUE;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.SOURCE_COMPANY;
import static com.tlc.crm.core.features.invoice.model.InvoicePlaceholderKey.SOURCE_PRODUCT;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.SOURCE_CONTACT;

/**
 * <p>
 * Builds the placeholder data used to resolve the default invoice template.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class InvoicePlaceholderDataBuilder extends AbstractSalesDocumentPlaceHolderDataBuilder<InvoicePlaceholderDataBuilder> {

    private PlaceholderValue language;
    private PlaceholderValue locale;
    private PlaceholderValue currency;
    private PlaceholderValue invoiceUrl;

    private PlaceholderValue headerInvoiceDateLabel;
    private PlaceholderValue headerInvoiceDateValue;

    private PlaceholderValue headerDueDateLabel;
    private PlaceholderValue headerDueDateFormat;
    private PlaceholderValue headerDueDateValue;

    private PlaceholderValue headerPoNumberLabel;
    private PlaceholderValue headerPoNumberValue;

    private PlaceholderValue headerCompanyNameValue;
    private PlaceholderValue headerLogoValue;

    private PlaceholderValue partyBuyerTitleValue;
    private PlaceholderValue partyBuyerCompanyLabel;

    private PlaceholderValue partyBuyerContactLabel;
    private PlaceholderValue partyBuyerContactNameValue;
    private PlaceholderValue partyBuyerContactEmailValue;
    private PlaceholderValue partyBuyerContactValue;

    private PlaceholderValue partyBillingSameAsCompanyValue;
    private PlaceholderValue partyBillingAddressValue;
    private PlaceholderValue partyBillingValue;

    private PlaceholderValue partyShippingAddressLabel;
    private PlaceholderValue partyShippingSameAsCompanyValue;
    private PlaceholderValue partyShippingAddressValue;
    private PlaceholderValue partyShippingValue;

    /**
     * {@inheritDoc}
     */
    @Override
    protected PlaceholderKey.Provider buildPlaceHolderKeyProvider() {
        return new InvoicePlaceholderKey.Provider();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PlaceholderEntries buildScalarValues() {
        final PlaceholderEntries scalars = new PlaceholderEntries();

        scalars.addEntry(LANGUAGE, language);
        scalars.addEntry(LOCALE, locale);
        scalars.addEntry(CURRENCY, currency);
        scalars.addEntry(INVOICE_URL, invoiceUrl);

        scalars.addEntry(HEADER_TITLE_VALUE, headerTitleValue);
        scalars.addEntry(HEADER_INVOICE_DATE_LABEL, headerInvoiceDateLabel);
        scalars.addEntry(HEADER_INVOICE_DATE_VALUE, headerInvoiceDateValue);
        scalars.addEntry(HEADER_DUE_DATE_LABEL, headerDueDateLabel);
        scalars.addEntry(HEADER_DUE_DATE_FORMAT, headerDueDateFormat);
        scalars.addEntry(HEADER_DUE_DATE_VALUE, headerDueDateValue);
        scalars.addEntry(HEADER_PO_NUMBER_LABEL, headerPoNumberLabel);
        scalars.addEntry(HEADER_PO_NUMBER_VALUE, headerPoNumberValue);
        scalars.addEntry(HEADER_COMPANY_NAME_VALUE, headerCompanyNameValue);
        scalars.addEntry(HEADER_LOGO_VALUE, headerLogoValue);

        scalars.addEntry(PARTY_BUYER_TITLE_VALUE, partyBuyerTitleValue);
        scalars.addEntry(PARTY_BUYER_COMPANY_LABEL, partyBuyerCompanyLabel);
        scalars.addEntry(PARTY_BUYER_COMPANY_VALUE, partyBuyerCompanyValue);
        scalars.addEntry(PARTY_BUYER_COMPANY_NAME_VALUE, partyBuyerCompanyNameValue);
        scalars.addEntry(PARTY_BUYER_COMPANY_ADDRESS_VALUE, partyBuyerCompanyAddressValue);
        scalars.addEntry(PARTY_BUYER_CONTACT_LABEL, partyBuyerContactLabel);
        scalars.addEntry(PARTY_BUYER_CONTACT_NAME_VALUE, partyBuyerContactNameValue);
        scalars.addEntry(PARTY_BUYER_CONTACT_EMAIL_VALUE, partyBuyerContactEmailValue);
        scalars.addEntry(PARTY_BUYER_CONTACT_VALUE, partyBuyerContactValue);

        scalars.addEntry(PARTY_BILLING_SAME_AS_COMPANY_VALUE, partyBillingSameAsCompanyValue);
        scalars.addEntry(PARTY_BILLING_ADDRESS_VALUE, partyBillingAddressValue);
        scalars.addEntry(PARTY_BILLING_VALUE, partyBillingValue);

        scalars.addEntry(PARTY_SHIPPING_ADDRESS_LABEL, partyShippingAddressLabel);
        scalars.addEntry(PARTY_SHIPPING_SAME_AS_COMPANY_VALUE, partyShippingSameAsCompanyValue);
        scalars.addEntry(PARTY_SHIPPING_ADDRESS_VALUE, partyShippingAddressValue);
        scalars.addEntry(PARTY_SHIPPING_VALUE, partyShippingValue);

        scalars.addEntry(ITEM_SUB_TOTAL_LABEL, itemSubTotalLabel);
        scalars.addEntry(ITEM_SUB_TOTAL_VALUE, itemSubTotalValue);
        scalars.addEntry(ITEM_SUB_TOTAL_CURRENCY_VALUE, currencyCodeValue);

        scalars.addEntry(ITEM_DISCOUNT_LABEL, itemDiscountLabel);
        scalars.addEntry(ITEM_DISCOUNT_FORMAT, itemDiscountFormat);
        scalars.addEntry(ITEM_DISCOUNT_AMOUNT_VALUE, itemDiscountAmountValue);
        scalars.addEntry(ITEM_DISCOUNT_PERCENTAGE_VALUE, itemDiscountPercentageValue);
        scalars.addEntry(ITEM_DISCOUNT_PERCENTAGE_AND_AMOUNT_VALUE, itemDiscountPercentageAndAmountValue);
        scalars.addEntry(ITEM_DISCOUNT_CURRENCY_VALUE, currencyCodeValue);

        scalars.addEntry(ITEM_TAX_LABEL, itemTaxLabel);
        scalars.addEntry(ITEM_TAX_VALUE, itemTaxValue);
        scalars.addEntry(ITEM_TAX_CURRENCY_VALUE, currencyCodeValue);

        scalars.addEntry(ITEM_TOTAL_VALUE_LABEL, itemTotalValueLabel);
        scalars.addEntry(ITEM_TOTAL_VALUE, itemTotalValue);
        scalars.addEntry(ITEM_TOTAL_VALUE_CURRENCY, currencyCodeValue);

        scalars.addEntry(SOURCE_CONTACT, sourceContactRenamedValue);
        scalars.addEntry(SOURCE_COMPANY, sourceCompanyRenamedValue);
        scalars.addEntry(SOURCE_PRODUCT, sourceProductRenamedValue);

        return scalars;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ArrayPlaceholderEntries buildArrayValues() {
        final ArrayPlaceholderEntries arrays = new ArrayPlaceholderEntries();

        addRowsIfNonNull(arrays, LINE_ITEMS, lineItemPlaceholderValues);
        return arrays;
    }

    /**
     * <p>
     * Adds the given placeholder rows to the specified array placeholder
     * collection when the rows are not {@code null}.
     * </p>
     *
     * @param arrays         the array placeholder entries to which the rows are added
     * @param placeholderKey the placeholder key representing the array
     * @param rows           the placeholder rows to add
     * @param <T>            the type of the placeholder row
     */
    private static <T extends ArrayTypePlaceholderValue> void addRowsIfNonNull(final ArrayPlaceholderEntries arrays,
                                                                               final PlaceholderKey placeholderKey, final Collection<T> rows) {

        if (Objects.nonNull(rows)) {

            for (final T item : rows) {
                final PlaceholderEntries placeholderEntries = new PlaceholderEntries();

                item.toMap().forEach(placeholderEntries::addEntry);

                arrays.addEntry(placeholderKey, placeholderEntries);
            }
        }
    }

    /**
     * <p>
     * Sets the language placeholder value.
     * </p>
     *
     * @param language the language placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setLanguage(final PlaceholderValue language) {
        this.language = language;

        return this;
    }

    /**
     * <p>
     * Sets the locale placeholder value.
     * </p>
     *
     * @param locale the locale placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setLocale(final PlaceholderValue locale) {
        this.locale = locale;

        return this;
    }

    /**
     * <p>
     * Sets the currency placeholder value.
     * </p>
     *
     * @param currency the currency placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setCurrency(final PlaceholderValue currency) {
        this.currency = currency;

        return this;
    }

    /**
     * <p>
     * Sets the invoice URL placeholder value.
     * </p>
     *
     * @param invoiceUrl the invoice URL placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setInvoiceUrl(final PlaceholderValue invoiceUrl) {
        this.invoiceUrl = invoiceUrl;

        return this;
    }

    /**
     * <p>
     * Sets the header title placeholder value.
     * </p>
     *
     * @param headerTitleValue the header title placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setHeaderTitleValue(final PlaceholderValue headerTitleValue) {
        this.headerTitleValue = headerTitleValue;

        return this;
    }

    /**
     * <p>
     * Sets the invoice date label placeholder value.
     * </p>
     *
     * @param headerInvoiceDateLabel the invoice date label placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setHeaderInvoiceDateLabel(final PlaceholderValue headerInvoiceDateLabel) {
        this.headerInvoiceDateLabel = headerInvoiceDateLabel;

        return this;
    }

    /**
     * <p>
     * Sets the invoice date placeholder value.
     * </p>
     *
     * @param headerInvoiceDateValue the invoice date placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setHeaderInvoiceDateValue(final PlaceholderValue headerInvoiceDateValue) {
        this.headerInvoiceDateValue = headerInvoiceDateValue;

        return this;
    }

    /**
     * <p>
     * Sets the due date label placeholder value.
     * </p>
     *
     * @param headerDueDateLabel the due date label placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setHeaderDueDateLabel(final PlaceholderValue headerDueDateLabel) {
        this.headerDueDateLabel = headerDueDateLabel;

        return this;
    }

    /**
     * <p>
     * Sets the due date payment terms placeholder value.
     * </p>
     *
     * @param headerDueDateFormat the due date payment terms placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setHeaderDueDateFormat(final PlaceholderValue headerDueDateFormat) {
        this.headerDueDateFormat = headerDueDateFormat;

        return this;
    }

    /**
     * <p>
     * Sets the due date placeholder value.
     * </p>
     *
     * @param headerDueDateValue the due date placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setHeaderDueDateValue(final PlaceholderValue headerDueDateValue) {
        this.headerDueDateValue = headerDueDateValue;

        return this;
    }

    /**
     * <p>
     * Sets the purchase order number label placeholder value.
     * </p>
     *
     * @param headerPoNumberLabel the purchase order number label placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setHeaderPoNumberLabel(final PlaceholderValue headerPoNumberLabel) {
        this.headerPoNumberLabel = headerPoNumberLabel;

        return this;
    }

    /**
     * <p>
     * Sets the purchase order number placeholder value.
     * </p>
     *
     * @param headerPoNumberValue the purchase order number placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setHeaderPoNumberValue(final PlaceholderValue headerPoNumberValue) {
        this.headerPoNumberValue = headerPoNumberValue;

        return this;
    }

    /**
     * <p>
     * Sets the header company name placeholder value.
     * </p>
     *
     * @param headerCompanyNameValue the header company name placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setHeaderCompanyNameValue(final PlaceholderValue headerCompanyNameValue) {
        this.headerCompanyNameValue = headerCompanyNameValue;

        return this;
    }

    /**
     * <p>
     * Sets the header logo placeholder value.
     * </p>
     *
     * @param headerLogoValue the header logo placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setHeaderLogoValue(final PlaceholderValue headerLogoValue) {
        this.headerLogoValue = headerLogoValue;

        return this;
    }

    /**
     * <p>
     * Sets the buyer title placeholder value.
     * </p>
     *
     * @param partyBuyerTitleValue the buyer title placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setPartyBuyerTitleValue(final PlaceholderValue partyBuyerTitleValue) {
        this.partyBuyerTitleValue = partyBuyerTitleValue;

        return this;
    }

    /**
     * <p>
     * Sets the buyer company label placeholder value.
     * </p>
     *
     * @param partyBuyerCompanyLabel the buyer company label placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setPartyBuyerCompanyLabel(final PlaceholderValue partyBuyerCompanyLabel) {
        this.partyBuyerCompanyLabel = partyBuyerCompanyLabel;

        return this;
    }

    /**
     * <p>
     * Sets the buyer company placeholder value.
     * </p>
     *
     * @param partyBuyerCompanyValue the buyer company placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setPartyBuyerCompanyValue(final PlaceholderValue partyBuyerCompanyValue) {
        this.partyBuyerCompanyValue = partyBuyerCompanyValue;

        return this;
    }

    /**
     * <p>
     * Sets the buyer company name placeholder value.
     * </p>
     *
     * @param partyBuyerCompanyNameValue the buyer company name placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setPartyBuyerCompanyNameValue(final PlaceholderValue partyBuyerCompanyNameValue) {
        this.partyBuyerCompanyNameValue = partyBuyerCompanyNameValue;

        return this;
    }

    /**
     * <p>
     * Sets the buyer company address placeholder value.
     * </p>
     *
     * @param partyBuyerCompanyAddressValue the buyer company address placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setPartyBuyerCompanyAddressValue(final PlaceholderValue partyBuyerCompanyAddressValue) {
        this.partyBuyerCompanyAddressValue = partyBuyerCompanyAddressValue;

        return this;
    }

    /**
     * <p>
     * Sets the buyer contact label placeholder value.
     * </p>
     *
     * @param partyBuyerContactLabel the buyer contact label placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setPartyBuyerContactLabel(final PlaceholderValue partyBuyerContactLabel) {
        this.partyBuyerContactLabel = partyBuyerContactLabel;

        return this;
    }

    /**
     * <p>
     * Sets the buyer contact name placeholder value.
     * </p>
     *
     * @param partyBuyerContactNameValue the buyer contact name placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setPartyBuyerContactNameValue(final PlaceholderValue partyBuyerContactNameValue) {
        this.partyBuyerContactNameValue = partyBuyerContactNameValue;

        return this;
    }

    /**
     * <p>
     * Sets the buyer contact email placeholder value.
     * </p>
     *
     * @param partyBuyerContactEmailValue the buyer contact email placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setPartyBuyerContactEmailValue(final PlaceholderValue partyBuyerContactEmailValue) {
        this.partyBuyerContactEmailValue = partyBuyerContactEmailValue;

        return this;
    }

    /**
     * <p>
     * Sets the buyer contact placeholder value.
     * </p>
     *
     * @param partyBuyerContactValue the buyer contact placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setPartyBuyerContactValue(final PlaceholderValue partyBuyerContactValue) {
        this.partyBuyerContactValue = partyBuyerContactValue;

        return this;
    }

    /**
     * <p>
     * Sets the billing address same-as-company placeholder value.
     * </p>
     *
     * @param partyBillingSameAsCompanyValue the same-as-company placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setPartyBillingSameAsCompanyValue(final PlaceholderValue partyBillingSameAsCompanyValue) {
        this.partyBillingSameAsCompanyValue = partyBillingSameAsCompanyValue;

        return this;
    }

    /**
     * <p>
     * Sets the billing address placeholder value.
     * </p>
     *
     * @param partyBillingAddressValue the billing address placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setPartyBillingAddressValue(final PlaceholderValue partyBillingAddressValue) {
        this.partyBillingAddressValue = partyBillingAddressValue;

        return this;
    }

    /**
     * <p>
     * Sets the shipping address label placeholder value.
     * </p>
     *
     * @param partyShippingAddressLabel the shipping address label placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setPartyShippingAddressLabel(final PlaceholderValue partyShippingAddressLabel) {
        this.partyShippingAddressLabel = partyShippingAddressLabel;

        return this;
    }

    /**
     * <p>
     * Sets the shipping address same-as-company placeholder value.
     * </p>
     *
     * @param partyShippingSameAsCompanyValue the same-as-company placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setPartyShippingSameAsCompanyValue(final PlaceholderValue partyShippingSameAsCompanyValue) {
        this.partyShippingSameAsCompanyValue = partyShippingSameAsCompanyValue;

        return this;
    }

    /**
     * <p>
     * Sets the shipping address placeholder value.
     * </p>
     *
     * @param partyShippingAddressValue the shipping address placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setPartyShippingAddressValue(final PlaceholderValue partyShippingAddressValue) {
        this.partyShippingAddressValue = partyShippingAddressValue;

        return this;
    }

    /**
     * <p>
     * Sets the invoice line item placeholder values.
     * </p>
     *
     * @param lineItemPlaceholderValues the invoice line item placeholder values
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setLineItems(final List<LineItemPlaceholderValue> lineItemPlaceholderValues) {
        this.lineItemPlaceholderValues = lineItemPlaceholderValues;

        return this;
    }

    /**
     * <p>
     * Sets the subtotal label placeholder value.
     * </p>
     *
     * @param itemSubTotalLabel the subtotal label placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setItemSubTotalLabel(final PlaceholderValue itemSubTotalLabel) {
        this.itemSubTotalLabel = itemSubTotalLabel;

        return this;
    }

    @Override
    public InvoicePlaceholderDataBuilder setItemSubTotalFormat(PlaceholderValue itemSubTotalFormat) {
        this.itemSubTotalFormat = itemSubTotalLabel;

        return this;
    }

    /**
     * <p>
     * Sets the subtotal placeholder value.
     * </p>
     *
     * @param itemSubTotalValue the subtotal placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setItemSubTotalValue(final PlaceholderValue itemSubTotalValue) {
        this.itemSubTotalValue = itemSubTotalValue;

        return this;
    }

    /**
     * <p>
     * Sets the subtotal currency placeholder value.
     * </p>
     *
     * @param currencyCodeValue the subtotal currency placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setCurrencyCodeValue(final PlaceholderValue currencyCodeValue) {
        this.currencyCodeValue = currencyCodeValue;

        return this;
    }

    /**
     * <p>
     * Sets the discount label placeholder value.
     * </p>
     *
     * @param itemDiscountLabel the discount label placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setItemDiscountLabel(final PlaceholderValue itemDiscountLabel) {
        this.itemDiscountLabel = itemDiscountLabel;

        return this;
    }

    /**
     * <p>
     * Sets the discount format placeholder value.
     * </p>
     *
     * @param itemDiscountFormat the discount format placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setItemDiscountFormat(final PlaceholderValue itemDiscountFormat) {
        this.itemDiscountFormat = itemDiscountFormat;

        return this;
    }

    /**
     * <p>
     * Sets the discount amount placeholder value.
     * </p>
     *
     * @param itemDiscountAmountValue the discount amount placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setItemDiscountAmountValue(final PlaceholderValue itemDiscountAmountValue) {
        this.itemDiscountAmountValue = itemDiscountAmountValue;

        return this;
    }

    /**
     * <p>
     * Sets the discount percentage placeholder value.
     * </p>
     *
     * @param itemDiscountPercentageValue the discount percentage placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setItemDiscountPercentageValue(final PlaceholderValue itemDiscountPercentageValue) {
        this.itemDiscountPercentageValue = itemDiscountPercentageValue;

        return this;
    }

    /**
     * <p>
     * Sets the combined discount percentage and amount placeholder value.
     * </p>
     *
     * @param itemDiscountPercentageAndAmountValue the combined discount placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setItemDiscountPercentageAndAmountValue(final PlaceholderValue itemDiscountPercentageAndAmountValue) {
        this.itemDiscountPercentageAndAmountValue = itemDiscountPercentageAndAmountValue;

        return this;
    }

    /**
     * <p>
     * Sets the tax label placeholder value.
     * </p>
     *
     * @param itemTaxLabel the tax label placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setItemTaxLabel(final PlaceholderValue itemTaxLabel) {
        this.itemTaxLabel = itemTaxLabel;

        return this;
    }

    /**
     * <p>
     * Sets the tax placeholder value.
     * </p>
     *
     * @param itemTaxValue the tax placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setItemTaxValue(final PlaceholderValue itemTaxValue) {
        this.itemTaxValue = itemTaxValue;

        return this;
    }

    /**
     * <p>
     * Sets the total value label placeholder value.
     * </p>
     *
     * @param itemTotalValueLabel the total value label placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setItemTotalValueLabel(final PlaceholderValue itemTotalValueLabel) {
        this.itemTotalValueLabel = itemTotalValueLabel;

        return this;
    }

    /**
     * <p>
     * Sets the total value placeholder value.
     * </p>
     *
     * @param itemTotalValue the total value placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setItemTotalValue(final PlaceholderValue itemTotalValue) {
        this.itemTotalValue = itemTotalValue;

        return this;
    }

    /**
     * <p>
     * Sets the renamed contact source placeholder value.
     * </p>
     *
     * @param sourceContactRenamedValue the renamed contact source placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setSourceContactRenamedValue(final PlaceholderValue sourceContactRenamedValue) {
        this.sourceContactRenamedValue = sourceContactRenamedValue;

        return this;
    }

    /**
     * <p>
     * Sets the renamed company source placeholder value.
     * </p>
     *
     * @param sourceCompanyRenamedValue the renamed company source placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setSourceCompanyRenamedValue(final PlaceholderValue sourceCompanyRenamedValue) {
        this.sourceCompanyRenamedValue = sourceCompanyRenamedValue;

        return this;
    }

    /**
     * <p>
     * Sets the renamed product source placeholder value.
     * </p>
     *
     * @param sourceProductRenamedValue the renamed product source placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setSourceProductRenamedValue(final PlaceholderValue sourceProductRenamedValue) {
        this.sourceProductRenamedValue = sourceProductRenamedValue;

        return this;
    }

    /**
     * <p>
     * Sets the party billing placeholder value.
     * </p>
     *
     * @param partyBillingValue the party billing placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setPartyBillingValue(final PlaceholderValue partyBillingValue) {
        this.partyBillingValue = partyBillingValue;

        return this;
    }

    /**
     * <p>
     * Sets the party shipping placeholder value.
     * </p>
     *
     * @param partyShippingValue the party shipping placeholder value
     * @return this builder instance
     */
    public InvoicePlaceholderDataBuilder setPartyShippingValue(final PlaceholderValue partyShippingValue) {
        this.partyShippingValue = partyShippingValue;

        return this;
    }
}