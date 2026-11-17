package com.tlc.crm.workflow.internal.trigger.action.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.features.api.tag.SourceTagManager;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * Aggregates the json data like email, phone, tags of the entity with the existing data
 *
 * @author Maharaja S
 * @version 1.0
 */
public abstract class EntityJsonAggregator {

    protected final FieldService fieldService;
    protected final SourceTagManager sourceTagManager;

    protected EntityJsonAggregator() {
        this.sourceTagManager = SourceTagManager.getInstance();
        this.fieldService = FieldService.getInstance();
    }

    /**
     * <p>
     * Merges the data of an entity with the existing data
     * </p>
     *
     * @param orgId      Represents the organization id
     * @param entityJson Represents the json data of an entity
     */
    public abstract void merge(final long orgId, final JsonObject entityJson);

    /**
     * <p>
     * Merges the tag values of an entity with the existing tag values
     * </p>
     *
     * @param entityJson     Represents the entity json data
     * @param existingTagIds Represents the collection of tag ids mapped with an entity
     * @param tagsFieldKey   Represents the tags field key
     */
    protected void mergeTags(final JsonObject entityJson, final Collection<Long> existingTagIds, final String tagsFieldKey) {
        final JsonArray tagArray = entityJson.getJsonArray(tagsFieldKey);
        final Collection<Long> tagIds = new HashSet<>();
        final int arraySize = tagArray.size();

        for (int index = 0; index < arraySize; index++) {
            final JsonObject jsonData = tagArray.getJsonObject(index);

            if (Objects.nonNull(jsonData) && jsonData.containsKey(ActionAttributes.ID)) {
                tagIds.add(jsonData.getLong(ActionAttributes.ID));
            }
        }

        for (final Long tagId : existingTagIds) {
            if (!tagIds.contains(tagId)) {
                final JsonObject tagJson = Json.object();

                tagJson.put(ActionAttributes.ID, tagId);
                tagArray.put(tagJson);
            }
        }
        entityJson.put(tagsFieldKey, tagArray);
    }
}