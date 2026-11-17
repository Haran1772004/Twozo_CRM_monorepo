package com.tlc.crm.workflow.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.email.notification.email.sync.api.EmailSyncManager;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.workflow.api.model.CriteriaConnector;
import com.tlc.crm.workflow.api.model.FieldGroup;
import com.tlc.crm.workflow.api.model.TemplateCategory;
import com.tlc.crm.workflow.api.model.TriggerActionEvent;
import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.TriggerSource.TriggerEvent;
import com.tlc.crm.workflow.api.model.WorkflowAction;
import com.tlc.crm.workflow.api.model.WorkflowComparator;
import com.tlc.crm.workflow.api.model.WorkflowCriteria;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaField;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaSet;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaValue;
import com.tlc.crm.workflow.api.model.WorkflowTemplate;
import com.tlc.crm.workflow.cache.service.WorkflowFieldService;
import com.tlc.crm.workflow.cache.service.WorkflowTemplateService;
import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;
import com.tlc.crm.workflow.resolver.internal.TemplateActionResolver;
import com.tlc.crm.workflow.resolver.internal.TemplateCriteriaResolver;

import com.tlc.crm.workflow.v2.api.model.EventContextType;
import com.tlc.crm.workflow.v2.api.model.NodeType;
import com.tlc.crm.workflow.v2.api.model.TriggerScope;
import com.tlc.crm.workflow.v2.field.WorkflowAttributesV2;

import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

/**
 * Manages the {@link WorkflowTemplate} related web actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 * @see AuthorizationController
 */
@WebAction(path = "/workflow/template/mgmt")
public class WorkflowTemplateManagement extends AuthorizationController {

    private final WorkflowTemplateService workflowTemplateService;
    private final FieldService fieldService;
    private final TemplateCriteriaResolver templateCriteriaResolver;
    private final TemplateActionResolver templateActionResolver;
    private final WorkflowFieldService workflowFieldService;
    private final EmailSyncManager emailSyncManager;

    public WorkflowTemplateManagement() {
        this.workflowTemplateService = WorkflowTemplateService.getInstance();
        this.fieldService = FieldService.getInstance();
        this.templateCriteriaResolver = TemplateCriteriaResolver.getInstance();
        this.templateActionResolver = TemplateActionResolver.getInstance();
        this.workflowFieldService = WorkflowFieldService.getInstance();
        this.emailSyncManager = EmailSyncManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Refers the incoming request.
     * @param response Refers the response to be sent.
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject("data");
        final long orgId = request.orgId();
        final String type = requestJson.getString("type");

        switch (type) {
            case "get" -> response.put(WorkflowAttributes.TEMPLATE, getTemplate(orgId, request, data));
            case "getV2" -> response.put(WorkflowAttributes.TEMPLATE, getTemplateV2(orgId, request, data));
            case "getAll" -> {
                response.put(WorkflowAttributes.LIST, getAllTemplates(request, response));
            }
            case "getByCategory" -> {
                response.put(WorkflowAttributes.LIST, getTemplatesByCategory(request, data, response));
            }
        }
    }

    /**
     * Gets the json object of {@link WorkflowTemplate}.
     *
     * @param orgId   Refers the organization id.
     * @param request Refers the {@link CrmRequest} data.
     * @param data    Refers the request json data.
     * @return The {@link JsonObject} of template details.
     */
    private JsonObject getTemplate(final long orgId, final CrmRequest request, final JsonObject data) {
        final WorkflowTemplate workflowTemplate = workflowTemplateService.get(data.getLong(WorkflowAttributes.ID));

        if (Objects.isNull(workflowTemplate)) {
            throw ErrorCode.get(WorkflowErrorCodes.TEMPLATE_NOT_FOUND, "i18n.workflow.template.not.found");
        }
        return convertTemplateToJson(orgId, workflowTemplate, request.getI18NCRMResolutionAdapter());
    }

