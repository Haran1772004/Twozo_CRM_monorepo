package com.tlc.crm.core.internal.contact.handler;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.LifeCycleRuleDetails;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.notification.email.settings.api.EmailSettingsManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * <p>
 * Handles the lifecycle stage/status migrate event based on deleting the lifecycle stage/status.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class LifeCycleMigrationHandler {

    private static final byte DEFAULT_STARTING_INDEX = 2;

    private final ContactManager contactManager;
    private final LifeCycleStageManager stageManager;
    private final LifeCycleStatusManager statusManager;
    private final OptionService optionService;
    private final FieldService fieldService;
    private final EmailSettingsManager emailSettingsManager;

    private LifeCycleMigrationHandler() {
        contactManager = ContactManager.getInstance();
        stageManager = LifeCycleStageManager.getInstance();
        statusManager = LifeCycleStatusManager.getInstance();
        optionService = OptionService.getInstance();
        fieldService = FieldService.getSecureInstance();
        emailSettingsManager = EmailSettingsManager.getInstance();
    }

    private static class Instance {
        private static final LifeCycleMigrationHandler INSTANCE = new LifeCycleMigrationHandler();
    }

    /**
     * <p>
     * Provides the {@link LifeCycleMigrationHandler}.
     * </p>
     *
     * @return {@link LifeCycleMigrationHandler}
     */
    public static LifeCycleMigrationHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Handles the lifecycle stage update event of the contact based on the deleting the lifecycle stage.
     * </p>
     *
     * @param orgId    Represents the organisation id.
     * @param stageIds Holds the details of the lifecycle stage ids.
     */
    public void handleStageRemoval(final Long orgId, final Collection<Long> stageIds) {
        Long defaultLifeCycleStageId = stageManager.getDefaultStageId(orgId);

        for (int index = DEFAULT_STARTING_INDEX; stageIds.contains(defaultLifeCycleStageId); index++) {
            defaultLifeCycleStageId = stageManager.getDefaultStageId(orgId, index);
        }
        final LifeCycleStatus defaultLifeCycleStatus = statusManager.getDefaultStatus(orgId, defaultLifeCycleStageId);
        final Predicate<LifeCycleRuleDetails> stageExistsPredicate = lifeCycleRule -> stageIds.contains(lifeCycleRule.getStage().id());
        final Long defaultStatusId = defaultLifeCycleStatus.id();
        final Option lostReason = fetchDefaultLostReason(orgId, defaultLifeCycleStatus.getLabel());

        contactManager.updateLifecycleStage(orgId, stageIds, defaultLifeCycleStageId, defaultStatusId, lostReason);
        migrateRules(orgId, defaultLifeCycleStageId, defaultStatusId, stageExistsPredicate);
        migrateEmailSettings(orgId, stageIds, Collections.emptySet(), defaultLifeCycleStageId, defaultStatusId);
    }

    /**
     * <p>
     * Handles the lifecycle status update event of the contact based on the deleting the lifecycle status.
     * </p>
     *
     * @param orgId     Represents the organisation id.
     * @param statusIds Holds the details of the lifecycle status ids.
     */
    public void handleStatusRemoval(final Long orgId, final Collection<Long> statusIds) {
        final Long defaultLifeCycleStageId = stageManager.getDefaultStageId(orgId);
        LifeCycleStatus defaultLifeCycleStatus = statusManager.getDefaultStatus(orgId, defaultLifeCycleStageId);

        for (int index = DEFAULT_STARTING_INDEX; Objects.nonNull(defaultLifeCycleStatus) && statusIds.contains(defaultLifeCycleStatus.id()); index++) {
            defaultLifeCycleStatus = statusManager.getDefaultStatus(orgId, defaultLifeCycleStageId, index);
        }

        if (Objects.nonNull(defaultLifeCycleStatus)) {
            migrateContactAndRules(orgId, statusIds, defaultLifeCycleStageId, defaultLifeCycleStatus);
        }
    }

    /**
     * <p>
     * Migrates the contacts and rules to the default lifecycle stage and status.
     * </p>
     *
     * @param orgId                    Represents the organisation id.
     * @param statusIdsToBeMigrated    Holds the lifecycle status ids to be migrated.
     * @param defaultLifeCycleStageId  Represents the default lifecycle stage id.
     * @param defaultLifeCycleStatus   Represents the default {@link LifeCycleStatus}.
     */
    public void migrateContactAndRules(final Long orgId, final Collection<Long> statusIdsToBeMigrated,
                                       final Long defaultLifeCycleStageId, final LifeCycleStatus defaultLifeCycleStatus) {
        final Long defaultStatusId = defaultLifeCycleStatus.id();
        final Option lostReason = fetchDefaultLostReason(orgId, defaultLifeCycleStatus.getLabel());

        contactManager.updateLifecycleStatus(orgId, statusIdsToBeMigrated, defaultLifeCycleStageId, defaultStatusId, lostReason);
        migrateRules(orgId, defaultLifeCycleStageId, defaultStatusId, lifeCycleRule -> statusIdsToBeMigrated.contains(lifeCycleRule.getStatus().id()));
        migrateEmailSettings(orgId, Collections.emptySet(), statusIdsToBeMigrated, defaultLifeCycleStageId, defaultStatusId);
    }

    /**
     * <p>
     * Migrates the lifecycle stage rules based on the lifecycle stage/status delete event.
     * </p>
     *
     * @param orgId                    Represents the organisation id.
     * @param defaultLifeCycleStageId  Represents the default lifecycle stage id.
     * @param defaultLifeCycleStatusId Represents the default lifecycle status id.
     * @param existsCheckPredicate     Represents the {@link Predicate} to check whether the given rule is associated with the deleted stage/status.
     */
    private void migrateRules(final Long orgId, final Long defaultLifeCycleStageId, final Long defaultLifeCycleStatusId,
                              final Predicate<LifeCycleRuleDetails> existsCheckPredicate) {
        final Collection<LifeCycleRuleDetails> lifeCycleRuleDetails = stageManager.getRules(orgId);
        final LifeCycleStage lifeCycleStage = new LifeCycleStage();
        final LifeCycleStatus lifeCycleStatus = new LifeCycleStatus();

        lifeCycleStage.setId(defaultLifeCycleStageId);
        lifeCycleStatus.setId(defaultLifeCycleStatusId);
        lifeCycleRuleDetails.stream().filter(existsCheckPredicate).forEach(lifeCycleRule -> {
            lifeCycleRule.setStage(lifeCycleStage);
            lifeCycleRule.setStatus(lifeCycleStatus);
        });
        stageManager.updateRules(orgId, lifeCycleRuleDetails);
    }

    /**
     * <p>
     * Migrates the per-user email linker settings (the "Auto create person with stage" selection in
     * Email Settings) whenever the lifecycle stage/status they reference is deleted. Without this,
     * a deleted stage/status leaves the stored reference dangling and the field renders empty
     * instead of falling back to the default stage/status.
     * </p>
     *
     * @param orgId                    Represents the organisation id.
     * @param stageIdsToBeMigrated     Holds the lifecycle stage ids being removed, or an empty collection if a stage removal is not in progress.
     * @param statusIdsToBeMigrated    Holds the lifecycle status ids being removed, or an empty collection if a status removal is not in progress.
     * @param defaultLifeCycleStageId  Represents the default lifecycle stage id to fall back to.
     * @param defaultLifeCycleStatusId Represents the default lifecycle status id to fall back to.
     */
    private void migrateEmailSettings(final Long orgId, final Collection<Long> stageIdsToBeMigrated, final Collection<Long> statusIdsToBeMigrated,
                                      final Long defaultLifeCycleStageId, final Long defaultLifeCycleStatusId) {
        emailSettingsManager.migrateEmailLinkerLifecycleReference(orgId, stageIdsToBeMigrated, statusIdsToBeMigrated,
                defaultLifeCycleStageId, defaultLifeCycleStatusId);
    }

    /**
     * <p>
     * Fetches the default lost reason of the contact.
     * </p>
     *
     * @param orgId Represents the organisation id.
     * @param label Represents the label of the status.
     * @return Default lost reason.
     */
    private Option fetchDefaultLostReason(final long orgId, final LifeCycleStatus.Label label) {
        if (LifeCycleStatus.Label.CLOSED_LOST == label) {
            final String lostReasonKey = ContactFields.CONTACT_LOST_REASON;
            final FieldInfo lostReasonField = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, lostReasonKey).get(lostReasonKey);

            return optionService.getDefaultOption(orgId, lostReasonField);
        }
        return null;
    }
}