package com.tlc.crm.core.api.analytics.resolver.field;

import com.tlc.crm.core.api.analytics.entity.config.fields.AnalyticsField;
import com.tlc.crm.core.api.analytics.resolver.internal.AnalyticsFieldResolverImpl;

import java.util.Collection;
import java.util.Map;

/**
 * Resolver interface for {@link AnalyticsField} that handles cache-miss resolution.
 * Checks cache first; on miss, fetches from DB and populates cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface AnalyticsFieldResolver {

    /**
     * <p>
     * Retrieves the instance of {@link AnalyticsFieldResolver}
     * </p>
     *
     * @return The {@link AnalyticsFieldResolver} instance
     */
    static AnalyticsFieldResolver getInstance() {
        return AnalyticsFieldResolverImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the {@link Collection} of {@link AnalyticsField} for the given module id.
     * If not found in cache, fetches from DB and populates cache.
     * </p>
     *
     * @param moduleId Represents the module id
     * @return {@link Collection} of {@link AnalyticsField}
     */
    Collection<AnalyticsField> get(final Integer moduleId);

    /**
     * <p>
     * Retrieves the {@link AnalyticsField} for the given field id.
     * If not found in cache, fetches from DB and populates cache.
     * </p>
     *
     * @param fieldId Represents the field id
     * @return {@link AnalyticsField}
     */
    AnalyticsField getById(final Long fieldId);

    /**
     * <p>
     * Retrieves the {@link Map} of field id to {@link AnalyticsField} for the given field ids.
     * If not found in cache, fetches from DB and populates cache.
     * </p>
     *
     * @param fieldIds Represents the collection of field ids
     * @return {@link Map} of field id to {@link AnalyticsField}
     */
    Map<Long, AnalyticsField> getByIds(final Collection<Long> fieldIds);
}
