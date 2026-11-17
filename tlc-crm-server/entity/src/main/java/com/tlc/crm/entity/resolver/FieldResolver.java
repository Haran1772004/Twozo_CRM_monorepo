package com.tlc.crm.entity.resolver;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.internal.FieldConfig;
import com.tlc.crm.entity.resolver.internal.FieldResolverImpl;
import com.tlc.crm.entity.resolver.internal.SecureFieldResolver;

import java.util.Collection;
import java.util.Map;

/**
 * A resolver that resolves {@link FieldInfo} and {@link FieldConfig}.
 *
 * @author Kishore J
 * @version 1.0
 */
public interface FieldResolver {

    /**
     * <p>
     * Gets the instance of {@link FieldResolver}
     * </p>
     *
     * @return The {@link FieldResolver}.
     */
    static FieldResolver getInstance() {
        return FieldResolverImpl.getInstance();
    }

    /**
     * <p>
     * Gets the instance of secure {@link FieldResolver}
     * </p>
     *
     * @return The {@link FieldResolver}.
     */
    static FieldResolver getSecureInstance() {
        return SecureFieldResolver.getInstance();
    }

    /**
     * <p>
     * Fetches organization fields by the given organization ID. If the fields are not cached,
     * it retrieves the data from the database, caches it, and returns org fields information from the cache.
     * </p>
     *
     * @param orgId Represents an organization id.
     * @return The collection of {@link FieldInfo}.
     */
    Collection<FieldInfo> get(final Long orgId);

    /**
     * <p>
     * Retrieves a collection of source transient fields
     * </p>
     *
     * @param sourceNames Represents the source names
     * @return the {@link Collection} of {@link FieldInfo}
     */
    Map<String, Collection<FieldInfo>> getDefaultFieldsBySource(final Collection<String> sourceNames);

    /**
     * <p>
     * Fetches the transient fields for the given field IDs.
     * </p>
     *
     * @param fieldIds Represents the field IDs.
     * @return The {@link FieldInfo}
     */
    Map<Long, FieldInfo> getDefaultFields(final Collection<Long> fieldIds);

    /**
     * <p>
     * Retrieves a collection of source persistent fields with an organization
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param sourceName Represents the source name
     * @return the {@link Collection} of {@link FieldInfo}
     */
    Collection<FieldInfo> getFieldsBySource(final Long orgId, final String sourceName);

    /**
     * <p>
     * Fetches the organization field configuration grouped by their field IDs for the given field ID.
     * </p>
     *
     * @param orgId Represents the org id.
     * @return The {@link FieldConfig}
     */
    Map<Long, FieldConfig> getFieldConfigs(final Long orgId);
}