package com.tlc.crm.core.features.invoice.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.invoice.fields.InvoiceFields;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Represents an invoice record, including its identity, owning deal, contacts and company
 * links, monetary amount, lifecycle {@link Status}, and audit metadata. Validation constraints
 * are grouped per {@link Group.Create}, {@link Group.Update}, and {@link Group.Delete}
 * operations.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class Invoice implements TlcModel {

    private static final String INVOICE_NUMBER_FORMAT = "INV-%03d";

    @NotNull(groups = {Group.Delete.class, Group.Update.class}, message = "i18n.invoice.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    @NotNull(groups = {Group.Create.class}, message = "i18n.invoice.invalid.template.id")
    private Long templateId;

    private Integer number;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.invoice.invalid.status")
    private Status status;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.invoice.invalid.invoice.json")
    private JsonObject invoiceJson;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.invoice.invalid.owner")
    private CrmUser owner;

    @NotNull(groups = {Group.Create.class}, message = "i18n.invoice.invalid.created.by")
    private CrmUser createdBy;

    @NotNull(groups = {Group.Update.class}, message = "i18n.invoice.invalid.updated.by")
    private CrmUser updatedBy;

    private Long htmlFileId;
    private Long pdfFileId;

    private String fullyQualifiedNumber;
    private String invoiceJsonAsString;
    private JsonObject schemaJson;
    private InvoiceType invoiceType;
    private Currency currency;

    private DateTime modifiedAt;
    private Date issueAt;
    private Date dueBy;

    private Deal deal;
    private Contact contact;
    private Company company;
    private Collection<InvoiceLineItem> invoiceLineItems;

    public Invoice() {
    }

    @Override
    public Long id() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(final Long templateId) {
        this.templateId = templateId;
    }

    public Integer getNumber() {
        return number;
    }

    /**
     * <p>
     * Sets the invoice number and derives the {@link #fullyQualifiedNumber} from it, formatted
     * as {@code INV-%03d}.
     * </p>
     *
     * @param number Represents the raw invoice number.
     */
    public void setNumber(final Integer number) {
        this.number = number;
        this.fullyQualifiedNumber = Objects.isNull(number) ? null : String.format(INVOICE_NUMBER_FORMAT, number);
    }

    public String getFullyQualifiedNumber() {
        return fullyQualifiedNumber;
    }

    public String getInvoiceJsonAsString() {
        return invoiceJsonAsString;
    }

    /**
     * <p>
     * Sets the invoice JSON from its string form and keeps the parsed {@link #invoiceJson}
     * representation in sync.
     * </p>
     *
     * @param invoiceJsonAsString Represents the invoice JSON as a string.
     */
    public void setInvoiceJsonAsString(final String invoiceJsonAsString) {
        this.invoiceJson = Json.object(invoiceJsonAsString);
        this.invoiceJsonAsString = invoiceJsonAsString;
    }

    public JsonObject getInvoiceJson() {
        return invoiceJson;
    }

    /**
     * <p>
     * Sets the invoice JSON and keeps the {@link #invoiceJsonAsString} representation in sync.
     * </p>
     *
     * @param invoiceJson Represents the invoice JSON as a {@link JsonObject}.
     */
    public void setInvoiceJson(final JsonObject invoiceJson) {
        this.invoiceJson = invoiceJson;
        this.invoiceJsonAsString = invoiceJson == null ? null : invoiceJson.toString();
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public CrmUser getOwner() {
        return owner;
    }

    public void setOwner(final CrmUser owner) {
        this.owner = owner;
    }

    public CrmUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final CrmUser createdBy) {
        this.createdBy = createdBy;
    }

    public CrmUser getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(final CrmUser updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Date getIssueAt() {
        return issueAt;
    }

    public void setIssueAt(final Date issueAt) {
        this.issueAt = issueAt;
    }

    public DateTime getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(final DateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public Long getHtmlFileId() {
        return htmlFileId;
    }

    public void setHtmlFileId(final Long htmlFileId) {
        this.htmlFileId = htmlFileId;
    }

    public Long getPdfFileId() {
        return pdfFileId;
    }

    public void setPdfFileId(final Long pdfFileId) {
        this.pdfFileId = pdfFileId;
    }

    public JsonObject getSchemaJson() {
        return schemaJson;
    }

    public void setSchemaJson(final JsonObject schemaJson) {
        this.schemaJson = schemaJson;
    }

    @Override
    public Object identity() {
        return null;
    }

    public InvoiceType getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(final InvoiceType invoiceType) {
        this.invoiceType = invoiceType;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(final Contact contact) {
        this.contact = contact;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(final Company company) {
        this.company = company;
    }

    public Collection<InvoiceLineItem> getInvoiceLineItems() {
        return invoiceLineItems;
    }

    public void setInvoiceLineItems(final Collection<InvoiceLineItem> invoiceLineItems) {
        this.invoiceLineItems = invoiceLineItems;
    }

    public Date getDueBy() {
        return dueBy;
    }

    public void setDueBy(final Date dueBy) {
        this.dueBy = dueBy;
    }

    public Deal getDeal() {
        return deal;
    }

    public void setDeal(final Deal deal) {
        this.deal = deal;
    }

    public Currency getCurrency() {
        if (Objects.nonNull(currency)) {
            return currency;
        }

        return Currency.getCurrencyById(invoiceJson.getJsonObject(InvoiceFields.SETTINGS)
                .getJsonObject(InvoiceFields.CURRENCY).getShort(InvoiceFields.ID));
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    /**
     * <p>
     * Represents the lifecycle state of an {@link Invoice}, from initial creation through
     * publishing and payment or voiding.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    public enum Status {

        DRAFT((short) 1, "i18n.invoice.status.draft"),
        PUBLISHED((short) 2, "i18n.invoice.status.published"),
        NOT_PUBLISHED((short) 3, "i18n.invoice.status.not.published"),
        VIEWED((short) 4, "i18n.invoice.status.viewed"),
        VOIDED((short) 5, "i18n.invoice.status.voided"),
        ;

        private final short id;
        private final String i18nKey;

        /**
         * <p>
         * Constructs the invoice status.
         * </p>
         *
         * @param id      Represents the persisted status id.
         * @param i18nKey Represents the i18n key.
         */
        Status(final short id, final String i18nKey) {
            this.id = id;
            this.i18nKey = i18nKey;
        }

        public short getId() {
            return id;
        }

        /**
         * <p>
         * Gets the i18n key.
         * </p>
         *
         * @return I18n key.
         */
        public String getI18nKey() {
            return i18nKey;
        }

        /**
         * <p>
         * Resolves a {@link Status} from its persisted numeric id.
         * </p>
         *
         * @param id Represents the persisted status id.
         * @return the matching {@link Status}
         * @throws IllegalArgumentException if {@code id} does not match a known status
         */
        public static Status get(final short id) {
            return switch (id) {
                case 1 -> DRAFT;
                case 2 -> PUBLISHED;
                case 3 -> NOT_PUBLISHED;
                case 4 -> VIEWED;
                case 5 -> VOIDED;
                default -> throw new IllegalArgumentException("Invalid invoice status id: " + id);
            };
        }

        public boolean isDraft() {
            return this == DRAFT;
        }
    }
}