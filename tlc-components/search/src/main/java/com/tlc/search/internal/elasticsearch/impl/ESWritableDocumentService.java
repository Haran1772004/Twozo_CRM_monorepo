package com.tlc.search.internal.elasticsearch.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.OpType;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.InlineScript;
import co.elastic.clients.elasticsearch._types.ScriptLanguage;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.CreateOperation;
import co.elastic.clients.elasticsearch.core.bulk.DeleteOperation;
import co.elastic.clients.elasticsearch.core.bulk.UpdateAction;
import co.elastic.clients.elasticsearch.core.bulk.UpdateOperation;

import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.ObjectBuilder;
import com.tlc.commons.code.ErrorCode;

import com.tlc.search.fields.SearchKeys;
import com.tlc.search.internal.elasticsearch.AbstractElasticSearchService;
import com.tlc.search.internal.status.IndexingErrorCodes;
import com.tlc.search.request.index.single.write.bulk.BulkWriteRequest;
import com.tlc.search.request.index.single.write.UpsertRequest;
import com.tlc.search.request.index.single.write.RemoveRequest;
import com.tlc.search.request.index.single.write.RestoreRequest;
import com.tlc.search.request.index.single.write.bulk.impl.BulkWriteRequestImpl.Operation;
import com.tlc.search.service.WritableDocumentService;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * <p>
 * Provides the write service to execute DML operations.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class ESWritableDocumentService extends AbstractElasticSearchService implements WritableDocumentService {

    private static final String FIELDS_TO_REMOVE = "fieldsToRemove";

    public ESWritableDocumentService(final ElasticsearchClient client) {
        super(client);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void upsert(final UpsertRequest upsertRequest) {
        if (upsertRequest.isUpdate()) {
            update(upsertRequest);
        } else {
            insert(upsertRequest);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(final RemoveRequest removeRequest) {
        final DeleteRequest.Builder deleteRequestbuilder = new DeleteRequest.Builder();

        deleteRequestbuilder.index(removeRequest.getIndexName()).id(removeRequest.id());

        try {
            client.delete(deleteRequestbuilder.build());
        } catch (IOException e) {
            throw ErrorCode.get(IndexingErrorCodes.DOCUMENT_DELETION_FAILED, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void performBulkOperations(final BulkWriteRequest bulkWriteRequest) {
        final List<BulkOperation> bulkOperations = new ArrayList<>();

        for (final Map.Entry<String, Operation> requests : bulkWriteRequest.getRequestOrders().values()) {
            final String documentId = requests.getKey();

            switch (requests.getValue()) {
                case INSERT -> {
                    final UpsertRequest upsertRequest = bulkWriteRequest.getInsertRequest(documentId);
                    final BulkOperation.Builder builder = new BulkOperation.Builder();
                    final CreateOperation.Builder<Object> createOperationBuilder = new CreateOperation.Builder<>();

                    createOperationBuilder.index(upsertRequest.getIndexName()).id(upsertRequest.id()).document(upsertRequest.getDoc());
                    bulkOperations.add(builder.create(createOperationBuilder.build()).build());
                }
                case UPDATE -> {
                    final UpsertRequest updateRequest = bulkWriteRequest.getUpdateRequest(documentId);
                    final BulkOperation.Builder builder = new BulkOperation.Builder();
                    final UpdateOperation.Builder<Object, Object> updateOperationBuilder = new UpdateOperation.Builder<>();

                    updateOperationBuilder.index(updateRequest.getIndexName()).id(updateRequest.id())
                            .action(new UpdateAction.Builder<>().doc(updateRequest.getDoc()).docAsUpsert(true).build());
                    bulkOperations.add(builder.update(updateOperationBuilder.build()).build());
                }
                case REMOVE -> {
                    final RemoveRequest removeRequest = bulkWriteRequest.getRemoveRequest(documentId);
                    final BulkOperation.Builder builder = new BulkOperation.Builder();
                    final DeleteOperation.Builder deleteOperationBuilder = new DeleteOperation.Builder();

                    deleteOperationBuilder.index(removeRequest.getIndexName()).id(removeRequest.id());
                    bulkOperations.add(builder.delete(deleteOperationBuilder.build()).build());
                }
                case FIELD_REMOVAL -> {
                    final UpsertRequest fieldRemovalRequest = bulkWriteRequest.getFieldRemovalRequest(documentId);
                    final BulkOperation.Builder builder = new BulkOperation.Builder();
                    final UpdateOperation.Builder<Object, Object> fieldRemovalReqBuilder = new UpdateOperation.Builder<>();
                    final Function<InlineScript.Builder, ObjectBuilder<InlineScript>> inlineScriptBuilder = inlineScript -> {
                        inlineScript.lang(ScriptLanguage.Painless);
                        inlineScript.source(constructScriptCode());
                        inlineScript.params(FIELDS_TO_REMOVE, JsonData.of(bulkWriteRequest.getFieldsToRemoveMap().get(documentId)));

                        return inlineScript;
                    };
                    final Function<Script.Builder, ObjectBuilder<Script>> scriptFunction = scriptBuilder -> scriptBuilder.inline(inlineScriptBuilder);

                    fieldRemovalReqBuilder.index(fieldRemovalRequest.getIndexName()).id(fieldRemovalRequest.id())
                            .action(new UpdateAction.Builder<>().script(scriptFunction).build());
                    bulkOperations.add(builder.update(fieldRemovalReqBuilder.build()).build());
                }
            }
        }

        if (!bulkOperations.isEmpty()) {
            final BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();

            bulkRequestBuilder.index(bulkWriteRequest.getIndexName()).operations(bulkOperations);

            try {
                client.bulk(bulkRequestBuilder.build());
            } catch (IOException e) {
                throw ErrorCode.get(IndexingErrorCodes.BULK_REQUEST_EXECUTION_FAILED, e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restore(final RestoreRequest restoreRequest) {
        try {
            client.update(new UpdateRequest.Builder<Map, Map>().index(restoreRequest.getIndexName()).id(restoreRequest.id())
                    .doc(Map.of(SearchKeys.DELETED, false)).docAsUpsert(true).build(), Map.class);
        } catch (IOException e) {
            throw ErrorCode.get(IndexingErrorCodes.DOCUMENT_RESTORE_FAILED, e);
        }
    }

    /**
     * <p>
     * Stores the document with in specified index.
     * </p>
     *
     * @param upsertRequest {@link UpsertRequest} that holds the details of the data indexing.
     */
    private void insert(final UpsertRequest upsertRequest) {
        final IndexRequest.Builder<Map> indexRequestBuilder = new IndexRequest.Builder<>();

        indexRequestBuilder.index(upsertRequest.getIndexName()).id(upsertRequest.id()).document(upsertRequest.getDoc()).opType(OpType.Index);

        try {
            client.index(indexRequestBuilder.build());
        } catch (IOException e) {
            throw ErrorCode.get(IndexingErrorCodes.DOCUMENT_INDEXING_FAILED, e);
        }
    }

    /**
     * <p>
     * Updates the existing document in the given index.
     * </p>
     *
     * @param updateRequest {@link UpsertRequest} that holds the details of the updating the document.
     */
    private void update(final UpsertRequest updateRequest) {
        final UpdateRequest.Builder<Map, Map> updateRequestBuilder = new UpdateRequest.Builder<>();

        updateRequestBuilder.index(updateRequest.getIndexName()).id(updateRequest.id()).doc(updateRequest.getDoc()).docAsUpsert(true);

        try {
            client.update(updateRequestBuilder.build(), Map.class);
        } catch (IOException e) {
            throw ErrorCode.get(IndexingErrorCodes.DOCUMENT_UPDATE_FAILED, e);
        }
    }

    /**
     * <p>
     * Constructs script code that is to be executed.
     * </p>
     *
     * @return code to be executed.
     */
    private String constructScriptCode() {
        final StringBuffer scriptBuilder = new StringBuffer();

        scriptBuilder.append("def fieldsToRemove = params.fieldsToRemove; ");
        scriptBuilder.append("for (String field : fieldsToRemove) { ctx._source.remove(field) }");

        return scriptBuilder.toString();
    }
}