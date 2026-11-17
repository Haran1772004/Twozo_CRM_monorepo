package com.tlc.commons.executor.context;

/**
 * Represents the types of context available in the module.
 *
 * @author sundar
 * @version 1.0
 */
public enum CommonContextType implements Context.Type {

    SESSION("SESSION");

    private final String name;

    CommonContextType(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}