package com.tlc.crm.core.notification.email.bulk.service.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.api.manager.addon.team.TeamInboxManager;
import com.tlc.crm.core.notification.email.bulk.service.DeleteMode;
import com.tlc.crm.core.notification.email.bulk.service.EntityThreadService;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.core.notification.email.internal.InboundEmailService;
import com.tlc.crm.core.notification.email.internal.OutboundEmailService;
import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;
import com.tlc.crm.email.notification.email.EmailDispatcher;
import com.tlc.crm.email.notification.email.api.model.config.EmailSyncConfig;
import com.tlc.crm.email.notification.email.sync.api.model.EmailSyncSettings;
import com.tlc.crm.email.notification.email.sync.api.EmailSyncManager;
import com.tlc.crm.email.notification.email.sync.internal.async.service.EmailSyncSettingsService;
import com.tlc.email.common.model.EmailStateTransitionInfo;
import com.tlc.email.common.model.type.Email;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.tlc.email.common.model.type.Email.State.ACTIVE;
import static com.tlc.email.common.model.type.Email.State.ARCHIVE;
import static com.tlc.email.common.model.type.Email.State.INACTIVE;
import static com.tlc.email.common.model.type.Email.State.READ;
import static com.tlc.email.common.model.type.Email.State.STARRED;
import static com.tlc.email.common.model.type.Email.State.UNARCHIVE;

/**
 * <p>
 * Implementation of {@link EntityThreadService} which provides functionalities to update the state and delete the entity threads.
 * </p>
 *
 * @author Mohammed Asif J
 * @version 1.0
 */
public class EntityThreadServiceImpl implements EntityThreadService {

    private final CoreMailManager mailManager;
    private final EmailDispatcher emailDispatcher;
    private final InboundEmailService inboundEmailService;
    private final OutboundEmailService outboundEmailService;
    private final TeamInboxManager teamInboxManager;
    private final EmailSyncSettingsService emailSyncSettingsService;

    private EntityThreadServiceImpl() {
        this.mailManager = CoreMailManager.getInstance();
        this.emailDispatcher = EmailDispatcher.get();
        this.inboundEmailService = InboundEmailService.getInstance();
        this.outboundEmailService = OutboundEmailService.getInstance();
        this.teamInboxManager = TeamInboxManager.getInstance();
        this.emailSyncSettingsService = EmailSyncSettingsService.getInstance();
    }

    public static EntityThreadService getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static class InstanceHolder {
        private static final EntityThreadServiceImpl INSTANCE = new EntityThreadServiceImpl();
    }

    /**
     * <p>
     * Updates the state of email threads based on the provided data.
     * The state can be active, read, archived, or starred.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a user id
     * @param ids    Represents a set of thread ids to be updated
     * @param data   Represents a {@link JsonObject} which contains the state information to be updated
     * @return A collection of thread ids that were updated
     */
    @Override
    public Collection<Long> updateState(final long orgId, final Long userId,
                                        final Set<Long> ids, final JsonObject data) {

        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        if (data.containsKey(CoreEmailFields.IS_DONE.getName())) {

            if (mailManager.isThreadExists(orgId, userId, ids, true)) {
                inboundEmailService.updateAwaitingStatus(orgId, userId, ids, false);
            } else {
                throw ErrorCode.get(EmailErrorCodes.THREAD_NOT_FOUND,
                        "i18n.notifications.mail.thread.not.found");
            }

            return ids;
        } else {
            final String isActiveKey = CoreEmailFields.IS_ACTIVE.getName();
            final String isArchiveKey = CoreEmailFields.IS_ARCHIVED.getName();
            final String isReadKey = CoreEmailFields.IS_READ.getName();
            final String isStarredKey = CoreEmailFields.IS_STARRED.getName();
            final Set<Long> threadIds = new HashSet<>(ids);

            if (data.containsKey(isActiveKey)) {
                final boolean isActive = data.getBoolean(isActiveKey);

                updateThreadState(orgId, userId, threadIds,
                        extractAndFilterTeamInboxThreadIds(orgId, threadIds), isActive, ACTIVE);

                if (!threadIds.isEmpty()) {
                    updateStateInMailbox(orgId, userId, threadIds, isActive ? ACTIVE : INACTIVE);
                }
            } else if (data.containsKey(isArchiveKey)) {
                final boolean isArchived = data.getBoolean(isArchiveKey);

                updateStateInMailbox(orgId, userId, threadIds, isArchived ? ARCHIVE : UNARCHIVE);
                mailManager.updateThread(orgId, userId, isArchived, threadIds, ARCHIVE);
            } else if (data.containsKey(isReadKey)) {
                updateThreadState(orgId, userId, threadIds,
                        extractAndFilterTeamInboxThreadIds(orgId, threadIds), data.getBoolean(isReadKey), READ);
            } else if (data.containsKey(isStarredKey)) {
                mailManager.updateThread(orgId, userId, data.getBoolean(isStarredKey), threadIds, STARRED);
            } else {
                throw ErrorCode.get(EmailErrorCodes.INVALID_INPUT, "i18n.notifications.mail.input.invalid");
            }
        }

        return ids;
    }

