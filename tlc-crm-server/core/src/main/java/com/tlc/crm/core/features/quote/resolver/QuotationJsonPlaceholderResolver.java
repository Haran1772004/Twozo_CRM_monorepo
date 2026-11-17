package com.tlc.crm.core.features.quote.resolver;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.json.resolver.model.PlaceholderValue;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;
import com.tlc.crm.common.models.Language;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.DealValue;
import com.tlc.crm.core.api.deal.model.Participant;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.features.sales.template.resolver.AbstractSalesDocumentJsonPlaceholderResolver;
import com.tlc.crm.core.features.sales.template.model.LineItemPlaceholderValue;
import com.tlc.crm.core.features.quote.builder.QuotationPlaceholderDataBuilder;
import com.tlc.crm.core.features.quote.fields.QuotationFields;
import com.tlc.crm.core.features.quote.model.AcceptanceBuyerContactPlaceholderValue;
import com.tlc.crm.core.features.quote.model.AcceptanceSellerContactPlaceholderValue;
import com.tlc.crm.core.features.quote.model.PartyBuyerContactPlaceholderValue;
import com.tlc.crm.core.features.quote.model.Quotation;
import com.tlc.crm.core.features.quote.model.QuotationLineItemPlaceholderValue;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.iam.common.models.Organization;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.service.TemporalService;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * <p>
 * Loads placeholder data from a {@link Deal} and resolves it against a quotation
 * template {@link JsonObject}.
 * </p>
 *
 * <p>The following sections are populated from the deal:</p>
 * <ul>
 *   <li><b>Header</b> — Quote reference, issue date, expiration date, currency, and seller company name</li>
 *   <li><b>Party — Buyer</b> — Company name, address, and contact list (primary contact + participants)</li>
 *   <li><b>Party — Seller</b> — Company name and sales-owner contact details</li>
 *   <li><b>Acceptance — Buyer</b> — Contacts eligible to sign, derived from the buyer contact list</li>
 *   <li><b>Acceptance — Seller</b> — Sales owner as the accepting seller contact</li>
 *   <li><b>Line Items</b> — Per-product name, quantity, unit price, discount percentage, and line total</li>
 *   <li><b>Totals</b> — Subtotal, discount, tax, amount due, and total contract value</li>
 *   <li><b>Settings</b> — Currency code, language, and locale</li>
 * </ul>
 *
 * @author Sathish
 * @version 1.0
 */
public final class QuotationJsonPlaceholderResolver extends AbstractSalesDocumentJsonPlaceholderResolver<QuotationPlaceholderDataBuilder, Quotation> {

    private static final String QUOTE_REF_FORMAT = "%s-%d-%d";
    private static final String SUBTOTAL_BREAKDOWN_FORMAT = "%s%,.2f/%s";
    private static final String BILLING_TERM_ONE_TIME = "one time";
    private static final int MAX_BUYER_CONTACTS = 10;

    private final DateTimeFormatter dateFormat;

    public QuotationJsonPlaceholderResolver(final long orgId) {
        super(orgId);
        this.builder = new QuotationPlaceholderDataBuilder();
        this.dateFormat = new DateTimeFormatterBuilder().appendPattern("yyyyMMdd").toFormatter();
    }

