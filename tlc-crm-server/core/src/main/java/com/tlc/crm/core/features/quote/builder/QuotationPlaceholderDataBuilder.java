package com.tlc.crm.core.features.quote.builder;

import com.tlc.commons.json.resolver.model.ArrayPlaceholderEntries;
import com.tlc.commons.json.resolver.model.PlaceholderEntries;
import com.tlc.commons.json.resolver.model.PlaceholderKey;
import com.tlc.commons.json.resolver.model.PlaceholderValue;
import com.tlc.crm.core.features.sales.template.model.AbstractSalesDocumentPlaceHolderDataBuilder;
import com.tlc.crm.core.features.sales.template.model.LineItemPlaceholderValue;
import com.tlc.crm.core.features.quote.model.AcceptanceBuyerContactPlaceholderValue;
import com.tlc.crm.core.features.quote.model.AcceptanceSellerContactPlaceholderValue;
import com.tlc.crm.core.features.sales.template.model.ArrayTypePlaceholderValue;
import com.tlc.crm.core.features.quote.model.PartyBuyerContactPlaceholderValue;
import com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ACCEPTANCE_BUYER_CONTACTS;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ACCEPTANCE_BUYER_SUB_TITLE_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ACCEPTANCE_METHOD_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ACCEPTANCE_SELLER_CONTACTS;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ACCEPTANCE_SELLER_SUB_TITLE_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ACCEPTANCE_TITLE_DESCRIPTION;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ACCEPTANCE_TITLE_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.CURRENCY;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.CUSTOM_SECTION_TITLE_DESCRIPTION;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.CUSTOM_SECTION_TITLE_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.HEADER_COMPANY_NAME_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.HEADER_CURRENCY_LABEL;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.HEADER_CURRENCY_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.HEADER_EXPIRATION_DATE_LABEL;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.HEADER_EXPIRATION_DATE_MIN_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.HEADER_EXPIRATION_DATE_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.HEADER_ISSUE_DATE_LABEL;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.HEADER_ISSUE_DATE_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.HEADER_LOGO_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.HEADER_QUOTE_REF_LABEL;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.HEADER_QUOTE_REF_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.HEADER_TITLE_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ITEM_DISCOUNT_AMOUNT_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ITEM_DISCOUNT_CURRENCY_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ITEM_DISCOUNT_FORMAT;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ITEM_DISCOUNT_LABEL;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ITEM_DISCOUNT_PERCENTAGE_AND_AMOUNT_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ITEM_DISCOUNT_PERCENTAGE_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ITEM_DUE_CURRENCY_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ITEM_DUE_DESCRIPTION;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ITEM_DUE_LABEL;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ITEM_DUE_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ITEM_SUB_TOTAL_CURRENCY_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ITEM_SUB_TOTAL_FORMAT;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ITEM_SUB_TOTAL_LABEL;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ITEM_SUB_TOTAL_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ITEM_SUB_TOTAL_WITH_BREAKDOWN_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ITEM_TAX_CURRENCY_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ITEM_TAX_LABEL;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ITEM_TAX_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ITEM_TOTAL_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ITEM_TOTAL_VALUE_CURRENCY;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.ITEM_TOTAL_VALUE_LABEL;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.LANGUAGE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.LINE_ITEMS;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.LOCALE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.PARTY_BUYER_COMPANY_ADDRESS_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.PARTY_BUYER_COMPANY_NAME_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.PARTY_BUYER_COMPANY_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.PARTY_BUYER_CONTACTS;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.PARTY_BUYER_CONTACT_LABEL;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.PARTY_BUYER_TITLE_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.PARTY_SELLER_COMPANY_ADDRESS_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.PARTY_SELLER_COMPANY_NAME_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.PARTY_SELLER_COMPANY_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.PARTY_SELLER_CONTACT_EMAIL_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.PARTY_SELLER_CONTACT_LABEL;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.PARTY_SELLER_CONTACT_NAME_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.PARTY_SELLER_CONTACT_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.PARTY_SELLER_TITLE_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.PAYMENT_BILLING_LABEL;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.PAYMENT_BILLING_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.PAYMENT_TITLE_LABEL;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.QUOTE_NAME;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.QUOTE_URL;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.SOURCE_COMPANY;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.SOURCE_CONTACT;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.SOURCE_PRODUCT;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.SUMMARY_DISCOUNT_AMOUNT_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.SUMMARY_DISCOUNT_CURRENCY_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.SUMMARY_DISCOUNT_FORMAT;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.SUMMARY_DISCOUNT_LABEL;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.SUMMARY_DISCOUNT_PERCENTAGE_AND_AMOUNT_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.SUMMARY_DISCOUNT_PERCENTAGE_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.SUMMARY_EFFECTIVE_DATE_DROPDOWN_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.SUMMARY_EFFECTIVE_DATE_FORM_HIDDEN_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.SUMMARY_EFFECTIVE_DATE_DEFAULT_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.SUMMARY_EFFECTIVE_DATE_LABEL;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.SUMMARY_EFFECTIVE_DATE_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.SUMMARY_TERM_LENGTH_LABEL;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.SUMMARY_TERM_LENGTH_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.SUMMARY_VALUE;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.SUMMARY_VALUE_CURRENCY;
import static com.tlc.crm.core.features.quote.model.QuotationPlaceholderKey.SUMMARY_VALUE_LABEL;

