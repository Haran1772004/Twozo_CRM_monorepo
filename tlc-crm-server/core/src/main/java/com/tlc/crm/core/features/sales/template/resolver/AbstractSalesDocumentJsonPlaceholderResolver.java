package com.tlc.crm.core.features.sales.template.resolver;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.json.resolver.JsonPlaceholderResolver;
import com.tlc.commons.json.resolver.model.PlaceholderData;
import com.tlc.commons.json.resolver.model.PlaceholderValue;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.StringUtil;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.features.sales.template.model.AbstractSalesDocumentPlaceHolderDataBuilder;
import com.tlc.crm.core.features.invoice.fields.InvoiceFields;
import com.tlc.crm.core.features.sales.template.model.LineItemPlaceholderValue;
import com.tlc.crm.core.features.quote.fields.QuotationFields;
import com.tlc.crm.entity.fields.displayPicture.DisplayPictureFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.models.settings.SourceSettings;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.fields.service.settings.SourceSettingsService;
import com.tlc.iam.common.fields.ProfilePictureFields;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.web.HttpUtil;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides common placeholder resolution behavior for sales document templates.
 * </p>
 *
 * @param <T> Represents the concrete sales document data builder type.
 * @param <R> Represents the sales document model type.
 * @author Mohamed Yasar K
 * @version 1.0
 */
public abstract class AbstractSalesDocumentJsonPlaceholderResolver<T extends AbstractSalesDocumentPlaceHolderDataBuilder<T>, R> {

    protected static final String PROFILE_PIC_URL_FORMAT = "%s%s%s%s%s%s";
    protected static final String DISCOUNT_PERCENTAGE_AND_AMOUNT_FORMAT = "%s (%s%%)";
    protected static final String PERCENTAGE_FORMAT = "%s%%";

    protected T builder;
    protected final long orgId;
    private final String sourceProfilePicRedirectUrl;
    private final String userProfilePicRedirectUrl;
    protected boolean hasLineItems;
    private final FieldSourceService fieldSourceService;
    private final SourceSettingsService sourceSettingsService;

    /**
     * <p>
     * Creates an instance of the sales document JSON placeholder resolver
     * for the given organization.
     * </p>
     *
     * @param orgId Represents the organization id.
     */
    protected AbstractSalesDocumentJsonPlaceholderResolver(final long orgId) {
        this.orgId = orgId;
        final Configurations profilePicRedirectConfigs = ConfHolder.getInstance()
                .getProperties("profile-picture")
                .getNestedConfigs("redirectUrl");

        this.sourceProfilePicRedirectUrl = profilePicRedirectConfigs.getConfig("source");
        this.userProfilePicRedirectUrl = profilePicRedirectConfigs.getConfig("user");
        this.fieldSourceService = FieldSourceService.getInstance();
        this.sourceSettingsService = SourceSettingsService.getInstance();
    }

    /**
     * <p>
     * Resolves the given template JSON using the supplied sales document model.
     * </p>
     *
     * @param templateJson Represents the sales document template JSON.
     * @param model        Represents the sales document model.
     * @return The resolved sales document template JSON.
     */
    public abstract JsonObject resolve(final JsonObject templateJson, final R model);

    /**
     * <p>
     * Resolves the supplied template JSON using the given placeholder data.
     * </p>
     *
     * @param templateJson    Represents the sales document template JSON.
     * @param placeholderData Represents the placeholder data used for resolution.
     * @return The resolved template JSON, or {@code null} when the template JSON
     * is {@code null}.
     */
    protected JsonObject resolveTemplateJson(final JsonObject templateJson, final PlaceholderData placeholderData) {
        if (templateJson == null) {
            return null;
        }
        final Collection<SourceSettings> sourceSettings = sourceSettingsService.getCommonSourceSettings(orgId);

        if (Objects.nonNull(sourceSettings)) {
            final Map<Long, SourceSettings> sourceSettingsMap = sourceSettings.stream()
                    .collect(Collectors.toMap(SourceSettings::getSourceId, Function.identity()));

            final Collection<Source> supportedSources = Set.of(Source.CONTACT, Source.COMPANY, Source.PRODUCT);
            final Collection<String> quoteSourceNames = supportedSources.stream()
                    .map(Source::getName)
                    .collect(Collectors.toSet());

            final Map<String, FieldSource> sources = fieldSourceService
                    .getSources(quoteSourceNames)
                    .stream()
                    .collect(Collectors.toMap(FieldSource::getName, Function.identity()));

            for (final Source quoteSource : supportedSources) {
                final FieldSource source = sources.get(quoteSource.getName());
                final Long sourceId = source.id();

                if (sourceSettingsMap.containsKey(sourceId)) {
                    final String alternateName =
                            sourceSettingsMap.get(sourceId).getAlternateName();

                    final Consumer<PlaceholderValue> valueLoader = switch (quoteSource) {
                        case CONTACT -> builder::setSourceContactRenamedValue;
                        case COMPANY -> builder::setSourceCompanyRenamedValue;
                        case PRODUCT -> builder::setSourceProductRenamedValue;
                        default -> value -> {
                            // No action for unrecognized source names
                        };
                    };

                    if (Objects.nonNull(alternateName)) {
                        valueLoader.accept(PlaceholderValue.of(alternateName));
                    }
                }
            }
        }
        final JsonObject resolvedPlaceHolders = JsonPlaceholderResolver.resolve(templateJson, placeholderData);

        if (!hasLineItems) {
            clearEmptyLineItemRow(resolvedPlaceHolders);
        }

        return resolvedPlaceHolders;
    }

