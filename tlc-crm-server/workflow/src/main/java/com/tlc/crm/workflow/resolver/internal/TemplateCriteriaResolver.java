package com.tlc.crm.workflow.resolver.internal;

import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;

import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;

import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.features.api.activity.model.Availability;
import com.tlc.crm.entity.features.api.activity.model.Priority;
import com.tlc.crm.entity.features.api.activity.model.Reminder;
import com.tlc.crm.entity.features.api.activity.model.Status;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.models.type.DropdownType;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;

import com.tlc.crm.workflow.api.model.FieldGroup;
import com.tlc.crm.workflow.api.model.WorkflowCriteria;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaField;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaValue;
import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.cache.service.WorkflowFieldService;
import com.tlc.crm.workflow.resolver.CriteriaResolver;

import com.tlc.i18n.I18nResolutionAdapter;

import java.util.Objects;

/**
 * <p>
 * Provides the template related {@link WorkflowCriteria} services
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see CriteriaResolver
 */
public final class TemplateCriteriaResolver implements CriteriaResolver {

    private final FieldService fieldService;
    private final PipelineManager pipelineManager;
    private final PipelineStageManager pipelineStageManager;
    private final LifeCycleStageManager lifeCycleStageManager;
    private final OptionService optionService;
    private final WorkflowFieldService workflowFieldService;
    private final ActivityTypeManager activityTypeManager;

    private TemplateCriteriaResolver() {
        this.fieldService = FieldService.getInstance();
        this.pipelineManager = PipelineManager.getInstance();
        this.pipelineStageManager = PipelineStageManager.getInstance();
        this.lifeCycleStageManager = LifeCycleStageManager.getInstance();
        this.optionService = OptionService.getInstance();
        this.workflowFieldService = WorkflowFieldService.getInstance();
        this.activityTypeManager = ActivityTypeManager.getInstance();
    }

    private static class Instance {
        private static final TemplateCriteriaResolver INSTANCE = new TemplateCriteriaResolver();
    }

    /**
     * Gets the {@link TemplateCriteriaResolver} instance.
     *
     * @return {@link TemplateCriteriaResolver}.
     */
    public static TemplateCriteriaResolver getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                   Represents the organization id
     * @param workflowCriteriaValue   It holds the {@link WorkflowCriteriaValue} details
     * @param workflowCriteriaField   It holds the {@link WorkflowCriteriaValue} details
     * @param i18NResolutionAdapter Represents the resolver of the I18N strings
     */
    @Override
    public void resolveCriteriaDetails(final Long orgId, final WorkflowCriteriaField workflowCriteriaField,
                                       final WorkflowCriteriaValue workflowCriteriaValue, final I18nResolutionAdapter i18NResolutionAdapter) {
        final long fieldId = workflowCriteriaField.getFieldId();

        if (Objects.equals(FieldGroup.SOURCE_RELATED_FIELD, workflowCriteriaField.getFieldGroup())) {
            final FieldInfo fieldInfo = fieldService.getField(orgId, fieldId);

            if (StoreType.isSelectorType(fieldInfo.storeType())) {
                final DropdownType dropdownType = fieldInfo.presetConfig().dropdownType();

                if (Objects.equals(DropdownType.HIDDEN_OPTION, dropdownType)) {

                    switch (fieldInfo.aliasName()) {
                        case "pipeline" -> {
                            final Pipeline defaultPipeline = pipelineManager.getDefaultPipeline(orgId);

                            workflowCriteriaValue.setValueId(defaultPipeline.id());
                            workflowCriteriaValue.setValue(defaultPipeline.getName());
                        }
                        case "pipelineStage" -> {
                            final PipelineStage defaultPipelineStage = pipelineStageManager.getDefaultPipelineStage(orgId);

                            workflowCriteriaValue.setValueId(defaultPipelineStage.id());
                            workflowCriteriaValue.setValue(defaultPipelineStage.getName());
                        }
                        case "lifeCycleStage" -> {
                            final LifeCycleStage defaultLifeCycleStage = lifeCycleStageManager.getDefaultStage(orgId);

                            workflowCriteriaValue.setValueId(defaultLifeCycleStage.id());
                            workflowCriteriaValue.setValue(defaultLifeCycleStage.getName());
                        }
                        case "status" ->
                                workflowCriteriaValue.setValueId((long) Deal.State.getStateByName(workflowCriteriaValue.getValue().toString()).getId());
                    }
                } else {
                    final Option defaultOption = optionService.getDefaultOption(orgId, fieldInfo);
                    final Option option = Objects.nonNull(defaultOption) ? defaultOption :
                            optionService.getOption(orgId, fieldInfo, (String) workflowCriteriaValue.getValue());

                    if (Objects.nonNull(option)) {
                        workflowCriteriaValue.setValueId(option.id());
                        workflowCriteriaValue.setValue(option.getOption());
                    }
                }
            }
        } else {
            final WorkflowField workflowField = workflowFieldService.get(fieldId);

            if (StoreType.isSelectorType(workflowField.getFieldType().storeType())) {
                final String value = workflowCriteriaValue.getValue().toString();

                switch (workflowField.getAliasName()) {
                    case "availability" -> {
                        final Availability availability = Availability.getByName(value);

                        workflowCriteriaValue.setValueId((long) availability.getId());
                        workflowCriteriaValue.setValue(i18NResolutionAdapter.get(availability.getDisplayName()));
                    }
                    case "reminder" -> {
                        final Reminder reminder = Reminder.getByName(value);

                        workflowCriteriaValue.setValueId((long) reminder.getId());
                        workflowCriteriaValue.setValue(i18NResolutionAdapter.get(reminder.getDisplayName()));
                    }
                    case "priority" -> {
                        final Priority priority = Priority.getByName(value);

                        workflowCriteriaValue.setValueId((long) priority.getId());
                        workflowCriteriaValue.setValue(i18NResolutionAdapter.get(priority.getDisplayName()));
                    }
                    case "status" -> {
                        final Status status = Status.getByName(value);

                        workflowCriteriaValue.setValueId((long) status.getId());
                        workflowCriteriaValue.setValue(i18NResolutionAdapter.get(status.getDisplayName()));
                    }
                    case "activityType" -> {
                        final ActivityType defaultActivityType = activityTypeManager.getDefaultActivityType(orgId);

                        workflowCriteriaValue.setValueId(defaultActivityType.id());
                        workflowCriteriaValue.setValue(defaultActivityType.getName());
                    }
                }
            }
        }
    }
}
