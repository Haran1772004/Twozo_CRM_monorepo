package com.tlc.search.request.index.single.write.bulk;

import com.tlc.search.request.SingleIndexRequest;
import com.tlc.search.request.index.single.write.RemoveRequest;
import com.tlc.search.request.index.single.write.UpsertRequest;
import com.tlc.search.request.index.single.write.bulk.impl.BulkWriteRequestImpl;
import com.tlc.search.request.index.single.write.bulk.impl.BulkWriteRequestImpl.Operation;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Provides the request which is used to index, update and remove the documents.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface BulkWriteRequest extends SingleIndexRequest {

    /**
     * <p>
     * Provides the instance of {@link BulkWriteRequest}.
     * </p>
     *
     * @param index Refers the name which holds the group of documents.
     * @return {@link BulkWriteRequest} that hold the request to bulk operations of the document.
     */
    static BulkWriteRequest get(final String index) {
        return new BulkWriteRequestImpl(index);
    }

    /**
     * <p>
     * Sets the collection of {@link UpsertRequest} into {@link BulkWriteRequest}.
     * </p>
     *
     * @param upsertRequests Holds the collection of {@link UpsertRequest}.
     */
    void setInsertRequests(final Collection<UpsertRequest> upsertRequests);

    /**
     * <p>
     * Adds the {@link UpsertRequest} into {@link BulkWriteRequest}.
     * </p>
     *
     * @param upsertRequest Holds the {@link UpsertRequest}.
     */
    void addInsertRequest(final UpsertRequest upsertRequest);

    /**
     * <p>
     * Adds the {@link UpsertRequest} into {@link BulkWriteRequest}.
     * </p>
     *
     * @param upsertRequest Holds the {@link UpsertRequest}.
     */
    void addInsertRequest(final UpsertRequest upsertRequest, final int requestOrder);

    /**
     * <p>
     * Provides the {@link UpsertRequest} for the given id.
     * </p>
     *
     * @param id Document id.
     * @return {@link UpsertRequest} for the given id.
     */
    UpsertRequest getInsertRequest(final String id);

    /**
     * <p>
     * Provides the collection of {@link UpsertRequest}.
     * </p>
     *
     * @return Collection of {@link UpsertRequest}.
     */
    Collection<UpsertRequest> getInsertRequests();

    /**
     * <p>
     * Sets the collection of update requests into {@link BulkWriteRequest}.
     * </p>
     *
     * @param updateRequests Holds the collection of update requests.
     */
    void setUpdateRequests(final Collection<UpsertRequest> updateRequests);

    /**
     * <p>
     * Adds the update request into {@link BulkWriteRequest}.
     * </p>
     *
     * @param updateRequest Holds the update request.
     */
    void addUpdateRequest(final UpsertRequest updateRequest);

    /**
     * <p>
     * Adds the update request into {@link BulkWriteRequest}.
     * </p>
     *
     * @param updateRequest Holds the update request.
     */
    void addUpdateRequest(final UpsertRequest updateRequest, final int requestOrder);

    /**
     * <p>
     * Provides the update request for the given id.
     * </p>
     *
     * @param id Document id.
     * @return Update request for the given id.
     */
    UpsertRequest getUpdateRequest(final String id);

    /**
     * <p>
     * Provides the collection of update requests.
     * </p>
     *
     * @return Collection of update requests.
     */
    Collection<UpsertRequest> getUpdateRequests();

    /**
     * <p>
     * Sets the collection of {@link RemoveRequest} into {@link BulkWriteRequest}.
     * </p>
     *
     * @param removeRequests Holds the collection of {@link RemoveRequest}.
     */
    void setRemoveRequests(final Collection<RemoveRequest> removeRequests);

    /**
     * <p>
     * Adds the {@link RemoveRequest} into {@link BulkWriteRequest}.
     * </p>
     *
     * @param removeRequest Holds the {@link RemoveRequest}.
     */
    void addRemoveRequest(final RemoveRequest removeRequest);

    /**
     * <p>
     * Adds the {@link RemoveRequest} into {@link BulkWriteRequest}.
     * </p>
     *
     * @param removeRequest Holds the {@link RemoveRequest}.
     */
    void addRemoveRequest(final RemoveRequest removeRequest, final int requestOrder);

    /**
     * <p>
     * Provides the {@link RemoveRequest} for the given id.
     * </p>
     *
     * @param id Document id.
     * @return {@link RemoveRequest} for the given id.
     */
    RemoveRequest getRemoveRequest(final String id);

    /**
     * <p>
     * Provides the collection of {@link RemoveRequest}.
     * </p>
     *
     * @return Collection of {@link RemoveRequest}.
     */
    Collection<RemoveRequest> getRemoveRequests();

    /**
     * <p>
     * Provides the order of the available requests.
     * </p>
     *
     * @return Order of the available requests.
     */
    Map<Integer, Map.Entry<String, Operation>> getRequestOrders();

    /**
     * <p>
     * Checks whether the request containers has values or not.
     * </p>
     *
     * @return A flag that indicates the request containers has values or not.
     */
    boolean hasRequests();

    /**
     * <p>
     * Fetches the field removal request for the given id.
     * </p>
     *
     * @param id Document id.
     * @return Field removal request for the given id.
     */
    UpsertRequest getFieldRemovalRequest(final String id);

    /**
     * <p>
     * Adds the field removal request request into {@link BulkWriteRequest}.
     * </p>
     *
     * @param fieldRemovalRequest Holds the field removal request request.
     */
    void addFieldRemovalRequest(final UpsertRequest fieldRemovalRequest);

    /**
     * <p>
     * Sets a {@link Map} holding id & field names.
     * </p>
     *
     * @param fieldsMap Holds {@link Map} of field names for the given id.
     */
    void setFieldsToRemove(final Map<String, Collection<String>> fieldsMap);

    /**
     * <p>
     * Gets {@link Map} of field names that are to be removed.
     * </p>
     *
     * @return {@link Map} of field names for the given id.
     */
    Map<String, Collection<String>> getFieldsToRemoveMap();
}