    /**
     * Gets the json array of {@link WorkflowTemplate}.
     *
     * @param request  Refers the {@link CrmRequest} data.
     * @param response Refers the {@link CrmResponse} to be sent.
     * @return The {@link JsonArray} of template details.
     */
    private JsonArray getAllTemplates(final CrmRequest request, final CrmResponse response) {
        final Collection<WorkflowTemplate> workflowTemplates = workflowTemplateService.getAll();
        final boolean isEmailSyncEnabled = emailSyncManager.isEmailSyncEnabled(request.orgId(), request.userId());
        final JsonArray jsonArray = Json.array();

        if (!workflowTemplates.isEmpty()) {

            for (final WorkflowTemplate workflowTemplate : workflowTemplates) {
                jsonArray.put(convertTemplateToJson(workflowTemplate, isEmailSyncEnabled, response));
            }
        }
        response.put(WorkflowAttributes.TOTAL_RECORDS, jsonArray.size());

        return jsonArray;
    }

    /**
     * Gets the json array of {@link WorkflowTemplate}.
     *
     * @param request  Refers the {@link CrmRequest} data.
     * @param data     Refers the request json data.
     * @param response Refers the response to be sent.
     * @return The {@link JsonArray} of template details.
     */
    private JsonArray getTemplatesByCategory(final CrmRequest request, final JsonObject data, final CrmResponse response) {
        final TemplateCategory templateCategory = TemplateCategory.get(data.getShort(WorkflowAttributes.CATEGORY));
        final Collection<WorkflowTemplate> workflowTemplates = workflowTemplateService.getByCategoryId(templateCategory.getId());
        final boolean isEmailSyncEnabled = emailSyncManager.isEmailSyncEnabled(request.orgId(), request.userId());
        final JsonArray jsonArray = Json.array();

        if (!workflowTemplates.isEmpty()) {

            for (final WorkflowTemplate workflowTemplate : workflowTemplates) {
                jsonArray.put(convertTemplateToJson(workflowTemplate, isEmailSyncEnabled, response));
            }
        }
        response.put(WorkflowAttributes.TOTAL_RECORDS, jsonArray.size());

        return jsonArray;
    }

    /**
     * Converts the {@link WorkflowTemplate} into json.
     *
     * @param workflowTemplate Refers the {@link WorkflowTemplate}.
     * @param emailSyncEnabled Refers the user has synced their email or not.
     * @param response         Refers the {@link CrmResponse} to be sent.
     * @return The {@link JsonObject} which holds the template details.
     */
    private JsonObject convertTemplateToJson(final WorkflowTemplate workflowTemplate, final boolean emailSyncEnabled, final CrmResponse response) {
        final JsonObject jsonObject = Json.object();
        final TemplateCategory category = workflowTemplate.category();

        jsonObject.put(WorkflowAttributes.ID, workflowTemplate.id());
        jsonObject.put(WorkflowAttributes.NAME, response.resolveI18nPlaceholder(workflowTemplate.name()));
        jsonObject.put(WorkflowAttributes.DESCRIPTION, response.resolveI18nPlaceholder(workflowTemplate.description()));
        jsonObject.put(WorkflowAttributes.CATEGORY_ID, category.getId());
        jsonObject.put(WorkflowAttributes.ACTION_COUNT, workflowTemplate.actions().size());
        jsonObject.put(WorkflowAttributes.IS_ACTION_ENABLED, TemplateCategory.EMAIL == category ? emailSyncEnabled : true);

        return jsonObject;
    }

    /**
     * Converts the {@link WorkflowTemplate} into json.
     *
     * @param orgId                   Refers the organization id.
     * @param workflowTemplate        Refers the {@link WorkflowTemplate}.
     * @param i18NResolutionAdapter Refers the {@link I18nResolutionAdapter} for resolve the i18n data.
     * @return The {@link JsonObject} which holds the template details.
     */
    private JsonObject convertTemplateToJson(final long orgId, final WorkflowTemplate workflowTemplate, final I18nResolutionAdapter i18NResolutionAdapter) {
        final JsonObject jsonObject = Json.object();
        final JsonObject triggerJson = Json.object();
        final TriggerSource triggerSource = workflowTemplate.triggerSource();
        final TriggerEvent triggerEvent = workflowTemplate.triggerEvent();
        final short triggerSourceId = triggerSource.getId();
        final Collection<WorkflowAction> workflowActions = workflowTemplate.actions();

        jsonObject.put(WorkflowAttributes.ID, workflowTemplate.id());
        jsonObject.put(WorkflowAttributes.NAME, i18NResolutionAdapter.get(workflowTemplate.name()));
        jsonObject.put(WorkflowAttributes.DESCRIPTION, i18NResolutionAdapter.get(workflowTemplate.description()));

        triggerJson.put(WorkflowAttributes.SOURCE, triggerSourceId);
        triggerJson.put(WorkflowAttributes.EVENT, triggerEvent.getId());

        jsonObject.put(WorkflowAttributes.TRIGGER, triggerJson);
        jsonObject.put(WorkflowAttributes.CONDITIONS, getCriteriaSetJson(orgId, workflowTemplate.criteriaSets(), i18NResolutionAdapter));
        jsonObject.put(WorkflowAttributes.ACTIONS, getActionJson(orgId, triggerSourceId, workflowActions, i18NResolutionAdapter));

        return jsonObject;
    }

