package com.tlc.search.model;

/**
 * <p>
 * Represents a stored script definition with an identifier and source code.
 * </p>
 *
 * @author Ajay
 * @version 1.0
 */
public class ScriptDefinition {

    private final String id;
    private final String source;

    public ScriptDefinition(final String id, final String source) {
        this.id = id;
        this.source = source;
    }

    /**
     * <p>
     * Gets the script id.
     * </p>
     *
     * @return The script id.
     */
    public String getId() {
        return id;
    }

    /**
     * <p>
     * Gets the script source code.
     * </p>
     *
     * @return The script source.
     */
    public String getSource() {
        return source;
    }
}

