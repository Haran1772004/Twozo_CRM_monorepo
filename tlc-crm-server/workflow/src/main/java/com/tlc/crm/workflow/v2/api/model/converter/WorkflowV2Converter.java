package com.tlc.crm.workflow.v2.api.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.entity.builder.EntityConverter;

import com.tlc.crm.fields.models.StoreType;

import com.tlc.crm.workflow.api.model.CriteriaConnector;
import com.tlc.crm.workflow.api.model.FieldGroup;
import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.TriggerSource.TriggerEvent;
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
import com.tlc.crm.workflow.resolver.internal.WorkflowCriteriaResolver;
import com.tlc.crm.workflow.v2.api.manager.WorkflowV2Manager;
import com.tlc.crm.workflow.v2.api.model.*;
import com.tlc.crm.workflow.v2.field.WorkflowAttributesV2;
import com.tlc.crm.workflow.v2.resolver.WorkflowActionResolverV2;

import com.tlc.i18n.I18nResolutionAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Converts between JSON and {@link WorkflowV2} models.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class WorkflowV2Converter extends EntityConverter<WorkflowV2> {

    private final WorkflowV2Manager workflowV2Manager;
    private final TriggerActionSourceService triggerActionSourceService;
    private final TriggerActionEventService triggerActionEventService;
    private final WorkflowComparatorService workflowComparatorService;
    private final WorkflowFieldService workflowFieldService;
    private final UserService crmUserService;
    private final WorkflowCriteriaResolver workflowCriteriaResolver;
    private final WorkflowActionResolverV2 workflowActionResolver;

    private WorkflowV2Converter() {
        this.workflowV2Manager = WorkflowV2Manager.getInstance();
        this.triggerActionSourceService = TriggerActionSourceService.getInstance();
        this.triggerActionEventService = TriggerActionEventService.getInstance();
        this.workflowComparatorService = WorkflowComparatorService.getInstance();
        this.workflowFieldService = WorkflowFieldService.getInstance();
        this.crmUserService = UserService.getInstance();
        this.workflowCriteriaResolver = WorkflowCriteriaResolver.getInstance();
        this.workflowActionResolver = WorkflowActionResolverV2.getInstance();
    }

    private static class Instance {
        private static final WorkflowV2Converter INSTANCE = new WorkflowV2Converter();
    }

    public static WorkflowV2Converter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}.
     *
     * @param converterConfig the converter config
     * @param jsonObject      the JSON object
     * @return the {@link WorkflowV2} model
     */
    @Override
    public WorkflowV2 convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        return converterConfig.isPartialData() ? constructByPartialData(converterConfig, jsonObject)
                : constructWorkflow(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}.
     *
     * @param converterConfig the converter config
     * @param workflow        the {@link WorkflowV2} model
     * @return the JSON object
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final WorkflowV2 workflow) {
        final JsonObject result = Json.object();
        final WorkflowVersion workflowVersion = workflow.getWorkflowVersion();
        final I18nResolutionAdapter i18NResolutionAdapter = converterConfig.getI18nPlaceholderResolver();

        result.put(WorkflowAttributesV2.ID, workflow.id());
        result.put(WorkflowAttributesV2.NAME, workflow.getName());
        result.put(WorkflowAttributesV2.DESCRIPTION, workflow.getDescription());
        result.put(WorkflowAttributesV2.IS_ACTIVE, workflow.isActive());
        result.put(WorkflowAttributesV2.INITIAL_NODE, workflowVersion.getInitialNodeId());
        result.put(WorkflowAttributesV2.NODES, convertNodesToJson(workflow, i18NResolutionAdapter));

        return result;
    }

    /**
     * Constructs a {@link WorkflowV2} model from a JSON object.
     *
     * @param converterConfig the converter config
     * @param jsonObject      the JSON object
     * @return the {@link WorkflowV2} model
     */
    private WorkflowV2 constructByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final WorkflowV2 workflow = workflowV2Manager.get(converterConfig.orgId(), jsonObject.getLong(WorkflowAttributesV2.ID));

        setJsonDataToModel(converterConfig, workflow, jsonObject);
        return workflow;
    }

    /**
     * Constructs a {@link WorkflowV2} model from a JSON object.
     *
     * @param converterConfig the converter config
     * @param jsonObject      the JSON object
     * @return the {@link WorkflowV2} model
     */
    private WorkflowV2 constructWorkflow(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final WorkflowV2 workflow = new WorkflowV2();

        if (jsonObject.containsKey(WorkflowAttributesV2.ID)) {
            workflow.setId(jsonObject.getLong(WorkflowAttributesV2.ID));
        }
        setJsonDataToModel(converterConfig, workflow, jsonObject);
        return workflow;
    }

    /**
     * Sets the JSON data to the {@link WorkflowV2} model.
     *
     * @param converterConfig the converter config
     * @param workflow        the {@link WorkflowV2} model
     * @param jsonObject      the JSON object
     */
    private void setJsonDataToModel(final ConverterConfig converterConfig, final WorkflowV2 workflow, final JsonObject jsonObject) {
        final Long orgId = converterConfig.orgId();

        workflow.setOrgId(orgId);
        workflow.setOwnerId(converterConfig.ownerId());

        if (jsonObject.containsKey(WorkflowAttributesV2.NAME)) {
            workflow.setName(jsonObject.getString(WorkflowAttributesV2.NAME));
        }

        if (jsonObject.containsKey(WorkflowAttributesV2.DESCRIPTION)) {
            workflow.setDescription(jsonObject.getString(WorkflowAttributesV2.DESCRIPTION));
        }

        if (jsonObject.containsKey(WorkflowAttributesV2.IS_ACTIVE)) {
            workflow.setActive(jsonObject.getBoolean(WorkflowAttributesV2.IS_ACTIVE));
        }

        if (jsonObject.containsKey(WorkflowAttributesV2.CANCEL_PENDING_EXECUTION)) {
            workflow.setCancelPendingExecution(jsonObject.getBoolean(WorkflowAttributesV2.CANCEL_PENDING_EXECUTION));
        }

        if (jsonObject.containsKey(WorkflowAttributes.TEMPLATE_ID)) {
            workflow.setTemplateId(jsonObject.getLong(WorkflowAttributes.TEMPLATE_ID));
        }

        if (jsonObject.containsKey(WorkflowAttributesV2.INITIAL_NODE)) {
            final WorkflowVersion workflowVersion = new WorkflowVersion();

            workflowVersion.setInitialNodeId(jsonObject.getShort(WorkflowAttributesV2.INITIAL_NODE));
            workflowVersion.setVersionStatus(VersionStatus.PUBLISHED);
            workflowVersion.setOrgId(orgId);
            workflow.setWorkflowVersion(workflowVersion);
        }

        if (jsonObject.containsKey(WorkflowAttributesV2.NODES)) {
            workflow.setNodes(parseNodes(orgId, workflow, jsonObject.getJsonObject(WorkflowAttributesV2.NODES)));
            workflow.setGraphUpdate(true);
        }
    }

    /**
     * Parses the nodes from a JSON object.
     *
     * @param orgId      the organization id
     * @param workflowV2 the {@link WorkflowV2} model
     * @param nodesJson  the JSON object
     * @return the map of nodes
     */
    private Map<String, WorkflowNode> parseNodes(final Long orgId, final WorkflowV2 workflowV2, final JsonObject nodesJson) {
        final Map<String, WorkflowNode> nodes = WorkflowNode.newNodeMap();

        for (final String clientKey : nodesJson.keySet().stream().sorted(Comparator.naturalOrder()).toList()) {
            final JsonObject nodeJson = nodesJson.getJsonObject(clientKey);
            final WorkflowNode node = parseNode(orgId, workflowV2, clientKey, nodeJson, nodes);

            nodes.put(clientKey, node);
        }
        return nodes;
    }

    /**
     * Parses a node from a JSON object.
     *
     * @param orgId      the organization id
     * @param workflowV2 the {@link WorkflowV2} model
     * @param clientKey  the client key
     * @param nodeJson   the JSON object
     * @param nodes      the map of nodes
     * @return the node
     */
    private WorkflowNode parseNode(final Long orgId, final WorkflowV2 workflowV2, final String clientKey,
                                   final JsonObject nodeJson, final Map<String, WorkflowNode> nodes) {
        final WorkflowNode node = new WorkflowNode();

        node.setClientKey(clientKey);
        node.setOrgId(orgId);
        node.setNodeType(NodeType.get(nodeJson.getShort(WorkflowAttributesV2.NODE_TYPE)));

        if (nodeJson.containsKey(WorkflowAttributesV2.ID)) {
            node.setId(nodeJson.getLong(WorkflowAttributesV2.ID));
        }

        if (nodeJson.containsKey(WorkflowAttributesV2.NEXT_NODE)) {
            final WorkflowNodeEdge nextEdge = new WorkflowNodeEdge();

            nextEdge.setEdgeType(EdgeType.NEXT);
            nextEdge.setToNodeId(nodeJson.getLong(WorkflowAttributesV2.NEXT_NODE));
            node.setNextEdge(nextEdge);
        }

        if (nodeJson.containsKey(WorkflowAttributesV2.FALLBACK_NODE)) {
            final WorkflowNodeEdge fallbackEdge = new WorkflowNodeEdge();

            fallbackEdge.setEdgeType(EdgeType.FALLBACK);
            fallbackEdge.setToNodeId(nodeJson.getLong(WorkflowAttributesV2.FALLBACK_NODE));
            node.setFallbackEdge(fallbackEdge);
        }

        node.setConfiguration(buildNodeConfiguration(orgId, workflowV2, node, nodeJson, nodes));
        return node;
    }

    /**
     * Builds the node configuration for a workflow node.
     *
     * @param orgId    the organization id
     * @param workflow the {@link WorkflowV2} model
     * @param node     the workflow node
     * @param nodeJson the JSON object
     * @param nodes    the map of nodes
     * @return the node configuration
     */
    private NodeConfiguration buildNodeConfiguration(final Long orgId, final WorkflowV2 workflow, final WorkflowNode node,
                                                     final JsonObject nodeJson, final Map<String, WorkflowNode> nodes) {
        return switch (node.getNodeType()) {
            case TRIGGER -> buildTriggerConfiguration(nodeJson, workflow);
            case INSTANT_CONDITION -> buildConditionConfiguration(orgId, nodeJson, nodes);
            case IF_ELSE_CONDITION -> buildIfElseConfiguration(orgId, nodeJson, nodes);
            case ACTION -> parseAction(orgId, nodeJson);
            case DELAY -> buildDelayConfiguration(orgId, nodeJson);
            case WAIT_FOR_CONDITION -> buildWaitConfiguration(orgId, nodeJson, nodes);
        };
    }

    /**
     * Builds the trigger configuration for a workflow node.
     *
     * @param nodeJson the JSON object
     * @param workflow the {@link WorkflowV2} model
     * @return the trigger configuration
     */
    private TriggerConfiguration buildTriggerConfiguration(final JsonObject nodeJson, final WorkflowV2 workflow) {
        final TriggerConfiguration triggerConfiguration = new TriggerConfiguration();

        if (nodeJson.containsKey(WorkflowAttributesV2.SOURCE)) {
            final short triggerSource = nodeJson.getShort(WorkflowAttributesV2.SOURCE);

            triggerConfiguration.setTriggerSource(TriggerSource.get(triggerSource));
            workflow.setTriggerSourceId(triggerSource);
        }

        if (nodeJson.containsKey(WorkflowAttributesV2.EVENT)) {
            triggerConfiguration.setTriggerEvent(TriggerEvent.get(nodeJson.getShort(WorkflowAttributesV2.EVENT)));
        }

        if (nodeJson.containsKey(WorkflowAttributesV2.TRIGGER_SCOPE)) {
            triggerConfiguration.setTriggerScope(TriggerScope.get(nodeJson.getShort(WorkflowAttributesV2.TRIGGER_SCOPE)));
        }

        if (nodeJson.containsKey(WorkflowAttributesV2.USER_IDS)) {
            final JsonArray jsonArray = nodeJson.getJsonArray(WorkflowAttributesV2.USER_IDS);
            final Collection<Long> userIds = new HashSet<>(jsonArray.size());

            jsonArray.forEach(value -> userIds.add((Long) value));
            triggerConfiguration.setUserIds(userIds);
        }

        return triggerConfiguration;
    }

    /**
     * Builds the condition configuration for a workflow node.
     *
     * @param orgId    the organization id
     * @param nodeJson the JSON object
     * @param nodes    the map of nodes
     * @return the condition configuration
     */
    private ConditionConfiguration buildConditionConfiguration(final Long orgId, final JsonObject nodeJson, final Map<String, WorkflowNode> nodes) {
        final ConditionConfiguration conditionConfiguration = new ConditionConfiguration();

        if (nodeJson.containsKey(WorkflowAttributesV2.EVENT_INFO)) {
            conditionConfiguration.setNodeEvent(toNodeEvent(orgId, nodeJson.getJsonObject(WorkflowAttributesV2.EVENT_INFO)));
        }

        if (nodeJson.containsKey(WorkflowAttributesV2.CONDITIONS)) {
            conditionConfiguration.setCriteriaSets(parseCriteriaSets(nodeJson, nodeJson.getJsonArray(WorkflowAttributesV2.CONDITIONS), nodes));
        }

        return conditionConfiguration;
    }

    /**
     * Builds the if/else configuration for a workflow node.
     *
     * @param orgId    the organization id
     * @param nodeJson the JSON object
     * @param nodes    the map of nodes
     * @return the if/else configuration
     */
    private IfElseConfiguration buildIfElseConfiguration(final Long orgId, final JsonObject nodeJson, final Map<String, WorkflowNode> nodes) {
        final IfElseConfiguration ifElseConfiguration = new IfElseConfiguration();
        final ConditionConfiguration conditionConfiguration = new ConditionConfiguration();

        if (nodeJson.containsKey(WorkflowAttributesV2.CONDITIONS)) {
            conditionConfiguration.setCriteriaSets(parseCriteriaSets(nodeJson, nodeJson.getJsonArray(WorkflowAttributesV2.CONDITIONS), nodes));
        }

        if (nodeJson.containsKey(WorkflowAttributesV2.EVENT_INFO)) {
            ifElseConfiguration.setNodeEvent(toNodeEvent(orgId, nodeJson.getJsonObject(WorkflowAttributesV2.EVENT_INFO)));
        }

        ifElseConfiguration.setCondition(conditionConfiguration);
        return ifElseConfiguration;
    }

    /**
     * Builds the delay configuration for a workflow node.
     *
     * @param orgId    the organization id
     * @param nodeJson the JSON object
     * @return the delay configuration
     */
    private DelayConfiguration buildDelayConfiguration(final Long orgId, final JsonObject nodeJson) {
        final DelayConfiguration delayConfiguration = new DelayConfiguration();

        if (nodeJson.containsKey(WorkflowAttributesV2.SPAN_CONFIGURATION)) {
            delayConfiguration.setSpan(parseSpanConfiguration(orgId, nodeJson.getJsonObject(WorkflowAttributesV2.SPAN_CONFIGURATION)));
        }
        return delayConfiguration;
    }

    /**
     * Builds the wait-for-condition configuration for a workflow node.
     *
     * @param orgId    the organization id
     * @param nodeJson the JSON object
     * @param nodes    the map of nodes
     * @return the wait-for-condition configuration
     */
    private WaitForConditionConfiguration buildWaitConfiguration(final Long orgId, final JsonObject nodeJson, final Map<String, WorkflowNode> nodes) {
        final WaitForConditionConfiguration waitConfiguration = new WaitForConditionConfiguration();
        final ConditionConfiguration conditionConfiguration = new ConditionConfiguration();

        if (nodeJson.containsKey(WorkflowAttributesV2.CONDITIONS)) {
            conditionConfiguration.setCriteriaSets(parseCriteriaSets(nodeJson, nodeJson.getJsonArray(WorkflowAttributesV2.CONDITIONS), nodes));
        }

        if (nodeJson.containsKey(WorkflowAttributesV2.EVENT_INFO)) {
            waitConfiguration.setNodeEvent(toNodeEvent(orgId, nodeJson.getJsonObject(WorkflowAttributesV2.EVENT_INFO)));
        }

        if (nodeJson.containsKey(WorkflowAttributesV2.SPAN_CONFIGURATION)) {
            waitConfiguration.setSpan(parseSpanConfiguration(orgId, nodeJson.getJsonObject(WorkflowAttributesV2.SPAN_CONFIGURATION)));
        }

        waitConfiguration.setCondition(conditionConfiguration);
        return waitConfiguration;
    }

    /**
     * Parses the span configuration for a workflow node.
     *
     * @param orgId    the organization id
     * @param spanJson the JSON object
     * @return the span configuration
     */
    private SpanConfiguration parseSpanConfiguration(final Long orgId, final JsonObject spanJson) {
        final SpanConfiguration spanConfiguration = new SpanConfiguration();
        final SpanType spanType = SpanType.get(spanJson.getShort(WorkflowAttributesV2.TYPE));

        spanConfiguration.setOrgId(orgId);
        spanConfiguration.setType(spanType);
        spanConfiguration.setDueTime(spanJson.optString(WorkflowAttributesV2.DUE_TIME, null));
        spanConfiguration.setSkipWeekends(spanJson.getBoolean(WorkflowAttributesV2.SKIP_WEEKENDS));

        switch (spanType) {
            case PREDEFINED ->
                    spanConfiguration.setPredefinedSpanUnit(PredefinedSpanUnit.get(spanJson.getShort(WorkflowAttributesV2.UNIT)));
            case CUSTOM -> {
                spanConfiguration.setAmount(spanJson.getInt(WorkflowAttributesV2.AMOUNT));
                spanConfiguration.setCustomSpanUnit(CustomSpanUnit.get(spanJson.getShort(WorkflowAttributesV2.UNIT)));
            }
        }
        return spanConfiguration;
    }

    /**
     * Converts a node event to a JSON object.
     *
     * @param orgId     the organization id
     * @param eventInfo the event info
     * @return the node event
     */
    private NodeEvent toNodeEvent(final Long orgId, final JsonObject eventInfo) {
        final NodeEvent nodeEvent = new NodeEvent();

        nodeEvent.setContextType(EventContextType.fromName(eventInfo.getString(WorkflowAttributesV2.TYPE)));
        nodeEvent.setOrgId(orgId);

        if (eventInfo.containsKey(WorkflowAttributesV2.NODE)) {
            nodeEvent.setSourceNodeClientKey(eventInfo.getString(WorkflowAttributesV2.NODE));
        }
        return nodeEvent;
    }

    /**
     * Parses the action configuration for a workflow node.
     *
     * @param orgId      the organization id
     * @param actionJson the JSON object
     * @return the action configuration
     */
    private ActionConfiguration parseAction(final Long orgId, final JsonObject actionJson) {
        final ActionConfiguration action = new ActionConfiguration();

        if (actionJson.containsKey(WorkflowAttributesV2.ID)) {
            action.setId(actionJson.getLong(WorkflowAttributesV2.ID));
        }
        action.setOrgId(orgId);
        action.setTriggerActionSource(triggerActionSourceService.get(actionJson.getLong(WorkflowAttributesV2.SOURCE)));
        action.setTriggerActionEvent(triggerActionEventService.get(actionJson.getLong(WorkflowAttributesV2.EVENT)));
        action.setFieldMapping(actionJson.getJsonArray(WorkflowAttributesV2.FIELD_MAPPING).toString());

        return action;
    }

    /**
     * Parses the criteria sets for a workflow node.
     *
     * @param nodeJson       the JSON object
     * @param conditionArray the condition array
     * @param nodes          the map of nodes
     * @return the criteria sets
     */
    private Collection<WorkflowCriteriaSet> parseCriteriaSets(final JsonObject nodeJson, final JsonArray conditionArray, final Map<String, WorkflowNode> nodes) {
        final List<WorkflowCriteriaSet> criteriaSets = new ArrayList<>();

        for (int index = 0; index < conditionArray.size(); index++) {
            final JsonObject criteriaSetJson = conditionArray.optJsonObject(index);
            final WorkflowCriteriaSet criteriaSet = new WorkflowCriteriaSet();

            if (criteriaSetJson.containsKey(WorkflowAttributesV2.ID)) {
                criteriaSet.setId(criteriaSetJson.getLong(WorkflowAttributesV2.ID));
            }

            if (criteriaSetJson.containsKey(WorkflowAttributesV2.CONNECTOR)) {
                criteriaSet.setConnector(CriteriaConnector.get(criteriaSetJson.getShort(WorkflowAttributesV2.CONNECTOR)));
            }

            if (criteriaSetJson.containsKey(WorkflowAttributesV2.CRITERIA)) {
                criteriaSet.setCriteriaList(parseCriteriaList(nodeJson, criteriaSetJson.getJsonArray(WorkflowAttributesV2.CRITERIA), nodes));
            }
            criteriaSets.add(criteriaSet);
        }
        return criteriaSets;
    }

    /**
     * Parses the criteria list for a workflow node.
     *
     * @param nodeJson      the JSON object
     * @param criteriaArray the criteria array
     * @param nodes         the map of nodes
     * @return the criteria list
     */
    private Collection<WorkflowCriteria> parseCriteriaList(final JsonObject nodeJson, final JsonArray criteriaArray, final Map<String, WorkflowNode> nodes) {
        final List<WorkflowCriteria> criteriaList = new ArrayList<>();

        for (int index = 0; index < criteriaArray.size(); index++) {
            final JsonObject criteriaJson = criteriaArray.optJsonObject(index);
            final WorkflowCriteria criteria = new WorkflowCriteria();

            if (criteriaJson.containsKey(WorkflowAttributesV2.ID)) {
                criteria.setId(criteriaJson.getLong(WorkflowAttributesV2.ID));
            }
            if (criteriaJson.containsKey(WorkflowAttributesV2.CONNECTOR)) {
                criteria.setConnector(CriteriaConnector.get(criteriaJson.getShort(WorkflowAttributesV2.CONNECTOR)));
            }
            criteria.setComparator(workflowComparatorService.get(criteriaJson.getLong(WorkflowAttributesV2.COMPARATOR)));

            if (criteriaJson.containsKey(WorkflowAttributesV2.FIELD_ID)) {
                criteria.setCriteriaField(parseCriteriaField(nodeJson, criteriaJson.getLong(WorkflowAttributesV2.FIELD_ID), nodes));
            }

            if (criteriaJson.containsKey(WorkflowAttributesV2.VALUES)) {
                criteria.setCriteriaValues(parseCriteriaValues(criteriaJson.getJsonArray(WorkflowAttributesV2.VALUES)));
            }
            criteriaList.add(criteria);
        }
        return criteriaList;
    }

    /**
     * Parses the criteria field for a workflow node.
     *
     * @param nodeJson the JSON object
     * @param fieldId  the field id
     * @param nodes    the map of nodes
     * @return the criteria field
     */
    private WorkflowCriteriaField parseCriteriaField(final JsonObject nodeJson, final long fieldId, final Map<String, WorkflowNode> nodes) {
        final WorkflowCriteriaField field = new WorkflowCriteriaField();
        final JsonObject eventInfo = nodeJson.getJsonObject(WorkflowAttributesV2.EVENT_INFO);
        final EventContextType eventContextType = EventContextType.fromName(eventInfo.getString(WorkflowAttributesV2.TYPE));
        final String string = eventInfo.getString(WorkflowAttributesV2.NODE);

        field.setFieldId(fieldId);

        switch (eventContextType) {
            case TRIGGER -> {
                final TriggerConfiguration triggerConfiguration = WorkflowNodeSupport.triggerConfiguration(nodes.get(string));

                field.setFieldGroup(Objects.equals(TriggerSource.ACTIVITY, triggerConfiguration.getTriggerSource())
                        ? FieldGroup.WORKFLOW_RELATED_FIELD : FieldGroup.SOURCE_RELATED_FIELD);
            }
            case ACTION -> {
                final ActionConfiguration actionConfiguration = WorkflowNodeSupport.actionConfiguration(nodes.get(string));

                field.setFieldGroup(actionConfiguration.getTriggerActionSource().isCommonActionSource()
                        ? FieldGroup.SOURCE_RELATED_FIELD : FieldGroup.WORKFLOW_RELATED_FIELD);
            }
        }

        return field;
    }

    /**
     * Parses the criteria values for a workflow node.
     *
     * @param valuesArray the values array
     * @return the criteria values
     */
    private Collection<WorkflowCriteriaValue> parseCriteriaValues(final JsonArray valuesArray) {
        final List<WorkflowCriteriaValue> values = new ArrayList<>();

        for (int index = 0; index < valuesArray.size(); index++) {
            final JsonObject valueJson = valuesArray.optJsonObject(index);
            final WorkflowCriteriaValue criteriaValue = new WorkflowCriteriaValue();

            if (valueJson.containsKey(WorkflowAttributesV2.ID)) {
                criteriaValue.setId(valueJson.getLong(WorkflowAttributesV2.ID));
            }

            if (valueJson.containsKey(WorkflowAttributes.VALUE)) {
                final Object value = valueJson.get(WorkflowAttributes.VALUE);

                if (value instanceof final Map<?, ?> valueJsonMap) {
                    final JsonObject valueJson1 = Json.object((Map<String, ?>) valueJsonMap);

                    if (valueJson1.containsKey(WorkflowAttributes.ID)) {
                        criteriaValue.setValueId(valueJson1.getLong(WorkflowAttributes.ID));
                    }

                    if (valueJson1.containsKey(WorkflowAttributes.NAME)) {
                        criteriaValue.setValue(valueJson1.get(WorkflowAttributes.NAME));
                    }
                } else {
                    criteriaValue.setValue(value);
                }
            }
            values.add(criteriaValue);
        }
        return values;
    }

    /**
     * Converts the nodes to a JSON object.
     *
     * @param workflow              the workflow
     * @param i18NResolutionAdapter the i18n resolution adapter
     * @return the JSON object
     */
    private JsonObject convertNodesToJson(final WorkflowV2 workflow, final I18nResolutionAdapter i18NResolutionAdapter) {
        final JsonObject nodesJson = Json.object();
        final Long orgId = workflow.orgId();
        final Map<String, WorkflowNode> nodes = workflow.getNodes();
        final Map<Long, String> idToClientKey = workflow.getIdToClientKey();

        for (final Map.Entry<String, WorkflowNode> entry : nodes.entrySet()) {
            nodesJson.put(entry.getKey(), convertNodeToJson(orgId, workflow, entry.getValue(), idToClientKey, i18NResolutionAdapter));
        }
        return nodesJson;
    }

    /**
     * Converts a node to a JSON object.
     *
     * @param orgId                 the organization id
     * @param workflow              the workflow
     * @param node                  the node
     * @param idToClientKey         the id to client key
     * @param i18NResolutionAdapter the i18n resolution adapter
     * @return the JSON object
     */
    private JsonObject convertNodeToJson(final Long orgId, final WorkflowV2 workflow, final WorkflowNode node,
                                         final Map<Long, String> idToClientKey, final I18nResolutionAdapter i18NResolutionAdapter) {
        final JsonObject nodeJson = Json.object();
        final NodeType nodeType = node.getNodeType();
        final WorkflowNodeEdge nextEdge = node.getNextEdge();
        final WorkflowNodeEdge fallbackEdge = node.getFallbackEdge();

        nodeJson.put(WorkflowAttributesV2.NODE_TYPE, nodeType.getId());

        if (Objects.nonNull(node.id())) {
            nodeJson.put(WorkflowAttributesV2.ID, node.id());
        }

        if (Objects.nonNull(nextEdge)) {
            nodeJson.put(WorkflowAttributesV2.NEXT_NODE, idToClientKey.get(nextEdge.getToNodeId()));
        }

        if (Objects.nonNull(fallbackEdge)) {
            nodeJson.put(WorkflowAttributesV2.FALLBACK_NODE, idToClientKey.get(fallbackEdge.getToNodeId()));
        }

        switch (nodeType) {
            case TRIGGER -> {
                final TriggerConfiguration triggerConfiguration = node.configurationAs(TriggerConfiguration.class);
                final TriggerScope triggerScope = triggerConfiguration.getTriggerScope();

                nodeJson.put(WorkflowAttributesV2.SOURCE, triggerConfiguration.getTriggerSource().getId());
                nodeJson.put(WorkflowAttributesV2.EVENT, triggerConfiguration.getTriggerEvent().getId());
                nodeJson.put(WorkflowAttributesV2.TRIGGER_SCOPE, triggerScope.getId());

                if (TriggerScope.SELECTED_USERS == triggerScope) {
                    final JsonArray users = Json.array();

                    for (final Long userId : triggerConfiguration.getUserIds()) {
                        final JsonObject jsonObject = Json.object();

                        jsonObject.put(WorkflowAttributesV2.ID, userId);
                        jsonObject.put(WorkflowAttributesV2.NAME, crmUserService.getUser(orgId, userId).getFullName());

                        users.put(jsonObject);
                    }
                    nodeJson.put(WorkflowAttributesV2.USERS, users);
                }
            }
            case INSTANT_CONDITION -> {
                final ConditionConfiguration conditionConfiguration = node.configurationAs(ConditionConfiguration.class);
                final NodeEvent nodeEvent = conditionConfiguration.getNodeEvent();

                if (Objects.nonNull(nodeEvent)) {
                    nodeJson.put(WorkflowAttributesV2.EVENT_INFO, convertNodeEventToJson(nodeEvent, idToClientKey));
                }
                nodeJson.put(WorkflowAttributesV2.CONDITIONS, getCriteriaSetJson(orgId, conditionConfiguration, i18NResolutionAdapter));
            }
            case IF_ELSE_CONDITION -> {
                final IfElseConfiguration ifElseConfiguration = node.configurationAs(IfElseConfiguration.class);
                final NodeEvent nodeEvent = ifElseConfiguration.getNodeEvent();

                if (Objects.nonNull(nodeEvent)) {
                    nodeJson.put(WorkflowAttributesV2.EVENT_INFO, convertNodeEventToJson(nodeEvent, idToClientKey));
                }
                nodeJson.put(WorkflowAttributesV2.CONDITIONS, getCriteriaSetJson(orgId, ifElseConfiguration.getCondition(), i18NResolutionAdapter));
            }
            case WAIT_FOR_CONDITION -> {
                final WaitForConditionConfiguration waitForConditionConfiguration = node.configurationAs(WaitForConditionConfiguration.class);
                final NodeEvent nodeEvent = waitForConditionConfiguration.getNodeEvent();

                if (Objects.nonNull(nodeEvent)) {
                    nodeJson.put(WorkflowAttributesV2.EVENT_INFO, convertNodeEventToJson(nodeEvent, idToClientKey));
                }
                nodeJson.put(WorkflowAttributesV2.CONDITIONS, getCriteriaSetJson(orgId, waitForConditionConfiguration.getCondition(), i18NResolutionAdapter));
                nodeJson.put(WorkflowAttributesV2.SPAN_CONFIGURATION, convertSpanConfigToJson(waitForConditionConfiguration.getSpan()));
            }
            case ACTION -> {
                final ActionConfiguration actionConfiguration = node.configurationAs(ActionConfiguration.class);

                loadActionJson(orgId, workflow.getTriggerSourceId(), actionConfiguration, i18NResolutionAdapter, nodeJson);
            }
            case DELAY -> {
                final DelayConfiguration delayConfiguration = node.configurationAs(DelayConfiguration.class);

                nodeJson.put(WorkflowAttributesV2.SPAN_CONFIGURATION, convertSpanConfigToJson(delayConfiguration.getSpan()));
            }
        }

        return nodeJson;
    }

    /**
     * Converts the span configuration to a JSON object.
     *
     * @param spanConfiguration the span configuration
     * @return the JSON object
     */
    private JsonObject convertSpanConfigToJson(final SpanConfiguration spanConfiguration) {
        final JsonObject jsonObject = Json.object();
        final SpanType type = spanConfiguration.getType();

        jsonObject.put(WorkflowAttributesV2.TYPE, type.getId());
        jsonObject.put(WorkflowAttributesV2.DUE_TIME, spanConfiguration.getDueTime());
        jsonObject.put(WorkflowAttributesV2.SKIP_WEEKENDS, spanConfiguration.getSkipWeekends());

        switch (type) {
            case PREDEFINED -> {
                final PredefinedSpanUnit predefinedSpanUnit = spanConfiguration.getPredefinedSpanUnit();

                jsonObject.put(WorkflowAttributesV2.UNIT, Json.object().put(WorkflowAttributesV2.ID, predefinedSpanUnit.getId())
                        .put(WorkflowAttributesV2.NAME, predefinedSpanUnit.getName()));
            }
            case CUSTOM -> {
                jsonObject.put(WorkflowAttributesV2.UNIT, spanConfiguration.getUnit());
                jsonObject.put(WorkflowAttributesV2.AMOUNT, spanConfiguration.getAmount());
            }
        }

        return jsonObject;
    }

    /**
     * Converts the node event to a JSON object.
     *
     * @param nodeEvent     the node event
     * @param idToClientKey the id to client key
     * @return the JSON object
     */
    private JsonObject convertNodeEventToJson(final NodeEvent nodeEvent, final Map<Long, String> idToClientKey) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(WorkflowAttributesV2.TYPE, nodeEvent.getContextType().name());
        jsonObject.put(WorkflowAttributesV2.NODE, idToClientKey.get(nodeEvent.getSourceNodeId()));

        return jsonObject;
    }

    /**
     * Loads the action JSON.
     *
     * @param orgId                 the organization id
     * @param triggerSourceId       the trigger source id
     * @param actionConfiguration   the action configuration
     * @param i18NResolutionAdapter the i18n resolution adapter
     * @param nodeJson              the node JSON
     */
    private void loadActionJson(final Long orgId, final short triggerSourceId, final ActionConfiguration actionConfiguration,
                                final I18nResolutionAdapter i18NResolutionAdapter, final JsonObject nodeJson) {
        final JsonObject actionSourceJson = Json.object();
        final JsonObject actionEventJson = Json.object();
        final long actionSourceId = actionConfiguration.getTriggerActionSource().id();
        final long actionEventId = actionConfiguration.getTriggerActionEvent().id();

        actionSourceJson.put(WorkflowAttributes.ID, actionSourceId);
        actionSourceJson.put(WorkflowAttributes.NAME, i18NResolutionAdapter.get(triggerActionSourceService.get(actionSourceId).getDisplayName()));

        actionEventJson.put(WorkflowAttributes.ID, actionEventId);
        actionEventJson.put(WorkflowAttributes.NAME, i18NResolutionAdapter.get(triggerActionEventService.get(actionEventId).getDisplayName()));

        nodeJson.put(WorkflowAttributes.ID, actionConfiguration.id());
        nodeJson.put(WorkflowAttributes.SOURCE, actionSourceJson);
        nodeJson.put(WorkflowAttributes.EVENT, actionEventJson);

        workflowActionResolver.resolveActionJson(orgId, triggerSourceId, nodeJson, actionConfiguration, i18NResolutionAdapter);
    }

    /**
     * Gets the criteria set JSON.
     *
     * @param orgId                  the organization id
     * @param conditionConfiguration the condition configuration
     * @param i18NResolutionAdapter  the i18n resolution adapter
     * @return the criteria set JSON
     */
    public JsonArray getCriteriaSetJson(final long orgId, final ConditionConfiguration conditionConfiguration, final I18nResolutionAdapter i18NResolutionAdapter) {
        final JsonArray conditions = Json.array();

        for (final WorkflowCriteriaSet workflowCriteriaSet : conditionConfiguration.getCriteriaSets()) {
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
                            final long userId = NumberUtils.parseNumber(workflowCriteriaValue.getValue()).longValue();

                            workflowCriteriaValue.setValueId(userId);
                            workflowCriteriaValue.setValue(crmUserService.getUser(orgId, userId).getFullName());
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
     * @param workflowComparator    Refers the {@link WorkflowComparator} details.
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
     * @param orgId                 Refers the organization id.
     * @param workflowCriteriaField Refers the criteria field.
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

}
