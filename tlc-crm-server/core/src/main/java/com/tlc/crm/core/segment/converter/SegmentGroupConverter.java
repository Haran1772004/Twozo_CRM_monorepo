package com.tlc.crm.core.segment.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.segment.models.Segment;
import com.tlc.crm.core.segment.models.SegmentCondition;
import com.tlc.crm.core.segment.models.SegmentConditionCriteria;
import com.tlc.crm.core.segment.models.SegmentConditionGroup;
import com.tlc.crm.entity.filter.api.manager.FilterManager;
import com.tlc.crm.entity.filter.api.models.FilterConnector;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides methods to convert {@link SegmentConditionGroup} collection into a JSON string and vice versa.
 * </p>
 *
 * @author  Mohammed Asif J.
 * @version 1.0
 */
public final class SegmentGroupConverter {

    private static final String KEY_GROUPS = "groups";
    private static final String KEY_CONDITIONS = "conditions";
    private static final String KEY_CONNECTOR_ID = "connectorId";
    private static final String KEY_TYPE_ID = "typeId";
    private static final String KEY_CRITERIA_ID = "criteriaId";

    private final FilterManager filterManager;

    private SegmentGroupConverter() {
        this.filterManager = FilterManager.getInstance();
    }

    public static SegmentGroupConverter getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static final class InstanceHolder {
        private static final SegmentGroupConverter INSTANCE = new SegmentGroupConverter();
    }

    /**
     * <p>
     * Constructs a {@link SegmentConditionGroup} from its minimal json representation.
     * </p>
     *
     * @param segment      the segment the group belongs to.
     * @param groupJson    the minimal group JSON.
     * @param criteriaById the preloaded criteria, keyed by id.
     * @return {@link SegmentConditionGroup} instance that hold the converted data.
     */
    public SegmentConditionGroup convertGroupFromJson(final Segment segment, final JsonObject groupJson,
                                                      final Map<Long, FilterCriteria> criteriaById) {
        final SegmentConditionGroup segmentConditionGroup = new SegmentConditionGroup();

        segmentConditionGroup.setConnector(groupJson.containsKey(KEY_CONNECTOR_ID)
                ? FilterConnector.get(groupJson.getInt(KEY_CONNECTOR_ID)) : FilterConnector.AND);
        segmentConditionGroup.setSegmentId(segment.id());

        if (groupJson.containsKey(KEY_CONDITIONS)) {
            final JsonArray conditionsArray = groupJson.getJsonArray(KEY_CONDITIONS);

            segmentConditionGroup.setConditions(null);
            final int size = conditionsArray.size();

            for (int index = NumericalConstant.ZERO; index < size; index++) {
                segmentConditionGroup.addCondition(
                        constructConditionFromJson(conditionsArray.getJsonObject(index), criteriaById)
                );
            }
        }

        return segmentConditionGroup;
    }

    /**
     * <p>
     * Deserializes groups and conditions from the JSON.
     * </p>
     *
     * @param orgId      the organization id.
     * @param segment    the segment to populate with groups and conditions.
     * @param groupsJson the JSON string containing the serialized groups.
     */
    public void loadGroupsFromJson(final Long orgId, final Segment segment, final String groupsJson) {
        if (Objects.isNull(groupsJson) || groupsJson.isBlank()) {
            return;
        }
        final JsonObject jsonObject = Json.object(groupsJson);

        if (!jsonObject.containsKey(KEY_GROUPS)) {
            return;
        }
        final JsonArray groupsArray = jsonObject.getJsonArray(KEY_GROUPS);
        final Map<Long, FilterCriteria> criteriaById = loadCriteriaByIdForGroups(orgId, groupsArray);

        segment.setSegmentConditionGroups(null);
        final int size = groupsArray.size();

        for (int index = NumericalConstant.ZERO; index < size; index++) {
            segment.addGroup(convertGroupFromJson(segment, groupsArray.getJsonObject(index), criteriaById));
        }
    }

    /**
     * <p>
     * Serializes the given segment condition groups into a minimal JSON string
     * containing only ids (connector id, type id, criteria id).
     * </p>
     *
     * @param segmentConditionGroups collection of {@link SegmentConditionGroup} to serialize.
     * @return JSON string holding the minimal group attributes.
     */
    public String convertGroupsToJsonString(final Collection<SegmentConditionGroup> segmentConditionGroups) {
        final JsonArray groupsArray = Json.array();

        for (final SegmentConditionGroup segmentConditionGroup : segmentConditionGroups) {
            final JsonObject groupJson = Json.object();
            final FilterConnector connector = segmentConditionGroup.getConnector();
            final Collection<SegmentCondition> conditions = segmentConditionGroup.getConditions();

            if (Objects.nonNull(connector)) {
                groupJson.put(KEY_CONNECTOR_ID, connector.getId());
            }

            if (Objects.nonNull(conditions) && !conditions.isEmpty()) {
                groupJson.put(KEY_CONDITIONS, convertConditionsToJson(conditions));
            }
            groupsArray.put(groupJson);
        }

        return Json.object().put(KEY_GROUPS, groupsArray).toString();
    }

