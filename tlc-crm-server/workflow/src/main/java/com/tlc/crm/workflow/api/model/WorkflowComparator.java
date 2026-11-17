package com.tlc.crm.workflow.api.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.i18n.I18nKey;
import com.tlc.validator.type.Group;

import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;

import jakarta.validation.constraints.NotNull;

/**
 * A model that represents the {@link WorkflowComparator} data.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowComparator {

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.workflow.comparator.id.required")
    private Long id;
    private I18nKey name;
    private String aliasName;
    private boolean hasValue;
    private Category category;
    private final String idAsString;

    public WorkflowComparator(final Long id, final I18nKey name, final String aliasName, final boolean hasValue, final Category category) {
        this.id = id;
        this.idAsString = id.toString();
        this.name = name;
        this.aliasName = aliasName;
        this.hasValue = hasValue;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public I18nKey getName() {
        return name;
    }

    public void setName(final I18nKey name) {
        this.name = name;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(final String aliasName) {
        this.aliasName = aliasName;
    }

    public boolean hasValue() {
        return hasValue;
    }

    public void setHasValue(final boolean hasValue) {
        this.hasValue = hasValue;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(final Category category) {
        this.category = category;
    }

    public String idAsString() {
        return idAsString;
    }

    /**
     * Represents the comparator {@link Category} details.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    public enum Category {

        /**
         * Indicates the comparator has been specified for all the events.
         */
        ALL((short) 1),

        /**
         * Indicates that the comparator has been exclusively specified for create events.
         */
        CREATE((short) 2),

        /**
         * Indicates the comparators exclusively specified for update events.
         */
        UPDATE((short) 3),

        /**
         * Indicates the comparators exclusively specified for delete events.
         */
        DELETE((short) 4);

        private final short id;

        Category(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        /**
         * Gets the comparator category for given id.
         *
         * @param id Refers the category id.
         * @return {@link Category} of comparator.
         */
        public static Category get(final short id) {
            return switch (id) {
                case 1 -> ALL;
                case 2 -> CREATE;
                case 3 -> UPDATE;
                default -> throw ErrorCode.get(WorkflowErrorCodes.INVALID_COMPARATOR_CATEGORY, "i18n.workflow.invalid.comparator.category");
            };
        }
    }

}
