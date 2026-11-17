package com.tlc.crm.workflow.v2.validator.internal;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.validator.ValidatorContext;

import com.tlc.crm.workflow.api.manager.WorkflowActionManager;
import com.tlc.crm.workflow.api.model.FieldGroup;
import com.tlc.crm.workflow.api.model.TriggerActionField;
import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.api.model.WorkflowCriteria;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaField;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaSet;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaValue;
import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.cache.service.TriggerActionEventService;
import com.tlc.crm.workflow.cache.service.TriggerActionFieldService;
import com.tlc.crm.workflow.cache.service.TriggerActionSourceService;
import com.tlc.crm.workflow.cache.service.WorkflowFieldService;
import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.crm.workflow.v2.api.model.ActionConfiguration;
import com.tlc.crm.workflow.v2.api.model.ConditionConfiguration;
import com.tlc.crm.workflow.v2.api.model.DelayConfiguration;
import com.tlc.crm.workflow.v2.api.model.EventContextType;
import com.tlc.crm.workflow.v2.api.model.IfElseConfiguration;
import com.tlc.crm.workflow.v2.api.model.NodeEvent;
import com.tlc.crm.workflow.v2.api.model.NodeType;
import com.tlc.crm.workflow.v2.api.model.SpanConfiguration;
import com.tlc.crm.workflow.v2.api.model.SpanType;
import com.tlc.crm.workflow.v2.api.model.TriggerConfiguration;
import com.tlc.crm.workflow.v2.api.model.TriggerScope;
import com.tlc.crm.workflow.v2.api.model.WaitForConditionConfiguration;
import com.tlc.crm.workflow.v2.api.model.WorkflowNode;
import com.tlc.crm.workflow.v2.api.model.WorkflowNodeSupport;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2;
import com.tlc.crm.workflow.v2.validator.annotation.WorkflowV2Validator;
import com.tlc.crm.workflow.validator.internal.ContactActionFieldsValidator;
import com.tlc.crm.workflow.validator.internal.EmailActionFieldsValidator;
import com.tlc.crm.workflow.validator.internal.WAActionFieldsValidator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates {@link WorkflowV2} entity constraints.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public class WorkflowV2ValidatorImpl implements ConstraintValidator<WorkflowV2Validator, WorkflowV2> {

    private final TriggerActionEventService triggerActionEventService;
    private final TriggerActionSourceService triggerActionSourceService;
    private final ContactActionFieldsValidator contactActionFieldsValidator;
    private final EmailActionFieldsValidator emailActionFieldsValidator;
    private final FieldService fieldService;
    private final WorkflowFieldService workflowFieldService;
    private final WorkflowActionManager workflowActionManager;
    private final TriggerActionFieldService triggerActionFieldService;
    private final WAActionFieldsValidator waActionFieldsValidator;
    private final Collection<String> messageActionSources;

    public WorkflowV2ValidatorImpl() {
        this.triggerActionEventService = TriggerActionEventService.getInstance();
        this.triggerActionSourceService = TriggerActionSourceService.getInstance();
        this.contactActionFieldsValidator = ContactActionFieldsValidator.getInstance();
        this.emailActionFieldsValidator = EmailActionFieldsValidator.getInstance();
        this.fieldService = FieldService.getInstance();
        this.workflowFieldService = WorkflowFieldService.getInstance();
        this.workflowActionManager = WorkflowActionManager.getInstance();
        this.triggerActionFieldService = TriggerActionFieldService.getInstance();
        this.waActionFieldsValidator = WAActionFieldsValidator.getInstance();
        this.messageActionSources = Set.of(WorkflowAttributes.EMAIL, WorkflowAttributes.WHATSAPP);
    }

    /**
     * {@inheritDoc}
     *
     * @param workflow {@link Workflow} to validate.
     * @param context  Context in which the constraint is evaluated.
     */
    @Override
    public boolean isValid(final WorkflowV2 workflow, final ConstraintValidatorContext context) {
        final Long orgId = workflow.orgId();
        final ValidatorContext validatorContext = new ValidatorContext(context);
        final Map<NodeType, List<WorkflowNode>> groupedNodesByType = workflow.getNodesAsCollection().stream()
                .collect(Collectors.groupingBy(WorkflowNode::getNodeType));

        for (final WorkflowNode workflowNode : groupedNodesByType.get(NodeType.TRIGGER)) {
            final TriggerConfiguration triggerConfiguration = WorkflowNodeSupport.triggerConfiguration(workflowNode);

            if (TriggerScope.SELECTED_USERS == triggerConfiguration.getTriggerScope()) {
                final Collection<Long> userIds = triggerConfiguration.getUserIds();

                if (Objects.isNull(userIds) || userIds.isEmpty()) {
                    addConstraintViolationMessage(validatorContext, "i18n.workflow.trigger.users.required");
                    return false;
                } else if (userIds.size() > NumericalConstant.FIFTY) {
                    addConstraintViolationMessage(validatorContext, "i18n.workflow.trigger.users.limit.exceeds");
                    return false;
                }
            }
        }
        final List<WorkflowNode> conditionNodes = groupedNodesByType.getOrDefault(NodeType.INSTANT_CONDITION, Collections.emptyList());

        if (conditionNodes.size() > NumericalConstant.FIVE) {
            addConstraintViolationMessage(validatorContext, "i18n.workflow.instant.condition.limit.exceeds");

            return false;
        } else {
            for (final WorkflowNode workflowNode : conditionNodes) {
                final ConditionConfiguration conditionConfiguration = WorkflowNodeSupport.conditionConfiguration(workflowNode);

                validateConditions(orgId, conditionConfiguration, workflow, conditionConfiguration.getNodeEvent(), validatorContext);

                if (validatorContext.isErrorsFound()) {
                    return false;
                }
            }
        }

        final List<WorkflowNode> ifElseConditions = groupedNodesByType.getOrDefault(NodeType.IF_ELSE_CONDITION, Collections.emptyList());

        if (ifElseConditions.size() > NumericalConstant.FIVE) {
            addConstraintViolationMessage(validatorContext, "i18n.workflow.ifelse.condition.limit.exceeds");

            return false;
        } else {
            for (final WorkflowNode workflowNode : ifElseConditions) {
                final IfElseConfiguration ifElseConfiguration = WorkflowNodeSupport.ifElseConfiguration(workflowNode);

                validateConditions(orgId, ifElseConfiguration.getCondition(), workflow, ifElseConfiguration.getNodeEvent(), validatorContext);

                if (validatorContext.isErrorsFound()) {
                    return false;
                }
            }
        }
        final List<WorkflowNode> waitConditions = groupedNodesByType.getOrDefault(NodeType.WAIT_FOR_CONDITION, Collections.emptyList());

        if (waitConditions.size() > NumericalConstant.FIVE) {
            addConstraintViolationMessage(validatorContext, "i18n.workflow.wait.condition.limit.exceeds");

            return false;
        } else {
            for (final WorkflowNode workflowNode : waitConditions) {
                final WaitForConditionConfiguration waitForConditionConfiguration = WorkflowNodeSupport.waitForConditionConfiguration(workflowNode);

                validateConditions(orgId, waitForConditionConfiguration.getCondition(), workflow, waitForConditionConfiguration.getNodeEvent(), validatorContext);

                if (validatorContext.isErrorsFound()) {
                    return false;
                }

                validateSpanConfig(waitForConditionConfiguration.getSpan(), validatorContext);

                if (validatorContext.isErrorsFound()) {
                    return false;
                }
            }
        }
        final List<WorkflowNode> delayNodes = groupedNodesByType.getOrDefault(NodeType.DELAY, Collections.emptyList());

        if (delayNodes.size() > NumericalConstant.FIVE) {
            addConstraintViolationMessage(validatorContext, "i18n.workflow.delay.limit.exceeds");

            return false;
        } else {
            for (final WorkflowNode workflowNode : delayNodes) {
                final DelayConfiguration delayConfiguration = WorkflowNodeSupport.delayConfiguration(workflowNode);

                validateSpanConfig(delayConfiguration.getSpan(), validatorContext);

                if (validatorContext.isErrorsFound()) {
                    return false;
                }
            }
        }
        final List<WorkflowNode> actionNodes = groupedNodesByType.getOrDefault(NodeType.ACTION, Collections.emptyList());

        if (actionNodes.size() > NumericalConstant.FIVE) {
            addConstraintViolationMessage(validatorContext, "i18n.workflow.action.size.exceeds");
            return false;
        }

        if (actionNodes.isEmpty()) {
            addConstraintViolationMessage(validatorContext, "i18n.workflow.action.mandatory");
            return false;
        } else {
            for (final WorkflowNode workflowNode : actionNodes) {
                final ActionConfiguration actionConfiguration = WorkflowNodeSupport.actionConfiguration(workflowNode);

                validateActions(orgId, actionConfiguration, workflow.getTriggerSourceId(), validatorContext);

                if (validatorContext.isErrorsFound()) {
                    return false;
                }
            }
        }

        return !validatorContext.isErrorsFound();
    }

    /**
     * Validates the actions for a workflow node.
     *
     * @param orgId               the organization id
     * @param actionConfiguration the action configuration
     * @param triggerSourceId     the trigger source id
     * @param validatorContext    the validator context
     */
    private void validateActions(final long orgId, final ActionConfiguration actionConfiguration, final short triggerSourceId, final ValidatorContext validatorContext) {
        final JsonArray fieldMappingArray = actionConfiguration.getFieldMappingAsJson();
        final Collection<Long> fieldIds = new HashSet<>();
        final int fieldMappingArraySize = fieldMappingArray.size();
        final String triggerActionSource = triggerActionSourceService.get(actionConfiguration.getTriggerActionSource().id()).getAliasName();

        if (messageActionSources.contains(triggerActionSource)) {
            for (int index = 0; index < fieldMappingArraySize; index++) {
                final JsonObject fieldJson = fieldMappingArray.getJsonObject(index);

                fieldIds.add(fieldJson.getLong(WorkflowAttributes.FIELD_ID));
            }
        } else {
            for (int index = 0; index < fieldMappingArraySize; index++) {
                final JsonArray fieldsArray = fieldMappingArray.getJsonArray(index);

                for (int fieldsArrayIndex = 0; fieldsArrayIndex < fieldsArray.size(); fieldsArrayIndex++) {
                    fieldIds.add(fieldsArray.getJsonObject(fieldsArrayIndex).getLong(WorkflowAttributes.FIELD_ID));
                }
            }
        }

        if (!isValidActionFields(orgId, fieldIds, triggerSourceId, actionConfiguration)) {
            addConstraintViolationMessage(validatorContext, "i18n.workflow.action.fields.invalid");
        }
        final String actionEventAliasName = triggerActionEventService.get(actionConfiguration.getTriggerActionEvent().id()).getAliasName();

        switch (triggerActionSource) {
            case WorkflowAttributes.CONTACT ->
                    contactActionFieldsValidator.validateContactActionFields(fieldMappingArray, actionEventAliasName, validatorContext);
            case WorkflowAttributes.EMAIL ->
                    emailActionFieldsValidator.validateEmailActionFields(fieldMappingArray, validatorContext);
            case WorkflowAttributes.WHATSAPP ->
                    waActionFieldsValidator.validateWAActionFields(fieldMappingArray, validatorContext);
            default -> validateActionFields(fieldMappingArray, validatorContext);
        }
    }

    /**
     * Validates the span configuration for a workflow node.
     *
     * @param span             the span configuration
     * @param validatorContext the validator context
     */
    private void validateSpanConfig(final SpanConfiguration span, final ValidatorContext validatorContext) {
        if (SpanType.CUSTOM == span.getType()) {
            if (null == span.getAmount()) {
                addConstraintViolationMessage(validatorContext, "i18n.workflow.span.unit.amount.invalid");
            }
        }
    }

    /**
     * Validates the conditions for a workflow node.
     *
     * @param orgId                  the organization id
     * @param conditionConfiguration the condition configuration
     * @param workflow               the workflow
     * @param nodeEvent              the node event
     * @param validatorContext       the validator context
     */
    private void validateConditions(final long orgId, final ConditionConfiguration conditionConfiguration,
                                    final WorkflowV2 workflow, final NodeEvent nodeEvent, final ValidatorContext validatorContext) {
        final List<WorkflowCriteriaSet> criteriaSets = (List<WorkflowCriteriaSet>) conditionConfiguration.getCriteriaSets();

        if (Objects.nonNull(criteriaSets)) {
            final int size = criteriaSets.size();

            for (int index = 0; index < size; index++) {
                final WorkflowCriteriaSet workflowCriteriaSet = criteriaSets.get(index);

                if (index < (size - 1) && Objects.isNull(workflowCriteriaSet.getConnector())) {
                    addConstraintViolationMessage(validatorContext, "i18n.workflow.criteria.set.connector.required");
                    break;
                }
                final List<WorkflowCriteria> criteriaList = (List<WorkflowCriteria>) workflowCriteriaSet.getCriteriaList();
                final Collection<Long> fieldIds = criteriaList.stream().map(WorkflowCriteria::getCriteriaField)
                        .filter(Objects::nonNull).map(WorkflowCriteriaField::getFieldId).collect(Collectors.toUnmodifiableSet());

                if (!isValidCriteriaFields(orgId, fieldIds, workflow, nodeEvent)) {
                    addConstraintViolationMessage(validatorContext, "i18n.workflow.criteria.fields.invalid");
                    break;
                } else {
                    final int criteriaSize = criteriaList.size();
                    final int lastIndex = criteriaSize - 1;

                    for (int criteriaIndex = 0; criteriaIndex < criteriaSize; criteriaIndex++) {
                        final WorkflowCriteria workflowCriteria = criteriaList.get(criteriaIndex);
                        final Collection<WorkflowCriteriaValue> criteriaValues = workflowCriteria.getCriteriaValues();

                        if (criteriaIndex < lastIndex && Objects.isNull(workflowCriteria.getConnector())) {
                            addConstraintViolationMessage(validatorContext, "i18n.workflow.criteria.connector.required");
                            break;
                        }

                        if (workflowCriteria.getComparator().hasValue() && (Objects.isNull(criteriaValues) || criteriaValues.isEmpty())) {
                            addConstraintViolationMessage(validatorContext, "i18n.workflow.criteria.value.required");
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if the given collection of field IDs matches the fields associated with a specific source.
     *
     * @param orgId    Represents the organization ID
     * @param fieldIds Represents the collection of field IDs
     * @return True if all field IDs are associated with the specified source, false otherwise
     */
    private boolean isValidCriteriaFields(final Long orgId, final Collection<Long> fieldIds, final WorkflowV2 workflow, final NodeEvent nodeEvent) {
        final EventContextType contextType = nodeEvent.getContextType();

        switch (contextType) {
            case TRIGGER -> {
                final TriggerConfiguration triggerConfiguration = WorkflowNodeSupport.triggerConfiguration(workflow.getNodes().get(nodeEvent.getSourceNodeClientKey()));
                final String sourceName = triggerConfiguration.getTriggerSource().getName();
                final TriggerActionSource triggerActionSource = triggerActionSourceService.getActionSourceByAliasName(sourceName);

                if (triggerActionSource.isCommonActionSource()) {
                    final Collection<FieldInfo> fieldInfoList = fieldService.getFieldsBySource(orgId, Source.get(sourceName).getName());

                    return fieldInfoList.stream().map(FieldInfo::id).collect(Collectors.toSet()).containsAll(fieldIds);
                } else {
                    final Collection<WorkflowField> workflowFields = workflowFieldService.getWorkflowFieldsBySource(triggerActionSource.id());

                    return workflowFields.stream().map(WorkflowField::getId).collect(Collectors.toSet()).containsAll(fieldIds);
                }
            }
            case ACTION -> {
                final ActionConfiguration actionConfiguration = WorkflowNodeSupport.actionConfiguration(workflow.getNodes().get(nodeEvent.getSourceNodeClientKey()));
                final TriggerActionSource triggerActionSource = actionConfiguration.getTriggerActionSource();

                if (triggerActionSource.isCommonActionSource()) {
                    final Collection<FieldInfo> fieldInfoList = fieldService.getFieldsBySource(orgId, Source.get(triggerActionSource.getAliasName()).getName());

                    return fieldInfoList.stream().map(FieldInfo::id).collect(Collectors.toSet()).containsAll(fieldIds);
                } else {
                    final Collection<WorkflowField> workflowFields = workflowFieldService.getWorkflowFieldsBySource(triggerActionSource.id());

                    return workflowFields.stream().map(WorkflowField::getId).collect(Collectors.toSet()).containsAll(fieldIds);
                }
            }
        }
        return false;
    }

    /**
     * <p>
     * Checks whether the action fields specified in the workflow action are valid for the given organization
     * </p>
     *
     * @param orgId               Represents the organization id
     * @param fieldIds            Represents the collection og field ids
     * @param actionConfiguration It holds the {@link ActionConfiguration} details
     * @return True if all action fields are valid, false otherwise.
     */
    private boolean isValidActionFields(final Long orgId, final Collection<Long> fieldIds, final short triggerSourceId,
                                        final ActionConfiguration actionConfiguration) {
        final TriggerActionSource triggerActionSource = actionConfiguration.getTriggerActionSource();
        final long triggerActionSourceId = triggerActionSource.id();
        final Long triggerActionId = workflowActionManager.getTriggerActionId(triggerSourceId, triggerActionSourceId, actionConfiguration.getTriggerActionEvent().id());
        final Collection<TriggerActionField> triggerActionFields = triggerActionFieldService.get(triggerActionId);
        final Optional<TriggerActionField> firstTriggerActionField = triggerActionFields.stream().findFirst();

        if (firstTriggerActionField.isPresent() && FieldGroup.SOURCE_RELATED_FIELD == firstTriggerActionField.get().getFieldGroup()) {
            final Collection<FieldInfo> fieldInfoList = fieldService.getFieldsBySource(orgId, Source.get(triggerActionSource.getAliasName()).getName());

            return fieldInfoList.stream().map(FieldInfo::id).collect(Collectors.toSet()).containsAll(fieldIds);
        } else {
            final Collection<WorkflowField> workflowFields = workflowFieldService.getWorkflowFieldsBySource(triggerActionSourceId);

            return workflowFields.stream().map(WorkflowField::getId).collect(Collectors.toSet()).containsAll(fieldIds);
        }
    }

    /**
     * Adds the constraint violation message to the validation context.
     *
     * @param validatorContext Refers the validator context.
     * @param message          Violation message.
     */
    protected void addConstraintViolationMessage(final ValidatorContext validatorContext, final String message) {
        validatorContext.setErrorsFound(true);
        validatorContext.getContext().buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }

    /**
     * <p>
     * Validates the fieldMapping {@link JsonArray} to ensure that required fields have non-empty values
     * </p>
     *
     * @param fieldMappingArray The {@link JsonArray} containing field mappings to validate
     * @param validatorContext  The {@link ValidatorContext} for adding constraint violation messages.
     */
    private void validateActionFields(final JsonArray fieldMappingArray, final ValidatorContext validatorContext) {
        final int fieldMappingArraySize = fieldMappingArray.size();

        outerLoop:
        for (int fieldMappingArrayIndex = 0; fieldMappingArrayIndex < fieldMappingArraySize; fieldMappingArrayIndex++) {
            final JsonArray fieldArray = fieldMappingArray.getJsonArray(fieldMappingArrayIndex);
            final int fieldArraySize = fieldArray.size();

            for (int fieldArrayIndex = 0; fieldArrayIndex < fieldArraySize; fieldArrayIndex++) {
                final JsonObject fieldJson = fieldArray.getJsonObject(fieldArrayIndex);

                if (fieldJson.optBoolean(WorkflowAttributes.IS_REQUIRED) && !(fieldJson.containsKey(WorkflowAttributes.VALUES) &&
                        fieldJson.getJsonArray(WorkflowAttributes.VALUES).size() > 0)) {
                    addConstraintViolationMessage(validatorContext, "i18n.workflow.action.field.value.invalid");
                    break outerLoop;
                }
            }
        }
    }
}
