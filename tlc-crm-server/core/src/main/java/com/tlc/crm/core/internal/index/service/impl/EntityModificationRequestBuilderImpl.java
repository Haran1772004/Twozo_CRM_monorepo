package com.tlc.crm.core.internal.index.service.impl;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.internal.index.service.EntityModificationRequestBuilder;
import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.search.request.index.single.write.UpsertRequest;

import java.util.Collection;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * <p>
 * Fetches the necessary request to update the index.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class EntityModificationRequestBuilderImpl implements EntityModificationRequestBuilder {

    private EntityModificationRequestBuilderImpl() {
    }

    private static class Instance {
        private static final EntityModificationRequestBuilderImpl INSTANCE = new EntityModificationRequestBuilderImpl();
    }

    /**
     * <p>
     * Gets the {@link EntityModificationRequestBuilderImpl} Instance
     * </p>
     *
     * @return {@link EntityModificationRequestBuilderImpl}
     */
    public static EntityModificationRequestBuilderImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets requests to modify entity.
     * </p>
     *
     * @param index       Index's name.
     * @param entityMap   Entity details.
     * @param isUpdate    Whether the action's update.
     * @param entityKey   Entity such as deal/contact/company.
     * @param fetchedJson Fetched json.
     * @return {@link UpsertRequest} in a collection.
     */
    @Override
    public Collection<UpsertRequest> buildEntityModificationRequests(final String index, final Map<String, Object> entityMap,
                                                                     final boolean isUpdate, final String entityKey,
                                                                     final JsonObject fetchedJson) {
        final Collection<UpsertRequest> updateRequests = new ArrayList<>();

        if (!Json.isEmpty(fetchedJson)) {
            final JsonArray docArray = fetchedJson.getJsonArray(index);
            final int size = docArray.size();

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                final JsonObject docJson = docArray.getJsonObject(count);
                final UpsertRequest updateRequest = UpsertRequest.get(docJson.getString(CommonSearchFields.ID), index);

                if (isUpdate) {
                    docJson.put(entityKey, entityMap);
                } else {
                    docJson.remove(entityKey);
                }
                updateRequest.addFieldToRemove(entityKey);
                updateRequest.setDoc(docJson);
                updateRequests.add(updateRequest);
            }
        }
        return updateRequests;
    }

    /**
     * <p>
     * Gets requests to update followers.
     * </p>
     *
     * @param index       Index's name.
     * @param fetchedJson Fetched json.
     * @param entityKey   Entity such as deal/contact/company.
     * @param followerIds Followers ids.
     * @return {@link UpsertRequest} in a collection.
     */
    @Override
    public Collection<UpsertRequest> buildFollowersUpdateRequests(final String index, final JsonObject fetchedJson,
                                                                  final String entityKey, final Collection<Long> followerIds) {
        final Collection<UpsertRequest> upsertRequests = new ArrayList<>();

        if (!Json.isEmpty(fetchedJson)) {
            final JsonArray docArray = fetchedJson.getJsonArray(index);
            final int size = docArray.size();

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                final JsonObject docJson = docArray.getJsonObject(count);
                final JsonObject entityJson = docJson.getJsonObject(entityKey);
                final UpsertRequest upsertRequest = UpsertRequest.get(docJson.getString(CommonSearchFields.ID), index);

                entityJson.put(CommonSearchFields.FOLLOWER_IDS, followerIds);
                docJson.put(entityKey, entityJson);
                upsertRequest.setDoc(docJson);
                upsertRequests.add(upsertRequest);
            }
        }
        return upsertRequests;
    }

    /**
     * <p>
     * Gets requests to remove followers.
     * </p>
     *
     * @param index           Index's name.
     * @param fetchedJson     Fetched json.
     * @param removableFields Name of the fields to be removed.
     * @return {@link UpsertRequest} in a collection.
     */
    public Collection<UpsertRequest> buildFollowersRemovalRequests(final String index, final JsonObject fetchedJson,
                                                                   final String... removableFields) {
        final Collection<UpsertRequest> updateRequests = new ArrayList<>();

        if (!Json.isEmpty(fetchedJson)) {
            final JsonArray noteAndFileArray = fetchedJson.getJsonArray(index);
            final int size = noteAndFileArray.size();

            for (int count = NumericalConstant.ZERO; count < size; count++) {
                final UpsertRequest updateRequest = UpsertRequest.get(
                        noteAndFileArray.getJsonObject(count).getString(CommonSearchFields.ID), index);

                updateRequest.addFieldsToRemove(Arrays.stream(removableFields).toList());
                updateRequests.add(updateRequest);
            }
        }
        return updateRequests;
    }
}