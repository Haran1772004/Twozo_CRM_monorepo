package com.tlc.crm.core.api.handler;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.entity.timeline.field.TimelineActionFields;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.entity.timeline.model.TimelineType;
import com.tlc.i18n.I18nKey;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.sql.common.api.DataContainer;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tlc.crm.entity.timeline.model.TimelineType.CONTACT_LIFE_CYCLE_LOST_REASON_SET_UPON_DEAL_CREATION;
import static com.tlc.crm.entity.timeline.model.TimelineType.CONTACT_LIFE_CYCLE_LOST_REASON_SET_UPON_DEAL_WON;

/**
 * <p>
 * Handles updates to contact lifecycle timelines based on various lifecycle events
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class LifeCycleTimelineUpdateHandlerImpl implements LifeCycleTimelineUpdateHandler {

    private final LifeCycleStageManager lifeCycleStageManager;
    private final LifeCycleStatusManager lifeCycleStatusManager;
    private final ContactManager contactManager;
    private final FieldService fieldService;
    private final OptionService optionService;

    private LifeCycleTimelineUpdateHandlerImpl() {
        lifeCycleStageManager = LifeCycleStageManager.getInstance();
        lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
        contactManager = ContactManager.getInstance();
        fieldService = FieldService.getInstance();
        optionService = OptionService.getInstance();
    }

    private static class InstanceHolder {
        private static final LifeCycleTimelineUpdateHandlerImpl INSTANCE = new LifeCycleTimelineUpdateHandlerImpl();
    }

    /**
     * <p>
     * Gets the {@link LifeCycleTimelineUpdateHandlerImpl} instance
     * </p>
     *
     * @return The {@link LifeCycleTimelineUpdateHandlerImpl} instance
     */
    public static LifeCycleTimelineUpdateHandlerImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param contacts Which contain a {@link Collection} of {@link Contact}
     */
    @Override
    public void updateLifeCycleOnDealCreation(final long orgId, final Collection<Contact> contacts) {
        processLifecycleUpdates(orgId, contacts,
                TimelineType.CONTACT_LIFE_CYCLE_STAGE_UPDATED_UPON_DEAL_CREATION,
                TimelineType.CONTACT_LIFE_CYCLE_STATUS_UPDATED_UPON_DEAL_CREATION,
                TimelineType.CONTACT_LIFE_CYCLE_LOST_REASON_UPDATED_UPON_DEAL_CREATION);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param contacts Which contain a {@link Collection} of {@link Contact}
     */
    @Override
    public void updateLifeCycleOnDealWon(final long orgId, final Collection<Contact> contacts) {
        processLifecycleUpdates(orgId, contacts,
                TimelineType.CONTACT_LIFE_CYCLE_STAGE_UPDATED_UPON_DEAL_WON,
                TimelineType.CONTACT_LIFE_CYCLE_STATUS_UPDATED_UPON_DEAL_WON,
                TimelineType.CONTACT_LIFE_CYCLE_LOST_REASON_UPDATED_UPON_DEAL_WON);
    }

    /**
     * <p>
     * Processes lifecycle updates for a {@link  Collection} of {@link Contact}
     * </p>
     *
     * @param orgId                  Represents an organization id
     * @param newContacts            Which contains the collection of {@link Contact}
     * @param stageTimelineType      Represents the stage {@link TimelineType}
     * @param statusTimelineType     Represents the status {@link TimelineType}
     * @param lostReasonTimelineType Represents the lostReason {@link TimelineType}
     */
    private void processLifecycleUpdates(final long orgId, final Collection<Contact> newContacts,
                                         final TimelineType stageTimelineType,
                                         final TimelineType statusTimelineType,
                                         final TimelineType lostReasonTimelineType) {
        final Set<Long> ids = newContacts.stream().map(Contact::id).collect(Collectors.toSet());
        final Map<Long, Contact> oldContacts = contactManager.fetchContactsWithLifeCycleDetails(orgId, ids).stream()
                .collect(Collectors.toMap(Contact::id, Function.identity()));
        final String lifeCycleStageKey = ContactFields.LIFECYCLE_STAGE;
        final String lifeCycleStatusKey = ContactFields.LIFECYCLE_STATUS;
        final String lostReasonKey = ContactFields.CONTACT_LOST_REASON;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE,
                List.of(lifeCycleStageKey, lifeCycleStatusKey, lostReasonKey));
        final DataContainer targetContainer = DataContainer.create();

        for (final Contact contact : newContacts) {
            final Long contactId = contact.id();

            if (oldContacts.containsKey(contactId)) {
                final Contact oldContact = oldContacts.get(contactId);
                final Option newLostReason = contact.getLostReason();
                final LifeCycleStage oldLifeCycleStage = oldContact.getLifeCycleStage();
                final LifeCycleStatus oldLifeCycleStatus = oldContact.getLifeCycleStatus();
                final LifeCycleStage newtLifeCycleStage = contact.getLifeCycleStage();
                final LifeCycleStatus newtLifeCycleStatus = contact.getLifeCycleStatus();

                if (Objects.nonNull(newtLifeCycleStage) && Objects.nonNull(oldLifeCycleStage)) {
                    final Long newtLifeCycleStageId = newtLifeCycleStage.id();

                    if (!Objects.equals(oldLifeCycleStage.id(), newtLifeCycleStageId)) {
                        final Timeline timeline = buildTimeline(contact);
                        final LifeCycleStage lifeCycleStage = lifeCycleStageManager.partialGet(orgId, newtLifeCycleStageId);

                        timeline.setTimelineType(stageTimelineType);
                        loadFieldTimeline(timeline, fields.get(lifeCycleStageKey).i18nName(),
                                oldLifeCycleStage.getName(), lifeCycleStage.getName());
                        contactManager.loadContactTimelineRowIntoContainer(timeline, contactId, targetContainer);
                    }
                }

                if (Objects.nonNull(newtLifeCycleStatus) && Objects.nonNull(oldLifeCycleStatus)) {
                    final Long newtLifeCycleStatusId = newtLifeCycleStatus.id();

                    if (!Objects.equals(oldLifeCycleStatus.id(), newtLifeCycleStatusId)) {
                        final Timeline timeline = buildTimeline(contact);
                        final LifeCycleStatus lifeCycleStatus = lifeCycleStatusManager.partialGet(orgId, newtLifeCycleStatusId);

                        timeline.setTimelineType(statusTimelineType);
                        loadFieldTimeline(timeline, fields.get(lifeCycleStatusKey).i18nName(),
                                oldLifeCycleStatus.getName(), lifeCycleStatus.getName());
                        contactManager.loadContactTimelineRowIntoContainer(timeline, contactId, targetContainer);
                    }
                }

                if (Objects.nonNull(newLostReason)) {
                    final Option oldLostReason = oldContact.getLostReason();
                    final String oldLostReasonName = Objects.isNull(oldLostReason) ? null : oldLostReason.getOption();
                    final FieldInfo lostReasonField = fields.get(lostReasonKey);
                    final Option newLostReasonOption = optionService.get(orgId, lostReasonField.id(), newLostReason.id());
                    final String newLostReasonName = newLostReasonOption.getOption();

                    if (!Objects.equals(oldLostReasonName, newLostReasonName)) {
                        final Timeline timeline = buildTimeline(contact);
                        final I18nKey lostReasonFieldI18nName = lostReasonField.i18nName();

                        if (Objects.isNull(oldLostReason)) {
                            timeline.setTimelineType(mapTimelineType(lostReasonTimelineType));
                            timeline.setAction(TimelineActionFields.SET);
                            loadFieldTimeline(timeline, lostReasonFieldI18nName, null, newLostReasonName);
                        } else {
                            timeline.setTimelineType(lostReasonTimelineType);
                            loadFieldTimeline(timeline, lostReasonFieldI18nName, oldLostReasonName, newLostReasonName);
                        }
                        contactManager.loadContactTimelineRowIntoContainer(timeline, contactId, targetContainer);
                    }
                }
            }
        }
        contactManager.updateLifeCycle(orgId, newContacts, targetContainer);
    }

    /**
     * <p>
     * Maps the {@link TimelineType}
     * </p>
     *
     * @param timelineType Represents the {@link TimelineType}
     * @return The {@link TimelineType}
     */
    private TimelineType mapTimelineType(final TimelineType timelineType) {
        return switch (timelineType) {
            case CONTACT_LIFE_CYCLE_LOST_REASON_UPDATED_UPON_DEAL_CREATION ->
                    CONTACT_LIFE_CYCLE_LOST_REASON_SET_UPON_DEAL_CREATION;
            case CONTACT_LIFE_CYCLE_LOST_REASON_UPDATED_UPON_DEAL_WON ->
                    CONTACT_LIFE_CYCLE_LOST_REASON_SET_UPON_DEAL_WON;
            default -> throw ErrorCode.get(EntityErrorCodes.INVALID_TIMELINE_TYPE);
        };
    }

    /**
     * <p>
     * Loads the field timeline into {@link Timeline}
     * </p>
     *
     * @param timeline  Represents the {@link Timeline}
     * @param fieldName Represents the field name
     * @param oldValue  Represents the previous value
     * @param newValue  Represents the new value
     */
    private void loadFieldTimeline(final Timeline timeline, final I18nKey fieldName, final String oldValue,
                                   final String newValue) {

        if (!Objects.equals(oldValue, newValue)) {
            final FieldTimeline fieldTimeline = new FieldTimeline(Objects.isNull(oldValue)
                    ? Collections.emptyList() : List.of(oldValue), newValue, fieldName);

            timeline.setFieldTimeline(fieldTimeline);
        }
    }

    /**
     * <p>
     * Builds the {@link Timeline}
     * </p>
     *
     * @param contact Represents the {@link Contact}
     * @return The  {@link Timeline}
     */
    private Timeline buildTimeline(final Contact contact) {
        final Timeline timeline = new Timeline();

        timeline.setOrgId(contact.orgId());
        timeline.setCategory(Timeline.Category.CONTACT);
        timeline.setAction(TimelineActionFields.UPDATED);
        timeline.setOwnerId(contact.getUpdatedBy().id());
        timeline.setAuditedTime(new DateTime(Instant.now().toEpochMilli()));

        return timeline;
    }
}