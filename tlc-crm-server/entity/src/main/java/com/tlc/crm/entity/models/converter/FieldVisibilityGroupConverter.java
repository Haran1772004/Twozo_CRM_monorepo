package com.tlc.crm.entity.models.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.models.internal.FieldVisibilityCriteria;
import com.tlc.crm.entity.models.internal.FieldVisibilityCriteriaGroup;
import com.tlc.crm.entity.service.internal.AbstractFieldService;
import com.tlc.i18n.I18nResolutionAdapter;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * A converter that converts the {@link FieldVisibilityCriteriaGroup} details
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public final class FieldVisibilityGroupConverter {

    private FieldVisibilityGroupConverter() {
    }

    private static class Instance {
        private static final FieldVisibilityGroupConverter INSTANCE = new FieldVisibilityGroupConverter();
    }

    /**
     * Gets the instance of {@link AbstractFieldService}.
     *
     * @return The {@link AbstractFieldService}
     */
    public static FieldVisibilityGroupConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Loads the {@link FieldVisibilityCriteriaGroup} information into a {@link Json} representation
     * </p>
     *
     * @param orgId                        Represents an organization id
     * @param jsonObject                   The {@link JsonObject} to be updated with the converted {@link FieldVisibilityCriteriaGroup} details.
     * @param fieldVisibilityCriteriaGroup Holds the {@link FieldVisibilityCriteriaGroup} details.
     * @param i18NResolutionAdapter        Refers the {@link I18nResolutionAdapter} for resolve the i18n data.
     */
    public void loadCriteriaGroupIntoJson(final Long orgId, final JsonObject jsonObject, final FieldVisibilityCriteriaGroup fieldVisibilityCriteriaGroup,
                                          final I18nResolutionAdapter i18NResolutionAdapter, final String fieldVisibilityKey) {
        final JsonObject showWhenJson = Json.object();
        final JsonArray criteriaGroupArray = Json.array();
        final Collection<FieldVisibilityCriteria> fieldVisibilityCriteria = fieldVisibilityCriteriaGroup.getFieldVisibilityCriteria();
        final FieldVisibilityCriteria criteria = Objects.isNull(fieldVisibilityCriteria) ? null : fieldVisibilityCriteria.stream().findFirst().orElse(null);
        final Collection<FieldVisibilityCriteriaGroup> childFieldVisibilityCriteriaGroups = fieldVisibilityCriteriaGroup.getChildFieldVisibilityCriteriaGroups();

        showWhenJson.put(FieldKeys.CONNECTOR.getName(), fieldVisibilityCriteriaGroup.getConnector());

        if (Objects.nonNull(criteria)) {
            loadCriteriaIntoJson(showWhenJson, criteria, i18NResolutionAdapter);
        }

        if (Objects.nonNull(childFieldVisibilityCriteriaGroups)) {
            loadCriteriaGroupsIntoJsonArray(orgId, criteriaGroupArray, childFieldVisibilityCriteriaGroups, i18NResolutionAdapter);
        }

        if (criteriaGroupArray.size() > 0) {
            showWhenJson.put(FieldKeys.RULES.getName(), criteriaGroupArray);
        }

        if (showWhenJson.size() > 0) {
            jsonObject.put(fieldVisibilityKey, showWhenJson);
        }
    }

    /**
     * <p>
     * Loads the {@link FieldVisibilityCriteria} model into a {@link JsonObject}
     * </p>
     *
     * @param rulesJson             Represents the rule json
     * @param criteria              Represents the {@link FieldVisibilityCriteria} models to be converter
     * @param i18NResolutionAdapter Represents an {@link I18nResolutionAdapter}
     */
    private void loadCriteriaIntoJson(final JsonObject rulesJson, final FieldVisibilityCriteria criteria, final I18nResolutionAdapter i18NResolutionAdapter) {
        rulesJson.put(FieldKeys.CONDITION.getName(), criteria.getCondition());
        rulesJson.put(FieldKeys.VALUE.getName(), criteria.getValue());
        rulesJson.put(FieldKeys.FIELD.getName(), criteria.getFieldId());
        rulesJson.put(FieldKeys.VALIDATION_MESSAGE.getName(), i18NResolutionAdapter.get(criteria.getValidationMessage()));
    }

    /**
     * <p>
     * Loads a Collection of {@link FieldVisibilityCriteriaGroup} models into a {@link JsonArray}
     * </p>
     *
     * @param orgId                              Represents the organization id
     * @param ruleGroupsJsonArray                The existing {@link JsonArray} to which the converted {@link FieldVisibilityCriteriaGroup} will be appended
     * @param childFieldVisibilityCriteriaGroups The Collection of {@link FieldVisibilityCriteriaGroup} models to be converted
     * @param i18NResolutionAdapter              Refers the {@link I18nResolutionAdapter} for resolve the i18n data.
     * @return A {@link JsonArray} containing the converted {@link FieldVisibilityCriteriaGroup}, appended to the provided ruleGroupsJsonArray
     */
    private void loadCriteriaGroupsIntoJsonArray(final Long orgId, final JsonArray ruleGroupsJsonArray,
                                                 final Collection<FieldVisibilityCriteriaGroup> childFieldVisibilityCriteriaGroups,
                                                 final I18nResolutionAdapter i18NResolutionAdapter) {
        final String connectorKey = FieldKeys.CONNECTOR.getName();

        for (final FieldVisibilityCriteriaGroup fieldVisibilityCriteriaGroup : childFieldVisibilityCriteriaGroups) {
            final JsonObject ruleGroupJson = Json.object();
            final JsonArray rulesArray = Json.array();
            final Collection<FieldVisibilityCriteria> fieldVisibilityCriteria = fieldVisibilityCriteriaGroup.getFieldVisibilityCriteria();
            final FieldVisibilityCriteria criteria = Objects.isNull(fieldVisibilityCriteria) ? null : fieldVisibilityCriteria.stream().findFirst().orElse(null);
            final Collection<FieldVisibilityCriteriaGroup> fieldVisibilityCriteriaGroups = fieldVisibilityCriteriaGroup.getChildFieldVisibilityCriteriaGroups();

            ruleGroupJson.put(connectorKey, fieldVisibilityCriteriaGroup.getConnector());

            if (Objects.nonNull(criteria)) {
                loadCriteriaIntoJson(ruleGroupJson, criteria, i18NResolutionAdapter);
            }

            if (Objects.nonNull(fieldVisibilityCriteriaGroups)) {
                loadCriteriaGroupsIntoJsonArray(orgId, rulesArray, fieldVisibilityCriteriaGroups, i18NResolutionAdapter);
            }

            if (rulesArray.size() > 0) {
                ruleGroupJson.put(FieldKeys.RULES.getName(), rulesArray);
            }
            ruleGroupsJsonArray.put(ruleGroupJson);
        }
    }
}