    /**
     * <p>
     * Clears the default empty line item row when the sales document does not
     * contain any products.
     * </p>
     *
     * @param resolvedJson Represents the resolved sales document JSON.
     */
    private void clearEmptyLineItemRow(final JsonObject resolvedJson) {
        final JsonArray formAttributes = resolvedJson.getJsonArray(QuotationFields.FORM_ATTRIBUTES);
        final int formAttributeCount = formAttributes.size();

        for (int formAttributeIndex = 0; formAttributeIndex < formAttributeCount; formAttributeIndex++) {

            final JsonObject formAttribute = formAttributes.getJsonObject(formAttributeIndex);

            if (Objects.equals(formAttribute.getString(QuotationFields.TYPE), QuotationFields.LINE_ITEMS)) {
                final JsonArray lineItemRows = formAttribute.getJsonArray(QuotationFields.ITEMS);
                final int lineItemRowCount = lineItemRows.size();

                for (int lineItemRowIndex = 0; lineItemRowIndex < lineItemRowCount; lineItemRowIndex++) {

                    lineItemRows.getJsonObject(lineItemRowIndex).put(QuotationFields.VALUES, Json.array());
                }
            }
        }
    }

    /**
     * <p>
     * Formats the supplied address into a comma-separated string containing
     * only the address components that are present.
     * </p>
     *
     * @param address Represents the address to format.
     * @return The formatted address, or an empty string when the address is null.
     */
    protected static String formatAddress(final Address address) {
        if (address == null) {
            return StringUtil.Delimiter.EMPTY_STRING.getDelimiter();
        }
        final StringJoiner stringJoiner = new StringJoiner(", ");

        appendIfPresent(stringJoiner, address.getAddressLine1());
        appendIfPresent(stringJoiner, address.getAddressLine2());
        appendIfPresent(stringJoiner, address.getCity());
        appendIfPresent(stringJoiner, address.getState());
        appendIfPresent(stringJoiner, address.getZipcode());
        appendIfPresent(stringJoiner, address.getCountryAsString());

        return stringJoiner.toString();
    }

    /**
     * <p>
     * Resolves the currency symbol from the supplied currency.
     * </p>
     *
     * @param currency Represents the currency.
     * @return The currency symbol, or an empty string when the currency is null.
     */
    protected static String resolveCurrencySymbol(final Currency currency) {
        return currency == null ? StringUtil.Delimiter.EMPTY_STRING.getDelimiter() : currency.getSymbol();
    }

    /**
     * <p>
     * Appends the supplied value to the given string joiner when the value
     * is not null or blank.
     * </p>
     *
     * @param joiner Represents the string joiner to which the value is appended.
     * @param value  Represents the value to append.
     */
    protected static void appendIfPresent(final StringJoiner joiner, final String value) {
        if (Objects.nonNull(value) && !value.isBlank()) {
            joiner.add(value);
        }
    }

    /**
     * <p>
     * Loads the buyer company name and, when present, its formatted postal address
     * into the party buyer company placeholders.
     * </p>
     *
     * @param company Represents the buyer company associated with the sales document.
     */
    protected void loadBuyerCompanyPlaceholders(final Company company) {
        if (Objects.nonNull(company)) {
            final Address address = company.getAddress();
            final String companyName = company.getName();

            if (Objects.nonNull(address)) {
                builder.setPartyBuyerCompanyAddressValue(PlaceholderValue.of(formatAddress(address)));
            }

            builder.setPartyBuyerCompanyNameValue(PlaceholderValue.of(companyName));
            builder.setPartyBuyerCompanyValue(
                    PlaceholderValue.of(
                            Json.object()
                                    .put(QuotationFields.ID, company.id())
                                    .put(QuotationFields.NAME, companyName)));
        }
    }

