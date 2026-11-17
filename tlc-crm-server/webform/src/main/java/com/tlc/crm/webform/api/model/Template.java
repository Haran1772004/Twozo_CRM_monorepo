package com.tlc.crm.webform.api.model;

import com.tlc.i18n.I18nKey;

import java.util.Collection;

/**
 * Holds the {@link Template} details.
 *
 * @param id                  Refers the template id.
 * @param name                Refers the template name.
 * @param description         Refers the description of template.
 * @param submitButtonName    Refers the submit button name.
 * @param formStyle           Refers the template {@link FormStyle}.
 * @param fieldStyle          Refers the template {@link FieldStyle}.
 * @param submitButtonStyle   Refers the template {@link SubmitButtonStyle}.
 * @param submitAction        Refers the template {@link SubmitAction}.
 * @param formFieldProperties Refers the template {@link FormFieldProperty}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public record Template(Long id, I18nKey name, I18nKey description, I18nKey submitButtonName,
                       FormStyle formStyle, FieldStyle fieldStyle, SubmitButtonStyle submitButtonStyle,
                       SubmitAction submitAction, Collection<FormFieldProperty> formFieldProperties) {

}
