package com.tlc.search.internal.opensearch.impl;

import com.tlc.commons.code.ErrorCode;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.search.fields.SearchKeys;
import com.tlc.search.internal.opensearch.AbstractOpenSearchService;
import com.tlc.search.internal.status.IndexingErrorCodes;
import com.tlc.search.request.index.single.write.bulk.BulkWriteRequest;
import com.tlc.search.request.index.single.write.RemoveRequest;
import com.tlc.search.request.index.single.write.RestoreRequest;
import com.tlc.search.request.index.single.write.UpsertRequest;
import com.tlc.search.request.index.single.write.bulk.impl.BulkWriteRequestImpl.Operation;
import com.tlc.search.service.WritableDocumentService;

import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpType;
import org.opensearch.client.opensearch._types.Script;
import org.opensearch.client.opensearch._types.ScriptField;
import org.opensearch.client.opensearch._types.StoredScriptId;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.UpdateRequest;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.CreateOperation;
import org.opensearch.client.opensearch.core.bulk.DeleteOperation;
import org.opensearch.client.opensearch.core.bulk.UpdateOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Collection;
import java.util.HashMap;

/**
 * <p>
 * Provides the write service to execute DML operations.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class OSWritableDocumentService extends AbstractOpenSearchService implements WritableDocumentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OSWritableDocumentService.class);
    private static final String FIELDS_TO_REMOVE = "fieldsToRemove";
    private static final String FIELDS_TO_REMOVE_SCRIPT = "fields_to_remove_script";

    public OSWritableDocumentService(final OpenSearchClient client) {
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
        final List<BulkOperation> bulkScriptOperations = new ArrayList<>();
        final String index = bulkWriteRequest.getIndexName();

        for (final Map.Entry<String, Operation> requests : bulkWriteRequest.getRequestOrders().values()) {
            final String documentId = requests.getKey();

            switch (requests.getValue()) {
                case INSERT -> {
                    final UpsertRequest upsertRequest = bulkWriteRequest.getInsertRequest(documentId);
                    final Map<String, Object> doc = upsertRequest.getDoc();

                    if (Objects.nonNull(doc) && !doc.isEmpty()) {
                        final BulkOperation.Builder builder = new BulkOperation.Builder();
                        final CreateOperation.Builder<Object> createOperationBuilder = new CreateOperation.Builder<>();

                        createOperationBuilder.index(upsertRequest.getIndexName()).id(upsertRequest.id()).document(doc);
                        bulkOperations.add(builder.create(createOperationBuilder.build()).build());
                    }
                }
                case UPDATE -> {
                    final UpsertRequest updateRequest = bulkWriteRequest.getUpdateRequest(documentId);
                    final String requestId = updateRequest.id();
                    final Script script = buildFieldsToRemoveScript(updateRequest.fieldsToRemove());
                    final Map<String, Object> doc = updateRequest.getDoc();

                    if (Objects.nonNull(script)) {
                        final BulkOperation.Builder scriptOperationBuilder = new BulkOperation.Builder();
                        final UpdateOperation.Builder<Object> scriptUpdateBuilder = new UpdateOperation.Builder<>();

                        scriptUpdateBuilder.id(requestId).script(script).index(index);
                        bulkScriptOperations.add(scriptOperationBuilder.update(scriptUpdateBuilder.build()).build());
                    }

                    if (Objects.nonNull(doc) && !doc.isEmpty()) {
                        final BulkOperation.Builder builder = new BulkOperation.Builder();
                        final UpdateOperation.Builder<Object> updateOperationBuilder = new UpdateOperation.Builder<>();

                        updateOperationBuilder.index(index).id(requestId).document(doc).docAsUpsert(true);
                        bulkOperations.add(builder.update(updateOperationBuilder.build()).build());
                    }
                }
                case REMOVE -> {
                    final RemoveRequest removeRequest = bulkWriteRequest.getRemoveRequest(documentId);
                    final BulkOperation.Builder builder = new BulkOperation.Builder();
                    final DeleteOperation.Builder deleteOperationBuilder = new DeleteOperation.Builder();

                    deleteOperationBuilder.index(index).id(removeRequest.id());
                    bulkOperations.add(builder.delete(deleteOperationBuilder.build()).build());
                }
            }
        }

        performBulkOperations(bulkScriptOperations, index);
        performBulkOperations(bulkOperations, index);
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
     * Performs bulk operations.
     * </p>
     *
     * @param bulkOperations   {@link BulkOperation}.
     * @param index            Index's name.
     */
    private void performBulkOperations(final List<BulkOperation> bulkOperations, final String index) {

        if (!bulkOperations.isEmpty()) {
            final BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();

            bulkRequestBuilder.index(index).operations(bulkOperations);

            try {
                client.bulk(bulkRequestBuilder.build());
            } catch (IOException e) {
                LoggerUtil.logError(LOGGER, e, "Search Action: Bulk Operation failed: Message: {}", e.getMessage());
                e.printStackTrace();
                throw ErrorCode.get(IndexingErrorCodes.BULK_REQUEST_EXECUTION_FAILED, e);
            }
        }
    }

    /**
     * <p>
     * Builds the script for fields to removed via update operation.
     * </p>
     *
     * @param fieldsToRemove Fields to be removed.
     * @return {@link Script}.
     */
    private Script buildFieldsToRemoveScript(final Collection<String> fieldsToRemove) {

        if (fieldsToRemove.isEmpty()) {
            return null;
        } else {
            final Map<String, JsonData> paramsJsonDataMap = new HashMap<>(fieldsToRemove.size());

            paramsJsonDataMap.put(FIELDS_TO_REMOVE, JsonData.of(fieldsToRemove));

            return ScriptField.of(scriptFieldFunction -> scriptFieldFunction.script(Script.of(
                    scriptFunction -> scriptFunction.stored(StoredScriptId.of(
                            stored -> stored.id(FIELDS_TO_REMOVE_SCRIPT).params(paramsJsonDataMap)))))).script();
        }
    }
}