    /**
     * <p>
     * Builds an absolute thumbnail profile-picture URL for a {@link CrmUser}.
     * </p>
     *
     * @param user Represents the CRM user whose profile picture URL is to be built.
     * @return The absolute profile picture URL, or {@code null} if the user
     * has no thumbnail.
     */
    protected String buildUserProfileUrl(final CrmUser user) {
        final DisplayPicture thumbnail = user.getThumbnail();

        if (Objects.isNull(thumbnail)) {
            return null;
        }
        final String extension = thumbnail.extension().getName();
        final String imageType = thumbnail.type().getName();
        final JsonObject userJson = Json.object();

        userJson.put(ProfilePictureFields.USER_ID, user.getIamId());

        return HttpUtil.buildAbsoluteUrl(String.format(
                PROFILE_PIC_URL_FORMAT,
                userProfilePicRedirectUrl,
                Base64.getEncoder().encodeToString(userJson.getBytes()),
                StringUtil.Delimiter.FORWARD_SLASH.getDelimiter(),
                imageType,
                StringUtil.Delimiter.DOT.getDelimiter(),
                extension));
    }

    /**
     * <p>
     * Builds an absolute thumbnail profile-picture URL for a {@link Contact}.
     * </p>
     *
     * @param contact Represents the contact whose profile picture URL is to be built.
     * @return The absolute profile picture URL, or {@code null} if the contact
     * has no thumbnail.
     */
    protected String buildContactProfileUrl(final Contact contact) {
        final DisplayPicture thumbnail = contact.getThumbnail();

        if (Objects.isNull(thumbnail)) {
            return null;
        }
        final String extension = thumbnail.extension().getName();
        final String imageType = thumbnail.type().getName();
        final JsonObject json = Json.object();

        json.put(DisplayPictureFields.SOURCE, Source.CONTACT.getName().toLowerCase());
        json.put(DisplayPictureFields.ENTITY_ID, contact.id());

        return HttpUtil.buildAbsoluteUrl(String.format(
                PROFILE_PIC_URL_FORMAT,
                sourceProfilePicRedirectUrl,
                Base64.getEncoder().encodeToString(json.getBytes()),
                StringUtil.Delimiter.FORWARD_SLASH.getDelimiter(),
                imageType,
                StringUtil.Delimiter.DOT.getDelimiter(),
                extension));
    }

    /**
     * <p>
     * Converts the given {@link CrmUser} into a {@link JsonObject}
     * containing the user's display attributes.
     * </p>
     *
     * @param salesOwner Represents the user to convert.
     * @return A {@link JsonObject} representing the user.
     */
    protected JsonObject convertUserIntoJson(final CrmUser salesOwner) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(QuotationFields.FIRST_NAME, salesOwner.getFirstName());
        jsonObject.put(QuotationFields.LAST_NAME, salesOwner.getLastName());
        jsonObject.put(QuotationFields.EMAIL, salesOwner.getEmail());
        jsonObject.put(QuotationFields.PHONE, salesOwner.getMobile());