    /**
     * <p>
     * Updates the state of email threads for both user and team inbox based on the provided parameters.
     * </p>
     *
     * @param orgId              Represents an organization id
     * @param userId             Represents a user id
     * @param threadIds          Represents a set of thread ids to be updated for the user
     * @param teamInboxThreadIds Represents a set of thread ids to be updated for the team inbox
     * @param userDefinedState   Represents the state defined by the user to be updated
     * @param state              Represents the state to be updated in mailbox if sync is active
     */
    private void updateThreadState(final long orgId, final Long userId, final Set<Long> threadIds,
                                   final Set<Long> teamInboxThreadIds, final boolean userDefinedState, final Email.State state) {

        if (!teamInboxThreadIds.isEmpty()) {
            mailManager.updateConversation(orgId, null, userDefinedState, teamInboxThreadIds, state);
        }

        if (!threadIds.isEmpty()) {
            mailManager.updateConversation(orgId, userId, userDefinedState, threadIds, state);
        }
    }

    /**
     * <p>
     * Deletes the email threads based on the provided delete mode.
     * It supports both soft delete (moving to trash) for all emails and draft delete (permanently deleting).
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents a user id
     * @param ids        Represents a set of thread ids to be deleted
     * @param deleteMode Represents the delete operation mode
     * @return A collection of thread ids that were deleted
     */
    @Override
    public Collection<Long> delete(final long orgId, final Long userId, final Set<Long> ids, final DeleteMode deleteMode) {

        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        final Set<Long> threadIds = new HashSet<>(ids);
        final Set<Long> teamInboxThreadIds = extractAndFilterTeamInboxThreadIds(orgId, threadIds);
        final Set<Long> deletedThreadIds = new HashSet<>();

        switch (deleteMode) {
            case SOFT -> {

                if (!teamInboxThreadIds.isEmpty()) {
                    deleteTrashThreads(orgId, null, teamInboxThreadIds);
                    deletedThreadIds.addAll(teamInboxThreadIds);
                }

                if (!threadIds.isEmpty()) {
                    deleteTrashThreads(orgId, userId, threadIds);
                    deletedThreadIds.addAll(threadIds);
                }
            }
            case HARD -> {

                if (!teamInboxThreadIds.isEmpty()) {
                    deletedThreadIds.addAll(outboundEmailService.discardDrafts(orgId, null, teamInboxThreadIds));
                }

                if (!threadIds.isEmpty()) {
                    deletedThreadIds.addAll(outboundEmailService.discardDrafts(orgId, userId, threadIds));
                }
            }
        }

        return deletedThreadIds;
    }


