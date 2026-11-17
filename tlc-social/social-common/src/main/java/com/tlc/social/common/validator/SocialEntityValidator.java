package com.tlc.social.common.validator;

import com.tlc.social.common.model.SocialEntity;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.service.provider.ValidatorServiceProvider;

import jakarta.validation.ConstraintViolation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A validator that validates the provided social entities
 *
 * @author Maharaja S
 * @version 1.0
 */
public final class SocialEntityValidator {

    /**
     * <p>
     * Validates the models against the given validator groups
     * </p>
     *
     * @param models          Represents the collection of models to be validated
     * @param validatorGroups Represents the validator groups that the models are validated against
     */
    public static <T extends SocialEntity> Collection<String> validate(final Collection<T> models, final Class... validatorGroups) {
        final ModelValidator validator = ValidatorServiceProvider.get().modelValidator();
        final Set<ConstraintViolation<T>> violations = new HashSet<>();

        models.forEach(model -> violations.addAll(validator.validate(model, validatorGroups)));

        return violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }
}