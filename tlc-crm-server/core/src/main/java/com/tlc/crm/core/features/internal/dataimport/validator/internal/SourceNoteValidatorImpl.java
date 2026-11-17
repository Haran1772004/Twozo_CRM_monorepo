package com.tlc.crm.core.features.internal.dataimport.validator.internal;

import com.tlc.crm.core.features.internal.dataimport.loader.source.model.SourceNote;
import com.tlc.crm.core.features.internal.dataimport.validator.annotation.SourceNoteValidator;
import com.tlc.crm.entity.features.field.note.NoteFields;
import com.tlc.crm.entity.validator.ValidatorContext;
import com.tlc.l10n.temporal.model.DateTime;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Instant;
import java.util.Objects;

/**
 * Validates {@link SourceNote} entity and returns appropriate message.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class SourceNoteValidatorImpl implements ConstraintValidator<SourceNoteValidator, SourceNote> {

    /**
     * {@inheritDoc}
     *
     * @param sourceNote Refers the {@link SourceNote} to validate.
     * @param context    Context in which the constraint is evaluated.
     * @return {@code false} if {@code value} does not pass the constraint.
     */
    @Override
    public boolean isValid(final SourceNote sourceNote, final ConstraintValidatorContext context) {
        final DateTime createdTime = sourceNote.getCreatedTime();
        final ValidatorContext validatorContext = new ValidatorContext(context);

        if (Objects.nonNull(createdTime) && createdTime.getTimeStamp() > Instant.now().toEpochMilli()) {
            addConstraintViolationMessage(validatorContext, "i18n.entity.field.created.invalid", NoteFields.CREATED_TIME.getName());
        }
        return (Objects.nonNull(sourceNote.getCompany()) || Objects.nonNull(sourceNote.getContact())
                || Objects.nonNull(sourceNote.getDeal())) && !validatorContext.isErrorsFound();
    }

    /**
     * <p>
     * Adds the validation error message to the validator context
     * </p>
     *
     * @param validatorContext Represents the {@link ValidatorContext}
     * @param message          Represents the error message
     */
    private void addConstraintViolationMessage(final ValidatorContext validatorContext, final String message, final String fieldName) {
        validatorContext.setErrorsFound(true);
        validatorContext.getContext().buildConstraintViolationWithTemplate(message).addPropertyNode(fieldName).addConstraintViolation();
    }
}
