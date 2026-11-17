package com.tlc.crm.auto.assignment.api.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.auto.assignment.internal.status.AutoAssignmentErrorCodes;

/**
 * Represents the {@link CriteriaConnector}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum CriteriaConnector {

    AND((short) 1),
    OR((short) 2),
    ;

    private final short id;

    CriteriaConnector(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * Gets the {@link CriteriaConnector} by its id.
     *
     * @param id Refers the id.
     * @return The {@link CriteriaConnector}.
     */
    public static CriteriaConnector get(final short id) {
        return switch (id) {
            case 1 -> AND;
            case 2 -> OR;
            default ->
                    throw ErrorCode.get(AutoAssignmentErrorCodes.INVALID_CONNECTOR, "i18n.auto.assignment.invalid.connector");
        };
    }
}
