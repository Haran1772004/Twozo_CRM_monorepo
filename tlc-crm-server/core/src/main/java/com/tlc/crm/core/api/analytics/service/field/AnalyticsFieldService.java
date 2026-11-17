package com.tlc.crm.core.api.analytics.service.field;

import com.tlc.crm.core.api.analytics.entity.config.fields.AnalyticsField;
import com.tlc.crm.core.api.analytics.service.internal.AnalyticsFieldServiceImpl;

import java.util.Collection;
import java.util.Map;

/**
 * Service interface for accessing {@link AnalyticsField} with local caching support.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface AnalyticsFieldService {

    /**
     * <p>
     * Retrieves the instance of {@link AnalyticsFieldService}
     * </p>
     *
     * @return The {@link AnalyticsFieldService} instance
     */
    static AnalyticsFieldService getInstance() {
        return AnalyticsFieldServiceImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the {@link Collection} of {@link AnalyticsField} for the given module id
     * </p>
     *
     * @param moduleId Represents the module id
     * @return {@link Collection} of {@link AnalyticsField}
     */
    Collection<AnalyticsField> getAnalyticsFields(final Integer moduleId);

    /**
     * <p>
     * Retrieves the {@link AnalyticsField} for the given field id
     * </p>
     *
     * @param fieldId Represents the field id
     * @return {@link AnalyticsField}
     */
    AnalyticsField getAnalyticsFieldById(final Long fieldId);

    /**
     * <p>
     * Retrieves the {@link Map} of field id to {@link AnalyticsField} for the given field ids.
     * </p>
     *
     * @param fieldIds Represents the collection of field ids
     * @return {@link Map} of field id to {@link AnalyticsField}
     */
    Map<Long, AnalyticsField> getAnalyticsFieldsByIds(final Collection<Long> fieldIds);
}
