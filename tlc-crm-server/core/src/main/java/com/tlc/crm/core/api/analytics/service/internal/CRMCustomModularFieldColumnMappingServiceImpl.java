package com.tlc.crm.core.api.analytics.service.internal;

import com.tlc.crm.core.api.analytics.entity.integ.reports.CRMCustomModularFieldColumnMapping;
import com.tlc.crm.core.api.analytics.resolver.module.CRMCustomModularFieldColumnMappingResolver;
import com.tlc.crm.core.api.analytics.service.module.CRMCustomModularFieldColumnMappingService;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Implementation of {@link CRMCustomModularFieldColumnMappingService} backed by {@link CRMCustomModularFieldColumnMappingResolver}.
 *
 * @author Karthick V
 * @version 1.0
 */
public class CRMCustomModularFieldColumnMappingServiceImpl implements CRMCustomModularFieldColumnMappingService {

    private final CRMCustomModularFieldColumnMappingResolver resolver;

    private CRMCustomModularFieldColumnMappingServiceImpl() {
        this.resolver = CRMCustomModularFieldColumnMappingResolver.getInstance();
    }

    private static class Instance {
        private static final CRMCustomModularFieldColumnMappingServiceImpl INSTANCE = new CRMCustomModularFieldColumnMappingServiceImpl();
    }

    public static CRMCustomModularFieldColumnMappingServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<CRMCustomModularFieldColumnMapping> getCustomColumnMappings(final Integer moduleId, final Long fieldTypeId) {
        if (Objects.isNull(moduleId) || Objects.isNull(fieldTypeId)) {
            return Collections.emptyList();
        }

        final Collection<CRMCustomModularFieldColumnMapping> result = resolver.getByModuleId(moduleId);

        if (Objects.isNull(result) || result.isEmpty()) {
            return Collections.emptyList();
        }

        return result.stream()
                .filter(mapping -> Objects.equals(mapping.getFieldTypeId(), fieldTypeId))
                .toList();
    }
}
