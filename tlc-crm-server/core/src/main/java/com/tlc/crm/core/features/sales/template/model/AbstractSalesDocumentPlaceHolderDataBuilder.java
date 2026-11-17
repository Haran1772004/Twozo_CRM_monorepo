package com.tlc.crm.core.features.sales.template.model;

import com.tlc.commons.json.resolver.model.PlaceholderData;
import com.tlc.commons.json.resolver.model.PlaceholderValue;

import java.util.List;

/**
 * <p>
 * Base data builder for sales document placeholder values shared across
 * different sales document types such as invoices and quotations.
 * </p>
 *
 * @param <T> Represents the concrete builder type.
 * @author Mohamed Yasar K
 * @version 1.0
 */
public abstract class AbstractSalesDocumentPlaceHolderDataBuilder<T> extends PlaceholderData.Builder {

    /**
     * <p>
     * Represents the placeholder value for the header title.
     * </p>
     */
    protected PlaceholderValue headerTitleValue;

    /**
     * <p>
     * Represents the placeholder value for the subtotal currency.
     * </p>
     */
    protected PlaceholderValue currencyCodeValue;

    /**
     * <p>
     * Represents the placeholder value for the buyer company.
     * </p>
     */
    protected PlaceholderValue partyBuyerCompanyValue;

    /**
     * <p>
     * Represents the placeholder value for the buyer company name.
     * </p>
     */
    protected PlaceholderValue partyBuyerCompanyNameValue;

    /**
     * <p>
     * Represents the placeholder value for the buyer company address.
     * </p>
     */
    protected PlaceholderValue partyBuyerCompanyAddressValue;

    /**
     * <p>
     * Represents the placeholder value for the subtotal label.
     * </p>
     */
    protected PlaceholderValue itemSubTotalLabel;

    /**
     * <p>
     * Represents the placeholder value for the subtotal format.
     * </p>
     */
    protected PlaceholderValue itemSubTotalFormat;

    /**
     * <p>
     * Represents the placeholder value for the subtotal.
     * </p>
     */
    protected PlaceholderValue itemSubTotalValue;

    /**
     * <p>
     * Represents the placeholder value for the tax label.
     * </p>
     */
    protected PlaceholderValue itemTaxLabel;

    /**
     * <p>
     * Represents the placeholder value for the tax amount.
     * </p>
     */
    protected PlaceholderValue itemTaxValue;

    /**
     * <p>
     * Represents the placeholder value for the discount label.
     * </p>
     */
    protected PlaceholderValue itemDiscountLabel;

    /**
     * <p>
     * Represents the placeholder value for the discount format.
     * </p>
     */
    protected PlaceholderValue itemDiscountFormat;

    /**
     * <p>
     * Represents the placeholder value for the discount amount.
     * </p>
     */
    protected PlaceholderValue itemDiscountAmountValue;

    /**
     * <p>
     * Represents the placeholder value for the discount percentage.
     * </p>
     */
    protected PlaceholderValue itemDiscountPercentageValue;

    /**
     * <p>
     * Represents the placeholder value for the discount percentage and amount.
     * </p>
     */
    protected PlaceholderValue itemDiscountPercentageAndAmountValue;

    /**
     * <p>
     * Represents the placeholder value for the total value label.
     * </p>
     */
    protected PlaceholderValue itemTotalValueLabel;

    /**
     * <p>
     * Represents the placeholder value for the total value.
     * </p>
     */
    protected PlaceholderValue itemTotalValue;

    /**
     * <p>
     * Represents the placeholder value for the renamed contact source.
     * </p>
     */
    protected PlaceholderValue sourceContactRenamedValue;

    /**
     * <p>
     * Represents the placeholder value for the renamed company source.
     * </p>
     */
    protected PlaceholderValue sourceCompanyRenamedValue;

    /**
     * <p>
     * Represents the placeholder value for the renamed product source.
     * </p>
     */
    protected PlaceholderValue sourceProductRenamedValue;

    /**
     * <p>
     * Holds the placeholder values for the line items associated with the
     * sales document.
     * </p>
     */
    protected List<LineItemPlaceholderValue> lineItemPlaceholderValues;

    /**
     * <p>
     * Sets the buyer company placeholder value.
     * </p>
     *
     * @param partyBuyerCompanyValue Represents the buyer company placeholder value.
     * @return The concrete sales document data builder instance.
     */
    public abstract T setPartyBuyerCompanyValue(final PlaceholderValue partyBuyerCompanyValue);

    /**
     * <p>
     * Sets the buyer company name placeholder value.
     * </p>
     *
     * @param partyBuyerCompanyNameValue Represents the buyer company name placeholder value.
     * @return The concrete sales document data builder instance.
     */
    public abstract T setPartyBuyerCompanyNameValue(final PlaceholderValue partyBuyerCompanyNameValue);

    /**
     * <p>
     * Sets the buyer company address placeholder value.
     * </p>
     *
     * @param partyBuyerCompanyAddressValue Represents the buyer company address placeholder value.
     * @return The concrete sales document data builder instance.
     */
    public abstract T setPartyBuyerCompanyAddressValue(final PlaceholderValue partyBuyerCompanyAddressValue);

