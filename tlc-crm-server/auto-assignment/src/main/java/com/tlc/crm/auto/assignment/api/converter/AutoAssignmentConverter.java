package com.tlc.crm.auto.assignment.api.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.auto.assignment.api.manager.AutoAssignmentManager;
import com.tlc.crm.auto.assignment.api.model.AssignmentModule;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentRule;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentComparator;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteria;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteriaSet;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteriaValue;
import com.tlc.crm.auto.assignment.api.model.CriteriaConnector;
import com.tlc.crm.auto.assignment.api.model.AssignmentRuleUser;
import com.tlc.crm.auto.assignment.cache.service.AutoAssignmentComparatorService;
import com.tlc.crm.auto.assignment.fields.AutoAssignmentAttributes;
import com.tlc.crm.auto.assignment.resolver.AutoAssignmentCriteriaResolver;

import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.fields.models.StoreType;

import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;
import com.tlc.iam.common.models.User;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides different ways to convert the data from / into {@link AutoAssignmentRule}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AutoAssignmentConverter extends EntityConverter<AutoAssignmentRule> {

    private final AutoAssignmentManager autoAssignmentManager;
    private final I18nService i18nAccess;
    private final AutoAssignmentComparatorService assignmentComparatorService;
    private final UserService userService;
    private final AutoAssignmentCriteriaResolver assignmentCriteriaResolver;

    private AutoAssignmentConverter() {
        this.autoAssignmentManager = AutoAssignmentManager.getInstance();
        this.i18nAccess = I18nServiceProvider.get();
        this.assignmentComparatorService = AutoAssignmentComparatorService.getInstance();
        this.userService = UserService.getInstance();
        this.assignmentCriteriaResolver = AutoAssignmentCriteriaResolver.getInstance();
    }

    private static class Instance {
        private static final AutoAssignmentConverter INSTANCE = new AutoAssignmentConverter();
    }

    /**
     * Gets the {@link AutoAssignmentConverter} Instance.
     *
     * @return {@link AutoAssignmentConverter}.
     */
    public static AutoAssignmentConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterConfig Refers the {@link ConverterConfig} data.
     * @param jsonObject      Refers the request json data.
     * @return {@link AutoAssignmentRule} data.
     */
    @Override
    public AutoAssignmentRule convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        return converterConfig.isPartialData() ? constructAutoAssignmentByPartialData(converterConfig, jsonObject)
                : constructAutoAssignment(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param converterConfig    Represents the converter configuration.
     * @param autoAssignmentRule Represents the {@link AutoAssignmentRule} entity.
     * @return The {@link JsonObject} of auto-assignment details.
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final AutoAssignmentRule autoAssignmentRule) {
        final JsonObject jsonObject = Json.object();
        final Long orgId = autoAssignmentRule.orgId();
        final I18nResolver i18nResolver = i18nAccess.getResolver(converterConfig.locale());
        final JsonArray criteriaSetJson = getCriteriaSetJson(orgId, autoAssignmentRule, converterConfig.getI18nPlaceholderResolver());

        jsonObject.put(AutoAssignmentAttributes.ID, autoAssignmentRule.id());
        jsonObject.put(AutoAssignmentAttributes.NAME, i18nResolver.get(autoAssignmentRule.getName()));
        jsonObject.put(AutoAssignmentAttributes.DESCRIPTION, i18nResolver.get(autoAssignmentRule.getDescription()));
        jsonObject.put(AutoAssignmentAttributes.IS_ACTIVE, autoAssignmentRule.isActive());
        jsonObject.put(AutoAssignmentAttributes.TEMPLATE_ID, autoAssignmentRule.getTemplateId());
        jsonObject.put(AutoAssignmentAttributes.MODULE, autoAssignmentRule.getAssignmentModule().getId());
        jsonObject.put(AutoAssignmentAttributes.RULE_TARGET, getAssignmentRule(orgId, autoAssignmentRule.getAssignmentRuleUsers()));

        if (0 < criteriaSetJson.size()) {
            jsonObject.put(AutoAssignmentAttributes.CONDITIONS, criteriaSetJson);
        }

        return jsonObject;
    }

    /**
     * Get the assignment rule for the specified organization using the provided rule assignment users.
     *
     * @param orgId               The organization id.
     * @param assignmentRuleUsers The collection of RuleAssignmentUser objects.
     * @return A JsonObject representing the assignment rule.
     */
    private JsonObject getAssignmentRule(final long orgId, final Collection<AssignmentRuleUser> assignmentRuleUsers) {
        final JsonObject jsonObject = Json.object();
        final JsonArray nonTerritoryUsers = Json.array();
        final JsonArray territoryUsers = Json.array();

        assignmentRuleUsers.stream().filter(ruleAssignmentUser -> Objects.nonNull(ruleAssignmentUser.getTerritory()))
                .findFirst().ifPresent(ruleUser -> {
                    final Territory territory = ruleUser.getTerritory();
                    final JsonObject territoryJson = Json.object();

                    territoryJson.put(AutoAssignmentAttributes.ID, territory.id());
                    territoryJson.put(AutoAssignmentAttributes.NAME, territory.getName());

                    jsonObject.put(AutoAssignmentAttributes.TERRITORY, territoryJson);
                    jsonObject.put(AutoAssignmentAttributes.TERRITORY_USERS, territoryUsers);
                });

        final User.State activeState = User.State.ACTIVE;

        for (final AssignmentRuleUser assignmentRuleUser : assignmentRuleUsers) {
            final JsonObject userJson = Json.object();
            final JsonObject configJson = Json.object();
            final boolean hasTerritory = Objects.nonNull(assignmentRuleUser.getTerritory());
            final CrmUser crmUser = assignmentRuleUser.getCrmUser();

            configJson.put(AutoAssignmentAttributes.IS_ACTIVE, Objects.equals(activeState, crmUser.getState()));

            userJson.put(AutoAssignmentAttributes.ID, crmUser.id());
            userJson.put(AutoAssignmentAttributes.NAME, crmUser.getFullName());
            userJson.put(AutoAssignmentAttributes.CONFIG, configJson);

            if (hasTerritory) {
                territoryUsers.put(userJson);
            } else {
                nonTerritoryUsers.put(userJson);
            }
        }

        if (nonTerritoryUsers.size() > 0) {
            jsonObject.put(AutoAssignmentAttributes.NON_TERRITORY_USERS, nonTerritoryUsers);
        }

        return jsonObject;
    }

    /**
     * Converts the criteria sets into the {@link JsonArray}.
     *
     * @param orgId                 Refers the organization id.
     * @param autoAssignmentRule    Refers the {@link AutoAssignmentRule}.
     * @param i18nResolutionAdapter Refers the {@link I18nResolutionAdapter} for resolve the i18n data.
     * @return The criteria set {@link JsonArray}.
     */
    public JsonArray getCriteriaSetJson(final long orgId, final AutoAssignmentRule autoAssignmentRule, final I18nResolutionAdapter i18nResolutionAdapter) {
        final JsonArray conditions = Json.array();

        for (final AutoAssignmentCriteriaSet assignmentCriteriaSet : autoAssignmentRule.getCriteriaSets()) {
            final JsonObject criteriaSet = Json.object();
            final CriteriaConnector connector = assignmentCriteriaSet.getConnector();
            final JsonArray criteriaArray = Json.array();

            for (final AutoAssignmentCriteria assignmentCriteria : assignmentCriteriaSet.getCriteriaList()) {
                final JsonObject criteriaJson = Json.object();
                final JsonArray criteriaValueArray = Json.array();
                final CriteriaConnector criteriaConnector = assignmentCriteria.getConnector();

                final JsonObject criteriaFieldJson = Json.object();
                final Long fieldId = assignmentCriteria.getFieldId();

                criteriaFieldJson.put(AutoAssignmentAttributes.NAME, i18nResolutionAdapter.get(fieldService.getField(orgId, fieldId).i18nName()));
                criteriaFieldJson.put(AutoAssignmentAttributes.ID, fieldId);

                criteriaJson.put(AutoAssignmentAttributes.ID, assignmentCriteria.getId());
                criteriaJson.put(AutoAssignmentAttributes.FIELD, criteriaFieldJson);
                criteriaJson.put(AutoAssignmentAttributes.COMPARATOR, getComparatorJson(assignmentCriteria.getComparator(), i18nResolutionAdapter));

                if (Objects.nonNull(criteriaConnector)) {
                    criteriaJson.put(AutoAssignmentAttributes.CONNECTOR, criteriaConnector.getId());
                }

                for (final AutoAssignmentCriteriaValue assignmentCriteriaValue : assignmentCriteria.getCriteriaValues()) {

                    if (assignmentCriteria.getComparator().hasValue()) {
                        final StoreType storeType = fieldService.getField(orgId, fieldId).storeType();

                        if (storeType.isOwnerType()) {
                            final long valueEntityId = NumberUtils.parseNumber(assignmentCriteriaValue.getValue()).longValue();

                            assignmentCriteriaValue.setValue(userService.getUser(orgId, valueEntityId).getFullName());
                            assignmentCriteriaValue.setValueEntityId(valueEntityId);
                        } else {
                            assignmentCriteriaResolver.resolveCriteria(orgId, fieldId, assignmentCriteriaValue, i18nResolutionAdapter);
                        }
                    }

                    final JsonObject criteriaValue = Json.object();
                    final Long valueId = assignmentCriteriaValue.getValueEntityId();

                    if (Objects.nonNull(valueId)) {
                        final JsonObject valueJson = Json.object();

                        criteriaValue.put(AutoAssignmentAttributes.ID, assignmentCriteriaValue.getValueEntityId());
                        criteriaValue.put(AutoAssignmentAttributes.NAME, assignmentCriteriaValue.getValue());

                        valueJson.put(AutoAssignmentAttributes.ID, assignmentCriteriaValue.getId());
                        valueJson.put(AutoAssignmentAttributes.VALUE, criteriaValue);

                        criteriaValueArray.put(valueJson);
                    } else {
                        criteriaValue.put(AutoAssignmentAttributes.VALUE, assignmentCriteriaValue.getValue());
                        criteriaValue.put(AutoAssignmentAttributes.ID, assignmentCriteriaValue.getId());

                        criteriaValueArray.put(criteriaValue);
                    }
                }

                if (criteriaValueArray.size() > 0) {
                    criteriaJson.put(AutoAssignmentAttributes.VALUES, criteriaValueArray);
                }
                criteriaArray.put(criteriaJson);
            }

            if (Objects.nonNull(connector)) {
                criteriaSet.put(AutoAssignmentAttributes.CONNECTOR, connector.getId());
            }
            criteriaSet.put(AutoAssignmentAttributes.ID, assignmentCriteriaSet.getId());
            criteriaSet.put(AutoAssignmentAttributes.CRITERIA, criteriaArray);
            conditions.put(criteriaSet);
        }

        return conditions;
    }

    /**
     * Gets the {@link JsonObject} of comparator details.
     *
     * @param assignmentComparator  Refers the {@link AutoAssignmentComparator} details.
     * @param i18nResolutionAdapter Refers the {@link I18nResolutionAdapter} for resolve the i18n data.
     * @return The {@link AutoAssignmentComparator} details json.
     */
    private JsonObject getComparatorJson(final AutoAssignmentComparator assignmentComparator, final I18nResolutionAdapter i18nResolutionAdapter) {
        final JsonObject comparatorJson = Json.object();

        comparatorJson.put(AutoAssignmentAttributes.ID, assignmentComparator.getId());
        comparatorJson.put(AutoAssignmentAttributes.NAME, i18nResolutionAdapter.get(assignmentComparator.getName()));
        comparatorJson.put(AutoAssignmentAttributes.HAS_VALUE, assignmentComparator.hasValue());

        if (Objects.equals(AutoAssignmentAttributes.ANY, assignmentComparator.getAliasName())) {
            comparatorJson.put(AutoAssignmentAttributes.TYPE, AutoAssignmentAttributes.MULTI_SELECT);
        }
        return comparatorJson;
    }

    /**
     * Converts the partial {@link JsonObject} into the {@link AutoAssignmentRule}.
     *
     * @param converterConfig Which refers the {@link ConverterConfig}.
     * @param jsonObject      Which holds the json data.
     * @return {@link AutoAssignmentRule} which contains the auto-assignment details.
     */
    private AutoAssignmentRule constructAutoAssignmentByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final AutoAssignmentRule autoAssignmentRule = autoAssignmentManager.get(converterConfig.orgId(), jsonObject.getLong(AutoAssignmentAttributes.ID));

        setJsonDataToModel(converterConfig, autoAssignmentRule, jsonObject);
        return autoAssignmentRule;
    }

    /**
     * Converts the {@link JsonObject} into the {@link AutoAssignmentRule}.
     *
     * @param converterConfig Which refers the {@link ConverterConfig}.
     * @param jsonObject      Which holds the json data.
     * @return {@link AutoAssignmentRule} which contains the auto-assignment details.
     */
    private AutoAssignmentRule constructAutoAssignment(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final AutoAssignmentRule autoAssignmentRule = new AutoAssignmentRule();

        if (jsonObject.containsKey(AutoAssignmentAttributes.ID)) {
            autoAssignmentRule.setId(jsonObject.getLong(AutoAssignmentAttributes.ID));
        } else {
            autoAssignmentRule.setCreatedBy(userService.getUser(converterConfig.orgId(), converterConfig.ownerId()));
        }

        setJsonDataToModel(converterConfig, autoAssignmentRule, jsonObject);
        return autoAssignmentRule;
    }

    /**
     * Sets the json details into the {@link AutoAssignmentRule}.
     *
     * @param converterConfig    Refers the {@link ConverterConfig} details.
     * @param autoAssignmentRule Refers the {@link AutoAssignmentRule} to which the data to be set from json.
     * @param jsonObject         Refers the {@link JsonObject} of auto-assignment details.
     */
    private void setJsonDataToModel(final ConverterConfig converterConfig, final AutoAssignmentRule autoAssignmentRule, final JsonObject jsonObject) {
        final long orgId = converterConfig.orgId();

        autoAssignmentRule.setOrgId(orgId);
        autoAssignmentRule.setUpdatedBy(userService.getUser(orgId, converterConfig.ownerId()));
        autoAssignmentRule.setName(jsonObject.containsKey(AutoAssignmentAttributes.NAME)
                ? jsonObject.getString(AutoAssignmentAttributes.NAME) : autoAssignmentRule.getName());
        autoAssignmentRule.setDescription(jsonObject.containsKey(AutoAssignmentAttributes.DESCRIPTION)
                ? jsonObject.getString(AutoAssignmentAttributes.DESCRIPTION) : autoAssignmentRule.getDescription());
        autoAssignmentRule.setActive(jsonObject.optBoolean(AutoAssignmentAttributes.IS_ACTIVE, true));
        autoAssignmentRule.setAssignmentModule(AssignmentModule.get(jsonObject.getShort(AutoAssignmentAttributes.MODULE)));

        if (jsonObject.containsKey(AutoAssignmentAttributes.TEMPLATE_ID)) {
            autoAssignmentRule.setTemplateId(jsonObject.getLong(AutoAssignmentAttributes.TEMPLATE_ID));
        } else if (converterConfig.isPartialData()) {
            autoAssignmentRule.setTemplateId(autoAssignmentRule.getTemplateId());
        }

        if (jsonObject.containsKey(AutoAssignmentAttributes.CONDITIONS)) {
            constructCriteriaSets(converterConfig, autoAssignmentRule, jsonObject);
        }

        if (jsonObject.containsKey(AutoAssignmentAttributes.RULE_TARGET)) {
            constructRuleTarget(converterConfig, autoAssignmentRule, jsonObject.getJsonObject(AutoAssignmentAttributes.RULE_TARGET));
        }
    }

    /**
     * Constructs the {@link AutoAssignmentRule} from the json.
     *
     * @param converterConfig    Refers the {@link ConverterConfig} details.
     * @param autoAssignmentRule Refers the {@link AutoAssignmentRule} to which the data to be set from json.
     * @param jsonObject         Refers the {@link JsonObject} of auto-assignment details.
     */
    private void constructRuleTarget(final ConverterConfig converterConfig, final AutoAssignmentRule autoAssignmentRule, final JsonObject jsonObject) {
        final Collection<AssignmentRuleUser> oldAssignmentRuleUsers = autoAssignmentRule.getAssignmentRuleUsers();

        autoAssignmentRule.setAssignmentRuleUsers(null);

        if (jsonObject.containsKey(AutoAssignmentAttributes.TERRITORY_USERS)) {
            final Territory territory = new Territory();

            territory.setOrgId(converterConfig.orgId());
            territory.setId(jsonObject.getLong(AutoAssignmentAttributes.TERRITORY_ID));
            jsonObject.getJsonArray(AutoAssignmentAttributes.TERRITORY_USERS)
                    .forEach(territoryUserId -> {
                        final AssignmentRuleUser assignmentRuleUser = new AssignmentRuleUser();

                        assignmentRuleUser.setUserId((Long) territoryUserId);
                        assignmentRuleUser.setTerritory(territory);
                        autoAssignmentRule.addAssignmentRuleUser(assignmentRuleUser);
                    });
        }

        if (jsonObject.containsKey(AutoAssignmentAttributes.NON_TERRITORY_USERS)) {
            jsonObject.getJsonArray(AutoAssignmentAttributes.NON_TERRITORY_USERS)
                    .forEach(territoryUserId -> {
                        final AssignmentRuleUser assignmentRuleUser = new AssignmentRuleUser();

                        assignmentRuleUser.setUserId((Long) territoryUserId);
                        autoAssignmentRule.addAssignmentRuleUser(assignmentRuleUser);
                    });
        }

        if (converterConfig.isPartialData()) {
            final Set<Long> currentRuleAssignmentUserIds = autoAssignmentRule.getAssignmentRuleUsers().stream()
                    .map(AssignmentRuleUser::getUserId).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());

            oldAssignmentRuleUsers.stream().filter(oldRuleUser -> !currentRuleAssignmentUserIds.contains(oldRuleUser.getUserId()))
                    .forEach(autoAssignmentRule::addAssignmentRuleUser);
        }
    }

    /**
     * Constructs the {@link AutoAssignmentCriteriaSet} from the json.
     *
     * @param converterConfig    Refers the {@link ConverterConfig} details.
     * @param autoAssignmentRule Refers the {@link AutoAssignmentRule} to which the data to be set from json.
     * @param jsonObject         Refers the {@link JsonObject} of auto-assignment details.
     */
    private void constructCriteriaSets(final ConverterConfig converterConfig, final AutoAssignmentRule autoAssignmentRule, final JsonObject jsonObject) {
        final Collection<AutoAssignmentCriteriaSet> oldCriteriaSets = autoAssignmentRule.getCriteriaSets();
        final boolean isPartialData = converterConfig.isPartialData();

        autoAssignmentRule.setCriteriaSets(null);

        if (jsonObject.containsKey(AutoAssignmentAttributes.CONDITIONS)) {
            final JsonArray conditionArray = jsonObject.getJsonArray(AutoAssignmentAttributes.CONDITIONS);

            for (int index = 0; index < conditionArray.size(); index++) {
                final JsonObject criteriaSetJson = conditionArray.optJsonObject(index);
                final AutoAssignmentCriteriaSet autoAssignmentCriteriaSet = new AutoAssignmentCriteriaSet();

                if (criteriaSetJson.containsKey(AutoAssignmentAttributes.ID)) {
                    autoAssignmentCriteriaSet.setId(criteriaSetJson.getLong(AutoAssignmentAttributes.ID));
                }
                final AutoAssignmentCriteriaSet oldCriteriaSet = isPartialData ? oldCriteriaSets.stream()
                        .filter(oldSet -> Objects.nonNull(oldSet.getId()) && Objects.equals(autoAssignmentCriteriaSet.getId(), oldSet.getId()))
                        .findAny().orElseGet(() -> null) : null;

                if (criteriaSetJson.containsKey(AutoAssignmentAttributes.CONNECTOR)) {
                    autoAssignmentCriteriaSet.setConnector(CriteriaConnector.get(criteriaSetJson.getShort(AutoAssignmentAttributes.CONNECTOR)));
                }

                if (criteriaSetJson.containsKey(AutoAssignmentAttributes.CRITERIA)) {
                    setCriteria(converterConfig, autoAssignmentRule, autoAssignmentCriteriaSet, oldCriteriaSet, criteriaSetJson);
                }

                autoAssignmentRule.addCriteriaSet(autoAssignmentCriteriaSet);
            }

            if (isPartialData) {
                final Set<Long> currentCriteriaSetIds = autoAssignmentRule.getCriteriaSets().stream()
                        .map(AutoAssignmentCriteriaSet::getId).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());

                oldCriteriaSets.stream().filter(oldCriteria -> !currentCriteriaSetIds.contains(oldCriteria.getId()))
                        .forEach(autoAssignmentRule::addCriteriaSet);
            }
        } else if (isPartialData) {
            autoAssignmentRule.setCriteriaSets(oldCriteriaSets);
        }
    }

    /**
     * Sets the {@link AutoAssignmentCriteria} details into the auto-assignment from the json.
     *
     * @param converterConfig           Refers the {@link ConverterConfig} details.
     * @param autoAssignmentRule        Refers the {@link AutoAssignmentRule} to which the data to be set from json.
     * @param autoAssignmentCriteriaSet Refers the {@link AutoAssignmentCriteriaSet} details.
     * @param oldCriteriaSet            Refers the old {@link AutoAssignmentCriteriaSet} details.
     * @param jsonObject                Refers the {@link JsonObject} of auto-assignment details.
     */
    private void setCriteria(final ConverterConfig converterConfig, final AutoAssignmentRule autoAssignmentRule,
                             final AutoAssignmentCriteriaSet autoAssignmentCriteriaSet,
                             final AutoAssignmentCriteriaSet oldCriteriaSet, final JsonObject jsonObject) {
        final boolean isOldCriteriaSetExist = converterConfig.isPartialData() && Objects.nonNull(oldCriteriaSet);

        if (jsonObject.containsKey(AutoAssignmentAttributes.CRITERIA)) {
            final JsonArray jsonArray = jsonObject.getJsonArray(AutoAssignmentAttributes.CRITERIA);

            for (int index = 0; index < jsonArray.size(); index++) {
                final JsonObject criteriaJson = jsonArray.optJsonObject(index);
                final AutoAssignmentCriteria autoAssignmentCriteria = new AutoAssignmentCriteria();

                if (criteriaJson.containsKey(AutoAssignmentAttributes.ID)) {
                    autoAssignmentCriteria.setId(criteriaJson.getLong(AutoAssignmentAttributes.ID));
                }

                if (criteriaJson.containsKey(AutoAssignmentAttributes.CONNECTOR)) {
                    autoAssignmentCriteria.setConnector(CriteriaConnector.get(criteriaJson.getShort(AutoAssignmentAttributes.CONNECTOR)));
                }
                autoAssignmentCriteria.setComparator(assignmentComparatorService.get(criteriaJson.getLong(AutoAssignmentAttributes.COMPARATOR)));
                autoAssignmentCriteria.setFieldId(criteriaJson.getLong(AutoAssignmentAttributes.FIELD_ID));

                if (criteriaJson.containsKey(AutoAssignmentAttributes.VALUES)) {
                    setCriteriaValue(autoAssignmentCriteria, criteriaJson.getJsonArray(AutoAssignmentAttributes.VALUES));
                }
                autoAssignmentCriteriaSet.addCriteria(autoAssignmentCriteria);
            }

            if (isOldCriteriaSetExist) {
                final Set<Long> currentCriteriaIds = autoAssignmentCriteriaSet.getCriteriaList().stream()
                        .map(AutoAssignmentCriteria::getId).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());

                oldCriteriaSet.getCriteriaList().stream().filter(oldCriteria -> !currentCriteriaIds.contains(oldCriteria.getId()))
                        .forEach(autoAssignmentCriteriaSet::addCriteria);
            }
        } else if (isOldCriteriaSetExist) {
            autoAssignmentCriteriaSet.setCriteriaList(oldCriteriaSet.getCriteriaList());
        }
    }

    /**
     * Sets the {@link AutoAssignmentCriteriaValue} into the criteria details.
     *
     * @param autoAssignmentCriteria Refers the {@link AutoAssignmentCriteria} details.
     * @param jsonArray              Refers the criteria value {@link JsonArray}.
     */
    private void setCriteriaValue(final AutoAssignmentCriteria autoAssignmentCriteria, final JsonArray jsonArray) {

        for (int index = 0; index < jsonArray.size(); index++) {
            final AutoAssignmentCriteriaValue assignmentCriteriaValue = new AutoAssignmentCriteriaValue();
            final JsonObject jsonObject = jsonArray.getJsonObject(index);

            if (jsonObject.containsKey(AutoAssignmentAttributes.ID)) {
                assignmentCriteriaValue.setId(jsonObject.getLong(AutoAssignmentAttributes.ID));
            }

            if (jsonObject.containsKey(AutoAssignmentAttributes.VALUE)) {
                final Object value = jsonObject.get(AutoAssignmentAttributes.VALUE);

                if (value instanceof final Map<?, ?> valueJsonMap) {
                    final JsonObject valueJson = Json.object((Map<String, ?>) valueJsonMap);

                    if (valueJson.containsKey(AutoAssignmentAttributes.ID)) {
                        assignmentCriteriaValue.setValueEntityId(valueJson.getLong(AutoAssignmentAttributes.ID));
                    }

                    if (valueJson.containsKey(AutoAssignmentAttributes.NAME)) {
                        assignmentCriteriaValue.setValue(valueJson.get(AutoAssignmentAttributes.NAME));
                    }
                } else {
                    assignmentCriteriaValue.setValue(value);
                }
            }
            autoAssignmentCriteria.addCriteriaValue(assignmentCriteriaValue);
        }
    }
}
