package com.tlc.crm.workflow.builder;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.models.type.DropdownType;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.workflow.api.model.CriteriaDropdownInfo;
import com.tlc.crm.workflow.api.model.FieldGroup;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaField;
import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.field.WorkflowAttributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Builds the {@link WorkflowField} / {@link FieldInfo} to {@link WorkflowCriteriaField}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public final class WorkflowCriteriaFieldBuilder {

    private final FieldTypeService fieldTypeService;

    private WorkflowCriteriaFieldBuilder() {
        this.fieldTypeService = FieldTypeService.getInstance();
    }

    private static class Instance {
        private static final WorkflowCriteriaFieldBuilder INSTANCE = new WorkflowCriteriaFieldBuilder();
    }

    /**
     * <p>
     * Gets the {@link WorkflowCriteriaFieldBuilder} Instance
     * </p>
     *
     * @return {@link WorkflowCriteriaFieldBuilder}
     */
    public static WorkflowCriteriaFieldBuilder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Builds the collection of workflow criteria fields for the given {@link Collection} of {@link WorkflowField}
     * </p>
     *
     * @return {@link Collection} of {@link WorkflowCriteriaField}
     */
    public Collection<WorkflowCriteriaField> buildWorkflowCriteriaFields(final Collection<WorkflowField> workflowFields) {
        final Collection<WorkflowCriteriaField> workflowCriteriaFields = new ArrayList<>();
        final FieldGroup fieldGroup = FieldGroup.WORKFLOW_RELATED_FIELD;
        final String staticDropdown = DropdownType.STATIC.name();

        for (final WorkflowField workflowField : workflowFields) {
            final WorkflowCriteriaField workflowCriteriaField = new WorkflowCriteriaField();
            final String dropdown = workflowField.dropdown();
            final String dropdownType = workflowField.dropdownType();

            if (Objects.nonNull(dropdown)) {
                final CriteriaDropdownInfo criteriaDropdownInfo = new CriteriaDropdownInfo();

                if (Objects.nonNull(dropdownType) && Objects.equals(staticDropdown, dropdownType)) {
                    criteriaDropdownInfo.setStatic(true);
                }
                criteriaDropdownInfo.setDropdownName(dropdown);
                workflowCriteriaField.setCriteriaDropdownInfo(criteriaDropdownInfo);
            }
            workflowCriteriaField.setName(workflowField.getName());
            workflowCriteriaField.setFieldId(workflowField.getId());
            workflowCriteriaField.setFieldGroup(fieldGroup);
            workflowCriteriaField.setFieldType(workflowField.getFieldType());
            workflowCriteriaField.setFieldConfig(workflowField.getConfigJson());

            workflowCriteriaFields.add(workflowCriteriaField);
        }

        return workflowCriteriaFields;
    }

    /**
     * <p>
     * Builds the collection of workflow criteria fields for the given {@link Collection} of {@link FieldInfo}
     * </p>
     *
     * @param fields Represents the {@link Collection} of {@link FieldInfo}
     * @return {@link Collection} of {@link WorkflowCriteriaField}
     */
    public Collection<WorkflowCriteriaField> buildSourceCriteriaFields(final Collection<FieldInfo> fields) {
        final Collection<WorkflowCriteriaField> workflowCriteriaFields = new ArrayList<>();
        final String idKey = WorkflowAttributes.ID;
        final FieldGroup fieldGroup = FieldGroup.SOURCE_RELATED_FIELD;

        for (final FieldInfo fieldInfo : fields) {

            if (!Objects.equals(idKey, fieldInfo.aliasName())) {
                final WorkflowCriteriaField workflowCriteriaField = new WorkflowCriteriaField();
                final StoreType storeType = fieldInfo.storeType();

                workflowCriteriaField.setCriteriaDropdownInfo(getDropdownInfo(fieldInfo));
                workflowCriteriaField.setName(fieldInfo.i18nName());
                workflowCriteriaField.setFieldId(fieldInfo.id());
                workflowCriteriaField.setAliasName(fieldInfo.aliasName());
                workflowCriteriaField.setSourceId(fieldInfo.sourceId());
                workflowCriteriaField.setFieldGroup(fieldGroup);
                workflowCriteriaField.setFieldType(fieldTypeService.getFieldTypeByStoreType(Optional.ofNullable(fieldInfo.presetConfig().getAutomationConfig())
                        .map(config -> StoreType.valueOf(config.getString(WorkflowAttributes.FIELD_TYPE))).orElseGet(() -> storeType)));
                workflowCriteriaField.setFieldConfig(fieldInfo.presetConfig().presetConfigAsJson());

                workflowCriteriaFields.add(workflowCriteriaField);
            }
        }

        return workflowCriteriaFields;
    }

    /**
     * <p>
     * Gets the {@link CriteriaDropdownInfo} for the given {@link FieldInfo}
     * </p>
     *
     * @param fieldInfo Represents the {@link FieldInfo}
     * @return the {@link CriteriaDropdownInfo} for the given {@link FieldInfo}
     */
    private CriteriaDropdownInfo getDropdownInfo(final FieldInfo fieldInfo) {
        final PresetConfig presetConfig = fieldInfo.presetConfig();
        final String dropdown = getDropdownName(fieldInfo, presetConfig);
        final StoreType storeType = fieldInfo.storeType();

        if ((StoreType.isSelectorType(storeType) || storeType.isEntityType() || storeType.isOwnerType()) || Objects.nonNull(dropdown)) {
            final CriteriaDropdownInfo criteriaDropdownInfo = new CriteriaDropdownInfo();

            if (Objects.equals(presetConfig.dropdownType(), DropdownType.STATIC)) {
                criteriaDropdownInfo.setStatic(true);
            }

            if (Objects.nonNull(presetConfig.dependsOn())) {
                criteriaDropdownInfo.addRequiredKey(WorkflowAttributes.DEPENDENT_ID);
            }

            switch (storeType) {
                case OWNER -> criteriaDropdownInfo.setDropdownName(WorkflowAttributes.USER_LIST);
                case COMPANY, COMPANY_ONBOARD -> criteriaDropdownInfo.setDropdownName(WorkflowAttributes.COMPANY_LIST);
                case CONTACT, CONTACT_ONBOARD, CONTACTS_ONBOARD ->
                        criteriaDropdownInfo.setDropdownName(WorkflowAttributes.CONTACT_LIST);
                default -> {
                    criteriaDropdownInfo.setDropdownName(dropdown);
                    criteriaDropdownInfo.addRequiredKey(WorkflowAttributes.ID);
                }
            }

            return criteriaDropdownInfo;
        }
        return null;
    }

    /**
     * Retrieves the appropriate dropdown name based on the provided {@link FieldInfo} and {@link PresetConfig}.
     * Specific cases are handled based on alias name, such as returning a predefined dropdown name.
     *
     * @param fieldInfo    The {@link FieldInfo}.
     * @param presetConfig The {@link PresetConfig} data.
     * @return The dropdown name.
     */
    private String getDropdownName(final FieldInfo fieldInfo, final PresetConfig presetConfig) {
        final String fieldName = fieldInfo.fieldName();
        final String aliasName = fieldInfo.aliasName();

        return switch (Objects.isNull(aliasName) ? fieldName : aliasName) {
            case "pipelineStage" -> WorkflowAttributes.STAGE_LIST;
            case "lastActivityType" -> WorkflowAttributes.ACTIVITY_TYPE_LIST;
            default -> presetConfig.dropdown();
        };
    }
}
