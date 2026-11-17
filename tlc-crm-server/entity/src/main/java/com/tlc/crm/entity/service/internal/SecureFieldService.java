package com.tlc.crm.entity.service.internal;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.resolver.FieldResolver;
import com.tlc.crm.entity.service.FieldService;

/**
 * Implements the {@link FieldService} provides functionality for accessing the {@link FieldInfo} with secure manner.
 *
 * @author Sathish
 * @version 1.0
 * @see FieldService
 */
public class SecureFieldService extends AbstractFieldService {

    private SecureFieldService() {
        super(FieldResolver.getSecureInstance());
    }

    private static class Instance {
        private static final SecureFieldService INSTANCE = new SecureFieldService();
    }

    /**
     * Gets the instance of {@link SecureFieldService}.
     *
     * @return The {@link SecureFieldService}
     */
    public static SecureFieldService getInstance() {
        return Instance.INSTANCE;
    }
}
