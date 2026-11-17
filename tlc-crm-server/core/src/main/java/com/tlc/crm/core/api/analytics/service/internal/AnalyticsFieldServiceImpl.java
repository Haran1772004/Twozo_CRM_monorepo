package com.tlc.crm.core.api.analytics.service.internal;

import com.tlc.crm.core.api.analytics.entity.config.fields.AnalyticsField;
import com.tlc.crm.core.api.analytics.resolver.field.AnalyticsFieldResolver;
import com.tlc.crm.core.api.analytics.service.field.AnalyticsFieldService;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link AnalyticsFieldService} backed by {@link AnalyticsFieldResolver}.
 *
 * @author Karthick V
 * @version 1.0
 */
public class AnalyticsFieldServiceImpl implements AnalyticsFieldService {

    private final AnalyticsFieldResolver analyticsFieldResolver;

    private AnalyticsFieldServiceImpl() {
        this.analyticsFieldResolver = AnalyticsFieldResolver.getInstance();
    }

    private static class Instance {
        private static final AnalyticsFieldServiceImpl INSTANCE = new AnalyticsFieldServiceImpl();
    }

    /**
     * <p>
     * Retrieves the {@link AnalyticsFieldServiceImpl} instance
     * </p>
     *
     * @return {@link AnalyticsFieldServiceImpl}
     */
    public static AnalyticsFieldServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<AnalyticsField> getAnalyticsFields(final Integer moduleId) {
        if (Objects.isNull(moduleId)) {
            return Collections.emptyList();
        }

        return analyticsFieldResolver.get(moduleId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalyticsField getAnalyticsFieldById(final Long fieldId) {
        if (Objects.isNull(fieldId)) {
            return null;
        }

        return analyticsFieldResolver.getById(fieldId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, AnalyticsField> getAnalyticsFieldsByIds(final Collection<Long> fieldIds) {
        if (Objects.isNull(fieldIds) || fieldIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return analyticsFieldResolver.getByIds(fieldIds);
    }
}
