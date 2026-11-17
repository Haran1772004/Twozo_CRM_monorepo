package com.tlc.crm.core.list;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.features.invoice.service.InvoiceURLGenerator;
import com.tlc.crm.core.features.invoice.fields.InvoiceFields;
import com.tlc.crm.core.features.invoice.manager.InvoiceManager;
import com.tlc.crm.core.features.invoice.manager.InvoiceManagerImpl;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.OMIVINVOICE;
import com.tlc.crm.entity.list.model.ListPreviewFetchRequest;
import com.tlc.crm.entity.list.model.ListPreviewResponse;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.file.client.converter.AttachmentConverter;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.file.common.models.Attachment;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the service for retrieving invoice related preview details to be loaded into the invoice list response.
 * Along with the invoice preview/download URLs, the linked buyer contact's email (when available) is nested
 * under the {@link ContactFields#CONTACT} key so that contact related fields stay grouped together.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class InvoiceListPreviewRetriever extends AbstractListPreviewRetriever {

    private final InvoiceManager invoiceManager;
    private final InvoiceURLGenerator invoiceURLGenerator;
    private final AttachmentService attachmentService;
    private final AttachmentConverter attachmentConverter;

    private InvoiceListPreviewRetriever() {
        this.invoiceManager = InvoiceManagerImpl.getInstance();
        this.invoiceURLGenerator = InvoiceURLGenerator.getInstance();
        this.attachmentService = AttachmentService.getInstance();
        this.attachmentConverter = AttachmentConverter.getInstance();
    }

    private static class Instance {
        private static final InvoiceListPreviewRetriever INVOICE_LIST_PREVIEW_RETRIEVER = new InvoiceListPreviewRetriever();
    }

    /**
     * <p>
     * Gets the instance of {@link InvoiceListPreviewRetriever}
     * </p>
     *
     * @return The instance of {@link InvoiceListPreviewRetriever}
     */
    public static InvoiceListPreviewRetriever getInstance() {
        return InvoiceListPreviewRetriever.Instance.INVOICE_LIST_PREVIEW_RETRIEVER;
    }

    /**
     * {@inheritDoc}
     *
     * @param listPreviewFetchRequest Represents the {@link ListPreviewFetchRequest}
     * @return The optional {@link ListPreviewResponse} containing, for every resolvable invoice id, the
     * preview URL, download URL, pdf file metadata (when present) and (when the invoice is linked to a contact)
     * the contact's id, name and email nested under the contact key
     */
    @Override
    public Optional<ListPreviewResponse> fetchPreviewResponse(final ListPreviewFetchRequest listPreviewFetchRequest) {
        final Collection<Long> invoiceIds = listPreviewFetchRequest.entityIds();
        final long orgId = listPreviewFetchRequest.orgId();
        final DataContainer dataContainer = invoiceManager.getPreviewDetails(orgId, invoiceIds);

        if (dataContainer.isEmpty()) {
            return Optional.empty();
        }
        final Table invoiceTable = invoiceManager.getTable();
        final Table contactTable = Table.get(MCOCONTACT.TABLE);
        final Table emailTable = Table.get(OMETEMAIL.TABLE);
        final Map<String, Map<String, Object>> entityDataMap = new HashMap<>();
        final long userId = listPreviewFetchRequest.userId();
        final Map<Long, JsonObject> attachmentJsonById = loadAttachmentJsonById(dataContainer, invoiceTable, invoiceIds);

        for (final Long invoiceId : invoiceIds) {
            final Row invoiceRow = dataContainer.getRow(invoiceTable, new WhereClause(Criteria.eq(invoiceTable.getPKColumn(), invoiceId)));

            if (Objects.isNull(invoiceRow)) {
                continue;
            }
            final Map<String, Object> entityFieldsMap = new HashMap<>();
            final Long contactId = invoiceRow.get(OMIVINVOICE.CONTACT_ID);

            entityFieldsMap.put(InvoiceFields.INVOICE_PREVIEW_URL, invoiceURLGenerator.generatePreviewURL(orgId, userId, invoiceId));
            entityFieldsMap.put(InvoiceFields.INVOICE_DOWNLOAD_URL, invoiceURLGenerator.generateDownloadURL(orgId, userId, invoiceId));

            final Long pdfFileId = invoiceRow.get(OMIVINVOICE.PDF_FILE_ID);

            if (Objects.nonNull(pdfFileId)) {
                final Object pdfFile = attachmentJsonById.get(pdfFileId);

                if (Objects.nonNull(pdfFile)) {
                    entityFieldsMap.put(InvoiceFields.PDF_FILE, pdfFile);
                }
            }

            if (Objects.nonNull(contactId)) {
                final Row contactRow = dataContainer.getRow(contactTable, contactId);
                final Map<String, Object> contactDataMap = buildContactPreviewData(contactRow, dataContainer, emailTable);

                if (!contactDataMap.isEmpty()) {
                    entityFieldsMap.put(ContactFields.CONTACT, contactDataMap);
                }
            }
            entityDataMap.put(String.valueOf(invoiceId), entityFieldsMap);
        }

        return Optional.of(new ListPreviewResponse(entityDataMap));
    }

    /**
     * <p>
     * Builds the preview data of the buyer contact from the supplied contact row. The data contains
     * the contact's id and name, and the primary email's id, value and type when the email is available.
     * If the contact row is null, an empty map is returned.
     * </p>
     *
     * @param contactRow    Represents the {@link Row} of the contact.
     * @param dataContainer Represents the {@link DataContainer} holding the contact and email rows.
     * @param emailTable    Represents the {@link Table} of the email.
     * @return The map containing the contact preview data, or an empty map if the contact row is null.
     */
    private Map<String, Object> buildContactPreviewData(final Row contactRow, final DataContainer dataContainer,
                                                        final Table emailTable) {
        final Map<String, Object> contactDataMap = new HashMap<>();

        if (Objects.isNull(contactRow)) {
            return contactDataMap;
        }
        contactDataMap.put(ContactFields.ID, contactRow.getPKValue());
        contactDataMap.put(ContactFields.NAME, Contact.getFullName(
                contactRow.get(MCOCONTACT.FIRST_NAME),
                contactRow.get(MCOCONTACT.LAST_NAME)));
        final Long emailId = contactRow.get(MCOCONTACT.PRIMARY_EMAIL);

        if (Objects.nonNull(emailId)) {
            final Row emailRow = dataContainer.getRow(emailTable, emailId);

            if (Objects.nonNull(emailRow)) {
                final Object emailValue = emailRow.get(OMETEMAIL.VALUE);
                final Integer emailType = emailRow.get(OMETEMAIL.TYPE);

                if (Objects.nonNull(emailValue)) {
                    final Map<String, Object> emailDataMap = new HashMap<>();

                    emailDataMap.put(ContactFields.ID, emailId);
                    emailDataMap.put(ContactFields.VALUE, emailValue);
                    emailDataMap.put(ContactFields.TYPE, Objects.nonNull(emailType) ? Email.Type.valueOf(emailType) : null);
                    contactDataMap.put(ContactFields.EMAIL, emailDataMap);
                }
            }
        }

        return contactDataMap;
    }

    /**
     * <p>
     * Collects the pdf file ids of the supplied invoices and loads their attachments in a single call.
     * Each attachment is converted into its JSON representation and mapped against its attachment id.
     * If none of the invoices has a pdf file, an empty map is returned.
     * </p>
     *
     * @param dataContainer Represents the {@link DataContainer} holding the invoice rows.
     * @param invoiceTable  Represents the {@link Table} of the invoice.
     * @param invoiceIds    Represents the ids of the invoices whose pdf files are to be loaded.
     * @return The map of attachment id to its JSON representation.
     */
    private Map<Long, JsonObject> loadAttachmentJsonById(final DataContainer dataContainer, final Table invoiceTable,
                                                         final Collection<Long> invoiceIds) {
        final Collection<Long> pdfFileIds = new HashSet<>();

        for (final Long invoiceId : invoiceIds) {
            final Row invoiceRow = dataContainer.getRow(invoiceTable, new WhereClause(Criteria.eq(invoiceTable.getPKColumn(), invoiceId)));

            if (Objects.isNull(invoiceRow)) {
                continue;
            }
            final Long pdfFileId = invoiceRow.get(OMIVINVOICE.PDF_FILE_ID);

            if (Objects.nonNull(pdfFileId)) {
                pdfFileIds.add(pdfFileId);
            }
        }

        if (pdfFileIds.isEmpty()) {
            return Map.of();
        }

        return attachmentService.getAttachments(new ArrayList<>(pdfFileIds)).stream()
                .collect(Collectors.toMap(Attachment::id, attachmentConverter::convert, (left, right) -> left));
    }
}