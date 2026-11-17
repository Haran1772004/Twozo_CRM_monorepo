package com.tlc.crm.core.validator;

import com.tlc.crm.entity.models.Entity;

import java.util.Collection;

/**
 * Validates the entity data
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface EntityDataValidator<T extends Entity> {

    /**
     * <p>
     * Validates the entity and throws error if any validation fails
     * </p>
     *
     * @param orgId  Represents the org id
     * @param models Represents the models to be validated
     */
    void validate(final Long orgId, final Collection<T> models);
}