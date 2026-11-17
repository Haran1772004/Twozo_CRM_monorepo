package com.tlc.crm.entity.models.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;

/**
 * <p>
 * A model that represents the {@link FieldVisibilityCriteria}
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public class FieldVisibilityCriteria {

    private Long id;
    private Long fieldId;
    private Long value;
    private Condition condition;
    private String validationMessage;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(final Long fieldId) {
        this.fieldId = fieldId;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(final Long value) {
        this.value = value;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(final Condition condition) {
        this.condition = condition;
    }

    public String getValidationMessage() {
        return validationMessage;
    }

    public void setValidationMessage(final String validationMessage) {
        this.validationMessage = validationMessage;
    }

    /**
     * <p>
     * Represents the conditions
     * </p>
     */
    public enum Condition {

        EQUALS((short) 1),
        CONTAINS((short) 2),
        STARTS_WITH((short) 3),
        NOT_EQUAL((short) 4),
        NOT_NULL((short) 5),
        ;

        private final short id;

        Condition(final short id) {
            this.id = id;
        }

        /**
         * <p>
         * Gets the condition id
         * </p>
         *
         * @return the type id
         */
        public short getId() {
            return this.id;
        }

        /**
         * Gets the {@link FieldVisibilityCriteria.Condition} by id
         *
         * @param id represents the id of the condition
         * @return {@link FieldVisibilityCriteria.Condition}
         */
        public static FieldVisibilityCriteria.Condition valueOf(final short id) {
            return switch (id) {
                case 1 -> EQUALS;
                case 2 -> CONTAINS;
                case 3 -> STARTS_WITH;
                case 4 -> NOT_EQUAL;
                case 5 -> NOT_NULL;
                default -> throw ErrorCode.get(EntityErrorCodes.INVALID_TYPE);
            };
        }
    }
}
