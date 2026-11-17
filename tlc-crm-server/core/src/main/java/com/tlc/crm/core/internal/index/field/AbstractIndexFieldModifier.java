package com.tlc.crm.core.internal.index.field;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.search.request.index.single.write.UpsertRequest;

import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Index field's value modifier.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public abstract class AbstractIndexFieldModifier {

    protected AbstractIndexFieldModifier() {
    }

    /**
     * <p>
     * Adds entity to the entity json.
     * </p>
     *
     * @param entityId        Entity's id.
     * @param entityKey       Entity keys such as Deal/Contact/Company.
     * @param nestedEntityKey Entity keys such as Contact/Company.
     * @param docJson         Doc json.
     * @param entityMap       Entity details.
     */
    protected void modifyEntity(final Long entityId, final String entityKey, final String nestedEntityKey,
                                final JsonObject docJson, final Map<String, Object> entityMap, final UpsertRequest updateRequest,
                                final String fieldToRemove) {
        final JsonObject entityJson = docJson.optJsonObject(entityKey);

        if (Objects.nonNull(entityJson)) {
            final JsonObject nestedEntityJson = entityJson.optJsonObject(nestedEntityKey);

            if (Objects.nonNull(nestedEntityJson) && Objects.equals(entityId, nestedEntityJson.getLong(CommonSearchFields.ID))) {
                entityJson.put(nestedEntityKey, entityMap);
                docJson.put(entityKey, entityJson);
                updateRequest.addFieldToRemove(fieldToRemove);
            }
        }
    }

    /**
     * <p>
     * Removes field.
     * </p>
     *
     * @param entityId        Entity's id.
     * @param entityKey       Entity keys such as Deal/Contact/Company.
     * @param nestedEntityKey Entity keys such as Contact/Company.
     * @param docJson         Doc json.
     * @param updateRequest   {@link UpsertRequest}.
     * @param fieldToRemove   Name of the field to be removed.
     */
    protected void removeField(final Long entityId, final String entityKey, final String nestedEntityKey,
                               final JsonObject docJson, final UpsertRequest updateRequest, final String fieldToRemove) {
        final JsonObject entityJson = docJson.optJsonObject(entityKey);

        if (!Json.isEmpty(entityJson)) {
            final JsonObject nestedEntityJson = entityJson.optJsonObject(nestedEntityKey);

            if (!Json.isEmpty(nestedEntityJson) && Objects.equals(entityId, nestedEntityJson.getLong(CommonSearchFields.ID))) {
                entityJson.remove(nestedEntityKey);
                updateRequest.addFieldToRemove(fieldToRemove);
            }
        }
    }

    /**
     * <p>
     * Removes field.
     * </p>
     *
     * @param entityId        Entity's id.
     * @param entityKey       Entity keys such as Deal/Contact/Company.
     * @param nestedEntityKey Entity keys such as Contact/Company.
     * @param docJson         Doc json.
     * @param updateRequest   {@link UpsertRequest}.
     * @param fieldToRemove   Name of the field to be removed.
     */
    protected void removeFollowersField(final Long entityId, final String entityKey, final String nestedEntityKey,
                                        final JsonObject docJson, final UpsertRequest updateRequest, final String fieldToRemove) {
        final JsonObject entityJson = docJson.optJsonObject(entityKey);

        if (!Json.isEmpty(entityJson)) {
            final JsonObject nestedEntityJson = entityJson.optJsonObject(nestedEntityKey);

            if (!Json.isEmpty(nestedEntityJson) && Objects.equals(entityId, nestedEntityJson.getLong(CommonSearchFields.ID))) {
                nestedEntityJson.remove(CommonSearchFields.FOLLOWER_IDS);
                entityJson.put(nestedEntityKey, nestedEntityJson);
                docJson.put(entityKey, entityJson);
                updateRequest.addFieldToRemove(fieldToRemove);
            }
        }
    }
}