package com.tlc.crm.core.fields.builder;

import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.Contact.SubscriptionStatus;
import com.tlc.crm.core.api.contact.model.Contact.UnsubscribeReason;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.internal.FieldVisibilityCriteriaGroup;
import com.tlc.crm.entity.models.internal.FieldVisibilityCriteriaGroup.Connector;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Handles the {@link FieldVisibilityCriteriaGroup} related to {@link Contact} based on the {@link FieldInfo}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public final class ContactFieldVisibilityHandler implements FieldVisibilityHandler {

    private final FieldService fieldService;
    private final OptionService optionService;
    private final LifeCycleStageManager stageManager;

    private static class Instance {
        private static final ContactFieldVisibilityHandler INSTANCE = new ContactFieldVisibilityHandler();
    }

    private ContactFieldVisibilityHandler() {
        fieldService = FieldService.getSecureInstance();
        optionService = OptionService.getInstance();
        stageManager = LifeCycleStageManager.getInstance();
    }

    /**
     * <p>
     * Gets the {@link ContactFieldVisibilityHandler} Instance
     * </p>
     *
     * @return {@link ContactFieldVisibilityHandler}
     */
    public static ContactFieldVisibilityHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents an organization id
     * @param fieldInfo Represents the {@link FieldInfo}
     * @return {@link FieldVisibilityCriteriaGroup}
     */
    @Override
    public Optional<FieldVisibilityCriteriaGroup> getCriteriaGroup(final long orgId, final FieldInfo fieldInfo) {
        final String subscriptionStatusKey = ContactFields.SUBSCRIPTION_STATUS;
        final String unsubscribeReasonKey = ContactFields.UNSUBSCRIBE_REASON;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, List.of(subscriptionStatusKey, unsubscribeReasonKey));
        final FieldInfo subscriptionStatusFieldInfo = fields.get(subscriptionStatusKey);
        final FieldVisibilityCriteriaGroup fieldVisibilityCriteriaGroup = new FieldVisibilityCriteriaGroup();

        switch (fieldInfo.aliasName()) {
            case "subscriptionTypes" ->
                    fieldVisibilityCriteriaGroup.setChildFieldVisibilityCriteriaGroups(List.of(buildCriteriaGroup(subscriptionStatusFieldInfo.id(),
                            optionService.getOption(orgId, subscriptionStatusFieldInfo, SubscriptionStatus.SUBSCRIBED.getName()).id())));
            case "unsubscribeReason" ->
                    fieldVisibilityCriteriaGroup.setChildFieldVisibilityCriteriaGroups(List.of(buildCriteriaGroup(subscriptionStatusFieldInfo.id(),
                            optionService.getOption(orgId, subscriptionStatusFieldInfo, SubscriptionStatus.UNSUBSCRIBED.getName()).id())));
            case "otherUnsubscribeReason" -> {
                final FieldInfo unsubscribeReasonFieldInfo = fields.get(unsubscribeReasonKey);
                final Collection<FieldVisibilityCriteriaGroup> childFieldVisibilityCriteriaGroups = List.of(buildCriteriaGroup(subscriptionStatusFieldInfo.id(),
                                optionService.getOption(orgId, subscriptionStatusFieldInfo, SubscriptionStatus.UNSUBSCRIBED.getName()).id()),
                        buildCriteriaGroup(unsubscribeReasonFieldInfo.id(), optionService.getOption(orgId, unsubscribeReasonFieldInfo,
                                UnsubscribeReason.OTHER_UNSUBSCRIBE_REASON.getName()).id())
                );

                fieldVisibilityCriteriaGroup.setChildFieldVisibilityCriteriaGroups(childFieldVisibilityCriteriaGroups);
                fieldVisibilityCriteriaGroup.setConnector(Connector.AND);
            }
            case "contactLostReason" -> setCriteriaGroup(orgId, fieldVisibilityCriteriaGroup, LifeCycleStatus.Label.CLOSED_LOST);
        }

        return Optional.of(fieldVisibilityCriteriaGroup);
    }

    /**
     * <p>
     * Sets the {@link FieldVisibilityCriteriaGroup} based on the given labels.
     * </p>
     *
     * @param orgId  Represents an organization id.
     * @param labels Represents the label of the lifecycle status.
     * @return {@link FieldVisibilityCriteriaGroup}.
     */
    private void setCriteriaGroup(final long orgId, final FieldVisibilityCriteriaGroup fieldVisibilityCriteriaGroup, final LifeCycleStatus.Label... labels) {
        final Map<Long, Collection<Long>> lifecycleStages = stageManager.getStagesByLabels(orgId, List.of(labels));
        final Collection<FieldVisibilityCriteriaGroup> fieldVisibilityCriteriaGroups = new ArrayList<>();
        final String lifecycleStageFieldKey = ContactFields.LIFECYCLE_STAGE;
        final String lifecycleStatusFieldKey = ContactFields.LIFECYCLE_STATUS;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, List.of(lifecycleStageFieldKey, lifecycleStatusFieldKey));
        final Long lifecycleStageFieldId = fields.get(lifecycleStageFieldKey).id();
        final Long lifecycleStatusFieldId = fields.get(lifecycleStatusFieldKey).id();
        final Connector orConnector = Connector.OR;
        final Connector andConnector = Connector.AND;

        for (final Map.Entry<Long, Collection<Long>> lifecycleStage : lifecycleStages.entrySet()) {
            final FieldVisibilityCriteriaGroup childFieldVisibilityCriteriaGroup = new FieldVisibilityCriteriaGroup();
            final Collection<FieldVisibilityCriteriaGroup> childFieldVisibilityCriteriaGroups = new ArrayList<>();

            childFieldVisibilityCriteriaGroups.add(buildCriteriaGroup(lifecycleStageFieldId, lifecycleStage.getKey()));
            final Collection<FieldVisibilityCriteriaGroup> stagesCriteriaGroups = lifecycleStage.getValue().stream()
                    .map(lifecycleStatusId -> buildCriteriaGroup(lifecycleStatusFieldId, lifecycleStatusId)).toList();

            if (labels.length > 1) {
                final FieldVisibilityCriteriaGroup stagesCriteriaGroup = new FieldVisibilityCriteriaGroup();

                stagesCriteriaGroup.setConnector(orConnector);
                stagesCriteriaGroup.setChildFieldVisibilityCriteriaGroups(stagesCriteriaGroups);
                childFieldVisibilityCriteriaGroups.add(stagesCriteriaGroup);
            } else {
                childFieldVisibilityCriteriaGroups.addAll(stagesCriteriaGroups);
            }

            childFieldVisibilityCriteriaGroup.setChildFieldVisibilityCriteriaGroups(childFieldVisibilityCriteriaGroups);
            childFieldVisibilityCriteriaGroup.setConnector(andConnector);
            fieldVisibilityCriteriaGroups.add(childFieldVisibilityCriteriaGroup);
        }

        if (lifecycleStages.size() > 1) {
            fieldVisibilityCriteriaGroup.setConnector(orConnector);
        }

        fieldVisibilityCriteriaGroup.setChildFieldVisibilityCriteriaGroups(fieldVisibilityCriteriaGroups);
    }
}
