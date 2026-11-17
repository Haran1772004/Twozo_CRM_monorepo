package com.tlc.crm.core.features.invoice.resolver;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.json.resolver.model.PlaceholderValue;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;
import com.tlc.crm.common.models.Language;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.features.sales.template.resolver.AbstractSalesDocumentJsonPlaceholderResolver;
import com.tlc.crm.core.features.invoice.builder.InvoicePlaceholderDataBuilder;
import com.tlc.crm.core.features.invoice.fields.InvoiceFields;
import com.tlc.crm.core.features.invoice.model.Invoice;
import com.tlc.crm.core.features.invoice.model.InvoiceLineItemPlaceholderValue;
import com.tlc.crm.core.features.sales.template.model.LineItemPlaceholderValue;
import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.entity.currency.api.manager.BaseCurrencyManager;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.iam.common.models.Organization;
import com.tlc.l10n.model.country.Country;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.service.TemporalService;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Resolves the invoice template JSON using the placeholder data model
 * defined for the invoice schema.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class InvoiceJsonPlaceholderResolver extends AbstractSalesDocumentJsonPlaceholderResolver<InvoicePlaceholderDataBuilder, Invoice> {

    private final BaseCurrencyManager baseCurrencyManager;

    /**
     * <p>
     * Creates an invoice JSON placeholder resolver for the given organization.
     * </p>
     *
     * @param orgId Represents the organization id.
     */
    public InvoiceJsonPlaceholderResolver(final long orgId) {
        super(orgId);
        this.builder = new InvoicePlaceholderDataBuilder();
        this.baseCurrencyManager = BaseCurrencyManager.getInstance();
    }

    /**
     * <p>
     * Creates the invoice-specific line item placeholder value for the supplied
     * product detail and sets its tax value.
     * </p>
     *
     * @param productDetail Represents the product detail associated with the line item.
     * @return The invoice line item placeholder value.
     */
    @Override
    protected LineItemPlaceholderValue getLineItemPlaceHolderValue(final ProductDetail productDetail) {
        final InvoiceLineItemPlaceholderValue lineItemPlaceholderValue = new InvoiceLineItemPlaceholderValue();

        lineItemPlaceholderValue.setItemTax(PlaceholderValue.of(productDetail.getTax()));
        lineItemPlaceholderValue.setItemValue(PlaceholderValue.of(productDetail.getProduct().id()));

        return lineItemPlaceholderValue;
    }

    /**
     * <p>
     * Resolves the supplied invoice template JSON using the invoice data.
     * </p>
     *
     * @param templateJson Represents the invoice template JSON.
     * @param invoice      Represents the invoice used to populate the placeholders.
     * @return The resolved invoice template JSON.
     */
    @Override
    public JsonObject resolve(final JsonObject templateJson, final Invoice invoice) {
        final CrmUser crmUser = LoggedInUserInfo.getUser();
        final TemporalService currentDate = TemporalServiceProvider.get();
        final UserRegionalPreference userPreference = crmUser.getPreferenceHolder().get(UserPreferenceType.USER_REGIONAL_PREFERENCE);
        final Language language = userPreference.getLanguage();
        final Locale locale = Locale.forLanguageTag(language.getLanguageTag());
        final PlaceholderValue currentUtcPlaceHolder = PlaceholderValue.of(currentDate.getUtcDateTime());
        final Currency currency = baseCurrencyManager.getBaseCurrency(orgId).getDefaultCurrency();

        builder.setLanguage(PlaceholderValue.of(Json.object()
                        .put(InvoiceFields.ID, language.getId())
                        .put(InvoiceFields.NAME, language.getName())))
                .setLocale(PlaceholderValue.of(locale.getCountry()))
                .setHeaderInvoiceDateValue(currentUtcPlaceHolder)
                .setHeaderDueDateValue(currentUtcPlaceHolder)
                .setCurrency(PlaceholderValue.of(Json.object().put(InvoiceFields.ID, currency.getId())
                        .put(InvoiceFields.CODE, currency.getCode())));
        final Deal deal = invoice.getDeal();

        loadOrganizationData(invoice.getOwner().getOrganization());
        loadBuyerCompanyPlaceholders(invoice.getCompany());
        loadSellerContactPlaceholders(invoice.getContact(), invoice.getCompany());

        if (Objects.nonNull(deal)) {
            final ProductDetailsHolder productDetailsHolder = deal.getProductDetailsHolder();

            if (Objects.nonNull(productDetailsHolder) && Objects.equals(productDetailsHolder.getCurrency(), currency)) {
                loadLineItemPlaceholders(productDetailsHolder);
            }
        }

        return resolveTemplateJson(templateJson, builder.build());
    }

    /**
     * <p>
     * Loads the organization name into the invoice header company name
     * placeholder.
     * </p>
     *
     * @param organization Represents the organization associated with the invoice.
     */
    private void loadOrganizationData(final Organization organization) {
        final String name = organization.getName();

        if (Objects.nonNull(name)) {
            builder.setHeaderCompanyNameValue(PlaceholderValue.of(name));
        }
    }

    /**
     * <p>
     * Loads the buyer company name and, when present, its formatted postal address
     * into the party buyer company placeholders. For invoices the buyer company
     * JSON will include a structured address object to be used by invoice templates.
     * </p>
     *
     * @param company Represents the buyer company associated with the sales document.
     */
    @Override
    protected void loadBuyerCompanyPlaceholders(final Company company) {
        if (Objects.isNull(company)) {
            return;
        }
        final String companyName = company.getName();
        final Address address = company.getAddress();

        builder.setPartyBuyerCompanyNameValue(PlaceholderValue.of(companyName));

        if (Objects.nonNull(address)) {
            builder.setPartyBuyerCompanyAddressValue(PlaceholderValue.of(formatAddress(address)));
        }
        builder.setPartyBuyerCompanyValue(PlaceholderValue.of(buildBuyerCompanyValueJson(company)));
    }

    /**
     * Builds the buyer company JSON used for {@code PARTY_BUYER_COMPANY_VALUE} and nested
     * under {@code PARTY_BUYER_CONTACT_VALUE}.
     *
     * @param company Represents the buyer company.
     * @return Company JSON, or {@code null} when company is null.
     */
    private JsonObject buildBuyerCompanyValueJson(final Company company) {
        if (Objects.isNull(company)) {
            return null;
        }
        final String companyName = company.getName();
        final JsonObject jsonObject = Json.object()
                .put(InvoiceFields.VALUE, company.id())
                .put(InvoiceFields.NAME, companyName);
        final Address address = company.getAddress();

        if (Objects.nonNull(address)) {
            final Country country = address.getCountry();
            final JsonObject countryJson = Json.object();

            if (Objects.nonNull(country)) {
                countryJson.put(InvoiceFields.NAME, country.getName());
                countryJson.put(InvoiceFields.VALUE, country.getId());
            }

            jsonObject.put(CompanyFields.ADDRESS, Json.object()
                    .put(StoreSubType.ADDRESS_LINE1.getAliasName(), address.getAddressLine1())
                    .put(StoreSubType.ADDRESS_LINE2.getAliasName(), address.getAddressLine2())
                    .put(StoreSubType.CITY.getAliasName(), address.getCity())
                    .put(StoreSubType.STATE.getAliasName(), address.getState())
                    .put(StoreSubType.COUNTRY.getAliasName(), countryJson)
                    .put(StoreSubType.ZIPCODE.getAliasName(), address.getZipcode()));
        }

        return jsonObject;
    }

    /**
     * <p>
     * Loads the seller contact's name, email address, and contact details
     * into the corresponding invoice placeholders.
     * </p>
     *
     * @param contact        Represents the buyer contact associated with the invoice.
     * @param invoiceCompany Represents the buyer company on the invoice when it is not on the contact.
     */
    private void loadSellerContactPlaceholders(final Contact contact, final Company invoiceCompany) {
        if (Objects.isNull(contact)) {
            return;
        }
        final String fullName = contact.getFullName();
        final Email email = Optional.ofNullable(contact.getEmails())
                .flatMap(emails -> emails.stream().findFirst())
                .orElse(null);
        final JsonObject contactJson = convertContactIntoJson(contact);
        final Company company = Objects.isNull(invoiceCompany) ? contact.getCompany() : invoiceCompany;
        final JsonObject companyJson = buildBuyerCompanyValueJson(company);

        contactJson.put(InvoiceFields.NAME, contact.getFullName());
        contactJson.put(InvoiceFields.EMAIL, Optional.ofNullable(contact.getPrimaryEmail()).map(Email::getValue).orElse(null));

        if (Objects.nonNull(companyJson)) {
            contactJson.put(InvoiceFields.COMPANY, companyJson);
        }

        builder.setPartyBuyerContactNameValue(PlaceholderValue.of(fullName))
                .setPartyBuyerContactValue(PlaceholderValue.of(contactJson));

        if (Objects.nonNull(email)) {
            builder.setPartyBuyerContactEmailValue(
                    PlaceholderValue.of(email.getValue()));
        }
    }
}