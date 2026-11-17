package com.tlc.crm.core.features.invoice.model;

import com.tlc.attachment.io.Extension;
import com.tlc.attachment.io.File;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.template.model.DefaultTemplateBuildRequest;
import com.tlc.commons.template.model.Section;
import com.tlc.commons.template.model.TemplateBuildRequest;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.features.sales.template.model.AbstractSalesDocumentFile;
import com.tlc.crm.core.features.sales.template.model.AbstractSalesDocumentFileBuilder;
import com.tlc.crm.core.features.sales.template.model.PaymentTerms;
import com.tlc.crm.core.features.invoice.service.InvoiceURLGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Represents the generated files associated with an invoice.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class InvoiceFile extends AbstractSalesDocumentFile {

    private InvoiceFile() {
        this(null, null);
    }

    private InvoiceFile(final File previewFile, final File downloadFile) {
        super(previewFile, downloadFile);
    }

    /**
     * <p>
     * Creates an empty {@link InvoiceFile}.
     * </p>
     *
     * @return Empty {@link InvoiceFile}.
     */
    private static InvoiceFile empty() {
        return new InvoiceFile();
    }

    /**
     * Builder used to create instances of {@link InvoiceFile}.
     */
    public static class Builder extends AbstractSalesDocumentFileBuilder<InvoiceTemplateData, Invoice, InvoiceFile> {

        private static final String INVOICE_TEMPLATE_NAME = "invoice";

        private final InvoiceURLGenerator invoiceURLGenerator;

        /**
         * <p>
         * Creates an invoice file builder for the given invoice.
         * </p>
         *
         * @param invoice Holds the {@link Invoice} attributes.
         */
        public Builder(final Invoice invoice) {
            super(invoice);
            this.invoiceURLGenerator = InvoiceURLGenerator.getInstance();
        }

        /**
         * {@inheritDoc}
         *
         * @return Invoice template name.
         */
        @Override
        protected String getTemplateName() {
            return INVOICE_TEMPLATE_NAME;
        }

        /**
         * {@inheritDoc}
         *
         * @param extension Represents the file extension.
         * @return Generated invoice file path.
         */
        @Override
        protected String generateFilePath(final Extension extension) {
            return invoiceURLGenerator.generateFilePath(document.orgId(), document.id(), extension);
        }

        /**
         * {@inheritDoc}
         *
         * @return Sections excluded from PDF generation.
         */
        @Override
        protected Set<Section> getPdfExcludedSections() {
            return Set.of();
        }

        /**
         * {@inheritDoc}
         *
         * @param previewFile  Represents the generated preview file.
         * @param downloadFile Represents the generated download file.
         * @return {@link InvoiceFile} containing the generated files.
         */
        @Override
        protected InvoiceFile newDocumentFile(final File previewFile, final File downloadFile) {
            return new InvoiceFile(previewFile, downloadFile);
        }

        /**
         * {@inheritDoc}
         *
         * @return Empty {@link InvoiceFile}.
         */
        @Override
        protected InvoiceFile newEmptyDocumentFile() {
            return InvoiceFile.empty();
        }

        /**
         * {@inheritDoc}
         *
         * @param sectionId Represents the invoice section identifier.
         * @return Resolved {@link InvoiceSection}.
         */
        @Override
        protected Section resolveSection(final String sectionId) {
            return InvoiceSection.get(sectionId);
        }

        /**
         * {@inheritDoc}
         *
         * @return Base invoice section.
         */
        @Override
        protected Section getBaseSection() {
            return InvoiceSection.BASE_CARD_SECTION;
        }

        /**
         * {@inheritDoc}
         *
         * @param sectionId Represents the custom section identifier.
         * @param name      Represents the custom section name.
         * @return Custom invoice section.
         */
        @Override
        protected Section createCustomSection(final String sectionId, final String name) {
            throw new UnsupportedOperationException("Invoices do not support custom sections: " + sectionId);
        }

        /**
         * {@inheritDoc}
         *
         * @param productJson Represents the product JSON data.
         * @return Product attributes used for invoice template rendering.
         */
        @Override
        protected Map<String, Object> buildProductAttributes(final JsonObject productJson) {
            return Map.of(
                    TemplateField.NAME, getValue(Key.NAME, productJson),
                    TemplateField.QUANTITY, getValue(Key.QUANTITY, productJson),
                    TemplateField.UNIT_PRICE, getValue(Key.UNIT_PRICE, productJson),
                    TemplateField.DISCOUNT, getValue(Key.DISCOUNT, productJson),
                    InvoiceTemplateKey.TAX_VALUE, getValue(InvoiceJsonKey.TAX, productJson),
                    TemplateField.TOTAL_VALUE, getValue(InvoiceJsonKey.TOTAL, productJson)
            );
        }

        /**
         * <p>
         * Builds the template data required for rendering the invoice.
         * </p>
         *
         * @param invoice Represents the invoice whose template data is being built.
         * @return {@link TemplateBuildRequest} containing invoice template data.
         */
        @Override
        protected TemplateBuildRequest<InvoiceTemplateData> buildTemplateDataHolder(final Invoice invoice) {
            final CrmUser owner = invoice.getOwner();
            final JsonArray invoiceJson = invoice.getInvoiceJson().getJsonArray(InvoiceJsonKey.FORM_ATTRIBUTES);
            final InvoiceTemplateData invoiceTemplateData = new InvoiceTemplateData();

            loadSectionData(invoiceJson, invoiceTemplateData);
            loadSalesPersonData(invoiceTemplateData, invoice.getOwner());
            invoiceTemplateData.addValue(InvoiceTemplateKey.CURRENCY, invoice.getCurrency().getSymbol())
                    .addValue(TemplateField.DOWNLOAD_URL, invoiceURLGenerator.generateDownloadURL(invoice.orgId(), owner.id(), invoice.id()));
            final DefaultTemplateBuildRequest<InvoiceTemplateData> templateDataHolder =
                    new DefaultTemplateBuildRequest<>(Map.of(InvoiceTemplateKey.INVOICE, invoiceTemplateData));

            loadProcessors(invoiceJson, templateDataHolder);

            return templateDataHolder;
        }

        /**
         * <p>
         * Loads invoice section data into the template data holder.
         * </p>
         *
         * @param invoiceJson         Represents the invoice form attributes.
         * @param invoiceTemplateData Holds the invoice template data.
         */
        private void loadSectionData(final JsonArray invoiceJson, final InvoiceTemplateData invoiceTemplateData) {
            final int sectionCount = invoiceJson.size();

            for (int sectionIndex = 0; sectionIndex < sectionCount; sectionIndex++) {
                final JsonObject sectionJson = invoiceJson.getJsonObject(sectionIndex);

                switch (sectionJson.getString(Key.TYPE)) {
                    case InvoiceSectionType.HEADER -> invoiceTemplateData.addValue(InvoiceTemplateKey.HEADER,
                            buildHeaderData(sectionJson));

                    case InvoiceSectionType.PARTIES -> invoiceTemplateData.addValue(InvoiceTemplateKey.PARTIES,
                            buildPartiesData(sectionJson));

                    case InvoiceSectionType.LINE_ITEMS -> invoiceTemplateData.addValue(InvoiceTemplateKey.LINE_ITEMS,
                            buildLineItemsData(sectionJson));
                }
            }
        }

        /**
         * <p>
         * Builds the invoice header data including logo, company information,
         * invoice date, due date, payment terms, and PO number.
         * </p>
         *
         * @param sectionJson Represents the invoice header section.
         * @return Map containing invoice header data.
         */
        private Map<String, Object> buildHeaderData(final JsonObject sectionJson) {
            final Map<String, Object> headerData = new HashMap<>();
            final JsonArray detailsJson = sectionJson.getJsonArray(Key.DETAILS);
            final JsonArray logoJson = sectionJson.getJsonArray(Key.LOGO);
            final int detailCount = detailsJson.size();

            loadLogoAndCompanyData(logoJson, headerData);

            for (int detailIndex = 0; detailIndex < detailCount; detailIndex++) {
                final JsonObject detailJson = detailsJson.getJsonObject(detailIndex);

                switch (detailJson.getString(Key.TYPE)) {
                    case InvoiceSectionType.TITLE_DETAIL ->
                            headerData.put(TemplateField.TITLE, getSingleValue(detailJson));
                    case InvoiceSectionType.DATE -> {
                        headerData.put(InvoiceTemplateKey.INVOICE_DATE_TITLE, getLabelName(detailJson));
                        headerData.put(InvoiceTemplateKey.INVOICE_DATE_VALUE, convertToDateString(getSingleValue(detailJson)));
                    }
                    case InvoiceSectionType.DUE_DATE -> {
                        headerData.put(InvoiceTemplateKey.DUE_DATE_TITLE, getLabelName(detailJson));
                        loadDueDateValues(detailJson, headerData);
                    }
                    case InvoiceSectionType.PO_NUMBER -> {
                        headerData.put(InvoiceTemplateKey.PO_NUMBER_TITLE, getLabelName(detailJson));
                        headerData.put(InvoiceTemplateKey.PO_NUMBER_VALUE, getSingleValue(detailJson));
                    }
                }
            }

            return Map.copyOf(headerData);
        }

        /**
         * <p>
         * Loads the payment terms and due date values from the due date section.
         * </p>
         *
         * <p>
         * The due date section contains both payment terms and the resolved date,
         * so both values are processed separately.
         * </p>
         *
         * @param dueDateJson Represents the due date JSON object.
         * @param headerData  Holds the invoice header data.
         */
        private void loadDueDateValues(final JsonObject dueDateJson,
                                       final Map<String, Object> headerData) {
            final JsonArray valuesJson = dueDateJson.getJsonArray(Key.VALUES);
            final int valueCount = valuesJson.size();

            for (int valueIndex = 0; valueIndex < valueCount; valueIndex++) {
                final JsonObject valueJson = valuesJson.getJsonObject(valueIndex);

                switch (valueJson.getString(Key.TYPE)) {
                    case InvoiceSectionType.PAYMENT_TERMS -> headerData.put(InvoiceTemplateKey.PAYMENT_TERMS_VALUE,
                            PaymentTerms.get(NumberUtils.parseNumber(getValue(Key.VALUE, valueJson)).intValue()).getName());

                    case InvoiceSectionType.DATE -> headerData.put(InvoiceTemplateKey.DUE_DATE_VALUE,
                            convertToDateString(getValue(Key.VALUE, valueJson)));
                }
            }
        }

        /**
         * <p>
         * Builds the invoice parties data including buyer and shipping details.
         * </p>
         *
         * @param sectionJson Represents the invoice parties section.
         * @return Map containing invoice parties data.
         */
        private Map<String, Object> buildPartiesData(final JsonObject sectionJson) {
            final Map<String, Object> partiesData = new HashMap<>();

            loadBuyerDetails(sectionJson.getJsonArray(InvoiceJsonKey.BUYER_DETAILS), partiesData);
            loadShippingDetails(sectionJson.getJsonArray(InvoiceJsonKey.SHIPPING_DETAILS), partiesData);

            return Map.copyOf(partiesData);
        }

        /**
         * <p>
         * Loads buyer company, contact, billing address, and buyer title details.
         * </p>
         *
         * @param buyerDetailsJson Represents the buyer details JSON array.
         * @param partiesData      Holds the invoice parties data.
         */
        private void loadBuyerDetails(final JsonArray buyerDetailsJson,
                                      final Map<String, Object> partiesData) {
            final int detailCount = buyerDetailsJson.size();

            for (int detailIndex = 0; detailIndex < detailCount; detailIndex++) {
                final JsonObject detailJson = buyerDetailsJson.getJsonObject(detailIndex);

                switch (detailJson.getString(Key.TYPE)) {
                    case InvoiceSectionType.TITLE_DETAIL ->
                            partiesData.put(InvoiceTemplateKey.BUYER_TITLE, getSingleValue(detailJson));
                    case InvoiceSectionType.BUYER_COMPANY -> loadCompanyDetails(detailJson, partiesData);
                    case InvoiceSectionType.BUYER_CONTACT -> loadBuyerContact(detailJson, partiesData);
                    case InvoiceSectionType.BILLING_ADDRESS -> partiesData.put(InvoiceTemplateKey.BILLING_ADDRESS,
                            getSingleValue(detailJson));
                }
            }
        }

        /**
         * <p>
         * Loads buyer company information from the company JSON section.
         * </p>
         *
         * @param companyJson Represents the buyer company JSON object.
         * @param partiesData Holds the invoice parties data.
         */
        private void loadCompanyDetails(final JsonObject companyJson,
                                        final Map<String, Object> partiesData) {
            final JsonArray values = companyJson.getJsonArray(Key.VALUES);
            final int valueCount = values.size();

            for (int valueIndex = 0; valueIndex < valueCount; valueIndex++) {
                final JsonObject valueJson = values.getJsonObject(valueIndex);

                switch (valueJson.getString(Key.TYPE)) {
                    case SectionType.COMPANY, SectionType.ORGANISATION ->
                            partiesData.put(InvoiceTemplateKey.BUYER_COMPANY_NAME,
                                    getValue(Key.VALUE, valueJson));
                    case SectionType.ADDRESS -> partiesData.put(InvoiceTemplateKey.BUYER_COMPANY_ADDRESS,
                            getValue(Key.VALUE, valueJson));
                    default -> {
                        if ("Domain".equalsIgnoreCase(getValue(Key.NAME, valueJson))) {
                            partiesData.put(InvoiceTemplateKey.BUYER_COMPANY_DOMAIN, getValue(Key.VALUE, valueJson));
                        }
                    }
                }
            }
        }

        /**
         * <p>
         * Loads buyer contact information including contact name and email address.
         * </p>
         *
         * @param contactJson Represents the buyer contact JSON object.
         * @param partiesData Holds the invoice parties data.
         */
        private void loadBuyerContact(final JsonObject contactJson,
                                      final Map<String, Object> partiesData) {
            partiesData.put(
                    InvoiceTemplateKey.BUYER_CONTACT_TITLE,
                    getLabelName(contactJson));

            final JsonArray values = contactJson.getJsonArray(Key.VALUES);
            final int valueCount = values.size();

            for (int valueIndex = 0; valueIndex < valueCount; valueIndex++) {
                final JsonObject valueJson = values.getJsonObject(valueIndex);

                switch (valueJson.getString(Key.TYPE)) {
                    case InvoiceSectionType.CONTACT ->
                            partiesData.put(InvoiceTemplateKey.BUYER_CONTACT_NAME, getValue(Key.VALUE, valueJson));

                    case SectionType.EMAIL ->
                            partiesData.put(InvoiceTemplateKey.BUYER_CONTACT_EMAIL, getValue(Key.VALUE, valueJson));
                }
            }
        }

        /**
         * <p>
         * Loads shipping address information into the invoice parties data.
         * </p>
         *
         * @param shippingDetailsJson Represents the shipping details JSON array.
         * @param partiesData         Holds the invoice parties data.
         */
        private void loadShippingDetails(final JsonArray shippingDetailsJson, final Map<String, Object> partiesData) {
            final int detailCount = shippingDetailsJson.size();

            for (int detailIndex = 0; detailIndex < detailCount; detailIndex++) {
                final JsonObject detailJson = shippingDetailsJson.getJsonObject(detailIndex);

                if (InvoiceSectionType.SHIPPING_ADDRESS.equals(detailJson.getString(Key.TYPE))) {
                    partiesData.put(InvoiceTemplateKey.SHIPPING_ADDRESS_TITLE, getLabelName(detailJson));
                    partiesData.put(InvoiceTemplateKey.SHIPPING_ADDRESS, getSingleValue(detailJson));
                }
            }
        }

        /**
         * <p>
         * Defines the section type values used within invoice JSON.
         * </p>
         *
         * @author Mohamed Yasar K
         * @version 1.0
         */
        private static final class InvoiceSectionType {

            private static final String HEADER = "HEADER";
            private static final String PARTIES = "PARTIES";
            private static final String LINE_ITEMS = "LINE_ITEMS";
            private static final String TITLE_DETAIL = "TITLE";
            private static final String INVOICE_DATE = "INVOICE_DATE";
            private static final String DUE_DATE = "DUE_DATE";
            private static final String PO_NUMBER = "PO_NUMBER";
            private static final String PAYMENT_TERMS = "PAYMENT_TERMS";
            private static final String DATE = "DATE";
            private static final String BUYER_COMPANY = "BUYER_COMPANY";
            private static final String BUYER_CONTACT = "BUYER_CONTACT";
            private static final String CONTACT = "CONTACT";
            private static final String BILLING_ADDRESS = "BILLING_ADDRESS";
            private static final String SHIPPING_ADDRESS = "SHIPPING_ADDRESS";

            private InvoiceSectionType() {
            }
        }

        /**
         * <p>
         * Defines the JSON property names used within invoice JSON.
         * </p>
         *
         * @author Mohamed Yasar K
         * @version 1.0
         */
        private static final class InvoiceJsonKey {

            private static final String FORM_ATTRIBUTES = "formAttributes";
            private static final String BUYER_DETAILS = "buyerDetails";
            private static final String SHIPPING_DETAILS = "shippingDetails";
            private static final String TAX = "tax";
            private static final String TOTAL = "total";

            private InvoiceJsonKey() {
            }
        }

        /**
         * <p>
         * Defines the template model key names used during invoice rendering.
         * </p>
         *
         * @author Mohamed Yasar K
         * @version 1.0
         */
        private static final class InvoiceTemplateKey {

            private static final String INVOICE = "invoice";
            private static final String HEADER = "header";
            private static final String PARTIES = "parties";
            private static final String LINE_ITEMS = "lineItems";
            private static final String CURRENCY = "currency";
            private static final String INVOICE_DATE_TITLE = "invoiceDateTitle";
            private static final String INVOICE_DATE_VALUE = "invoiceDateValue";
            private static final String DUE_DATE_TITLE = "dueDateTitle";
            private static final String DUE_DATE_VALUE = "dueDateValue";
            private static final String PAYMENT_TERMS_VALUE = "paymentTermsValue";
            private static final String PO_NUMBER_TITLE = "poNumberTitle";
            private static final String PO_NUMBER_VALUE = "poNumberValue";
            private static final String BUYER_TITLE = "buyerTitle";
            private static final String BUYER_COMPANY_NAME = "buyerCompanyName";
            private static final String BUYER_COMPANY_DOMAIN = "buyerCompanyDomain";
            private static final String BUYER_COMPANY_ADDRESS = "buyerCompanyAddress";
            private static final String BUYER_CONTACT_TITLE = "buyerContactTitle";
            private static final String BUYER_CONTACT_NAME = "buyerContactName";
            private static final String BUYER_CONTACT_EMAIL = "buyerContactEmail";
            private static final String BILLING_ADDRESS = "billingAddress";
            private static final String SHIPPING_ADDRESS_TITLE = "shippingAddressTitle";
            private static final String SHIPPING_ADDRESS = "shippingAddress";
            private static final String TAX_VALUE = "taxValue";

            private InvoiceTemplateKey() {
            }
        }
    }
}