    /**
     * <p>
     * Sets the line item placeholder values.
     * </p>
     *
     * @param lineItemPlaceholderValues Represents the line item placeholder values.
     * @return The concrete sales document data builder instance.
     */
    public abstract T setLineItems(final List<LineItemPlaceholderValue> lineItemPlaceholderValues);

    /**
     * <p>
     * Sets the subtotal label placeholder value.
     * </p>
     *
     * @param itemSubTotalLabel Represents the subtotal label placeholder value.
     * @return The concrete sales document data builder instance.
     */
    public abstract T setItemSubTotalLabel(final PlaceholderValue itemSubTotalLabel);

    /**
     * <p>
     * Sets the subtotal format placeholder value.
     * </p>
     *
     * @param itemSubTotalFormat Represents the subtotal format placeholder value.
     * @return The concrete sales document data builder instance.
     */
    public abstract T setItemSubTotalFormat(final PlaceholderValue itemSubTotalFormat);

    /**
     * <p>
     * Sets the subtotal placeholder value.
     * </p>
     *
     * @param itemSubTotalValue Represents the subtotal placeholder value.
     * @return The concrete sales document data builder instance.
     */
    public abstract T setItemSubTotalValue(final PlaceholderValue itemSubTotalValue);

    /**
     * <p>
     * Sets the tax label placeholder value.
     * </p>
     *
     * @param itemTaxLabel Represents the tax label placeholder value.
     * @return The concrete sales document data builder instance.
     */
    public abstract T setItemTaxLabel(final PlaceholderValue itemTaxLabel);

    /**
     * <p>
     * Sets the tax placeholder value.
     * </p>
     *
     * @param itemTaxValue Represents the tax placeholder value.
     * @return The concrete sales document data builder instance.
     */
    public abstract T setItemTaxValue(final PlaceholderValue itemTaxValue);

    /**
     * <p>
     * Sets the discount label placeholder value.
     * </p>
     *
     * @param itemDiscountLabel Represents the discount label placeholder value.
     * @return The concrete sales document data builder instance.
     */
    public abstract T setItemDiscountLabel(final PlaceholderValue itemDiscountLabel);

    /**
     * <p>
     * Sets the discount format placeholder value.
     * </p>
     *
     * @param itemDiscountFormat Represents the discount format placeholder value.
     * @return The concrete sales document data builder instance.
     */
    public abstract T setItemDiscountFormat(final PlaceholderValue itemDiscountFormat);

    /**
     * <p>
     * Sets the discount amount placeholder value.
     * </p>
     *
     * @param itemDiscountAmountValue Represents the discount amount placeholder value.
     * @return The concrete sales document data builder instance.
     */
    public abstract T setItemDiscountAmountValue(final PlaceholderValue itemDiscountAmountValue);

    /**
     * <p>
     * Sets the discount percentage placeholder value.
     * </p>
     *
     * @param itemDiscountPercentageValue Represents the discount percentage placeholder value.
     * @return The concrete sales document data builder instance.
     */
    public abstract T setItemDiscountPercentageValue(final PlaceholderValue itemDiscountPercentageValue);

    /**
     * <p>
     * Sets the discount percentage and amount placeholder value.
     * </p>
     *
     * @param itemDiscountPercentageAndAmountValue Represents the discount percentage and amount
     *                                             placeholder value.
     * @return The concrete sales document data builder instance.
     */
    public abstract T setItemDiscountPercentageAndAmountValue(
            final PlaceholderValue itemDiscountPercentageAndAmountValue);

    /**
     * <p>
     * Sets the total value label placeholder value.
     * </p>
     *
     * @param itemTotalValueLabel Represents the total value label placeholder value.
     * @return The concrete sales document data builder instance.
     */
    public abstract T setItemTotalValueLabel(final PlaceholderValue itemTotalValueLabel);

    /**
     * <p>
     * Sets the total value placeholder value.
     * </p>
     *
     * @param itemTotalValue Represents the total value placeholder value.
     * @return The concrete sales document data builder instance.
     */
    public abstract T setItemTotalValue(final PlaceholderValue itemTotalValue);

    /**
     * <p>
     * Sets the renamed contact source placeholder value.
     * </p>
     *
     * @param sourceContactRenamedValue Represents the renamed contact source placeholder value.
     * @return The concrete sales document data builder instance.
     */
    public abstract T setSourceContactRenamedValue(final PlaceholderValue sourceContactRenamedValue);

    /**
     * <p>
     * Sets the renamed company source placeholder value.
     * </p>
     *
     * @param sourceCompanyRenamedValue Represents the renamed company source placeholder value.
     * @return The concrete sales document data builder instance.
     */
    public abstract T setSourceCompanyRenamedValue(final PlaceholderValue sourceCompanyRenamedValue);

    /**
     * <p>
     * Sets the renamed product source placeholder value.
     * </p>
     *
     * @param sourceProductRenamedValue Represents the renamed product source placeholder value.
     * @return The concrete sales document data builder instance.
     */
    public abstract T setSourceProductRenamedValue(final PlaceholderValue sourceProductRenamedValue);
}