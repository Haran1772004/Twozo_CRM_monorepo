package com.tlc.crm.webform.api.model;

import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Update;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A Model which represents the form element details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class FormElement extends FormFieldProperty {

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.formElement.invalid.name")
    @Size(max = 10, groups = {Create.class, Update.class}, message = "i18n.webForm.formElement.invalid.name.max.size")
    private String name;
    private Category category;

    public FormElement() {
        super(Type.ELEMENT);
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Category getCategory() {
        return getCategoryBasedOnName();
    }

    private Category getCategoryBasedOnName() {
        return switch (this.name.toLowerCase()) {
            case "subheader" -> Category.SUBHEADER;
            case "text" -> Category.TEXT;
            case "image" -> Category.IMAGE;
            default -> null;
        };
    }

    public enum Category {
        SUBHEADER("subheader"),
        TEXT("text"),
        IMAGE("image"),
        ;

        private final String name;

        Category(final String name) {
            this.name = name;
        }

        /**
         * <p>
         * Gets the name of the state
         * </p>
         *
         * @return the name of State
         */
        public String getName() {
            return this.name;
        }
    }
}
