package com.tlc.crm.workflow.api.model.filter;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;

/**
 * <p>
 * A model that represents the {@link FilterGroup}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public enum FilterGroup {

    TRIGGERS((short) 1, "triggers"),
    ACTIONS((short) 2, "actions"),
    STATUS((short) 3, "status"),
    CREATED_BY((short) 4, "createdBy"),
    ;

    private final int id;
    private final String name;

    FilterGroup(final short id, final String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * <p>
     * Gets the filter group for the given id
     * </p>
     *
     * @param id Refers the id of the filter group
     * @return The {@link Type}
     */
    public static FilterGroup get(final short id) {
        return switch (id) {
            case 1 -> TRIGGERS;
            case 2 -> ACTIONS;
            case 3 -> STATUS;
            case 4 -> CREATED_BY;
            default -> throw ErrorCode.get(WorkflowErrorCodes.FILTER_GROUP_NOT_FOUND, "i18n.workflow.filter.filterGroup.invalid");
        };
    }
}
