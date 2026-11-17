package com.tlc.crm.core.features.sales.template.model;

import com.tlc.attachment.generator.HtmlContentFileGenerator;
import com.tlc.attachment.io.Extension;
import com.tlc.attachment.io.File;
import com.tlc.attachment.model.FileGenerateRequest;
import com.tlc.attachment.model.FileInfo;
import com.tlc.attachment.model.HtmlContentHolder;
import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.attachment.storage.model.PreSignedUrlHeader;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.template.builder.TemplateBuilder;
import com.tlc.commons.template.model.DefaultTemplateBuildRequest;
import com.tlc.commons.template.model.Section;
import com.tlc.commons.template.model.TemplateBuildRequest;
import com.tlc.commons.template.model.TemplateModel;
import com.tlc.commons.template.model.TemplateWrapper;
import com.tlc.commons.template.processor.PostSectionRemover;
import com.tlc.commons.template.processor.PreSectionRemover;
import com.tlc.commons.template.processor.Processor;
import com.tlc.commons.template.processor.SectionOrderUpdater;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;
import com.tlc.crm.common.models.CommonFields;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.core.features.quote.fields.QuotationFields;
import com.tlc.crm.core.features.quote.model.EffectiveDateValueType;
import com.tlc.crm.core.features.quote.model.SubTotalFormat;
import com.tlc.crm.security.iam.LoggedInUserInfo;

import com.tlc.file.client.service.AttachmentService;
import com.tlc.file.common.models.Attachment;

import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.web.HttpUtil;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * <p>
 * Provides the common file generation logic for sales documents such as
 * quotations and invoices, including HTML preview and PDF download generation.
 * </p>
 *
 * @param <T> Represents the {@link TemplateModel} implementation used by the document.
 * @param <D> Represents the domain object being rendered.
 * @param <F> Represents the generated {@link AbstractSalesDocumentFile}.
 * @author Mohamed Yasar K
 * @version 1.0
 */
public abstract class AbstractSalesDocumentFileBuilder<T extends TemplateModel, D, F extends AbstractSalesDocumentFile> {

    protected static final DateTimeFormatter SIMPLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private static final String PROFILE_PIC_URL_FORMAT = "%s%s%s%s%s%s";

    protected final TemplateBuilder<TemplateBuildRequest<T>> templateBuilder;
    protected final AttachmentService attachmentService;
    protected final String userProfilePicRedirectUrl;
    protected final String filePreviewUrl;
    protected final D document;

    /**
     * <p>
     * Creates an instance of {@link AbstractSalesDocumentFileBuilder}.
     * </p>
     *
     * @param document Represents the sales document to be rendered.
     */
    protected AbstractSalesDocumentFileBuilder(final D document) {
        final ConfHolder confHolder = ConfHolder.getInstance();

        final Configurations profilePicRedirectConfigs = confHolder
                .getProperties("profile-picture")
                .getNestedConfigs("redirectUrl");

        this.document = document;
        this.userProfilePicRedirectUrl = profilePicRedirectConfigs.getConfig("user");
        this.templateBuilder = TemplateBuilder.getInstance();
        this.attachmentService = AttachmentService.getInstance();
        this.filePreviewUrl = confHolder.getProperties(CommonFields.CRM_COMMON_CONFIG_KEY)
                .getConfig(CommonFields.FILE_PREVIEW_REDIRECT_URL);
    }

    /**
     * <p>
     * Builds the sales document file by rendering the configured template into
     * HTML preview and PDF download files.
     * </p>
     *
     * @return {@link AbstractSalesDocumentFile} containing the generated files,
     * or an empty document file when template generation fails.
     */
    public final F build() {
        final TemplateBuildRequest<T> templateBuildRequest = buildTemplateDataHolder(document);
        final Optional<String> optionalTemplate = templateBuilder.build(getTemplateName(), templateBuildRequest);

        return optionalTemplate.map(renderedTemplate -> {
            final File htmlFile = generateFile(generateFilePath(Extension.HTML), Extension.HTML, renderedTemplate);
            final String pdfTemplate = createPostSectionRemover(getPdfExcludedSections()).process(renderedTemplate);
            final File pdfFile = generateFile(generateFilePath(Extension.PDF), Extension.PDF, pdfTemplate);

            return newDocumentFile(htmlFile, pdfFile);
        }).orElseGet(this::newEmptyDocumentFile);
    }

