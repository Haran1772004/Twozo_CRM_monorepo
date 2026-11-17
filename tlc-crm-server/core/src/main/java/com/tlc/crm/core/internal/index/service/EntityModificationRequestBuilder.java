package com.tlc.crm.core.internal.index.service;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.internal.index.service.impl.EntityModificationRequestBuilderImpl;
import com.tlc.search.request.index.single.write.UpsertRequest;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Fetches the necessary request to update the index.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface EntityModificationRequestBuilder {

    static EntityModificationRequestBuilder getInstance() {
        return EntityModificationRequestBuilderImpl.getInstance();
    }

    /**
     * <p>
     * Builds requests to modify entity.
     * </p>
     *
     * @param index       Index's name.
     * @param entityMap   Entity details.
     * @param isUpdate    Whether the action's update.
     * @param entityKey   Entity such as deal/contact/company.
     * @param fetchedJson Fetched json.
     * @return {@link UpsertRequest} in a collection.
     */
    Collection<UpsertRequest> buildEntityModificationRequests(final String index, final Map<String, Object> entityMap,
                                                              final boolean isUpdate, final String entityKey,
                                                              final JsonObject fetchedJson);

    /**
     * <p>
     * Builds requests to update followers.
     * </p>
     *
     * @param index       Index's name.
     * @param fetchedJson Fetched json.
     * @param entityKey   Entity such as deal/contact/company.
     * @param followerIds Followers ids.
     * @return {@link UpsertRequest} in a collection.
     */
    Collection<UpsertRequest> buildFollowersUpdateRequests(final String index, final JsonObject fetchedJson,
                                                           final String entityKey, final Collection<Long> followerIds);

    /**
     * <p>
     * Builds requests to remove followers.
     * </p>
     *
     * @param index           Index's name.
     * @param fetchedJson     Fetched json.
     * @param removableFields Name of the fields to be removed.
     * @return {@link UpsertRequest} in a collection.
     */
    Collection<UpsertRequest> buildFollowersRemovalRequests(final String index, final JsonObject fetchedJson,
                                                            final String... removableFields);
}
