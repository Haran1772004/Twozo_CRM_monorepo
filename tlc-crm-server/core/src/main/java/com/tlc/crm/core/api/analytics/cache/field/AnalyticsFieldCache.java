package com.tlc.crm.core.api.analytics.cache.field;

import com.tlc.crm.common.cache.LocalCache;
import com.tlc.crm.core.api.analytics.cache.internal.AnalyticsFieldCacheImpl;
import com.tlc.crm.core.api.analytics.entity.config.fields.AnalyticsField;

import java.util.Collection;

/**
 * Cache interface for {@link AnalyticsField} objects using local cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface AnalyticsFieldCache extends LocalCache {

    /**
     * <p>
     * Retrieves the instance of {@link AnalyticsFieldCache}
     * </p>
     *
     * @return The {@link AnalyticsFieldCache} instance
     */
    static AnalyticsFieldCache getInstance() {
        return AnalyticsFieldCacheImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the {@link Collection} of {@link AnalyticsField} from the cache for the given module id
     * </p>
     *
     * @param moduleId Represents the module id
     * @return {@link Collection} of {@link AnalyticsField}
     */
    Collection<AnalyticsField> getByModuleId(final Integer moduleId);

    /**
     * <p>
     * Retrieves the {@link AnalyticsField} from the cache for the given field id
     * </p>
     *
     * @param fieldId Represents the field id
     * @return {@link AnalyticsField}
     */
    AnalyticsField get(final Long fieldId);

    /**
     * <p>
     * Stores the {@link AnalyticsField} into the cache
     * </p>
     *
     * @param field Represents the {@link AnalyticsField}
     */
    void set(final AnalyticsField field);

    /**
     * <p>
     * Stores the {@link Collection} of {@link AnalyticsField} into the cache
     * </p>
     *
     * @param fields Represents the {@link Collection} of {@link AnalyticsField}
     */
    void setAll(final Collection<AnalyticsField> fields);
}