    /**
     * <p>
     * Generates a file using the given content and extension.
     * </p>
     *
     * @param filePath  Represents the generated file path.
     * @param extension Represents the generated file extension.
     * @param content   Represents the file content.
     * @return {@link File} containing the generated file.
     */
    private File generateFile(final String filePath, final Extension extension, final String content) {
        final FileGenerateRequest<HtmlContentHolder> request = new FileGenerateRequest<>(new FileInfo(filePath, extension),
                new HtmlContentHolder(content));

        return HtmlContentFileGenerator.get(extension).generate(request);
    }

    /**
     * <p>
     * Gets the template name registered for the document type.
     * </p>
     *
     * @return Template name.
     */
    protected abstract String getTemplateName();

    /**
     * <p>
     * Generates the storage path for the generated document file.
     * </p>
     *
     * @param extension Represents the generated file extension.
     * @return Generated file path.
     */
    protected abstract String generateFilePath(final Extension extension);

    /**
     * <p>
     * Gets the sections that must be excluded from the PDF rendering.
     * </p>
     *
     * @return Collection of sections excluded from PDF generation.
     */
    protected abstract Set<Section> getPdfExcludedSections();

    /**
     * <p>
     * Creates the concrete sales document file.
     * </p>
     *
     * @param previewFile  Represents the generated preview file.
     * @param downloadFile Represents the generated download file.
     * @return Concrete sales document file.
     */
    protected abstract F newDocumentFile(final File previewFile, final File downloadFile);

    /**
     * <p>
     * Creates an empty concrete sales document file.
     * </p>
     *
     * @return Empty sales document file.
     */
    protected abstract F newEmptyDocumentFile();

    /**
     * <p>
     * Builds the template data holder for the given document.
     * </p>
     *
     * @param document Represents the sales document.
     * @return Template build request containing the document data.
     */
    protected abstract TemplateBuildRequest<T> buildTemplateDataHolder(final D document);

    /**
     * <p>
     * Resolves the given section id into a known template section.
     * </p>
     *
     * @param sectionId Represents the section id.
     * @return Resolved {@link Section}, or {@code null} when the section is custom.
     */
    protected abstract Section resolveSection(final String sectionId);

    /**
     * <p>
     * Gets the base section used when ordering document sections.
     * </p>
     *
     * @return Base {@link Section}.
     */
    protected abstract Section getBaseSection();

    /**
     * <p>
     * Creates a custom section for an unknown section id.
     * </p>
     *
     * @param sectionId Represents the section id.
     * @param name      Represents the section name.
     * @return Created {@link Section}.
     */
    protected abstract Section createCustomSection(final String sectionId, final String name);

    /**
     * <p>
     * Builds the template attributes for a single product.
     * </p>
     *
     * @param productJson Represents the product JSON.
     * @return Map containing the product template attributes.
     */
    protected abstract Map<String, Object> buildProductAttributes(final JsonObject productJson);

    /**
     * <p>
     * Determines whether the given section represents an issue date section.
     * </p>
     *
     * @param section Represents the document section.
     * @return {@code true} when the section represents an issue date;
     * otherwise {@code false}.
     */
    protected boolean isIssueDateSection(final Section section) {
        return false;
    }

    /**
     * <p>
     * Loads the section ordering and section removal processors into the template
     * data holder.
     * </p>
     *
     * @param documentJson       Represents the document section JSON.
     * @param templateDataHolder Holds the template build data.
     */
    protected final void loadProcessors(final JsonArray documentJson,
                                        final DefaultTemplateBuildRequest<T> templateDataHolder) {
        final List<Section> sectionsToOrderUpdate = new ArrayList<>();
        final Set<Section> sectionsToRemove = new HashSet<>();
        final int sectionCount = documentJson.size();

        for (int sectionIndex = 0; sectionIndex < sectionCount; sectionIndex++) {
            final JsonObject sectionJson = documentJson.getJsonObject(sectionIndex);
            final String sectionId = sectionJson.getString(Key.SECTION_ID);
            final Section defaultSection = resolveSection(sectionId);
            final boolean isDefaultSectionAbsent = Objects.isNull(defaultSection);
            final Section section = isDefaultSectionAbsent
                    ? createCustomSection(sectionId, getValue(Key.NAME, sectionJson))
                    : defaultSection;

            sectionsToOrderUpdate.add(section);

            if (!isDefaultSectionAbsent) {
                sectionsToOrderUpdate.addAll(defaultSection.getRelatedSections());
            }

            sectionsToRemove.addAll(getDisabledSections(sectionJson));
        }

        templateDataHolder.addPreProcessor(createPreSectionRemover(sectionsToRemove));
        templateDataHolder.addPostProcessor(createSectionOrderUpdater(sectionsToOrderUpdate));
    }

