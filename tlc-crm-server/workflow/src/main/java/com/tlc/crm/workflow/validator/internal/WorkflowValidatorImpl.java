package com.tlc.crm.workflow.validator.internal;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.validator.ValidatorContext;
import com.tlc.crm.workflow.api.manager.WorkflowActionManager;
import com.tlc.crm.workflow.api.model.FieldGroup;
import com.tlc.crm.workflow.api.model.TriggerActionField;
import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.api.model.WorkflowAction;
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
import com.tlc.crm.workflow.validator.annotation.WorkflowValidator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates the {@link Workflow} entity and returns appropriate message.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class WorkflowValidatorImpl implements ConstraintValidator<WorkflowValidator, Workflow> {

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

    public WorkflowValidatorImpl() {
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
    public boolean isValid(final Workflow workflow, final ConstraintValidatorContext context) {
        final ValidatorContext validatorContext = new ValidatorContext(context);
        final List<WorkflowCriteriaSet> criteriaSets = (List<WorkflowCriteriaSet>) workflow.getCriteriaSets();
        final Long orgId = workflow.orgId();

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

                if (!isValidCriteriaFields(orgId, fieldIds, workflow.getTriggerSource().getName())) {
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
        final Collection<WorkflowAction> workflowActions = workflow.getActions();

        if (Objects.nonNull(workflowActions)) {
            for (final WorkflowAction workflowAction : workflow.getActions()) {
                final JsonArray fieldMappingArray = workflowAction.getFieldMappingAsJson();
                final Collection<Long> fieldIds = new HashSet<>();
                final int fieldMappingArraySize = fieldMappingArray.size();
                final String triggerActionSource = triggerActionSourceService.get(workflowAction.getTriggerActionSource().id()).getAliasName();

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

                if (!isValidActionFields(orgId, fieldIds, workflow, workflowAction)) {
                    addConstraintViolationMessage(validatorContext, "i18n.workflow.action.fields.invalid");
                }
                final String actionEventAliasName = triggerActionEventService.get(workflowAction.getTriggerActionEvent().id()).getAliasName();

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
        }

        return !validatorContext.isErrorsFound();
    }

    /**
     * <p>
     * Checks if the given collection of field IDs matches the fields associated with a specific source
     * </p>
     *
     * @param orgId      Represents the organization ID
     * @param fieldIds   Represents the collection of field IDs
     * @param sourceName The name of the source
     * @return True if all field IDs are associated with the specified source, false otherwise
     */
    private boolean isValidCriteriaFields(final Long orgId, final Collection<Long> fieldIds, final String sourceName) {
        final TriggerActionSource triggerActionSource = triggerActionSourceService.getActionSourceByAliasName(sourceName);

        if (triggerActionSource.isCommonActionSource()) {
            final Collection<FieldInfo> fieldInfoList = fieldService.getFieldsBySource(orgId, Source.get(sourceName).getName());

            return fieldInfoList.stream().map(FieldInfo::id).collect(Collectors.toSet()).containsAll(fieldIds);
        } else {
            final Collection<WorkflowField> workflowFields = workflowFieldService.getWorkflowFieldsBySource(triggerActionSource.id());

            return workflowFields.stream().map(WorkflowField::getId).collect(Collectors.toSet()).containsAll(fieldIds);
        }
    }

    /**
     * <p>
     * Checks whether the action fields specified in the workflow action are valid for the given organization
     * </p>
     *
     * @param orgId          Represents the organization id
     * @param fieldIds       Represents the collection og field ids
     * @param workflow       It holds the {@link Workflow} details
     * @param workflowAction It holds the {@link WorkflowAction} details
     * @return True if all action fields are valid, false otherwise.
     */
    private boolean isValidActionFields(final Long orgId, final Collection<Long> fieldIds, final Workflow workflow, final WorkflowAction workflowAction) {
        final TriggerActionSource triggerActionSource = workflowAction.getTriggerActionSource();
        final long triggerActionSourceId = triggerActionSource.id();
        final Long triggerActionId = workflowActionManager.getTriggerActionId(workflow.getTriggerSource().getId(),
                triggerActionSourceId, workflowAction.getTriggerActionEvent().id());
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
