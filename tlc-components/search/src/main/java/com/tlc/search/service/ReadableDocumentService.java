package com.tlc.search.service;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.search.model.SearchRequest;
import com.tlc.search.request.index.multi.read.MultiSearchIndexRequest;
import com.tlc.search.request.index.multi.read.count.MultiIndexCountRequest;
import com.tlc.search.request.index.multi.read.MultiSingleIndexRequest;
import com.tlc.search.request.index.single.read.SingleIndexSearchRequest;
import com.tlc.search.request.index.single.read.count.SingleIndexCountRequest;

import java.util.Collection;

/**
 * <p>
 * Provides the read service to read the available documents in different ways.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface ReadableDocumentService {

    /**
     * <p>
     * Gets docs and converts it into {@link JsonObject} based off of {@link SearchRequest}.
     * </p>
     *
     * @param searchRequest {@link SingleIndexSearchRequest}.
     * @return {@link JsonObject}.
     */
    JsonObject search(final SingleIndexSearchRequest searchRequest);

    /**
     * <p>
     * Gets docs and converts it into {@link JsonObject} based off of {@link SearchRequest}.
     * </p>
     *
     * @param multiSearchRequest {@link MultiSearchIndexRequest}.
     * @return {@link JsonObject}.
     */
    JsonObject search(final MultiSearchIndexRequest multiSearchRequest);

    /**
     * <p>
     * Gets docs and converts them into {@link JsonObject} grouped by index name for each msearch sub-request.
     * </p>
     *
     * @param multiSearchRequest {@link MultiSingleIndexRequest}.
     * @return {@link JsonObject} keyed by index name.
     */
    JsonObject multiSearchGroupedByIndex(final MultiSingleIndexRequest multiSearchRequest);

    /**
     * <p>
     * Gets docs for each msearch sub-request in submission order without merging by index name.
     * Each element is a {@link JsonArray} of documents returned for that sub-request.
     * </p>
     *
     * @param multiSearchRequest {@link MultiSingleIndexRequest}.
     * @return one {@link JsonArray} slot per executed sub-request, in the same order as {@link MultiSingleIndexRequest#get()}.
     */
    JsonArray multiSearchGroupedByOrder(final MultiSingleIndexRequest multiSearchRequest);

    /**
     * <p>
     * Fetches the document count..
     * </p>
     *
     * @param multiIndexCountRequest {@link MultiIndexCountRequest}.
     * @return doc count.
     */
    Long count(final MultiIndexCountRequest multiIndexCountRequest);

    /**
     * <p>
     * Fetches the document count.
     * </p>
     *
     * @param singleIndexCountRequest {@link SingleIndexCountRequest}.
     * @return doc count.
     */
    Long count(final SingleIndexCountRequest singleIndexCountRequest);

    /**
     * <p>
     * Checks whether the doc exists for the given id & index.
     * </p>
     *
     * @param id    Represents the doc's id.
     * @param index Represents the index name.
     * @return whether the doc exists.
     */
    boolean exists(final String id, final String index);

    /**
     * <p>
     * Gets the document ids for the given field & index.
     * </p>
     *
     * @param fieldName Represents the field name.
     * @param index     Represents the index name.
     * @param exists    True to check if field exists, false to check if field does not exist.
     * @return Collection of document ids.
     */
    Collection<String> getDocIdsBasedOnFieldExistence(final boolean exists, final String fieldName, final String index);
}