    /**
     * <p>
     * Updates the mail status in the respective mailboxes also
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param userId    Represents a user id
     * @param threadIds Represents the thread ids
     * @param newState  Represents the state to be updated
     */
    private void updateStateInMailbox(final Long orgId, final Long userId,
                                      final Collection<Long> threadIds, final Email.State newState) {
        final Map<String, Collection<EmailStateTransitionInfo>> emailStateTransitionInfo = new HashMap<>();

        mailManager.fillStateTransitionDetailsByThreadIds(orgId, userId, threadIds, newState, emailStateTransitionInfo);
        final Map<String, Collection<EmailStateTransitionInfo>> filteredForDispatch = new HashMap<>();
        final EmailSyncManager emailSyncManager = EmailSyncManager.getInstance();
        final Map<String, Collection<EmailSyncSettings>> partialSettings = emailSyncManager.getPartialSettings(orgId, emailStateTransitionInfo.keySet());

        for (final Map.Entry<String, Collection<EmailStateTransitionInfo>> entry : emailStateTransitionInfo.entrySet()) {
            final String emailAddress = entry.getKey();
            final Collection<EmailStateTransitionInfo> transitions = entry.getValue();

            boolean shouldDispatch = false;
            final Collection<EmailSyncSettings> emailSyncSettings = partialSettings.get(emailAddress);

            if (Objects.nonNull(emailSyncSettings) && !emailSyncSettings.isEmpty()) {

                for (final EmailSyncSettings emailSetting : emailSyncSettings) {
                    final Optional<EmailSyncSettings> optionalFullSettings =
                            emailSyncSettingsService.getSyncSetting(orgId, emailSetting.getUserId(), emailSetting.id());

                    if (optionalFullSettings.isEmpty() || !optionalFullSettings.get().isSynced()) {
                        continue;
                    }
                    final EmailSyncConfig config = optionalFullSettings.get().getEmailSyncConfig();

                    if (newState == ARCHIVE || newState == UNARCHIVE) {
                        if (EmailSyncConfig.ConfigState.get(config.getArchiveMode()).equals(EmailSyncConfig.ConfigState.ACTIVE)) {
                            shouldDispatch = true;
                            break;
                        }
                    } else if (newState == ACTIVE || newState == INACTIVE) {
                        if (EmailSyncConfig.ConfigState.get(config.getDeleteMode()).equals(EmailSyncConfig.ConfigState.ACTIVE)) {
                            shouldDispatch = true;
                            break;
                        }
                    } else {
                        shouldDispatch = true;
                        break;
                    }
                }
            }

            if (shouldDispatch) {
                filteredForDispatch.put(emailAddress, transitions);
            }
        }

        emailDispatcher.update(filteredForDispatch, newState);
    }

    /**
     * <p>
     * Filters the thread ids which are part of team inbox.
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Represents a collection of thread ids to be filtered
     * @return A collection of thread ids which are part of team inbox
     */
    private Set<Long> extractAndFilterTeamInboxThreadIds(final long orgId, final Collection<Long> ids) {
        final Set<Long> teamInboxThreadIds = teamInboxManager.filterTeamInboxThreadIds(orgId, ids);

        if (!teamInboxThreadIds.isEmpty()) {
            ids.removeAll(teamInboxThreadIds);
        }

        return teamInboxThreadIds;
    }

    /**
     * <p>
     * Deletes the threads which are in trash.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a user id
     * @param ids    Represents a set of thread ids to be deleted from trash
     */
    private void deleteTrashThreads(final long orgId, final Long userId, final Set<Long> ids) {

        if (mailManager.existInTrash(orgId, userId, ids)) {
            mailManager.deleteConversationsForever(orgId, userId, ids);

        } else {
            throw ErrorCode.get(EmailErrorCodes.THREAD_NOT_FOUND,
                    "i18n.notifications.mail.thread.not.found");
        }
    }
}