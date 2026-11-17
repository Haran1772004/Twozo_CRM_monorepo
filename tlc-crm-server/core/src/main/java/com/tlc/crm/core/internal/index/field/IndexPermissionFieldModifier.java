package com.tlc.crm.core.internal.index.field;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.search.request.index.single.write.UpsertRequest;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Modifies permission related fields in an index.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class IndexPermissionFieldModifier extends AbstractIndexFieldModifier {

    private IndexPermissionFieldModifier() {
    }

    private static class Instance {
        private static final IndexPermissionFieldModifier INSTANCE = new IndexPermissionFieldModifier();
    }

    /**
     * <p>
     * Gets the {@link IndexPermissionFieldModifier} Instance
     * </p>
     *
     * @return {@link IndexPermissionFieldModifier}
     */
    public static IndexPermissionFieldModifier getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Adds follower in the entity.
     * </p>
     *
     * @param entityKey   Entity such as Deal/Contact/Company.
     * @param docJson     Doc about to be indexed.
     * @param followerIds Followers ids.
     */
    public void addFollowersToEntity(final String entityKey, final JsonObject docJson, final Collection<Long> followerIds) {
        final JsonObject entityJson = docJson.optJsonObject(entityKey);

        if (!Json.isEmpty(entityJson)) {
            entityJson.put(CommonSearchFields.FOLLOWER_IDS, followerIds);
            docJson.put(entityKey, entityJson);
        }
    }

    /**
     * <p>
     * Adds followers in the entity.
     * </p>
     *
     * @param entityId    Entity's id.
     * @param entityKey   Entity such as Deal/Contact/Company.
     * @param docJson     Doc json.
     * @param followerIds Followers ids.
     */
    public void addFollowersToEntity(final Long entityId, final String entityKey, final JsonObject docJson,
                                     final Collection<Long> followerIds) {
        final JsonObject entityJson = docJson.optJsonObject(entityKey);

        if (!Json.isEmpty(entityJson) && Objects.equals(entityId, entityJson.getLong(CommonSearchFields.ID))) {
            entityJson.put(CommonSearchFields.FOLLOWER_IDS, followerIds);
            docJson.put(entityKey, entityJson);
        }
    }

    /**
     * <p>
     * Adds contact followers in the entity.
     * </p>
     *
     * @param contactId          Contact's id.
     * @param entityKey          Entity such as Deal/Contact/Company.
     * @param docJson            Doc json.
     * @param contactFollowerIds Contact Followers ids.
     */
    public void addContactFollowersToEntity(final Long contactId, final String entityKey, final JsonObject docJson,
                                            final Collection<Long> contactFollowerIds) {
        final JsonObject entityJson = docJson.optJsonObject(entityKey);

        if (!Json.isEmpty(entityJson)) {
            final JsonObject nestedEntityJson = entityJson.optJsonObject(CommonSearchFields.CONTACT);

            if (!Json.isEmpty(nestedEntityJson) && Objects.equals(contactId, nestedEntityJson.getLong(CommonSearchFields.ID))) {
                nestedEntityJson.put(CommonSearchFields.FOLLOWER_IDS, contactFollowerIds);
                entityJson.put(CommonSearchFields.CONTACT, nestedEntityJson);
                docJson.put(entityKey, entityJson);
            }
        }
    }

    /**
     * <p>
     * Adds company followers in the entity.
     * </p>
     *
     * @param companyId          Company's id.
     * @param entityKey          Entity such as Deal/Contact/Company.
     * @param docJson            Doc json.
     * @param companyFollowerIds Company Followers ids.
     */
    public void addCompanyFollowersToEntity(final Long companyId, final String entityKey, final JsonObject docJson,
                                            final Collection<Long> companyFollowerIds) {
        final JsonObject entityJson = docJson.optJsonObject(entityKey);

        if (!Json.isEmpty(entityJson)) {
            final JsonObject companyJson = entityJson.optJsonObject(CommonSearchFields.COMPANY);

            if (!Json.isEmpty(companyJson) && Objects.equals(companyId, companyJson.getLong(CommonSearchFields.ID))) {
                companyJson.put(CommonSearchFields.FOLLOWER_IDS, companyFollowerIds);
                entityJson.put(CommonSearchFields.COMPANY, companyJson);
                docJson.put(entityKey, entityJson);
            }
        }
    }

    /**
     * <p>
     * Removes contact followers from the entity.
     * </p>
     *
     * @param entityKey     Entity such as Deal/Contact/Company.
     * @param docJson       Doc json.
     * @param updateRequest {@link UpsertRequest}.
     * @param fieldToRemove Name of the field to removed.
     */
    public void removeFollowersFromEntity(final String entityKey, final JsonObject docJson,
                                          final UpsertRequest updateRequest, final String fieldToRemove) {
        final JsonObject entityJson = docJson.optJsonObject(entityKey);

        if (!Json.isEmpty(entityJson)) {
            entityJson.remove(CommonSearchFields.FOLLOWER_IDS);
            docJson.put(entityKey, entityJson);
            updateRequest.addFieldToRemove(fieldToRemove);
        }
    }

    /**
     * <p>
     * Removes contact followers from the entity.
     * </p>
     *
     * @param entityId      Entity's id.
     * @param entityKey     Entity such as Deal/Contact/Company.
     * @param docJson       Doc json.
     * @param updateRequest {@link UpsertRequest}.
     * @param fieldToRemove Name of the field to removed.
     */
    public void removeFollowersFromEntity(final Long entityId, final String entityKey, final JsonObject docJson,
                                          final UpsertRequest updateRequest, final String fieldToRemove) {
        final JsonObject entityJson = docJson.optJsonObject(entityKey);

        if (!Json.isEmpty(entityJson) && Objects.equals(entityId, entityJson.getLong(CommonSearchFields.ID))) {
            entityJson.remove(CommonSearchFields.FOLLOWER_IDS);
            docJson.put(entityKey, entityJson);
            updateRequest.addFieldToRemove(fieldToRemove);
        }
    }

    /**
     * <p>
     * Removes contact followers from the entity.
     * </p>
     *
     * @param contactId     Contact's id.
     * @param entityKey     Entity such as Deal.
     * @param docJson       Doc json.
     * @param updateRequest {@link UpsertRequest}.
     * @param fieldToRemove Name of the field to removed.
     */
    public void removeContactFollowersFromEntity(final Long contactId, final String entityKey, final JsonObject docJson,
                                                 final UpsertRequest updateRequest, final String fieldToRemove) {
        removeFollowersField(contactId, entityKey, CommonSearchFields.CONTACT, docJson, updateRequest, fieldToRemove);
    }

    /**
     * <p>
     * Removes company followers from the entity.
     * </p>
     *
     * @param companyId     Company's id.
     * @param entityKey     Entity such as Deal.
     * @param docJson       Doc json.
     * @param updateRequest {@link UpsertRequest}.
     * @param fieldToRemove Name of the field to removed.
     */
    public void removeCompanyFollowersFromEntity(final Long companyId, final String entityKey, final JsonObject docJson,
                                                 final UpsertRequest updateRequest, final String fieldToRemove) {
        removeFollowersField(companyId, entityKey, CommonSearchFields.COMPANY, docJson, updateRequest, fieldToRemove);
    }
}