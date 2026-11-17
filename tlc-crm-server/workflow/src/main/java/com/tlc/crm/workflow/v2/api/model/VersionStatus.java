package com.tlc.crm.workflow.v2.api.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.workflow.v2.internal.status.WorkflowV2ErrorCodes;

/**
 * Represents workflow V2 version status.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public enum VersionStatus {

    PUBLISHED((short) 1),
    ARCHIVED((short) 2),
    ;

    private final short id;

    VersionStatus(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    public static VersionStatus get(final short id) {
        return switch (id) {
            case 1 -> PUBLISHED;
            case 2 -> ARCHIVED;
            default ->
                    throw ErrorCode.get(WorkflowV2ErrorCodes.INVALID_VERSION_STATUS, "i18n.workflow.invalid.version.status");
        };
    }
}