        return jsonObject;
    }

    /**
     * <p>
     * Converts the given {@link Contact} into a {@link JsonObject}
     * containing the contact's display attributes.
     * </p>
     *
     * @param contact Represents the contact to convert.
     * @return A {@link JsonObject} representing the contact.
     */
    protected JsonObject convertContactIntoJson(final Contact contact) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(InvoiceFields.VALUE, contact.id());

        return jsonObject;
    }

    /**
     * <p>
     * Loads the line item placeholder values from the product details
     * associated with the sales document.
     * </p>
     *
     * @param productDetailsHolder Represents the holder containing the product details.
     */
    protected void loadLineItemPlaceholders(final ProductDetailsHolder productDetailsHolder) {
        if (Objects.isNull(productDetailsHolder)) {
            return;
        }
        final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();

        if (Objects.isNull(productDetails) || productDetails.isEmpty()) {
            return;
        }

        final List<LineItemPlaceholderValue> lineItems = new ArrayList<>();
        final PlaceholderValue currencyValue = PlaceholderValue.of(resolveCurrencySymbol(productDetailsHolder.getCurrency()));

        for (final ProductDetail productDetail : productDetails) {
            final LineItemPlaceholderValue lineItemPlaceholderValue = getLineItemPlaceHolderValue(productDetail);

            lineItemPlaceholderValue.setItemCurrency(currencyValue);
            lineItemPlaceholderValue.setItemDiscount(PlaceholderValue.of(productDetail.getDiscount()));
            lineItemPlaceholderValue.setItemQuantity(PlaceholderValue.of(productDetail.getQuantity()));
            lineItemPlaceholderValue.setItemName(PlaceholderValue.of(productDetail.getProduct().getName()));
            lineItemPlaceholderValue.setItemUnitPrice(PlaceholderValue.of(productDetail.getPrice()));
            lineItemPlaceholderValue.setItemTermTotal(PlaceholderValue.of(productDetail.getAmount()));

            lineItems.add(lineItemPlaceholderValue);
        }

        hasLineItems = true;

        builder.setLineItems(lineItems);
        loadLineItemTotals(productDetailsHolder);
    }

    /**
     * <p>
     * Computes and sets the aggregate line item totals for the supplied
     * product details.
     * </p>
     *
     * @param productDetailsHolder Represents the holder from which totals are derived.
     */
    protected void loadLineItemTotals(final ProductDetailsHolder productDetailsHolder) {
        final double subTotal = productDetailsHolder.getSubTotal(productDetailsHolder);
        final double taxTotal =
                productDetailsHolder.getTaxTotal(productDetailsHolder);
        final double totalAmount =
                productDetailsHolder.getTotalAmount(productDetailsHolder);
        final double discountTotal =
                computeTotalDiscount(productDetailsHolder);
        final double discountPercentage =
                computeTotalDiscountPercentage(productDetailsHolder);

        final PlaceholderValue discountValue = PlaceholderValue.of(discountTotal);
        final PlaceholderValue discountPercentageValue = PlaceholderValue.of(String.format(PERCENTAGE_FORMAT, discountPercentage));
        final PlaceholderValue discountPercentageAndAmountValue =
                PlaceholderValue.of(String.format(DISCOUNT_PERCENTAGE_AND_AMOUNT_FORMAT, discountTotal, discountPercentage));

        builder.setItemSubTotalValue(PlaceholderValue.of(subTotal))
                .setItemDiscountAmountValue(discountValue)
                .setItemDiscountPercentageValue(discountPercentageValue)
                .setItemDiscountPercentageAndAmountValue(discountPercentageAndAmountValue)
                .setItemTaxValue(PlaceholderValue.of(taxTotal))
                .setItemTotalValue(PlaceholderValue.of(totalAmount));
    }

    /**
     * <p>
     * Computes the total discount amount across all line items.
     * </p>
     *
     * @param productDetailsHolder Represents the holder containing product details.
     * @return The total discount amount, or {@code 0.0} when there are no
     * product details.
     */
    protected double computeTotalDiscount(final ProductDetailsHolder productDetailsHolder) {

        double totalDiscount = 0.0;
        final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();

        if (Objects.nonNull(productDetails)) {

            for (final ProductDetail productDetail : productDetails) {
                totalDiscount += (productDetail.getPrice() * productDetail.getDiscount() / 100) * productDetail.getQuantity();
            }
        }

        return totalDiscount;
    }

    /**
     * <p>
     * Computes the weighted average discount percentage across all line items.
     * </p>
     *
     * @param productDetailsHolder Represents the holder containing product details.
     * @return The weighted average discount percentage, or {@code 0.0} when
     * the gross total is zero or there are no product details.
     */
    protected double computeTotalDiscountPercentage(final ProductDetailsHolder productDetailsHolder) {
        final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();

        if (Objects.isNull(productDetails) || productDetails.isEmpty()) {
            return 0.0;
        }
        double grossTotal = 0.0;
        double totalDiscount = 0.0;

        for (final ProductDetail productDetail : productDetails) {
            final double lineGross = productDetail.getPrice() * productDetail.getQuantity();

            grossTotal += lineGross;
            totalDiscount += lineGross * productDetail.getDiscount() / 100;
        }

        return grossTotal > 0 ? Math.round(totalDiscount / grossTotal * 10000.0) / 100.0 : 0.0;
    }

    /**
     * <p>
     * Creates the line item placeholder value for the supplied product detail.
     * </p>
     *
     * @param productDetail Represents the product detail from which the line
     *                      item placeholder value is created.
     * @return The line item placeholder value.
     */
    protected abstract LineItemPlaceholderValue getLineItemPlaceHolderValue(final ProductDetail productDetail);
}