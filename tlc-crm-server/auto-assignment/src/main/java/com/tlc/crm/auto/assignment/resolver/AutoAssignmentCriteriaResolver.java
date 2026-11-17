package com.tlc.crm.auto.assignment.resolver;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;

import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.features.api.tag.manager.TagManager;
import com.tlc.crm.entity.marketinglist.manager.MarketingListManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.DropdownType;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;

import com.tlc.crm.fields.models.StoreType;

import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteria;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteriaValue;
import com.tlc.crm.auto.assignment.fields.AutoAssignmentAttributes;

import com.tlc.i18n.I18nResolutionAdapter;

import java.util.Objects;
import java.util.Optional;

/**
 * A resolver that resolves the {@link AutoAssignmentCriteria} related details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class AutoAssignmentCriteriaResolver {

    private final FieldService fieldService;
    private final PipelineManager pipelineManager;
    private final PipelineStageManager pipelineStageManager;
    private final LifeCycleStageManager lifeCycleStageManager;
    private final OptionService optionService;
    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final TagManager tagManager;
    private final DealManager dealManager;
    private final TerritoryManager territoryManager;
    private final MarketingListManager marketingListManager;

    private AutoAssignmentCriteriaResolver() {
        this.fieldService = FieldService.getInstance();
        this.pipelineManager = PipelineManager.getInstance();
        this.pipelineStageManager = PipelineStageManager.getInstance();
        this.lifeCycleStageManager = LifeCycleStageManager.getInstance();
        this.optionService = OptionService.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.companyManager = CompanyManager.getInstance();
        this.tagManager = TagManager.getInstance();
        this.dealManager = DealManager.getInstance();
        this.territoryManager = TerritoryManager.getInstance();
        this.marketingListManager = MarketingListManager.getInstance();
    }

    private static class Instance {
        private static final AutoAssignmentCriteriaResolver CRITERIA_RESOLVER = new AutoAssignmentCriteriaResolver();
    }

    /**
     * Gets the {@link AutoAssignmentCriteriaResolver} instance.
     *
     * @return {@link AutoAssignmentCriteriaResolver}.
     */
    public static AutoAssignmentCriteriaResolver getInstance() {
        return Instance.CRITERIA_RESOLVER;
    }

    /**
     * Resolve the template criteria values.
     *
     * @param orgId                   Represents the organization id
     * @param autoAssignmentCriteria  It holds the {@link AutoAssignmentCriteria} details
     * @param assignmentCriteriaValue It holds the {@link AutoAssignmentCriteriaValue} details
     */
    public void resolveTemplateCriteria(final Long orgId, final AutoAssignmentCriteria autoAssignmentCriteria,
                                        final AutoAssignmentCriteriaValue assignmentCriteriaValue) {
        final long fieldId = autoAssignmentCriteria.getFieldId();
        final FieldInfo fieldInfo = fieldService.getField(orgId, fieldId);

        if (StoreType.isSelectorType(fieldInfo.storeType())) {
            final DropdownType dropdownType = fieldInfo.presetConfig().dropdownType();

            if (Objects.equals(DropdownType.HIDDEN_OPTION, dropdownType)) {

                switch (fieldInfo.aliasName()) {
                    case AutoAssignmentAttributes.LIFECYCLE_STAGE -> {
                        final LifeCycleStage defaultLifeCycleStage = lifeCycleStageManager.getDefaultStage(orgId);

                        assignmentCriteriaValue.setValue(defaultLifeCycleStage.id());
                    }
                }
            } else {
                final Option defaultOption = optionService.getOption(orgId, fieldInfo, (String) assignmentCriteriaValue.getValue());
                final Option option = Objects.nonNull(defaultOption) ? defaultOption :
                        optionService.getOption(orgId, fieldInfo, (String) assignmentCriteriaValue.getValue());

                if (Objects.nonNull(option)) {
                    assignmentCriteriaValue.setValue(option.id());
                }
            }
        }
    }

    /**
     * Resolve the assignment criteria values.
     *
     * @param orgId                   Represents the organization id.
     * @param fieldId                 Refers the field id.
     * @param assignmentCriteriaValue It holds the {@link AutoAssignmentCriteriaValue} details.
     * @param i18nResolutionAdapter   Represents the resolver of the I18N strings.
     */
    public void resolveCriteria(final Long orgId, final long fieldId, final AutoAssignmentCriteriaValue assignmentCriteriaValue,
            final I18nResolutionAdapter i18nResolutionAdapter) {
        final FieldInfo fieldInfo = fieldService.getField(orgId, fieldId);
        final JsonObject ruleConfig = fieldInfo.presetConfig().getAutomationConfig();
        final StoreType storeType = Optional.ofNullable(ruleConfig)
                .map(config -> StoreType.valueOf(config.getString(AutoAssignmentAttributes.FIELD_TYPE)))
                .orElseGet(fieldInfo::storeType);

        if (StoreType.isSelectorType(storeType)) {
            final Long id = NumberUtils.parseNumber(assignmentCriteriaValue.getValue()).longValue();
            final DropdownType dropdownType = Optional.ofNullable(ruleConfig)
                    .map(config -> DropdownType.valueOf(config.getString(AutoAssignmentAttributes.DROPDOWN_TYPE)))
                    .orElseGet(() -> fieldInfo.presetConfig().dropdownType());

            if (Objects.equals(DropdownType.HIDDEN_OPTION, dropdownType)) {

                switch (fieldInfo.aliasName()) {
                    case AutoAssignmentAttributes.PIPELINE -> {
                        final Pipeline pipeline = pipelineManager.get(orgId, id);

                        assignmentCriteriaValue.setValueEntityId(pipeline.id());
                        assignmentCriteriaValue.setValue(pipeline.getName());
                    }
                    case AutoAssignmentAttributes.PIPELINE_STAGE -> {
                        final PipelineStage pipelineStage = pipelineStageManager.get(orgId, id);

                        assignmentCriteriaValue.setValueEntityId(pipelineStage.id());
                        assignmentCriteriaValue.setValue(pipelineStage.getName());
                    }
                    case AutoAssignmentAttributes.LIFECYCLE_STAGE -> {
                        final LifeCycleStage lifeCycleStage = lifeCycleStageManager.get(orgId, id);

                        assignmentCriteriaValue.setValueEntityId(lifeCycleStage.id());
                        assignmentCriteriaValue.setValue(lifeCycleStage.getName());
                    }
                    case AutoAssignmentAttributes.LIFECYCLE_STATUS -> {
                        final LifeCycleStatus lifecycleStatus = LifeCycleStatusManager.getInstance().get(orgId, id);

                        assignmentCriteriaValue.setValueEntityId(lifecycleStatus.id());
                        assignmentCriteriaValue.setValue(lifecycleStatus.getName());
                    }
                    case AutoAssignmentAttributes.LAST_ACTIVITY_TYPE -> {
                        final ActivityType activityType = ActivityTypeManager.getInstance().get(orgId, id);

                        assignmentCriteriaValue.setValueEntityId(activityType.id());
                        assignmentCriteriaValue.setValue(activityType.getName());
                    }
                    case AutoAssignmentAttributes.STATUS -> {
                        final Deal.State dealState = Deal.State.getStateById(id.intValue());

                        assignmentCriteriaValue.setValueEntityId(dealState.id());
                        assignmentCriteriaValue.setValue(dealState.getDisplayName());
                    }
                }
            } else {
                final Option option = optionService.get(orgId, fieldId, id);

                if (Objects.nonNull(option)) {
                    assignmentCriteriaValue.setValueEntityId(option.id());
                    assignmentCriteriaValue.setValue(i18nResolutionAdapter.get(option.getOption()));
                }
            }
        } else if (storeType.isEntityType() || StoreType.TAGS == storeType || StoreType.TERRITORY == storeType || StoreType.MARKETING_LIST == storeType) {
            final Long id = NumberUtils.parseNumber(assignmentCriteriaValue.getValue()).longValue();

            resolveEntityTypeFields(orgId, id, assignmentCriteriaValue, storeType);
        }
    }

    /**
     * Resolves the entity type fields based on the provided organization ID, entity ID, and auto-assignment criteria value.
     *
     * @param orgId                   Refers the ID of the organization to which the entity belongs.
     * @param id                      Refers the ID of the entity whose fields are to be resolved.
     * @param assignmentCriteriaValue Refers the auto-assignment criteria value used for resolving the entity type fields.
     * @param storeType               Refers the storeType.
     */
    private void resolveEntityTypeFields(final Long orgId, final Long id, final AutoAssignmentCriteriaValue assignmentCriteriaValue,
                                         final StoreType storeType) {
        assignmentCriteriaValue.setValueEntityId(id);

        switch (storeType) {
            case COMPANY, COMPANY_ONBOARD -> assignmentCriteriaValue.setValue(companyManager.getCompanyName(orgId, id));
            case CONTACT, CONTACT_ONBOARD, CONTACTS_ONBOARD ->
                    assignmentCriteriaValue.setValue(contactManager.getContactFullName(orgId, id));
            case DEAL -> assignmentCriteriaValue.setValue(dealManager.partialGet(orgId, id).getTitle());
            case TAGS -> assignmentCriteriaValue.setValue(tagManager.partialGet(orgId, id).getName());
            case MARKETING_LIST -> assignmentCriteriaValue.setValue(marketingListManager.partialGet(orgId, id).getName());
            case TERRITORY -> assignmentCriteriaValue.setValue(territoryManager.get(orgId, id).getName());
        }
    }

}