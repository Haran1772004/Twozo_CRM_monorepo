package com.tlc.crm.core.api.analytics.service.internal;

import com.tlc.crm.core.api.analytics.entity.config.modules.CRMSystemModularField;
import com.tlc.crm.core.api.analytics.resolver.module.CRMSystemModularFieldResolver;
import com.tlc.crm.core.api.analytics.service.module.CRMSystemModularFieldService;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link CRMSystemModularFieldService} backed by {@link CRMSystemModularFieldResolver}.
 *
 * @author Karthick V
 * @version 1.0
 */
public class CRMSystemModularFieldServiceImpl implements CRMSystemModularFieldService {

    private final CRMSystemModularFieldResolver resolver;

    private CRMSystemModularFieldServiceImpl() {
        this.resolver = CRMSystemModularFieldResolver.getInstance();
    }

    private static class Instance {
        private static final CRMSystemModularFieldServiceImpl INSTANCE = new CRMSystemModularFieldServiceImpl();
    }

    public static CRMSystemModularFieldServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CRMSystemModularField get(final Long id) {
        if (Objects.isNull(id)) {
            return null;
        }

        return resolver.getById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, CRMSystemModularField> getAll(final Collection<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<Long, CRMSystemModularField> resultMap = new HashMap<>();

        for (final Long id : ids) {
            final CRMSystemModularField value = get(id);

            if (Objects.nonNull(value)) {
                resultMap.put(id, value);
            }
        }

        return resultMap;
    }
}
