package com.tlc.crm.common.list.sort;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.list.sort.models.SortCriteria;
import com.tlc.crm.common.list.sort.models.SortFields;

import java.util.Collection;

/**
 * <p>
 * Converts the {@link SortCriteria} related data
 * </p>
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public final class SortConverter {

    private SortConverter() {
    }

    private static class Instance {
        private static final SortConverter SORT_CONFIG_CONVERTER = new SortConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link SortConverter}
     * </p>
     *
     * @return The instance of {@link SortConverter}
     */
    public static SortConverter getInstance() {
        return Instance.SORT_CONFIG_CONVERTER;
    }

    /**
     * <p>
     * Converts the {@link Collection} of {@link SortCriteria} to {@link JsonArray}
     * </p>
     *
     * @param sortCriteriaCollection The {@link Collection} of {@link SortCriteria}
     * @return The {@link JsonArray} containing sort criteria data
     */
    public JsonArray convertModelsToJson(final Collection<SortCriteria> sortCriteriaCollection) {
        final JsonArray sortCriteriaArray = Json.array();

        for (final SortCriteria sortCriteria : sortCriteriaCollection) {
            final JsonObject sortCriteriaJson = Json.object();

            sortCriteriaJson.put(SortFields.FIELD_ID, sortCriteria.getFieldId());
            sortCriteriaJson.put(SortFields.FIELD, sortCriteria.getFieldName());
            sortCriteriaJson.put(SortFields.TYPE, sortCriteria.getSortType().getOrderTypeName());
            sortCriteriaJson.put(SortFields.ORDER, sortCriteria.getOrder());
            sortCriteriaArray.put(sortCriteriaJson);
        }
        return sortCriteriaArray;
    }
}