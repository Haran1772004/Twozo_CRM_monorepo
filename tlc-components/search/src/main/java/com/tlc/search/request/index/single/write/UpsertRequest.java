package com.tlc.search.request.index.single.write;

import com.tlc.commons.json.JsonObject;
import com.tlc.search.request.index.single.write.impl.UpsertRequestImpl;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Provides the request which is used to index the documents.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface UpsertRequest extends SingleIndexWriteRequest {

    /**
     * <p>
     * Provides the instance of {@link UpsertRequest}.
     * </p>
     *
     * @param id    Primary value to index the document.
     * @param index Refers the name which holds the group of documents.
     * @return {@link UpsertRequest} that hold the request to index the given data.
     */
    static UpsertRequest get(final String id, final String index) {
        return get(id, index, false);
    }

    /**
     * <p>
     * Provides the instance of {@link UpsertRequest}.
     * </p>
     *
     * @param id       Primary value to index the document.
     * @param index    Refers the name which holds the group of documents.
     * @param isUpdate A flag that decides the request is to perform update or create action.
     * @return {@link UpsertRequest} that hold the request to index the given data.
     */
    static UpsertRequest get(final String id, final String index, final boolean isUpdate) {
        return new UpsertRequestImpl(id, index, isUpdate);
    }

    /**
     * <p>
     * Adds the document into {@link UpsertRequest}.
     * </p>
     *
     * @param field Holds the field name
     * @param value Holds the value of the respective field.
     */
    void addDoc(final String field, final Object value);

    /**
     * <p>
     * Adds the document into {@link UpsertRequest}.
     * </p>
     *
     * @param doc document.
     */
    void setDoc(final Map<String, Object> doc);

    /**
     * <p>
     * Adds the document into {@link UpsertRequest}.
     * </p>
     *
     * @param doc document.
     */
    void setDoc(final JsonObject doc);

    /**
     * <p>
     * Returns the flag that decides the request is to perform update or create action.
     * </p>
     *
     * @return Flag that decides the request is to perform update or create action.
     */
    boolean isUpdate();

    /**
     * <p>
     * Provides the {@link Map} which holds the details of an document.
     * </p>
     *
     * @return Map of document.
     */
    Map<String, Object> getDoc();

    /**
     * <p>
     * Adds a field to be removed.
     * </p>
     *
     */
    void addFieldToRemove(final String field);

    /**
     * <p>
     * Adds a field to be removed.
     * </p>
     *
     */
    void addFieldsToRemove(final Collection<String> fields);

    /**
     * <p>
     * Collection of fields to be removed.
     * </p>
     *
     * @return fields to be removed.
     */
    Collection<String> fieldsToRemove();
}
