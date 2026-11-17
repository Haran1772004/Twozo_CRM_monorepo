package com.tlc.validator.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeProvider;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.TlcModel;

import com.tlc.validator.ValidationErrorCodes;
import com.tlc.validator.model.ErrorDescriptor;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.hibernate.validator.HibernateValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Abishek
 * @version 1.0
 */

public class ModelValidatorImpl implements ModelValidator
{
    private final Validator validator;
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelValidatorImpl.class);

    public ModelValidatorImpl() {
        final ValidatorFactory validatorFactory = Validation.byProvider(HibernateValidator.class).configure().buildValidatorFactory();
        this.validator = validatorFactory.getValidator();

        LOGGER.info("Validator: Model validator service activated");
    }

    @Override
    public final <T extends TlcModel> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups)
    {
        return validator.validate(object, groups);
    }

    @Override
    public final <T extends TlcModel> boolean isValid(T object, Class<?>... groups)
    {
        final Set<ConstraintViolation<T>> constraintViolations = validator.validate(object, groups);
        return constraintViolations.size() == 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param models          Represents the {@link TlcModel}
     * @param validatorGroups Represents the validator groups
     * @return {@link Collection}
     */
    @Override
    public <T extends TlcModel> Collection<ErrorDescriptor> validate(final Collection<T> models, final Class<?>... validatorGroups) {
        final Set<ConstraintViolation<T>> violations = new HashSet<>();
        final ErrorCodeProvider errorCodeProvider = ValidationErrorCodes.DATA_VALIDATION_FAILED;

        models.forEach(model -> violations.addAll(validator.validate(model, validatorGroups)));

        return violations.stream().map(violation -> new ErrorDescriptor(ErrorCode.get(errorCodeProvider, violation.getMessage()), null,
                violation.getPropertyPath().toString())).collect(Collectors.toSet());
    }
}
