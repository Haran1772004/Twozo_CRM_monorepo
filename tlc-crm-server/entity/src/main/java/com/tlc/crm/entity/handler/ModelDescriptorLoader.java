package com.tlc.crm.entity.handler;

import com.tlc.crm.entity.internal.ModelDescriptorLoaderImpl;
import com.tlc.crm.entity.models.Entity;

import java.util.Collection;
import java.util.Set;

/**
 * Provides the services for property descriptor of a model
 *
 * @param <T>
 * @author KowsalyaSP
 * @version 1.0
 */
public interface ModelDescriptorLoader<T> {

    /**
     * <p>
     * Fetches the instance of the {@link ModelDescriptorLoader}
     * </p>
     *
     * @return {@link ModelDescriptorLoader}
     */
    static ModelDescriptorLoader<?> getInstance() {
        return ModelDescriptorLoaderImpl.getInstance();
    }

    /**
     * Merges the properties of secondary model with the primary model
     *
     * @param fieldNames     Represents the {@link Collection} of field names
     * @param primaryModel   Represents the primary model
     * @param secondaryModel Represents the secondary model
     */
    void mergeProperties(final Collection<String> fieldNames, final T primaryModel, final T secondaryModel);

    /**
     * Loads the value holder of secondary with the primary
     *
     * @param primaryEntity   Represents the primary entity
     * @param secondaryEntity Represents the secondary entity
     */
    void loadEntityValueHolder(final Entity primaryEntity, final Entity secondaryEntity);

    /**
     * Gets the field names of an entity
     *
     * @param entity         Represents the entity
     * @param excludedFields Holds the excluded fields to remove
     * @return {@link Set} of field names
     */
    Set<String> getFieldNames(final T entity, final Set<String> excludedFields);
}
