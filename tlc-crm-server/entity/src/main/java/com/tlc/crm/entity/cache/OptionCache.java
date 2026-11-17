package com.tlc.crm.entity.cache;

import com.tlc.crm.entity.models.type.Option;

import java.util.Collection;

/**
 * Provides a service to fetch {@link Option} from the cache
 *
 * @author Kishore J
 * @version 1.0
 */
public interface OptionCache {

    /**
     * <p>
     * Gets the {@link OptionCache} instance
     * </p>
     *
     * @return The {@link OptionCache}
     */
    static OptionCache getInstance() {
        return OptionCacheImpl.getInstance();
    }

    /***
     * <p>
     * Sets the organization options into the cache
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param fieldId Represents the field id
     * @param options Which contains a collection of {@link Option}
     */
    void setOptions(final long orgId, final long fieldId, final Collection<Option> options);

    /**
     * <p>
     * Fetches a collection of {@link Option} for the given org id
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param fieldId Represents the field id
     * @return The collection of {@link Option}
     */
    Collection<Option> getOptions(final long orgId, final long fieldId);
}