public class QuotationPlaceholderDataBuilder extends AbstractSalesDocumentPlaceHolderDataBuilder<QuotationPlaceholderDataBuilder> {

    // Settings
    private PlaceholderValue quoteName;
    private PlaceholderValue language;
    private PlaceholderValue locale;
    private PlaceholderValue currency;
    private PlaceholderValue quoteUrl;

    // Header - Quote Reference
    private PlaceholderValue headerQuoteRefLabel;
    private PlaceholderValue headerQuoteRefValue;

    // Header - Issue Date
    private PlaceholderValue headerIssueDateLabel;
    private PlaceholderValue headerIssueDateValue;

    // Header - Expiration Date
    private PlaceholderValue headerExpirationDateLabel;
    private PlaceholderValue headerExpirationDateValue;
    private PlaceholderValue headerExpirationDateMinValue;

    // Header - Currency
    private PlaceholderValue headerCurrencyLabel;
    private PlaceholderValue headerCurrencyValue;

    // Header - Logo
    private PlaceholderValue headerLogoValue;

    // Header - Company Name
    private PlaceholderValue headerCompanyNameValue;

    // Party - Seller Title
    private PlaceholderValue partySellerTitleValue;

    // Party - Seller Company
    private PlaceholderValue partySellerCompanyValue;
    private PlaceholderValue partySellerCompanyNameValue;
    private PlaceholderValue partySellerCompanyAddressValue;

    // Party - Seller Contact
    private PlaceholderValue partySellerContactLabel;
    private PlaceholderValue partySellerContactValue;
    private PlaceholderValue partySellerContactNameValue;
    private PlaceholderValue partySellerContactEmailValue;

    // Party - Buyer Title
    private PlaceholderValue partyBuyerTitleValue;

    // Party - Buyer Contact
    private PlaceholderValue partyBuyerContactLabel;

    // Party - Buyer Contacts (Array)
    private Collection<PartyBuyerContactPlaceholderValue> partyBuyerContactPlaceholderValues;

    // Summary - Effective Date
    private PlaceholderValue summaryEffectiveDateLabel;
    private PlaceholderValue summaryEffectiveDateDropdownValue;
    private PlaceholderValue summaryEffectiveDateValue;
    private PlaceholderValue summaryEffectiveDateFormHiddenValue;
    private PlaceholderValue summaryEffectiveDateDefaultValue;

    // Summary - Term Length
    private PlaceholderValue summaryTermLengthLabel;
    private PlaceholderValue summaryTermLengthValue;

    // Summary - Discount
    private PlaceholderValue summaryDiscountLabel;
    private PlaceholderValue summaryDiscountFormat;
    private PlaceholderValue summaryDiscountAmountValue;
    private PlaceholderValue summaryDiscountPercentageValue;
    private PlaceholderValue summaryDiscountPercentageAndAmountValue;

    // Summary - Total Value
    private PlaceholderValue summaryValueLabel;
    private PlaceholderValue summaryValue;

    // Line Items - Totals - Subtotal
    private PlaceholderValue itemSubTotalWithBreakdownValue;

    // Line Items - Totals - Due
    private PlaceholderValue itemDueDescription;
    private PlaceholderValue itemDueLabel;
    private PlaceholderValue itemDueValue;

