package com.tlc.crm.core.features.quote.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.internal.quote.status.QuotationErrorCodes;

/**
 * Represents the supported effective date calculation types for a quotation.
 *
 * <p>This enum defines how the effective date of a quotation is determined:
 * <ul>
 *     <li>{@link #ON_AGREEMENT} - Effective date starts on the agreement date.</li>
 *     <li>{@link #CUSTOM_DATE} - Effective date is based on a user-provided date.</li>
 *     <li>{@link #DELAYED_START_DAYS} - Effective date starts after a specified number of days.</li>
 *     <li>{@link #DELAYED_START_MONTHS} - Effective date starts after a specified number of months.</li>
 * </ul>
 *
 * <p>Each type is mapped to a unique numeric identifier used for persistence
 * and data conversion.</p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum EffectiveDateValueType {

    ON_AGREEMENT(1),
    CUSTOM_DATE(2),
    DELAYED_START_DAYS(3),
    DELAYED_START_MONTHS(4),
    ;

    private final int id;

    /**
     * Creates an effective date value type with the given identifier.
     *
     * @param id Numeric identifier associated with the effective date value type.
     */
    EffectiveDateValueType(final int id) {
        this.id = id;
    }

    /**
     * Returns the unique identifier of this effective date value type.
     *
     * @return Numeric identifier of the effective date value type.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the effective date value type matching the given identifier.
     *
     * @param id Identifier of the required effective date value type.
     * @return Matching {@link EffectiveDateValueType} value.
     * @throws RuntimeException if the provided identifier does not map to any supported effective date value type.
     */
    public static EffectiveDateValueType get(final int id) {
        return switch (id) {
            case 1 -> ON_AGREEMENT;
            case 2 -> CUSTOM_DATE;
            case 3 -> DELAYED_START_DAYS;
            case 4 -> DELAYED_START_MONTHS;
            default -> throw ErrorCode.get(QuotationErrorCodes.INVALID_EFFECTIVE_DATE_VALUE_TYPE, "Effective date value type not found");
        };
    }
}