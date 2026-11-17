package com.tlc.crm.core.notification.email.template.api.model.fields;

import com.tlc.crm.core.notification.email.template.api.model.TemplateFieldHolder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * Represents the contact field types that are supported in template placeholders
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public enum TemplateContactFields implements TemplateFieldHolder {

    FIRSTNAME("firstName"),
    LASTNAME("lastName"),
    EMAIL("emails"),
    PHONE("phones"),
    COMPANY("company"),
    SALES_OWNER("salesOwner"),
    FULL_NAME("fullName"),
    CREATED_BY("createdBy"),
    UPDATED_BY("updatedBy"),
    ;

    private final String name;

    TemplateContactFields(final String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     *
     * @return The template placeholder field name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Lists out the fields of Contact that should be included in placeHolder fields of template
     * </p>
     *
     * @return {@link List} of field names
     */
    public static List<String> getDefaultFields() {
        return Arrays.stream(values()).map(TemplateContactFields::getName).collect(Collectors.toList());
    }
}