    // Payment - Title
    private PlaceholderValue paymentTitleLabel;

    // Payment - Billing
    private PlaceholderValue paymentBillingLabel;
    private PlaceholderValue paymentBillingValue;

    // Acceptance - Title
    private PlaceholderValue acceptanceTitleDescription;
    private PlaceholderValue acceptanceTitleValue;

    // Acceptance - Method
    private PlaceholderValue acceptanceMethodValue;

    // Acceptance - Subtitles
    private PlaceholderValue acceptanceBuyerSubTitleValue;
    private PlaceholderValue acceptanceSellerSubTitleValue;

    // Acceptance - Buyer Contacts (Array)
    private Collection<AcceptanceBuyerContactPlaceholderValue> acceptanceBuyerContactPlaceholderValues;

    // Acceptance - Seller Contacts (Array)
    private Collection<AcceptanceSellerContactPlaceholderValue> acceptanceSellerContactPlaceholderValues;

    // Custom Section - Title
    private PlaceholderValue customSectionTitleDescription;
    private PlaceholderValue customSectionTitleValue;

    public QuotationPlaceholderDataBuilder setQuoteName(final PlaceholderValue quoteName) {
        this.quoteName = quoteName;
        return this;
    }

    public QuotationPlaceholderDataBuilder setLanguage(final PlaceholderValue language) {
        this.language = language;
        return this;
    }

    public QuotationPlaceholderDataBuilder setLocale(final PlaceholderValue locale) {
        this.locale = locale;
        return this;
    }

    public QuotationPlaceholderDataBuilder setCurrency(final PlaceholderValue currency) {
        this.currency = currency;
        return this;
    }

    public QuotationPlaceholderDataBuilder setQuoteUrl(final PlaceholderValue quoteUrl) {
        this.quoteUrl = quoteUrl;
        return this;
    }

