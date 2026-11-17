package com.tlc.crm.entity.resolver;

import com.tlc.crm.entity.models.type.Option;

import java.util.Collection;

/**
 * A resolver that resolves {@link Option} related details
 *
 * @author Kishore J
 * @version 1.0
 */
public interface OptionResolver {

    /**
     * <p>
     * Gets the {@link OptionResolver} instance
     * </p>
     *
     * @return The {@link OptionResolver}
     */
    static OptionResolver getInstance() {
        return OptionResolverImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves a collection of {@link Option} for the given organization id, field id.
     * If the options are not cached, it retrieves the data from the database, caches it, and returns organization
     * options from the cache
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param fieldId fieldId Represents the field id
     * @return The collection of {@link Option}
     */
    Collection<Option> get(final long orgId, final long fieldId);
}