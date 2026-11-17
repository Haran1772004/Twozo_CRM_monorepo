package com.tlc.crm.workflow.api.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.workflow.api.manager.WorkflowManager;
import com.tlc.crm.workflow.api.model.CriteriaConnector;
import com.tlc.crm.workflow.api.model.FieldGroup;
import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.TriggerSource.TriggerEvent;
import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.api.model.WorkflowAction;
import com.tlc.crm.workflow.api.model.WorkflowComparator;
import com.tlc.crm.workflow.api.model.WorkflowCriteria;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaField;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaSet;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaValue;
import com.tlc.crm.workflow.cache.service.TriggerActionEventService;
import com.tlc.crm.workflow.cache.service.TriggerActionSourceService;
import com.tlc.crm.workflow.cache.service.WorkflowComparatorService;
import com.tlc.crm.workflow.cache.service.WorkflowFieldService;
import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.crm.workflow.resolver.internal.WorkflowActionResolver;
import com.tlc.crm.workflow.resolver.internal.WorkflowCriteriaResolver;

import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides different ways to convert the data from / into {@link Workflow}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowConverter extends EntityConverter<Workflow> {

    private final WorkflowManager workflowManager;
    private final I18nService i18nAccess;
    private final TriggerActionSourceService triggerActionSourceService;
    private final TriggerActionEventService triggerActionEventService;
    private final WorkflowFieldService workflowFieldService;
    private final WorkflowComparatorService workflowComparatorService;
    private final WorkflowActionResolver workflowActionResolver;
    private final DateTimeUtil dateTimeUtil;
    private final WorkflowCriteriaResolver workflowCriteriaResolver;

    private WorkflowConverter() {
        this.workflowManager = WorkflowManager.getInstance();
        this.i18nAccess = I18nServiceProvider.get();
        this.triggerActionSourceService = TriggerActionSourceService.getInstance();
        this.triggerActionEventService = TriggerActionEventService.getInstance();
        this.workflowFieldService = WorkflowFieldService.getInstance();
        this.workflowComparatorService = WorkflowComparatorService.getInstance();
        this.workflowActionResolver = WorkflowActionResolver.getInstance();
        this.dateTimeUtil = DateTimeUtil.getInstance();
        this.workflowCriteriaResolver = WorkflowCriteriaResolver.getInstance();
    }

    private static class Instance {
        private static final WorkflowConverter INSTANCE = new WorkflowConverter();
    }

    /**
     * Gets the {@link WorkflowConverter} Instance.
     *
     * @return {@link WorkflowConverter}.
     */
    public static WorkflowConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterConfig Refers the {@link ConverterConfig} data.
     * @param jsonObject      Refers the request json data.
     * @return {@link Workflow} data.
     */
    @Override
    public Workflow convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        return converterConfig.isPartialData() ? constructWorkflowByPartialData(converterConfig, jsonObject)
                : constructWorkflow(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param converterConfig Represents the converter configuration.
     * @param workflow        Represents the {@link Workflow} entity.
     * @return The {@link JsonObject} of workflow details.
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final Workflow workflow) {
        final JsonObject workflowJson = Json.object();
        final JsonObject triggerJson = Json.object();
        final Long orgId = workflow.orgId();
        final I18nResolutionAdapter i18NResolutionAdapter = converterConfig.getI18nPlaceholderResolver();
        final JsonArray criteriaSetJson = getCriteriaSetJson(orgId, workflow, i18NResolutionAdapter);
        final short triggerSourceId = workflow.getTriggerSource().getId();

        workflowJson.put(WorkflowAttributes.ID, workflow.id());
        workflowJson.put(WorkflowAttributes.NAME, workflow.getName());
        workflowJson.put(WorkflowAttributes.DESCRIPTION, i18NResolutionAdapter.get(workflow.getDescription()));
        workflowJson.put(WorkflowAttributes.IS_ACTIVE, workflow.isActive());
        workflowJson.put(WorkflowAttributes.TEMPLATE_ID, workflow.getTemplateId());

        triggerJson.put(WorkflowAttributes.SOURCE, triggerSourceId);
        triggerJson.put(WorkflowAttributes.EVENT, workflow.getTriggerEvent().getId());

        workflowJson.put(WorkflowAttributes.TRIGGER, triggerJson);
        workflowJson.put(WorkflowAttributes.ACTIONS, getWorkflowActions(orgId, triggerSourceId, workflow, i18NResolutionAdapter));

        if (0 < criteriaSetJson.size()) {
            workflowJson.put(WorkflowAttributes.CONDITIONS, criteriaSetJson);
        }

        return workflowJson;
    }

    /**
     * <p>
     * Gets the array of {@link WorkflowAction} details
     * </p>
     *
     * @param orgId                   Refers the organization id.
     * @param triggerSourceId         Refers the trigger source id.
     * @param workflow                Refers the {@link Workflow}
     * @param i18NResolutionAdapter Refers the {@link I18nResolutionAdapter} for resolve the i18n data.
     * @return The {@link JsonArray} filled with {@link WorkflowAction} details.
     */
    private JsonArray getWorkflowActions(final Long orgId, final short triggerSourceId, final Workflow workflow, final I18nResolutionAdapter i18NResolutionAdapter) {
        final JsonArray workflowActionArray = Json.array();

        for (final WorkflowAction workflowAction : workflow.getActions()) {
            final JsonObject workflowActionJson = Json.object();
            final JsonObject actionSourceJson = Json.object();
            final JsonObject actionEventJson = Json.object();
            final long actionSourceId = workflowAction.getTriggerActionSource().id();
            final long actionEventId = workflowAction.getTriggerActionEvent().id();

            actionSourceJson.put(WorkflowAttributes.ID, actionSourceId);
            actionSourceJson.put(WorkflowAttributes.NAME, i18NResolutionAdapter.get(triggerActionSourceService.get(actionSourceId).getDisplayName()));

            actionEventJson.put(WorkflowAttributes.ID, actionEventId);
            actionEventJson.put(WorkflowAttributes.NAME, i18NResolutionAdapter.get(triggerActionEventService.get(actionEventId).getDisplayName()));

            workflowActionJson.put(WorkflowAttributes.ID, workflowAction.getId());
            workflowActionJson.put(WorkflowAttributes.SOURCE, actionSourceJson);
            workflowActionJson.put(WorkflowAttributes.EVENT, actionEventJson);

            workflowActionResolver.resolveActionJson(orgId, workflow, triggerSourceId, workflowActionJson, workflowAction, i18NResolutionAdapter);
            workflowActionArray.put(workflowActionJson);
        }
        return workflowActionArray;
    }

    /**
     * {@inheritDoc}
     *
     * @param workflow Refers the {@link Workflow}.
     * @return The {@link JsonObject} which holds the workflow data.
     */
    @Override
    public JsonObject convert(final Workflow workflow) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(WorkflowAttributes.ID, workflow.id());
        jsonObject.put(WorkflowAttributes.NAME, workflow.getName());
        jsonObject.put(WorkflowAttributes.IS_ACTIVE, workflow.isActive());
        jsonObject.put(WorkflowAttributes.TRIGGER_SOURCE, workflow.getTriggerSource().getId());
        jsonObject.put(WorkflowAttributes.TRIGGER_EVENT, workflow.getTriggerEvent().getId());
        jsonObject.put(WorkflowAttributes.ACTIONS, workflow.getTotalActions());
        jsonObject.put(WorkflowAttributes.MODIFIED_TIME, dateTimeUtil.getTimeStamp(workflow.getModifiedTime()));

        return jsonObject;
    }

    /**
     * Converts the criteria sets into the {@link JsonArray}.
     *
     * @param orgId                   Refers the organization id.
     * @param workflow                Refers the {@link Workflow}.
     * @param i18NResolutionAdapter Refers the {@link I18nResolutionAdapter} for resolve the i18n data.
     * @return The criteria set {@link JsonArray}.
     */
    public JsonArray getCriteriaSetJson(final long orgId, final Workflow workflow, final I18nResolutionAdapter i18NResolutionAdapter) {
        final JsonArray conditions = Json.array();

        for (final WorkflowCriteriaSet workflowCriteriaSet : workflow.getCriteriaSets()) {
            final JsonObject criteriaSet = Json.object();
            final CriteriaConnector connector = workflowCriteriaSet.getConnector();
            final Collection<WorkflowCriteria> criteriaList = workflowCriteriaSet.getCriteriaList()
                    .stream().sorted(Comparator.comparing(WorkflowCriteria::getOrder)).toList();
            final JsonArray criteriaArray = Json.array();

            for (final WorkflowCriteria workflowCriteria : criteriaList) {
                final JsonObject criteriaJson = Json.object();
                final JsonArray criteriaValueArray = Json.array();
                final CriteriaConnector criteriaConnector = workflowCriteria.getConnector();
                final WorkflowCriteriaField workflowCriteriaField = workflowCriteria.getCriteriaField();

                criteriaJson.put(WorkflowAttributes.ID, workflowCriteria.getId());
                criteriaJson.put(WorkflowAttributes.FIELD, getCriteriaFieldJson(orgId, workflowCriteriaField, i18NResolutionAdapter));
                criteriaJson.put(WorkflowAttributes.COMPARATOR, getComparatorJson(workflowCriteria.getComparator(), i18NResolutionAdapter));

                if (Objects.nonNull(criteriaConnector)) {
                    criteriaJson.put(WorkflowAttributes.CONNECTOR, criteriaConnector.getId());
                }

                for (final WorkflowCriteriaValue workflowCriteriaValue : workflowCriteria.getCriteriaValues()) {

                    if (workflowCriteria.getComparator().hasValue()) {
                        final long fieldId = workflowCriteriaField.getFieldId();
                        final StoreType storeType = Objects.equals(FieldGroup.SOURCE_RELATED_FIELD, workflowCriteriaField.getFieldGroup())
                                ? fieldService.getField(orgId, fieldId).storeType() : workflowFieldService.get(fieldId).getFieldType().storeType();

                        if (storeType.isOwnerType()) {
                            workflow.addUserId(NumberUtils.parseNumber(workflowCriteriaValue.getValue()).longValue());
                        } else {
                            workflowCriteriaResolver.resolveCriteriaDetails(orgId, workflowCriteriaField, workflowCriteriaValue, i18NResolutionAdapter);
                        }
                    }

                    final JsonObject criteriaValue = Json.object();
                    final Long valueId = workflowCriteriaValue.getValueId();

                    if (Objects.nonNull(valueId)) {
                        final JsonObject valueJson = Json.object();

                        criteriaValue.put(WorkflowAttributes.ID, workflowCriteriaValue.getValueId());
                        criteriaValue.put(WorkflowAttributes.NAME, workflowCriteriaValue.getValue());

                        valueJson.put(WorkflowAttributes.ID, workflowCriteriaValue.getId());
                        valueJson.put(WorkflowAttributes.VALUE, criteriaValue);

                        criteriaValueArray.put(valueJson);
                    } else {
                        criteriaValue.put(WorkflowAttributes.VALUE, workflowCriteriaValue.getValue());
                        criteriaValue.put(WorkflowAttributes.ID, workflowCriteriaValue.getId());

                        criteriaValueArray.put(criteriaValue);
                    }
                }

                if (criteriaValueArray.size() > 0) {
                    criteriaJson.put(WorkflowAttributes.VALUES, criteriaValueArray);
                }
                criteriaArray.put(criteriaJson);
            }

            if (Objects.nonNull(connector)) {
                criteriaSet.put(WorkflowAttributes.CONNECTOR, connector.getId());
            }
            criteriaSet.put(WorkflowAttributes.ID, workflowCriteriaSet.getId());
            criteriaSet.put(WorkflowAttributes.CRITERIA, criteriaArray);
            conditions.put(criteriaSet);
        }

        return conditions;
    }

    /**
     * Gets the {@link JsonObject} of comparator details.
     *
     * @param workflowComparator      Refers the {@link WorkflowComparator} details.
     * @param i18NResolutionAdapter Refers the {@link I18nResolutionAdapter} for resolve the i18n data.
     * @return The {@link WorkflowComparator} details json.
     */
    private JsonObject getComparatorJson(final WorkflowComparator workflowComparator, final I18nResolutionAdapter i18NResolutionAdapter) {
        final JsonObject comparatorJson = Json.object();

        comparatorJson.put(WorkflowAttributes.ID, workflowComparator.getId());
        comparatorJson.put(WorkflowAttributes.NAME, i18NResolutionAdapter.get(workflowComparator.getName()));
        comparatorJson.put(WorkflowAttributes.HAS_VALUE, workflowComparator.hasValue());

        return comparatorJson;
    }

    /**
     * Gets the {@link JsonObject} of {@link WorkflowCriteriaField}.
     *
     * @param orgId                   Refers the organization id.
     * @param workflowCriteriaField   Refers the criteria field.
     * @param i18NResolutionAdapter Refers the {@link I18nResolutionAdapter} for resolve the i18n data.
     * @return The criteria field json.
     */
    private JsonObject getCriteriaFieldJson(final long orgId, final WorkflowCriteriaField workflowCriteriaField, final I18nResolutionAdapter i18NResolutionAdapter) {
        final JsonObject criteriaFieldJson = Json.object();
        final Long fieldId = workflowCriteriaField.getFieldId();

        criteriaFieldJson.put(WorkflowAttributes.NAME, i18NResolutionAdapter.get(Objects
                .equals(FieldGroup.SOURCE_RELATED_FIELD, workflowCriteriaField.getFieldGroup())
                ? fieldService.getField(orgId, fieldId).i18nName()
                : workflowFieldService.get(fieldId).getName()));
        criteriaFieldJson.put(WorkflowAttributes.ID, fieldId);

        return criteriaFieldJson;
    }

    /**
     * Converts the partial {@link JsonObject} into the {@link Workflow}.
     *
     * @param converterConfig Which refers the {@link ConverterConfig}.
     * @param jsonObject      Which holds the json data.
     * @return {@link Workflow} which contains the workflow details.
     */
    private Workflow constructWorkflowByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Workflow workflow = workflowManager.get(converterConfig.orgId(), jsonObject.getLong(WorkflowAttributes.ID));

        setJsonDataToModel(converterConfig, workflow, jsonObject);
        return workflow;
    }

    /**
     * Converts the {@link JsonObject} into the {@link Workflow}.
     *
     * @param converterConfig Which refers the {@link ConverterConfig}.
     * @param jsonObject      Which holds the json data.
     * @return {@link Workflow} which contains the workflow details.
     */
    private Workflow constructWorkflow(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Workflow workflow = new Workflow();

        if (jsonObject.containsKey(WorkflowAttributes.ID)) {
            workflow.setId(jsonObject.getLong(WorkflowAttributes.ID));
        }
        setJsonDataToModel(converterConfig, workflow, jsonObject);

        return workflow;
    }

    /**
     * Sets the json details into the {@link Workflow}.
     *
     * @param converterConfig Refers the {@link ConverterConfig} details.
     * @param workflow        Refers the {@link Workflow} to which the data to be set from json.
     * @param jsonObject      Refers the {@link JsonObject} of workflow details.
     */
    private void setJsonDataToModel(final ConverterConfig converterConfig, final Workflow workflow, final JsonObject jsonObject) {
        workflow.setOrgId(converterConfig.orgId());
        workflow.setOwnerId(converterConfig.ownerId());
        workflow.setName(jsonObject.containsKey(WorkflowAttributes.NAME)
                ? jsonObject.getString(WorkflowAttributes.NAME) : workflow.getName());
        workflow.setDescription(jsonObject.containsKey(WorkflowAttributes.DESCRIPTION)
                ? jsonObject.getString(WorkflowAttributes.DESCRIPTION) : workflow.getDescription());
        workflow.setActive(jsonObject.optBoolean(WorkflowAttributes.IS_ACTIVE, true));

        if (jsonObject.containsKey(WorkflowAttributes.TEMPLATE_ID)) {
            workflow.setTemplateId(jsonObject.getLong(WorkflowAttributes.TEMPLATE_ID));
        } else if (converterConfig.isPartialData()) {
            workflow.setTemplateId(workflow.getTemplateId());
        }

        if (jsonObject.containsKey(WorkflowAttributes.TRIGGER)) {
            final JsonObject triggerJson = jsonObject.getJsonObject(WorkflowAttributes.TRIGGER);

            workflow.setTriggerSource(TriggerSource.get(triggerJson.getShort(WorkflowAttributes.SOURCE)));
            workflow.setTriggerEvent(TriggerEvent.get(triggerJson.getShort(WorkflowAttributes.EVENT)));
        } else if (converterConfig.isPartialData()) {
            workflow.setTriggerSource(workflow.getTriggerSource());
            workflow.setTriggerEvent(workflow.getTriggerEvent());
        }

        if (jsonObject.containsKey(WorkflowAttributes.CONDITIONS)) {
            constructCriteriaSets(converterConfig, workflow, jsonObject);
        }

        if (jsonObject.containsKey(WorkflowAttributes.ACTIONS)) {
            constructWorkflowAction(converterConfig, workflow, jsonObject);
        }
    }

    /**
     * Constructs the {@link WorkflowAction} from the json.
     *
     * @param converterConfig Refers the {@link ConverterConfig} details.
     * @param workflow        Refers the {@link Workflow} to which the data to be set from json.
     * @param jsonObject      Refers the {@link JsonObject} of workflow details.
     */
    private void constructWorkflowAction(final ConverterConfig converterConfig, final Workflow workflow, final JsonObject jsonObject) {
        final Collection<WorkflowAction> oldWorkflowActions = workflow.getActions();
        final boolean isPartialData = converterConfig.isPartialData();

        workflow.setActions(null);

        if (jsonObject.containsKey(WorkflowAttributes.ACTIONS)) {
            final JsonArray actions = jsonObject.getJsonArray(WorkflowAttributes.ACTIONS);

            for (int index = 0; index < actions.size(); index++) {
                final JsonObject actionJson = actions.optJsonObject(index);
                final WorkflowAction workflowAction = new WorkflowAction();

                if (actionJson.containsKey(WorkflowAttributes.ID)) {
                    workflowAction.setId(actionJson.getLong(WorkflowAttributes.ID));
                }
                workflowAction.setTriggerActionSource(triggerActionSourceService.get(actionJson.getLong(WorkflowAttributes.SOURCE)));
                workflowAction.setTriggerActionEvent(triggerActionEventService.get(actionJson.getLong(WorkflowAttributes.EVENT)));
                workflowAction.setFieldMapping(actionJson.getJsonArray(WorkflowAttributes.FIELD_MAPPING).toString());

                workflow.addAction(workflowAction);
            }

            if (isPartialData) {
                final Set<Long> currentActionIds = workflow.getActions().stream()
                        .map(WorkflowAction::getId).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());

                oldWorkflowActions.stream().filter(oldAction -> !currentActionIds.contains(oldAction.getId()))
                        .forEach(workflow::addAction);
            }
        } else if (isPartialData) {
            workflow.setActions(oldWorkflowActions);
        }
    }

    /**
     * Constructs the {@link WorkflowCriteriaSet} from the json.
     *
     * @param converterConfig Refers the {@link ConverterConfig} details.
     * @param workflow        Refers the {@link Workflow} to which the data to be set from json.
     * @param jsonObject      Refers the {@link JsonObject} of workflow details.
     */
    private void constructCriteriaSets(final ConverterConfig converterConfig, final Workflow workflow, final JsonObject jsonObject) {
        final Collection<WorkflowCriteriaSet> oldCriteriaSets = workflow.getCriteriaSets();
        final boolean isPartialData = converterConfig.isPartialData();

        workflow.setCriteriaSets(null);

        if (jsonObject.containsKey(WorkflowAttributes.CONDITIONS)) {
            final JsonArray conditionArray = jsonObject.getJsonArray(WorkflowAttributes.CONDITIONS);

            for (int index = 0; index < conditionArray.size(); index++) {
                final JsonObject criteriaSetJson = conditionArray.optJsonObject(index);
                final WorkflowCriteriaSet workflowCriteriaSet = new WorkflowCriteriaSet();

                if (criteriaSetJson.containsKey(WorkflowAttributes.ID)) {
                    workflowCriteriaSet.setId(criteriaSetJson.getLong(WorkflowAttributes.ID));
                }
                final WorkflowCriteriaSet oldCriteriaSet = isPartialData ? oldCriteriaSets.stream()
                        .filter(oldSet -> Objects.nonNull(oldSet.getId()) && Objects.equals(workflowCriteriaSet.getId(), oldSet.getId()))
                        .findAny().orElse(null) : null;

                if (criteriaSetJson.containsKey(WorkflowAttributes.CONNECTOR)) {
                    workflowCriteriaSet.setConnector(CriteriaConnector.get(criteriaSetJson.getShort(WorkflowAttributes.CONNECTOR)));
                }

                if (criteriaSetJson.containsKey(WorkflowAttributes.CRITERIA)) {
                    setCriteria(converterConfig, workflow, workflowCriteriaSet, oldCriteriaSet, criteriaSetJson);
                }

                workflow.addCriteriaSet(workflowCriteriaSet);
            }

            if (isPartialData) {
                final Set<Long> currentCriteriaSetIds = workflow.getCriteriaSets().stream()
                        .map(WorkflowCriteriaSet::getId).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());

                oldCriteriaSets.stream().filter(oldCriteria -> !currentCriteriaSetIds.contains(oldCriteria.getId()))
                        .forEach(workflow::addCriteriaSet);
            }
        } else if (isPartialData) {
            workflow.setCriteriaSets(oldCriteriaSets);
        }
    }

    /**
     * Sets the {@link WorkflowCriteria} details into the workflow from the json.
     *
     * @param converterConfig     Refers the {@link ConverterConfig} details.
     * @param workflow            Refers the {@link Workflow} to which the data to be set from json.
     * @param workflowCriteriaSet Refers the {@link WorkflowCriteriaSet} details.
     * @param oldCriteriaSet      Refers the old {@link WorkflowCriteriaSet} details.
     * @param jsonObject          Refers the {@link JsonObject} of workflow details.
     */
    private void setCriteria(final ConverterConfig converterConfig, final Workflow workflow,
                             final WorkflowCriteriaSet workflowCriteriaSet,
                             final WorkflowCriteriaSet oldCriteriaSet, final JsonObject jsonObject) {
        final boolean isOldCriteriaSetExist = converterConfig.isPartialData() && Objects.nonNull(oldCriteriaSet);

        if (jsonObject.containsKey(WorkflowAttributes.CRITERIA)) {
            final TriggerSource triggerSource = workflow.getTriggerSource();
            final JsonArray jsonArray = jsonObject.getJsonArray(WorkflowAttributes.CRITERIA);

            for (int index = 0; index < jsonArray.size(); index++) {
                final JsonObject criteriaJson = jsonArray.optJsonObject(index);
                final WorkflowCriteria workflowCriteria = new WorkflowCriteria();
                final WorkflowCriteriaField workflowCriteriaField = new WorkflowCriteriaField();

                if (criteriaJson.containsKey(WorkflowAttributes.ID)) {
                    workflowCriteria.setId(criteriaJson.getLong(WorkflowAttributes.ID));
                }

                if (criteriaJson.containsKey(WorkflowAttributes.CONNECTOR)) {
                    workflowCriteria.setConnector(CriteriaConnector.get(criteriaJson.getShort(WorkflowAttributes.CONNECTOR)));
                }
                workflowCriteria.setComparator(workflowComparatorService.get(criteriaJson.getLong(WorkflowAttributes.COMPARATOR)));

                workflowCriteriaField.setFieldId(criteriaJson.getLong(WorkflowAttributes.FIELD_ID));
                workflowCriteriaField.setFieldGroup(Objects.equals(TriggerSource.ACTIVITY, triggerSource)
                        ? FieldGroup.WORKFLOW_RELATED_FIELD : FieldGroup.SOURCE_RELATED_FIELD);
                workflowCriteria.setCriteriaField(workflowCriteriaField);

                if (criteriaJson.containsKey(WorkflowAttributes.VALUES)) {
                    setCriteriaValue(workflowCriteria, criteriaJson.getJsonArray(WorkflowAttributes.VALUES));
                }
                workflowCriteriaSet.addCriteria(workflowCriteria);
            }

            if (isOldCriteriaSetExist) {
                final Set<Long> currentCriteriaIds = workflowCriteriaSet.getCriteriaList().stream()
                        .map(WorkflowCriteria::getId).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());

                oldCriteriaSet.getCriteriaList().stream().filter(oldCriteria -> !currentCriteriaIds.contains(oldCriteria.getId()))
                        .forEach(workflowCriteriaSet::addCriteria);
            }
        } else if (isOldCriteriaSetExist) {
            workflowCriteriaSet.setCriteriaList(oldCriteriaSet.getCriteriaList());
        }
    }

    /**
     * Sets the {@link WorkflowCriteriaValue} into the criteria details.
     *
     * @param workflowCriteria Refers the {@link WorkflowCriteria} details.
     * @param jsonArray        Refers the criteria value {@link JsonArray}.
     */
    private void setCriteriaValue(final WorkflowCriteria workflowCriteria, final JsonArray jsonArray) {

        for (int index = 0; index < jsonArray.size(); index++) {
            final WorkflowCriteriaValue workflowCriteriaValue = new WorkflowCriteriaValue();
            final JsonObject jsonObject = jsonArray.getJsonObject(index);

            if (jsonObject.containsKey(WorkflowAttributes.ID)) {
                workflowCriteriaValue.setId(jsonObject.getLong(WorkflowAttributes.ID));
            }

            if (jsonObject.containsKey(WorkflowAttributes.VALUE)) {
                final Object value = jsonObject.get(WorkflowAttributes.VALUE);

                if (value instanceof final Map<?, ?> valueJsonMap) {
                    final JsonObject valueJson = Json.object((Map<String, ?>) valueJsonMap);

                    if (valueJson.containsKey(WorkflowAttributes.ID)) {
                        workflowCriteriaValue.setValueId(valueJson.getLong(WorkflowAttributes.ID));
                    }

                    if (valueJson.containsKey(WorkflowAttributes.NAME)) {
                        workflowCriteriaValue.setValue(valueJson.get(WorkflowAttributes.NAME));
                    }
                } else {
                    workflowCriteriaValue.setValue(value);
                }
            }
            workflowCriteria.addCriteriaValue(workflowCriteriaValue);
        }
    }
}
