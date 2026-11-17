package com.tlc.crm.entity.features.validator.internal;

import com.tlc.commons.parser.html.HtmlParser;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.entity.features.validator.annotation.ActivityValidator;
import com.tlc.crm.entity.validator.ValidatorContext;
import com.tlc.l10n.temporal.model.DateTime;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Instant;
import java.util.Calendar;
import java.util.Objects;

/**
 * <p>
 * It validates {@link Activity} entity and returns appropriate message
 * </p>
 *
 * @author pandiaraj
 * @version 1.0
 */
public class ActivityValidatorImpl implements ConstraintValidator<ActivityValidator, Activity> {

    private static final int MAXIMUM_MONTH = 1;
    private static final HtmlParser HTML_PARSER = HtmlParser.getInstance();

    @Override
    public boolean isValid(final Activity activity, final ConstraintValidatorContext constraintValidatorContext) {
        final Long startTime = activity.getStartTime().getTimeStamp();
        final Long endTime = activity.getEndTime().getTimeStamp();
        final Calendar calendar = Calendar.getInstance();
        final ValidatorContext validatorContext = new ValidatorContext(constraintValidatorContext);
        final String endTimeKey = ActivityFields.END_TIME.getName();

        calendar.setTimeInMillis(startTime);
        calendar.add(Calendar.MONTH, MAXIMUM_MONTH);

        if (endTime > calendar.getTimeInMillis()) {
            addConstraintViolationMessage(validatorContext, "i18n.activity.invalid.time.difference", endTimeKey);
        }

        if (endTime < startTime) {
            addConstraintViolationMessage(validatorContext, "i18n.activity.date.and.time.invalid", endTimeKey);
        }
        final Note note = activity.getNote();

        if (Objects.nonNull(note)) {
            final String content = note.getContent();
            final String plainTextContent = HTML_PARSER.isHtml(content) ? HTML_PARSER.parse(content).text() : content;

            if (plainTextContent.length() > 10000) {
                addConstraintViolationMessage(validatorContext, "i18n.note.max.size", ActivityFields.NOTE.getName());
            }
        }
        final DateTime createdTime = activity.getCreatedTime();

        if (Objects.nonNull(createdTime) && createdTime.getTimeStamp() > Instant.now().toEpochMilli()) {
            addConstraintViolationMessage(validatorContext, "i18n.entity.field.created.invalid", ActivityFields.CREATED_TIME.getName());
        }
        disableDefaultConstraintViolation(validatorContext);

        return !validatorContext.isErrorsFound();
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

    /**
     * <p>
     * Disables the default constraint violation of the {@link ValidatorContext}
     * </p>
     *
     * @param validatorContext Represents {@link ValidatorContext}
     */
    private void disableDefaultConstraintViolation(final ValidatorContext validatorContext) {
        if (validatorContext.isErrorsFound()) {
            validatorContext.getContext().disableDefaultConstraintViolation();
        }
    }
}
