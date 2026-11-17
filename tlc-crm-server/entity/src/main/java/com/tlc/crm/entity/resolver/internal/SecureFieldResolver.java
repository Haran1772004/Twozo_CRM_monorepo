package com.tlc.crm.entity.resolver.internal;

import com.tlc.crm.entity.cache.field.FieldCache;
import com.tlc.crm.entity.manager.CrmFieldsManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.internal.FieldConfig;
import com.tlc.crm.entity.resolver.FieldResolver;

/**
 * Resolves the {@link FieldInfo} and {@link FieldConfig} related details with secure manner.
 *
 * @author Sathish
 * @version 1.0
 * @see FieldResolver
 */
public class SecureFieldResolver extends AbstractFieldResolver {

    private SecureFieldResolver() {
        super(CrmFieldsManager.getSecureFieldsManager(), FieldCache.getSecureInstance());
    }

    private static class Instance {
        private static final SecureFieldResolver INSTANCE = new SecureFieldResolver();
    }

    /**
     * <p>
     * Gets the instance of {@link SecureFieldResolver}.
     * </p>
     *
     * @return The {@link SecureFieldResolver}
     */
    public static SecureFieldResolver getInstance() {
        return Instance.INSTANCE;
    }
}
