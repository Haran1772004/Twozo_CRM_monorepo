package com.tlc.crm.webform.api.model;

import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Update;

import jakarta.validation.constraints.NotNull;

/**
 * A Model which represents the form field details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class FormField extends FormFieldProperty {

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.formField.invalid.fieldId")
    private Long fieldId;

    private Long fieldTypeId;

    public FormField() {
        super(Type.FIELD);
    }

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(final Long fieldId) {
        this.fieldId = fieldId;
    }

    public Long getFieldTypeId() {
        return fieldTypeId;
    }

    public void setFieldTypeId(final Long fieldTypeId) {
        this.fieldTypeId = fieldTypeId;
    }

}
