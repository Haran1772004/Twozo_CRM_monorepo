package com.tlc.search.request.index.single.write.impl;

import com.tlc.commons.json.JsonObject;
import com.tlc.search.request.index.single.write.UpsertRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Collections;

/**
 * <p>
 * Provides the request which is used to index the document.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class UpsertRequestImpl extends AbstractSingleIndexWriteRequest implements UpsertRequest {

    private final Map<String, Object> doc;
    private final boolean isUpdate;

    private Collection<String> fieldsToRemove;

    public UpsertRequestImpl(final String id, final String index, final boolean isUpdate) {
        super(id, index);
        this.isUpdate = isUpdate;
        doc = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDoc(final String field, final Object value) {
        doc.put(field, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDoc(final Map<String, Object> doc) {
        this.doc.putAll(doc);
    }

    /**
     * {@inheritDoc}
     */
    public void setDoc(final JsonObject doc) {
        this.doc.putAll(doc.toMap());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUpdate() {
        return isUpdate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getDoc() {
        return doc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addFieldToRemove(final String field) {
        if (Objects.isNull(fieldsToRemove)) {
            fieldsToRemove = new ArrayList<>();
        }
        fieldsToRemove.add(field);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addFieldsToRemove(final Collection<String> fields) {
        if (Objects.isNull(fieldsToRemove)) {
            fieldsToRemove = new ArrayList<>();
        }
        fieldsToRemove.addAll(fields);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> fieldsToRemove() {
        return Objects.isNull(fieldsToRemove) ? Collections.emptyList() : fieldsToRemove;
    }
}
