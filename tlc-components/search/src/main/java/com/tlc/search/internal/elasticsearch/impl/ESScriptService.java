package com.tlc.search.internal.elasticsearch.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ScriptLanguage;
import co.elastic.clients.elasticsearch._types.StoredScript;
import co.elastic.clients.elasticsearch.core.PutScriptRequest;
import co.elastic.clients.elasticsearch.core.DeleteScriptRequest;

import com.tlc.commons.code.ErrorCode;

import com.tlc.search.internal.elasticsearch.AbstractElasticSearchService;
import com.tlc.search.internal.status.IndexingErrorCodes;
import com.tlc.search.model.ScriptDefinition;
import com.tlc.search.service.ScriptService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import java.util.Collection;

/**
 * <p>
 * Provides the script service to manage stored scripts in Elasticsearch.
 * </p>
 *
 * @author Ajay
 * @version 1.0
 */
public class ESScriptService extends AbstractElasticSearchService implements ScriptService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ESScriptService.class);

    public ESScriptService(final ElasticsearchClient client) {
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
                        .lang(ScriptLanguage.Painless)
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


