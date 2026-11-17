package com.tlc.payment.gateway.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.payment.gateway.internal.status.PaymentGatewayErrorCodes;

import java.util.Optional;

/**
 * Enum representing various payment statuses.
 * Provides commonly used payment states for transactions.
 *
 * @author Sathish
 * @version 1.0
 */
public enum PaymentStatus {

    SUCCESS((short) 1),
    FAILED((short) 2),
    REFUND_INITIATED((short) 3),
    REFUNDED((short) 4),
    DISPUTE((short) 5),
    PAYMENT_PENDING((short) 6),
    ;

    private final short id;

    PaymentStatus(final short id) {
        this.id = id;
    }

    public static PaymentStatus get(final short id) {
        return switch (id) {
            case 1 -> SUCCESS;
            case 2 -> FAILED;
            case 3 -> REFUND_INITIATED;
            case 4 -> REFUNDED;
            case 5 -> DISPUTE;
            case 6 -> PAYMENT_PENDING;
            default -> throw ErrorCode.get(PaymentGatewayErrorCodes.INVALID_PAYMENT_STATUS);
        };
    }

    public short getId() {
        return id;
    }

    /**
     * Maps a string status from external sources to the corresponding PaymentStatus enum.
     *
     * @param status String status value (e.g., "success", "failed")
     * @return Matching PaymentStatus enum or null if no match
     */
    public static Optional<PaymentStatus> getByName(final String status) {
        return Optional.ofNullable(switch (status.toLowerCase()) {
            case "success" -> SUCCESS;
            case "failed", "failure" -> FAILED;
            case "refund_initiated" -> REFUND_INITIATED;
            case "refunded" -> REFUNDED;
            case "dispute" -> DISPUTE;
            default -> null;
        });
    }
}
