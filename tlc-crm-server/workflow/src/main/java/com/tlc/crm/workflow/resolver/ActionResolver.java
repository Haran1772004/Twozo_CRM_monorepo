package com.tlc.crm.workflow.resolver;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.fields.builder.FieldVisibilityHandler;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.models.converter.FieldVisibilityGroupConverter;
import com.tlc.crm.entity.models.type.DropdownType;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.service.settings.SourceSettingsService;
import com.tlc.crm.workflow.api.manager.WorkflowActionManager;
import com.tlc.crm.workflow.api.model.FieldGroup;
import com.tlc.crm.workflow.api.model.TriggerActionField;
import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.api.model.WorkflowAction;
import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.cache.service.StepOptionService;
import com.tlc.crm.workflow.cache.service.TriggerActionEventService;
import com.tlc.crm.workflow.cache.service.TriggerActionFieldService;
import com.tlc.crm.workflow.cache.service.TriggerActionSourceService;
import com.tlc.crm.workflow.cache.service.WorkflowFieldService;
import com.tlc.crm.workflow.field.WorkflowAttributes;

import com.tlc.i18n.I18nResolutionAdapter;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * An {@link ActionResolver} which resolves the workflow and template related actions
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public abstract class ActionResolver {

    protected final WorkflowFieldService workflowFieldService;
    protected final TriggerActionSourceService triggerActionSourceService;
    protected final FieldService fieldService;
    protected final WorkflowActionManager workflowActionManager;
    protected final TriggerActionFieldService triggerActionFieldService;
    protected final FieldVisibilityGroupConverter fieldVisibilityGroupConverter;
    protected final StepOptionService stepOptionService;
    protected final TriggerActionEventService triggerActionEventService;
    private final SourceSettingsService sourceSettingsService;

    protected ActionResolver() {
        this.workflowFieldService = WorkflowFieldService.getInstance();
        this.triggerActionSourceService = TriggerActionSourceService.getInstance();
        this.fieldService = FieldService.getInstance();
        this.fieldVisibilityGroupConverter = FieldVisibilityGroupConverter.getInstance();
        this.workflowActionManager = WorkflowActionManager.getInstance();
        this.triggerActionFieldService = TriggerActionFieldService.getInstance();
        this.stepOptionService = StepOptionService.getInstance();
        this.triggerActionEventService = TriggerActionEventService.getInstance();
        this.sourceSettingsService = SourceSettingsService.getInstance();
    }

    /**
     * <p>
     * Resolves the {@link WorkflowAction} details into the {@link JsonObject}
     * </p>
     *
     * @param orgId                   Represents the organization id
     * @param triggerSourceId         Represents the trigger source id
     * @param workflowAction          It holds the {@link WorkflowAction} details
     * @param actionJson              Represents the action {@link JsonObject}
     * @param i18NResolutionAdapter Refers the {@link I18nResolutionAdapter} for resolve the i18n data
     */
    public void resolveActionJson(final long orgId, final short triggerSourceId, final JsonObject actionJson,
                                  final WorkflowAction workflowAction, final I18nResolutionAdapter i18NResolutionAdapter) {
        final long triggerActionSourceId = workflowAction.getTriggerActionSource().id();
        final JsonArray fieldMappingArray = workflowAction.getFieldMappingAsJson();
        final Collection<TriggerActionField> triggerActionFields = triggerActionFieldService.get(workflowActionManager
                .getTriggerActionId(triggerSourceId, triggerActionSourceId, workflowAction.getTriggerActionEvent().id()));
        final Map<Integer, List<TriggerActionField>> groupedActionFields = triggerActionFieldService.groupActionFieldsByConfig(triggerActionFields);

        if (Objects.equals(WorkflowAttributes.EMAIL, triggerActionSourceService.getAliasName(triggerActionSourceId))) {
            resolveEmailActionJson(orgId, groupedActionFields, fieldMappingArray, actionJson, i18NResolutionAdapter);
        } else {
            resolveSourceActionJson(orgId, groupedActionFields, workflowAction, actionJson, i18NResolutionAdapter);
        }
    }

    /**
     * <p>
     * Resolves the email related {@link WorkflowAction} details into the {@link JsonObject}
     * </p>
     *
     * @param orgId                   Represents the organization id
     * @param groupedActionFields     Represents the grouped {@link TriggerActionField}
     * @param fieldMappingArray       The {@link JsonArray} containing the mapping of fields
     * @param actionJson              Represents the action {@link JsonObject}
     * @param i18NResolutionAdapter Refers the {@link I18nResolutionAdapter} for resolve the i18n data
     */
    protected void resolveEmailActionJson(final Long orgId, final Map<Integer, List<TriggerActionField>> groupedActionFields,
                                          final JsonArray fieldMappingArray, final JsonObject actionJson, final I18nResolutionAdapter i18NResolutionAdapter) {
        final JsonObject fieldMappingJson = Json.object();

        for (final Map.Entry<Integer, List<TriggerActionField>> triggerActionFieldEntry : groupedActionFields.entrySet()) {

            for (final TriggerActionField triggerActionField : triggerActionFieldEntry.getValue()) {
                final JsonObject emailActionFieldJson = Json.object();

                loadActionFieldToJson(orgId, emailActionFieldJson, i18NResolutionAdapter, null, triggerActionField);
                fillEmailValuesToJson(orgId, i18NResolutionAdapter, emailActionFieldJson, fieldMappingArray);

                fieldMappingJson.put(workflowFieldService.get(emailActionFieldJson.getLong(WorkflowAttributes.FIELD_ID)).getAliasName(), emailActionFieldJson);
            }
        }
        actionJson.put(WorkflowAttributes.FIELD_MAPPING, fieldMappingJson);
    }

    /**
     * <p>
     * Resolves the source related {@link WorkflowAction} details into the {@link JsonObject}
     * </p>
     *
     * @param orgId                   Represents the organization id
     * @param groupedActionFields     It holds the grouped {@link TriggerActionField}
     * @param workflowAction          Refers the workflow action attributes.
     * @param actionJson              Represents the action {@link JsonObject}
     * @param i18NResolutionAdapter Refers the {@link I18nResolutionAdapter} for resolve the i18n data
     */
    private void resolveSourceActionJson(final Long orgId, final Map<Integer, List<TriggerActionField>> groupedActionFields,
                                         final WorkflowAction workflowAction, final JsonObject actionJson, final I18nResolutionAdapter i18NResolutionAdapter) {
        final long triggerActionSourceId = workflowAction.getTriggerActionSource().id();
        final long triggerActionEventId = workflowAction.getTriggerActionEvent().id();

        final boolean isCommonActionSource = triggerActionSourceService.get(triggerActionSourceId).isCommonActionSource();
        final boolean isCommonActionEvent = triggerActionEventService.get(triggerActionEventId).isCommonActionEvent();

        final JsonArray fieldMappingArray = workflowAction.getFieldMappingAsJson();
        final JsonArray actionFieldsArray = Json.array();

        for (final Map.Entry<Integer, List<TriggerActionField>> triggerActionFieldEntry : groupedActionFields.entrySet()) {
            final JsonArray fieldsArray = Json.array();

            for (final TriggerActionField triggerActionField : triggerActionFieldEntry.getValue()) {
                final JsonObject actionFieldJson = Json.object();
                final boolean isEntitySource = isCommonActionSource && FieldGroup.SOURCE_RELATED_FIELD == triggerActionField.getFieldGroup();

                loadActionFieldToJson(orgId, actionFieldJson, i18NResolutionAdapter, isEntitySource
                        ? fieldService.getField(orgId, triggerActionField.getFieldId()) : null, triggerActionField);
                fillActionFieldValuesToJson(orgId, null, isEntitySource, i18NResolutionAdapter, actionFieldJson, fieldMappingArray);

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
     * Builds a {@link JsonObject} representation of a workflow {@link TriggerActionField} based on the provided
     * parameters
     * </p>
     *
     * @param orgId                   Represents the organization id
     * @param triggerActionFieldJson  The {@link JsonObject} to be populated with workflow {@link TriggerActionField} details
     * @param i18NResolutionAdapter Refers the {@link I18nResolutionAdapter} for resolve the i18n data.
     * @param fieldInfo               Refers the {@link FieldInfo} details
     * @param triggerActionField      Refers the {@link TriggerActionField} details
     */
    public void loadActionFieldToJson(final long orgId, final JsonObject triggerActionFieldJson, final I18nResolutionAdapter i18NResolutionAdapter,
                                      final FieldInfo fieldInfo, final TriggerActionField triggerActionField) {
        final JsonArray requiredFieldArray = Json.array();
        final JsonObject dropdownJson = Json.object();
        final JsonObject configJson = Json.object();

        if (Objects.isNull(fieldInfo)) {
            final Long fieldId = triggerActionField.getFieldId();
            final String storeType = triggerActionField.storeType();
            final WorkflowField workflowField = workflowFieldService.get(fieldId);

            triggerActionField.dependsOn().ifPresent(dependsOn -> {
                final WorkflowField dependsOnField = workflowFieldService.getWorkflowField(dependsOn);

                configJson.put(WorkflowAttributes.DEPENDS_ON, dependsOnField.getId());
            });

            triggerActionFieldJson.put(WorkflowAttributes.DISPLAY_NAME, i18NResolutionAdapter.get(workflowField.getName()));
            triggerActionFieldJson.put(WorkflowAttributes.FIELD_ID, fieldId);
            triggerActionFieldJson.put(WorkflowAttributes.FIELD_TYPE, Objects.isNull(storeType) ? workflowField.getFieldType().storeType() : storeType);

            if (Objects.nonNull(workflowField.dependsOn())) {
                requiredFieldArray.put(WorkflowAttributes.DEPENDENT_ID);
            }
            loadValidationDetailsToJson(configJson, workflowField.getConfigJson(), i18NResolutionAdapter);
            dropdownJson.put(WorkflowAttributes.NAME, workflowField.dropdown());
        } else {
            final String storeType = Objects.isNull(triggerActionField.storeType()) ? fieldInfo.storeType().getName() : triggerActionField.storeType();
            final PresetConfig presetConfig = fieldInfo.presetConfig();
            final FieldVisibilityHandler fieldVisibilityHandler = FieldVisibilityHandler.get(Source.get(fieldInfo.fieldSource().getName()));
            final String dependsOn = presetConfig.dependsOn();
            final String dropdown = presetConfig.dropdown();
            final StoreType fieldStoreType = fieldInfo.storeType();

            loadValidationDetailsToJson(configJson, presetConfig.presetConfigAsJson(), i18NResolutionAdapter);

            if (Objects.equals(WorkflowAttributes.ID, fieldInfo.aliasName())) {
                final FieldSource fieldSource = fieldInfo.fieldSource();
                final String alternateModuleName = sourceSettingsService.getSourceSettings(orgId, fieldSource.id()).getAlternateName();

                triggerActionFieldJson.put(WorkflowAttributes.DISPLAY_NAME, Objects.nonNull(alternateModuleName)
                        ? alternateModuleName
                        : i18NResolutionAdapter.get(fieldSource.getDisplayName()));
            } else {
                triggerActionFieldJson.put(WorkflowAttributes.DISPLAY_NAME, i18NResolutionAdapter.get(fieldInfo.i18nName()));
            }
            triggerActionFieldJson.put(WorkflowAttributes.FIELD_ID, fieldInfo.id());
            triggerActionFieldJson.put(WorkflowAttributes.FIELD_TYPE, storeType);

            if (Objects.nonNull(dependsOn)) {
                requiredFieldArray.put(WorkflowAttributes.DEPENDENT_ID);
                configJson.put(WorkflowAttributes.DEPENDS_ON, dependsOn);
            }

            if (Objects.nonNull(fieldVisibilityHandler)) {
                fieldVisibilityHandler.getCriteriaGroup(orgId, fieldInfo).ifPresent(visibilityCriteriaGroup ->
                        fieldVisibilityGroupConverter.loadCriteriaGroupIntoJson(orgId, configJson, visibilityCriteriaGroup, i18NResolutionAdapter, FieldKeys.SHOW_WHEN.getName()));
            }

            if ((StoreType.isSelectorType(fieldStoreType) || fieldStoreType.isEntityType()) || Objects.nonNull(dropdown)) {
                switch (fieldStoreType) {
                    case COMPANY, COMPANY_ONBOARD ->
                            dropdownJson.put(WorkflowAttributes.NAME, WorkflowAttributes.COMPANY_LIST);
                    case CONTACT, CONTACT_ONBOARD, CONTACTS_ONBOARD ->
                            dropdownJson.put(WorkflowAttributes.NAME, WorkflowAttributes.CONTACT_LIST);
                    default -> dropdownJson.put(WorkflowAttributes.NAME, presetConfig.dropdown());
                }
            }

            if (Objects.equals(WorkflowAttributes.ID, fieldInfo.aliasName()) && !Objects.equals(WorkflowAttributes.STEP_PICKER, storeType)) {
                dropdownJson.put(WorkflowAttributes.NAME, switch (fieldInfo.fieldSource().getName()) {
                    case "Contact" -> WorkflowAttributes.CONTACT_LIST;
                    case "Company" -> WorkflowAttributes.COMPANY_LIST;
                    case "Deal" -> WorkflowAttributes.DEAL_LIST;
                    default -> null;
                });
            }
        }

        if (triggerActionField.hasStepOptions()) {
            configJson.put(WorkflowAttributes.HAS_STEP_OPTIONS, true);
        }

        if (triggerActionField.isRequired()) {
            configJson.put(WorkflowAttributes.IS_REQUIRED, true);
        }

        if (configJson.size() > 0) {
            triggerActionFieldJson.put(WorkflowAttributes.CONFIG, configJson);
        }

        if (requiredFieldArray.size() > 0) {
            dropdownJson.put(WorkflowAttributes.REQUIRED_KEYS, requiredFieldArray);
        }

        if (dropdownJson.size() > 0) {
            triggerActionFieldJson.put(WorkflowAttributes.DROPDOWN, dropdownJson);
        }
    }

    /**
     * <p>
     * Fill the values from the given fieldMapping {@link JsonArray} into the actionField {@link JsonObject}
     * </p>
     *
     * @param orgId                   Represents the organization id
     * @param workflow                Represents the {@link Workflow}
     * @param isCommonActionSource    A boolean flag indicating whether the trigger action source are common or not
     * @param i18NResolutionAdapter Represents the resolver of the I18N strings
     * @param triggerActionFieldJson  The {@link JsonObject} where the values will be loaded
     * @param fieldMappingArray       The {@link JsonArray} containing the mapping of fields
     */
    protected void fillActionFieldValuesToJson(final Long orgId, final Workflow workflow, final boolean isCommonActionSource, final I18nResolutionAdapter i18NResolutionAdapter,
                                               final JsonObject triggerActionFieldJson, final JsonArray fieldMappingArray) {
    }

    /**
     * <p>
     * Fill the email values from the given fieldMapping {@link JsonArray} into the actionField {@link JsonObject}
     * </p>
     *
     * @param orgId                   Represents the organization id
     * @param i18NResolutionAdapter Represents the resolver of the I18N strings
     * @param emailActionFieldJson    The {@link JsonObject} where the values will be loaded
     * @param fieldMappingArray       The {@link JsonArray} containing the mapping of fields
     */
    protected abstract void fillEmailValuesToJson(final Long orgId, final I18nResolutionAdapter i18NResolutionAdapter,
                                                  final JsonObject emailActionFieldJson, final JsonArray fieldMappingArray);

    /**
     * <p>
     * Loads the custom fields information into action field {@link JsonArray}.
     * </p>
     *
     * @param orgId                   Represents the organization id.
     * @param actionSourceId          Represents the action source id.
     * @param i18NResolutionAdapter Refers the {@link I18nResolutionAdapter} for resolve the i18n data.
     * @param actionFieldsArray       Represents the action fields {@link JsonArray}
     * @param fieldMappingArray       Refers the {@link JsonArray} fieldMapping data.
     */
    public void loadPersistentFields(final long orgId, final long actionSourceId, final I18nResolutionAdapter i18NResolutionAdapter,
                                     final JsonArray actionFieldsArray, final JsonArray fieldMappingArray) {
        final Collection<FieldInfo> persistentFieldsBySource = fieldService.getPersistentFieldsBySource(orgId,
                Source.get(triggerActionSourceService.getAliasName(actionSourceId)).getName());
        final Collection<FieldInfo> customFieldInfoList = Objects.isNull(persistentFieldsBySource) ? List.of() : persistentFieldsBySource.stream()
                .filter(fieldInfo -> fieldInfo.isActive() && !isRestrictedStoreType(fieldInfo.storeType()))
                .sorted(Comparator.comparingInt(fieldInfo -> fieldInfo.presetConfig().order())).toList();

        if (!customFieldInfoList.isEmpty()) {

            for (final FieldInfo customFieldInfo : customFieldInfoList) {
                final JsonArray customFieldsArray = Json.array();
                final JsonObject customFieldJson = Json.object();

                loadPersistentFieldIntoJson(customFieldJson, i18NResolutionAdapter, customFieldInfo);

                customFieldsArray.put(customFieldJson);
                actionFieldsArray.put(customFieldsArray);
            }
        }
    }

    /**
     * <p>
     * Builds a {@link JsonObject} representation of a custom fields based on the provided parameters
     * </p>
     *
     * @param triggerActionFieldJson  The {@link JsonObject} to be populated with workflow {@link TriggerActionField} details
     * @param i18NResolutionAdapter Refers the {@link I18nResolutionAdapter} for resolve the i18n data.
     * @param fieldInfo               Refers the {@link FieldInfo} details
     */
    protected void loadPersistentFieldIntoJson(final JsonObject triggerActionFieldJson, final I18nResolutionAdapter i18NResolutionAdapter,
                                               final FieldInfo fieldInfo) {
        final JsonArray requiredFieldArray = Json.array();
        final JsonObject dropdownJson = Json.object();
        final JsonObject configJson = Json.object();
        final StoreType storeType = fieldInfo.storeType();
        final PresetConfig presetConfig = fieldInfo.presetConfig();

        triggerActionFieldJson.put(WorkflowAttributes.DISPLAY_NAME, i18NResolutionAdapter.get(fieldInfo.i18nName()));
        triggerActionFieldJson.put(WorkflowAttributes.FIELD_ID, fieldInfo.id());
        triggerActionFieldJson.put(WorkflowAttributes.FIELD_TYPE, fieldInfo.storeType());

        if (StoreType.isSelectorType(storeType)) {
            requiredFieldArray.put(WorkflowAttributes.ID);
        }

        if (Objects.nonNull(presetConfig.dependsOn())) {
            requiredFieldArray.put(WorkflowAttributes.DEPENDENT_ID);
            configJson.put(WorkflowAttributes.DEPENDS_ON, presetConfig.dependsOn());
        }

        if (Objects.equals(DropdownType.STATIC, presetConfig.dropdownType())) {
            dropdownJson.put(WorkflowAttributes.IS_STATIC, true);
        }
        dropdownJson.put(WorkflowAttributes.NAME, presetConfig.dropdown());
        loadValidationDetailsToJson(configJson, presetConfig.presetConfigAsJson(), i18NResolutionAdapter);

        if (configJson.size() > 0) {
            triggerActionFieldJson.put(WorkflowAttributes.CONFIG, configJson);
        }

        if (requiredFieldArray.size() > 0) {
            dropdownJson.put(WorkflowAttributes.REQUIRED_KEYS, requiredFieldArray);
        }

        if (dropdownJson.size() > 0) {
            triggerActionFieldJson.put(WorkflowAttributes.DROPDOWN, dropdownJson);
        }
    }

    /**
     * Populates the provided JSON object with field validation details.
     *
     * @param configJson              Refers the target JSON object where validation details will be loaded.
     * @param fieldConfig             Refers the JSON object containing the field-specific configuration.
     * @param i18NResolutionAdapter Refers the resolver used to fetch localized validation messages.
     */
    private void loadValidationDetailsToJson(final JsonObject configJson, final JsonObject fieldConfig, final I18nResolutionAdapter i18NResolutionAdapter) {
        final String maxKey = PresetConfig.Configuration.MAX.getName();
        final String minKey = PresetConfig.Configuration.MIN.getName();
        final String minLengthKey = PresetConfig.Configuration.MIN_LENGTH.getName();
        final String maxLengthKey = PresetConfig.Configuration.MAX_LENGTH.getName();
        final String maxLimitKey = PresetConfig.Configuration.MAX_LIMIT.getName();
        final String validationKey = PresetConfig.Configuration.PATTERN.getName();
        final String violationMessageKey = FieldKeys.VIOLATION_MESSAGE.getName();
        final String associatedWithKey = FieldKeys.ASSOCIATED_WITH.getName();

        if (fieldConfig.containsKey(minKey)) {
            configJson.put(minKey, fieldConfig.getInt(minKey));
        }

        if (fieldConfig.containsKey(maxKey)) {
            configJson.put(maxKey, fieldConfig.getLong(maxKey));
        }

        if (fieldConfig.containsKey(minLengthKey)) {
            configJson.put(minLengthKey, fieldConfig.getInt(minLengthKey));
        }

        if (fieldConfig.containsKey(maxLengthKey)) {
            configJson.put(maxLengthKey, fieldConfig.getInt(maxLengthKey));
        }

        if (fieldConfig.containsKey(maxLimitKey)) {
            configJson.put(maxLimitKey, fieldConfig.getInt(maxLimitKey));
        }

        if (fieldConfig.containsKey(validationKey)) {
            final String valueKey = FieldKeys.VALUE.getName();
            final JsonObject validationJson = fieldConfig.getJsonObject(validationKey);
            final String messageKey = FieldKeys.MESSAGE.getName();
            final String violationMessage;

            if (validationJson.containsKey(violationMessageKey)) {
                violationMessage = validationJson.getString(violationMessageKey);
            } else if (validationJson.containsKey(messageKey)) {
                violationMessage = validationJson.getString(messageKey);
            } else {
                violationMessage = null;
            }

            if (Objects.nonNull(violationMessage)) {
                validationJson.put(violationMessageKey, i18NResolutionAdapter.get(violationMessage));
                validationJson.remove(messageKey);
            }

            if (validationJson.containsKey(valueKey)) {
                validationJson.put(FieldKeys.REGEX.getName(), validationJson.get(valueKey));
                validationJson.remove(valueKey);
            }
            configJson.put(PresetConfig.Configuration.VALIDATION.getName(), validationJson);
        }

        if (fieldConfig.containsKey(associatedWithKey)) {
            configJson.put(associatedWithKey, fieldConfig.getInt(associatedWithKey));
        }
    }

    /**
     * <p>
     * Checks if the specified {@link StoreType} is supported for criteria field storage.
     * </p>
     *
     * @param storeType Represents the {@link StoreType}
     * @return true if the {@link StoreType} is supported, false otherwise
     */
    protected boolean isRestrictedStoreType(final StoreType storeType) {
        return switch (storeType) {
            case TIME_RANGE, DATE_TIME_RANGE, DATE_RANGE -> true;
            default -> false;
        };
    }
}
