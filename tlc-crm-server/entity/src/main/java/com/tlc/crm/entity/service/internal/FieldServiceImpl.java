package com.tlc.crm.entity.service.internal;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.resolver.FieldResolver;
import com.tlc.crm.entity.service.FieldService;

/**
 * Implements the {@link FieldService} provides functionality for accessing the {@link FieldInfo}
 *
 * @author Sathish
 * @version 1.0
 * @see FieldService
 */
public class FieldServiceImpl extends AbstractFieldService {

    private FieldServiceImpl() {
        super(FieldResolver.getInstance());
    }

    private static class Instance {
        private static final FieldServiceImpl INSTANCE = new FieldServiceImpl();
    }

    /**
     * Gets the instance of {@link FieldServiceImpl}.
     *
     * @return The {@link FieldServiceImpl}
     */
    public static FieldServiceImpl getInstance() {
        return Instance.INSTANCE;
    }
}
