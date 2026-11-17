package com.tlc.crm.workflow.v2.resolver;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.product.manager.ProductManager;

import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.features.api.activity.model.Availability;
import com.tlc.crm.entity.features.api.activity.model.Priority;
import com.tlc.crm.entity.features.api.activity.model.Reminder;
import com.tlc.crm.entity.features.api.activity.model.Status;
import com.tlc.crm.entity.features.api.tag.manager.TagManager;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.marketinglist.manager.MarketingListManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.DropdownType;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.service.OptionService;

import com.tlc.crm.fields.models.StoreType;

import com.tlc.crm.workflow.api.model.FieldGroup;
import com.tlc.crm.workflow.api.model.StepOption;
import com.tlc.crm.workflow.api.model.TriggerActionField;
import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.crm.workflow.resolver.ActionResolver;
import com.tlc.crm.workflow.resolver.internal.TemplateCriteriaResolver;
import com.tlc.crm.workflow.resolver.internal.WorkflowActionResolver;
import com.tlc.crm.workflow.v2.api.model.ActionConfiguration;

import com.tlc.i18n.I18nResolutionAdapter;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Copied from the {@link WorkflowActionResolver}
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public class WorkflowActionResolverV2 extends ActionResolver {

    private final OptionService optionService;
    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final TagManager tagManager;
    private final LifeCycleStageManager lifeCycleStageManager;
    private final LifeCycleStatusManager lifeCycleStatusManager;
    private final PipelineManager pipelineManager;
    private final PipelineStageManager pipelineStageManager;
    private final ActivityTypeManager activityTypeManager;
    private final DealManager dealManager;
    private final ProductManager productManager;
    private final UserService crmUserService;
    private final MarketingListManager marketingListManager;

    private WorkflowActionResolverV2() {
        optionService = OptionService.getInstance();
        contactManager = ContactManager.getInstance();
        companyManager = CompanyManager.getInstance();
        tagManager = TagManager.getInstance();
        lifeCycleStageManager = LifeCycleStageManager.getInstance();
        lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
        pipelineManager = PipelineManager.getInstance();
        pipelineStageManager = PipelineStageManager.getInstance();
        activityTypeManager = ActivityTypeManager.getInstance();
        dealManager = DealManager.getInstance();
        productManager = ProductManager.getInstance();
        crmUserService = UserService.getInstance();
        marketingListManager = MarketingListManager.getInstance();
    }

    private static class Instance {
        private static final WorkflowActionResolverV2 INSTANCE = new WorkflowActionResolverV2();
    }

    /**
     * Gets the {@link TemplateCriteriaResolver} instance.
     *
     * @return {@link TemplateCriteriaResolver}.
     */
    public static WorkflowActionResolverV2 getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                  Represents the organization id
     * @param isCommonActionSource   A boolean flag indicating whether the action source are common or not
     * @param i18NResolutionAdapter  Represents the resolver of the I18N strings
     * @param triggerActionFieldJson The {@link JsonObject} where the values will be loaded
     * @param fieldMappingArray      The {@link JsonArray} containing the mapping of fields
     */
    protected void fillActionFieldValuesToJson(final Long orgId, final boolean isCommonActionSource, final I18nResolutionAdapter i18NResolutionAdapter,
                                               final JsonObject triggerActionFieldJson, final JsonArray fieldMappingArray) {
        final long fieldId = triggerActionFieldJson.getLong(WorkflowAttributes.FIELD_ID);
        final String aliasName = isCommonActionSource ? fieldService.getField(orgId, fieldId).aliasName() : workflowFieldService.get(fieldId).getAliasName();
        final Consumer<JsonObject> valueConsumer = getValueConsumer(i18NResolutionAdapter);
        final int fieldMappingArraySize = fieldMappingArray.size();

        outerLoop:
        for (int fieldMappingArrayIndex = 0; fieldMappingArrayIndex < fieldMappingArraySize; fieldMappingArrayIndex++) {
            final JsonArray fieldsArray = fieldMappingArray.getJsonArray(fieldMappingArrayIndex);
            final int fieldsArraySize = fieldsArray.size();

            if (fieldsArraySize > 0) {

                for (int fieldsArrayIndex = 0; fieldsArrayIndex < fieldsArraySize; fieldsArrayIndex++) {
                    final JsonObject fieldJson = fieldsArray.getJsonObject(fieldsArrayIndex);
                    final long compareFieldId = fieldJson.getLong(WorkflowAttributes.FIELD_ID);
                    final String aliasNameToCompare = isCommonActionSource ? fieldService.getField(orgId, compareFieldId).aliasName()
                            : workflowFieldService.get(compareFieldId).getAliasName();

                    if (Objects.equals(aliasName, aliasNameToCompare)) {

                        if (fieldJson.containsKey(WorkflowAttributes.VALUES)) {
                            final JsonArray valuesArray = fieldJson.getJsonArray(WorkflowAttributes.VALUES);

                            for (int valuesArrayIndex = 0; valuesArrayIndex < valuesArray.size(); valuesArrayIndex++) {
                                final JsonObject valueJson = valuesArray.getJsonObject(valuesArrayIndex);

                                if (valueJson.containsKey(WorkflowAttributes.DEPENDS_ON)) {
                                    valueConsumer.accept(valueJson);
                                } else if (isCommonActionSource) {
                                    final FieldInfo fieldInfo = fieldService.getField(orgId, compareFieldId);
                                    final StoreType storeType = fieldInfo.storeType();

                                    if (StoreType.isSelectorType(storeType)) {
                                        resolveDropdownFieldValue(orgId, compareFieldId, fieldInfo, valueJson, i18NResolutionAdapter);
                                    } else if (storeType.isOwnerType()) {
                                        final long userId = valueJson.getLong(WorkflowAttributes.VALUE);

                                        valueJson.put(WorkflowAttributes.VALUE, Json.object()
                                                .put(WorkflowAttributes.ID, userId)
                                                .put(WorkflowAttributes.NAME, crmUserService.getUser(orgId, userId).getFullName()));
                                    } else if (storeType.isEntityType() || StoreType.TAGS == storeType || StoreType.MARKETING_LIST == storeType) {
                                        resolveEntityFieldValue(orgId, valueJson, storeType);
                                    } else if (Objects.equals(WorkflowAttributes.ID, fieldInfo.aliasName())) {
                                        resolveIdFieldValue(orgId, valueJson, fieldInfo.fieldSource().getName().toLowerCase());
                                    }
                                } else {
                                    loadUncommonSourceFieldValue(orgId, workflowFieldService.get(compareFieldId), valueJson, i18NResolutionAdapter);
                                }
                            }
                            triggerActionFieldJson.put(WorkflowAttributes.VALUES, valuesArray);
                        }

                        if (fieldJson.containsKey(WorkflowAttributes.IS_ENABLED)) {
                            triggerActionFieldJson.put(WorkflowAttributes.IS_ENABLED, fieldJson.getBoolean(WorkflowAttributes.IS_ENABLED));
                        }
                        break outerLoop;
                    }
                }
            } else {
                fieldMappingArray.remove(fieldsArray);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                 Represents the organization id
     * @param i18NResolutionAdapter Represents the resolver of the I18N strings
     * @param emailActionFieldJson  The {@link JsonObject} where the values will be loaded
     * @param fieldMappingArray     The {@link JsonArray} containing the mapping of fields
     */
    @Override
    protected void fillEmailValuesToJson(final Long orgId, final I18nResolutionAdapter i18NResolutionAdapter, final JsonObject emailActionFieldJson, final JsonArray fieldMappingArray) {
        final String aliasName = workflowFieldService.get(emailActionFieldJson.getLong(WorkflowAttributes.FIELD_ID)).getAliasName();
        final Consumer<JsonObject> valueConsumer = getValueConsumer(i18NResolutionAdapter);
        final int fieldMappingArraySize = fieldMappingArray.size();

        for (int fieldMappingArrayIndex = 0; fieldMappingArrayIndex < fieldMappingArraySize; fieldMappingArrayIndex++) {
            final JsonObject fieldJson = fieldMappingArray.getJsonObject(fieldMappingArrayIndex);

            if (fieldJson.containsKey(WorkflowAttributes.VALUES)) {

                if (aliasName.equals(workflowFieldService.get(fieldJson.getLong(WorkflowAttributes.FIELD_ID)).getAliasName())) {
                    final JsonArray valuesArray = fieldJson.getJsonArray(WorkflowAttributes.VALUES);
                    final int valuesArraySize = valuesArray.size();

                    for (int valuesArrayIndex = 0; valuesArrayIndex < valuesArraySize; valuesArrayIndex++) {
                        final JsonObject valueJson = valuesArray.getJsonObject(valuesArrayIndex);

                        if (valueJson.containsKey(WorkflowAttributes.DEPENDS_ON)) {
                            valueConsumer.accept(valueJson);
                        }
                    }
                    emailActionFieldJson.put(WorkflowAttributes.VALUES, valuesArray);
                    fieldMappingArray.remove(fieldJson);
                    break;
                }
            }
        }
    }

    /**
     * <p>
     * Retrieves a {@link Consumer} that processes a {@link JsonObject} by replacing its value
     * </p>
     *
     * @param i18NResolutionAdapter Represents the resolver of the I18N strings
     * @return A {@link Consumer} that handles {@link JsonObject} values.
     */
    private Consumer<JsonObject> getValueConsumer(final I18nResolutionAdapter i18NResolutionAdapter) {
        return (valueJson) -> {
            if (valueJson.containsKey(WorkflowAttributes.DEPENDS_ON)) {
                final long stepOptionId = valueJson.getLong(WorkflowAttributes.VALUE);
                final StepOption stepOption = stepOptionService.getStepOption(stepOptionId);

                if (Objects.nonNull(stepOption)) {
                    valueJson.put(WorkflowAttributes.VALUE, convertDataToJson(stepOptionId,
                            i18NResolutionAdapter.get(stepOption.name())));
                }
            }
        };
    }

    /**
     * <p>
     * Resolves the values for the dropdown and multiselect fields and load it in the provided value json
     * </p>
     *
     * @param orgId                 Represents the organization id
     * @param fieldId               Represents the field id
     * @param fieldInfo             Represents the {@link FieldInfo}
     * @param valueJson             Represents the value json data
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}
     */
    private void resolveDropdownFieldValue(final Long orgId, final Long fieldId, final FieldInfo fieldInfo,
                                           final JsonObject valueJson, final I18nResolutionAdapter i18NResolutionAdapter) {
        final Long optionId = valueJson.getLong(WorkflowAttributes.VALUE);
        final JsonObject optionJson = Json.object();

        optionJson.put(WorkflowAttributes.ID, optionId);

        if (DropdownType.HIDDEN_OPTION == fieldInfo.presetConfig().dropdownType()) {
            final String aliasName = fieldInfo.aliasName();

            switch (aliasName) {
                case "lifeCycleStage" ->
                        optionJson.put(WorkflowAttributes.NAME, lifeCycleStageManager.get(orgId, optionId).getName());
                case "lifeCycleStatus" ->
                        optionJson.put(WorkflowAttributes.NAME, lifeCycleStatusManager.get(orgId, optionId).getName());
                case "pipeline" ->
                        optionJson.put(WorkflowAttributes.NAME, pipelineManager.get(orgId, optionId).getName());
                case "pipelineStage" ->
                        optionJson.put(WorkflowAttributes.NAME, pipelineStageManager.getPipelineStageName(orgId, optionId));
            }
        } else {
            final Option option = optionService.get(orgId, fieldId, optionId);

            if (Objects.nonNull(option)) {
                optionJson.put(WorkflowAttributes.NAME, i18NResolutionAdapter.get(option.getOption()));
            }
        }

        if (optionJson.size() > 0) {
            valueJson.put(WorkflowAttributes.VALUE, optionJson);
        }
    }

    /**
     * <p>
     * Resolves the values of the entity field and load it in the provided value json
     * </p>
     *
     * @param orgId             Represents the organization id
     * @param existingValueJson Represents the value json data
     * @param storeType         Represents the {@link StoreType}
     */
    private void resolveEntityFieldValue(final long orgId, final JsonObject existingValueJson, final StoreType storeType) {
        final long entityId = existingValueJson.getLong(WorkflowAttributes.VALUE);

        switch (storeType) {
            case CONTACT, CONTACT_ONBOARD, CONTACTS_ONBOARD ->
                    existingValueJson.put(WorkflowAttributes.VALUE, convertDataToJson(entityId, contactManager.getContactFullName(orgId, entityId)));
            case COMPANY, COMPANY_ONBOARD ->
                    existingValueJson.put(WorkflowAttributes.VALUE, convertDataToJson(entityId, companyManager.getCompanyName(orgId, entityId)));
            case TAGS ->
                    existingValueJson.put(WorkflowAttributes.VALUE, convertDataToJson(entityId, tagManager.partialGet(orgId, entityId).getName()));
            case MARKETING_LIST ->
                    existingValueJson.put(WorkflowAttributes.VALUE, convertDataToJson(entityId, marketingListManager.partialGet(orgId, entityId).getName()));
            default -> {
            }
        }
    }

    /**
     * <p>
     * Resolves the value of the id field and load it in the provided value json
     * </p>
     *
     * @param orgId             Represents the organization id
     * @param existingValueJson Represents the value json data
     * @param sourceName        Represents the source name
     */
    private void resolveIdFieldValue(final long orgId, final JsonObject existingValueJson, final String sourceName) {
        final long entityId = existingValueJson.getLong(WorkflowAttributes.VALUE);

        switch (sourceName) {
            case WorkflowAttributes.CONTACT ->
                    existingValueJson.put(WorkflowAttributes.VALUE, convertDataToJson(entityId, contactManager.getContactFullName(orgId, entityId)));
            case WorkflowAttributes.COMPANY ->
                    existingValueJson.put(WorkflowAttributes.VALUE, convertDataToJson(entityId, companyManager.getCompanyName(orgId, entityId)));
            case WorkflowAttributes.DEAL ->
                    existingValueJson.put(WorkflowAttributes.VALUE, convertDataToJson(entityId, dealManager.partialGet(orgId, entityId).getTitle()));
        }
    }

    /**
     * <p>
     * Resolves the uncommon source field value and load it in the provided value json
     * </p>
     *
     * @param orgId                 Represents the organization id
     * @param field                 Represents the workflow field
     * @param existingValueJson     Represents the value json data
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}
     */
    private void loadUncommonSourceFieldValue(final Long orgId, final WorkflowField field, final JsonObject existingValueJson, final I18nResolutionAdapter i18NResolutionAdapter) {
        final String aliasName = field.getAliasName();

        switch (aliasName) {
            case "activityType" -> {
                final long activityTypeId = existingValueJson.getLong(WorkflowAttributes.VALUE);

                existingValueJson.put(WorkflowAttributes.VALUE, convertDataToJson(activityTypeId, activityTypeManager.get(orgId, activityTypeId).getName()));
            }
            case "availability" -> {
                final long availabilityId = existingValueJson.getInt(WorkflowAttributes.VALUE);

                existingValueJson.put(WorkflowAttributes.VALUE, convertDataToJson(availabilityId,
                        i18NResolutionAdapter.get(Availability.get(NumberUtils.parseNumber(availabilityId).intValue()).getDisplayName())));
            }
            case "assignee", "collaborators" -> {
                final long userId = existingValueJson.getLong(WorkflowAttributes.VALUE);

                existingValueJson.put(WorkflowAttributes.VALUE, Json.object()
                        .put(WorkflowAttributes.ID, userId)
                        .put(WorkflowAttributes.NAME, crmUserService.getUser(orgId, userId).getFullName()));
            }
            case "priority" -> {
                final long priorityId = existingValueJson.getInt(WorkflowAttributes.VALUE);

                existingValueJson.put(WorkflowAttributes.VALUE, convertDataToJson(priorityId,
                        i18NResolutionAdapter.get(Priority.get(NumberUtils.parseNumber(priorityId).intValue()).getDisplayName())));
            }
            case "contact" -> {
                final long contactId = existingValueJson.getInt(WorkflowAttributes.VALUE);

                existingValueJson.put(WorkflowAttributes.VALUE, convertDataToJson(contactId, contactManager.getContactFullName(orgId, contactId)));
            }
            case "deal" -> {
                final long dealId = existingValueJson.getInt(WorkflowAttributes.VALUE);

                existingValueJson.put(WorkflowAttributes.VALUE, convertDataToJson(dealId, dealManager.partialGet(orgId, dealId).getTitle()));
            }
            case "company" -> {
                final long companyId = existingValueJson.getInt(WorkflowAttributes.VALUE);

                existingValueJson.put(WorkflowAttributes.VALUE, convertDataToJson(companyId, companyManager.getCompanyName(orgId, companyId)));
            }
            case "reminder" -> {
                final long reminderId = existingValueJson.getInt(WorkflowAttributes.VALUE);

                existingValueJson.put(WorkflowAttributes.VALUE, convertDataToJson(reminderId,
                        i18NResolutionAdapter.get(Reminder.get(NumberUtils.parseNumber(reminderId).intValue()).getDisplayName())));
            }
            case "status" -> {
                final long statusId = existingValueJson.getInt(WorkflowAttributes.VALUE);

                existingValueJson.put(WorkflowAttributes.VALUE, convertDataToJson(statusId,
                        i18NResolutionAdapter.get(Status.get(NumberUtils.parseNumber(statusId).intValue()).getDisplayName())));
            }
            case "product" -> {
                final long productId = existingValueJson.getLong(WorkflowAttributes.VALUE);

                existingValueJson.put(WorkflowAttributes.VALUE, convertDataToJson(productId, productManager.getProductName(orgId, productId)));
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                 Represents the organization id.
     * @param actionSourceId        Represents the action source id.
     * @param i18NResolutionAdapter Refers the {@link I18nResolutionAdapter} for resolve the i18n data.
     * @param actionFieldsArray     Represents the action fields {@link JsonArray}
     * @param fieldMappingArray     Refers the {@link JsonArray} fieldMapping data.
     */
    public void loadPersistentFields(final long orgId, final long actionSourceId, final I18nResolutionAdapter i18NResolutionAdapter,
                                     final JsonArray actionFieldsArray, final JsonArray fieldMappingArray) {
        final Collection<FieldInfo> persistentFieldsBySource = fieldService.getPersistentFieldsBySource(orgId,
                Source.get(triggerActionSourceService.getAliasName(actionSourceId)).getName());
        final Collection<FieldInfo> customFieldInfoList = Objects.isNull(persistentFieldsBySource) ? Collections.emptyList() : persistentFieldsBySource.stream()
                .filter(fieldInfo -> fieldInfo.isActive() && !isRestrictedStoreType(fieldInfo.storeType()))
                .sorted(Comparator.comparingInt(fieldInfo -> fieldInfo.presetConfig().order())).toList();

        if (!customFieldInfoList.isEmpty()) {

            for (final FieldInfo customFieldInfo : customFieldInfoList) {
                final JsonArray customFieldsArray = Json.array();
                final JsonObject customFieldJson = Json.object();

                if (Objects.nonNull(fieldMappingArray)) {
                    final int fieldMappingArraySize = fieldMappingArray.size();

                    for (int fieldMappingArrayIndex = 0; fieldMappingArrayIndex < fieldMappingArraySize; fieldMappingArrayIndex++) {
                        final JsonObject fieldJson = fieldMappingArray.getJsonArray(fieldMappingArrayIndex).getJsonObject(0);

                        if (Objects.equals(customFieldInfo.id(), NumberUtils.parseNumber(fieldJson.opt(WorkflowAttributes.FIELD_ID)).longValue())) {

                            if (fieldJson.containsKey(WorkflowAttributes.VALUES)) {
                                loadCustomFieldValues(orgId, customFieldInfo, customFieldJson, fieldJson.getJsonArray(WorkflowAttributes.VALUES));
                            }

                            if (fieldJson.containsKey(WorkflowAttributes.IS_ENABLED)) {
                                customFieldJson.put(WorkflowAttributes.IS_ENABLED, fieldJson.getBoolean(WorkflowAttributes.IS_ENABLED));
                            }
                            break;
                        }
                    }
                }
                loadPersistentFieldIntoJson(customFieldJson, i18NResolutionAdapter, customFieldInfo);

                customFieldsArray.put(customFieldJson);
                actionFieldsArray.put(customFieldsArray);
            }
        }
    }

    /**
     * <p>
     * Loads the values of the custom fields to the provided field json
     * </p>
     *
     * @param orgId              Represents the organization id
     * @param customField        Represents the custom field
     * @param customFieldJson    Represents the field json data of the custom field
     * @param existingValueArray Represents the value array in the field mapping json data
     */
    private void loadCustomFieldValues(final Long orgId, final FieldInfo customField, final JsonObject customFieldJson, final JsonArray existingValueArray) {
        final int valueArraySize = existingValueArray.size();

        if (valueArraySize > 0) {
            final StoreType storeType = customField.storeType();

            switch (storeType) {
                case DROPDOWN -> {
                    final JsonObject valueJson = existingValueArray.getJsonObject(0);
                    final long optionId = valueJson.getLong(WorkflowAttributes.VALUE);
                    final Option option = optionService.get(orgId, customField.id(), optionId);
                    final JsonArray valueArray = Json.array();

                    valueJson.put(WorkflowAttributes.VALUE, convertDataToJson(optionId, option.getOption()));
                    valueArray.put(valueJson);

                    customFieldJson.put(WorkflowAttributes.VALUES, valueArray);
                }
                case MULTI_SELECT -> {
                    final JsonArray valueArray = Json.array();
                    final Long fieldId = customField.id();

                    for (int index = 0; index < valueArraySize; index++) {
                        final JsonObject valueJson = existingValueArray.getJsonObject(index);
                        final long optionId = valueJson.getLong(WorkflowAttributes.VALUE);
                        final Option option = optionService.get(orgId, fieldId, optionId);

                        valueJson.put(WorkflowAttributes.VALUE, convertDataToJson(optionId, option.getOption()));
                        valueArray.put(valueJson);
                    }
                    customFieldJson.put(WorkflowAttributes.VALUES, valueArray);
                }
                case CONTACT -> {
                    final JsonObject existingValueJson = existingValueArray.getJsonObject(0);
                    final long contactId = existingValueJson.getLong(WorkflowAttributes.VALUE);
                    final JsonArray valueArray = Json.array();

                    existingValueJson.put(WorkflowAttributes.VALUE, convertDataToJson(contactId, contactManager.getContactFullName(orgId, contactId)));
                    valueArray.put(existingValueJson);

                    customFieldJson.put(WorkflowAttributes.VALUES, valueArray);
                }
                case OWNER -> {
                    final JsonObject existingValueJson = existingValueArray.getJsonObject(0);
                    final long userId = existingValueJson.getLong(WorkflowAttributes.VALUE);
                    final JsonArray valueArray = Json.array();

                    existingValueJson.put(WorkflowAttributes.VALUE, Json.object()
                            .put(WorkflowAttributes.ID, userId)
                            .put(WorkflowAttributes.NAME, crmUserService.getUser(orgId, userId).getFullName()));

                    valueArray.put(existingValueJson);
                    customFieldJson.put(WorkflowAttributes.VALUES, valueArray);
                }
                case COMPANY -> {
                    final JsonObject existingValueJson = existingValueArray.getJsonObject(0);
                    final long companyId = existingValueJson.getLong(WorkflowAttributes.VALUE);
                    final JsonArray valueArray = Json.array();

                    existingValueJson.put(WorkflowAttributes.VALUE, convertDataToJson(companyId, companyManager.getCompanyName(orgId, companyId)));
                    valueArray.put(existingValueJson);

                    customFieldJson.put(WorkflowAttributes.VALUES, valueArray);
                }
                default -> customFieldJson.put(WorkflowAttributes.VALUES, existingValueArray);
            }
        }
    }

    /**
     * <p>
     * Converts the provided data into json
     * </p>
     *
     * @param id   Represents the id of the value
     * @param name Represents the name of the value
     * @return the json object which contains the value
     */
    private JsonObject convertDataToJson(final long id, final String name) {
        final JsonObject valueJson = Json.object();

        valueJson.put(WorkflowAttributes.ID, id);
        valueJson.put(WorkflowAttributes.NAME, name);

        return valueJson;
    }

    /**
     * <p>
     * Resolves the {@code ActionConfiguration} details into the {@link JsonObject}
     * </p>
     *
     * @param orgId                 Represents the organization id
     * @param triggerSourceId       Represents the trigger source id
     * @param actionConfiguration   It holds the {@link ActionConfiguration} details
     * @param actionJson            Represents the action {@link JsonObject}
     * @param i18NResolutionAdapter Refers the {@link I18nResolutionAdapter} for resolve the i18n data
     */
    public void resolveActionJson(final long orgId, final short triggerSourceId, final JsonObject actionJson,
                                  final ActionConfiguration actionConfiguration, final I18nResolutionAdapter i18NResolutionAdapter) {
        final long triggerActionSourceId = actionConfiguration.getTriggerActionSource().id();
        final JsonArray fieldMappingArray = actionConfiguration.getFieldMappingAsJson();
        final Collection<TriggerActionField> triggerActionFields = triggerActionFieldService.get(workflowActionManager
                .getTriggerActionId(triggerSourceId, triggerActionSourceId, actionConfiguration.getTriggerActionEvent().id()));
        final Map<Integer, List<TriggerActionField>> groupedActionFields = triggerActionFieldService.groupActionFieldsByConfig(triggerActionFields);
        final String actionSourceName = triggerActionSourceService.getAliasName(triggerActionSourceId);

        if (Objects.equals(WorkflowAttributes.EMAIL, actionSourceName)) {
            resolveEmailActionJson(orgId, groupedActionFields, fieldMappingArray, actionJson, i18NResolutionAdapter);
        } else if (Objects.equals(WorkflowAttributes.WHATSAPP, actionSourceName)) {
            resolveWhatsappActionJson(orgId, groupedActionFields, fieldMappingArray, actionJson, i18NResolutionAdapter);
        } else {
            resolveSourceActionJson(orgId, groupedActionFields, actionConfiguration, actionJson, i18NResolutionAdapter);
        }
    }

    /**
     * <p>
     * Resolves the source related {@code ActionConfiguration} details into the {@link JsonObject}
     * </p>
     *
     * @param orgId                 Represents the organization id
     * @param groupedActionFields   It holds the grouped {@link TriggerActionField}
     * @param actionJson            Represents the action {@link JsonObject}
     * @param i18NResolutionAdapter Refers the {@link I18nResolutionAdapter} for resolve the i18n data
     */
    private void resolveSourceActionJson(final Long orgId, final Map<Integer, List<TriggerActionField>> groupedActionFields,
                                         final ActionConfiguration actionConfiguration, final JsonObject actionJson, final I18nResolutionAdapter i18NResolutionAdapter) {
        final long triggerActionSourceId = actionConfiguration.getTriggerActionSource().id();
        final long triggerActionEventId = actionConfiguration.getTriggerActionEvent().id();

        final boolean isCommonActionSource = triggerActionSourceService.get(triggerActionSourceId).isCommonActionSource();
        final boolean isCommonActionEvent = triggerActionEventService.get(triggerActionEventId).isCommonActionEvent();

        final JsonArray fieldMappingArray = actionConfiguration.getFieldMappingAsJson();
        final JsonArray actionFieldsArray = Json.array();

        for (final Map.Entry<Integer, List<TriggerActionField>> triggerActionFieldEntry : groupedActionFields.entrySet()) {
            final JsonArray fieldsArray = Json.array();

            for (final TriggerActionField triggerActionField : triggerActionFieldEntry.getValue()) {
                final JsonObject actionFieldJson = Json.object();
                final boolean isEntitySource = isCommonActionSource && FieldGroup.SOURCE_RELATED_FIELD == triggerActionField.getFieldGroup();

                loadActionFieldToJson(orgId, actionFieldJson, i18NResolutionAdapter, isEntitySource
                        ? fieldService.getField(orgId, triggerActionField.getFieldId()) : null, triggerActionField);
                fillActionFieldValuesToJson(orgId, isEntitySource, i18NResolutionAdapter, actionFieldJson, fieldMappingArray);

                fieldsArray.put(actionFieldJson);
            }
            actionFieldsArray.put(fieldsArray);
        }

        if (isCommonActionSource && isCommonActionEvent) {
            loadPersistentFields(orgId, triggerActionSourceId, i18NResolutionAdapter, actionFieldsArray, fieldMappingArray);
        }
        actionJson.put(WorkflowAttributes.FIELD_MAPPING, actionFieldsArray);
    }

    /**
     * <p>
     * Resolves the email related {@code ActionConfiguration} details into the {@link JsonObject}
     * </p>
     *
     * @param orgId                 Represents the organization id
     * @param groupedActionFields   Represents the grouped {@link TriggerActionField}
     * @param fieldMappingArray     The {@link JsonArray} containing the mapping of fields
     * @param actionJson            Represents the action {@link JsonObject}
     * @param i18NResolutionAdapter Refers the {@link I18nResolutionAdapter} for resolve the i18n data
     */
    private void resolveWhatsappActionJson(final Long orgId, final Map<Integer, List<TriggerActionField>> groupedActionFields,
                                           final JsonArray fieldMappingArray, final JsonObject actionJson, final I18nResolutionAdapter i18NResolutionAdapter) {
        final JsonObject fieldMappingJson = Json.object();

        for (final Map.Entry<Integer, List<TriggerActionField>> triggerActionFieldEntry : groupedActionFields.entrySet()) {

            for (final TriggerActionField triggerActionField : triggerActionFieldEntry.getValue()) {
                final JsonObject actionFieldJson = Json.object();

                loadActionFieldToJson(orgId, actionFieldJson, i18NResolutionAdapter, null, triggerActionField);
                fillWhatsappValuesToJson(i18NResolutionAdapter, actionFieldJson, fieldMappingArray);

                fieldMappingJson.put(workflowFieldService.get(actionFieldJson.getLong(WorkflowAttributes.FIELD_ID)).getAliasName(), actionFieldJson);
            }
        }
        actionJson.put(WorkflowAttributes.FIELD_MAPPING, fieldMappingJson);
    }

    /**
     * <p>
     * Fill the email values from the given fieldMapping {@link JsonArray} into the actionField {@link JsonObject}
     * </p>
     *
     * @param i18NResolutionAdapter   Represents the resolver of the I18N strings
     * @param whatsappActionFieldJson The {@link JsonObject} where the values will be loaded
     * @param fieldMappingArray       The {@link JsonArray} containing the mapping of fields
     */
    private void fillWhatsappValuesToJson(final I18nResolutionAdapter i18NResolutionAdapter, final JsonObject whatsappActionFieldJson, final JsonArray fieldMappingArray) {
        final Long fieldId = whatsappActionFieldJson.getLong(WorkflowAttributes.FIELD_ID);
        final int fieldMappingArraySize = fieldMappingArray.size();

        for (int fieldMappingArrayIndex = 0; fieldMappingArrayIndex < fieldMappingArraySize; fieldMappingArrayIndex++) {
            final JsonObject fieldJson = fieldMappingArray.getJsonObject(fieldMappingArrayIndex);
            final Long fieldIdInMapping = fieldJson.getLong(WorkflowAttributes.FIELD_ID);

            if (fieldJson.containsKey(WorkflowAttributes.VALUES) && Objects.equals(fieldId, fieldIdInMapping)) {
                final JsonArray valuesArray = fieldJson.getJsonArray(WorkflowAttributes.VALUES);

                if (Objects.equals(WorkflowAttributes.RECIPIENTS, workflowFieldService.get(fieldIdInMapping).getAliasName())) {
                    final int valuesArraySize = valuesArray.size();

                    for (int valuesArrayIndex = 0; valuesArrayIndex < valuesArraySize; valuesArrayIndex++) {
                        final JsonObject existingValueJson = valuesArray.getJsonObject(valuesArrayIndex);
                        final int phoneTypeId = existingValueJson.getInt(WorkflowAttributes.VALUE);
                        final JsonObject valueJson = Json.object();

                        valueJson.put(WorkflowAttributes.ID, phoneTypeId);
                        valueJson.put(WorkflowAttributes.NAME, i18NResolutionAdapter.get(Phone.Type.valueOf(phoneTypeId).getDisplayName()));
                        existingValueJson.put(WorkflowAttributes.VALUE, valueJson);
                    }
                }
                whatsappActionFieldJson.put(WorkflowAttributes.VALUES, valuesArray);
                fieldMappingArray.remove(fieldJson);
                break;
            }
        }
    }
}
