package com.tlc.search.internal.opensearch.impl;

import com.tlc.commons.code.ErrorCode;

import com.tlc.search.fields.ScriptKeys;
import com.tlc.search.internal.opensearch.AbstractOpenSearchService;
import com.tlc.search.internal.status.IndexingErrorCodes;
import com.tlc.search.model.ScriptDefinition;
import com.tlc.search.service.ScriptService;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.StoredScript;
import org.opensearch.client.opensearch.core.PutScriptRequest;
import org.opensearch.client.opensearch.core.DeleteScriptRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import java.util.Collection;

/**
 * <p>
 * Provides the script service to manage stored scripts in OpenSearch.
 * </p>
 *
 * @author Ajay
 * @version 1.0
 */
public class OSScriptService extends AbstractOpenSearchService implements ScriptService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OSScriptService.class);

    public OSScriptService(final OpenSearchClient client) {
        super(client);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void upload(final ScriptDefinition scriptDefinition) {
        final PutScriptRequest putScriptRequest = PutScriptRequest.of(builder -> builder
                .id(scriptDefinition.getId())
                .script(StoredScript.of(scriptBuilder -> scriptBuilder
                        .lang(ScriptKeys.PAINLESS)
                        .source(scriptDefinition.getSource())
                ))
        );

        try {
            client.putScript(putScriptRequest);
            LOGGER.info("Successfully uploaded script: {}", scriptDefinition.getId());
        } catch (IOException exception) {
            throw ErrorCode.get(IndexingErrorCodes.SCRIPT_UPLOAD_FAILED, exception);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void uploadAll(final Collection<ScriptDefinition> scriptDefinitions) {
        for (final ScriptDefinition scriptDefinition : scriptDefinitions) {
            upload(scriptDefinition);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final String scriptId) {
        final DeleteScriptRequest deleteScriptRequest = DeleteScriptRequest.of(builder -> builder.id(scriptId));

        try {
            client.deleteScript(deleteScriptRequest);
            LOGGER.info("Successfully deleted script: {}", scriptId);
        } catch (IOException exception) {
            throw ErrorCode.get(IndexingErrorCodes.SCRIPT_DELETION_FAILED, exception);
        }
    }
}
