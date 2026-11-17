package com.tlc.crm.core.features.quote.file;

import com.tlc.attachment.io.Extension;
import com.tlc.attachment.io.File;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.template.model.DefaultTemplateBuildRequest;
import com.tlc.commons.template.model.Section;
import com.tlc.commons.template.model.TemplateBuildRequest;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.StringUtil;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.features.quote.fields.QuotationFields;
import com.tlc.crm.core.features.quote.model.Quotation;
import com.tlc.crm.core.features.quote.model.QuoteCustomSection;
import com.tlc.crm.core.features.quote.model.QuoteSection;
import com.tlc.crm.core.features.quote.model.QuoteTemplateData;
import com.tlc.crm.core.features.quote.util.QuotationURLGenerator;
import com.tlc.crm.core.features.sales.template.model.AbstractSalesDocumentFile;
import com.tlc.crm.core.features.sales.template.model.AbstractSalesDocumentFileBuilder;
import com.tlc.crm.core.features.sales.template.model.PaymentTerms;
import com.tlc.web.HttpUtil;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Represents the generated files associated with a quotation.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class QuotationFile extends AbstractSalesDocumentFile {

    private QuotationFile() {
        this(null, null);
    }

    private QuotationFile(final File previewFile, final File downloadFile) {
        super(previewFile, downloadFile);
    }

    /**
     * <p>
     * Creates an empty quotation file instance.
     * </p>
     *
     * @return an empty {@link QuotationFile} instance
     */
    private static QuotationFile empty() {
        return new QuotationFile();
    }

    /**
     * <p>
     * Builder used to create instances of {@link QuotationFile}.
     * </p>
     */
    public static class Builder extends AbstractSalesDocumentFileBuilder<QuoteTemplateData, Quotation, QuotationFile> {

        private static final String QUOTE_IMAGE_PREVIEW_URL;

        private final QuotationURLGenerator quotationURLGenerator;

        static {
            final Configurations urlConfigs = ConfHolder.getInstance().getProperties(QuotationFields.QUOTATION).getNestedConfigs(QuotationFields.URL);

            QUOTE_IMAGE_PREVIEW_URL = urlConfigs.getConfig(QuotationFields.IMAGE_PREVIEW_CONF);
        }

        /**
         * <p>
         * Creates a builder for the specified quotation.
         * </p>
         *
         * @param quotation the quotation for which the file is generated
         */
        public Builder(final Quotation quotation) {
            super(quotation);
            this.quotationURLGenerator = QuotationURLGenerator.getInstance();
        }

        /**
         * <p>
         * Returns the template name used to generate the quotation file.
         * </p>
         *
         * @return the quotation template name
         */
        @Override
        protected String getTemplateName() {
            return QuotationFields.QUOTATION;
        }

        /**
         * <p>
         * Generates the file path for the quotation using the document
         * organization, document identifier, and requested file extension.
         * </p>
         *
         * @param extension the extension of the generated file
         * @return the generated quotation file path
         */
        @Override
        protected String generateFilePath(final Extension extension) {
            return quotationURLGenerator.generateFilePath(document.orgId(), document.id(), extension);
        }

        /**
         * <p>
         * Returns the sections that must be excluded while generating the PDF.
         * </p>
         *
         * @return the set of sections excluded from PDF generation
         */
        @Override
        protected Set<Section> getPdfExcludedSections() {
            return Set.of(QuoteSection.TOP_BANNER);
        }

        /**
         * <p>
         * Creates a quotation file using the generated preview and download files.
         * </p>
         *
         * @param previewFile  the generated preview file
         * @param downloadFile the generated download file
         * @return the quotation file containing the generated files
         */
        @Override
        protected QuotationFile newDocumentFile(final File previewFile, final File downloadFile) {
            return new QuotationFile(previewFile, downloadFile);
        }

        /**
         * <p>
         * Creates an empty quotation file.
         * </p>
         *
         * @return an empty quotation file
         */
        @Override
        protected QuotationFile newEmptyDocumentFile() {
            return QuotationFile.empty();
        }

        /**
         * <p>
         * Resolves a quotation section using the specified section identifier.
         * </p>
         *
         * @param sectionId the quotation section identifier
         * @return the resolved quotation section
         */
        @Override
        protected Section resolveSection(final String sectionId) {
            return QuoteSection.get(sectionId);
        }

        /**
         * <p>
         * Returns the base section used for quotation template rendering.
         * </p>
         *
         * @return the base quotation section
         */
        @Override
        protected Section getBaseSection() {
            return QuoteSection.BASE_CARD_SECTION;
        }

        /**
         * <p>
         * Creates a custom quotation section with the specified identifier and name.
         * </p>
         *
         * @param sectionId the custom section identifier
         * @param name      the custom section name
         * @return the created custom quotation section
         */
        @Override
        protected Section createCustomSection(final String sectionId, final String name) {
            return new QuoteCustomSection(sectionId, name);
        }

        /**
         * <p>
         * Determines whether the specified section represents the quotation issue date.
         * </p>
         *
         * @param section the section to evaluate
         * @return {@code true} if the section represents the issue date;
         * {@code false} otherwise
         */
        @Override
        protected boolean isIssueDateSection(final Section section) {
            return section == QuoteSection.HEADER_ISSUE_DATE;
        }

        /**
         * <p>
         * Builds the template attributes for a quotation line item.
         * </p>
         *
         * @param productJson the JSON object containing the product details
         * @return the template attributes for the product
         */
        @Override
        protected Map<String, Object> buildProductAttributes(final JsonObject productJson) {
            return Map.of(
                    TemplateField.NAME, getValue(Key.NAME, productJson),
                    TemplateField.QUANTITY, getValue(Key.QUANTITY, productJson),
                    TemplateField.UNIT_PRICE, getValue(Key.UNIT_PRICE, productJson),
                    TemplateField.DISCOUNT, getValue(Key.DISCOUNT, productJson),
                    TemplateField.TOTAL_VALUE, getValue(QuoteJsonKey.TERM_TOTAL, productJson)
            );
        }

        /**
         * <p>
         * Builds the template data required to render the quotation.
         * </p>
         *
         * @param quotation the quotation for which template data is generated
         * @return the template build request containing the quotation template data
         */
        @Override
        protected TemplateBuildRequest<QuoteTemplateData> buildTemplateDataHolder(final Quotation quotation) {
            final CrmUser owner = quotation.getOwner();
            final JsonArray quoteJson = quotation.getQuoteJson().getJsonArray(QuoteJsonKey.FORM_ATTRIBUTES);
            final QuoteTemplateData quoteTemplateData = new QuoteTemplateData();

            loadSectionData(quoteJson, quoteTemplateData);
            loadSalesPersonData(quoteTemplateData, owner);

            quoteTemplateData.addValue(QuoteTemplateKey.CURRENCY, quotation.getAmount().getCurrency().getSymbol())
                    .addValue(TemplateField.DOWNLOAD_URL, quotationURLGenerator.generateDownloadURL(quotation.orgId(), owner.id(), quotation.id()));

            final DefaultTemplateBuildRequest<QuoteTemplateData> templateDataHolder =
                    new DefaultTemplateBuildRequest<>(
                            Map.of(QuoteTemplateKey.QUOTATION, quoteTemplateData));

            loadProcessors(quoteJson, templateDataHolder);

            return templateDataHolder;
        }

        /**
         * <p>
         * Builds a URL for accessing the quotation attachment image with the specified identifier.
         * </p>
         *
         * <p>
         * This implementation encodes the attachment metadata (organization ID, user ID, quote ID,
         * image ID, and field name) into the URL as a Base64-encoded JSON object, which is then
         * used to generate an absolute file preview URL specific to the quotation context.
         * </p>
         *
         * @param attachmentId Represents the identifier of the attachment image.
         * @param field        Represents the field name associated with the attachment.
         * @return Absolute URL string for accessing the quotation attachment image.
         */
        @Override
        protected String buildAttachmentURL(final long attachmentId, final String field) {
            final Long orgId = document.orgId();
            final Long userId = document.getOwner().id();
            final Long quoteId = document.id();
            final JsonObject imageData = Json.object();

            imageData.put(QuotationFields.ORG_ID, orgId);
            imageData.put(QuotationFields.USER_ID, userId);
            imageData.put(QuotationFields.QUOTE_ID, quoteId);
            imageData.put(QuotationFields.IMAGE_ID, attachmentId);
            imageData.put(QuotationFields.FIELD, field);

            return HttpUtil.buildAbsoluteUrl(StringUtil.join(StringUtil.Delimiter.FORWARD_SLASH, QUOTE_IMAGE_PREVIEW_URL,
                    Base64.getEncoder().encodeToString(imageData.getBytes())));
        }

        /**
         * <p>
         * Loads the quotation form sections into the quotation template data.
         * </p>
         *
         * @param quoteJson         the quotation form attributes
         * @param quoteTemplateData the quotation template data to populate
         */
        private void loadSectionData(final JsonArray quoteJson, final QuoteTemplateData quoteTemplateData) {
            final Collection<Map<String, Object>> customSectionsData = new ArrayList<>();
            final int sectionCount = quoteJson.size();

            for (int sectionIndex = 0; sectionIndex < sectionCount; sectionIndex++) {
                final JsonObject sectionJson = quoteJson.getJsonObject(sectionIndex);

                switch (sectionJson.getString(Key.TYPE)) {
                    case QuoteSectionType.HEADER ->
                            quoteTemplateData.addValue(QuoteTemplateKey.HEADER, buildHeaderData(sectionJson));
                    case QuoteSectionType.PARTIES ->
                            quoteTemplateData.addValue(QuoteTemplateKey.PARTIES, buildPartiesData(sectionJson));
                    case QuoteSectionType.SUMMARY ->
                            quoteTemplateData.addValue(QuoteTemplateKey.SUMMARY, buildSummaryData(sectionJson));
                    case QuoteSectionType.LINE_ITEMS ->
                            quoteTemplateData.addValue(QuoteTemplateKey.LINE_ITEMS, buildLineItemsData(sectionJson));
                    case QuoteSectionType.PAYMENT ->
                            quoteTemplateData.addValue(QuoteTemplateKey.PAYMENT, buildPaymentData(sectionJson));
                    case QuoteSectionType.ACCEPTANCE ->
                            quoteTemplateData.addValue(QuoteTemplateKey.ACCEPTANCE, buildAcceptanceData(sectionJson));
                    case QuoteSectionType.CUSTOM_SECTION -> customSectionsData.add(buildCustomSectionData(sectionJson));
                }
            }

            quoteTemplateData.addValue(QuoteTemplateKey.CUSTOM_SECTIONS, customSectionsData);
        }

        /**
         * <p>
         * Builds the template data for a custom quotation section.
         * </p>
         *
         * @param sectionJson the custom section JSON
         * @return the custom section template data
         */
        private Map<String, Object> buildCustomSectionData(final JsonObject sectionJson) {
            final Map<String, Object> customSectionData = new HashMap<>();
            final String sectionId = getValue(Key.SECTION_ID, sectionJson);
            final JsonArray jsonArray = sectionJson.getJsonArray(Key.DETAILS);
            final int detailCount = jsonArray.size();

            for (int detailIndex = 0; detailIndex < detailCount; detailIndex++) {
                final JsonObject jsonObject = jsonArray.getJsonObject(detailIndex);

                if (QuoteSectionType.TITLE_DETAIL.equals(jsonObject.getString(Key.TYPE))) {
                    final JsonObject descriptionJson = jsonObject.getJsonObject(Key.DESCRIPTION);

                    customSectionData.put(QuoteTemplateKey.ID, sectionId);
                    customSectionData.put(TemplateField.TITLE, getSingleValue(jsonObject));
                    customSectionData.put(QuoteTemplateKey.CONTENT, getValue(Key.VALUE, descriptionJson));
                }
            }

            return Map.copyOf(customSectionData);
        }

        /**
         * <p>
         * Builds the template data for the quotation acceptance section.
         * </p>
         *
         * @param sectionJson the acceptance section JSON
         * @return the acceptance section template data
         */
        private Map<String, Object> buildAcceptanceData(final JsonObject sectionJson) {
            final Map<String, Object> acceptanceData = new HashMap<>();
            final JsonArray jsonArray = sectionJson.getJsonArray(Key.DETAILS);
            final int detailCount = jsonArray.size();

            for (int detailIndex = 0; detailIndex < detailCount; detailIndex++) {
                final JsonObject jsonObject = jsonArray.getJsonObject(detailIndex);

                switch (jsonObject.getString(Key.TYPE)) {
                    case QuoteSectionType.TITLE_DETAIL -> {
                        final JsonObject descriptionJson = jsonObject.getJsonObject(Key.DESCRIPTION);

                        acceptanceData.put(TemplateField.TITLE, getSingleValue(jsonObject));
                        acceptanceData.put(TemplateField.DESCRIPTION, getValue(Key.VALUE, descriptionJson));
                    }
                    case QuoteSectionType.BUYER_SUB_TITLE ->
                            acceptanceData.put(QuoteTemplateKey.BUYER_TITLE, getSingleValue(jsonObject));
                    case QuoteSectionType.BUYER_CONTACTS ->
                            acceptanceData.put(QuoteTemplateKey.BUYERS, collectNames(jsonObject));
                    case QuoteSectionType.SELLER_SUB_TITLE ->
                            acceptanceData.put(QuoteTemplateKey.SELLER_TITLE, getSingleValue(jsonObject));
                    case QuoteSectionType.SELLER_CONTACTS ->
                            acceptanceData.put(QuoteTemplateKey.SELLERS, collectNames(jsonObject));
                }
            }

            return acceptanceData;
        }

        /**
         * <p>
         * Collects the names from the values contained in the specified JSON object.
         * </p>
         *
         * @param jsonObject the JSON object containing the contact values
         * @return the collection of contact names
         */
        private List<String> collectNames(final JsonObject jsonObject) {
            final List<String> names = new ArrayList<>();
            final JsonArray values = jsonObject.getJsonArray(Key.VALUES);
            final int count = values.size();

            for (int index = 0; index < count; index++) {
                names.add(getValue(Key.NAME, values.getJsonObject(index)));
            }

            return names;
        }

        /**
         * <p>
         * Builds the template data for the quotation payment section.
         * </p>
         *
         * <p>
         * The payment terms are resolved from the billing value and added
         * to the payment template data.
         * </p>
         *
         * @param sectionJson the payment section JSON
         * @return the payment section template data
         */
        private Map<String, Object> buildPaymentData(final JsonObject sectionJson) {
            final Map<String, Object> paymentData = new HashMap<>();
            final JsonArray jsonArray = sectionJson.getJsonArray(Key.DETAILS);
            final int detailCount = jsonArray.size();

            for (int detailIndex = 0; detailIndex < detailCount; detailIndex++) {
                final JsonObject jsonObject = jsonArray.getJsonObject(detailIndex);

                switch (jsonObject.getString(Key.TYPE)) {
                    case QuoteSectionType.TITLE_DETAIL ->
                            paymentData.put(TemplateField.TITLE, getLabelName(jsonObject));
                    case QuoteSectionType.BILLING -> {
                        final String billingValue = getSingleValue(jsonObject);
                        final PaymentTerms paymentTerms = PaymentTerms.get(NumberUtils.parseNumber(billingValue).intValue());

                        paymentData.put(TemplateField.DESCRIPTION, getLabelName(jsonObject));
                        paymentData.put(TemplateField.VALUE, paymentTerms.getName());
                    }
                }
            }

            return Map.copyOf(paymentData);
        }

        /**
         * <p>
         * Builds the template data for the quotation summary section.
         * </p>
         *
         * @param sectionJson the summary section JSON
         * @return the summary section template data
         */
        private Map<String, Object> buildSummaryData(final JsonObject sectionJson) {
            final Map<String, Object> summaryData = new HashMap<>();

            loadTitleAndValue(sectionJson.getJsonArray(QuoteJsonKey.SUMMARY_DETAILS), summaryData);

            return Map.copyOf(summaryData);
        }

        /**
         * <p>
         * Builds the template data for the quotation parties section.
         * </p>
         *
         * <p>
         * Both seller and buyer details are loaded into the parties template data.
         * </p>
         *
         * @param sectionJson the parties section JSON
         * @return the parties section template data
         */
        private Map<String, Object> buildPartiesData(final JsonObject sectionJson) {
            final Map<String, Object> partiesData = new HashMap<>();

            loadPartyDetails(true, sectionJson.getJsonArray(QuoteJsonKey.SELLER_DETAILS), partiesData);
            loadPartyDetails(false, sectionJson.getJsonArray(QuoteJsonKey.BUYER_DETAILS), partiesData);

            return Map.copyOf(partiesData);
        }

        /**
         * <p>
         * Loads seller or buyer party details into the supplied template data.
         * </p>
         *
         * @param isSeller         whether the party represents the seller
         * @param partyDetailsJson the party details JSON
         * @param partiesData      the parties template data to populate
         */
        private void loadPartyDetails(final boolean isSeller, final JsonArray partyDetailsJson, final Map<String, Object> partiesData) {
            final int partyCount = partyDetailsJson.size();

            for (int partyIndex = 0; partyIndex < partyCount; partyIndex++) {
                final JsonObject jsonObject =
                        partyDetailsJson.getJsonObject(partyIndex);

                switch (jsonObject.getString(Key.TYPE)) {
                    case QuoteSectionType.TITLE_DETAIL -> partiesData.put(isSeller ? QuoteTemplateKey.SELLER_TITLE
                            : QuoteTemplateKey.BUYER_TITLE, getSingleValue(jsonObject));
                    case QuoteSectionType.SELLER_COMPANY, QuoteSectionType.BUYER_COMPANY ->
                            loadCompanyDetails(jsonObject, partiesData, isSeller);
                    case QuoteSectionType.SELLER_CONTACT -> loadSellerContact(jsonObject, partiesData);
                    case QuoteSectionType.BUYER_CONTACTS -> loadBuyerContacts(jsonObject, partiesData);
                }
            }
        }

        /**
         * <p>
         * Loads the company name and address from the company details JSON.
         * </p>
         *
         * @param companyJson the company details JSON
         * @param partiesData the parties template data to populate
         * @param seller      whether the company represents the seller
         */
        private void loadCompanyDetails(final JsonObject companyJson, final Map<String, Object> partiesData, final boolean seller) {
            final JsonArray values = companyJson.getJsonArray(Key.VALUES);
            final int valueCount = values.size();

            for (int valueIndex = 0; valueIndex < valueCount; valueIndex++) {
                final JsonObject valueJson = values.getJsonObject(valueIndex);

                switch (valueJson.getString(Key.TYPE)) {
                    case SectionType.ORGANISATION, SectionType.COMPANY -> partiesData.put(
                            seller ? QuoteTemplateKey.SELLER_COMPANY_NAME : QuoteTemplateKey.BUYER_COMPANY_NAME, getValue(Key.VALUE, valueJson));

                    case SectionType.ADDRESS -> partiesData.put(
                            seller ? QuoteTemplateKey.SELLER_COMPANY_ADDRESS : QuoteTemplateKey.BUYER_COMPANY_ADDRESS, getValue(Key.VALUE, valueJson));
                }
            }
        }

        /**
         * <p>
         * Loads the seller contact title, name, and email into the parties data.
         * </p>
         *
         * @param contactJson the seller contact JSON
         * @param partiesData the parties template data to populate
         */
        private void loadSellerContact(final JsonObject contactJson, final Map<String, Object> partiesData) {
            partiesData.put(QuoteTemplateKey.SELLER_CONTACT_TITLE, getLabelName(contactJson));

            final JsonArray sellerContactValues = contactJson.getJsonArray(Key.VALUES);
            final int contactCount = sellerContactValues.size();

            for (int contactIndex = 0; contactIndex < contactCount; contactIndex++) {
                final JsonObject valueJson = sellerContactValues.getJsonObject(contactIndex);

                switch (valueJson.getString(Key.TYPE)) {
                    case SectionType.USER ->
                            partiesData.put(QuoteTemplateKey.SELLER_CONTACT_NAME, getValue(Key.VALUE, valueJson));
                    case SectionType.EMAIL ->
                            partiesData.put(QuoteTemplateKey.SELLER_CONTACT_EMAIL, getValue(Key.VALUE, valueJson));
                }
            }
        }

        /**
         * <p>
         * Loads the buyer contact title, names, and email addresses into the
         * parties template data.
         * </p>
         *
         * @param contactJson the buyer contacts JSON
         * @param partiesData the parties template data to populate
         */
        private void loadBuyerContacts(final JsonObject contactJson, final Map<String, Object> partiesData) {

            partiesData.put(QuoteTemplateKey.BUYER_CONTACTS_TITLE, getLabelName(contactJson));

            final Collection<Map<String, String>> buyerContacts = new ArrayList<>();
            final JsonArray buyerContactValues = contactJson.getJsonArray(Key.VALUES);
            final int contactCount = buyerContactValues.size();

            for (int contactIndex = 0; contactIndex < contactCount; contactIndex++) {
                final JsonObject valueJson =
                        buyerContactValues.getJsonObject(contactIndex);
                final Map<String, String> contact = new HashMap<>();

                contact.put(TemplateField.NAME, getValue(Key.NAME, valueJson));
                contact.put(TemplateField.EMAIL, getValue(Key.EMAIL, valueJson));

                buyerContacts.add(contact);
            }

            partiesData.put(QuoteTemplateKey.BUYER_CONTACTS, buyerContacts);
        }

        /**
         * <p>
         * Builds the template data for the quotation header section.
         * </p>
         *
         * @param sectionJson the header section JSON
         * @return the header section template data
         */
        private Map<String, Object> buildHeaderData(final JsonObject sectionJson) {
            final Map<String, Object> headerData = new HashMap<>();
            final JsonArray detailsJson = sectionJson.getJsonArray(Key.DETAILS);
            final JsonArray logoJson = sectionJson.getJsonArray(Key.LOGO);

            loadLogoAndCompanyData(logoJson, headerData);
            loadTitleAndValue(detailsJson, headerData);

            return Map.copyOf(headerData);
        }

        /**
         * <p>
         * Section {@code type} values only used within quotation JSON.
         * </p>
         */
        private static final class QuoteSectionType {

            private static final String HEADER = "HEADER";
            private static final String PARTIES = "PARTIES";
            private static final String SUMMARY = "SUMMARY";
            private static final String LINE_ITEMS = "LINE_ITEMS";
            private static final String PAYMENT = "PAYMENT";
            private static final String ACCEPTANCE = "ACCEPTANCE";
            private static final String CUSTOM_SECTION = "CUSTOM_SECTION";
            private static final String TITLE_DETAIL = "TITLE";
            private static final String SELLER_COMPANY = "SELLER_COMPANY";
            private static final String SELLER_CONTACT = "SELLER_CONTACT";
            private static final String BUYER_COMPANY = "BUYER_COMPANY";
            private static final String BUYER_CONTACTS = "BUYER_CONTACTS";
            private static final String BILLING = "BILLING";
            private static final String BUYER_SUB_TITLE = "BUYER_SUB_TITLE";
            private static final String SELLER_SUB_TITLE = "SELLER_SUB_TITLE";
            private static final String SELLER_CONTACTS = "SELLER_CONTACTS";

            private QuoteSectionType() {
            }
        }

        /**
         * <p>
         * JSON property names only used within quotation JSON.
         * </p>
         */
        private static final class QuoteJsonKey {

            private static final String FORM_ATTRIBUTES = "formAttributes";
            private static final String SUMMARY_DETAILS = "summaryDetails";
            private static final String SELLER_DETAILS = "sellerDetails";
            private static final String BUYER_DETAILS = "buyerDetails";
            private static final String TERM_TOTAL = "termTotal";

            private QuoteJsonKey() {
            }
        }

        /**
         * <p>
         * Template model key names only used within quotation rendering.
         * </p>
         */
        private static final class QuoteTemplateKey {

            private static final String ID = "id";
            private static final String QUOTATION = "quotation";
            private static final String HEADER = "header";
            private static final String PARTIES = "parties";
            private static final String SUMMARY = "summary";
            private static final String LINE_ITEMS = "lineItems";
            private static final String PAYMENT = "payment";
            private static final String ACCEPTANCE = "acceptance";
            private static final String CUSTOM_SECTIONS = "customSections";
            private static final String SALES_PERSON_NAME = "salesPersonName";
            private static final String SALES_PERSON_EMAIL = "salesPersonEmail";
            private static final String SALES_PERSON_PROFILE_URL = "salesPersonProfileUrl";
            private static final String CURRENCY = "currency";
            private static final String CONTENT = "content";
            private static final String SELLER_TITLE = "sellerTitle";
            private static final String SELLER_COMPANY_NAME = "sellerCompanyName";
            private static final String SELLER_COMPANY_ADDRESS = "sellerCompanyAddress";
            private static final String SELLER_CONTACT_TITLE = "sellerContactTitle";
            private static final String SELLER_CONTACT_NAME = "sellerContactName";
            private static final String SELLER_CONTACT_EMAIL = "sellerContactEmail";
            private static final String BUYER_TITLE = "buyerTitle";
            private static final String BUYER_COMPANY_NAME = "buyerCompanyName";
            private static final String BUYER_COMPANY_ADDRESS = "buyerCompanyAddress";
            private static final String BUYER_CONTACTS_TITLE = "buyerContactsTitle";
            private static final String BUYER_CONTACTS = "buyerContacts";
            private static final String SELLERS = "sellers";
            private static final String BUYERS = "buyers";

            private QuoteTemplateKey() {
            }
        }
    }
}