package com.tlc.crm.security.context;

import com.tlc.commons.executor.context.Context;

/**
 * Represents the CRM context type
 *
 * @author sundar
 * @version 1.0
 */
public enum CrmContextType implements Context.Type {

    SECURITY("security");

    private final String name;

    CrmContextType(String name) {
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
