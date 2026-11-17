package com.tlc.search.service;

import java.io.IOException;
import java.io.InputStream;

import java.util.Map;

/**
 * <p>
 * Contract for discovering stored script resource files from the classpath.
 * </p>
 *
 * <p>
 * Implementations scan for individual {@code .painless} script files under
 * the {@code stored_scripts/} directory across all modules.
 * </p>
 *
 * @author Ajay
 * @version 1.0
 */
@FunctionalInterface
public interface ScriptResourceService {

    /**
     * <p>
     * Discovers all {@code .painless} script files from the {@code stored_scripts/} directory
     * across the classpath and returns a map of script id (filename without extension)
     * to its input stream.
     * </p>
     *
     * @return Map of script id to {@link InputStream} for each discovered script file.
     * @throws IOException If an I/O error occurs during resource discovery.
     */
    Map<String, InputStream> getScriptFiles() throws IOException;
}

