package com.tlc.crm.core.features.quote.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.internal.quote.status.QuotationErrorCodes;

/**
 * Defines the supported subtotal display formats for quotations.
 *
 * <p>This enum represents how subtotal information should be displayed in a
 * quotation document. The available formats include displaying only the subtotal
 * amount or displaying the subtotal along with its detailed breakdown.
 *
 * <p>Each format is identified by a unique numeric ID used for storage and
 * retrieval.
 *
 * @author Sathish
 * @version 1.0
 */
public enum SubTotalFormat {

    SIMPLE_SUBTOTAL_AMOUNT(1),
    SUBTOTAL_WITH_BREAKDOWN(2),
    ;

    private final int id;

    /**
     * Creates a subtotal format with the specified identifier.
     *
     * @param id Numeric identifier associated with this subtotal format.
     */
    SubTotalFormat(final int id) {
        this.id = id;
    }

    /**
     * Returns the unique identifier of this subtotal format.
     *
     * @return Numeric identifier of the subtotal format.
     */
    public int getId() {
        return id;
    }

    /**
     * Retrieves the subtotal format matching the given identifier.
     *
     * @param id Identifier of the required subtotal format.
     * @return Matching {@link SubTotalFormat} value.
     * @throws RuntimeException if the given identifier does not match any supported subtotal format.
     */
    public static SubTotalFormat get(final int id) {
        return switch (id) {
            case 1 -> SIMPLE_SUBTOTAL_AMOUNT;
            case 2 -> SUBTOTAL_WITH_BREAKDOWN;
            default -> throw ErrorCode.get(QuotationErrorCodes.INVALID_SUB_TOTAL_FORMAT, "SubTotal format not found");
        };
    }
}