    /**
     * <p>
     * Gets the disabled sections from the given section JSON.
     * </p>
     *
     * @param sectionJson Represents the section JSON.
     * @return Collection of disabled sections.
     */
    protected final Collection<? extends Section> getDisabledSections(final JsonObject sectionJson) {
        final Collection<Section> disabledSections = new ArrayList<>();

        if (!sectionJson.optBoolean(Key.IS_ENABLED, true)) {
            final Section section = resolveSection(sectionJson.getString(Key.SECTION_ID));

            if (Objects.nonNull(section)) {
                disabledSections.add(section);
                disabledSections.addAll(section.getRelatedSections());
            }
        }
        sectionJson.forEach((key, value) -> disabledSections.addAll(getDisabledSections(value)));

        return disabledSections;
    }

    /**
     * <p>
     * Recursively gets disabled sections from a JSON value.
     * </p>
     *
     * @param value Represents the JSON value to inspect.
     * @return Collection of disabled sections.
     */
    protected final Collection<? extends Section> getDisabledSections(final Object value) {
        final Collection<Section> disabledSections = new ArrayList<>();

        if (value instanceof final JsonObject jsonObject) {
            disabledSections.addAll(getDisabledSections(jsonObject));
        } else if (value instanceof final JsonArray jsonArray) {
            jsonArray.forEach(object -> disabledSections.addAll(getDisabledSections(object)));
        } else if (value instanceof Map) {
            disabledSections.addAll(getDisabledSections(Json.object((Map<String, Object>) value)));
        } else if (value instanceof final Collection<?> collection) {
            collection.forEach(object -> disabledSections.addAll(getDisabledSections(object)));
        }

        return disabledSections;
    }

    /**
     * <p>
     * Creates the section order update processor.
     * </p>
     *
     * @param sectionsToOrderUpdate Represents the sections to reorder.
     * @return Section order update processor.
     */
    protected final Processor<String> createSectionOrderUpdater(final List<Section> sectionsToOrderUpdate) {

        return new SectionOrderUpdater() {
            @Override
            protected List<Section> getReorderedSection() {
                return sectionsToOrderUpdate;
            }

            @Override
            protected Section getBaseSection() {
                return AbstractSalesDocumentFileBuilder.this.getBaseSection();
            }
        };
    }

    /**
     * <p>
     * Creates the pre-section removal processor.
     * </p>
     *
     * @param sectionsToRemove Represents the sections to remove.
     * @return Pre-section removal processor.
     */
    protected final Processor<TemplateWrapper> createPreSectionRemover(final Set<Section> sectionsToRemove) {

        return new PreSectionRemover() {
            @Override
            protected Collection<Section> getSections() {
                return sectionsToRemove;
            }
        };
    }

    /**
     * <p>
     * Creates the post-section removal processor.
     * </p>
     *
     * @param sectionsToRemove Represents the sections to remove.
     * @return Post-section removal processor.
     */
    protected final Processor<String> createPostSectionRemover(final Set<Section> sectionsToRemove) {

        return new PostSectionRemover() {
            @Override
            protected Collection<Section> getSections() {
                return sectionsToRemove;
            }
        };
    }

