package com.tlc.crm.plan.and.billing.api.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.plan.and.billing.internal.status.SubscriptionErrorCodes;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.payment.gateway.models.PaymentStatus;

import java.util.Objects;

/**
 * Represents the details of a single invoice including transaction information, billing amount, status, and metadata.
 *
 * @author Sathish
 * @version 1.0
 */
public class Invoice {

    private Long id;
    private Long orgId;
    private Long transactionId;
    private double amount;
    private String invoiceNumber;
    private String pdfUrl;
    private Date billingDate;
    private Currency currency;
    private Status status;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(final Long transactionId) {
        this.transactionId = transactionId;
    }

    public double getAmount() {
        return Math.round(amount * 10.0) / 10.0;
    }

    public void setAmount(final double amount) {
        this.amount = amount;
    }

    public String getInvoiceNumber() {
        return invoiceNumber = Objects.isNull(invoiceNumber) ? Objects.isNull(id) || Objects.isNull(orgId)
                ? null : String.format("#%s%s", orgId, id) : invoiceNumber;
    }

    public void setInvoiceNumber(final String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public Date getBillingDate() {
        return billingDate;
    }

    public void setBillingDate(final Date billingDate) {
        this.billingDate = billingDate;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(final Currency currency) {
        this.currency = currency;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(final String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public enum Status {

        PAID((short) 1),
        FAILED((short) 2),
        ;

        private final short id;

        Status(final short id) {
            this.id = id;
        }

        public static Status get(final short id) {
            return switch (id) {
                case 1 -> PAID;
                case 2 -> FAILED;
                default -> throw ErrorCode.get(SubscriptionErrorCodes.INVALID_INVOICE_STATUS);
            };
        }

        public short getId() {
            return id;
        }

        public static Status getByPaymentStatus(final PaymentStatus paymentStatus) {
            return switch (paymentStatus) {
                case SUCCESS -> PAID;
                default -> FAILED;
            };
        }
    }
}
