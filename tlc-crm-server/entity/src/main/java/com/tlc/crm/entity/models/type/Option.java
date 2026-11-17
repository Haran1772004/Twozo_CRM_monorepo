package com.tlc.crm.entity.models.type;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Objects;

/**
 * Model that represents the dropdown option
 *
 * @author sundar
 * @version 1.0
 */
public class Option implements TlcModel, Serializable {

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.entity.field.type.dropdown.option.id.required")
    @Min(value = 1, groups = {Group.Update.class, Group.Delete.class}, message = "i18n.entity.field.type.dropdown.option.id.invalid")
    private Long id;

    @Min(value = 1, groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.entity.field.type.dropdown.option.orgId.invalid")
    private Long orgId;

    @NotEmpty(groups = {Group.Create.class, Group.Update.class}, message = "i18n.entity.field.type.dropdown.option.notEmpty")
    private String option;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.entity.field.type.dropdown.option.fieldId.required")
    @Min(value = 1, groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.entity.field.type.dropdown.option.fieldId.invalid")
    private Long fieldId;

    private boolean isDefault;

    private boolean staticOption;

    private boolean editable;

    private boolean isDraggable;

    private int order;

    public Option() {
    }

    private Option(final Builder builder) {
        this.orgId = builder.getOrgId();
        this.id = builder.getId();
        this.option = builder.getOption();
        this.fieldId = builder.getFieldId();
        this.staticOption = builder.getStaticOption();
        this.order = builder.getOrder();
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public String getOption() {
        return option;
    }

    public void setOption(String option) {
        this.option = option;
    }

    public boolean isStaticOption() {
        return staticOption;
    }

    public void setStaticOption(boolean staticOption) {
        this.staticOption = staticOption;
    }

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(Long fieldId) {
        this.fieldId = fieldId;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(final int order) {
        this.order = order;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(final boolean editable) {
        this.editable = editable;
    }

    public void setDefault(final boolean isDefault) {
        this.isDefault = isDefault;
    }

    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public Object identity() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, fieldId, orgId);
    }

    /**
     *{@inheritDoc}
     *
     * @param object Represents the object to be compared
     * @return true if given object is equals to the current object
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (object instanceof Option) {
            return this.hashCode() == object.hashCode();
        }
        return false;
    }

    public static class Builder {
        private Long id;
        private Long orgId;
        private String option;
        private Long fieldId;
        private boolean staticOption;
        private int order;

        /**
         * <p>
         * Sets the id of the {@link Option}
         * </p>
         *
         * @param id Represents the of the option.
         * @return Instance of {@link Builder}
         */
        public Builder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * <p>
         * Sets the org id of the {@link Option}
         * </p>
         *
         * @param orgId Represents the org id of the option.
         * @return Instance of {@link Builder}
         */
        public Builder orgId(final Long orgId) {
            this.orgId = orgId;
            return this;
        }

        /**
         * <p>
         * Sets the option value of the {@link Option}
         * </p>
         *
         * @param option Represents the option value.
         * @return Instance of {@link Builder}
         */
        public Builder option(final String option) {
            this.option = option;
            return this;
        }

        /**
         * <p>
         * Sets the field id of the {@link Option}
         * </p>
         *
         * @param fieldId Represents the field id of the option
         * @return Instance of {@link Builder}
         */
        public Builder fieldId(final Long fieldId) {
            this.fieldId = fieldId;
            return this;
        }

        /**
         * <p>
         * Sets the type of the {@link Option}
         * </p>
         *
         * @param staticOption Reprsents the option type.
         * @return Instance of {@link Builder}
         */
        public Builder staticOption(final boolean staticOption) {
            this.staticOption = staticOption;
            return this;
        }

        /**
         * <p>
         * Sets the order of the {@link Option}
         * </p>
         *
         * @param order Represents the order of the option.
         * @return Instance of {@link Builder}
         */
        public Builder order(final int order) {
            this.order = order;
            return this;
        }

        /**
         * <p>
         * Builds the {@link Option} object.
         * </p>
         *
         * @return Instance of {@link Option}
         */
        public Option build() {
            return new Option(this);
        }

        /**
         * <p>
         * Gets the id of the {@link Option}
         * </p>
         *
         * @return Option id.
         */
        public Long getId() {
            return this.id;
        }

        /**
         * <p>
         * Gets the org id of the {@link Option}
         * </p>
         *
         * @return Org id of the option
         */
        public Long getOrgId() {
            return this.orgId;
        }

        /**
         * <p>
         * Gets the field id of the {@link Option}.
         * </p>
         *
         * @return Field id of the option.
         */
        public Long getFieldId() {
            return this.fieldId;
        }

        /**
         * <p>
         * Gets the value of the {@link Option}
         * </p>
         *
         * @return Option value.
         */
        public String getOption() {
            return this.option;
        }

        /**
         * <p>
         * Gets the type of the {@link Option}.
         * </p>
         *
         * @return Type of the option
         */
        public boolean getStaticOption() {
            return this.staticOption;
        }

        /**
         * <p>
         * Gets the order of the {@link Option}
         * </p>
         *
         * @return Order of the option.
         */
        public int getOrder() {
            return this.order;
        }
    }
}