    @Override
    public JsonObject resolve(final JsonObject templateJson, final Quotation quotation) {
        final int quotationNumber = quotation.getNumber();
        final Deal deal = quotation.getDeal();
        final CrmUser salesOwner = deal.getSalesOwner();
        final CrmUser crmUser = LoggedInUserInfo.getUser();
        final TemporalService currentDate = TemporalServiceProvider.get();
        final UserRegionalPreference userPreference = crmUser.getPreferenceHolder().get(UserPreferenceType.USER_REGIONAL_PREFERENCE);
        final TimezoneInfo timezoneInfo = userPreference.getTimezoneInfo();
        final Language language = userPreference.getLanguage();
        final Locale locale = Locale.forLanguageTag(language.getLanguageTag());
        final String issueDateString = currentDate.getZonedDate(timezoneInfo).format(dateFormat);

        builder.setQuoteName(PlaceholderValue.of(deal.getTitle()))
                .setHeaderQuoteRefValue(PlaceholderValue.of(String.format(QUOTE_REF_FORMAT, issueDateString, deal.id(), quotationNumber)))
                .setHeaderIssueDateValue(PlaceholderValue.of(currentDate.getUtcDateTime()))
                .setHeaderExpirationDateValue(PlaceholderValue.of(currentDate.getUtcDateTime()))
                .setHeaderExpirationDateMinValue(PlaceholderValue.of(TemporalServiceProvider.get().getStartTimeOfDay(timezoneInfo)))
                .setSummaryEffectiveDateDefaultValue(PlaceholderValue.of(currentDate.getUtcDateTime()))
                .setLanguage(PlaceholderValue.of(Json.object().put(QuotationFields.ID, language.getId()).put(QuotationFields.NAME, language.getName())))
                .setLocale(PlaceholderValue.of(locale.getCountry()));

        loadBuyerCompanyPlaceholders(deal.getCompany());
        loadBuyerContactPlaceholders(deal.getPrimaryContact(), deal.getParticipants());
        loadSellerContactPlaceholders(salesOwner);
        loadSellerCompanyPlaceholders(salesOwner.getOrganization());
        loadLineItemPlaceholders(deal.getProductDetailsHolder());
        loadCurrencyPlaceholder(deal.getDealValue());

        return resolveTemplateJson(templateJson, builder.build());
    }

    /**
     * <p>
     * Creates the invoice-specific placeholder values for the given product detail.
     * </p>
     *
     * @param productDetail Represents the product detail used to populate the line item
     * @return the invoice line-item placeholder value
     */
    @Override
    protected LineItemPlaceholderValue getLineItemPlaceHolderValue(final ProductDetail productDetail) {
        return new QuotationLineItemPlaceholderValue();
    }

    /**
     * Loads the deal's currency code into the builder.
     * Sets both the settings-level {@code currency} placeholder and the header
     * {@code currencyValue} placeholder using {@link Currency#getCode()}.
     * Has no effect if {@code dealValue} or its currency is {@code null}.
     *
     * @param dealValue the holder from which the currency is read
     */
    private void loadCurrencyPlaceholder(final DealValue dealValue) {
        if (Objects.isNull(dealValue)) {
            return;
        }

        final Currency currency = dealValue.getCurrency();

        if (Objects.nonNull(currency)) {
            final String currencyCode = currency.getCode();

            builder.setCurrency(PlaceholderValue.of(Json.object().put(QuotationFields.ID, currency.getId()).put(QuotationFields.CODE, currencyCode)))
                    .setHeaderCurrencyValue(PlaceholderValue.of(currencyCode))
                    .setCurrencyCodeValue(PlaceholderValue.of(resolveCurrencySymbol(currency)));
        }
    }

    /**
     * Loads the seller's organisation name into the party seller company and
     * header company name placeholders.
     * <p>
     * Address is not populated because the {@link Organization} model does not
     * carry address information.
     * </p>
     * Has no effect if {@code organization} or its name is {@code null}.
     *
     * @param organization the seller's organisation
     */
    private void loadSellerCompanyPlaceholders(final Organization organization) {
        if (Objects.isNull(organization)) {
            return;
        }

        final String name = organization.getName();

        if (Objects.nonNull(name)) {
            builder.setPartySellerCompanyNameValue(PlaceholderValue.of(name))
                    .setHeaderCompanyNameValue(PlaceholderValue.of(name))
                    .setPartySellerCompanyValue(PlaceholderValue.of(convertOrganizationIntoJson(organization)));
        }
    }