    /**
     * <p>
     * Loads title and value attributes from the supplied JSON array into the
     * template data holder.
     * </p>
     *
     * @param jsonArray  Represents the JSON attribute array.
     * @param dataHolder Represents the template data holder.
     */
    protected final void loadTitleAndValue(final JsonArray jsonArray, final Map<String, Object> dataHolder) {
        final int sectionCount = jsonArray.size();

        for (int sectionIndex = 0; sectionIndex < sectionCount; sectionIndex++) {
            final JsonObject jsonObject = jsonArray.getJsonObject(sectionIndex);
            final Consumer<String> titleAdder;
            final Consumer<String> valueAdder;

            switch (jsonObject.getString(Key.TYPE)) {
                case SectionType.TITLE -> {
                    titleAdder = title -> {
                    };
                    valueAdder = title -> dataHolder.put(TemplateField.TITLE, title);
                }
                case SectionType.QUOTE_REF -> {
                    titleAdder = referenceTitle -> dataHolder.put(TemplateField.REFERENCE_TITLE, referenceTitle);
                    valueAdder = referenceValue -> dataHolder.put(TemplateField.REFERENCE_VALUE, referenceValue);
                }
                case SectionType.DATE -> {
                    final Section section = resolveSection(jsonObject.getString(Key.SECTION_ID));

                    if (Objects.nonNull(section) && isIssueDateSection(section)) {
                        titleAdder = issueDateTitle -> dataHolder.put(TemplateField.ISSUE_DATE_TITLE, issueDateTitle);
                        valueAdder = issueDateValue -> dataHolder.put(TemplateField.ISSUE_DATE_VALUE, convertToDateString(issueDateValue));
                    } else {
                        titleAdder = expiryDateTitle -> dataHolder.put(TemplateField.EXPIRY_DATE_TITLE, expiryDateTitle);
                        valueAdder = expiryDateValue -> dataHolder.put(TemplateField.EXPIRY_DATE_VALUE, convertToDateString(expiryDateValue));
                    }
                }
                case SectionType.CURRENCY -> {
                    titleAdder = currencyTitle -> dataHolder.put(TemplateField.CURRENCY_TITLE, currencyTitle);
                    valueAdder = currencyValue -> dataHolder.put(TemplateField.CURRENCY_VALUE, currencyValue);
                }
                case SectionType.SUBTOTAL -> {
                    final SubTotalFormat subTotalFormat = SubTotalFormat.get(jsonObject.getInt(Key.FORMAT));
                    final JsonArray valuesArray = jsonObject.getJsonArray(Key.VALUES);
                    final int valueCount = valuesArray.size();

                    titleAdder = subTotalTitle -> dataHolder.put(TemplateField.SUBTOTAL_TITLE, subTotalTitle);
                    valueAdder = subTotalValue -> {
                    };

                    for (int valueIndex = 0; valueIndex < valueCount; valueIndex++) {
                        final JsonObject valueJson = valuesArray.getJsonObject(valueIndex);

                        final String subTotalValue = switch (subTotalFormat) {
                            case SIMPLE_SUBTOTAL_AMOUNT ->
                                    attachCurrencySymbol(getValue(Key.VALUE, valueJson), valueJson);
                            case SUBTOTAL_WITH_BREAKDOWN -> getValue(Key.VALUE_WITH_BREAKDOWN, valueJson);
                        };

                        dataHolder.put(TemplateField.SUBTOTAL_VALUE, subTotalValue);
                    }
                }
                case SectionType.DISCOUNT -> {
                    final DiscountFormat discountFormat = DiscountFormat.get(jsonObject.getInt(Key.FORMAT));
                    final JsonArray valuesArray = jsonObject.getJsonArray(Key.VALUES);
                    final int valueCount = valuesArray.size();

                    titleAdder = totalDiscountTitle -> dataHolder.put(TemplateField.TOTAL_DISCOUNT_TITLE, totalDiscountTitle);
                    valueAdder = totalDiscountValue -> {
                    };

                    for (int valueIndex = 0; valueIndex < valueCount; valueIndex++) {
                        final JsonObject valueJson = valuesArray.getJsonObject(valueIndex);

                        final String totalDiscountValue = switch (discountFormat) {
                            case SHOW_AS_AMOUNT ->
                                    attachCurrencySymbol(getValue(Key.VALUE_AS_AMOUNT, valueJson), valueJson);
                            case SHOW_AS_PERCENTAGE -> getValue(Key.VALUE_AS_PERCENTAGE, valueJson);
                            case SHOW_AS_PERCENTAGE_AND_AMOUNT ->
                                    attachCurrencySymbol(getValue(Key.VALUE_AS_PERCENTAGE_AND_AMOUNT, valueJson), valueJson);
                        };

                        dataHolder.put(TemplateField.TOTAL_DISCOUNT_VALUE, totalDiscountValue);
                    }
                }
                case SectionType.TAX -> {
                    titleAdder = taxTitle -> dataHolder.put(TemplateField.TAX_TITLE, taxTitle);
                    valueAdder = taxValue -> dataHolder.put(TemplateField.TAX_VALUE, taxValue);
                }
                case SectionType.DUE -> {
                    final JsonObject descriptionJson = jsonObject.getJsonObject(Key.DESCRIPTION);

                    dataHolder.put(TemplateField.DUE_DESCRIPTION, getValue(Key.VALUE, descriptionJson));

                    titleAdder = dueTitle -> dataHolder.put(TemplateField.DUE_TITLE, dueTitle);
                    valueAdder = dueValue -> dataHolder.put(TemplateField.DUE_VALUE, dueValue);
                }
                case SectionType.TOTAL_VALUE -> {
                    titleAdder = totalValueTitle -> dataHolder.put(TemplateField.TOTAL_VALUE_TITLE, totalValueTitle);
                    valueAdder = totalValue -> dataHolder.put(TemplateField.TOTAL_VALUE, totalValue);
                }
                case SectionType.EFFECTIVE_DATE -> {
                    final JsonArray effectiveDateValues = jsonObject.getJsonArray(Key.VALUES);
                    final int effectiveDateCount = effectiveDateValues.size();

                    titleAdder = effectiveDateTitle -> dataHolder.put(TemplateField.EFFECTIVE_DATE_TITLE, effectiveDateTitle);
                    valueAdder = effectiveDateValue -> {
                    };

                    for (int dateValueIndex = 0;
                         dateValueIndex < effectiveDateCount;
                         dateValueIndex++) {
                        final JsonObject valueJson = effectiveDateValues.getJsonObject(dateValueIndex);

                        if (!valueJson.containsKey(Key.POSITION)) {
                            final String effectiveDateValueType = getValue(Key.VALUE, valueJson);

                            dataHolder.put(TemplateField.EFFECTIVE_DATE_VALUE_TYPE, NumberUtils.parseNumber(effectiveDateValueType).intValue());
                        }
                    }

                    for (int dateValueIndex = 0; dateValueIndex < effectiveDateCount; dateValueIndex++) {
                        final JsonObject valueJson = effectiveDateValues.getJsonObject(dateValueIndex);

                        if (valueJson.containsKey(Key.POSITION)) {
                            final String effectiveDateValueAsString = getValue(Key.VALUE, valueJson);

                            final EffectiveDateValueType effectiveDateValueType = EffectiveDateValueType.get((int) dataHolder.remove(TemplateField.EFFECTIVE_DATE_VALUE_TYPE));

                            final String effectiveDateValue = switch (effectiveDateValueType) {
                                case ON_AGREEMENT -> effectiveDateValueAsString;
                                case CUSTOM_DATE -> convertToDateString(effectiveDateValueAsString);
                                case DELAYED_START_DAYS ->
                                        StringUtil.join(StringUtil.Delimiter.EMPTY_SPACE, effectiveDateValueAsString, "days after agreement");
                                case DELAYED_START_MONTHS ->
                                        StringUtil.join(StringUtil.Delimiter.EMPTY_SPACE, effectiveDateValueAsString, "months after agreement");
                            };

                            dataHolder.put(TemplateField.EFFECTIVE_DATE_VALUE, effectiveDateValue);
                        }
                    }
                }
                case SectionType.TERM_LENGTH -> {
                    titleAdder = termLengthTitle -> dataHolder.put(TemplateField.TERM_LENGTH_TITLE, termLengthTitle);
                    valueAdder = termLengthValue -> dataHolder.put(TemplateField.TERM_LENGTH_VALUE, termLengthValue);
                }
                default -> {
                    titleAdder = title -> {
                    };
                    valueAdder = value -> {
                    };
                }
            }

            titleAdder.accept(getLabelName(jsonObject));
            valueAdder.accept(getSingleValue(jsonObject));
        }
    }

