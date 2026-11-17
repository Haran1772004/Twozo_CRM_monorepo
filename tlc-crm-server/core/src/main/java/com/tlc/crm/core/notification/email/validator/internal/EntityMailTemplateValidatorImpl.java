package com.tlc.crm.core.notification.email.validator.internal;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.notification.email.template.api.TemplateTagManager;
import com.tlc.crm.core.notification.email.template.api.model.EntityMailTemplate;
import com.tlc.crm.core.notification.email.template.api.model.TemplateTag;
import com.tlc.crm.core.notification.email.template.fields.TemplateTagFields;
import com.tlc.crm.core.notification.email.validator.annotation.EntityMailTemplateValidator;
import com.tlc.crm.entity.validator.ValidatorContext;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Validator implementation for {@link EntityMailTemplate}, used in conjunction with
 * the {@link EntityMailTemplateValidator} annotation.
 * </p>
 *
 * @author Mohamed Yasar k
 * @version 1.0
 */
public class EntityMailTemplateValidatorImpl implements ConstraintValidator<EntityMailTemplateValidator, EntityMailTemplate> {

    private final TemplateTagManager templateTagManager = TemplateTagManager.getInstance();

    /**
     * <p>
     * Performs validation logic on the provided {@link EntityMailTemplate} instance.
     * </p>
     *
     * @param entityMailTemplate         The template instance to validate
     * @param constraintValidatorContext The context in which the constraint is evaluated
     * @return {@code true} if validation passes; {@code false} otherwise
     */
    @Override
    public boolean isValid(final EntityMailTemplate entityMailTemplate, final ConstraintValidatorContext constraintValidatorContext) {
        final ValidatorContext validatorContext = new ValidatorContext(constraintValidatorContext);
        final Collection<TemplateTag> templateTags = entityMailTemplate.getTemplateTags();
        final String nameField = TemplateTagFields.NAME.getName();

        if (Objects.nonNull(templateTags)) {

            for (final TemplateTag templateTag : templateTags) {
                final String tagName = templateTag.getName();

                if (Objects.nonNull(tagName) && tagName.length() > NumericalConstant.TWO_HUNDRED_FIFTY_FIVE) {
                    addConstraintViolationMessage(validatorContext, "i18n.template.tag.name.max.length", nameField);
                }
            }
        }

        if (validatorContext.isErrorsFound()) {
            validatorContext.getContext().disableDefaultConstraintViolation();
        }

        return !validatorContext.isErrorsFound();
    }

    /**
     * <p>
     * Adds a constraint violation message to the context for a specific field.
     * </p>
     *
     * @param validatorContext Refers to the current validator context
     * @param message          The i18n message key to use for the violation
     * @param fieldName        The field to which the violation is attached
     */
    private void addConstraintViolationMessage(final ValidatorContext validatorContext, final String message, final String fieldName) {
        validatorContext.setErrorsFound(true);
        validatorContext.getContext()
                .buildConstraintViolationWithTemplate(message)
                .addPropertyNode(fieldName)
                .addConstraintViolation();
    }
}