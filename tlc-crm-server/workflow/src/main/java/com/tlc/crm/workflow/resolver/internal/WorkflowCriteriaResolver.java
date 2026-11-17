package com.tlc.crm.workflow.resolver.internal;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.deal.manager.DealManager;
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
import com.tlc.crm.entity.features.api.tag.manager.TagManager;
import com.tlc.crm.entity.marketinglist.manager.MarketingListManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.DropdownType;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.fields.models.StoreType;

import com.tlc.crm.workflow.api.model.FieldGroup;
import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.WorkflowCriteria;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaField;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaValue;
import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.cache.service.WorkflowFieldService;
import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.crm.workflow.resolver.CriteriaResolver;

import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.iam.common.models.User;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A resolver that resolves the {@link WorkflowCriteria} related details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowCriteriaResolver implements CriteriaResolver {

    private final FieldService fieldService;
    private final OptionService optionService;
    private final PipelineManager pipelineManager;
    private final PipelineStageManager pipelineStageManager;
    private final LifeCycleStageManager lifeCycleStageManager;
    private final WorkflowFieldService workflowFieldService;
    private final ActivityTypeManager activityTypeManager;
    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final TagManager tagManager;
    private final DealManager dealManager;
    private final TerritoryManager territoryManager;
    private final UserService crmUserService;
    private final MarketingListManager marketingListManager;

    private WorkflowCriteriaResolver() {
        this.fieldService = FieldService.getInstance();
        this.optionService = OptionService.getInstance();
        this.pipelineManager = PipelineManager.getInstance();
        this.pipelineStageManager = PipelineStageManager.getInstance();
        this.lifeCycleStageManager = LifeCycleStageManager.getInstance();
        this.workflowFieldService = WorkflowFieldService.getInstance();
        this.activityTypeManager = ActivityTypeManager.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.companyManager = CompanyManager.getInstance();
        this.tagManager = TagManager.getInstance();
        this.dealManager = DealManager.getInstance();
        this.territoryManager = TerritoryManager.getInstance();
        this.crmUserService = UserService.getInstance();
        this.marketingListManager = MarketingListManager.getInstance();
    }

    private static class InstanceHolder {
        private static final WorkflowCriteriaResolver INSTANCE = new WorkflowCriteriaResolver();
    }

    /**
     * Gets the {@link WorkflowCriteriaResolver} instance.
     *
     * @return {@link WorkflowCriteriaResolver}.
     */
    public static WorkflowCriteriaResolver getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                   Represents the organization id.
     * @param workflowCriteriaField   It holds the {@link WorkflowCriteriaField} details
     * @param workflowCriteriaValue   It holds the {@link WorkflowCriteriaValue} details
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}
     */
    @Override
    public void resolveCriteriaDetails(final Long orgId, final WorkflowCriteriaField workflowCriteriaField,
                                       final WorkflowCriteriaValue workflowCriteriaValue, final I18nResolutionAdapter i18NResolutionAdapter) {
        final long fieldId = workflowCriteriaField.getFieldId();

        if (Objects.equals(FieldGroup.SOURCE_RELATED_FIELD, workflowCriteriaField.getFieldGroup())) {
            final FieldInfo fieldInfo = fieldService.getField(orgId, fieldId);
            final JsonObject workflowConfig = fieldInfo.presetConfig().getAutomationConfig();
            final StoreType storeType = Optional.ofNullable(workflowConfig)
                    .map(config -> StoreType.valueOf(config.getString(WorkflowAttributes.FIELD_TYPE)))
                    .orElseGet(fieldInfo::storeType);

            if (StoreType.isSelectorType(storeType)) {
                final Long id = NumberUtils.parseNumber(workflowCriteriaValue.getValue()).longValue();
                final DropdownType dropdownType = Optional.ofNullable(workflowConfig)
                        .map(config -> DropdownType.valueOf(config.getString(WorkflowAttributes.DROPDOWN_TYPE)))
                        .orElseGet(() -> fieldInfo.presetConfig().dropdownType());

                if (Objects.equals(DropdownType.HIDDEN_OPTION, dropdownType)) {

                    switch (fieldInfo.aliasName()) {
                        case "pipeline" -> {
                            final Pipeline pipeline = pipelineManager.get(orgId, id);

                            workflowCriteriaValue.setValueId(pipeline.id());
                            workflowCriteriaValue.setValue(pipeline.getName());
                        }
                        case "pipelineStage" -> {
                            final PipelineStage pipelineStage = pipelineStageManager.get(orgId, id);

                            workflowCriteriaValue.setValueId(pipelineStage.id());
                            workflowCriteriaValue.setValue(pipelineStage.getName());
                        }
                        case "lifeCycleStage" -> {
                            final LifeCycleStage lifeCycleStage = lifeCycleStageManager.get(orgId, id);

                            workflowCriteriaValue.setValueId(lifeCycleStage.id());
                            workflowCriteriaValue.setValue(lifeCycleStage.getName());
                        }
                        case "lifeCycleStatus" -> {
                            final LifeCycleStatus lifecycleStatus = LifeCycleStatusManager.getInstance().get(orgId, id);

                            workflowCriteriaValue.setValueId(lifecycleStatus.id());
                            workflowCriteriaValue.setValue(lifecycleStatus.getName());
                        }
                        case "lastActivityType" -> {
                            final ActivityType activityType = ActivityTypeManager.getInstance().get(orgId, id);

                            workflowCriteriaValue.setValueId(activityType.id());
                            workflowCriteriaValue.setValue(activityType.getName());
                        }
                        case "status" -> {
                            final Deal.State dealState = Deal.State.getStateById(id.intValue());

                            workflowCriteriaValue.setValueId(dealState.id());
                            workflowCriteriaValue.setValue(dealState.getDisplayName());
                        }
                    }
                } else {
                    final Option option = optionService.get(orgId, fieldId, id);

                    if (Objects.nonNull(option)) {
                        workflowCriteriaValue.setValueId(option.id());
                        workflowCriteriaValue.setValue(i18NResolutionAdapter.get(option.getOption()));
                    }
                }
            } else if (storeType.isEntityType() || StoreType.TAGS == storeType || StoreType.TERRITORY == storeType || StoreType.MARKETING_LIST == storeType) {
                final Long id = NumberUtils.parseNumber(workflowCriteriaValue.getValue()).longValue();

                resolveEntityTypeFields(orgId, id, workflowCriteriaValue, storeType);
            }
        } else {
            final WorkflowField workflowField = workflowFieldService.get(fieldId);
            final StoreType storeType = workflowField.getFieldType().storeType();

            if (StoreType.isSelectorType(storeType)) {
                final Long id = NumberUtils.parseNumber(workflowCriteriaValue.getValue()).longValue();

                switch (workflowField.getAliasName()) {
                    case "availability" -> {
                        final Availability availability = Availability.get(id.intValue());

                        workflowCriteriaValue.setValueId((long) availability.getId());
                        workflowCriteriaValue.setValue(i18NResolutionAdapter.get(availability.getDisplayName()));
                    }
                    case "reminder" -> {
                        final Reminder reminder = Reminder.get(id.intValue());

                        workflowCriteriaValue.setValueId((long) reminder.getId());
                        workflowCriteriaValue.setValue(i18NResolutionAdapter.get(reminder.getDisplayName()));
                    }
                    case "priority" -> {
                        final Priority priority = Priority.get(id.intValue());

                        workflowCriteriaValue.setValueId((long) priority.getId());
                        workflowCriteriaValue.setValue(i18NResolutionAdapter.get(priority.getDisplayName()));
                    }
                    case "status" -> {
                        final Status status = Status.get(id.intValue());

                        workflowCriteriaValue.setValueId((long) status.getId());
                        workflowCriteriaValue.setValue(i18NResolutionAdapter.get(status.getDisplayName()));
                    }
                    case "activityType" -> {
                        final ActivityType activityType = activityTypeManager.get(orgId, id);

                        workflowCriteriaValue.setValueId(activityType.id());
                        workflowCriteriaValue.setValue(activityType.getName());
                    }
                }
            } else if (storeType.isEntityType() || StoreType.DEAL == storeType) {
                final Long id = NumberUtils.parseNumber(workflowCriteriaValue.getValue()).longValue();

                resolveEntityTypeFields(orgId, id, workflowCriteriaValue, storeType);
            }
        }
    }

    /**
     * Resolves the entity type fields based on the provided organization ID, entity ID, and workflow criteria value.
     *
     * @param orgId                 Refers the ID of the organization to which the entity belongs.
     * @param id                    Refers the ID of the entity whose fields are to be resolved.
     * @param workflowCriteriaValue Refers the workflow criteria value used for resolving the entity type fields.
     * @param storeType             Refers the storeType.
     */
    private void resolveEntityTypeFields(final Long orgId, final Long id, final WorkflowCriteriaValue workflowCriteriaValue,
                                         final StoreType storeType) {
        workflowCriteriaValue.setValueId(id);

        switch (storeType) {
            case COMPANY, COMPANY_ONBOARD -> workflowCriteriaValue.setValue(companyManager.getCompanyName(orgId, id));
            case CONTACT, CONTACT_ONBOARD, CONTACTS_ONBOARD ->
                    workflowCriteriaValue.setValue(contactManager.getContactFullName(orgId, id));
            case DEAL -> workflowCriteriaValue.setValue(dealManager.partialGet(orgId, id).getTitle());
            case TAGS -> workflowCriteriaValue.setValue(tagManager.partialGet(orgId, id).getName());
            case TERRITORY -> workflowCriteriaValue.setValue(territoryManager.get(orgId, id).getName());
            case MARKETING_LIST -> workflowCriteriaValue.setValue(marketingListManager.partialGet(orgId, id).getName());
        }
    }

    /**
     * Resolves user conditions for the given workflow based on the provided organization ID and condition array.
     * It uses the user map to associate user data with the conditions.
     *
     * @param orgId          Refers the ID of the organization to which the workflow belongs.
     * @param triggerSource  Refers the workflow {@code TriggerSource}.
     * @param conditionArray A JSON array containing the conditions to be processed.
     */
    public void resolveUserConditions(final long orgId, final TriggerSource triggerSource, final JsonArray conditionArray, final Map<Long, CrmUser> userMap) {
        final int conditionSize = conditionArray.size();

        for (int index = 0; index < conditionSize; index++) {
            final JsonObject criteriaSetJson = conditionArray.getJsonObject(index);

            if (criteriaSetJson.containsKey(WorkflowAttributes.CRITERIA)) {
                final JsonArray jsonArray = criteriaSetJson.getJsonArray(WorkflowAttributes.CRITERIA);
                final int criteriaSize = jsonArray.size();

                for (int criteriaIndex = 0; criteriaIndex < criteriaSize; criteriaIndex++) {
                    final JsonObject criteriaJson = jsonArray.getJsonObject(criteriaIndex);
                    final long fieldId = criteriaJson.getJsonObject(WorkflowAttributes.FIELD).getLong(WorkflowAttributes.ID);
                    final StoreType storeType;

                    if (Objects.equals(TriggerSource.ACTIVITY, triggerSource)) {
                        storeType = workflowFieldService.get(fieldId).getFieldType().storeType();
                    } else {
                        storeType = fieldService.getField(orgId, fieldId).storeType();
                    }

                    if (storeType.isOwnerType()) {
                        final JsonArray valueArray = criteriaJson.optJsonArray(WorkflowAttributes.VALUES);

                        if (Objects.nonNull(valueArray) && valueArray.size() > 0) {
                            final JsonObject valueJson = valueArray.getJsonObject(0);
                            final Object existingValue = valueJson.opt(WorkflowAttributes.VALUE);
                            final JsonObject newValueJson = Json.object();
                            final Long userId = NumberUtils.parseNumber(existingValue).longValue();
                            final CrmUser user = userMap.get(userId);

                            newValueJson.put(WorkflowAttributes.ID, userId);
                            newValueJson.put(WorkflowAttributes.NAME, Objects.nonNull(user) ? user.getFullName() : crmUserService.getUser(orgId, userId).getFullName());

                            valueJson.put(WorkflowAttributes.VALUE, newValueJson);
                        }
                    }
                }
            }
        }
    }
}