    /**
     * Converts the criteria sets into the {@link JsonArray}.
     *
     * @param orgId                   Refers the organization id.
     * @param workflowCriteriaSets    Refers the collection of {@link WorkflowCriteriaSet}.
     * @param i18NResolutionAdapter Refers the {@link I18nResolutionAdapter} for resolve the i18n data.
     * @return The criteria set {@link JsonArray}.
     */
    private JsonArray getCriteriaSetJson(final long orgId, final Collection<WorkflowCriteriaSet> workflowCriteriaSets,
                                         final I18nResolutionAdapter i18NResolutionAdapter) {
        final JsonArray conditions = Json.array();

        for (final WorkflowCriteriaSet workflowCriteriaSet : workflowCriteriaSets) {
            final JsonObject criteriaSet = Json.object();
            final CriteriaConnector connector = workflowCriteriaSet.getConnector();
            final Collection<WorkflowCriteria> criteriaList = workflowCriteriaSet.getCriteriaList()
                    .stream().sorted(Comparator.comparing(WorkflowCriteria::getOrder)).toList();
            final JsonArray criteriaArray = Json.array();

            for (final WorkflowCriteria workflowCriteria : criteriaList) {
                final JsonObject criteriaJson = Json.object();
                final JsonArray criteriaValueArray = Json.array();
                final CriteriaConnector criteriaConnector = workflowCriteria.getConnector();

                criteriaJson.put(WorkflowAttributes.FIELD, getCriteriaFieldJson(orgId, workflowCriteria.getCriteriaField(), i18NResolutionAdapter));
                criteriaJson.put(WorkflowAttributes.COMPARATOR, getComparatorJson(workflowCriteria.getComparator(), i18NResolutionAdapter));

                if (Objects.nonNull(criteriaConnector)) {
                    criteriaJson.put(WorkflowAttributes.CONNECTOR, criteriaConnector.getId());
                }

                if (workflowCriteria.getComparator().hasValue()) {

                    for (final WorkflowCriteriaValue workflowCriteriaValue : workflowCriteria.getCriteriaValues()) {
                        templateCriteriaResolver.resolveCriteriaDetails(orgId, workflowCriteria.getCriteriaField(), workflowCriteriaValue, i18NResolutionAdapter);

                        final JsonObject criteriaValueJson = Json.object();
                        final Long valueId = workflowCriteriaValue.getValueId();

                        if (Objects.nonNull(valueId)) {
                            final JsonObject valueJson = Json.object();

                            criteriaValueJson.put(WorkflowAttributes.ID, workflowCriteriaValue.getValueId());
                            criteriaValueJson.put(WorkflowAttributes.NAME, workflowCriteriaValue.getValue());

                            valueJson.put(WorkflowAttributes.VALUE, criteriaValueJson);
                            criteriaValueArray.put(valueJson);
                        } else {
                            criteriaValueJson.put(WorkflowAttributes.VALUE, workflowCriteriaValue.getValue());
                            criteriaValueArray.put(criteriaValueJson);
                        }
                    }
                    criteriaJson.put(WorkflowAttributes.VALUES, criteriaValueArray);
                }
                criteriaArray.put(criteriaJson);
            }

            if (Objects.nonNull(connector)) {
                criteriaSet.put(WorkflowAttributes.CONNECTOR, connector.getId());
            }
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

        criteriaFieldJson.put(WorkflowAttributes.NAME, i18NResolutionAdapter.get(Objects.equals(FieldGroup.SOURCE_RELATED_FIELD,
                workflowCriteriaField.getFieldGroup()) ? fieldService.getField(orgId, fieldId).i18nName()
                : workflowFieldService.get(fieldId).getName()));
        criteriaFieldJson.put(WorkflowAttributes.ID, fieldId);

        return criteriaFieldJson;
    }

    /**
     * Gets the {@link JsonArray} of {@link WorkflowAction}.
     *
     * @param orgId                   Refers the organization id
     * @param triggerSourceId         Represents the trigger source id
     * @param workflowActions         Refers the collection of {@link WorkflowAction}
     * @param i18NResolutionAdapter Refers the {@link I18nResolutionAdapter} for resolve the i18n data.
     * @return The {@link JsonArray} filled with {@link WorkflowAction} details.
     */
    private JsonArray getActionJson(final long orgId, final short triggerSourceId, final Collection<WorkflowAction> workflowActions,
                                    final I18nResolutionAdapter i18NResolutionAdapter) {
        final JsonArray actionArray = Json.array();

        for (final WorkflowAction workflowAction : workflowActions) {
            final JsonObject workflowActionJson = Json.object();
            final JsonObject actionSourceJson = Json.object();
            final JsonObject actionEventJson = Json.object();
            final TriggerActionSource triggerActionSource = workflowAction.getTriggerActionSource();
            final TriggerActionEvent triggerActionEvent = workflowAction.getTriggerActionEvent();

            workflowActionJson.put(WorkflowAttributes.ID, workflowAction.getId());

            actionSourceJson.put(WorkflowAttributes.ID, triggerActionSource.id());
            actionSourceJson.put(WorkflowAttributes.NAME, i18NResolutionAdapter.get(triggerActionSource.getDisplayName()));
            actionEventJson.put(WorkflowAttributes.ID, triggerActionEvent.id());
            actionEventJson.put(WorkflowAttributes.NAME, i18NResolutionAdapter.get(triggerActionEvent.getDisplayName()));
            workflowActionJson.put(WorkflowAttributes.SOURCE, actionSourceJson);
            workflowActionJson.put(WorkflowAttributes.EVENT, actionEventJson);

            templateActionResolver.resolveActionJson(orgId, triggerSourceId, workflowActionJson, workflowAction, i18NResolutionAdapter);

            actionArray.put(workflowActionJson);
        }
        return actionArray;
    }

    /**
     * Gets the json object of {@link WorkflowTemplate}.
     *
     * @param orgId   Refers the organization id.
     * @param request Refers the {@link CrmRequest} data.
     * @param data    Refers the request json data.
     * @return The {@link JsonObject} of template details.
     */
    private JsonObject getTemplateV2(final long orgId, final CrmRequest request, final JsonObject data) {
        final WorkflowTemplate workflowTemplate = workflowTemplateService.get(data.getLong(WorkflowAttributes.ID));

        if (Objects.isNull(workflowTemplate)) {
            throw ErrorCode.get(WorkflowErrorCodes.TEMPLATE_NOT_FOUND, "i18n.workflow.template.not.found");
        }
        return convertTemplateToJsonV2(orgId, workflowTemplate, request.getI18NCRMResolutionAdapter());
    }

    /**
     * Converts the {@link WorkflowTemplate} into json.
     *
     * @param orgId                 Refers the organization id.
     * @param workflowTemplate      Refers the {@link WorkflowTemplate}.
     * @param i18NResolutionAdapter Refers the {@link I18nResolutionAdapter} for resolve the i18n data.
     * @return The {@link JsonObject} which holds the template details.
     */
    private JsonObject convertTemplateToJsonV2(final long orgId, final WorkflowTemplate workflowTemplate, final I18nResolutionAdapter i18NResolutionAdapter) {
        final JsonObject jsonObject = Json.object();
        final JsonObject nodeJson = Json.object();
        final JsonObject triggerJson = Json.object();
        final JsonObject conditionJson = Json.object();
        final TriggerSource triggerSource = workflowTemplate.triggerSource();
        final TriggerEvent triggerEvent = workflowTemplate.triggerEvent();
        final short triggerSourceId = triggerSource.getId();

        jsonObject.put(WorkflowAttributesV2.ID, workflowTemplate.id());
        jsonObject.put(WorkflowAttributesV2.NAME, i18NResolutionAdapter.get(workflowTemplate.name()));
        jsonObject.put(WorkflowAttributesV2.DESCRIPTION, i18NResolutionAdapter.get(workflowTemplate.description()));
        jsonObject.put(WorkflowAttributesV2.INITIAL_NODE, NumericalConstant.ONE);

        triggerJson.put(WorkflowAttributesV2.SOURCE, triggerSourceId);
        triggerJson.put(WorkflowAttributesV2.EVENT, triggerEvent.getId());
        triggerJson.put(WorkflowAttributesV2.NEXT_NODE, NumericalConstant.TWO);
        triggerJson.put(WorkflowAttributesV2.NODE_TYPE, NodeType.TRIGGER.getId());
        triggerJson.put(WorkflowAttributesV2.TRIGGER_SCOPE, TriggerScope.EVERYONE.getId());
        nodeJson.put(String.valueOf(NumericalConstant.ONE), triggerJson);

        conditionJson.put(WorkflowAttributes.CONDITIONS, getCriteriaSetJson(orgId, workflowTemplate.criteriaSets(), i18NResolutionAdapter));
        conditionJson.put(WorkflowAttributesV2.NEXT_NODE, NumericalConstant.THREE);
        conditionJson.put(WorkflowAttributesV2.NODE_TYPE, NodeType.INSTANT_CONDITION.getId());
        conditionJson.put(WorkflowAttributesV2.EVENT_INFO, Json.object()
                .put(WorkflowAttributesV2.TYPE, EventContextType.TRIGGER.name())
                .put(WorkflowAttributesV2.NODE, NumericalConstant.ONE));
        nodeJson.put(String.valueOf(NumericalConstant.TWO), conditionJson);

        workflowTemplate.actions().stream().findFirst().ifPresent(workflowAction -> {
            final JsonObject actionJson = getActionJson(orgId, triggerSourceId, workflowAction, i18NResolutionAdapter);

            actionJson.put(WorkflowAttributesV2.NODE_TYPE, NodeType.ACTION.getId());
            nodeJson.put(String.valueOf(NumericalConstant.THREE), actionJson);
        });

        jsonObject.put(WorkflowAttributesV2.NODES, nodeJson);
        return jsonObject;
    }

    /**
     * Gets the {@link JsonArray} of {@link WorkflowAction}.
     *
     * @param orgId                 Refers the organization id
     * @param triggerSourceId       Represents the trigger source id
     * @param workflowAction        Refers the {@link WorkflowAction}
     * @param i18NResolutionAdapter Refers the {@link I18nResolutionAdapter} for resolve the i18n data.
     * @return The {@link JsonArray} filled with {@link WorkflowAction} details.
     */
    private JsonObject getActionJson(final long orgId, final short triggerSourceId, final WorkflowAction workflowAction,
                                     final I18nResolutionAdapter i18NResolutionAdapter) {
        final JsonObject workflowActionJson = Json.object();
        final JsonObject actionSourceJson = Json.object();
        final JsonObject actionEventJson = Json.object();
        final TriggerActionSource triggerActionSource = workflowAction.getTriggerActionSource();
        final TriggerActionEvent triggerActionEvent = workflowAction.getTriggerActionEvent();

        actionSourceJson.put(WorkflowAttributes.ID, triggerActionSource.id());
        actionSourceJson.put(WorkflowAttributes.NAME, i18NResolutionAdapter.get(triggerActionSource.getDisplayName()));

        actionEventJson.put(WorkflowAttributes.ID, triggerActionEvent.id());
        actionEventJson.put(WorkflowAttributes.NAME, i18NResolutionAdapter.get(triggerActionEvent.getDisplayName()));

        workflowActionJson.put(WorkflowAttributes.ID, workflowAction.getId());
        workflowActionJson.put(WorkflowAttributes.SOURCE, actionSourceJson);
        workflowActionJson.put(WorkflowAttributes.EVENT, actionEventJson);

        templateActionResolver.resolveActionJson(orgId, triggerSourceId, workflowActionJson, workflowAction, i18NResolutionAdapter);

        return workflowActionJson;
    }
}
