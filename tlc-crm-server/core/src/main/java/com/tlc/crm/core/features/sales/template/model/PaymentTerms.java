package com.tlc.crm.core.features.sales.template.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.internal.quote.status.QuotationErrorCodes;

/**
 * Defines the supported payment terms available for quotations.
 *
 * <p>Each payment term contains a unique identifier and a display name that
 * can be used while creating and rendering quotation payment details.
 *
 * <p>The enum provides a lookup method to retrieve a payment term by its configured identifier.
 *
 * @author Sathish
 * @version 1.0
 */
public enum PaymentTerms {

    DUE_ON_RECEIPT(1, "Due on receipt"),
    NET_7(2, "Net 7"),
    NET_10(3, "Net 10"),
    NET_15(3, "Net 15"),
    NET_30(3, "Net 30"),
    ;

    private final int id;
    private final String name;

    PaymentTerms(final int id, final String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Returns the unique identifier of this payment term.
     *
     * @return Payment term identifier.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the display name of this payment term.
     *
     * @return Payment term name.
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves a payment term by its identifier.
     *
     * @param id Identifier of the payment term.
     * @return Matching {@link PaymentTerms} value.
     * @throws ErrorCode if no payment term exists for the given identifier.
     */
    public static PaymentTerms get(final int id) {
        return switch (id) {
            case 1 -> DUE_ON_RECEIPT;
            case 2 -> NET_7;
            case 3 -> NET_10;
            case 4 -> NET_15;
            case 5 -> NET_30;
            default -> throw ErrorCode.get(QuotationErrorCodes.INVALID_PAYMENT_TERM, "Payment term not found");
        };
    }
}