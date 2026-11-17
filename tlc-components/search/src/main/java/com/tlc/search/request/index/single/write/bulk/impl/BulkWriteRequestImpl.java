package com.tlc.search.request.index.single.write.bulk.impl;

import com.tlc.search.request.index.single.read.impl.AbstractSingleIndexRequest;
import com.tlc.search.request.index.single.write.bulk.BulkWriteRequest;
import com.tlc.search.request.index.single.write.UpsertRequest;
import com.tlc.search.request.index.single.write.RemoveRequest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * <p>
 * Provides the request which is used to index, update and remove the documents.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class BulkWriteRequestImpl extends AbstractSingleIndexRequest implements BulkWriteRequest {

    public enum Operation {
        INSERT,
        UPDATE,
        REMOVE,
        FIELD_REMOVAL
    }

    private final Map<Integer, Map.Entry<String, Operation>> requestOrders;

    private int order = 1;
    private Map<String, UpsertRequest> indexRequests;
    private Map<String, UpsertRequest> updateRequests;
    private Map<String, RemoveRequest> removeRequests;
    private Map<String, UpsertRequest> fieldRemovalRequests;
    private Map<String, Collection<String>> fieldsToRemoveMap;

    public BulkWriteRequestImpl(final String index) {
        super(index);
        requestOrders = new TreeMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInsertRequests(final Collection<UpsertRequest> upsertRequests) {
        this.indexRequests = Objects.isNull(this.indexRequests) ? new HashMap<>() : this.indexRequests;

        for (final UpsertRequest upsertRequest : upsertRequests) {
            final String id = upsertRequest.id();

            this.indexRequests.put(id, upsertRequest);
            requestOrders.put(order++, Map.entry(id, Operation.INSERT));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addInsertRequest(final UpsertRequest upsertRequest) {
        addInsertRequest(upsertRequest, order++);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addInsertRequest(final UpsertRequest upsertRequest, final int requestOrder) {
        final String id = upsertRequest.id();
        indexRequests = Objects.isNull(indexRequests) ? new HashMap<>() : indexRequests;

        indexRequests.put(id, upsertRequest);
        this.requestOrders.put(isDuplicateOrder(requestOrder) ? order++ : requestOrder, Map.entry(id, Operation.INSERT));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UpsertRequest getInsertRequest(final String id) {
        return indexRequests.get(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<UpsertRequest> getInsertRequests() {
        return indexRequests.values();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUpdateRequests(final Collection<UpsertRequest> updateRequests) {
        this.updateRequests = Objects.isNull(this.updateRequests) ? new HashMap<>() : this.updateRequests;

        for (final UpsertRequest updateRequest : updateRequests) {
            final String id = updateRequest.id();

            this.updateRequests.put(id, updateRequest);
            requestOrders.put(order++, Map.entry(id, Operation.UPDATE));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addUpdateRequest(final UpsertRequest updateRequest) {
        addUpdateRequest(updateRequest, order++);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addUpdateRequest(final UpsertRequest updateRequest, final int requestOrder) {
        final String id = updateRequest.id();
        updateRequests = Objects.isNull(updateRequests) ? new HashMap<>() : updateRequests;

        updateRequests.put(id, updateRequest);
        this.requestOrders.put(isDuplicateOrder(requestOrder) ? order++ : requestOrder, Map.entry(id, Operation.UPDATE));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UpsertRequest getUpdateRequest(final String id) {
        return updateRequests.get(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<UpsertRequest> getUpdateRequests() {
        return updateRequests.values();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRemoveRequests(final Collection<RemoveRequest> removeRequests) {
        this.removeRequests = Objects.isNull(this.removeRequests) ? new HashMap<>() : this.removeRequests;

        for (final RemoveRequest removeRequest : removeRequests) {
            final String id = removeRequest.id();

            this.removeRequests.put(id, removeRequest);
            requestOrders.put(order++, Map.entry(id, Operation.REMOVE));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addRemoveRequest(final RemoveRequest removeRequest) {
        addRemoveRequest(removeRequest, order++);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addRemoveRequest(final RemoveRequest removeRequest, final int requestOrder) {
        final String id = removeRequest.id();
        removeRequests = Objects.isNull(removeRequests) ? new HashMap<>() : removeRequests;

        removeRequests.put(id, removeRequest);
        this.requestOrders.put(isDuplicateOrder(requestOrder) ? order++ : requestOrder, Map.entry(id, Operation.REMOVE));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RemoveRequest getRemoveRequest(final String id) {
        return removeRequests.get(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<RemoveRequest> getRemoveRequests() {
        return removeRequests.values();
    }

    @Override
    public Map<Integer, Map.Entry<String, Operation>> getRequestOrders() {
        return requestOrders;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasRequests() {
        return !requestOrders.isEmpty();
    }

    /**
     * <p>
     * Adds the field removal request request into {@link BulkWriteRequest}.
     * </p>
     *
     * @param fieldRemovalRequest Holds the field removal request request.
     */
    public void addFieldRemovalRequest(final UpsertRequest fieldRemovalRequest) {
        addFieldRemovalRequest(fieldRemovalRequest, order++);
    }

    /**
     * <p>
     * Fetches the field removal request for the given id.
     * </p>
     *
     * @param id Document id.
     * @return Field removal request for the given id.
     */
    public UpsertRequest getFieldRemovalRequest(final String id) {
        return fieldRemovalRequests.get(id);
    }

    /**
     * <p>
     * Sets a {@link Map} holding id & field names.
     * </p>
     *
     * @param fieldsMap Holds {@link Map} of field names for the given id.
     */
    public void setFieldsToRemove(final Map<String, Collection<String>> fieldsMap) {
        this.fieldsToRemoveMap = fieldsMap;
    }

    /**
     * <p>
     * Gets {@link Map} of field names that are to be removed.
     * </p>
     *
     * @return {@link Map} of field names for the given id.
     */
    public Map<String, Collection<String>> getFieldsToRemoveMap() {
        return fieldsToRemoveMap;
    }

    /**
     * <p>
     * Adds the field removal request request into {@link BulkWriteRequest}.
     * </p>
     *
     * @param fieldRemovalRequest Holds the field removal request request.
     */
    private void addFieldRemovalRequest(final UpsertRequest fieldRemovalRequest, final int requestOrder) {
        final String id = fieldRemovalRequest.id();

        fieldRemovalRequests = Objects.isNull(fieldRemovalRequests) ? new HashMap<>() : fieldRemovalRequests;
        fieldRemovalRequests.put(id, fieldRemovalRequest);
        this.requestOrders.put(isDuplicateOrder(requestOrder) ? order++ : requestOrder, Map.entry(id, Operation.FIELD_REMOVAL));
    }

    /**
     * <p>
     * Returns a flag that indicates whether the order is duplicate or not.
     * </p>
     *
     * @param order Given order.
     * @return A flag that indicates whether the order is duplicate or not.
     */
    private boolean isDuplicateOrder(final int order) {
        return requestOrders.containsKey(order);
    }
}