    /**
     * <p>
     * Builds the template data for the line items section, including product
     * details and totals.
     * </p>
     *
     * @param sectionJson Represents the line items section JSON.
     * @return Map containing line item template data.
     */
    protected final Map<String, Object> buildLineItemsData(final JsonObject sectionJson) {
        final Map<String, Object> lineItemsData = new java.util.HashMap<>();
        final JsonArray itemsJson = sectionJson.getJsonArray(Key.ITEMS);
        final JsonObject totalsJson = sectionJson.getJsonObject(Key.TOTALS);
        final JsonArray totalsAttributesJson = totalsJson.getJsonArray(Key.ATTRIBUTES);

        lineItemsData.put(TemplateField.TITLE, getValue(Key.NAME, sectionJson));
        lineItemsData.put(TemplateField.ITEMS, buildProductsData(itemsJson));

        loadTitleAndValue(totalsAttributesJson, lineItemsData);

        return Map.copyOf(lineItemsData);
    }

    /**
     * <p>
     * Builds the product template data from the supplied items JSON.
     * </p>
     *
     * @param itemsArray Represents the product items JSON.
     * @return List of product template attributes.
     */
    private List<Map<String, Object>> buildProductsData(final JsonArray itemsArray) {
        final List<Map<String, Object>> productsData = new ArrayList<>();
        final int itemCount = itemsArray.size();

        for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
            final JsonObject itemJson = itemsArray.getJsonObject(itemIndex);
            final JsonArray productsArray = itemJson.getJsonArray(Key.VALUES);
            final int productCount = productsArray.size();

            for (int productIndex = 0; productIndex < productCount; productIndex++) {
                productsData.add(buildProductAttributes(productsArray.getJsonObject(productIndex)));
            }
        }

