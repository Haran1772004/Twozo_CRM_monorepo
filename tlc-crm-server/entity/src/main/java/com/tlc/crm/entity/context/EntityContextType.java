package com.tlc.crm.entity.context;

import com.tlc.commons.executor.context.Context;

/**
 * Represents the CRM context type
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public enum EntityContextType implements Context.Type {

    ENTITY_VALIDATION_CACHE("entityValidationCache");

    private final String name;

    EntityContextType(final String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     *
     * @return the type name
     */
    @Override
    public String getName() {
        return name;
    }
}