    public QuotationPlaceholderDataBuilder setHeaderTitleValue(final PlaceholderValue headerTitleValue) {
        this.headerTitleValue = headerTitleValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setHeaderQuoteRefLabel(final PlaceholderValue headerQuoteRefLabel) {
        this.headerQuoteRefLabel = headerQuoteRefLabel;
        return this;
    }

    public QuotationPlaceholderDataBuilder setHeaderQuoteRefValue(final PlaceholderValue headerQuoteRefValue) {
        this.headerQuoteRefValue = headerQuoteRefValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setHeaderIssueDateLabel(final PlaceholderValue headerIssueDateLabel) {
        this.headerIssueDateLabel = headerIssueDateLabel;
        return this;
    }

    public QuotationPlaceholderDataBuilder setHeaderIssueDateValue(final PlaceholderValue headerIssueDateValue) {
        this.headerIssueDateValue = headerIssueDateValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setHeaderExpirationDateLabel(final PlaceholderValue headerExpirationDateLabel) {
        this.headerExpirationDateLabel = headerExpirationDateLabel;
        return this;
    }

    public QuotationPlaceholderDataBuilder setHeaderExpirationDateMinValue(final PlaceholderValue headerExpirationDateMinValue) {
        this.headerExpirationDateMinValue = headerExpirationDateMinValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setHeaderExpirationDateValue(final PlaceholderValue headerExpirationDateValue) {
        this.headerExpirationDateValue = headerExpirationDateValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setHeaderCurrencyLabel(final PlaceholderValue headerCurrencyLabel) {
        this.headerCurrencyLabel = headerCurrencyLabel;
        return this;
    }

    public QuotationPlaceholderDataBuilder setHeaderCurrencyValue(final PlaceholderValue headerCurrencyValue) {
        this.headerCurrencyValue = headerCurrencyValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setHeaderLogoValue(final PlaceholderValue headerLogoValue) {
        this.headerLogoValue = headerLogoValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setHeaderCompanyNameValue(final PlaceholderValue headerCompanyNameValue) {
        this.headerCompanyNameValue = headerCompanyNameValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setPartySellerTitleValue(final PlaceholderValue partySellerTitleValue) {
        this.partySellerTitleValue = partySellerTitleValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setPartySellerCompanyValue(final PlaceholderValue partySellerCompanyValue) {
        this.partySellerCompanyValue = partySellerCompanyValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setPartySellerCompanyNameValue(final PlaceholderValue partySellerCompanyNameValue) {
        this.partySellerCompanyNameValue = partySellerCompanyNameValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setPartySellerCompanyAddressValue(final PlaceholderValue partySellerCompanyAddressValue) {
        this.partySellerCompanyAddressValue = partySellerCompanyAddressValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setPartySellerContactLabel(final PlaceholderValue partySellerContactLabel) {
        this.partySellerContactLabel = partySellerContactLabel;
        return this;
    }

    public QuotationPlaceholderDataBuilder setPartySellerContactValue(final PlaceholderValue partySellerContactValue) {
        this.partySellerContactValue = partySellerContactValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setPartySellerContactNameValue(final PlaceholderValue partySellerContactNameValue) {
        this.partySellerContactNameValue = partySellerContactNameValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setPartySellerContactEmailValue(final PlaceholderValue partySellerContactEmailValue) {
        this.partySellerContactEmailValue = partySellerContactEmailValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setPartyBuyerTitleValue(final PlaceholderValue partyBuyerTitleValue) {
        this.partyBuyerTitleValue = partyBuyerTitleValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setPartyBuyerCompanyValue(final PlaceholderValue partyBuyerCompanyValue) {
        this.partyBuyerCompanyValue = partyBuyerCompanyValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setPartyBuyerCompanyNameValue(final PlaceholderValue partyBuyerCompanyNameValue) {
        this.partyBuyerCompanyNameValue = partyBuyerCompanyNameValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setPartyBuyerCompanyAddressValue(final PlaceholderValue partyBuyerCompanyAddressValue) {
        this.partyBuyerCompanyAddressValue = partyBuyerCompanyAddressValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setPartyBuyerContactLabel(final PlaceholderValue partyBuyerContactLabel) {
        this.partyBuyerContactLabel = partyBuyerContactLabel;
        return this;
    }

    public QuotationPlaceholderDataBuilder setPartyBuyerContacts(final List<PartyBuyerContactPlaceholderValue> partyBuyerContactPlaceholderValues) {
        this.partyBuyerContactPlaceholderValues = partyBuyerContactPlaceholderValues;
        return this;
    }

    public QuotationPlaceholderDataBuilder setSummaryEffectiveDateLabel(final PlaceholderValue summaryEffectiveDateLabel) {
        this.summaryEffectiveDateLabel = summaryEffectiveDateLabel;
        return this;
    }

    public QuotationPlaceholderDataBuilder setSummaryEffectiveDateDropdownValue(final PlaceholderValue summaryEffectiveDateDropdownValue) {
        this.summaryEffectiveDateDropdownValue = summaryEffectiveDateDropdownValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setSummaryEffectiveDateValue(final PlaceholderValue summaryEffectiveDateValue) {
        this.summaryEffectiveDateValue = summaryEffectiveDateValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setSummaryEffectiveDateFormHiddenValue(final PlaceholderValue summaryEffectiveDateFormHiddenValue) {
        this.summaryEffectiveDateFormHiddenValue = summaryEffectiveDateFormHiddenValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setSummaryEffectiveDateDefaultValue(final PlaceholderValue summaryEffectiveDateDefaultValue) {
        this.summaryEffectiveDateDefaultValue = summaryEffectiveDateDefaultValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setSummaryTermLengthLabel(final PlaceholderValue summaryTermLengthLabel) {
        this.summaryTermLengthLabel = summaryTermLengthLabel;
        return this;
    }

    public QuotationPlaceholderDataBuilder setSummaryTermLengthValue(final PlaceholderValue summaryTermLengthValue) {
        this.summaryTermLengthValue = summaryTermLengthValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setSummaryDiscountLabel(final PlaceholderValue summaryDiscountLabel) {
        this.summaryDiscountLabel = summaryDiscountLabel;
        return this;
    }

    public QuotationPlaceholderDataBuilder setSummaryDiscountFormat(final PlaceholderValue summaryDiscountFormat) {
        this.summaryDiscountFormat = summaryDiscountFormat;
        return this;
    }

    public QuotationPlaceholderDataBuilder setSummaryDiscountAmountValue(final PlaceholderValue summaryDiscountAmountValue) {
        this.summaryDiscountAmountValue = summaryDiscountAmountValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setSummaryDiscountPercentageValue(final PlaceholderValue summaryDiscountPercentageValue) {
        this.summaryDiscountPercentageValue = summaryDiscountPercentageValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setSummaryDiscountPercentageAndAmountValue(final PlaceholderValue summaryDiscountPercentageAndAmountValue) {
        this.summaryDiscountPercentageAndAmountValue = summaryDiscountPercentageAndAmountValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setSummaryValueLabel(final PlaceholderValue summaryValueLabel) {
        this.summaryValueLabel = summaryValueLabel;
        return this;
    }

    public QuotationPlaceholderDataBuilder setSummaryValue(final PlaceholderValue summaryValue) {
        this.summaryValue = summaryValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setLineItems(final List<LineItemPlaceholderValue> quotationLineItemPlaceholderValues) {
        this.lineItemPlaceholderValues = quotationLineItemPlaceholderValues;
        return this;
    }

    public QuotationPlaceholderDataBuilder setItemSubTotalLabel(final PlaceholderValue itemSubTotalLabel) {
        this.itemSubTotalLabel = itemSubTotalLabel;
        return this;
    }

    public QuotationPlaceholderDataBuilder setItemSubTotalFormat(final PlaceholderValue itemSubTotalFormat) {
        this.itemSubTotalFormat = itemSubTotalFormat;
        return this;
    }

    public QuotationPlaceholderDataBuilder setItemSubTotalValue(final PlaceholderValue itemSubTotalValue) {
        this.itemSubTotalValue = itemSubTotalValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setItemSubTotalWithBreakdownValue(final PlaceholderValue itemSubTotalWithBreakdownValue) {
        this.itemSubTotalWithBreakdownValue = itemSubTotalWithBreakdownValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setCurrencyCodeValue(final PlaceholderValue currencyCodeValue) {
        this.currencyCodeValue = currencyCodeValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setItemDiscountLabel(final PlaceholderValue itemDiscountLabel) {
        this.itemDiscountLabel = itemDiscountLabel;
        return this;
    }

    public QuotationPlaceholderDataBuilder setItemDiscountFormat(final PlaceholderValue itemDiscountFormat) {
        this.itemDiscountFormat = itemDiscountFormat;
        return this;
    }

    public QuotationPlaceholderDataBuilder setItemDiscountAmountValue(final PlaceholderValue itemDiscountAmountValue) {
        this.itemDiscountAmountValue = itemDiscountAmountValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setItemDiscountPercentageValue(final PlaceholderValue itemDiscountPercentageValue) {
        this.itemDiscountPercentageValue = itemDiscountPercentageValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setItemDiscountPercentageAndAmountValue(final PlaceholderValue itemDiscountPercentageAndAmountValue) {
        this.itemDiscountPercentageAndAmountValue = itemDiscountPercentageAndAmountValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setItemTaxLabel(final PlaceholderValue itemTaxLabel) {
        this.itemTaxLabel = itemTaxLabel;
        return this;
    }

    public QuotationPlaceholderDataBuilder setItemTaxValue(final PlaceholderValue itemTaxValue) {
        this.itemTaxValue = itemTaxValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setItemDueDescription(final PlaceholderValue itemDueDescription) {
        this.itemDueDescription = itemDueDescription;
        return this;
    }

    public QuotationPlaceholderDataBuilder setItemDueLabel(final PlaceholderValue itemDueLabel) {
        this.itemDueLabel = itemDueLabel;
        return this;
    }

    public QuotationPlaceholderDataBuilder setItemDueValue(final PlaceholderValue itemDueValue) {
        this.itemDueValue = itemDueValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setItemTotalValueLabel(final PlaceholderValue itemTotalValueLabel) {
        this.itemTotalValueLabel = itemTotalValueLabel;
        return this;
    }

    public QuotationPlaceholderDataBuilder setItemTotalValue(final PlaceholderValue itemTotalValue) {
        this.itemTotalValue = itemTotalValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setPaymentTitleLabel(final PlaceholderValue paymentTitleLabel) {
        this.paymentTitleLabel = paymentTitleLabel;
        return this;
    }

    public QuotationPlaceholderDataBuilder setPaymentBillingLabel(final PlaceholderValue paymentBillingLabel) {
        this.paymentBillingLabel = paymentBillingLabel;
        return this;
    }

    public QuotationPlaceholderDataBuilder setPaymentBillingValue(final PlaceholderValue paymentBillingValue) {
        this.paymentBillingValue = paymentBillingValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setAcceptanceTitleDescription(final PlaceholderValue acceptanceTitleDescription) {
        this.acceptanceTitleDescription = acceptanceTitleDescription;
        return this;
    }

    public QuotationPlaceholderDataBuilder setAcceptanceTitleValue(final PlaceholderValue acceptanceTitleValue) {
        this.acceptanceTitleValue = acceptanceTitleValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setAcceptanceMethodValue(final PlaceholderValue acceptanceMethodValue) {
        this.acceptanceMethodValue = acceptanceMethodValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setAcceptanceBuyerSubTitleValue(final PlaceholderValue acceptanceBuyerSubTitleValue) {
        this.acceptanceBuyerSubTitleValue = acceptanceBuyerSubTitleValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setAcceptanceSellerSubTitleValue(final PlaceholderValue acceptanceSellerSubTitleValue) {
        this.acceptanceSellerSubTitleValue = acceptanceSellerSubTitleValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setAcceptanceBuyerContacts(final List<AcceptanceBuyerContactPlaceholderValue> acceptanceBuyerContactPlaceholderValues) {
        this.acceptanceBuyerContactPlaceholderValues = acceptanceBuyerContactPlaceholderValues;
        return this;
    }

    public QuotationPlaceholderDataBuilder setAcceptanceSellerContacts(final List<AcceptanceSellerContactPlaceholderValue> acceptanceSellerContactPlaceholderValues) {
        this.acceptanceSellerContactPlaceholderValues = acceptanceSellerContactPlaceholderValues;
        return this;
    }

    public QuotationPlaceholderDataBuilder setCustomSectionTitleDescription(final PlaceholderValue customSectionTitleDescription) {
        this.customSectionTitleDescription = customSectionTitleDescription;
        return this;
    }

    public QuotationPlaceholderDataBuilder setCustomSectionTitleValue(final PlaceholderValue customSectionTitleValue) {
        this.customSectionTitleValue = customSectionTitleValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setSourceContactRenamedValue(final PlaceholderValue sourceContactRenamedValue) {
        this.sourceContactRenamedValue = sourceContactRenamedValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setSourceCompanyRenamedValue(final PlaceholderValue sourceCompanyRenamedValue) {
        this.sourceCompanyRenamedValue = sourceCompanyRenamedValue;
        return this;
    }

    public QuotationPlaceholderDataBuilder setSourceProductRenamedValue(final PlaceholderValue sourceProductRenamedValue) {
        this.sourceProductRenamedValue = sourceProductRenamedValue;
        return this;
    }

    /**
     * Builds and returns the {@link PlaceholderKey.Provider} used to resolve
     * raw key strings back into typed {@link PlaceholderKey} constants.
     *
     * @return a non-null {@link PlaceholderKey.Provider} for this placeholder domain
     */
    @Override
    protected PlaceholderKey.Provider buildPlaceHolderKeyProvider() {
        return new QuotationPlaceholderKey.Provider();
    }

    /**
     * Builds and returns scalar placeholder values.
     *
     * @return a {@link PlaceholderEntries} containing placeholder keys and their corresponding values
     */
    @Override
    protected PlaceholderEntries buildScalarValues() {
        final PlaceholderEntries scalars = new PlaceholderEntries();

        scalars.addEntry(QUOTE_NAME, quoteName);
        scalars.addEntry(LANGUAGE, language);
        scalars.addEntry(LOCALE, locale);
        scalars.addEntry(CURRENCY, currency);
        scalars.addEntry(QUOTE_URL, quoteUrl);

        scalars.addEntry(HEADER_TITLE_VALUE, headerTitleValue);
        scalars.addEntry(HEADER_QUOTE_REF_LABEL, headerQuoteRefLabel);
        scalars.addEntry(HEADER_QUOTE_REF_VALUE, headerQuoteRefValue);
        scalars.addEntry(HEADER_ISSUE_DATE_LABEL, headerIssueDateLabel);
        scalars.addEntry(HEADER_ISSUE_DATE_VALUE, headerIssueDateValue);
        scalars.addEntry(HEADER_EXPIRATION_DATE_LABEL, headerExpirationDateLabel);
        scalars.addEntry(HEADER_EXPIRATION_DATE_MIN_VALUE, headerExpirationDateMinValue);
        scalars.addEntry(HEADER_EXPIRATION_DATE_VALUE, headerExpirationDateValue);
        scalars.addEntry(HEADER_CURRENCY_LABEL, headerCurrencyLabel);
        scalars.addEntry(HEADER_CURRENCY_VALUE, headerCurrencyValue);
        scalars.addEntry(HEADER_LOGO_VALUE, headerLogoValue);
        scalars.addEntry(HEADER_COMPANY_NAME_VALUE, headerCompanyNameValue);

        scalars.addEntry(PARTY_SELLER_TITLE_VALUE, partySellerTitleValue);
        scalars.addEntry(PARTY_SELLER_COMPANY_VALUE, partySellerCompanyValue);
        scalars.addEntry(PARTY_SELLER_COMPANY_NAME_VALUE, partySellerCompanyNameValue);
        scalars.addEntry(PARTY_SELLER_COMPANY_ADDRESS_VALUE, partySellerCompanyAddressValue);
        scalars.addEntry(PARTY_SELLER_CONTACT_LABEL, partySellerContactLabel);
        scalars.addEntry(PARTY_SELLER_CONTACT_VALUE, partySellerContactValue);
        scalars.addEntry(PARTY_SELLER_CONTACT_NAME_VALUE, partySellerContactNameValue);
        scalars.addEntry(PARTY_SELLER_CONTACT_EMAIL_VALUE, partySellerContactEmailValue);

        scalars.addEntry(PARTY_BUYER_TITLE_VALUE, partyBuyerTitleValue);
        scalars.addEntry(PARTY_BUYER_COMPANY_VALUE, partyBuyerCompanyValue);
        scalars.addEntry(PARTY_BUYER_COMPANY_NAME_VALUE, partyBuyerCompanyNameValue);
        scalars.addEntry(PARTY_BUYER_COMPANY_ADDRESS_VALUE, partyBuyerCompanyAddressValue);
        scalars.addEntry(PARTY_BUYER_CONTACT_LABEL, partyBuyerContactLabel);

        scalars.addEntry(SUMMARY_EFFECTIVE_DATE_LABEL, summaryEffectiveDateLabel);
        scalars.addEntry(SUMMARY_EFFECTIVE_DATE_DROPDOWN_VALUE, summaryEffectiveDateDropdownValue);
        scalars.addEntry(SUMMARY_EFFECTIVE_DATE_VALUE, summaryEffectiveDateValue);
        scalars.addEntry(SUMMARY_EFFECTIVE_DATE_FORM_HIDDEN_VALUE, summaryEffectiveDateFormHiddenValue);
        scalars.addEntry(SUMMARY_EFFECTIVE_DATE_DEFAULT_VALUE, summaryEffectiveDateDefaultValue);
        scalars.addEntry(SUMMARY_TERM_LENGTH_LABEL, summaryTermLengthLabel);
        scalars.addEntry(SUMMARY_TERM_LENGTH_VALUE, summaryTermLengthValue);
        scalars.addEntry(SUMMARY_DISCOUNT_LABEL, summaryDiscountLabel);
        scalars.addEntry(SUMMARY_DISCOUNT_FORMAT, summaryDiscountFormat);
        scalars.addEntry(SUMMARY_DISCOUNT_AMOUNT_VALUE, summaryDiscountAmountValue);
        scalars.addEntry(SUMMARY_DISCOUNT_PERCENTAGE_VALUE, summaryDiscountPercentageValue);
        scalars.addEntry(SUMMARY_DISCOUNT_PERCENTAGE_AND_AMOUNT_VALUE, summaryDiscountPercentageAndAmountValue);
        scalars.addEntry(SUMMARY_DISCOUNT_CURRENCY_VALUE, currencyCodeValue);
        scalars.addEntry(SUMMARY_VALUE_LABEL, summaryValueLabel);
        scalars.addEntry(SUMMARY_VALUE, summaryValue);
        scalars.addEntry(SUMMARY_VALUE_CURRENCY, currencyCodeValue);

        scalars.addEntry(ITEM_SUB_TOTAL_LABEL, itemSubTotalLabel);
        scalars.addEntry(ITEM_SUB_TOTAL_FORMAT, itemSubTotalFormat);
        scalars.addEntry(ITEM_SUB_TOTAL_VALUE, itemSubTotalValue);
        scalars.addEntry(ITEM_SUB_TOTAL_WITH_BREAKDOWN_VALUE, itemSubTotalWithBreakdownValue);
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
        scalars.addEntry(ITEM_DUE_DESCRIPTION, itemDueDescription);
        scalars.addEntry(ITEM_DUE_LABEL, itemDueLabel);
        scalars.addEntry(ITEM_DUE_VALUE, itemDueValue);
        scalars.addEntry(ITEM_DUE_CURRENCY_VALUE, currencyCodeValue);
        scalars.addEntry(ITEM_TOTAL_VALUE_LABEL, itemTotalValueLabel);
        scalars.addEntry(ITEM_TOTAL_VALUE, itemTotalValue);
        scalars.addEntry(ITEM_TOTAL_VALUE_CURRENCY, currencyCodeValue);

        scalars.addEntry(PAYMENT_TITLE_LABEL, paymentTitleLabel);
        scalars.addEntry(PAYMENT_BILLING_LABEL, paymentBillingLabel);
        scalars.addEntry(PAYMENT_BILLING_VALUE, paymentBillingValue);

        scalars.addEntry(ACCEPTANCE_TITLE_DESCRIPTION, acceptanceTitleDescription);
        scalars.addEntry(ACCEPTANCE_TITLE_VALUE, acceptanceTitleValue);
        scalars.addEntry(ACCEPTANCE_METHOD_VALUE, acceptanceMethodValue);
        scalars.addEntry(ACCEPTANCE_BUYER_SUB_TITLE_VALUE, acceptanceBuyerSubTitleValue);
        scalars.addEntry(ACCEPTANCE_SELLER_SUB_TITLE_VALUE, acceptanceSellerSubTitleValue);

        scalars.addEntry(CUSTOM_SECTION_TITLE_DESCRIPTION, customSectionTitleDescription);
        scalars.addEntry(CUSTOM_SECTION_TITLE_VALUE, customSectionTitleValue);

        scalars.addEntry(SOURCE_CONTACT, sourceContactRenamedValue);
        scalars.addEntry(SOURCE_COMPANY, sourceCompanyRenamedValue);
        scalars.addEntry(SOURCE_PRODUCT, sourceProductRenamedValue);

        return scalars;
    }

    /**
     * Builds and returns array-based placeholder values.
     *
     * @return an {@link ArrayPlaceholderEntries} containing placeholder keys and their corresponding rows
     */
    @Override
    protected ArrayPlaceholderEntries buildArrayValues() {
        final ArrayPlaceholderEntries arrays = new ArrayPlaceholderEntries();

        addRowsIfNonNull(arrays, PARTY_BUYER_CONTACTS, partyBuyerContactPlaceholderValues);
        addRowsIfNonNull(arrays, LINE_ITEMS, lineItemPlaceholderValues);
        addRowsIfNonNull(arrays, ACCEPTANCE_BUYER_CONTACTS, acceptanceBuyerContactPlaceholderValues);
        addRowsIfNonNull(arrays, ACCEPTANCE_SELLER_CONTACTS, acceptanceSellerContactPlaceholderValues);

        return arrays;
    }

    /**
     * Adds rows from a {@link Collection} of {@link ArrayTypePlaceholderValue} POJOs to the given
     * {@link ArrayPlaceholderEntries}.  Each POJO is converted to a {@link PlaceholderEntries}
     * row via its {@link ArrayTypePlaceholderValue#toMap()} method.  If {@code rows} is {@code null}
     * the method is a no-op.
     *
     * @param arrays         the target array-placeholder container
     * @param placeholderKey the array-level placeholder key
     * @param rows           the POJO collection (may be {@code null})
     * @param <T>            a concrete {@link ArrayTypePlaceholderValue} type
     */
    private static <T extends ArrayTypePlaceholderValue> void addRowsIfNonNull(final ArrayPlaceholderEntries arrays,
                                                                               final PlaceholderKey placeholderKey,
                                                                               final Collection<T> rows) {
        if (Objects.nonNull(rows)) {
            for (final T item : rows) {
                final PlaceholderEntries placeholderEntries = new PlaceholderEntries();

                item.toMap().forEach(placeholderEntries::addEntry);

                arrays.addEntry(placeholderKey, placeholderEntries);
            }
        }
    }
}
