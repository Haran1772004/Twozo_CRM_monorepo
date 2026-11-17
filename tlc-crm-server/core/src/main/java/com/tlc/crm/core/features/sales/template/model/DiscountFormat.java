package com.tlc.crm.core.features.sales.template.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.internal.quote.status.QuotationErrorCodes;

/**
 * Defines the supported discount display formats for quotations.
 *
 * <p>This enum represents how discount values should be displayed in quotation
 * documents, such as showing only the amount, only the percentage, or both
 * percentage and amount.
 *
 * <p>Each format is identified by a unique numeric ID that is used for
 * persistence and retrieval.
 *
 * @author Sathish
 * @version 1.0
 */
public enum DiscountFormat {

    SHOW_AS_AMOUNT(1),
    SHOW_AS_PERCENTAGE(2),
    SHOW_AS_PERCENTAGE_AND_AMOUNT(3),
    ;

    private final int id;

    /**
     * Creates a discount format with the given identifier.
     *
     * @param id Numeric identifier associated with the discount format.
     */
    DiscountFormat(final int id) {
        this.id = id;
    }

    /**
     * Returns the unique identifier of this discount format.
     *
     * @return Numeric identifier of the discount format.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the discount format matching the given identifier.
     *
     * @param id Identifier of the required discount format.
     * @return Matching {@link DiscountFormat} value.
     * @throws RuntimeException if the provided identifier does not map to any supported discount format.
     */
    public static DiscountFormat get(final int id) {
        return switch (id) {
            case 1 -> SHOW_AS_AMOUNT;
            case 2 -> SHOW_AS_PERCENTAGE;
            case 3 -> SHOW_AS_PERCENTAGE_AND_AMOUNT;
            default -> throw ErrorCode.get(QuotationErrorCodes.INVALID_DISCOUNT_FORMAT, "Discount format not found");
        };
    }
}