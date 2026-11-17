package com.tlc.iam.common.models.validator;

import com.tlc.validator.ModelValidator;
import com.tlc.validator.TlcModel;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import jakarta.validation.ConstraintViolation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A validator that validates the given entities
 *
 * @author Selvakumar
 * @version 1.0
 */
public class EntityValidator {

    /**
     * <p>
     * Validates the models against the given validator groups
     * </p>
     *
     * @param models          Collection of models which has to be validated against the given validator groups
     * @param validatorGroups Models are validated against the given validator groups
     */
    public static <T extends TlcModel> Collection<String> validate(final Collection<T> models,
                                                                   final Class... validatorGroups) {
        final ModelValidator validator = ValidatorServiceProvider.get().modelValidator();
        final Set<ConstraintViolation<T>> violations = new HashSet<>();

        models.forEach(model -> violations.addAll(validator.validate(model, validatorGroups)));

        return violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

}
