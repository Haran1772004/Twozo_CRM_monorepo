package com.tlc.crm.workflow.service.internal;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaField;
import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.builder.WorkflowCriteriaFieldBuilder;
import com.tlc.crm.workflow.cache.service.TriggerActionSourceService;
import com.tlc.crm.workflow.cache.service.WorkflowFieldService;
import com.tlc.crm.workflow.service.WorkflowCriteriaFieldService;
import com.tlc.crm.workflow.v2.field.WorkflowAttributesV2;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the implementation for the {@link WorkflowCriteriaFieldService} related services
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public final class WorkflowCriteriaFieldServiceImpl implements WorkflowCriteriaFieldService {

    private final WorkflowFieldService workflowFieldService;
    private final FieldService fieldService;
    private final WorkflowCriteriaFieldBuilder workflowCriteriaFieldBuilder;
    private final TriggerActionSourceService triggerActionSourceService;

    public WorkflowCriteriaFieldServiceImpl() {
        this.workflowFieldService = WorkflowFieldService.getInstance();
        this.fieldService = FieldService.getInstance();
        this.workflowCriteriaFieldBuilder = WorkflowCriteriaFieldBuilder.getInstance();
        this.triggerActionSourceService = TriggerActionSourceService.getInstance();
    }

    private static class Instance {
        private static final WorkflowCriteriaFieldServiceImpl INSTANCE = new WorkflowCriteriaFieldServiceImpl();
    }

    /**
     * Gets the {@link WorkflowCriteriaFieldServiceImpl} instance
     *
     * @return {@link WorkflowCriteriaFieldServiceImpl}
     */
    public static WorkflowCriteriaFieldServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId           Represents an organization id
     * @param triggerSourceId Represents the trigger source id
     * @return {@link Collection} of {@link WorkflowCriteriaField}
     */
    @Override
    public Collection<WorkflowCriteriaField> getFields(final long orgId, final Short triggerSourceId) {
        final TriggerSource triggerSource = TriggerSource.get(triggerSourceId);

        return switch (triggerSource) {
            case CONTACT, DEAL, COMPANY -> {
                final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, triggerSource.getName())
                        .stream().filter(field -> !isRestrictedStoreType(field.storeType()) && field.isActive())
                        .collect(Collectors.toUnmodifiableSet());

                yield workflowCriteriaFieldBuilder.buildSourceCriteriaFields(fields).stream().sorted(Comparator.comparing(WorkflowCriteriaField::getFieldId)).toList();
            }
            case ACTIVITY -> {
                final Collection<WorkflowField> workflowFields = workflowFieldService.getWorkflowFieldsBySource(triggerActionSourceService.getActionSourceByAliasName(triggerSource.name()).id())
                        .stream().filter(WorkflowField::isCriteriaField).collect(Collectors.toUnmodifiableSet());
                yield workflowCriteriaFieldBuilder.buildWorkflowCriteriaFields(workflowFields).stream().sorted(Comparator.comparing(WorkflowCriteriaField::getFieldId)).toList();
            }
        };
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents an organization id
     * @param actionSourceId Represents the action source id
     */
    @Override
    public Collection<WorkflowCriteriaField> getFields(final long orgId, final Long actionSourceId) {
        final TriggerActionSource triggerActionSource = triggerActionSourceService.get(actionSourceId);
        final String aliasName = triggerActionSource.getAliasName();

        return switch (aliasName.toLowerCase()) {
            case WorkflowAttributesV2.CONTACT, WorkflowAttributesV2.COMPANY, WorkflowAttributesV2.DEAL -> {
                final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, Source.get(aliasName).getName())
                        .stream().filter(field -> !isRestrictedStoreType(field.storeType()) && field.isActive())
                        .collect(Collectors.toUnmodifiableSet());

                yield workflowCriteriaFieldBuilder.buildSourceCriteriaFields(fields).stream().sorted(Comparator.comparing(WorkflowCriteriaField::getFieldId)).toList();
            }
            case WorkflowAttributesV2.ACTIVITY, WorkflowAttributesV2.EMAIL -> {
                final Collection<WorkflowField> workflowFields = workflowFieldService.getWorkflowFieldsBySource(triggerActionSource.id())
                        .stream().filter(WorkflowField::isCriteriaField).collect(Collectors.toUnmodifiableSet());
                yield workflowCriteriaFieldBuilder.buildWorkflowCriteriaFields(workflowFields).stream().sorted(Comparator.comparing(WorkflowCriteriaField::getFieldId)).toList();
            }
            default -> Collections.emptyList();
        };
    }

    /**
     * <p>
     * Checks whether the given {@link StoreType} is restricted for workflow fields or not
     * </p>
     *
     * @param storeType Represents the {@link StoreType}
     * @return true if the store type matched with workflow criteria fields store type
     */
    private boolean isRestrictedStoreType(final StoreType storeType) {
        return switch (storeType) {
            case TIME_RANGE, DATE_TIME_RANGE, DATE_RANGE -> true;
            default -> false;
        };
    }
}
