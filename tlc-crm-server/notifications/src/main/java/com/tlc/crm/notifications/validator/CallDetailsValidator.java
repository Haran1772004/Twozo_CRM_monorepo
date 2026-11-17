package com.tlc.crm.notifications.validator;

import com.tlc.crm.notifications.api.call.model.CallDetails;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.service.provider.ValidatorServiceProvider;

import java.util.Collection;

/**
 * Singleton utility class responsible for validating {@link CallDetails} models
 * using the configured {@link ModelValidator} instance from the validation framework.
 *
 * @author Sathish
 * @version 1.0
 */
public class CallDetailsValidator {

    private final ModelValidator validator;

    private CallDetailsValidator() {
        validator = ValidatorServiceProvider.get().modelValidator();
    }

    private static final class InstanceHolder {
        private static final CallDetailsValidator CALL_DETAILS_VALIDATOR = new CallDetailsValidator();
    }

    public static CallDetailsValidator getInstance() {
        return InstanceHolder.CALL_DETAILS_VALIDATOR;
    }

    /**
     * Validates a collection of {@link CallDetails} models using the provided validation groups.
     * Collects and returns all validation error messages across all models.
     *
     * @param models          the collection of CallDetails objects to validate
     * @param validatorGroups optional validator groups for scoped constraint checks
     * @return a collection of validation error messages (if any), or empty if all are valid
     */
    public final Collection<ErrorDescriptor> validate(final Collection<CallDetails> models, final Class<?>... validatorGroups) {
        return validator.validate(models, validatorGroups);
    }
}
