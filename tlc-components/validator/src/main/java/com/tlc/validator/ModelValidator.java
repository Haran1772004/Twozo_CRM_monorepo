package com.tlc.validator;


import com.tlc.validator.model.ErrorDescriptor;
import jakarta.validation.ConstraintViolation;

import java.util.Collection;
import java.util.Set;

/**
 * @author Abishek
 * @version 1.0
 */
public interface ModelValidator
{
    <T extends TlcModel> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups);

    <T extends TlcModel> boolean isValid(T object, Class<?>... groups);

    /**
     * Validates the models against the given validator groups
     *
     * @param models          Represents the {@link TlcModel}
     * @param validatorGroups Represents the validator groups
     * @return {@link Collection}
     */
    <T extends TlcModel> Collection<ErrorDescriptor> validate(final Collection<T> models, final Class<?>... validatorGroups);
}