    /**
     * <p>
     * Serializes the given conditions into a minimal JSON array containing only type and criteria ids.
     * </p>
     *
     * @param segmentConditions collection of {@link SegmentCondition}.
     * @return {@link JsonArray} holds the minimal condition attributes.
     */
    private JsonArray convertConditionsToJson(final Collection<SegmentCondition> segmentConditions) {
        final JsonArray conditionsArray = Json.array();

        for (final SegmentCondition segmentCondition : segmentConditions) {
            final JsonObject conditionJson = Json.object();
            final SegmentCondition.Type type = segmentCondition.getType();
            final SegmentConditionCriteria conditionCriteria = segmentCondition.getConditionCriteria();

            if (Objects.nonNull(type)) {
                conditionJson.put(KEY_TYPE_ID, type.getId());
            }

            if (Objects.nonNull(conditionCriteria)) {
                final FilterCriteria criteria = conditionCriteria.getFilterCriteria();

                if (Objects.nonNull(criteria) && Objects.nonNull(criteria.getId())) {
                    conditionJson.put(KEY_CRITERIA_ID, criteria.getId());
                }
            }
            conditionsArray.put(conditionJson);
        }

        return conditionsArray;
    }

    /**
     * <p>
     * Extracts all criteria ids from a minimal groups JSON string without fully deserializing it.
     * Used for efficient batch loading during updates.
     * </p>
     *
     * @param groupsJson the JSON string containing the serialized groups.
     * @return collection of criteria ids found in the JSON, or an empty collection if none found.
     */
    public Collection<Long> extractCriteriaIds(final String groupsJson) {
        if (Objects.isNull(groupsJson) || groupsJson.isBlank()) {
            return new HashSet<>();
        }
        final JsonObject jsonRoot = Json.object(groupsJson);

        return jsonRoot.containsKey(KEY_GROUPS) ? extractCriteriaIds(jsonRoot.getJsonArray(KEY_GROUPS)) : new HashSet<>();
    }

    /**
     * <p>
     * Extracts all criteria ids referenced by the given minimal group JSON array.
     * </p>
     *
     * @param groupsArray the minimal group JSON array.
     * @return collection of criteria ids found across all groups and conditions.
     */
    public Collection<Long> extractCriteriaIds(final JsonArray groupsArray) {
        final Collection<Long> criteriaIds = new HashSet<>();
        final int size = groupsArray.size();

        for (int groupIndex = NumericalConstant.ZERO; groupIndex < size; groupIndex++) {
            final JsonObject groupJson = groupsArray.getJsonObject(groupIndex);

            if (!groupJson.containsKey(KEY_CONDITIONS)) {
                continue;
            }
            final JsonArray conditionsArray = groupJson.getJsonArray(KEY_CONDITIONS);
            final int conditionSize = conditionsArray.size();

            for (int conditionIndex = NumericalConstant.ZERO; conditionIndex < conditionSize; conditionIndex++) {
                final JsonObject conditionJson = conditionsArray.getJsonObject(conditionIndex);

                if (conditionJson.containsKey(KEY_CRITERIA_ID)) {
                    criteriaIds.add(conditionJson.getLong(KEY_CRITERIA_ID));
                }
            }
        }

        return criteriaIds;
    }

    /**
     * <p>
     * Constructs a {@link SegmentCondition} from its json representation.
     * </p>
     *
     * @param conditionJson the minimal condition JSON.
     * @param criteriaById  the preloaded criteria, keyed by id.
     * @return {@link SegmentCondition} instance that hold the converted data.
     */
    private SegmentCondition constructConditionFromJson(final JsonObject conditionJson, final Map<Long, FilterCriteria> criteriaById) {
        final SegmentCondition segmentCondition = new SegmentCondition();

        if (conditionJson.containsKey(KEY_TYPE_ID)) {
            segmentCondition.setType(SegmentCondition.Type.get(conditionJson.getShort(KEY_TYPE_ID)));
        }

        if (conditionJson.containsKey(KEY_CRITERIA_ID)) {
            final Long criteriaId = conditionJson.getLong(KEY_CRITERIA_ID);
            final FilterCriteria criteria = criteriaById.getOrDefault(criteriaId, new FilterCriteria());

            criteria.setId(criteriaId);
            final SegmentConditionCriteria segmentConditionCriteria = new SegmentConditionCriteria();

            segmentConditionCriteria.setFilterCriteria(criteria);
            segmentCondition.setConditionCriteria(segmentConditionCriteria);
            segmentCondition.setConnector(criteria.getConnector());
        }

        return segmentCondition;
    }

    /**
     * <p>
     * Batch loads the {@link FilterCriteria} referenced by the given minimal group json array.
     * </p>
     *
     * @param orgId       the organization id.
     * @param groupsArray the minimal group JSON array.
     * @return map of criteria id to the loaded {@link FilterCriteria}.
     */
    private Map<Long, FilterCriteria> loadCriteriaByIdForGroups(final Long orgId, final JsonArray groupsArray) {
        final Collection<Long> criteriaIds = extractCriteriaIds(groupsArray);

        return criteriaIds.isEmpty() ? Map.of() : filterManager.getCriteria(orgId, criteriaIds).stream()
                .collect(Collectors.toMap(FilterCriteria::getId, Function.identity(), (oldValue, newValue) -> oldValue));
    }
}