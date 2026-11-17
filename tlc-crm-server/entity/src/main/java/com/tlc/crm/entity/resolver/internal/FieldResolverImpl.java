package com.tlc.crm.entity.resolver.internal;

import com.tlc.crm.entity.cache.field.FieldCache;
import com.tlc.crm.entity.manager.CrmFieldsManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.internal.FieldConfig;
import com.tlc.crm.entity.resolver.FieldResolver;

/**
 * Resolves the {@link FieldInfo} and {@link FieldConfig} related details.
 *
 * @author Sathish
 * @version 1.0
 * @see FieldResolver
 */
public class FieldResolverImpl extends AbstractFieldResolver {

    private FieldResolverImpl() {
        super(CrmFieldsManager.getFieldsManager(), FieldCache.getInstance());
    }

    private static class Instance {
        private static final FieldResolverImpl INSTANCE = new FieldResolverImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link FieldResolverImpl}.
     * </p>
     *
     * @return The {@link FieldResolverImpl}
     */
    public static FieldResolverImpl getInstance() {
        return Instance.INSTANCE;
    }
}
