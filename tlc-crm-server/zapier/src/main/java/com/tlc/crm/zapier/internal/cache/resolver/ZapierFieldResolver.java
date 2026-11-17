package com.tlc.crm.zapier.internal.cache.resolver;

import com.tlc.crm.zapier.api.models.ZapierField;

import java.util.Collection;

/**
 * The resolver responsible for retrieving and storing the details of {@link ZapierField} from/to cache.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface ZapierFieldResolver {

    /**
     * Gets the {@link ZapierFieldResolver} instance.
     *
     * @return Instance of {@link ZapierFieldResolver}.
     */
    static ZapierFieldResolver getInstance() {
        return ZapierFieldResolverImpl.getInstance();
    }

    /**
     * Retrieves a collection of ZapierField objects associated with a specific action ID.
     *
     * @param id The action ID for which the fields are to be retrieved.
     * @return A collection of ZapierField objects for the specified action ID.
     */
    Collection<ZapierField> getFieldsByActionId(final Short id);
}
