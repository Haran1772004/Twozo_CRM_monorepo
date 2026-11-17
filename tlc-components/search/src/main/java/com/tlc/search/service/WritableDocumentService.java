package com.tlc.search.service;

import com.tlc.search.request.index.single.write.bulk.BulkWriteRequest;
import com.tlc.search.request.index.single.write.RemoveRequest;
import com.tlc.search.request.index.single.write.RestoreRequest;
import com.tlc.search.request.index.single.write.UpsertRequest;

/**
 * <p>
 * Provides the write service to execute DML operations.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface WritableDocumentService {

    /**
     * <p>
     * Stores/Updates the document with in specified index.
     * </p>
     *
     * @param upsertRequest {@link UpsertRequest} that holds the details of the data indexing.
     */
    void upsert(final UpsertRequest upsertRequest);

    /**
     * <p>
     * Removes the document for the corresponding criteria, whether the index has soft delete or hard delete.
     * </p>
     *
     * @param removeRequest {@link RemoveRequest} that holds the details of the removing criteria.
     */
    void remove(final RemoveRequest removeRequest);

    /**
     * <p>
     * Performs the DML actions for the given operations.
     * </p>
     *
     * @param bulkWriteRequest Holds the details of the index, update and remove requests.
     */
    void performBulkOperations(final BulkWriteRequest bulkWriteRequest);

    /**
     * <p>
     * Restores the deleted document.
     * </p>
     *
     * @param restoreRequest Holds the details about the restore request of the deleted document.
     */
    void restore(final RestoreRequest restoreRequest);
}
