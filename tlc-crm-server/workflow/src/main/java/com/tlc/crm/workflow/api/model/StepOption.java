package com.tlc.crm.workflow.api.model;

import com.tlc.i18n.I18nKey;

/**
 * <p>
 * Represents the {@link StepOption} details
 * </p>
 *
 * @param id         Represents the step option id
 * @param name       Represents the {@link I18nKey} name
 * @param fieldId    Represents the workflow field id or source field id
 * @param fieldGroup Represents the {@link FieldGroup}
 *
 * @author Kavinila E
 * @version 1.0
 */
public record StepOption(Long id, I18nKey name, Long fieldId, String aliasName, FieldGroup fieldGroup) {
}