        return List.copyOf(productsData);
    }

    /**
     * <p>
     * Converts the UTC timestamp into a date string using the current user's
     * configured timezone.
     * </p>
     *
     * @param utcTimeAsString Represents the UTC timestamp.
     * @return Formatted date string.
     */
    protected final String convertToDateString(final String utcTimeAsString) {
        final CrmUser currentUser = LoggedInUserInfo.getUser();
        final UserRegionalPreference userPreference = currentUser.getPreferenceHolder().get(UserPreferenceType.USER_REGIONAL_PREFERENCE);
        final TimezoneInfo userTimezone = userPreference.getTimezoneInfo();

        return Instant.ofEpochMilli(NumberUtils.parseNumber(utcTimeAsString).longValue())
                .atZone(userTimezone.getTimeZone().toZoneId())
                .format(SIMPLE_DATE_FORMATTER);
    }

    /**
     * <p>
     * Attaches the currency symbol to the supplied value.
     * </p>
     *
     * @param value     Represents the monetary value.
     * @param valueJson Represents the JSON containing the currency.
     * @return Value prefixed with its currency symbol.
     */
    protected final String attachCurrencySymbol(final String value,
                                                final JsonObject valueJson) {
        final String currency = getValue(Key.CURRENCY, valueJson);

        return StringUtil.join(StringUtil.Delimiter.EMPTY_STRING, currency, Objects.isNull(value) ? String.valueOf(NumericalConstant.ZERO)
                : value);
    }

    /**
     * <p>
     * Loads the company logo and company-name configuration into the header data.
     * </p>
     *
     * @param logoJson   Represents the logo configuration JSON.
     * @param headerData Represents the header template data.
     */
    protected final void loadLogoAndCompanyData(final JsonArray logoJson,
                                                final Map<String, Object> headerData) {
        final int logoCount = logoJson.size();

        for (int logoIndex = 0; logoIndex < logoCount; logoIndex++) {
            final JsonObject jsonObject = logoJson.getJsonObject(logoIndex);

            switch (jsonObject.getString(Key.TYPE)) {
                case SectionType.ATTACHMENT -> {
                    final String imageId = getValue(Key.VALUE, jsonObject);

                    if (imageId != null && !imageId.isBlank()) {
                        final long attachmentId = Long.parseLong(imageId);

                        headerData.put(TemplateField.COMPANY_LOGO, buildAttachmentURL(attachmentId, QuotationFields.COMPANY_LOGO));
                    } else {
                        headerData.put(TemplateField.COMPANY_LOGO, imageId);
                    }
                }
                case SectionType.COMPANY_NAME_TOGGLE -> headerData.put(TemplateField.COMPANY_NAME, getValue(Key.VALUE, jsonObject));
            }
        }
    }

    /**
     * <p>
     * Builds a pre-signed URL for accessing the attachment with the specified identifier.
     * </p>
     *
     * <p>
     * The URL is configured with a 10-minute validity period and includes response
     * headers for inline content disposition and content type based on the attachment extension.
     * </p>
     *
     * @param attachmentId Represents the identifier of the attachment.
     * @param field        Represents the field name associated with the attachment.
     * @return Pre-signed URL string for accessing the attachment.
     */
    protected String buildAttachmentURL(final long attachmentId, final String field) {
        return attachmentService.getPresignedUrl(attachmentId, 10, Map.of(PreSignedUrlHeader.RESPONSE_CONTENT_DISPOSITION,
                String.format("inline; filename=%s", filePreviewUrl),
                PreSignedUrlHeader.RESPONSE_CONTENT_TYPE,
                Extension.get(attachmentService.getAttachments(List.of(attachmentId)).stream().findFirst().map(Attachment::getExtension)
                        .orElse(Extension.PNG.getName())).getName()));
    }

    /**
     * <p>
     * Builds the user profile picture URL.
     * </p>
     *
     * @param json      Represents the profile picture JSON.
     * @param extension Represents the image extension.
     * @return Absolute user profile picture URL.
     */
    protected final String buildUserProfileURL(final JsonObject json,
                                               final Extension extension) {
        return HttpUtil.buildAbsoluteUrl(
                String.format(
                        PROFILE_PIC_URL_FORMAT,
                        userProfilePicRedirectUrl,
                        Base64.getEncoder().encodeToString(json.getBytes()),
                        StringUtil.Delimiter.FORWARD_SLASH.getDelimiter(),
                        com.tlc.attachment.model.profilePicture.ProfilePicture.Quality.THUMBNAIL.getName(),
                        StringUtil.Delimiter.DOT.getDelimiter(),
                        extension));
    }

    /**
     * <p>
     * Gets the label value from the given JSON section.
     * </p>
     *
     * @param subSectionJson Represents the subsection JSON.
     * @return Label value, or an empty string when unavailable.
     */
    protected final String getLabelName(final JsonObject subSectionJson) {
        if (subSectionJson.containsKey(Key.LABEL)) {
            final JsonObject labelJson = subSectionJson.getJsonObject(Key.LABEL);

            return getValue(Key.VALUE, labelJson);
        }

        return StringUtil.EMPTY_STRING;
    }

    /**
     * <p>
     * Gets the first value from the given JSON subsection.
     * </p>
     *
     * @param subSectionJson Represents the subsection JSON.
     * @return First value, or an empty string when unavailable.
     */
    protected final String getSingleValue(final JsonObject subSectionJson) {
        if (subSectionJson.containsKey(Key.VALUES)) {
            final JsonArray valuesJson = subSectionJson.getJsonArray(Key.VALUES);
            final JsonObject jsonObject = valuesJson.optJsonObject(0);

            if (Objects.isNull(jsonObject)) {
                return StringUtil.EMPTY_STRING;
            }

            return getValue(Key.VALUE, jsonObject);
        }

        return StringUtil.EMPTY_STRING;
    }

    /**
     * <p>
     * Gets a value from the supplied JSON object.
     * </p>
     *
     * @param name        Represents the JSON property name.
     * @param sectionJson Represents the JSON object.
     * @return String representation of the value, or an empty string.
     */
    protected final String getValue(final String name, final JsonObject sectionJson) {
        return Optional.ofNullable(sectionJson.getNullableValue(name))
                .map(String::valueOf)
                .orElse(StringUtil.EMPTY_STRING);
    }

    /**
     * <p>
     * Loads the sales person details into the sales document template data.
     * </p>
     *
     * @param templateData Represents the sales document template data.
     * @param owner               Represents the sales person user details.
     */
    protected void loadSalesPersonData(final AbstractSalesDocumentTemplateData templateData, final CrmUser owner) {
        final DisplayPicture originalPicture = owner.getOriginalPicture();
        final DisplayPicture thumbnail = owner.getThumbnail();
        final Long profilePicFileId = Objects.isNull(thumbnail) ? null : thumbnail.getFileId();

        templateData.addValue(Key.SALES_PERSON_NAME, owner.getFullName()).addValue(Key.SALES_PERSON_EMAIL, owner.getEmail());

        if (Objects.isNull(originalPicture) || Objects.isNull(profilePicFileId)) {
            templateData.addValue(Key.SALES_PERSON_PROFILE_URL, StringUtil.EMPTY_STRING);
        } else {
            templateData.addValue(Key.SALES_PERSON_PROFILE_URL, buildAttachmentURL(profilePicFileId, QuotationFields.USER_PROFILE_PIC));
        }
    }

    /**
     * Defines the JSON property names used by sales document sections.
     */
    public static final class Key {

        public static final String TYPE = "type";
        public static final String SECTION_ID = "sectionId";
        public static final String NAME = "name";
        public static final String IS_ENABLED = "isEnabled";
        public static final String VALUES = "values";
        public static final String VALUE = "value";
        public static final String LABEL = "label";
        public static final String LOGO = "logo";
        public static final String POSITION = "position";
        public static final String DETAILS = "details";
        public static final String DESCRIPTION = "description";
        public static final String FORMAT = "format";
        public static final String CURRENCY = "currency";
        public static final String EMAIL = "email";
        public static final String ITEMS = "items";
        public static final String TOTALS = "totals";
        public static final String ATTRIBUTES = "attributes";
        public static final String QUANTITY = "quantity";
        public static final String UNIT_PRICE = "unitPrice";
        public static final String DISCOUNT = "discount";
        public static final String VALUE_WITH_BREAKDOWN = "valueWithBreakdown";
        public static final String VALUE_AS_AMOUNT = "valueAsAmount";
        public static final String VALUE_AS_PERCENTAGE = "valueAsPercentage";
        public static final String VALUE_AS_PERCENTAGE_AND_AMOUNT = "valueAsPercentageAndAmount";
        private static final String SALES_PERSON_NAME = "salesPersonName";
        private static final String SALES_PERSON_EMAIL = "salesPersonEmail";
        private static final String SALES_PERSON_PROFILE_URL = "salesPersonProfileUrl";

        private Key() {
        }
    }

    /**
     * Defines the type values used by sales document sections.
     */
    public static final class SectionType {

        public static final String TITLE = "TITLE";
        public static final String QUOTE_REF = "QUOTE_REF";
        public static final String DATE = "DATE";
        public static final String CURRENCY = "CURRENCY";
        public static final String SUBTOTAL = "SUBTOTAL";
        public static final String DISCOUNT = "DISCOUNT";
        public static final String TAX = "TAX";
        public static final String DUE = "DUE";
        public static final String TOTAL_VALUE = "TOTAL_VALUE";
        public static final String EFFECTIVE_DATE = "EFFECTIVE_DATE";
        public static final String TERM_LENGTH = "TERM_LENGTH";
        public static final String ATTACHMENT = "ATTACHMENT";
        public static final String COMPANY_NAME_TOGGLE = "COMPANY_NAME_TOGGLE";
        public static final String ORGANISATION = "ORGANISATION";
        public static final String COMPANY = "COMPANY";
        public static final String ADDRESS = "ADDRESS";
        public static final String USER = "USER";
        public static final String EMAIL = "EMAIL";

        private SectionType() {
        }
    }

    /**
     * Defines the template field names used by sales document templates.
     */
    public static final class TemplateField {

        public static final String TITLE = "title";
        public static final String DESCRIPTION = "description";
        public static final String CURRENCY_TITLE = "currencyTitle";
        public static final String CURRENCY_VALUE = "currencyValue";
        public static final String SUBTOTAL_TITLE = "subTotalTitle";
        public static final String SUBTOTAL_VALUE = "subTotalValue";
        public static final String TOTAL_DISCOUNT_TITLE = "totalDiscountTitle";
        public static final String TOTAL_DISCOUNT_VALUE = "totalDiscountValue";
        public static final String TAX_TITLE = "taxTitle";
        public static final String TAX_VALUE = "taxValue";
        public static final String DUE_DESCRIPTION = "dueDescription";
        public static final String DUE_TITLE = "dueTitle";
        public static final String DUE_VALUE = "dueValue";
        public static final String TOTAL_VALUE_TITLE = "totalValueTitle";
        public static final String TOTAL_VALUE = "totalValue";
        public static final String EFFECTIVE_DATE_TITLE = "effectiveDateTitle";
        public static final String EFFECTIVE_DATE_VALUE_TYPE = "effectiveDateValueType";
        public static final String EFFECTIVE_DATE_VALUE = "effectiveDateValue";
        public static final String TERM_LENGTH_TITLE = "termLengthTitle";
        public static final String TERM_LENGTH_VALUE = "termLengthValue";
        public static final String REFERENCE_TITLE = "referenceTitle";
        public static final String REFERENCE_VALUE = "referenceValue";
        public static final String ISSUE_DATE_TITLE = "issueDateTitle";
        public static final String ISSUE_DATE_VALUE = "issueDateValue";
        public static final String EXPIRY_DATE_TITLE = "expiryDateTitle";
        public static final String EXPIRY_DATE_VALUE = "expiryDateValue";
        public static final String COMPANY_LOGO = "companyLogo";
        public static final String COMPANY_NAME = "companyName";
        public static final String ITEMS = "items";
        public static final String NAME = "name";
        public static final String VALUE = "value";
        public static final String QUANTITY = "quantity";
        public static final String UNIT_PRICE = "unitPrice";
        public static final String DISCOUNT = "discount";
        public static final String EMAIL = "email";
        public static final String DOWNLOAD_URL = "downloadUrl";

        private TemplateField() {
        }
    }
}