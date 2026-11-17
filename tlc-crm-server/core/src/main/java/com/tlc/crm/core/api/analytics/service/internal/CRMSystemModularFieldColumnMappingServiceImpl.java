package com.tlc.crm.core.api.analytics.service.internal;

import com.tlc.crm.core.api.analytics.entity.integ.reports.CRMSystemModularFieldColumnMapping;
import com.tlc.crm.core.api.analytics.resolver.module.CRMSystemModularFieldColumnMappingResolver;
import com.tlc.crm.core.api.analytics.service.module.CRMSystemModularFieldColumnMappingService;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Implementation of {@link CRMSystemModularFieldColumnMappingService} backed by {@link CRMSystemModularFieldColumnMappingResolver}.
 *
 * @author Karthick V
 * @version 1.0
 */
public class CRMSystemModularFieldColumnMappingServiceImpl implements CRMSystemModularFieldColumnMappingService {

    private final CRMSystemModularFieldColumnMappingResolver resolver;

    private CRMSystemModularFieldColumnMappingServiceImpl() {
        this.resolver = CRMSystemModularFieldColumnMappingResolver.getInstance();
    }

    private static class Instance {
        private static final CRMSystemModularFieldColumnMappingServiceImpl INSTANCE = new CRMSystemModularFieldColumnMappingServiceImpl();
    }

    public static CRMSystemModularFieldColumnMappingServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<CRMSystemModularFieldColumnMapping> getSystemColumnMappings(final Long modularFieldId) {
        if (Objects.isNull(modularFieldId)) {
            return Collections.emptyList();
        }

        final Collection<CRMSystemModularFieldColumnMapping> result = resolver.getBySourceId(modularFieldId);

        return Objects.nonNull(result) ? result : Collections.emptyList();
    }
}
