package com.tlc.crm.entity.validator;

import jakarta.validation.ConstraintValidatorContext;

/**
 * A Class that wraps the {@link ConstraintValidatorContext}
 *
 * @author Sundar
 * @version 1.0
 */
public final class ValidatorContext {

    private final ConstraintValidatorContext context;
    private boolean isErrorsFound;

    public ValidatorContext(final ConstraintValidatorContext context) {
        this.context = context;
        this.isErrorsFound = false;
    }

    public ConstraintValidatorContext getContext() {
        return context;
    }

    public boolean isErrorsFound() {
        return isErrorsFound;
    }

    public void setErrorsFound(boolean errorsFound) {
        isErrorsFound = errorsFound;
    }
}
