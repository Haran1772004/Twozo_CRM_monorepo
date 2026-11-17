package com.tlc.crm.auto.assignment.api.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.auto.assignment.internal.status.AutoAssignmentErrorCodes;

import com.tlc.crm.entity.fields.source.Source;

/**
 * Refers the {@code AssignmentModule}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum AssignmentModule {

    CONTACTS(Source.CONTACT, (short) 1),
    COMPANIES(Source.COMPANY, (short) 2),
    DEALS(Source.DEAL, (short) 3),
    ;

    private final short id;
    private final Source source;

    AssignmentModule(final Source source, final short id) {
        this.source = source;
        this.id = id;
    }

    public Source source() {
        return source;
    }

    public short getId() {
        return id;
    }

    /**
     * Gets the {@link AssignmentModule} by its id.
     *
     * @param id Refers the module id.
     * @return The {@link AssignmentModule}.
     */
    public static AssignmentModule get(final short id) {
        return switch (id) {
            case 1 -> CONTACTS;
            case 2 -> COMPANIES;
            case 3 -> DEALS;
            default ->
                    throw ErrorCode.get(AutoAssignmentErrorCodes.INVALID_MODULE, "i18n.auto.assignment.invalid.module");
        };
    }

    /**
     * Gets the {@link AssignmentModule} by its source.
     *
     * @param source Refers the source.
     * @return The {@link AssignmentModule}.
     */
    public static AssignmentModule getBySource(final Source source) {
        return switch (source) {
            case CONTACT -> CONTACTS;
            case COMPANY -> COMPANIES;
            case DEAL -> DEALS;
            default ->
                    throw ErrorCode.get(AutoAssignmentErrorCodes.INVALID_MODULE, "i18n.auto.assignment.invalid.module");
        };
    }
}
