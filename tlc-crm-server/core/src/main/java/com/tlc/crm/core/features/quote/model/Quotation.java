package com.tlc.crm.core.features.quote.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;

import com.tlc.crm.core.internal.quote.status.QuotationErrorCodes;
import com.tlc.crm.entity.models.type.Monetary;

import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Represents a quotation entity in the CRM system. A quotation is a formal business proposal
 * that contains pricing information for products or services offered to a prospective customer.
 * It is associated with a deal and includes detailed quote information in JSON format.
 * </p>
 *
 * <p>
 * Key features of a quotation:
 * <ul>
 *   <li>Linked to a specific deal and customer contacts</li>
 *   <li>Contains structured quote data in JSON format</li>
 *   <li>Has lifecycle status (DRAFT, PUBLISHED, etc.)</li>
 *   <li>Includes financial information (amount, currency)</li>
 *   <li>Has expiration date for validity</li>
 *   <li>Tracks ownership and audit information</li>
 * </ul>
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see Deal
 * @see Contact
 * @see Company
 */
public class Quotation implements TlcModel {

    private static final String QUOTATION_NUMBER_FORMAT = "QT-%03d";

    @NotNull(groups = {Group.Delete.class, Group.Update.class}, message = "i18n.quotation.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    @NotNull(groups = {Group.Create.class}, message = "i18n.quotation.invalid.template.id")
    private Long templateId;

    @NotEmpty(groups = {Group.Create.class, Group.Update.class}, message = "i18n.quotation.invalid.name")
    @Size(max = 255, groups = {Group.Create.class, Group.Update.class}, message = "i18n.quotation.invalid.name.size")
    private String name;

    private Integer number;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.quotation.invalid.deal")
    private Deal deal;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.quotation.invalid.status")
    private Status status;

    @NotNull(groups = {Group.Create.class}, message = "i18n.quotation.invalid.uuid")
    private String uuid;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.quotation.invalid.quote.json")
    private JsonObject quoteJson;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.quotation.invalid.amount")
    private Monetary amount;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.quotation.invalid.owner")
    private CrmUser owner;

    @NotNull(groups = {Group.Create.class}, message = "i18n.quotation.invalid.created.by")
    private CrmUser createdBy;

    @NotNull(groups = {Group.Update.class}, message = "i18n.quotation.invalid.updated.by")
    private CrmUser updatedBy;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.quotation.invalid.expiration.time")
    private DateTime expirationTime;

    private Long htmlFileId;

    private Long pdfFileId;

    private String quoteJsonAsString;

    private Collection<Contact> contacts;

    private Company company;

    private DateTime createdAt;

    private DateTime updatedAt;

    private JsonObject schemaJson;

    private String fullyQualifiedNumber;

    private int actualContactsCount;

    private boolean isCompanyPresent;

    public Quotation() {
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

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(final Integer number) {
        this.number = number;
        this.fullyQualifiedNumber = Objects.isNull(number) ? null : String.format(QUOTATION_NUMBER_FORMAT, number);
    }

    public String getFullyQualifiedNumber() {
        return fullyQualifiedNumber;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public String getQuoteJsonAsString() {
        return quoteJsonAsString;
    }

    public void setQuoteJsonAsString(final String quoteJsonAsString) {
        this.quoteJson = Json.object(quoteJsonAsString);
        this.quoteJsonAsString = quoteJsonAsString;
    }

    public JsonObject getQuoteJson() {
        return quoteJson;
    }

    public void setQuoteJson(final JsonObject quoteJson) {
        this.quoteJson = quoteJson;
        this.quoteJsonAsString = quoteJson.toString();
    }

    public Monetary getAmount() {
        return amount;
    }

    public void setAmount(final Monetary amount) {
        this.amount = amount;
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

    public Deal getDeal() {
        return deal;
    }

    public void setDeal(final Deal deal) {
        this.deal = deal;
    }

    public Collection<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(final Collection<Contact> contacts) {
        this.contacts = contacts;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(final Company company) {
        this.company = company;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final DateTime createdAt) {
        this.createdAt = createdAt;
    }

    public DateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final DateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public DateTime getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(final DateTime expirationTime) {
        this.expirationTime = expirationTime;
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

    public boolean isCompanyPresent() {
        return isCompanyPresent;
    }

    public void setCompanyPresent(final boolean companyPresent) {
        isCompanyPresent = companyPresent;
    }

    public int getActualContactsCount() {
        return actualContactsCount;
    }

    public void setActualContactsCount(final int actualContactsCount) {
        this.actualContactsCount = actualContactsCount;
    }

    @Override
    public Object identity() {
        return null;
    }

    /**
     * Enumeration representing the various lifecycle states of a quotation.
     *
     * <p>Status progression typically follows: DRAFT -> PUBLISHED -> VIEWED</p>
     *
     * <ul>
     *   <li><strong>DRAFT</strong> - Quotation is being prepared and not yet ready for customer</li>
     *   <li><strong>PUBLISHED</strong> - Quotation has been finalized and sent to customer</li>
     *   <li><strong>NOT_PUBLISHED</strong> - Quotation exists but has not been sent to customer</li>
     *   <li><strong>VIEWED</strong> - Customer has viewed the published quotation</li>
     * </ul>
     */
    public enum Status {

        DRAFT((short) 1),
        PUBLISHED((short) 2),
        NOT_PUBLISHED((short) 3),
        VIEWED((short) 4),
        ;

        private final short id;

        Status(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        public static Status get(final short id) {
            return switch (id) {
                case 1 -> DRAFT;
                case 2 -> PUBLISHED;
                case 3 -> NOT_PUBLISHED;
                case 4 -> VIEWED;
                default -> throw ErrorCode.get(QuotationErrorCodes.INVALID_QUOTATION_STATUS, String.format("Status id: %d", id));
            };
        }
    }
}
