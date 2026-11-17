package com.tlc.search.internal.script;

import com.tlc.commons.code.ErrorCode;

import com.tlc.search.internal.status.IndexingErrorCodes;
import com.tlc.search.model.ScriptDefinition;

import java.io.InputStream;
import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Parses individual stored script files and extracts {@link ScriptDefinition} instances.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class ScriptFileParser {

    private ScriptFileParser() {
    }

    /**
     * <p>
     * Parses individual script files where each file contains a single script.
     * The script id is derived from the filename (without extension) and the entire file content
     * is the script source.
     * </p>
     *
     * @param scriptFiles Map of script id (filename without extension) to its {@link InputStream}.
     * @return Collection of {@link ScriptDefinition} parsed from all files.
     */
    public static Collection<ScriptDefinition> parse(final Map<String, InputStream> scriptFiles) {
        final Collection<ScriptDefinition> scripts = new ArrayList<>();

        for (final Map.Entry<String, InputStream> entry : scriptFiles.entrySet()) {
            final String scriptId = entry.getKey();

            try (final InputStream inputStream = entry.getValue()) {
                final String source = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();

                if (!source.isEmpty()) {
                    scripts.add(new ScriptDefinition(scriptId, source));
                }
            } catch (IOException exception) {
                throw ErrorCode.get(IndexingErrorCodes.SCRIPT_PARSING_FAILED, exception);
            }
        }

        return scripts;
    }
}
