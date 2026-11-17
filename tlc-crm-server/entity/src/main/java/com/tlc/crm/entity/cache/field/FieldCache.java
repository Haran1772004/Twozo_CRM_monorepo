package com.tlc.crm.entity.cache.field;

import com.tlc.crm.common.cache.RemoteCache;

import com.tlc.crm.entity.cache.internal.FieldCacheImpl;
import com.tlc.crm.entity.cache.internal.SecureFieldCache;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.internal.FieldConfig;

import java.util.Collection;
import java.util.Map;

/**
 * An interface which provides an abstract way to deal with {@link FieldInfo} objects
 *
 * @author Selvakumar
 * @version 1.0
 */
public interface FieldCache extends RemoteCache {

    /**
     * Gets the instance of {@link FieldCache}.
     *
     * @return The {@link FieldCache}
     */
    static FieldCache getInstance() {
        return FieldCacheImpl.getInstance();
    }

    /**
     * Gets the instance of secure {@link FieldCache}.
     *
     * @return The {@link FieldCache}
     */
    static FieldCache getSecureInstance() {
        return SecureFieldCache.getInstance();
    }

    /**
     * <p>
     * Fetches a collection of organization fields by given organization id.
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
     * Fetches the source persistent fields for an organization
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param sourceName Represents the source name
     * @return the {@link Collection} of field details
     */
    Collection<FieldInfo> getFieldsBySource(final Long orgId, final String sourceName);

    /**
     * <p>
     * Fetches the organization field configurations grouped by field id for the given org id.
     * </p>
     *
     * @param orgId Represents an organization id.
     * @return The {@link FieldConfig}.
     */
    Map<Long, FieldConfig> getFieldConfigs(final Long orgId);

    /**
     * <p>
     * Invalidates the field and field config caches.
     * </p>
     *
     * @param orgId Represents an organization id
     */
    void invalidateCache(final long orgId);

    /**
     * <p>
     * Adds the organization fields into cache
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param fields Which contains the collection of {@link FieldInfo}
     */
    void addFieldsIntoCache(final Long orgId, final Collection<FieldInfo> fields);

    /**
     * <p>
     * Adds the organization field configs into cache
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param fieldConfigs Which contains the collection of {@link FieldConfig}
     */
    void addFieldConfigsIntoCache(final Long orgId, final Collection<FieldConfig> fieldConfigs);
}
