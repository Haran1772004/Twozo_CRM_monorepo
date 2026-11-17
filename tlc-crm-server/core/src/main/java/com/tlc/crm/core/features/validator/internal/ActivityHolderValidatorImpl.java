package com.tlc.crm.core.features.validator.internal;

import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.features.api.activity.model.ActivityGuest;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.features.validator.annotation.ActivityHolderValidator;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.validator.ValidatorContext;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * It validates {@link ActivityHolder} entity and returns appropriate message
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class ActivityHolderValidatorImpl implements ConstraintValidator<ActivityHolderValidator, ActivityHolder> {

    /**
     * {@inheritDoc}
     *
     * @param activityHolder             which the activity values has to be validated
     * @param constraintValidatorContext Validation context
     * @return If the given entity is valid, it will return true
     */
    @Override
    public boolean isValid(final ActivityHolder activityHolder, final ConstraintValidatorContext constraintValidatorContext) {
        final Collection<ActivityGuest> activityGuests = activityHolder.getActivityGuests();
        final ValidatorContext validatorContext = new ValidatorContext(constraintValidatorContext);

        if (Objects.nonNull(activityGuests)) {
            final Collection<Contact> contacts = activityGuests.stream().filter(Objects::nonNull).map(ActivityGuest::getContact).collect(Collectors.toSet());
            final Collection<Email> emails = activityGuests.stream().filter(Objects::nonNull).map(ActivityGuest::getEmail).collect(Collectors.toSet());

            if (contacts.isEmpty() && emails.isEmpty()) {
                addConstraintViolationMessage(validatorContext, "i18n.guest.contact.required", ActivityFields.GUEST_CONTACT.getName());
            }
        }

        return !validatorContext.isErrorsFound();
    }

    /**
     * Adds the constraint violation message to the validation context.
     *
     * @param validatorContext Refers the validator context.
     * @param message          Violation message.
     */
    private void addConstraintViolationMessage(final ValidatorContext validatorContext, final String message, final String fieldName) {
        validatorContext.setErrorsFound(true);
        validatorContext.getContext().buildConstraintViolationWithTemplate(message).addPropertyNode(fieldName).addConstraintViolation();
    }
}