    /**
     * Converts the given {@link Organization} into a {@link JsonObject}
     * containing the organisation's display attributes.
     *
     * @param organization the organisation to convert
     * @return a {@link JsonObject} representing the organisation
     */
    private JsonObject convertOrganizationIntoJson(final Organization organization) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(QuotationFields.NAME, organization.getName());

        return jsonObject;
    }

    /**
     * Loads buyer contacts into the party and acceptance sections of the builder.
     * <p>
     * Contacts are collected from the primary contact first, followed by any
     * additional contacts from the participant list. In the party section, the
     * primary contact is flagged as the signer; in the acceptance section, only
     * the primary contact is marked as enabled. Each contact entry includes the
     * full name, primary email, and profile picture icon.
     * </p>
     * Has no effect if the resolved contact collection is empty.
     *
     * @param primaryContact the deal's primary contact
     * @param participants   the deal's participant list
     */
    private void loadBuyerContactPlaceholders(final Contact primaryContact, final Collection<Participant> participants) {
        final Collection<Contact> contacts = collectBuyerContacts(primaryContact, participants);

        if (contacts.isEmpty()) {
            return;
        }

        final List<PartyBuyerContactPlaceholderValue> partyBuyerContacts = new ArrayList<>();
        final List<AcceptanceBuyerContactPlaceholderValue> acceptanceBuyerContacts = new ArrayList<>();
        final Long primaryContactId = primaryContact.id();

        for (final Contact contact : contacts) {
            if (Objects.isNull(contact)) {
                continue;
            }

            final String fullName = contact.getFullName();
            final String email = getContactPrimaryEmailValue(contact);
            final boolean isPrimaryContact = Objects.equals(primaryContactId, contact.id());
            final String icon = buildContactProfileUrl(contact);

            partyBuyerContacts.add(new PartyBuyerContactPlaceholderValue()
                    .setContactIdValue(PlaceholderValue.of(contact.id()))
                    .setContactValue(PlaceholderValue.of(fullName))
                    .setContactEmailValue(PlaceholderValue.of(email))
                    .setContactIconValue(PlaceholderValue.of(icon))
                    .setContactSignerValue(PlaceholderValue.of(isPrimaryContact))
                    .setEnabled(PlaceholderValue.of(true)));

            acceptanceBuyerContacts.add(new AcceptanceBuyerContactPlaceholderValue()
                    .setContactIdValue(PlaceholderValue.of(contact.id()))
                    .setContactValue(PlaceholderValue.of(fullName))
                    .setContactEmailValue(PlaceholderValue.of(email))
                    .setContactIconValue(PlaceholderValue.of(icon))
                    .setContactSignerValue(PlaceholderValue.of(isPrimaryContact)));
        }

        builder.setPartyBuyerContacts(partyBuyerContacts)
                .setAcceptanceBuyerContacts(acceptanceBuyerContacts);
    }

    /**
     * Loads the deal's sales owner as the seller contact into the party and
     * acceptance sections of the builder.
     * <p>
     * Sets the seller's full name, email address, and profile picture icon in
     * both the party-level scalar fields and as a single-entry acceptance contact.
     * </p>
     * Has no effect if {@code salesOwner} is {@code null}.
     *
     * @param salesOwner the sales owner (seller contact) of the deal
     */
    private void loadSellerContactPlaceholders(final CrmUser salesOwner) {
        if (Objects.isNull(salesOwner)) {
            return;
        }

        final String fullName = salesOwner.getFullName();
        final String email = salesOwner.getEmail();
        final String icon = buildUserProfileUrl(salesOwner);

        builder.setPartySellerContactNameValue(PlaceholderValue.of(fullName))
                .setPartySellerContactEmailValue(PlaceholderValue.of(email))
                .setPartySellerContactValue(PlaceholderValue.of(convertUserIntoJson(salesOwner)))
                .setAcceptanceSellerContacts(List.of(new AcceptanceSellerContactPlaceholderValue()
                        .setUserIdValue(PlaceholderValue.of(salesOwner.id()))
                        .setContactValue(PlaceholderValue.of(fullName))
                        .setContactEmailValue(PlaceholderValue.of(email))
                        .setContactIconValue(PlaceholderValue.of(icon))));
    }

    /**
     * Computes and sets aggregate totals from the given {@link ProductDetailsHolder}:
     * subtotal, total discount amount, discount percentage, combined discount display,
     * tax, amount due, and total contract value.
     *
     * @param productDetailsHolder the holder from which totals are derived
     */
    @Override
    protected void loadLineItemTotals(final ProductDetailsHolder productDetailsHolder) {
        final double subTotal = productDetailsHolder.getSubTotal(productDetailsHolder);
        final double taxTotal = productDetailsHolder.getTaxTotal(productDetailsHolder);
        final double totalAmount = productDetailsHolder.getTotalAmount(productDetailsHolder);
        final double discountTotal = computeTotalDiscount(productDetailsHolder);
        final double discountPercentage = computeTotalDiscountPercentage(productDetailsHolder);
        final PlaceholderValue discountValue = PlaceholderValue.of(discountTotal);
        final PlaceholderValue discountPercentageValue = PlaceholderValue.of(String.format(PERCENTAGE_FORMAT, discountPercentage));
        final PlaceholderValue discountPercentageAndAmountValue = PlaceholderValue.of(
                String.format(DISCOUNT_PERCENTAGE_AND_AMOUNT_FORMAT, discountTotal, discountPercentage));
        final String currencySymbol = resolveCurrencySymbol(productDetailsHolder.getCurrency());

        builder.setItemSubTotalValue(PlaceholderValue.of(subTotal))
                .setItemSubTotalWithBreakdownValue(PlaceholderValue.of(
                        String.format(SUBTOTAL_BREAKDOWN_FORMAT, currencySymbol, subTotal, BILLING_TERM_ONE_TIME)))
                .setItemDiscountAmountValue(discountValue)
                .setItemDiscountPercentageValue(discountPercentageValue)
                .setItemDiscountPercentageAndAmountValue(discountPercentageAndAmountValue)
                .setItemTaxValue(PlaceholderValue.of(taxTotal))
                .setItemDueValue(PlaceholderValue.of(totalAmount))
                .setItemTotalValue(PlaceholderValue.of(totalAmount))
                .setSummaryDiscountAmountValue(discountValue)
                .setSummaryDiscountPercentageValue(discountPercentageValue)
                .setSummaryDiscountPercentageAndAmountValue(discountPercentageAndAmountValue)
                .setSummaryValue(PlaceholderValue.of(totalAmount));
    }

    /**
     * Collects all buyer contacts — the primary contact first, followed by any
     * additional contacts resolved from the participant list.
     * {@code null} participants or participant contacts are silently skipped.
     *
     * @param primaryContact the deal's primary contact; may be {@code null}
     * @param participants   the deal's participant list
     * @return an ordered collection of non-null buyer contacts
     */
    private Collection<Contact> collectBuyerContacts(final Contact primaryContact, final Collection<Participant> participants) {
        final Collection<Contact> contacts = new ArrayList<>();

        if (Objects.nonNull(primaryContact)) {
            contacts.add(primaryContact);
        }

        participants.stream().map(Participant::getContact).filter(Objects::nonNull)
                .limit(MAX_BUYER_CONTACTS - 1).forEach(contacts::add);

        return contacts;
    }

    /**
     * Returns the primary email address value of the given contact,
     * or {@code null} if no primary email is set.
     *
     * @param contact the contact whose primary email is requested
     * @return the email value string, or {@code null}
     */
    private String getContactPrimaryEmailValue(final Contact contact) {
        final Email primaryEmail = contact.getPrimaryEmail();

        return Objects.nonNull(primaryEmail) ? primaryEmail.getValue() : null;
    }
}
