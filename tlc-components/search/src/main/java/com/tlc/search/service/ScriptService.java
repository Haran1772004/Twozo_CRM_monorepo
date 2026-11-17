package com.tlc.search.service;

import com.tlc.search.model.ScriptDefinition;

import java.util.Collection;

/**
 * <p>
 * Provides the script services to manage stored scripts in the search engine.
 * </p>
 *
 * @author Ajay
 * @version 1.0
 */
public interface ScriptService {

    /**
     * <p>
     * Uploads a single stored script.
     * </p>
     *
     * @param scriptDefinition {@link ScriptDefinition} that holds the script id and source.
     */
    void upload(final ScriptDefinition scriptDefinition);

    /**
     * <p>
     * Uploads a collection of stored scripts.
     * </p>
     *
     * @param scriptDefinitions Collection of {@link ScriptDefinition} to be uploaded.
     */
    void uploadAll(final Collection<ScriptDefinition> scriptDefinitions);

    /**
     * <p>
     * Deletes a stored script by its id.
     * </p>
     *
     * @param scriptId The id of the stored script to delete.
     */
    void delete(final String scriptId);
}

