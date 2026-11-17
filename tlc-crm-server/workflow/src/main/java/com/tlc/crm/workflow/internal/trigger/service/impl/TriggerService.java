package com.tlc.crm.workflow.internal.trigger.service.impl;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.api.model.WorkflowAction;
import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.cache.service.TriggerActionSourceService;
import com.tlc.crm.workflow.cache.service.WorkflowFieldService;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;
import com.tlc.crm.workflow.v2.api.model.ActionConfiguration;
import com.tlc.crm.workflow.v2.api.model.NodeType;
import com.tlc.crm.workflow.v2.api.model.WorkflowNode;
import com.tlc.crm.workflow.v2.api.model.WorkflowNodeSupport;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Provides a range of services associated with the trigger event of a workflow.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class TriggerService {

    private final FieldService fieldService;
    private final WorkflowFieldService workflowFieldService;
    private final TriggerActionSourceService actionSourceService;

    private TriggerService() {
        this.fieldService = FieldService.getInstance();
        this.workflowFieldService = WorkflowFieldService.getInstance();
        this.actionSourceService = TriggerActionSourceService.getInstance();
    }

    private static class Instance {
        private static final TriggerService INSTANCE = new TriggerService();
    }

    /**
     * Gets the instance of {@link TriggerService}.
     *
     * @return The {@link TriggerService} instance.
     */
    public static TriggerService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Collects the user ids from the {@link WorkflowAction} json mapper.
     *
     * @param orgId     Refers the organization id.
     * @param workflows Refers the collection of {@link Workflow}.
     * @param userIds   Refers the user ids.
     */
    public void collectUserIds(final Long orgId, final Collection<Workflow> workflows, final Set<Long> userIds) {
        final Collection<FieldInfo> fields = fieldService.getAllFields(orgId);
        final Collection<WorkflowField> workflowFields = workflowFieldService.getWorkflowFieldsBySource(actionSourceService
                .getActionSourceByAliasName(ActionAttributes.ACTIVITY).id());

        final Collection<Long> userFieldIds = fields.stream()
                .filter(field -> Objects.equals(StoreType.OWNER, field.storeType()) && !Objects.equals(Source.ACTIVITY.getName(), field.fieldSource().getName()))
                .map(FieldInfo::id).collect(Collectors.toUnmodifiableSet());
        final Collection<Long> workflowFieldUserIds = workflowFields.stream().filter(field -> Objects.equals(StoreType.OWNER, field.getFieldType().storeType()))
                .map(WorkflowField::getId).collect(Collectors.toUnmodifiableSet());

        final Consumer<JsonObject> userIdCollector = (fieldJson) -> {
            final JsonArray values = fieldJson.getJsonArray(ActionAttributes.VALUES);
            final JsonObject valueJson = values.getJsonObject(0);

            if (!valueJson.containsKey(ActionAttributes.DEPENDS_ON)) {
                userIds.add(valueJson.getLong(ActionAttributes.VALUE));
            }
        };

        for (final Workflow workflow : workflows) {

            workflow.getActions().forEach(workflowAction -> {
                final TriggerActionSource actionSource = workflowAction.getTriggerActionSource();
                final String aliasName = actionSource.getAliasName();

                if (!aliasName.equals(ActionAttributes.NOTE) && !aliasName.equals(ActionAttributes.EMAIL) && !aliasName.equals(ActionAttributes.WHATSAPP)) {
                    final JsonArray fieldMapping = workflowAction.getFieldMappingAsJson();

                    for (int index = 0; index < fieldMapping.size(); index++) {
                        final JsonArray fieldArray = fieldMapping.getJsonArray(index);

                        for (int fieldIndex = 0; fieldIndex < fieldArray.size(); fieldIndex++) {
                            final JsonObject fieldJson = fieldArray.getJsonObject(fieldIndex);
                            final Long fieldId = fieldJson.getLong(ActionAttributes.FIELD_ID);

                            if (ActionAttributes.ACTIVITY.equals(aliasName) && workflowFieldUserIds.contains(fieldId)) {
                                userIdCollector.accept(fieldJson);
                                break;
                            } else if (userFieldIds.contains(fieldId)) {
                                userIdCollector.accept(fieldJson);
                                break;
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * Collects the user ids from the {@link WorkflowAction} json mapper.
     *
     * @param orgId     Refers the organization id.
     * @param userIds   Refers the user ids.
     * @param workflows Refers the collection of {@link WorkflowV2}.
     */
    public void collectUserIds(final Long orgId, final Set<Long> userIds, final Collection<WorkflowV2> workflows) {
        final Collection<FieldInfo> fields = fieldService.getAllFields(orgId);
        final Collection<WorkflowField> workflowFields = workflowFieldService.getWorkflowFieldsBySource(actionSourceService
                .getActionSourceByAliasName(ActionAttributes.ACTIVITY).id());

        final Collection<Long> userFieldIds = fields.stream()
                .filter(field -> Objects.equals(StoreType.OWNER, field.storeType()) && !Objects.equals(Source.ACTIVITY.getName(), field.fieldSource().getName()))
                .map(FieldInfo::id).collect(Collectors.toUnmodifiableSet());
        final Collection<Long> workflowFieldUserIds = workflowFields.stream().filter(field -> Objects.equals(StoreType.OWNER, field.getFieldType().storeType()))
                .map(WorkflowField::getId).collect(Collectors.toUnmodifiableSet());

        final Consumer<JsonObject> userIdCollector = (fieldJson) -> {
            final JsonArray values = fieldJson.getJsonArray(ActionAttributes.VALUES);
            final JsonObject valueJson = values.getJsonObject(0);

            if (!valueJson.containsKey(ActionAttributes.DEPENDS_ON)) {
                userIds.add(valueJson.getLong(ActionAttributes.VALUE));
            }
        };

        for (final WorkflowV2 workflow : workflows) {
            final List<WorkflowNode> actionNodes = workflow.getNodesAsCollection().stream().filter(workflowNode -> Objects.equals(NodeType.ACTION, workflowNode.getNodeType())).toList();

            for (final WorkflowNode workflowNode : actionNodes) {
                final ActionConfiguration actionConfiguration = WorkflowNodeSupport.actionConfiguration(workflowNode);
                final TriggerActionSource actionSource = actionConfiguration.getTriggerActionSource();
                final String aliasName = actionSource.getAliasName();

                if (!aliasName.equals(ActionAttributes.NOTE) && !aliasName.equals(ActionAttributes.EMAIL) && !aliasName.equals(ActionAttributes.WHATSAPP)) {
                    final JsonArray fieldMapping = actionConfiguration.getFieldMappingAsJson();

                    for (int index = 0; index < fieldMapping.size(); index++) {
                        final JsonArray fieldArray = fieldMapping.getJsonArray(index);

                        for (int fieldIndex = 0; fieldIndex < fieldArray.size(); fieldIndex++) {
                            final JsonObject fieldJson = fieldArray.getJsonObject(fieldIndex);
                            final Long fieldId = fieldJson.getLong(ActionAttributes.FIELD_ID);

                            if (ActionAttributes.ACTIVITY.equals(aliasName) && workflowFieldUserIds.contains(fieldId)) {
                                userIdCollector.accept(fieldJson);
                                break;
                            } else if (userFieldIds.contains(fieldId)) {
                                userIdCollector.accept(fieldJson);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}
