package com.tlc.crm.workflow.api.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;

/**
 * Represents the {@link TemplateCategory}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum TemplateCategory {

    EMAIL((short) 1),
    ACTIVITY((short) 2),
    DEAL((short) 3),
    ;

    private final short id;

    TemplateCategory(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * Gets the category of the template.
     *
     * @param id Refers the template category id.
     * @return The {@link TemplateCategory}.
     */
    public static TemplateCategory get(final short id) {
        return switch (id) {
            case 1 -> EMAIL;
            case 2 -> ACTIVITY;
            case 3 -> DEAL;
            default -> throw ErrorCode.get(WorkflowErrorCodes.INVALID_TEMPLATE_CATEGORY, "i18n.workflow.invalid.template.category");
        };
    }
}
